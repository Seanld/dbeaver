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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Arrays;

public class ClickhouseSQLDialect extends GenericSQLDialect {

    private static final String[] CLICKHOUSE_FUNCTIONS = {
        "quantile",
        "quantileExact",
        "uniq",
        "concat",
        "replaceOne",
        "replaceAll",
        "toStartOfFifteenMinutes",
        "toStartOfFiveMinute",
        "toStartOfInterval",
        "toTimezone",
        "formatDateTime",
        "now",
        "multiIf",
        "geoToS2",
        "s2ToGeo",
        "greatCircleDistance",
        "greatCircleAngle",
        "plus",
        "minus",
        "multiply",
        "divide",
        "arrayConcat",
        "hasAll",
        "hasAny",
        "indexOf",
        "mapKeys",
        "mapValues",
        "UUIDNumToString",
        "UUIDStringToNum",
        "visitParamHas",
        "IPv4StringToNum",
        "randConstant",
        "javaHash",
        "bitmapBuild",
        "bitCount",
        "splitByChar",
        "splitByWhitespace",
        "toLowCardinality",
        "formatRow"
    };

    public ClickhouseSQLDialect() {
        super("Clickhouse SQL", "clickhouse");
    }

    @Override
    public boolean supportsOrderByIndex() {
        return false;
    }

    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        removeSQLKeyword("DEFAULT");
        removeSQLKeyword("SYSTEM");
        addFunctions(Arrays.asList(CLICKHOUSE_FUNCTIONS));
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        if (typeName.equals("String")) {
            return null;
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }

    @Override
    public boolean supportsNestedComments() {
        return true;
    }
}
