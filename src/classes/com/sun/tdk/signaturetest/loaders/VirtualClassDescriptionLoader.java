/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.loaders;

import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * An abstract class loader that stores and restores classes to/from internal
 * cache
 *
 * @author Roman Makarchuk
 * @author Mikhail Ershov
 */
public class VirtualClassDescriptionLoader extends FeaturesHolder implements ClassDescriptionLoader {

    private final Map<String, ClassDescription> classDescriptions;

    public VirtualClassDescriptionLoader() {
        this.classDescriptions = new TreeMap<>();
    }

    public ClassDescription load(String className) throws ClassNotFoundException {
        ClassDescription cl = classDescriptions.get(className);
        if (cl == null) {
            throw new ClassNotFoundException(className);
        }
        return cl;
    }

    public void add(ClassDescription cls) {
        classDescriptions.put(cls.getQualifiedName(), cls);
    }

    public Iterator<ClassDescription> getClassIterator() {
        return classDescriptions.values().iterator();
    }

    public void remove(ClassDescription cls) {
        remove(cls.getQualifiedName());
    }

    public void remove(String clsName) {
        classDescriptions.remove(clsName);
    }

    public void cleanUp() {
        classDescriptions.clear();
    }
}
