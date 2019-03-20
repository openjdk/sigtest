/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.plugin;

import com.sun.tdk.signaturetest.sigfile.Reader;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.sigfile.f43.F43Format;

/**
 * @author Mikhail Ershov
 */
public class FormatAdapter extends F43Format {

    private final String signature;
    protected Reader reader;
    protected Writer writer;

    public FormatAdapter(String signature) {
        this.signature = signature;
        init();
    }

    public String getVersion() {
        return signature;
    }

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

    public void setReader(Reader r) {
        reader = r;
    }

    public void setWriter(Writer w) {
        writer = w;
    }

    protected void init() {
        reader = new ReaderAdapter(this);
        writer = new WriterAdapter(this);
    }

    // extend access level from protected to public
    public void addSupportedFeature(Feature feature) {
        super.addSupportedFeature(feature);
    }

    // extend access level from protected to public
    public void removeSupportedFeature(Feature feature) {
        super.removeSupportedFeature(feature);
    }
}
