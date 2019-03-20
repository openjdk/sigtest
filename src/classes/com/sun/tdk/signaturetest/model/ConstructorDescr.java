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
package com.sun.tdk.signaturetest.model;

import com.sun.tdk.signaturetest.util.SwissKnife;

import java.util.Set;
import java.util.StringTokenizer;

public final class ConstructorDescr extends MemberDescription {

    public static final ConstructorDescr[] EMPTY_ARRAY = new ConstructorDescr[0];
    public static final String CONSTRUCTOR_NAME = "<init>";

    public ConstructorDescr() {
        super(MemberType.CONSTRUCTOR, MEMBER_DELIMITER);
    }

    public ConstructorDescr(ClassDescription clazz, int modifiers) {
        super(MemberType.CONSTRUCTOR, MEMBER_DELIMITER);
        setupConstuctorName(clazz.getQualifiedName());
        setModifiers(modifiers);
    }

    // for reflection
    public ConstructorDescr(Class<?> clazz, int modifiers) {
        super(MemberType.CONSTRUCTOR, MEMBER_DELIMITER);
        setupConstuctorName(clazz.getName());
        setModifiers(modifiers);
    }

    public void setupConstuctorName(String clName) {

        this.declaringClass = clName.intern();
        this.name = CONSTRUCTOR_NAME;
    }

    // NOTE: Change this method carefully if you changed the code,
    // please, update the method isCompatible() in order it works as previously
    public boolean equals(Object o) {
        if (!(o instanceof ConstructorDescr)) {
            return false;
        }

        ConstructorDescr ctor = (ConstructorDescr) o;

        // == used instead of equals() because name is always assigned via String.intern() call
        return name == ctor.name && SwissKnife.equals(typeParameters, ctor.typeParameters)
                && args.equals(ctor.args);
    }

    public int hashCode() {
        return name.hashCode() + args.hashCode() + ((typeParameters != null) ? typeParameters.hashCode() : 0);
    }

    public boolean isCompatible(MemberDescription m) {

        if (!equals(m)) {
            throw new IllegalArgumentException("Only equal members can be checked for compatibility!");
        }

        return memberType.isCompatible(getModifiers(), m.getModifiers())
                && throwables.equals(m.throwables);
    }

    public boolean isConstructor() {
        return true;
    }

    public String toString() {

        StringBuffer buf = new StringBuffer();

        buf.append("constructor");

        String modifiers = Modifier.toString(memberType, getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        if (typeParameters != null) {
            buf.append(' ');
            buf.append(typeParameters);
        }

        buf.append(' ');
        if (!NO_DECLARING_CLASS.equals(declaringClass)) {
            buf.append(declaringClass);
            buf.append(delimiter);
        }
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

        return declaringClass +
                delimiter +
                name +
                '(' +
                args +
                ')';
    }

    protected void populateDependences(Set<String> set) {
        StringTokenizer st = new StringTokenizer(args, ARGS_DELIMITER);
        while (st.hasMoreTokens()) {
            addDependency(set, st.nextToken());
        }

        st = new StringTokenizer(throwables, THROWS_DELIMITER);
        while (st.hasMoreTokens()) {
            addDependency(set, st.nextToken());
        }
    }
}
