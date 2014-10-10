/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest;

import java.io.PrintWriter;

/**
 * This is a simple wrapper for SignatureTest class that implements required by
 * JavaTest interface.
 *
 * @author Serguei Ivashin
 *
 * @test
 * @executeClass com.sun.tdk.signaturetest.Test
 */
public class Test implements com.sun.javatest.Test {

    /**
     * Run the test using command-line; return status via numeric exit code.
     */
    public static void main(String[] args) {
        Test t = new Test();
        t.run(args, new PrintWriter(System.err, true), new PrintWriter(System.out, true)).exit();
    }

    /**
     * This is the gate to run the test with the JavaTest application.
     *
     * @param log This log-file is used for error messages.
     * @param ref This reference-file is ignored here.
     */
    public com.sun.javatest.Status run(String[] args, PrintWriter log, PrintWriter ref) {

        SignatureTest t = SignatureTest.getInstance();
        t.run(args, log, ref);
        return com.sun.javatest.Status.parse(t.toString().substring(7));
    }

}
