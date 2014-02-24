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
package com.sun.tdk.signaturetest.mvn;

import com.sun.tdk.signaturetest.Merge;
import com.sun.tdk.signaturetest.Result;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Maven 2 plugin - performs merge
 *
 * @goal merge
 * @phase test
 */
public class MMerge extends MBase {

    /**
     * @parameter @required
     */
    protected List files;

    /**
     * @parameter @required
     */
    protected String write;

    /**
     * @parameter default-value="false"
     */
    protected boolean binary;

    public void execute() throws MojoExecutionException, MojoFailureException {
        checkParams();
        Merge m = new Merge();
        System.setProperty(Result.NO_EXIT, "true");
        m.run(createParams(), new MLogAdapter(getLog()), null);
        if (negative ? m.isPassed() : !m.isPassed()) {
            if (failOnError) {
                throw new MojoExecutionException(m.toString());
            } else {
                getLog().error(m.toString());
            }
        }
    }

    private String[] createParams() {
        ArrayList params = new ArrayList();

        params.add(Merge.FILES_OPTION);

        if (files != null) {
            StringBuffer cpb = new StringBuffer();
            for (int i = 0; i < files.size(); i++) {
                cpb.append(files.get(i));
                if (i != files.size() - 1) {
                    cpb.append(File.pathSeparatorChar);
                }
            }
            params.add(cpb.toString());
        }

        params.add(Merge.WRITE_OPTION);

        params.add(write);

        if (binary) {
            params.add(Merge.BINARY_OPTION);
        }

        return (String[]) params.toArray(new String[]{});
    }

    @Override
    protected void checkParams() throws MojoExecutionException {
        if (files == null || files.size() == 0) {
            throw new MojoExecutionException("Files are not specified");
        }

        // write
        if (write == null || "".equals(write)) {
            throw new MojoExecutionException("Output file is not specified");
        }
    }

}
