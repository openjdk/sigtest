/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.model;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Represents Type Annotations (JSR 308)
 *
 * @author Mikhail Ershov
 */
public class AnnotationItemEx extends AnnotationItem {

    public static final String ANNOTATION_EX_PREFIX = "typeAnno";

    public AnnotationItemEx(int target) {
        super(target);
    }

    // for parser
    public AnnotationItemEx() {
    }

    private int targetType;
    private int parameterIndex;
    private int boundIndex;
    private int typeIndex;
    private String path;
    private boolean tracked = true;
    public static final int TARGET_CLASS_TYPE_PARAMETER = 0x00;
    public static final int TARGET_METHOD_TYPE_PARAMETER = 0x01;
    public static final int TARGET_CLASS_EXTENDS = 0x10;
    public static final int TARGET_CLASS_TYPE_PARAMETER_BOUND = 0x11;
    public static final int TARGET_METHOD_TYPE_PARAMETER_BOUND = 0x12;
    public static final int TARGET_FIELD = 0x13;
    public static final int TARGET_METHOD_RETURN = 0x14;
    public static final int TARGET_METHOD_RECEIVER = 0x15;
    public static final int TARGET_METHOD_FORMAL_PARAMETER = 0x16;
    public static final int TARGET_THROWS = 0x17;
    public static final int TARGET_LOCAL_VARIABLE = 0x40;
    public static final int TARGET_RESOURCE_VARIABLE = 0x41;
    public static final int TARGET_EXCEPTION_PARAMETER = 0x42;
    public static final int TARGET_INSTANCEOF = 0x43;
    public static final int TARGET_NEW = 0x44;
    public static final int TARGET_CONSTRUCTOR_REFERENCE = 0x45;
    public static final int TARGET_METHOD_REFERENCE = 0x46;
    public static final int TARGET_CAST = 0x47;
    public static final int TARGET_CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;
    public static final int TARGET_METHOD_INVOCATION_TYPE_ARGUMENT = 0x49;
    public static final int TARGET_CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4a;
    public static final int TARGET_METHOD_REFERENCE_TYPE_ARGUMENT = 0x4b;
    public static final String ANN_TARGET_TYPE = "target";
    public static final String ANN_TYPE_IND = "type";
    public static final String ANN_BOUND_IND = "bound";
    public static final String ANN_PARAM_IND = "param";
    public static final String ANN_PATH = "path";

    public void parseBinaryDescription(DataInputStream is) throws IllegalStateException, IOException {
        int target_type = is.readUnsignedByte();
        setTargetType(target_type);
        //System.err.println("type=" + intToHex(target_type) + "  " + getExtendedAnnotationName(target_type));

        // some specific code
        switch (target_type) {

            // 3.3.1 Type parameters
            case TARGET_METHOD_TYPE_PARAMETER:
            case TARGET_CLASS_TYPE_PARAMETER:
                setParameterIndex(is.readUnsignedByte()).setTracked(true);
                break;

            // 3.3.2 Class supertypes: extends and implements clauses
            case TARGET_CLASS_EXTENDS:
                setTypeIndex(is.readUnsignedShort()).setTracked(true);
                break;

            // 3.3.3 Type parameter bounds
            case TARGET_CLASS_TYPE_PARAMETER_BOUND:
            case TARGET_METHOD_TYPE_PARAMETER_BOUND:
                setParameterIndex(is.readUnsignedByte()).setBoundIndex(is.readUnsignedByte()).setTracked(true);
                break;

            // 3.3.4 Method return type, receiver, and fields
            case TARGET_FIELD:
            case TARGET_METHOD_RETURN:
            case TARGET_METHOD_RECEIVER:
                setTracked(true);
                break;

            // 3.3.5 Method formal parameters
            case TARGET_METHOD_FORMAL_PARAMETER:
                setParameterIndex(is.readUnsignedByte()).setTracked(true);
                break;

            // 3.3.6 throws clauses
            case TARGET_THROWS:
                setTypeIndex(is.readUnsignedShort()).setTracked(true);
                break;

            //----------------------------------------------------------
            // 3.3.7 Local variables and resource variables
            case TARGET_LOCAL_VARIABLE:
            case TARGET_RESOURCE_VARIABLE:
                int table_length = is.readUnsignedShort();
                for (int i = 0; i < table_length; i++) {
                    is.readUnsignedShort();
                    is.readUnsignedShort();
                    is.readUnsignedShort();
                }
                setTracked(false);
                break;

            // 3.3.8 Exception parameters (catch clauses)
            case TARGET_EXCEPTION_PARAMETER:
//                is.readUnsignedShort();
                is.readUnsignedByte();
                setTracked(false);
                break;

            // 3.3.9 Type tests, object creation, and method/constructor references
            case TARGET_INSTANCEOF:
            case TARGET_NEW:
            case TARGET_CONSTRUCTOR_REFERENCE:
            case TARGET_METHOD_REFERENCE:

                is.readUnsignedShort();
                setTracked(false);
                break;

            // 3.3.10 Casts and type arguments to constructor/method invocation/references
            case TARGET_CAST:
            case TARGET_CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case TARGET_METHOD_INVOCATION_TYPE_ARGUMENT:
            case TARGET_CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
            case TARGET_METHOD_REFERENCE_TYPE_ARGUMENT:

                is.readUnsignedShort();
                is.readUnsignedByte();
                setTracked(false);
                break;

            default:

                throw new IllegalStateException("Unknown type " + intToHex(target_type));
        }

        // read paths
        int path_length = is.readUnsignedByte();
        if (path_length > 0) {
            StringBuffer p = new StringBuffer();
            for (int i = 0; i < path_length; i++) {
                if (p.length() > 0) {
                    p.append('-');
                }
                int i1 = is.readUnsignedByte();
                p.append(Integer.toHexString(i1));
                p.append(':');
                int i2 = is.readUnsignedByte();
                p.append(Integer.toHexString(i2));
            }
            setPath(p.toString());
        }
        // classic part
    }

