/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.ant;

import com.sun.tdk.signaturetest.Merge;
import com.sun.tdk.signaturetest.Result;
import com.sun.tdk.signaturetest.core.context.Option;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <pre>
 * Ant wrapper for merge command
 * Required parameters:
 *   nested "file" element defines input files
 *     Corresponds to -files option
 *     Samples -
 *     &lt;merge ...
 *       &lt;file name="a1.sig" /&gt;
 *       &lt;file name="a2.sig" /&gt;
 *       ...
 *     &lt;/merge&gt;
 *   "classpath" attribute or nested "classpath" element is required.
 *     Corresponds to -classpath option
 *
 *   "write" attribute is required. Specifies output file name
 *     Corresponds to -write option
 *
 * Optional parameters:
 *   "binary" - Sets binary merge mode. Corresponds to "-Binary" option. Default is false.
 *   "failonerror" - Stop the build process if the command exits with an error. Default is "false".
 *   "negative" - inverts result (that is passed status treats as failed and vice versa, default is "false"
 * </pre>
 *
 * @author Mikhail Ershov
 */
public class AMerge extends ASuperBase {

    private final List<AFile> files = new ArrayList<>();
    private String write;
    private boolean binary = false;

    public void execute() throws BuildException {
        checkParams();
        Merge m = new Merge();
        System.setProperty(Result.NO_EXIT, "true");
        m.run(createParams(), new PrintWriter(System.out, true), null);
        if (negative == m.isPassed()) {
            if (failOnError) {
                throw new BuildException(m.toString());
            } else {
                getProject().log(m.toString(), Project.MSG_ERR);
            }
        }
    }

    private String[] createParams() {
        ArrayList<String> params = new ArrayList<>();

        params.add(Option.FILES.getKey());

        Iterator<AFile> it = files.iterator();
        StringBuffer filesValues = new StringBuffer();
        while (it.hasNext()) {
            AFile af = it.next();
            filesValues.append(af.value);
            if (it.hasNext()) {
                filesValues.append(File.pathSeparatorChar);
            }
        }
        params.add(filesValues.toString());

        params.add(Option.WRITE.getKey());

        params.add(write);

        if (binary) {
            params.add(Option.BINARY.getKey());
        }

        return params.toArray(new String[]{});
    }

    private void checkParams() throws BuildException {
        if (files.isEmpty()) {
            throw new BuildException("Files are not specified");
        }

        // write
        if (write == null || write.isEmpty()) {
            throw new BuildException("Output file is not specified");
        }
    }

    public AFile createFile() {
        AFile af = new AFile();
        files.add(af);
        return af;
    }

    public void setWrite(String write) {
        this.write = write;
    }

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    public static class AFile extends DataType {

        String value;

        public void setName(String p) {
            value = p;
        }
    }
}
