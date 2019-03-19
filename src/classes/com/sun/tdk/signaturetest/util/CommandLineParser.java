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

import com.sun.tdk.signaturetest.core.context.Option;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Roman Makarchuk
 * @author Yuri Danilevich
 */
public class CommandLineParser {

    private Object servicedObject;
    private KnownOptions knownOptions;
    private Map<String, String> decoders = new HashMap<>();
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(CommandLineParser.class);
    private Map<String, List<String>> foundOptions = new HashMap<>();

    public CommandLineParser(Object servicedObject, String optionPrefix) {
        this.servicedObject = servicedObject;
        knownOptions = new KnownOptions(optionPrefix);
    }

    public final void addOption(String option, OptionInfo info) {

        String temp = option;
        if (!info.isCaseSentitive()) {
            temp = option.toLowerCase();
        }

        knownOptions.add(temp, info);
    }

    public final void addOption(String option, OptionInfo info, String decoder) {

        String temp = option;
        if (!info.isCaseSentitive()) {
            temp = option.toLowerCase();
        }

        knownOptions.add(temp, info);
        decoders.put(temp, decoder);
    }

    public final void removeKnownOption(String option) {
        knownOptions.remove(option);
    }

    public void processArgs(String[] args) throws CommandLineParserException {

        args = BatchFileParser.processParameters(args);

        foundOptions.clear();
        boolean noValidate = false;

        String optionStr = null;

        for (String arg : args) {
            if (knownOptions.isKnownOption(arg)) {
                OptionInfo ki = knownOptions.get(arg);
                optionStr = ki.toKey(arg);

                List<String> params = foundOptions.get(optionStr);

                if (params == null) {
                    foundOptions.put(optionStr, new ArrayList<String>());
                } else if (!ki.isMultiple()) {
                    throw new CommandLineParserException(i18n.getString("CommandLineParser.error.option.duplicate", optionStr));
                }

                Option o = Option.byKey(arg);
                if (o != null) {
                    noValidate = noValidate || o.getKind() == Option.Kind.INSTEAD_OF_ANY;
                }

            } else if (!knownOptions.isOption(arg)) {
                if (optionStr != null) {
                    foundOptions.get(optionStr).add(arg);
                }
            } else {
//                optionStr = null;
                throw new CommandLineParserException(i18n.getString("CommandLineParser.error.option.unknown", arg));
            }
        }

        if (!noValidate) {
            knownOptions.validate(foundOptions);
        }

        for (String foundOption : foundOptions.keySet()) {
            invokeDecoder(foundOption, foundOptions.get(foundOption));
        }
    }

    public boolean isOptionSpecified(String arg, String checkedOption) {
        return knownOptions.isOption(arg) && checkedOption.equalsIgnoreCase(arg);
    }

    // Note: USE ONLY KNOWN OPTIONS as ARGUMENT OF THIS METHOD!
    // Option should not has the prefix
    public boolean isOptionSpecified(String option) {

        if (!knownOptions.isKnownOption(option)) {
            throw new IllegalArgumentException(i18n.getString("CommandLineParser.error.option.unknown", option));
        }

        OptionInfo ki = knownOptions.get(option);
        String temp = ki.toKey(option);
        return foundOptions.get(temp) != null;
    }

    private void invokeDecoder(String option, List<String> params) throws CommandLineParserException {

        String decoder = decoders.get(option);
        if (decoder != null) {
            invokeExplicitDecoder(decoder, option, params);
        } else {
            invokeDefaultDecoder(option, params);
        }
    }

    private void invokeExplicitDecoder(String decoder, String option, List<String> params) throws CommandLineParserException {

        Class<?> cl = servicedObject.getClass();

        String[] stemp = new String[params.size()];
        for (int i = 0; i < params.size(); ++i) {
            stemp[i] = params.get(i);
        }

        try {
            Method method = cl.getMethod(decoder, String.class, String[].class);
            method.invoke(servicedObject, option, stemp);

        } catch (NoSuchMethodException nsme) {
            throw new CommandLineParserException(i18n.getString("CommandLineParser.error.decoder.explicit.notfound", new Object[]{decoder, option, cl.getName()}), nsme);
        } catch (InvocationTargetException e) {
            Throwable th = e.getTargetException();
            String message;
            if (th instanceof CommandLineParserException) {
                message = th.getMessage();
            } else {
                message = i18n.getString("CommandLineParser.error.decoder.failed", new Object[]{option, th});
            }
            throw new CommandLineParserException(message);
        } catch (Exception e) {
            throw new CommandLineParserException(i18n.getString("CommandLineParser.error.decoder.failed", new Object[]{option, e}));
        }
    }

    private void invokeDefaultDecoder(String option, List<String> params) throws CommandLineParserException {
        try {
            String[] stemp = new String[params.size()];
            for (int i = 0; i < params.size(); ++i) {
                stemp[i] = params.get(i);
            }

            getDefaultDecoderMethod(option).invoke(servicedObject, new Object[]{stemp});
        } catch (Exception e) {
            throw new CommandLineParserException(i18n.getString("CommandLineParser.error.decoder.failed", new Object[]{option, e}));
        }
    }

