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

import com.sun.tdk.signaturetest.Result;
import com.sun.tdk.signaturetest.Setup;
import java.util.ArrayList;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Maven 2 plugin - performs setup
 *
 * @goal setup
 * @phase test
 */
public class MSetup extends MBase {

    /**
     * @parameter default-value="false"
     */
    protected boolean nonClosedFile;

    public void execute() throws MojoExecutionException, MojoFailureException {
        dumpMe();
        checkParams();
        Setup s = new Setup();
        System.setProperty(Result.NO_EXIT, "true");
        s.run(createParams(), new MLogAdapter(getLog()), null);
        if (negative ? s.isPassed() : !s.isPassed()) {
            if (failOnError) {
                throw new MojoExecutionException(s.toString());
            } else {
                getLog().error(s.toString());
            }
        }
    }

    private String[] createParams() {
        ArrayList params = new ArrayList();
        createBaseParameters(params);
        if (nonClosedFile) {
            params.add(Setup.NONCLOSEDFILE_OPTION);
        }
        return (String[]) params.toArray(new String[]{});
    }

    @Override
    protected void dumpMe() {
        getLog().debug("*** Signature test setup ***");
        dump(MSetup.class, this);
        super.dumpMe();
    }
}
