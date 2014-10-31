/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.core.context;

import com.sun.tdk.signaturetest.util.CommandLineParser;

import java.util.*;

/*
 * This bean represents the core application options
 * associated with SigTest class
 * @author Mikhail Ershov
 */
public class BaseOptions {

    public static final String ID = BaseOptions.class.getName();

    private EnumSet<Option> options = EnumSet.of(Option.X_JIMAGE_OPTION);
    private Map<Option, List<String>> values = new HashMap<>();

    public boolean readOptions(String optionName, String[] args) {
        for (Option option : options) {
            if (option.accept(optionName)) {
                switch (option.getKind()) {
                    case NONE:
                        values.put(option, null);
                        break;
                    case SINGLE:
                        assert args != null;
                        assert args.length > 0;
                        assert !args[0].isEmpty();
                        values.put(option, Arrays.asList(args[0]));
                        break;
                    case MANY:
                        assert args != null;
                        assert args.length > 0;
                        if (!values.containsKey(option)) {
                            values.put(option, new ArrayList<String>());
                        }
                        values.get(option).addAll(Arrays.asList(CommandLineParser.parseListOption(args)));
                }
                return true;
            }
        }
        return false;
    }

    /* Returns if the NONE option was specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public boolean isSet(Option option) {
        assert option != null;
        assert option.getKind() == Option.Kind.NONE;
        if (!options.contains(option)) {
            throw new IllegalArgumentException("Option " + option.getKey() + " is not defined in the context");
        }
        return values.containsKey(option);
    }

    /* Returns a value for SINGLE option
     * or null if the option was not specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public String getValue(Option option) {
        assert option != null;
        return getValues(option, Option.Kind.SINGLE).get(0);
    }

    /* Returns a list of values for MANY option
     * or null if the option was not specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public List<String> getValues(Option option) {
        return getValues(option, Option.Kind.MANY);
    }

    private List<String> getValues(Option option, Option.Kind expectedKind ) {
        assert option != null;
        if (!options.contains(option)) {
            throw new IllegalArgumentException("Option " + option.getKey() + " is not defined in the context");
        }
        if (!values.containsKey(option)) {
            return null;
        }
        assert option.getKind() == expectedKind;
        assert option.getKind() != Option.Kind.SINGLE || values.get(option).size() == 1;
        return values.get(option);
    }

    public EnumSet<Option> getOptions() {
        return options;
    }
}
