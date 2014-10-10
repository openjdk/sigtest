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
 *
 */
package com.sun.tdk.apicover.markup;

import com.sun.tdk.signaturetest.model.AnnotationItem;
import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.model.Modifier;
import com.sun.tdk.signaturetest.plugin.FormatAdapter;
import com.sun.tdk.signaturetest.plugin.ReaderAdapter;
import com.sun.tdk.signaturetest.sigfile.FileManager;

/**
 * @author Mikhail Ershov
 */
public class Adapter {

    public Adapter(FileManager fm) {

        // allow this for any type of elements
        for (MemberType mt : MemberType.knownTypes) {
            mt.setModifiersMask(mt.getModifiersMask() | coverIgnore.getValue());
        }
        // add format
        FormatAdapter f = new FormatAdapter("#APICover file v4.1");
        f.setReader(new ReaderAdapter(f) {
            boolean covOn = false;

            protected String preprocessLine(String currentLine) {

                if (currentLine != null) {
                    String st = currentLine.trim();
                    if (st.equals("#coverage off")) {
                        covOn = false;
                    } else if (st.equals("#coverage on")) {
                        covOn = true;
                    } else if (!"".equals(st) && !covOn && !st.startsWith("#")) {
                        if (!st.startsWith(AnnotationItem.ANNOTATION_PREFIX)
                                && !st.startsWith(FormatAdapter.HIDDEN_FIELDS)
                                && !st.startsWith(FormatAdapter.HIDDEN_CLASSES)) {
                            currentLine = currentLine.replaceFirst(" ", " " + coverIgnore.toString() + " ");
                        }
                    }
                }
                return super.preprocessLine(currentLine);
            }
        });
        fm.addFormat(f, false);
    }
    // pseudo-modifier
    public static final Modifier coverIgnore = new Modifier("!cover-ignore", true);
}
