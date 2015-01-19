/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.core.context;

/*
 * @author Mikhail Ershov
 */
public enum Option {

    X_JIMAGE("-XJImage", Kind.SINGLE_OPT),
    X_JAKE("-XJake", Kind.NONE),

    DEBUG("-Debug", Kind.NONE),
    HELP("-Help", "-?", Kind.INSTEAD_OF_ANY),
    PACKAGE("-Package", Kind.MANY_OPT),

    PURE_PACKAGE("-PackageWithoutSubpackages", Kind.MANY_OPT),
    EXCLUDE("-Exclude", Kind.MANY_OPT),
    API_INCLUDE("-apiInclude", Kind.MANY_OPT),
    API_EXCLUDE("-apiExclude", Kind.MANY_OPT),
    CLASSPATH("-Classpath", Kind.SINGLE_OPT),
    ALL_PUBLIC("-AllPublic", Kind.NONE),
    STATIC("-Static", Kind.NONE),

    FILES("-Files", Kind.REQ_LIST), // merge's
    WRITE("-Write", Kind.SINGLE_OPT), // merge's
    BINARY("-Binary", Kind.NONE),  // merge's
    TESTURL("-TestURL", Kind.SINGLE_OPT),
    VERSION("-Version", "-V", Kind.INSTEAD_OF_ANY),

    // APICOV
    API("-api", Kind.SINGLE_OPT),
    TS("-ts" , Kind.SINGLE_REQ),
    TS_ICNLUDE("-tsInclude", Kind.MANY_OPT),
    TS_ICNLUDEW("-tsIncludeW", Kind.MANY_OPT),
    TS_EXCLUDE("-tsExclude", Kind.MANY_OPT),
    API_INCLUDEW("-apiIncludeW", Kind.MANY_OPT),

    FILTERMAP("-FilterMap", Kind.MANY_OPT),
    FILTERSIG("-FilterSig", Kind.MANY_OPT),


    EXCLUDE_INTERFACES("-excludeInterfaces", Kind.NONE),
    EXCLUDE_ABSTRACT_CLASSES("-excludeAbstractClasses", Kind.NONE),
    EXCLUDE_ABSTRACT_METHODS("-excludeAbstractMethods", Kind.NONE),
    EXCLUDE_FIELDS("-excludeFields", Kind.NONE),
    INCLUDE_CONSTANT_FIELDS("-includeConstantFields", Kind.NONE),

    MODE("-mode", Kind.SINGLE_OPT),
    DETAIL("-detail", Kind.SINGLE_OPT),
    FORMAT("-format", Kind.SINGLE_OPT),
    REPORT("-report", Kind.SINGLE_OPT),
    EXCLUDE_LIST("-excludeList", Kind.MANY_OPT),

    FORMATPLAIN("-FormatPlain", Kind.NONE),
    FORMATHUMAN("-FormatHuman", "-H", Kind.NONE),
    BACKWARD("-Backward", "-B", Kind.NONE),

    ;

    private String key;
    private Kind kind;
    private String alias;
    private Option(String key, Kind kind) {
        this.key = key;
        this.kind = kind;
    }

    private Option(String key, String alias, Kind kind) {
        this.key = key;
        this.kind = kind;
        this.alias = alias;
    }

    public String getKey() {
        return key;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean hasAlias() {
        return alias != null;
    }

    public String getAlias() {
        assert alias != null;
        return alias;
    }

    public boolean accept(String optionName) {
        assert optionName != null;
        if (optionName.equalsIgnoreCase(key)) {
            return true;
        }
        return hasAlias() && getAlias().equalsIgnoreCase(optionName);
    }

    public static Option byKey(String key) {
        assert key != null;
        for (Option o : Option.values()) {
            if (key.equalsIgnoreCase(o.getKey()) || ( o.hasAlias() && key.equalsIgnoreCase(o.getAlias()))) {
                return o;
            }
        }
        return null;
    }

    public enum Kind {
        NONE, SINGLE_OPT, SINGLE_REQ, MANY_OPT, REQ_LIST, INSTEAD_OF_ANY;
    }

}
