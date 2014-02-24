/*
 * $Id$
 *
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.apicheck;

import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.loaders.LoadingHints;
import com.sun.tdk.signaturetest.model.ClassDescription;

public class CombinedLoader implements ClassDescriptionLoader, LoadingHints {

    private ClassDescriptionLoader mainLoader;
    private ClassDescriptionLoader secondLoader;

    CombinedLoader(ClassDescriptionLoader main, ClassDescriptionLoader second) {
        this.mainLoader = main;
        this.secondLoader = second;
    }

    public ClassDescription load(String className) throws ClassNotFoundException {
        ClassDescription result;
        try {
            result = mainLoader.load(className);
        } catch (ClassNotFoundException cnf) {
            return secondLoader.load(className);
        }
        return result;
    }

    public void addLoadingHint(Hint hint) {
        if (mainLoader instanceof LoadingHints) {
            LoadingHints h = (LoadingHints) mainLoader;
            h.addLoadingHint(hint);
        }
    }
}
