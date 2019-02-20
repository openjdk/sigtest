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
package com.sun.tdk.signaturetest.sigfile.f40;

import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.sigfile.AnnotationParser;
import com.sun.tdk.signaturetest.sigfile.Parser;

import java.util.*;

/**
 * Parse string representation used in sigfile v4.0 and create corresponding
 * member object
 *
 * @author Roman Makarchuk
 * @author Mikhail Ershov
 */
public class F40Parser implements Parser {

    private static final String VALUE = " value= ";
    private String line;
    private int linesz;
    private int idx;
    private char chr;
    private List<String> elems;
    private String currentClassName;

    public ClassDescription parseClassDescription(String classDefinition, List<String> members) {

        ClassDescription classDescription = processClassDescription(classDefinition);

        MemberDescription m = classDescription;
        List<String> alist = new ArrayList<>();
        List<MemberDescription> items = new ArrayList<>();

        int method_count = 0, field_count = 0, constructor_count = 0, inner_count = 0, interfaces_count = 0;

        for (Object member : members) {

            String str = (String) member;

            if (parseFutureSpecific(str, classDescription)) {
                continue;
            }

            str = convertFutureSpecific(str, classDescription);

            if (str.startsWith(AnnotationItem.ANNOTATION_PREFIX) || str.startsWith(AnnotationItemEx.ANNOTATION_EX_PREFIX)) {
                alist.add(str);
            } else if (str.startsWith(ClassDescription.OUTER_PREFIX)) {
                processOuter(classDescription, str);
            } else if (str.startsWith(F40Format.HIDDEN_FIELDS)) {
                Set<String> internalFields = parseInternals(str);
                classDescription.setInternalFields(internalFields);
            } else if (str.startsWith(F40Format.HIDDEN_CLASSES)) {
                Set<String> internalClasses = parseInternals(str);
                classDescription.setInternalClasses(internalClasses);
            } else {
                appendAnnotations(m, alist);
                m = parse(str);
                MemberType mt = m.getMemberType();

                if (mt == MemberType.METHOD) {
                    method_count++;
                } else if (mt == MemberType.FIELD) {
                    field_count++;
                } else if (mt == MemberType.CONSTRUCTOR) {
                    constructor_count++;
                } else if (mt == MemberType.INNER) {
                    inner_count++;
                } else if (mt == MemberType.SUPERINTERFACE) {
                    interfaces_count++;
                }

                if (m != classDescription) {
                    items.add(m);
                }
            }
        }

        appendAnnotations(m, alist);

        if (constructor_count > 0) {
            classDescription.createConstructors(constructor_count);
        }
        if (method_count > 0) {
            classDescription.createMethods(method_count);
        }
        if (field_count > 0) {
            classDescription.createFields(field_count);
        }
        if (inner_count > 0) {
            classDescription.createNested(inner_count);
        }
        if (interfaces_count > 0) {
            classDescription.createInterfaces(interfaces_count);
        }

        constructor_count = 0;
        method_count = 0;
        field_count = 0;
        inner_count = 0;
        interfaces_count = 0;

        for (Object item : items) {
            m = (MemberDescription) item;
            MemberType mt = m.getMemberType();

            if (mt == MemberType.METHOD) {
                classDescription.setMethod(method_count, (MethodDescr) m);
                method_count++;
            } else if (mt == MemberType.FIELD) {
                classDescription.setField(field_count, (FieldDescr) m);
                field_count++;
            } else if (mt == MemberType.CONSTRUCTOR) {
                classDescription.setConstructor(constructor_count, (ConstructorDescr) m);
                ((ConstructorDescr) m).setupConstuctorName(classDescription.getQualifiedName());
                constructor_count++;
            } else if (mt == MemberType.INNER) {
                classDescription.setNested(inner_count, (InnerDescr) m);
                inner_count++;
            } else if (mt == MemberType.SUPERCLASS) {
                classDescription.setSuperClass((SuperClass) m);
            } else if (mt == MemberType.SUPERINTERFACE) {
                SuperInterface si = (SuperInterface) m;
                si.setDirect(true);
                classDescription.setInterface(interfaces_count, si);
                interfaces_count++;
            } else {
                assert false;
            }
        }

        return classDescription;
    }

