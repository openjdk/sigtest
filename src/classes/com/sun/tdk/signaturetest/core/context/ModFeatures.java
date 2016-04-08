/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;
import java.util.Set;

/*
Features for recording and for checking
 */
public enum ModFeatures {

    /*
    checks module existence
     */
    AVAILABILITY,

    /*
    the module's version
     */
    VERSION,

    /*
    the module's main class
     */
    MAIN_CLASS,

    /*
     the names of all the packages defined in this module, whether exported or concealed
     */
    PACKAGES,

    /*
    the names of the packages defined in, but not exported by, this module
     */
    CONCEAL,

    /*
    Not targeted ("public") module's exports
     */
    EXPORTS_PUBLIC,

    /*
    All the module exports (including public)
     */
    EXPORTS_ALL,

    /*
    The "public" dependences of this module.
     */
    REQUIRES_PUBLIC,

    /*
    The all dependences of this module (including public).
     */
    REQUIRES_ALL,

    /*
    The services provided by the module
     */
    SERVICES,

    /*
    The service dependences of this module
     */
    USES,

    /*
    All of above features
     */
    ALL;

    /**
     * @param fList is comma separated feature list,
     *              values are ModSetupFeatures's names (case insensitive)
     * @return Set of specified features.
     * If fList is null or the set contains ALL it returns {MODULE_LIST, REQUIRES_PUBLIC, EXPORTS_PUBLIC}
     * @throws IllegalArgumentException in case of wrong feature name
     *                                  with name in exception message
     */
    public static EnumSet<ModFeatures> featureSetFromCommaList(String fList) throws IllegalArgumentException {
        EnumSet<ModFeatures> res = EnumSet.noneOf(ModFeatures.class);
        if (fList != null) {
            for (String f : fList.split(",")) {
                f = f.trim().toUpperCase();
                if (!f.isEmpty()) {
                    try {
                        res.add(ModFeatures.valueOf(f));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(f);
                    }
                }
            }
        }
        if (res.isEmpty()) {
            return EnumSet.of(AVAILABILITY, REQUIRES_PUBLIC, EXPORTS_PUBLIC);
        }
        return res;
    }

    public static String commaListFromFeatureSet(Set<ModFeatures> set) {
        if (set.isEmpty() || set.contains(ALL)) {
            return ALL.name();
        }

        // no lambdas here!!!
        StringBuilder sb = new StringBuilder();
        for (ModFeatures f : set) {
            sb.append(f.name()).append(',');
        }
        sb.deleteCharAt(sb.length() - 1); //delete last comma

        return sb.toString();

    }

}
