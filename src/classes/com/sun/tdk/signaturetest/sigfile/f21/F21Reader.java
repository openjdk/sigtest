/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.sigfile.f21;

import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.sigfile.Format;
import com.sun.tdk.signaturetest.sigfile.Parser;
import com.sun.tdk.signaturetest.sigfile.SignatureClassLoader;
import com.sun.tdk.signaturetest.sigfile.f31.F31Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mikhail Ershov
 */
class F21Reader extends SignatureClassLoader {

    F21Reader(Format format) {
        super(format);
    }

    protected Parser getParser() {
        // use F31Parser!
        return new F31Parser();
    }

    protected String convertClassDescr(String descr) {
        return descr;
    }

    protected List<String> convertClassDefinitions(List<String> definitions) {
        List<String> newDef = new ArrayList<>();
        for (String memberDef : definitions) {
            // 1) skip "supr null"
            if ("supr null".equals(memberDef)) {
                continue;
            }
            // 2) convert constant declaration to the new form
            memberDef = processConstants(memberDef);
            // 3) arrays - from VM from to Java
            memberDef = processArrays(memberDef);
            // 4) convert constructor to the new form
            memberDef = processConstructors(memberDef);

            newDef.add(memberDef);

        }
        return newDef;
    }

    private String processConstructors(String memberDef) {

        if (memberDef.startsWith("cons ")) {
            Matcher m = constructorName.matcher(memberDef);
            if (m.find()) {
                memberDef = m.replaceFirst("(");
            }
        }
        return memberDef;
    }

    private String processArrays(String memberDef) {

        Matcher m = arrayDeclaration.matcher(memberDef);
        while (m.find()) {
            int stPos = m.start();
            int eqPos = memberDef.indexOf(" = ");
            if (eqPos > -1 && stPos > eqPos) {
                // this is string constant value
                break;
            }
            int endPos = stPos;
            int tmp = memberDef.indexOf(' ', stPos);
            if (tmp >= 0) {
                endPos = tmp;
            }
            tmp = memberDef.indexOf(',', stPos);
            if (tmp >= 0 && (tmp < endPos || endPos == stPos)) {
                endPos = tmp;
            }
            tmp = memberDef.indexOf(')', stPos);
            if (tmp >= 0 && (tmp < endPos || endPos == stPos)) {
                endPos = tmp;
            }
            tmp = memberDef.indexOf(';', stPos);
            if (tmp >= 0 && (tmp < endPos || endPos == stPos)) {
                endPos = tmp + 1;
            }
            String p1 = memberDef.substring(0, stPos);
            String p4 = memberDef.substring(stPos, endPos);
            String p2 = MemberDescription.getTypeName(p4.replace('/', '.'));
            String p3 = memberDef.substring(endPos);

            memberDef = p1 + p2 + p3;
            m = arrayDeclaration.matcher(memberDef);
        }
        return memberDef;
    }

    private String processConstants(String memberDef) throws NumberFormatException {

        Matcher m = constantDeclaration.matcher(memberDef);
        if (m.find()) {
            String constDef = memberDef.substring(m.start(), m.end());
            memberDef = m.replaceFirst("");
            Matcher v = valueDeclaration.matcher(constDef);
            if (v.find()) {
                String value = constDef.substring(v.start() + 1, v.end() - 1);

                // try to determine constant type
                int end = memberDef.lastIndexOf(' ');
                int start = memberDef.lastIndexOf(' ', end - 1);
                String type = memberDef.substring(++start, end);
                Object oVal = null;
                switch (type) {
                    case "java.lang.String":
                        // decode unicode
                        Matcher uc = unicodeSim.matcher(value);
                        while (uc.find()) {
                            String uValue = value.substring(uc.start() + 2, uc.end());
                            char ch = (char) Integer.parseInt(uValue, 16);
                            String repl = "" + ch;
                            if (ch == '\\' || ch == '$') {
                                repl = "\\" + ch;
                            }
                            value = uc.replaceFirst(repl);
                            uc = unicodeSim.matcher(value);
                        }
                        oVal = value;

                        break;
                    case "boolean":
                        if ("0".equals(value)) {
                            oVal = Boolean.FALSE;
                        } else {
                            oVal = Boolean.TRUE;
                        }
                        break;
                    case "int":
                        oVal = new Integer(value);
                        break;
                    case "long":
                        oVal = new Long(value);
                        break;
                    case "char":
                        oVal = (char) Integer.parseInt(value);
                        break;
                    case "byte":
                        oVal = new Byte(value);
                        break;
                    case "double":
                        oVal = new Double(value);
                        break;
                    case "float":
                        oVal = new Float(value);
                        break;
                }

                if (oVal != null) {
                    value = MemberDescription.valueToString(oVal);
                }

                memberDef += " = " + value;
            }
        }
        return memberDef;
    }

    private static final Pattern constantDeclaration = Pattern.compile("<constant> <value=\".*\">");
    private static final Pattern valueDeclaration = Pattern.compile("\".*\"");
    private static final Pattern arrayDeclaration = Pattern.compile("\\[+[BCDFIJSZVL]");
    private static final Pattern constructorName = Pattern.compile("\\.\\w+\\(");
    private static final Pattern unicodeSim = Pattern.compile("\\\\u(?i)[\\da-f]{4}");
}
