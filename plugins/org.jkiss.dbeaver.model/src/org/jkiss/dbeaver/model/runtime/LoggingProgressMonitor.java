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
package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IProgressMonitor;

import java.io.PrintStream;

/**
 * Progress monitor null implementation
 */
public class LoggingProgressMonitor extends DefaultProgressMonitor {

    public LoggingProgressMonitor() {
        super(new LoggingMonitorProxy());
    }

    private static class LoggingMonitorProxy implements IProgressMonitor {

        private PrintStream out = System.out;

        @Override
        public void beginTask(String name, int totalWork) {
            out.println(name);
        }

        @Override
        public void done() {

        }

        @Override
        public void internalWorked(double work) {

        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setCanceled(boolean value) {

        }

        @Override
        public void setTaskName(String name) {

        }

        @Override
        public void subTask(String name) {
            out.println("\t" + name);
        }

        @Override
        public void worked(int work) {

        }
    }

}