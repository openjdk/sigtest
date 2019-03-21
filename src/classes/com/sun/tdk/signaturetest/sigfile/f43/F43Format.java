/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tdk.signaturetest.sigfile.f43;

import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.Reader;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.sigfile.f42.F42Format;

public class F43Format extends F42Format {

    static final String MODULE = "module";
    static final String NAME = "name";
    static final String FEATURES = "features";
    static final String VERSION = "version";
    static final String MAIN_CLASS = "main-class";
    static final String PACKAGE = "package";
    static final String EXPORTS = "exports";
    static final String SOURCE = "source";
    static final String TARGET = "target";
    static final String REQUIRES = "requires";
    static final String TRUE = "true";
    static final String PROVIDES = "provides";
    static final String SERVICE = "service";
    static final String PROVIDER = "provider";
    static final String USES = "uses";


    public F43Format() {
        addSupportedFeature(FeaturesHolder.ModuleInfo);
        addSupportedFeature(FeaturesHolder.AnnDevVal);
    }

    public Reader getReader() {
        return new F43Reader(this);
    }

    public Writer getWriter() {
        return new F43Writer();
    }

    public String getVersion() {
        return "#Signature file v4.3";
    }


}
