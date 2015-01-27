/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @author Mikhail Ershov
 */
public abstract class Options {
    private Map<Option, List<String>> values;

    protected Options() {
        values = new HashMap<>();
    }

    protected Options getParent() {
        return null;
    }

    public boolean readOptions(String optionName, String[] args) {
        boolean parentRead = false;
        if (getParent() != null) {
            parentRead = getParent().readOptions(optionName, args);
        }
        for (Option option : getOptions()) {
            if (option.accept(optionName)) {
                switch (option.getKind()) {
                    case NONE:
                    case INSTEAD_OF_ANY:
                        values.put(option, null);
                        break;
                    case SINGLE_OPT:
                    case SINGLE_REQ:
                        assert args != null;
                        assert args.length > 0;
                        assert !args[0].isEmpty();
                        values.put(option, Arrays.asList(args[0]));
                        break;
                    case REQ_LIST:
                        assert args != null;
                        assert args.length > 0;
                        assert !args[0].isEmpty();
                        values.put(option, Arrays.asList(CommandLineParser.parseListOption(args)));
                        break;
                    case MANY_OPT:
                        assert args != null;
                        assert args.length > 0;
                        if (!values.containsKey(option)) {
                            values.put(option, new ArrayList<String>());
                        }
                        values.get(option).addAll(Arrays.asList(CommandLineParser.parseListOption(args)));
                        break;
                }
                return true;
            }
        }
        return parentRead;
    }

    /* Returns if the NONE option was specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public boolean isSet(Option option) {
        assert option != null;
        assert option.getKind() == Option.Kind.NONE || option.getKind() == Option.Kind.INSTEAD_OF_ANY;
        if (!getOptions().contains(option)) {
            throw new IllegalArgumentException("Option " + option.getKey() + " is not defined in the context");
        }
        return values.containsKey(option);
    }

    /* Returns a value for SINGLE_OPT option
     * or null if the option was not specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public String getValue(Option option) {
        assert option != null;
        List<String> vals = getValues(option);
        if (vals == null) {
            return null;
        } else {
            return vals.get(0);
        }
    }

    /* Returns a list of values for MANY_OPT option
     * or null if the option was not specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public List<String> getValues(Option option) {
        assert option != null;
        if (!getOptions().contains(option)) {
            throw new IllegalArgumentException("Option " + option.getKey() + " is not defined in the context");
        }
        if (!values.containsKey(option)) {
            return null;
        }
        return values.get(option);
    }

    public abstract EnumSet<Option> getOptions();
}
