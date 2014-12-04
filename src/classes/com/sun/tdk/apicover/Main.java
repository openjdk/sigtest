/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.apicover;

import com.sun.tdk.signaturetest.Version;
import com.sun.tdk.signaturetest.classpath.ClasspathImpl;
import com.sun.tdk.signaturetest.core.*;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.loaders.BinaryClassDescrLoader;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.sigfile.Format;
import com.sun.tdk.signaturetest.sigfile.MultipleFileReader;
import com.sun.tdk.signaturetest.util.BatchFileParser;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.OptionInfo;
import com.sun.tdk.apicover.markup.Adapter;
import com.sun.tdk.signaturetest.Result;

import com.sun.tdk.signaturetest.core.MemberCollectionBuilder.BuildMode;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.util.SwissKnife;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Main implements Log {

    private final static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Main.class);
    private ApicovOptions ao = AppContext.getContext().getBean(ApicovOptions.class);

    // mandatory options with one parameter
//    public static final String API_OPTION = "-api";
//    public static final String TS_OPTION = "-ts";
    // non-mandatory Strings
    public static final String MODE_OPTION = "-mode";
    public static final String MODE_VALUE_WORST = "w";
    public static final String MODE_VALUE_REAL = "r";
    public static final String DETAIL_OPTION = "-detail";
    public static final String FORMAT_OPTION = "-format";
    public static final String FORMAT_VALUE_XML = "xml";
    public static final String FORMAT_VALUE_PLAIN = "plain";
    public static final String REPORT_OPTION = "-report";
//    public static final String TSICNLUDE_OPTION = "-tsInclude";
//    public static final String TSICNLUDEW_OPTION = "-tsIncludeW";
//    public static final String TSEXCLUDE_OPTION = "-tsExclude";
    //public static final String APIINCLUDE_OPTION = "-apiInclude";
