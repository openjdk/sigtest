/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.classpath.Classpath;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Application's context holder
 * Stores environmental values such as options and settings
 * @author Mikhail Ershov
 */
public abstract class AppContext {

    /*
     * On demand holder singleton pattern
     * See: AppContext.getContext()
     */
    private static class AppContextHolder {
        public static final AppContext INSTANCE = new AppContextImpl();
    }

    public static AppContext getContext() {
        return AppContextHolder.INSTANCE;
    }

    public abstract String getString(String id);

    public abstract <T> T getBean(Class<T> clz);

    public abstract void setString(String id, String value);

    public abstract void setBean(Object bean);

    public abstract PrintWriter getLogWriter();

    public abstract void setLogWriter(PrintWriter pw);

    public abstract AppContext clean();

    public abstract Classpath getInputClasspath();

    public abstract void setInputClasspath(Classpath cp);

    public abstract void setClassLoader(ClassDescriptionLoader loader);

    public abstract ClassDescriptionLoader getClassLoader();

    private static class AppContextImpl extends AppContext {

        private ConcurrentHashMap<String, String> strings = new ConcurrentHashMap<>();
        private ConcurrentHashMap<Class<?>, Object> beans = new ConcurrentHashMap<>();
        private PrintWriter log;
        private Classpath cp;
        private ClassDescriptionLoader loader;

        @Override
        public void setString(String id, String value) {
            strings.put(id, value);
        }

        @Override
        public void setBean(Object bean) {
            beans.put(bean.getClass(), bean);
        }

        @Override
        public PrintWriter getLogWriter() {
            return log;
        }

        @Override
        public void setLogWriter(PrintWriter pw) {
            log = pw;
        }

        @Override
        public String getString(String id) {
            return strings.get(id);
        }

        @Override
        public synchronized <T> T getBean(Class<T> clz) {
            if (!beans.containsKey(clz)) {
                Object beanObj = null;
                try {
                    beanObj = clz.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                beans.put(clz, beanObj);
            }
            return (T) beans.get(clz);
        }

        @Override
        public synchronized AppContext clean() {
            strings.clear();
            beans.clear();
            log = null;
            cp = null;
            return this;
        }

        @Override
        public Classpath getInputClasspath() {
            return cp;
        }

        @Override
        public void setInputClasspath(Classpath cp) {
            this.cp = cp;
        }

        @Override
        public void setClassLoader(ClassDescriptionLoader loader) {
            this.loader = loader;
        }

        @Override
        public ClassDescriptionLoader getClassLoader() {
            return loader;
        }
    }
}


