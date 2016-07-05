/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Array;

/**
 * @author Mikhail Ershov
 */
public class PrimitiveTypes {

    // prevents creating this utility class
    private PrimitiveTypes() {
    }

    public static boolean isPrimitive(String jlsType) {

        for (int i = 0; i < types.length; ++i) {
            if (types[i].JLSNotation.equals(jlsType)) {
                return true;
            }
        }

        return false;
    }

    //  Convert VM notation to JLS
    public static String getPrimitiveType(char vmType) {

        for (int i = 0; i < types.length; ++i) {
            if (vmType == types[i].VMNotation) {
                return types[i].JLSNotation;
            }
        }

        return null;
    }

    public static String getVMPrimitiveType(String jlsType) {
        for (int i = 0; i < types.length; ++i) {
            if (types[i].JLSNotation.equals(jlsType)) {
                return String.valueOf(types[i].VMNotation);
            }
        }

        return null;
    }

    public static String simpleObjectToString(Object o) {
        StringBuffer sb = new StringBuffer();
        simpleObjectToString(o, sb);
        return sb.toString().trim();
    }

    private static void simpleObjectToString(Object o, StringBuffer sb) {
        if (o.getClass().isArray()) {
            sb.append("[");
            Object[] arr = (Object[]) o;
            for (Object oo : arr) {
                simpleObjectToString(oo, sb);
            }
            // trim sb
            if (sb.charAt(sb.length()-1) == ' ') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("]");
        } else if (o instanceof String) {
            sb.append('"');
            sb.append(o);
            sb.append('"');
        } else if (o instanceof Class) {
            sb.append(((Class) o).getName());
        } else {
            sb.append(o.toString());
        }
        sb.append(' ');
    }


    private static class Pair {

        Pair(char vm, String jls) {
            VMNotation = vm;
            JLSNotation = jls;
        }
        char VMNotation;
        String JLSNotation;
    }

    private static Pair[] types = {
        new Pair('Z', "boolean"),
        new Pair('V', "void"),
        new Pair('I', "int"),
        new Pair('J', "long"),
        new Pair('C', "char"),
        new Pair('B', "byte"),
        new Pair('D', "double"),
        new Pair('S', "short"),
        new Pair('F', "float")
    };
}