    private boolean isDecoder(Method method, String option) {
        String methodName = "decode" + option;
        return method.getName().equalsIgnoreCase(methodName)
                && method.getParameterTypes().length == 1
                && method.getParameterTypes()[0].isAssignableFrom(String[].class);
    }

    private Method getDefaultDecoderMethod(String option) throws CommandLineParserException {

        Method m = getDefaultDecoderMethod(servicedObject.getClass().getMethods(), option);

        if (m == null) {
            throw new CommandLineParserException(i18n.getString("CommandLineParser.error.decoder.default.notfound", new Object[]{option, servicedObject.getClass().getName()}));
        }

        return m;
    }

    private Method getDefaultDecoderMethod(Method[] methods, String option) {

        for (Method method : methods) {
            if (isDecoder(method, option)) {
                return method;
            }
        }

        return null;
    }

    public static String[] parseListOption(String[] args) {
        ArrayList<String> ar = new ArrayList<>();
        for (String arg : args) {
            StringTokenizer st = new StringTokenizer(arg, System.getProperty("path.separator"));
            while (st.hasMoreTokens()) {
                ar.add(st.nextToken());
            }
        }
        return ar.toArray(new String[]{});
    }

    public void addOptions(EnumSet<Option> options, String optionsDecoder) {
        for (Option o : options) {
            addOption(o, optionsDecoder);
        }
    }

    public void addOption(Option o, String optionsDecoder) {
        switch (o.getKind()) {
            case NONE:
            case INSTEAD_OF_ANY:
                addOption(o.getKey(), OptionInfo.optionalFlag(), optionsDecoder);
                if (o.hasAlias()) {
                    addOption(o.getAlias(), OptionInfo.optionalFlag(), optionsDecoder);
                }
                break;
            case SINGLE_OPT:
                addOption(o.getKey(), OptionInfo.option(1), optionsDecoder);
                if (o.hasAlias()) {
                    addOption(o.getAlias(), OptionInfo.option(1), optionsDecoder);
                }
                break;
            case SINGLE_REQ:
            case REQ_LIST:
                addOption(o.getKey(), OptionInfo.requiredOption(1), optionsDecoder);
                if (o.hasAlias()) {
                    addOption(o.getKey(), OptionInfo.requiredOption(1), optionsDecoder);
                }
                break;
            case MANY_OPT:
                addOption(o.getKey(), OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
                if (o.hasAlias()) {
                    addOption(o.getAlias(), OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
                }
                break;
        }
    }

    private static class KnownOptions {

        private Map<String, OptionInfo> data = new HashMap<>();
        private final String optionPrefix;

        public KnownOptions(String optionPrefix) {
            this.optionPrefix = optionPrefix;
        }

        private boolean isKnownOption(String arg) {

            if (isOption(arg)) {
                String temp = arg;

                if (!data.containsKey(temp)) {
                    temp = temp.toLowerCase();
                    OptionInfo ki = data.get(temp);
                    if (ki != null) {
                        return !ki.isCaseSentitive();
                    }
                } else {
                    return true;
                }
            }
            return false;
        }

        private void add(String option, OptionInfo info) {
            if (!option.startsWith(optionPrefix)) {
                throw new IllegalArgumentException(i18n.getString("CommandLineParser.error.option.noprefix", optionPrefix));
            }
            data.put(option, info);
        }

        private void remove(String option) {
            data.remove(option);
        }

        private OptionInfo get(String option) {

            String temp = option;
            OptionInfo ki = data.get(temp);

            if (ki == null) {
                temp = temp.toLowerCase();
                ki = data.get(temp);

                if (ki == null || ki.isCaseSentitive()) {
                    return null;
                }
            }

            return ki;
        }

        private void validateRequiredOptions(Set<String> foundKeys) throws CommandLineParserException {
            Set<String> keySet = data.keySet();

            for (String option : keySet) {
                OptionInfo ki = data.get(option);
                if (ki.isRequired() && !foundKeys.contains(option)) {
                    throw new CommandLineParserException(i18n.getString("CommandLineParser.error.option.required", option));
                }
            }
        }

        private void validateCount(String option, int paramCount) throws CommandLineParserException {
            OptionInfo info = data.get(option);
            int minCount = info.getMinCount();
            int maxCount = info.getMaxCount();

            if (paramCount < minCount) {
                throw new CommandLineParserException(i18n.getString("CommandLineParser.error.option.require_more_parameters", new Object[]{option, minCount}));
            }

            if (paramCount > maxCount) {

                String msg = i18n.getString("CommandLineParser.error.option.require_less_parameters", new Object[]{option, maxCount});

                if (maxCount == 0) {
                    msg = i18n.getString("CommandLineParser.error.option.require_no_parameters", option);
                }

                throw new CommandLineParserException(msg);
            }
        }

        private void validate(Map<String, List<String>> params) throws CommandLineParserException {
            validateRequiredOptions(params.keySet());

            for (String option : params.keySet()) {
                validateCount(option, params.get(option).size());
            }
        }

        public boolean isOption(String arg) {
            return arg.startsWith(optionPrefix);
        }
    }
}
