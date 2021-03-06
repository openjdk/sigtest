/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.util;

public final class OptionInfo {

    public static final boolean DEFAULT_SENSITIVITY = false;
    public static final int UNLIMITED = Integer.MAX_VALUE;
    private final boolean required; // the option must be always specified in the command line
    private final int minCount;     // minimum parameters that the option requires
    private final int maxCount;
    private final boolean multiple; // the option can be specified several times
    private final boolean caseSensitive;  // is option case sensitive ot not

    public static OptionInfo requiredOption(int paramCount) {
        return new OptionInfo(true, paramCount, paramCount, false, DEFAULT_SENSITIVITY);
    }

    public static OptionInfo option(int paramCount) {
        return new OptionInfo(false, paramCount, paramCount, false, DEFAULT_SENSITIVITY);
    }

    public static OptionInfo requiredOptionVariableParams(int min, int max) {
        return new OptionInfo(true, min, max, true, DEFAULT_SENSITIVITY);
    }

    public static OptionInfo optionVariableParams(int min, int max) {
        return new OptionInfo(false, min, max, true, DEFAULT_SENSITIVITY);
    }

    public static OptionInfo optionalFlag() {
        return new OptionInfo(false, 0, 0, false, DEFAULT_SENSITIVITY);
    }

    public OptionInfo(boolean required, int minCount, int maxCount, boolean multiple, boolean isCaseSensitive) {
        this.required = required;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.multiple = multiple;
        this.caseSensitive = isCaseSensitive;
    }

    public String toKey(String arg) {
        String temp = arg;
        if (!caseSensitive) {
            temp = temp.toLowerCase();
        }
        return temp;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public boolean isCaseSentitive() {
        return caseSensitive;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isRequired() {
        return required;
    }
}
