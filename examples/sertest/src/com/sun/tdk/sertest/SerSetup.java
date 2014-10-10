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
package com.sun.tdk.sertest;

import com.sun.tdk.signaturetest.Setup;
import java.io.PrintWriter;

/**
 * @author Mikhail Ershov
 */
public class SerSetup extends Setup {

    public static void main(String[] args) {
        SerSetup t = new SerSetup();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    public SerSetup() {
        super();
        new SerUtil().initFormat(this);
    }

    @Override
    protected String getComponentName() {
        return "Serialization test  setup";
    }

    @Override
    // add NONCLOSEDFILE_OPTION
    protected boolean parseParameters(String[] args) {
        for (String option : args) {
            if (option.equalsIgnoreCase(NONCLOSEDFILE_OPTION)) {
                return super.parseParameters(args);
            }
        }
        int len = args.length;
        String[] newArgs = new String[len + 1];

        System.arraycopy(args, 0, newArgs, 0, len);
        newArgs[args.length] = NONCLOSEDFILE_OPTION;

        return super.parseParameters(newArgs);
    }
}