//    public static final String APIINCLUDEW_OPTION = "-apiIncludeW";
    //public static final String APIEXCLUDE_OPTION = "-apiExclude";
    public static final String EXCLUDELIST_OPTION = "-excludeList";
    // Single switches
    public static final String EXCLUDEINTERFACES_OPTION = "-excludeInterfaces";
    public static final String EXCLUDEABSTRACTCLASSES_OPTION = "-excludeAbstractClasses";
    public static final String EXCLUDEABSTRACTMETHODS_OPTION = "-excludeAbstractMethods";
    public static final String EXCLUDEFIELD_OPTION = "-excludeFields";
    public static final String INCLUDECONSTANTFIELDS_OPTION = "-includeConstantFields";
    public static final String DEBUG_OPTION = "-debug";
    // special Strings
    public static final String VERSION_OPTION = "-version";
    public static final String HELP_OPTION = "-help";
    public static final String QUESTIONMARK = "-?";
    // old Strings, have their own reports
    public static final String OUT_OPTION = "-out";
    public static final String VALIDATE_OPTION = "-validate";
    static final String MAIN_URI = "file:";
    private PrintWriter log;
    static protected boolean debug = false;
    public final static int DefaultCacheSize = 4096;
    private boolean isWorstCaseMode = true; // worst case is default
    protected ClasspathImpl classpath;
    //protected String classpathStr = null;
    /**
     * URL pointing to signature file.
     */
    protected String signatureFile;
    RefCounter refCounter = new RefCounter();
    ReportGenerator reporter;
    String ts;
    private PackageGroup packagesTS = new PackageGroup(true);
    private PackageGroup excludedPackagesTS = new PackageGroup(true);
    private PackageGroup purePackagesTS = new PackageGroup(false);
    private PackageGroup packages = new PackageGroup(true);
    private PackageGroup purePackages = new PackageGroup(false);
    private PackageGroup excludedPackages = new PackageGroup(true);

    /**
     * Run the test using command-line; return status via numeric exit code.
     *
     * @see #run(String[],PrintWriter,PrintWriter)
     */
    public static void main(String[] args) {
        Main main = new Main();
        main.run(args, new PrintWriter(System.err, true), null);
    }

    /**
     * This is the gate to run the test with the JavaTest application.
     *
     * @param log This log-file is used for error messages.
     * @param ref This reference-file is ignored here.
     * @see #main(String[])
     */
    public void run(String[] args, PrintWriter log, PrintWriter ref) {
        this.log = log;
        reporter = ReportGenerator.createReportGenerator(refCounter, log);
        try {
            parseParameters(args);
            check();
        } catch (Exception e) {
            debug(e);
            error(e.getMessage());
        } finally {
            if (classpath != null) {
                classpath.close();
            }
        }
    }

    /**
     * Parse options specific for <b>SignatureTest</b>, and pass other options
     * to <b>SigTest</b> parameters parser.
     *
     * @param args Same as <code>args[]</code> passes to <code>main()</code>.
     * @throws Exception
     */
    protected void parseParameters(String[] args) throws Exception {

        try {
            args = BatchFileParser.processParameters(args);
        } catch (CommandLineParserException ex) {
            SwissKnife.reportThrowable(ex);
        }

        CommandLineParser parser = new CommandLineParser(this, "-");

        // Print help text only and exit.
        if (args != null && args.length == 1 && (parser.isOptionSpecified(args[0], VERSION_OPTION))) {
            System.err.println(Version.getVersionInfo());
            passed();
        } else if (args == null || args.length == 0 || (args.length == 1
                && (parser.isOptionSpecified(args[0], HELP_OPTION)
                || parser.isOptionSpecified(args[0], QUESTIONMARK)))) {
            version();
            usage();
            passed();
        }

        final String optionsDecoder = "decodeOptions";

        parser.addOption(REPORT_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(MODE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(DETAIL_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(FORMAT_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(EXCLUDEINTERFACES_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(EXCLUDEABSTRACTCLASSES_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(EXCLUDEABSTRACTMETHODS_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(EXCLUDEFIELD_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(INCLUDECONSTANTFIELDS_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(EXCLUDELIST_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);

        parser.addOption(DEBUG_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(VERSION_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(HELP_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(QUESTIONMARK, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(OUT_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(VALIDATE_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOptions(ao.getOptions(), optionsDecoder);

        try {
            reporter.addConfig(MODE_OPTION, MODE_VALUE_WORST);
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            //usage();
            error(e.getMessage());
        }

        if (parser.isOptionSpecified(INCLUDECONSTANTFIELDS_OPTION)
                && parser.isOptionSpecified(EXCLUDEFIELD_OPTION)) {
            error(i18n.getString("Main.error.arg.conflict", new Object[]{EXCLUDEFIELD_OPTION, INCLUDECONSTANTFIELDS_OPTION}));
        }

        packages.addPackages(ao.getValues(Option.API_INCLUDE));
        purePackages.addPackages(ao.getValues(Option.API_INCLUDEW));
        excludedPackages.addPackages(ao.getValues(Option.API_EXCLUDE));

        packagesTS.addPackages(ao.getValues(Option.TS_ICNLUDE));
        purePackagesTS.addPackages(ao.getValues(Option.TS_ICNLUDEW));
        excludedPackagesTS.addPackages(ao.getValues(Option.TS_EXCLUDE));

        if (packages.isEmpty() && purePackages.isEmpty()) {
            packages.addPackage("");
        }

        if (packagesTS.isEmpty() && purePackagesTS.isEmpty()) {
            packagesTS.addPackage("");
        }

        ts = ao.getValue(Option.TS);
        reporter.addConfig(Option.TS.getKey(), ts);

        try {
            classpath = new ClasspathImpl(ts);
        } catch (SecurityException e) {
            debug(e);
            log.println(i18n.getString("Main.error.sec.newclasses"));
        }

        signatureFile = ao.getValue(Option.API);
        reporter.addConfig(Option.API.getKey(), signatureFile);

    }

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {

        if (ao.readOptions(optionName, args)) {
            return;
        }

        if (optionName.equalsIgnoreCase(MODE_OPTION)) {
            if (!MODE_VALUE_WORST.equalsIgnoreCase(args[0])
                    && !MODE_VALUE_REAL.equalsIgnoreCase(args[0])) {
                error(i18n.getString("Main.error.arg.invalid", MODE_OPTION));
            }
            isWorstCaseMode = MODE_VALUE_WORST.equalsIgnoreCase(args[0]);
            refCounter.setMode(args[0]);
            reporter.addConfig(MODE_OPTION, args[0].toLowerCase());
        } else if (optionName.equalsIgnoreCase(FORMAT_OPTION)) {
            if (!FORMAT_VALUE_PLAIN.equalsIgnoreCase(args[0])
                    && !FORMAT_VALUE_XML.equalsIgnoreCase(args[0])) {
                error(i18n.getString("Main.error.arg.invalid", FORMAT_OPTION));
            }
            reporter = reporter.createReportGenerator(args[0], log);
        } else if (optionName.equalsIgnoreCase(DETAIL_OPTION)) {
            try {
                int d = Integer.parseInt(args[0]);
                if (d < 0 || d >= ReportGenerator.DETAIL_LEVEL.values().length) {
                    throw new NumberFormatException();
                }
                reporter.setDetail(ReportGenerator.DETAIL_LEVEL.values()[d]);
            } catch (NumberFormatException e) {
                error(i18n.getString("Main.error.arg.invalid", DETAIL_OPTION));
            }
        } else if (optionName.equalsIgnoreCase(REPORT_OPTION)) {
            reporter.setReportfile(args[0]);
        } else if (optionName.equalsIgnoreCase(INCLUDECONSTANTFIELDS_OPTION)) {
            reporter.setConstatnChecking(true);
            reporter.addConfig(INCLUDECONSTANTFIELDS_OPTION, "yes");
        } else if (optionName.equalsIgnoreCase(EXCLUDEABSTRACTCLASSES_OPTION)) {
            reporter.excludeAbstractClasses();
            reporter.addConfig(EXCLUDEABSTRACTCLASSES_OPTION, "yes");
        } else if (optionName.equalsIgnoreCase(EXCLUDEABSTRACTMETHODS_OPTION)) {
            reporter.excludeAbstractMethods();
            reporter.addConfig(EXCLUDEABSTRACTMETHODS_OPTION, "yes");
        } else if (optionName.equalsIgnoreCase(EXCLUDEINTERFACES_OPTION)) {
            reporter.excludeInterfaces();
            reporter.addConfig(EXCLUDEINTERFACES_OPTION, "yes");
        } else if (optionName.equalsIgnoreCase(EXCLUDEFIELD_OPTION)) {
            reporter.excludeFields();
            reporter.addConfig(EXCLUDEFIELD_OPTION, "yes");
        } else if (optionName.equalsIgnoreCase(EXCLUDELIST_OPTION)) {
            reporter.addXList(args);
        } else if (optionName.equalsIgnoreCase(DEBUG_OPTION)) {
            debug = true;
        } else if (optionName.equalsIgnoreCase(HELP_OPTION) || optionName.equalsIgnoreCase(QUESTIONMARK)) {
            version();
            usage();
            passed();
        } else if (optionName.equalsIgnoreCase(VERSION_OPTION)) {
            version();
            passed();
        } else if (optionName.equalsIgnoreCase(OUT_OPTION)) {
            throw new CommandLineParserException(i18n.getString("Main.error.arg.legacy", OUT_OPTION));
        } else if (optionName.equalsIgnoreCase(VALIDATE_OPTION)) {
            throw new CommandLineParserException(i18n.getString("Main.error.arg.legacy", VALIDATE_OPTION));
        }
    }

    private boolean isPackageMember(String name) {
        return !excludedPackages.checkName(name)
                && (packages.checkName(name) || purePackages.checkName(name));
    }

    private boolean isTSMember(String name) {
        return !excludedPackagesTS.checkName(name)
                && (packagesTS.checkName(name) || purePackagesTS.checkName(name));
    }

    private static void version() {
        System.err.println("API Cover Tool -  SignatureTest version " + Version.Number);
    }

    public void usage() {
        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();
        sb.append(i18n.getString("Main.usage.start"));
        sb.append(nl).append(i18n.getString("Main.usage.ts", Option.TS.getKey()));
        sb.append(nl).append(i18n.getString("Main.usage.tsInclude", Option.TS_ICNLUDE.getKey()));
        sb.append(nl).append(i18n.getString("Main.usage.tsIncludeW", Option.TS_ICNLUDEW.getKey()));
        sb.append(nl).append(i18n.getString("Main.usage.tsExclude", Option.TS_EXCLUDE.getKey()));
        sb.append(nl).append(i18n.getString("Main.usage.api", Option.API.getKey()));
        sb.append(nl).append(i18n.getString("Main.usage.apiInclude", Option.API_INCLUDE.getKey()));
        sb.append(nl).append(i18n.getString("Main.usage.apiIncludeW", Option.API_INCLUDEW.getKey()));
        sb.append(nl).append(i18n.getString("Main.usage.apiExclude", Option.API_EXCLUDE.getKey()));
        sb.append(nl).append(i18n.getString("Main.usage.excludeList", EXCLUDELIST_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.excludeInterfaces", EXCLUDEINTERFACES_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.excludeAbstractClasses", EXCLUDEABSTRACTCLASSES_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.excludeAbstractMethods", EXCLUDEABSTRACTMETHODS_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.excludeFields", EXCLUDEFIELD_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.includeConstantFields", INCLUDECONSTANTFIELDS_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.mode", MODE_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.detail", DETAIL_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.format", FORMAT_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.report", REPORT_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.debug", DEBUG_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.help", HELP_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.version", VERSION_OPTION));
        sb.append(nl).append(i18n.getString("Main.usage.end"));
        System.err.println(sb.toString());
    }

    void check() {
        FileManager f = new FileManager();
        MultipleFileReader in = new MultipleFileReader(log, MultipleFileReader.CLASSPATH_MODE, f);
        ClassHierarchy apiHierarchy = new ClassHierarchyImpl(in, ClassHierarchy.ALL_PUBLIC);
        new Adapter(f);

        try {
            if (!in.readSignatureFiles(MAIN_URI, signatureFile)) {
                error(i18n.getString("Main.error.sigfile.invalid", signatureFile));
            }

            // Signature file version
            boolean is4 = in.isFeatureSupported(Format.BuildMembers);
            MemberCollectionBuilder b = new MemberCollectionBuilder(this);
            ClassDescription cd;
            while ((cd = in.nextClass()) != null) {
                try {
                    if (is4) {
                        cd.setHierarchy(apiHierarchy);
                        if (!isWorstCaseMode) {
                            b.setBuildMode(BuildMode.SIGFILE);
                        }
                        b.createMembers(cd, true, false, true);
                    }
                } catch (Exception e) {
                    debug(e);
                    error(i18n.getString("Main.error.check", e.getMessage()));
                }
                if (isPackageMember(cd.getQualifiedName())) {
                    refCounter.addClass(cd);
                }
                refCounter.addTSClass(cd, true);
            }

            /*
             * Read TS and send each call to reporter.
             */
            BinaryClassDescrLoader tsLoader = new BinaryClassDescrLoader(classpath,
                    DefaultCacheSize);

            tsLoader.setLog(log);
            tsLoader.setIgnoreAnnotations(true);
            ClassHierarchy tsHierarchy = new ClassHierarchyImpl(tsLoader,
                    ClassHierarchy.ALL_PUBLIC);
            int size = 0;
            List<MemberDescription> calls = new ArrayList<MemberDescription>();
            while (classpath.hasNext()) {
                String name = classpath.nextClassName();
                if (!isTSMember(name)) {
                    continue;
                }

                try {
                    ClassDescription tsClass = tsHierarchy.load(name);
                    refCounter.addTSClass(tsClass, false);
                    calls.addAll(tsLoader.loadCalls(name));
                } catch (ClassNotFoundException e) {
                    if (debug) {
                        log.println(i18n.getString("Main.warning.class.invalid", name));
                    }
                    debug(e);
                } catch (ClassFormatError e) {
                    if (debug) {
                        log.println(i18n.getString("Main.warning.class.invalid", name));
                    }
                    debug(e);
                } catch (Throwable t) {
                    debug(t);
                    error(i18n.getString("Main.error.check", t.getMessage()));
                }

            }
            //classpath.close();
            for (MemberDescription md : calls) {
                size++;
                refCounter.addRef(md);
            }

            if (size == 0) {
                System.err.println(i18n.getString("Main.warning.ts.empty", ts));
            }
            reporter.out();
        } catch (Throwable e) {
            debug(e);
            error(i18n.getString("Main.error.check", e.getMessage()));
        } finally {
            in.close();
        }
    }

    private void error(String s) {
        log.println(s);
        if (!new Boolean(System.getProperty(Result.NO_EXIT)).booleanValue()) {
            System.exit(1);
        }
    }

    private void passed() {
        if (!new Boolean(System.getProperty(Result.NO_EXIT)).booleanValue()) {
            System.exit(0);
        }
    }

    private void debug(Throwable t) {
        if (debug) {
            SwissKnife.reportThrowable(t, log);
        }
    }

    public void storeError(String s, Logger utilLog) {
        log.append(s);
    }

    public void storeWarning(String s, Logger utilLog) {
        log.append(s);
    }

}
