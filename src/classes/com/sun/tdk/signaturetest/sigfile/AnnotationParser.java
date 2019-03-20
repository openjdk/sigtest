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
package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.model.AnnotationItem;
import com.sun.tdk.signaturetest.model.AnnotationItemEx;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Sergey Ivashin
 * @author Mikhail Ershov
 */
public class AnnotationParser {

    private static final String CLASS_PREFIX = "java.lang.Class";
    private static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(AnnotationParser.class);

    // Opposite action that toString() method does.
    // TODO should be moved to the parser as well as "toString" moved to the writer
    public AnnotationItem parse(String str) {
        return parse(new StringBuffer(str));
    }

    private AnnotationItem parse(StringBuffer theRest) {

        if (theRest == null) {
            return null;
        }

//        str = "anno 0 javax.xml.ws.BindingType(java.lang.String value=\"http://schemas.xmlsoap.org/wsdl/soap/http\", javax.xml.ws.Feature[] features=[anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\"http://www.w3.org/2005/08/addressing/module\", javax.xml.ws.FeatureParameter[] parameters=[]), anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\"http://www.w3.org/2004/08/soap/features/http-optimization\", javax.xml.ws.FeatureParameter[] parameters=[anno 0 javax.xml.ws.FeatureParameter(java.lang.String name=\"MTOM_THRESHOLD\", java.lang.String value=\"1000\")])]):     anno 0 javax.xml.ws.BindingType(java.lang.String value=\\\"http://schemas.xmlsoap.org/wsdl/soap/http\\\", javax.xml.ws.Feature[] features=[anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\\\"http://www.w3.org/2005/08/addressing/module\\\", javax.xml.ws.FeatureParameter[] parameters=[]), anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\\\"http://www.w3.org/2004/08/soap/features/http-optimization\\\", javax.xml.ws.FeatureParameter[] parameters=[anno 0 javax.xml.ws.FeatureParameter(java.lang.String name=\\\"MTOM_THRESHOLD\\\", java.lang.String value=\\\"1000\\\")])])";
        String str = theRest.toString();
        AnnotationItem item;

        if (!str.startsWith(AnnotationItem.ANNOTATION_PREFIX) && !str.startsWith(AnnotationItemEx.ANNOTATION_EX_PREFIX)) {
            throw new IllegalArgumentException(i18n.getString("AnnotationParser.error.bad_annotation_descr") + str);
        }

        if (str.startsWith(AnnotationItemEx.ANNOTATION_EX_PREFIX)) {
            item = new AnnotationItemEx();
        } else {
            item = new AnnotationItem();
        }

        int pos;
        pos = str.indexOf(' ');
        // skip the prefix
        str = str.substring(pos).trim();

        pos = str.indexOf(' ');
        String specificData = str.substring(0, pos);
        if (item instanceof AnnotationItemEx) {
            parseAnnExData((AnnotationItemEx) item, specificData);
        } else {
            parseAnnData(item, specificData);
        }

        // remove target
        str = str.substring(pos + 1);

        pos = str.indexOf('(');
        item.setName(str.substring(0, pos).trim());
        int endPos = findCorresponding(str, '(', ')');
        String rest = str.substring(endPos + 1);
        str = str.substring(pos + 1, endPos);

        if (!str.isEmpty()) {

            while (!str.isEmpty() && str.charAt(0) != ')') {
                pos = parseMember(item, str);
                str = str.substring(pos);
                if (!str.isEmpty() && str.charAt(0) == ',') {
                    str = str.substring(1).trim();
                }
            }
        }

        theRest.delete(0, theRest.length() - 1); // clear old if any
        theRest.append(rest);

        return item;

    }

    // unpack annotations from Container's array
    public List<AnnotationItem> unpack(String annS) {
        List<AnnotationItem> res = new ArrayList<>();
        String str = annS.trim();
        if (str.charAt(0) == '[') {
            str = str.substring(1);
        } else {
            return Collections.emptyList();
        }

        if (str.charAt(str.length() - 1) == ']') {
            str = str.substring(0, str.length() - 1);
        }

        StringBuffer sb = new StringBuffer(str);
        while (sb.length() > 0) {
            AnnotationItem nA = parse(sb);
            res.add(nA);
            if (sb.length() > 0 && sb.charAt(0) == ')') {
                sb.deleteCharAt(0);
            }
            if (sb.length() > 0 && sb.charAt(0) == ',') {
                sb.deleteCharAt(0);
            }
            if (sb.length() > 0 && sb.charAt(0) == ' ') {
                sb.deleteCharAt(0);
            }
        }

        return res;
    }