    /*
     * This method can be overriden in subclasses
     */
    protected boolean parseFutureSpecific(String str, ClassDescription cl) {
        return false;
    }

    /*
     * This method can be overriden in subclasses
     */
    protected String convertFutureSpecific(String str, ClassDescription classDescription) {
        return str;
    }

    protected void processOuter(ClassDescription classDescription, String str) {
    }

    protected Set<String> parseInternals(String str) {

        Set<String> result = new HashSet<>();
        int startPos = str.indexOf(' ') + 1;
        int nextPos;
        do {
            nextPos = str.indexOf(',', startPos);
            String name;
            if (nextPos != -1) {
                name = str.substring(startPos, nextPos);
                startPos = nextPos + 1;
            } else {
                name = str.substring(startPos);
            }

            result.add(name);

        } while (nextPos != -1);

        return result;
    }

    protected void appendAnnotations(MemberDescription fid, List<String> alist) {
        if (alist.size() != 0) {

            AnnotationItem[] tmp = new AnnotationItem[alist.size()];
            AnnotationParser par = new AnnotationParser();

            for (int i = 0; i < alist.size(); ++i) {
                tmp[i] = par.parse(alist.get(i));
            }

            fid.setAnnoList(tmp);
            alist.clear();
        }
    }

    protected MemberDescription parse(String definition) {
        MemberDescription member = null;

        MemberType type = MemberType.getItemType(definition);

        if (type == MemberType.CLASS) {
            member = parse(new ClassDescription(), definition);
            currentClassName = member.getQualifiedName();
        } else if (type == MemberType.CONSTRUCTOR) {
            member = parse(new ConstructorDescr(), definition);
        } else if (type == MemberType.METHOD) {
            member = parse(new MethodDescr(), definition);
        } else if (type == MemberType.FIELD) {
            member = parse(new FieldDescr(), definition);
        } else if (type == MemberType.SUPERCLASS) {
            member = parse(new SuperClass(), definition);
        } else if (type == MemberType.SUPERINTERFACE) {
            member = parse(new SuperInterface(), definition);
        } else if (type == MemberType.INNER) {
            member = parse(new InnerDescr(), definition);
        } else {
            assert false;  // unknown member type
        }
        return member;
    }

    protected ClassDescription processClassDescription(String classDefinition) {
        ClassDescription classDescription = (ClassDescription) parse(classDefinition);
        return classDescription;
    }

    private void init(MemberDescription m, String def) {
        //System.out.println(def);
        line = def.trim();
        linesz = line.length();

        // skip member type
        idx = def.indexOf(' ');

        scanElems();
    }

    protected MemberDescription parse(ClassDescription cls, String def) {

        init(cls, def);

        cls.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        cls.setupGenericClassName(s);

        return cls;
    }

    protected MemberDescription parse(ConstructorDescr ctor, String def) {

        init(ctor, def);

        ctor.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();

        if (s != null && s.charAt(0) == '<' && !s.equals(ConstructorDescr.CONSTRUCTOR_NAME)) {
            ctor.setTypeParameters(s);
            s = getElem();
        }

        ctor.setupConstuctorName(s);

        s = getElem();
        if (s.charAt(0) != '(') {
            err();
        }

        if (!"()".equals(s)) {
            ctor.setArgs(s.substring(1, s.length() - 1));
        }

        if (elems.size() != 0) {
            s = getElem();
            if (!s.equals("throws")) {
                err();
            }
            s = getElem();
            ctor.setThrowables(s);
        }

        return ctor;
    }

