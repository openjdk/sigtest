/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.core.context.TestOptions;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.OptionInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The purpose of this program is to create the signature file and make
 * comparison in a one step. This is a simple wrapper that parses command line
 * options and calls Setup first and SignatureTest next.
 *
 * @author Serguei Ivashin
 */
public class SetupAndTest extends Result {

    // specific SetupAndTest options
    public static final String REFERENCE_OPTION = "-Reference";
    public static final String TEST_OPTION = "-Test";
    // Sets of command line options for:
    private final List<String> setupOptions = new ArrayList<>();
    private final List<String> testOptions = new ArrayList<>();

    public static void main(String[] args) {

        SetupAndTest t = new SetupAndTest();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    public boolean run(String[] args, PrintWriter log, PrintWriter ref) {

        CommandLineParser parser = new CommandLineParser(this, "-");
        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        TestOptions to = AppContext.getContext().getBean(TestOptions.class);

        // Print help text only and exit.
        if (Option.HELP.accept(args[0])) {
            usage();
            notrun();
            return true;
        }

        // Both Setup and SignatureTest always will work in the static mode
        addFlag(setupOptions, Option.STATIC.getKey());
        addFlag(testOptions, Option.STATIC.getKey());

        final String optionsDecoder = "decodeOptions";

        parser.addOption(REFERENCE_OPTION, OptionInfo.requiredOption(1), optionsDecoder);
        parser.addOption(TEST_OPTION, OptionInfo.requiredOption(1), optionsDecoder);

        parser.addOption(SigTest.APIVERSION_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(SigTest.OUT_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(SigTest.CLASSCACHESIZE_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(SignatureTest.CHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(SignatureTest.NOCHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(SignatureTest.MODE_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(SigTest.VERBOSE_OPTION, OptionInfo.optionVariableParams(0, 1), optionsDecoder);

        parser.addOptions(bo.getOptions(), optionsDecoder);
        parser.addOptions(to.getOptions(), optionsDecoder);

        try {
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            if (args.length > 0 && Option.VERSION.accept(args[0])) {
                System.err.println(Version.getVersionInfo());
                return passed();
            } else {
                usage();
                log.println(e.getMessage());
                return failed(e.getMessage());
            }

        }

        // Assign temporary name for the sigfile if none was specified
        if (!parser.isOptionSpecified(Option.FILE_NAME.getKey())) {
            String tmpsigfile = null;

            try {
                File f = File.createTempFile("tmpsigfile", ".sig");
                f.deleteOnExit();
                tmpsigfile = f.getPath();

                addOption(setupOptions, Option.FILE_NAME.getKey(), tmpsigfile);
                addOption(testOptions, Option.FILE_NAME.getKey(), tmpsigfile);
            } catch (IOException ioe) {
                return failed(i18n.getString("SetupAndTest.error.message.tempfile"));
            }
        }

        // Run Setup
        log.println(i18n.getString("SetupAndTest.message.invoke.setup"));
        Setup setup = new Setup();
        setup.run(setupOptions.toArray(new String[0]), log, ref);

        // Run SignatureTest
        if (setup.isPassed()) {
            log.println(i18n.getString("SetupAndTest.message.invoke.sigtest"));
            SignatureTest sigtest = new SignatureTest();
            sigtest.run(testOptions.toArray(new String[0]), log, ref);
            return sigtest.exit();
        } else {
            return setup.exit();
        }
    }

    private void addOption(List<String> options, String optionName, String optionValue) {
        options.add(optionName);
        options.add(optionValue);
    }

    private void addFlag(List<String> options, String flag) {
        options.add(flag);
    }

    private void addOption(List<String> options, String optionName, String[] optionValues) {
        options.add(optionName);
        options.addAll(Arrays.asList(optionValues));
    }

    public void decodeOptions(String optionName, String[] args) {

        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        bo.readOptions(optionName, args);

        TestOptions to = AppContext.getContext().getBean(TestOptions.class);
        to.readOptions(optionName, args);

        if (Option.HELP.accept(optionName)) {
            usage();
        } else if (optionName.equalsIgnoreCase(REFERENCE_OPTION)) {
            addOption(setupOptions, Option.CLASSPATH.getKey(), args[0]);
        } else if (optionName.equalsIgnoreCase(TEST_OPTION)) {
            addOption(testOptions, Option.CLASSPATH.getKey(), args[0]);
        } else if (optionName.equalsIgnoreCase(Option.FILE_NAME.getKey())
                || optionName.equalsIgnoreCase(Option.PACKAGE.getKey())
                || optionName.equalsIgnoreCase(Option.PURE_PACKAGE.getKey())
                || optionName.equalsIgnoreCase(Option.EXCLUDE.getKey())
                || optionName.equalsIgnoreCase(SigTest.APIVERSION_OPTION)
                || optionName.equalsIgnoreCase(SigTest.CLASSCACHESIZE_OPTION)
                || optionName.equalsIgnoreCase(Option.API_INCLUDE.getKey())
                || optionName.equalsIgnoreCase(Option.API_EXCLUDE.getKey())) {

            addOption(setupOptions, optionName, args[0]);
            addOption(testOptions, optionName, args[0]);

        } else if (optionName.equalsIgnoreCase(SigTest.VERBOSE_OPTION)) {

            addOption(setupOptions, optionName, args);
            addOption(testOptions, optionName, args);

        } else if (optionName.equalsIgnoreCase(SigTest.OUT_OPTION)) {

            addOption(testOptions, optionName, args[0]);

        } else if (optionName.equalsIgnoreCase(SignatureTest.MODE_OPTION)) {

            addOption(testOptions, optionName, args[0]);

        } else if (optionName.equalsIgnoreCase(Option.FORMATPLAIN.getKey())
                || optionName.equalsIgnoreCase(Option.FORMATHUMAN.getKey())
                || optionName.equalsIgnoreCase(Option.FORMATHUMAN.getAlias())
                || optionName.equalsIgnoreCase(Option.BACKWARD.getKey())
                || optionName.equalsIgnoreCase(Option.BACKWARD.getAlias())
                || optionName.equalsIgnoreCase(SignatureTest.CHECKVALUE_OPTION)) {

            addFlag(testOptions, optionName);
        }
    }


    /*
     *  Prints the help text.
     *
     */
    public static void usage() {
        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();

        sb.append(i18n.getString("SetupAndTest.usage.version", Version.Number));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.start"));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.reference", REFERENCE_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.test", TEST_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.package", Option.PACKAGE));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.out", SigTest.OUT_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.backward", new Object[]{Option.BACKWARD.getKey(), Option.BACKWARD.getAlias()}));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.human", new Object[]{Option.FORMATHUMAN.getKey(), Option.FORMATHUMAN.getAlias()}));

        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));

        sb.append(nl).append(i18n.getString("SetupAndTest.usage.filename", Option.FILE_NAME));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.packagewithoutsubpackages", Option.PURE_PACKAGE));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.exclude", Option.EXCLUDE));

        sb.append(nl).append(i18n.getString("SetupAndTest.usage.verbose", new Object[]{Setup.VERBOSE_OPTION, Setup.NOWARN}));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.checkvalue", Setup.CHECKVALUE_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.mode", SignatureTest.MODE_OPTION));

        sb.append(nl).append(i18n.getString("SetupAndTest.usage.formatplain", Option.FORMATPLAIN));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.classcachesize", new Object[]{SigTest.CLASSCACHESIZE_OPTION, SigTest.DefaultCacheSize}));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("SetupAndTest.helpusage.version", Option.VERSION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.help", Option.HELP));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));

        System.err.println(sb.toString());
    }
}
