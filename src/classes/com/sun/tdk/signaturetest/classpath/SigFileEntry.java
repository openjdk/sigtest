/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.sigfile.MultipleFileReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Mikhail Ershov
 */
public class SigFileEntry extends ClasspathEntry implements ClassDescriptionLoader {

    TreeMap<String, ClassDescription> readClasses;
    Iterator<Map.Entry<String, ClassDescription>> it;

    public SigFileEntry(ClasspathEntry previousEntry, String sigfile) throws IOException {
        super(previousEntry);
        init(sigfile);
    }

    @Override
    public void init(String sigFileName) throws IOException {
        FileManager fm = new FileManager();
        PrintWriter log = AppContext.getContext().getLogWriter();
        MultipleFileReader in = new MultipleFileReader(log, MultipleFileReader.CLASSPATH_MODE, fm);

        if (!in.readSignatureFile("", sigFileName)) {
            in.close();
            throw new IOException("Can't read " + sigFileName);
        }

        ClassDescription currentClass;
        readClasses = new TreeMap<>();

        in.rewind();
        while ((currentClass = in.nextClass()) != null) {
            readClasses.put(currentClass.getQualifiedName(), currentClass);
        }

        setListToBegin();
    }

    @Override
    public void close() {
        assert readClasses != null;
        readClasses.clear();
    }

    @Override
    public InputStream findClass(String qualifiedClassName) throws IOException, ClassNotFoundException {
        throw new ClassNotFoundException(qualifiedClassName);
    }

    @Override
    public ClassDescription load(String qualifiedClassName) throws ClassNotFoundException {
        assert readClasses != null;
        if (!readClasses.containsKey(qualifiedClassName)) {
            throw new ClassNotFoundException(qualifiedClassName);
        }
        return readClasses.get(qualifiedClassName);
    }

    @Override
    protected boolean contains(String className) {
        return readClasses.containsKey(className);
    }

    @Override
    public boolean isEmpty() {
        return readClasses.isEmpty();
    }

    @Override
    public boolean hasNext() {
        assert  it != null;
        return it.hasNext();
    }

    @Override
    public String nextClassName() {
        assert  it != null;
        return it.next().getKey();
    }

    @Override
    public void setListToBegin() {
        it = readClasses.entrySet().iterator();
    }
}
