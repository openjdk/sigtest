/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.classpath;

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ExoticCharTools;
import com.sun.tdk.signaturetest.util.SwissKnife;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Maxim Sokolnikov
 * @author Roman Makarchuk
 */
class JarFileEntry extends ClasspathEntry {

    /**
     * Specified jar file. *
     */
    private JarFile jarfile;
    private BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);

    public JarFileEntry(ClasspathEntry previous, String jarfile) throws IOException {
        super(previous);
        init(jarfile);
    }

    public void init(String jarfile) throws IOException {
        this.jarfile = new JarFile(jarfile);

        Enumeration<JarEntry> entries = this.jarfile.entries();
        classes = new LinkedHashSet<>();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (name.endsWith(JAVA_CLASSFILE_EXTENSION)) {
                name = name.substring(0, name.length() - JAVA_CLASSFILE_EXTENSION_LEN).replace('/', '.');
                if (previousEntry == null || !previousEntry.contains(name)) {
                    classes.add(name.intern());
                }
            }
        }
        currentPosition = classes.iterator();
    }

    /**
     * Closes zip/jar file.
     */
    public void close() {
        if (jarfile != null) {
            try {
                jarfile.close();
            } catch (IOException e) {
                if (bo.isSet(Option.DEBUG)) {
                    SwissKnife.reportThrowable(e);
                }
            }
            jarfile = null;
            classes = null;
        }
    }

    /**
     * Returns <b>InputStream</b> providing bytecode for the required class, if
     * that class could be found by the given qualified name in
     * <code>JarFileEntry</code>.
     *
     * @param name Qualified name of the class requested.
     * @throws ClassNotFoundException if the class was not found inside this
     * <code>JarFileEntry</code>.
     */
    public InputStream findClass(String name) throws IOException, ClassNotFoundException {

        name = ExoticCharTools.decodeExotic(name);

        JarEntry jarEntry = jarfile.getJarEntry(name.replace('.', '/') + JAVA_CLASSFILE_EXTENSION);
        if (jarEntry == null) {
            throw new ClassNotFoundException(name);
        }

        return jarfile.getInputStream(jarEntry);
    }

}
