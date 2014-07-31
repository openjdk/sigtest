/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcovfilter;

import com.sun.tdk.signaturetest.core.ClassHierarchy;
import com.sun.tdk.signaturetest.model.AnnotationItem;
import com.sun.tdk.signaturetest.util.BatchFileParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.SwissKnife;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

public class Setup extends com.sun.tdk.signaturetest.Setup {

    private HashSet<String> excludes = new HashSet<String>();
    private HashSet<String> includes = new HashSet<String>();
    private HashSet<String> apiInclude = new HashSet<String>();
    private HashSet<String> apiExclude = new HashSet<String>();

    public static void main(String[] args) {
        Setup t = new Setup();
        new Util().init(t, t.excludes, t.includes, t.apiInclude, t.apiExclude);
        long start = System.currentTimeMillis();
        t.run(args, new PrintWriter(System.err, true), null);
        long time = (System.currentTimeMillis() - start) / 1000;
        System.out.println("Elasped time " + time + " sec.");
        t.exit();
    }

    @Override
    protected boolean parseParameters(String[] args) {
        try {
            args = BatchFileParser.processParameters(args);
        } catch (CommandLineParserException ex) {
            SwissKnife.reportThrowable(ex);
        }
        ArrayList<String> rest = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            String par = args[i];
            if (par.equalsIgnoreCase("-excludesig")) {
                excludes.add(args[++i]);
            } else if (par.equalsIgnoreCase("-includesig")) {
                includes.add(args[++i]);
            } else if (par.equalsIgnoreCase("-apiinclude")) {
                apiInclude.add(args[++i]);
            } else if (par.equalsIgnoreCase("-apiexclude")) {
                apiExclude.add(args[++i]);
            } else {
                rest.add(par);
            }
        }
        // by default API is java and javax packages
        if (apiInclude.isEmpty()) {
            apiInclude.add("java");
            apiInclude.add("javax");
        }
        return super.parseParameters(rest.toArray(new String[]{}));
    }

    @Override
    protected String getComponentName() {
        return "JCov filter setup";
    }

    @Override
    protected boolean addInherited() {
        return false;
    }

    @Override
    protected AnnotationItem[] removeUndocumentedAnnotations(AnnotationItem[] annotations, ClassHierarchy h) {
        return annotations;
    }

}
