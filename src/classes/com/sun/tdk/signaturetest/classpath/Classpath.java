/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.model.ClassDescription;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 * @author Roman Makarchuk
 * @author Mikhail Ershov
 */
public interface Classpath extends AutoCloseable {

    /**
     * Initialize the module with given parameter: <code>classPath</code>.
     *
     * @param classPath parameter provided to initialize module (usually a
     * directory or file classPath).
     */
    void init(String classPath) throws IOException;

    /**
     * Free resources used (if any) or do nothing.
     */
    @Override
    void close();

    /**
     * @return true if more classes available
     */
    boolean hasNext();

    /**
     * Return name of the next available class.
     *
     * @return Class qualified name
     */
    String nextClassName();

    boolean isEmpty();

    void printErrors(PrintWriter out);

    /**
     * Reset enumeration of classes which are found by this module.
     */
    void setListToBegin();

    /**
     * Returns <b>InputStream</b> instance providing bytecode for the required
     * class. If classpath has several classes with the same qualified name, an
     * implementation must always return first class in the path
     */
    InputStream findClass(String qualifiedClassName) throws IOException, ClassNotFoundException;

    ClassDescription findClassDescription(String qualifiedClassName) throws ClassNotFoundException;

    KIND_CLASS_DATA isClassPresent(String qualifiedClassName);

    enum KIND_CLASS_DATA {
        DESCRIPTION, BYTE_CODE, NOT_FOUND
    }


}
