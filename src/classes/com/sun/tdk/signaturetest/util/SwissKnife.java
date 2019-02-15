/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.util;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.ConstructorDescr;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Objects;

public class SwissKnife {

    // prevent creating this utility class
    private SwissKnife() {
    }

    /**
     * Determines whether the object <code>x</code> is equal to object
     * <code>y</code>. If ( <code>x</code> == <code>null</code>) and (
     * <code>y</code> == <code>null</code>) the result is true.
     *
     * @param x - first comparable object, may by <code>null</code>
     * @param y - second comparable object, may by <code>null</code>
     * @return true if x equal to y
     */
    public static boolean equals(Object x, Object y) {
        return Objects.equals(x, y);
    }

    public static boolean canBeSubclassed(ClassDescription cd) {
        ConstructorDescr[] constrs = cd.getDeclaredConstructors();
        if (cd.isFinal() || constrs == null || constrs.length == 0) {
            return false;
        }
        for (ConstructorDescr constr : constrs) {
            if (constr.isPublic() || constr.isProtected()) {
                return true;
            }
        }
        return false;
    }

    public static void reportThrowable(Throwable t) {
        t.printStackTrace();
    }

    public static void reportThrowable(Throwable t, PrintWriter pw) {
        t.printStackTrace(pw);
    }

    public static FileReader approveFileReader(String name) throws FileNotFoundException {
        return new FileReader(name);
    }

    public static FileInputStream approveFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(name));
    }
}