    protected MemberDescription parse(MethodDescr method, String def) {

        init(method, def);

        method.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        if (s != null && s.charAt(0) == '<') {
            method.setTypeParameters(s);
            s = getElem();
        }

        method.setType(s);

        method.setupMemberName(getElem(), currentClassName);

        s = getElem();
        if (s.charAt(0) != '(') {
            err();
        }

        if (!"()".equals(s)) {
            method.setArgs(s.substring(1, s.length() - 1));
        }

        if (!elems.isEmpty() && elems.get(0).equals("throws")) {
            getElem(); // "throws"
            s = getElem();
            method.setThrowables(s);
        }

        if (!elems.isEmpty() && supportsValues() && elems.get(0).equals(VALUE.trim())) {
            int pos = line.indexOf(VALUE);
            if (pos >= 0) {
                method.setDefaultValue(line.substring(pos + VALUE.length()));
                elems.clear();
            }
        }

        if (!elems.isEmpty()) {
            err();
        }

        return method;
    }

    protected boolean supportsValues() {
        return false;
    }

    protected MemberDescription parse(FieldDescr field, String def) {

        init(field, def);

        field.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        field.setType(s);

        s = getElem();

        field.setupMemberName(s, currentClassName);

        if (elems.size() != 0) {
            s = getElem();
            if (!s.startsWith("=")) {
                err();
            }

            field.setConstantValue(s.substring(1).trim());
        }

        return field;
    }

    protected MemberDescription parse(SuperClass superCls, String def) {

        init(superCls, def);
        superCls.setModifiers(Modifier.scanModifiers(elems));
        int n = elems.size();
        if (n == 0) {
            err();
        }
        superCls.setupGenericClassName( elems.get(n - 1));

        return superCls;
    }

    protected MemberDescription parse(SuperInterface superIntf, String def) {

        init(superIntf, def);
        superIntf.setModifiers(Modifier.scanModifiers(elems));
        int n = elems.size();
        if (n == 0) {
            err();
        }
        superIntf.setupGenericClassName( elems.get(n - 1));

        return superIntf;
    }

    protected MemberDescription parse(InnerDescr inner, String def) {

        init(inner, def);

        inner.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        inner.setupInnerClassName(s, currentClassName);

        return inner;
    }

    private String getElem() {
        String s = null;

        if (elems.size() != 0) {
            s = elems.get(0);
            elems.remove(0);
        }

        if (s == null) {
            err();
        }

        return s;
    }

    private void scanElems() {
        elems = new LinkedList<>();

        for (;;) {

            //  skip leading blanks at the start of lexeme
            while (idx < linesz && (chr = line.charAt(idx)) == ' ') {
                idx++;
            }

            //  test for end of line
            if (idx >= linesz) {
                break;
            }

            //  store the start position of lexeme
            int pos = idx;

            if (chr == '=') {
                idx = linesz;
                elems.add(line.substring(pos));
                break;
            }

            if (chr == '(') {
                idx++;
                skip(')');
                idx++;
                elems.add(line.substring(pos, idx));
                continue;
            }

            if (chr == '<') {
                idx++;
                skip('>');
                idx++;
                elems.add(line.substring(pos, idx));
                continue;
            }

            idx++;
            while (idx < linesz) {
                chr = line.charAt(idx);

                if (chr == '<') {
                    idx++;
                    skip('>');
                    idx++;
                    continue;
                }

                if (chr == ' ' || chr == '(') {
                    break;
                }

                idx++;
            }
            elems.add(line.substring(pos, idx));
        }
    }

    private void skip(char term) {
        for (;;) {
            if (idx >= linesz) {
                err();
            }

            if ((chr = line.charAt(idx)) == term) {
                return;
            }

            if (chr == '(') {
                idx++;
                skip(')');
                idx++;
                continue;
            }

            if (chr == '<') {
                idx++;
                skip('>');
                idx++;
                continue;
            }

            idx++;
        }
    }

    private void err() {
        throw new Error(line);
    }
}
