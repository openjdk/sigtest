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

import com.sun.tdk.signaturetest.util.I18NResourceBundle;

/**
 * This is utility class for Setup, SignatureTest and Merge.
 *
 * @author Serguei Ivashin
 */
public class Result {

    static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Result.class);

    // Name of system property, see exit method
    public final static String NO_EXIT = "SigTest.NO_EXIT";

    private int type = NOT_RUN;
    private String reason;

    private static final int PASSED = 0;
    private static final int FAILED = 1;
    private static final int ERROR = 2;
    private static final int NOT_RUN = 3;
    private static final String[] texts = {i18n.getString("Result.code.passed"),
        i18n.getString("Result.code.failed"), i18n.getString("Result.code.error"),
        i18n.getString("Result.code.notrun")};
    private static final int[] exitCodes = {95, 97, 98, 99};

    public String toString() {
        String[] invargs = {texts[type], (reason == null ? "" : reason)};
        return i18n.getString("Result.message.status", invargs);
    }

    protected boolean passed() {
        type = PASSED;
        reason = null;
        return true;
    }

    public boolean passed(String s) {
        type = PASSED;
        reason = s;
        return true;
    }

    public boolean failed(String s) {
        type = FAILED;
        reason = s;
        return false;
    }

    public boolean error(String s) {
        type = ERROR;
        reason = s;
        return false;
    }

    public boolean notrun(String s) {
        type = NOT_RUN;
        reason = s;
        return true;
    }

    public boolean notrun() {
        type = NOT_RUN;
        reason = "";
        return true;
    }


    public boolean isPassed() {
        return type == PASSED;
    }

    public String getReason() {
        return reason;
    }

    protected boolean exit() {
        if (System.err != null) {
            System.err.println(toString());
            System.err.flush();
        }
        // for unit-tests and mass runs

        // Don't change this to if(Boolean.parseBoolean(System.getProperty(NO_EXIT))) {
        // because Boolean.parseBoolean is since 1.5
        if (Boolean.valueOf(System.getProperty(NO_EXIT))) {
            return isPassed();
        } else {
            System.exit(exitCodes[type]);
            return false; // never happens
        }

    }
}
