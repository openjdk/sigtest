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
package com.sun.tdk.signaturetest.model;

import com.sun.tdk.signaturetest.core.PrimitiveTypes;
import com.sun.tdk.signaturetest.util.SwissKnife;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author Roman Makarchuk
 */
public final class MethodDescr extends MemberDescription {

    public static final MethodDescr[] EMPTY_ARRAY = new MethodDescr[0];

    public MethodDescr() {
        super(MemberType.METHOD, MEMBER_DELIMITER);
    }

    public MethodDescr(String methodName, String className, int modifiers) {
        super(MemberType.METHOD, MEMBER_DELIMITER);
        setupMemberName(methodName, className);
        setModifiers(modifiers);
    }

    // NOTE: Change this method carefully if you changed the code,
    // please, update the method isCompatible() in order it works as previously
    public boolean equals(Object o) {
        if (!(o instanceof MethodDescr)) {
            return false;
        }

        MethodDescr method = (MethodDescr) o;

        // == used instead of equals() because name is always assigned via String.intern() call
        return name == method.name && args.equals(method.args)
                && SwissKnife.equals(typeParameters, method.typeParameters);
    }

    public int hashCode() {
        return name.hashCode() + args.hashCode() + ((typeParameters != null) ? typeParameters.hashCode() : 0);
    }

    public boolean isCompatible(MemberDescription m) {

        if (!equals(m)) {
            throw new IllegalArgumentException("Only equal members can be checked for compatibility!");
        }

        return memberType.isCompatible(getModifiers(), m.getModifiers()) && type.equals(m.type)
                && throwables.equals(m.throwables);
    }

    public boolean isMethod() {
        return true;
    }

    public String toString() {

        StringBuffer buf = new StringBuffer();

        buf.append("method");

        String modifiers = Modifier.toString(memberType, getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        if (typeParameters != null) {
            buf.append(' ');
            buf.append(typeParameters);
        }

        if (type.length() != 0) {
            buf.append(' ');
            buf.append(type);
        }

        buf.append(' ');
        buf.append(declaringClass);
        buf.append(delimiter);
        buf.append(name);
        buf.append('(');
        buf.append(args);
        buf.append(')');

        if (throwables.length() > 0) {
            buf.append(" throws ");
            buf.append(throwables);
        }

        AnnotationItem[] annoList = getAnnoList();
        for (AnnotationItem annotationItem : annoList) {
            buf.append("\n ");
            buf.append(annotationItem);
        }

        return buf.toString();
    }

    public String getSignature() {

        StringBuffer buf = new StringBuffer();

        buf.append(name);
        buf.append('(');
        buf.append(args);
        buf.append(')');

        return buf.toString();
    }
    //  Default value for annotation member or null;
    private Object annoDef;

    public Object getAnnoDef() {
        return annoDef;
    }

    public void setAnnoDef(Object annodef) {
        this.annoDef = annodef;
    }

    protected void populateDependences(Set set) {
        addDependency(set, type);
        StringTokenizer st = new StringTokenizer(args, ARGS_DELIMITER);
        while (st.hasMoreTokens()) {
            addDependency(set, st.nextToken());
        }

        st = new StringTokenizer(throwables, THROWS_DELIMITER);
        while (st.hasMoreTokens()) {
            addDependency(set, st.nextToken());
        }
    }

    public void setDefaultValue(String defValueAsString) {
        setAnnoDef(PrimitiveTypes.stringToSimpleObject(defValueAsString));
    }
}
