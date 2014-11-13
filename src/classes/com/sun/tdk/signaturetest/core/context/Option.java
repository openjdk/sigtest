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
    DEBUG("-Debug", Kind.NONE),
    HELP("-Help", "-?", Kind.NONE),
    PACKAGE("-Package", Kind.MANY_OPT),

    PURE_PACKAGE("-PackageWithoutSubpackages", Kind.MANY_OPT),
    EXCLUDE("-Exclude", Kind.MANY_OPT),
    API_INCLUDE("-ApiInclude", Kind.MANY_OPT),
    API_EXCLUDE("-ApiExclude", Kind.MANY_OPT),
    CLASSPATH("-Classpath", Kind.SINGLE_OPT),
    ALL_PUBLIC("-AllPublic", Kind.NONE),
    STATIC("-Static", Kind.NONE),

    FILES("-Files", Kind.REQ_LIST), // merge's
    WRITE("-Write", Kind.SINGLE_OPT), // merge's
    BINARY("-Binary", Kind.NONE),  // merge's
    TESTURL("-TestURL", Kind.SINGLE_OPT),
    VERSION("-Version", "-V", Kind.NONE)
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

    private boolean hasAlias() {
        return alias != null;
    }

    private String getAlias() {
        assert alias != null;
        return alias;
    }

    public boolean accept(String optionName) {
        assert optionName != null;
        if (optionName.equalsIgnoreCase(key)) {
            return true;
        }
        return hasAlias() && getAlias().equalsIgnoreCase(key);
    }


    public enum Kind {
        NONE, SINGLE_OPT, SINGLE_REQ, MANY_OPT, REQ_LIST;
    }

}
