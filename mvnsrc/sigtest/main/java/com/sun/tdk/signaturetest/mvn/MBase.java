/*
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
package com.sun.tdk.signaturetest.mvn;

import com.sun.tdk.signaturetest.SigTest;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.tdk.signaturetest.core.context.Option;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author Mikhail Ershov
 */
public abstract class MBase extends MSuperBase {

    /**
     * @parameter
     */
    protected List pathElements;

    /**
     * @parameter
     */
    protected List packages;

    /**
     * @parameter
     */
    protected List excludes;

    /**
     * @parameter
     */
    protected String fileName;

    /**
     * @parameter
     */
    protected String apiVersion;

    @Override
    protected void dumpMe() {
        dump(MBase.class, this);
        super.dumpMe();
    }

    void createBaseParameters(ArrayList params) {
        params.add(SigTest.FILENAME_OPTION);
        params.add(fileName);

        params.add( Option.CLASSPATH.getKey());
        if (pathElements != null) {
            StringBuffer cpb = new StringBuffer();
            for (int i = 0; i < pathElements.size(); i++) {
                cpb.append(pathElements.get(i));
                if (i != pathElements.size() - 1) {
                    cpb.append(File.pathSeparatorChar);
                }
            }
            params.add(cpb.toString());
        }

        if (apiVersion != null && !"".equals(apiVersion)) {
            params.add(SigTest.APIVERSION_OPTION);
            params.add(apiVersion);
        }

        Iterator it;
        if (packages != null) {
            it = packages.iterator();
            while (it.hasNext()) {
                params.add(Option.PACKAGE.getKey());
                params.add(it.next());
            }
        }

        if (excludes != null) {
            it = excludes.iterator();
            while (it.hasNext()) {
                params.add(Option.EXCLUDE.getKey());
                params.add(it.next());
            }
        }
    }

    protected void checkParams() throws MojoExecutionException {
        // classpath
        if (pathElements == null || pathElements.size() == 0) {
            throw new MojoExecutionException("Classpath is not specified");
        }

        // package
        if (packages == null || packages.size() == 0) {
            throw new MojoExecutionException("Packag[es] are not specified");
        }

        // filename
        if (fileName == null || "".equals(fileName)) {
            throw new MojoExecutionException("Filename[s] are not specified");
        }
    }

}
