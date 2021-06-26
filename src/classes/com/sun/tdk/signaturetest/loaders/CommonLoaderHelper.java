/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.PermittedSubClass;

/**
 * This class contains common methods for loaders
 *
 * @author Victor Rudometov
 */
class CommonLoaderHelper {

    /**
     * Reads typenames from permits clause of a sealed class (classObject) and writes down fully qualified names to
     * ClassDescription object cd
     * @param cd ClassDescription object to write found permitted subclasses
     * @param classObject object to read permitted subclasses from
     */
    static void readPermittedSubClasses(ClassDescription cd, Class<?> classObject) {
        try {
            Object permClassesObj = Class.class.getMethod("getPermittedSubclasses").invoke(classObject);
            if (permClassesObj instanceof Class<?>[]) {
                Class<?>[] permClasses = (Class<?>[]) permClassesObj;

                if (permClasses.length > 0) {
                    PermittedSubClass[] permittedSubClassesArray = new PermittedSubClass[permClasses.length];

                    for (int i = 0; i < permClasses.length; i++) {
                        Class<?> clss = permClasses[i];
                        String fullQualifiedName = clss.getName();

                        PermittedSubClass permittedSubClass = new PermittedSubClass();
                        permittedSubClass.setupGenericClassName(fullQualifiedName);
                        permittedSubClassesArray[i] = permittedSubClass;
                    }
                    cd.setPermittedSubclasses(permittedSubClassesArray);
                }
            }
        } catch (ReflectiveOperationException | ClassCastException | NullPointerException | ArrayIndexOutOfBoundsException e) {
            //just skipping since getPermittedSubclasses might not be available
        }
    }
}