    public int getTargetType() {
        return targetType;
    }

    public AnnotationItemEx setTargetType(int target_type) {
        this.targetType = target_type;
        return this;
    }

    public boolean getTracked() {
        return tracked;
    }

    public AnnotationItemEx setTracked(boolean tracked) {
        this.tracked = tracked;
        return this;
    }

    public String getPath() {
        return path;
    }

    public AnnotationItemEx setPath(String path) {
        this.path = path;
        return this;
    }

    public AnnotationItemEx addToPath(String part) {
        if (path == null) {
            path = part;
        } else {
            path = path + "-" + part;
        }
        return this;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public AnnotationItemEx setParameterIndex(int parameterIndex) {
        this.parameterIndex = parameterIndex;
        return this;
    }

    public int getBoundIndex() {
        return boundIndex;
    }

    public AnnotationItemEx setBoundIndex(int boundIndex) {
        this.boundIndex = boundIndex;
        return this;
    }

    public int getTypeIndex() {
        return typeIndex;
    }

    public AnnotationItemEx setTypeIndex(int typeIndex) {
        this.typeIndex = typeIndex;
        return this;
    }

    // -----------------
    protected String getSpecificData() {
        StringBuffer sb = new StringBuffer();
        addTargetType(sb);
        switch (targetType) {

            case TARGET_CLASS_TYPE_PARAMETER:
            case TARGET_METHOD_TYPE_PARAMETER:
                addParameterInd(sb);
                break;

            case TARGET_CLASS_EXTENDS:
                addTypeInd(sb);
                break;

            case TARGET_CLASS_TYPE_PARAMETER_BOUND:
            case TARGET_METHOD_TYPE_PARAMETER_BOUND:
                addParameterInd(sb);
                addBoundInd(sb);
                break;

            case TARGET_METHOD_RETURN:
            case TARGET_METHOD_RECEIVER:
            case TARGET_FIELD:
                break;

            case TARGET_METHOD_FORMAL_PARAMETER:
                addParameterInd(sb);
                break;

            case TARGET_THROWS:
                addTypeInd(sb);
                break;
            // -----------------------------------------------------------------

            case TARGET_LOCAL_VARIABLE:
            case TARGET_RESOURCE_VARIABLE:
            case TARGET_EXCEPTION_PARAMETER:
            case TARGET_INSTANCEOF:
            case TARGET_NEW:
            case TARGET_CONSTRUCTOR_REFERENCE:
            case TARGET_METHOD_REFERENCE:
            case TARGET_CAST:
            case TARGET_CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case TARGET_METHOD_INVOCATION_TYPE_ARGUMENT:
            case TARGET_CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
            case TARGET_METHOD_REFERENCE_TYPE_ARGUMENT:

                break;

            default:
                throw new IllegalStateException("Unknown type " + intToHex(targetType));
        }

        addPath(sb);
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    private void addTargetType(StringBuffer sb) {
        sb.append("[").append(ANN_TARGET_TYPE).append("=").append(intToHex(targetType)).append(";");
    }

    private void addTypeInd(StringBuffer sb) {
        sb.append(ANN_TYPE_IND).append("=").append(typeIndex).append(";");
    }

    private void addBoundInd(StringBuffer sb) {
        sb.append(ANN_BOUND_IND).append("=").append(boundIndex).append(";");
    }

    private void addParameterInd(StringBuffer sb) {
        sb.append(ANN_PARAM_IND).append("=").append(parameterIndex).append(";");
    }

    private void addPath(StringBuffer sb) {
        if (path != null) {
            sb.append(ANN_PATH).append("=").append(path).append(";");
        }
    }

    public static String intToHex(int i) {
        String s = Integer.toHexString(i).toUpperCase();
        if (s.length() == 1) {
            s = "0" + s;
        }
        return "0x" + s;
    }

    // this code should be 1.3 compatible
    // so we can't use 1.5's Arrays.toString()
    private static String arrayToString(int[] a) {
        if (a == null) {
            return "null";
        }
        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }

        StringBuffer b = new StringBuffer();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax) {
                return b.append(']').toString();
            }
            b.append(",");
        }
    }

    protected String getPrefix() {
        return ANNOTATION_EX_PREFIX;
    }
}
