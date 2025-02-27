/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.net;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.impl.app.CertificateGenHelper;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Default Java SSL Handler. Saves certificate in local trust store
 */
public class SSLHandlerTrustStoreImpl extends SSLHandlerImpl {

    public static final String PROP_SSL_CA_CERT = "ssl.ca.cert";
    public static final String PROP_SSL_CLIENT_CERT = "ssl.client.cert";
    public static final String PROP_SSL_CLIENT_KEY = "ssl.client.key";
    public static final String PROP_SSL_SELF_SIGNED_CERT = "ssl.self-signed-cert";
    public static final String PROP_SSL_KEYSTORE = "ssl.keystore";
    public static final String PROP_SSL_METHOD = "ssl.method";
    public static final String PROP_SSL_FORCE_TLS12 = "ssl.forceTls12";
    public static final String CERT_TYPE = "ssl";

    public static final String TLS_PROTOCOL_VAR_NAME = "jdk.tls.client.protocols";
    public static final String TLS_1_2_VERSION = "TLSv1.2";

    /**
     * Creates certificates and adds them into trust store
     */
    public static void initializeTrustStore(DBRProgressMonitor monitor, DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws DBException, IOException {
        final DBACertificateStorage securityManager = dataSource.getContainer().getPlatform().getCertificateStorage();

        final String caCertProp = sslConfig.getStringProperty(PROP_SSL_CA_CERT);
        final String clientCertProp = sslConfig.getStringProperty(PROP_SSL_CLIENT_CERT);
        final String clientCertKeyProp = sslConfig.getStringProperty(PROP_SSL_CLIENT_KEY);
        final String selfSignedCert = sslConfig.getStringProperty(PROP_SSL_SELF_SIGNED_CERT);
        final String keyStore = sslConfig.getStringProperty(PROP_SSL_KEYSTORE);
        final String password = sslConfig.getPassword();

        final SSLConfigurationMethod method = CommonUtils.valueOf(
            SSLConfigurationMethod.class,
            sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD),
            SSLConfigurationMethod.CERTIFICATES);

        {
            if (method == SSLConfigurationMethod.KEYSTORE && keyStore != null) {
                monitor.subTask("Load keystore");
                char[] keyStorePasswordData = CommonUtils.isEmpty(password) ? new char[0] : password.toCharArray();
                securityManager.addCertificate(dataSource.getContainer(), CERT_TYPE, keyStore, keyStorePasswordData);
            } else if (CommonUtils.toBoolean(selfSignedCert)) {
                monitor.subTask("Generate self-signed certificate");
                securityManager.addSelfSignedCertificate(dataSource.getContainer(), CERT_TYPE, "CN=" + dataSource.getContainer().getActualConnectionConfiguration().getHostName());
            } else if (!CommonUtils.isEmpty(caCertProp) || !CommonUtils.isEmpty(clientCertProp)) {
                monitor.subTask("Load certificates");
                byte[] caCertData = CommonUtils.isEmpty(caCertProp) ? null : IOUtils.readFileToBuffer(new File(caCertProp));
                byte[] clientCertData = CommonUtils.isEmpty(clientCertProp) ? null : IOUtils.readFileToBuffer(new File(clientCertProp));
                byte[] keyData = CommonUtils.isEmpty(clientCertKeyProp) ? null : IOUtils.readFileToBuffer(new File(clientCertKeyProp));
                securityManager.addCertificate(dataSource.getContainer(), CERT_TYPE, caCertData, clientCertData, keyData);
            } else {
                securityManager.deleteCertificate(dataSource.getContainer(), CERT_TYPE);
            }
        }
    }

    public static void setGlobalTrustStore(DBPDataSource dataSource) {
        final DBACertificateStorage securityManager = dataSource.getContainer().getPlatform().getCertificateStorage();

        String keyStorePath = securityManager.getKeyStorePath(dataSource.getContainer(), CERT_TYPE).getAbsolutePath();
        String keyStoreType = securityManager.getKeyStoreType(dataSource.getContainer());
        char[] keyStorePass = securityManager.getKeyStorePassword(dataSource.getContainer(), CERT_TYPE);

        System.setProperty("javax.net.ssl.trustStore", keyStorePath);
        System.setProperty("javax.net.ssl.trustStoreType", keyStoreType);
        System.setProperty("javax.net.ssl.trustStorePassword", String.valueOf(keyStorePass));
        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);
        System.setProperty("javax.net.ssl.keyStorePassword", String.valueOf(keyStorePass));
    }

    public static SSLContext createTrustStoreSslContext(DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws Exception {
        final DBACertificateStorage securityManager = dataSource.getContainer().getPlatform().getCertificateStorage();
        KeyStore trustStore = securityManager.getKeyStore(dataSource.getContainer(), CERT_TYPE);
        char[] keyStorePass = securityManager.getKeyStorePassword(dataSource.getContainer(), CERT_TYPE);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(trustStore, keyStorePass);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        if (sslConfig.getBooleanProperty(PROP_SSL_SELF_SIGNED_CERT)) {
            trustManagers = CertificateGenHelper.NON_VALIDATING_TRUST_MANAGERS;
        } else {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }

        final boolean forceTLS12 = sslConfig.getBooleanProperty(PROP_SSL_FORCE_TLS12);


        SSLContext sslContext = forceTLS12 ? SSLContext.getInstance(TLS_1_2_VERSION) : SSLContext.getInstance("SSL");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    public static SSLSocketFactory createTrustStoreSslSocketFactory(DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws Exception {
        return createTrustStoreSslContext(dataSource, sslConfig).getSocketFactory();
    }

}