    private int findCorresponding(String str, char open, char close) {
        char[] chars = str.toCharArray();
        int count = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == open) {
                count++;
            } else if (chars[i] == close) {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void parseAnnData(AnnotationItem item, String specificData) {
        item.setTarget(Integer.valueOf(specificData));
    }

    private void parseAnnExData(AnnotationItemEx item, String specificData) {
        assert specificData.startsWith("[");
        assert specificData.endsWith("]");
        specificData = specificData.substring(1, specificData.length() - 1);
        StringTokenizer st = new StringTokenizer(specificData, ";");
        while (st.hasMoreTokens()) {
            String set = st.nextToken();
            int delPos = set.indexOf('=');
            assert delPos > 0;
            String name = set.substring(0, delPos);
            String val = set.substring(delPos + 1);
            switch (name) {
                case AnnotationItemEx.ANN_TARGET_TYPE:
                    item.setTargetType(Integer.parseInt(val.substring(2), 16));
                    break;
                case AnnotationItemEx.ANN_TYPE_IND:
                    item.setTypeIndex(Integer.parseInt(val));
                    break;
                case AnnotationItemEx.ANN_BOUND_IND:
                    item.setBoundIndex(Integer.parseInt(val));
                    break;
                case AnnotationItemEx.ANN_PARAM_IND:
                    item.setParameterIndex(Integer.parseInt(val));
                    break;
                case AnnotationItemEx.ANN_PATH:
                    item.setPath(val);
                    break;
                default:
                    assert false;
                    break;
            }

        }
    }

    protected int parseMember(AnnotationItem item, String str) {

        int pos, result = 0;

        AnnotationItem.Member m = new AnnotationItem.Member();

        pos = str.indexOf(' ');

        // java.lang.Class<? extends java.util.ArrayList<? super javax.swing.JLabel>> value=class com.sun.tdk.signaturetest.model.Regtest_6564000$CL_4
        if (str.startsWith(CLASS_PREFIX + "<")) {
            // skip possible spaces inside
            char[] strChar = str.toCharArray();
            int level = 0;
            for (int i = CLASS_PREFIX.length(); i < strChar.length; i++) {
                if (strChar[i] == '<') {
                    level++;
                } else if (strChar[i] == '>') {
                    level--;
                }
                if (level == 0 && strChar[i + 1] == ' ') {
                    pos = i + 1;
                    break;
                }
            }
        }

        m.type = str.substring(0, pos);
        str = str.substring(pos + 1).trim();
        result += pos + 1;

        pos = str.indexOf('=');
        m.name = str.substring(0, pos);

        str = str.substring(pos + 1).trim();
        result += pos + 1;

        char ch = str.charAt(0);

        switch (ch) {
            case '[': {
                pos = findClosingBracket(str, 1, '[', ']') + 1;
                break;
            }

            case '"':
            case '\'': {
                pos = str.indexOf(ch, 1) + 1;
                break;
            }

            case 'a': {
                if (str.startsWith(AnnotationItem.ANNOTATION_PREFIX)) {
                    AnnotationItem a = parse(str);
                    pos = a.toString().length();
                    break;
                }
            }

            default: {
                pos = str.indexOf(',');

                if (pos == -1) {
                    pos = str.indexOf(')');
                }

                if (pos == -1) {
                    pos = str.length();
                }
            }
        }

        m.value = str.substring(0, pos);
        item.addMember(m);

        result += pos;
        return result;
    }

    private int findClosingBracket(String str, int startPos, char openingChar, char closingChar) {

        int level = 0;
        int len = str.length();
        for (int i = startPos; i < len; ++i) {

            char ch = str.charAt(i);

            if (ch == openingChar) {
                ++level;
                continue;
            }

            if (ch == closingChar) {
                if (level == 0) {
                    return i;
                }
                --level;
            }
        }

        return -1;
    }
}
