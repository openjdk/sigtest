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

import com.sun.tdk.signaturetest.Result;
import com.sun.tdk.signaturetest.SigTest;
import com.sun.tdk.signaturetest.SignatureTest;
import java.util.ArrayList;

import com.sun.tdk.signaturetest.core.context.Option;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * <pre>
 * Maven 2 wrapper for test command
 *
 * Required parameters:
 *   pathelements
 *      corresponds to -classpath option
 *   fileName
 *      corresponds to -filename option
 *   packages
 *      corresponds to -package option
 *
 * Optional parameters:
 *   "failonerror" - Stop the build process if the command exits with an error. Default is "false".
 *   "apiVersion" -  corresponds to -apiVersion. Set API version for signature file
 *   "backward" - corresponds to -Backward option. Performs backward compatibility checking.
 *      Default is "false".
 *   "binary" - corresponds to "-mode bin" option. Turns on binary mode. Default is "false".
 *   "errorAll" - corresponds to "-errorAll" option. Handles warnings as errors. Default is "false".
 *   "debug" - corresponds to "-debug" option, prints debug information. Default is "false".
 *   "formatHuman" - corresponds to "-formatHuman" option, processes human readable error output.
 *     Default is "false".
 *   "output" - corresponds to "-out filename" option, specifies report file name
 *   "negative" - inverts result (that is passed status treats as faild and vice versa, default is "false"
 *   "exclude" attribute or nested "exclude" element. Corresponds to -exclude option.
 *
 * </pre>
 *
 * @goal test
 * @phase test
 */
public class MTest extends MBase {

    /**
     * @parameter default-value="false"
     */
    protected boolean binary = false;
    /**
     * @parameter default-value="false"
     */
    protected boolean backward = false;
    /**
     * @parameter default-value="false"
     */
    protected boolean human = false;
    /**
     * @parameter
     */
    protected String out;
    /**
     * @parameter default-value="false"
     */
    protected boolean debug = false;
    /**
     * @parameter default-value="false"
     */
    protected boolean errorAll = false;

    // ------------------------------------------
    public void execute() throws MojoExecutionException, MojoFailureException {
        dumpMe();
        checkParams();
        SignatureTest s = testFactory();
        String oldExit = System.setProperty(Result.NO_EXIT, "true");
        s.run(createParams(), new MLogAdapter(getLog()), null);
        if (negative ? s.isPassed() : !s.isPassed()) {
            if (failOnError) {
                throw new MojoExecutionException(s.toString());
            } else {
                getLog().error(s.toString());
            }
        }
        if (oldExit != null) {
            System.setProperty(Result.NO_EXIT, oldExit);
        }

    }

    // APICheck overrides it
    protected SignatureTest testFactory() {
        return new SignatureTest();
    }

    private String[] createParams() {
        ArrayList params = new ArrayList();
        createBaseParameters(params);
        params.add(SigTest.STATIC_OPTION);
        if (binary) {
            params.add(SignatureTest.MODE_OPTION);
            params.add(SignatureTest.BINARY_MODE);
        }
        if (backward) {
            params.add(SigTest.BACKWARD_OPTION);
        } else if (human) {
            params.add(SigTest.FORMATHUMAN_OPTION);
        }

        if (out != null && out.length() > 0) {
            params.add(SigTest.OUT_OPTION);
            params.add(out);
        }

        if (debug) {
            params.add(Option.DEBUG.getKey());
        }
        if (errorAll) {
            params.add(SigTest.ERRORALL_OPTION);
        }

        return (String[]) params.toArray(new String[]{});
    }

    @Override
    protected void dumpMe() {
        getLog().debug("*** Signature test ***");
        dump(MTest.class, this);
        super.dumpMe();
    }
}
