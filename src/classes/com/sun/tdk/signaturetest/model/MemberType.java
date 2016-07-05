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

import java.io.Serializable;

/**
 * @author Roman Makarchuk
 */
public final class MemberType implements Comparable, Serializable {

    /**
     * NOTE: changing order of modifiers in these arrays affects order of
     * modifiers in string returned via method <code>Modifier.toString()</code>
     *
     * @see Modifier#toString(MemberType,int,boolean)
     */
    // Classes including nested and interfaces
    private static final Modifier[] classModifiers = {
        Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE,
        Modifier.ABSTRACT, Modifier.FINAL, Modifier.INTERFACE,
        Modifier.STATIC, Modifier.ANNOTATION, Modifier.ENUM, Modifier.ACC_STRICT,
        Modifier.ACC_SUPER, Modifier.ACC_SYNTHETIC, Modifier.ACC_MODULE
    };
    private static final Modifier[] constructorModifiers = {
        Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE,
        Modifier.VARARGS, Modifier.ACC_SYNTHETIC, Modifier.ACC_STRICT
    };
    private static final Modifier[] methodModifiers = {
        Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.ABSTRACT,
        Modifier.NATIVE, Modifier.VARARGS, Modifier.BRIDGE, Modifier.FINAL, Modifier.HASDEFAULT,
        Modifier.STATIC, Modifier.SYNCHRONIZED, Modifier.ACC_SYNTHETIC, Modifier.ACC_STRICT
    };
    private static final Modifier[] fieldModifiers = {
        Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.FINAL,
        Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE,
        Modifier.FIELD_ENUM, Modifier.ACC_SYNTHETIC
    };
    private static final Modifier[] noneModifiers = {};
    public static final MemberType CLASS = new MemberType("CLSS", classModifiers);
    public static final MemberType INNER = new MemberType("innr", classModifiers);
    public static final MemberType SUPERCLASS = new MemberType("supr", noneModifiers);
    public static final MemberType SUPERINTERFACE = new MemberType("intf", noneModifiers);
    public static final MemberType CONSTRUCTOR = new MemberType("cons", constructorModifiers);
    public static final MemberType METHOD = new MemberType("meth", methodModifiers);
    public static final MemberType FIELD = new MemberType("fld", fieldModifiers);
    public static final MemberType MODULE = new MemberType("module", noneModifiers);


    public String toString() {
        return name;
    }

    public static MemberType getItemType(String def) {
        for (int i = 0; i < knownTypes.length; ++i) {
            if (def.startsWith(knownTypes[i].name)) {
                return knownTypes[i];
            }
        }
        return null;
    }

    Modifier[] getApplicableModifiers() {
        return applicableModifiers;
    }

    public int getModifiersMask() {
        return modifiersMask;
    }

    public void setModifiersMask(int m) {
        modifiersMask = m;
    }

    boolean isCompatible(int m1, int m2) {
        return (m1 & trackedModifiersMask) == (m2 & trackedModifiersMask);
    }

    private MemberType(String memberType, Modifier[] applicableModifiers) {
        this.name = memberType;
        this.applicableModifiers = applicableModifiers;

        modifiersMask = 0;
        for (int i = 0; i < applicableModifiers.length; ++i) {
            // check that modifiers don't conflict with each other
            Modifier m = applicableModifiers[i];

//          assertion commented because Java 1.3 does not support them
//            assert !Modifier.hasModifier(modifiersMask, m);
            modifiersMask = Modifier.addModifier(modifiersMask, m);
        }

        updateTrackedModifiersMask();
    }

    private void updateTrackedModifiersMask() {
        trackedModifiersMask = 0;
        for (int i = 0; i < applicableModifiers.length; ++i) {
            Modifier m = applicableModifiers[i];
            if (m.isTracked()) {
                trackedModifiersMask = Modifier.addModifier(trackedModifiersMask, m);
            }
        }
    }

    public static void setMode(boolean binary) {
        // track vararg modifier only in source mode; its absence does not break binary compatibility
        Modifier.VARARGS.setTracked(!binary);

        for (int i = 0; i < knownTypes.length; ++i) {
            knownTypes[i].updateTrackedModifiersMask();
        }
    }

    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (name != null ? name.hashCode() : 0);
        return hash;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MemberType other = (MemberType) obj;
        return true;
    }

    public int compareTo(Object o) {
        MemberType other = (MemberType) o;
        return name.compareTo(other.name);
    }

    private final String name;
    private final Modifier[] applicableModifiers;
    private int modifiersMask;
    private int trackedModifiersMask;     // used to clean non-tracked modifiers quickly
    public static final MemberType[] knownTypes = {CLASS, SUPERCLASS, SUPERINTERFACE, CONSTRUCTOR, METHOD, FIELD, INNER, MODULE};
}
