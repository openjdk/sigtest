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
package com.sun.tdk.signaturetest.sigfile.f41;

import com.sun.tdk.signaturetest.sigfile.f40.F40Parser;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.ConstructorDescr;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.MemberType;

import java.util.Set;

/**
 * Parse string representation used in sigfile v4.1 and create corresponding
 * member object
 *
 * @author Mikhail Ershov
 */
public class F41Parser extends F40Parser {

    protected boolean parseFutureSpecific(String str, ClassDescription cl) {
        if (str.startsWith(F41Format.X_FIELDS)) {
            Set<String> internalFields = parseInternals(str);
            cl.setXFields(internalFields);
            return true;
        }
        if (str.startsWith(F41Format.X_CLASSES)) {
            Set<String> internalClasses = parseInternals(str);
            cl.setXClasses(internalClasses);
            return true;
        }
        return false;
    }

    protected String convertFutureSpecific(String str, ClassDescription classDescription) {
        if (str.startsWith(MemberType.CONSTRUCTOR.toString() /*  toString returns prefix here */)) {
            // is it "old-style" ?
            if (!str.contains(ConstructorDescr.CONSTRUCTOR_NAME + "(")) {
                String name = '.' + classDescription.getName() + '(';
                int i = str.indexOf(name);
                // String.replace can not be used here - it appeared in 1.5
                if (i >= 0) {
                    String first = str.substring(0, i);
                    String last = str.substring(i + name.length());
                    str = first + "." + ConstructorDescr.CONSTRUCTOR_NAME + "(" + last;
                }
            }
        }
        return str;
    }

    protected ClassDescription processClassDescription(String classDefinition) {
        ClassDescription classDescription = (ClassDescription) parse(classDefinition);
        classDescription.setupClassName(classDescription.getQualifiedName(),
                MemberDescription.NO_DECLARING_CLASS);
        return classDescription;
    }

    protected void processOuter(ClassDescription classDescription, String str) {
        classDescription.setDeclaringClass(parseOuter(str));
        classDescription.setupClassName(classDescription.getQualifiedName(),
                classDescription.getDeclaringClassName());
    }

    protected String parseOuter(String str) {
        return str.substring(ClassDescription.OUTER_PREFIX.length() + 1);
    }
}
