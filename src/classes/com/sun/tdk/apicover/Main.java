/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tdk.apicover.markup.Adapter;
import com.sun.tdk.signaturetest.Result;

import com.sun.tdk.signaturetest.core.MemberCollectionBuilder.BuildMode;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Main implements Log {

    private final static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Main.class);
    private ApicovOptions ao = AppContext.getContext().getBean(ApicovOptions.class);

    // non-mandatory Strings
    public static final String MODE_VALUE_WORST = "w";
    public static final String MODE_VALUE_REAL = "r";
    public static final String FORMAT_VALUE_XML = "xml";
    public static final String FORMAT_VALUE_PLAIN = "plain";
    static final String MAIN_URI = "file:";
    private PrintWriter log;
    static protected boolean debug = false;
    public final static int DefaultCacheSize = 4096;
    private boolean isWorstCaseMode = true; // worst case is default
    protected ClasspathImpl classpath;

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

    private CallFilter callFilter = new CallFilter();

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
            if (parseParameters(args)) {
                check();
            }
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
    protected boolean parseParameters(String[] args) throws Exception {

        if (args.length == 0) {
            version();
            usage();
            passed();
            return false;
        }

        try {
            args = BatchFileParser.processParameters(args);
        } catch (CommandLineParserException ex) {
            SwissKnife.reportThrowable(ex);
        }

        CommandLineParser parser = new CommandLineParser(this, "-");

        // Print help text only and exit.

        final String optionsDecoder = "decodeOptions";

        parser.addOptions(ao.getOptions(), optionsDecoder);

        try {
            reporter.addConfig(Option.MODE.getKey(), MODE_VALUE_WORST); // default
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            error(e.getMessage());
        }

        if (ao.getValue(Option.API) == null && ao.getValue(Option.FILTERSIG) == null) {
            error(i18n.getString("Main.error.option.required", Option.API.getKey()));
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

        if (ao.isSet(Option.INCLUDE_CONSTANT_FIELDS) && ao.isSet(Option.EXCLUDE_FIELDS)) {
            error(i18n.getString("Main.error.arg.conflict",
                    new Object[]{Option.EXCLUDE_FIELDS.getKey(), Option.INCLUDE_CONSTANT_FIELDS.getKey()}));
        }

        if (ao.isSet(Option.INCLUDE_CONSTANT_FIELDS)) {
            reporter.setConstatnChecking(true);
            reporter.addConfig(Option.INCLUDE_CONSTANT_FIELDS.getKey(), "yes");
        }

        if (ao.isSet(Option.EXCLUDE_ABSTRACT_CLASSES)) {
            reporter.excludeAbstractClasses();
            reporter.addConfig(Option.EXCLUDE_ABSTRACT_CLASSES.getKey(), "yes");
        }

        if (ao.isSet(Option.EXCLUDE_ABSTRACT_METHODS)) {
            reporter.excludeAbstractMethods();
            reporter.addConfig(Option.EXCLUDE_ABSTRACT_METHODS.getKey(), "yes");
        }

        if (ao.isSet(Option.EXCLUDE_INTERFACES)) {
            reporter.excludeInterfaces();
            reporter.addConfig(Option.EXCLUDE_INTERFACES.getKey(), "yes");
        }

        if (ao.isSet(Option.EXCLUDE_FIELDS)) {
            reporter.excludeFields();
            reporter.addConfig(Option.EXCLUDE_FIELDS.getKey(), "yes");
        }

        {
            String mode = ao.getValue(Option.MODE);
            if (mode != null) {
                if (!MODE_VALUE_WORST.equalsIgnoreCase(mode)
                        && !MODE_VALUE_REAL.equalsIgnoreCase(mode)) {
                    error(i18n.getString("Main.error.arg.invalid", Option.MODE.getKey()));
                }
                isWorstCaseMode = MODE_VALUE_WORST.equalsIgnoreCase(mode);
                refCounter.setMode(mode.toLowerCase());
                reporter.addConfig(Option.MODE.getKey(), mode.toLowerCase());
            }
        }

        {
            String format = ao.getValue(Option.FORMAT);
            if (format != null) {
                if (!FORMAT_VALUE_PLAIN.equalsIgnoreCase(format)
                        && !FORMAT_VALUE_XML.equalsIgnoreCase(format)) {
                    error(i18n.getString("Main.error.arg.invalid", Option.FORMAT.getKey()));
                }
                reporter = reporter.createReportGenerator(format, log);
            }
        }

        {
            String detail = ao.getValue(Option.DETAIL);
            if (detail != null) {
                try {
                    int d = Integer.parseInt(detail);
                    if (d < 0 || d >= ReportGenerator.DETAIL_LEVEL.values().length) {
                        throw new NumberFormatException();
                    }
                    reporter.setDetail(ReportGenerator.DETAIL_LEVEL.values()[d]);
                } catch (NumberFormatException e) {
                    error(i18n.getString("Main.error.arg.invalid", Option.DETAIL.getKey()));
                }
            }
        }

        {
            String report = ao.getValue(Option.REPORT);
            if (report != null) {
                reporter.setReportfile(report);
            }
        }

        {
            List<String> excludes = ao.getValues(Option.EXCLUDE_LIST);
            if (excludes != null) {
                reporter.addXList(excludes.toArray(new String[]{}));
            }
        }

        debug = ao.isSet(Option.DEBUG);

        if (!callFilter.init(log)) {
            error(i18n.getString("Main.error.initfilter"));
        }

        if (ao.isSet(Option.HELP)) {
            version();
            usage();
            passed();
            return false;
        }

        if (ao.isSet(Option.VERSION)) {
            version();
            passed();
            return false;
        }

        return true;
    }

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        ao.readOptions(optionName, args);
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
        sb.append(nl).append(i18n.getString("Main.usage.ts", Option.TS));
        sb.append(nl).append(i18n.getString("Main.usage.tsInclude", Option.TS_ICNLUDE));
        sb.append(nl).append(i18n.getString("Main.usage.tsIncludeW", Option.TS_ICNLUDEW));
        sb.append(nl).append(i18n.getString("Main.usage.tsExclude", Option.TS_EXCLUDE));
        sb.append(nl).append(i18n.getString("Main.usage.api", Option.API));
        sb.append(nl).append(i18n.getString("Main.usage.apiInclude", Option.API_INCLUDE));
        sb.append(nl).append(i18n.getString("Main.usage.apiIncludeW", Option.API_INCLUDEW));
        sb.append(nl).append(i18n.getString("Main.usage.apiExclude", Option.API_EXCLUDE));
        sb.append(nl).append(i18n.getString("Main.usage.excludeList", Option.EXCLUDE_LIST));
        sb.append(nl).append(i18n.getString("Main.usage.excludeInterfaces",      Option.EXCLUDE_INTERFACES));
        sb.append(nl).append(i18n.getString("Main.usage.excludeAbstractClasses", Option.EXCLUDE_ABSTRACT_CLASSES));
        sb.append(nl).append(i18n.getString("Main.usage.excludeAbstractMethods", Option.EXCLUDE_ABSTRACT_METHODS));
        sb.append(nl).append(i18n.getString("Main.usage.excludeFields", Option.EXCLUDE_FIELDS));
        sb.append(nl).append(i18n.getString("Main.usage.includeConstantFields",  Option.INCLUDE_CONSTANT_FIELDS));
        sb.append(nl).append(i18n.getString("Main.usage.mode", Option.MODE));
        sb.append(nl).append(i18n.getString("Main.usage.detail", Option.DETAIL));
        sb.append(nl).append(i18n.getString("Main.usage.format", Option.FORMAT));
        sb.append(nl).append(i18n.getString("Main.usage.report", Option.REPORT));
        sb.append(nl).append(i18n.getString("Main.usage.debug", Option.DEBUG));
        sb.append(nl).append(i18n.getString("Main.usage.help", Option.HELP));
        sb.append(nl).append(i18n.getString("Main.usage.version", Option.VERSION));
        sb.append(nl).append(i18n.getString("Main.usage.end"));
        System.err.println(sb.toString());
    }

    void check() {
        FileManager f = new FileManager();
        MultipleFileReader in = new MultipleFileReader(log, MultipleFileReader.CLASSPATH_MODE, f);
        ClassHierarchy apiHierarchy = new ClassHierarchyImpl(in, ClassHierarchy.ALL_PUBLIC);
        new Adapter(f);

        try {

            if (!searachOnly()) {

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
                    List<MemberDescription> fCalls = tsLoader.loadCalls(name);
                    fCalls = callFilter.filterCalls(fCalls, name);
                    calls.addAll(fCalls);
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

            if (!searachOnly()) {
                if (size == 0) {
                    System.err.println(i18n.getString("Main.warning.ts.empty", ts));
                }

                reporter.out();
            }

        } catch (Throwable e) {
            debug(e);
            error(i18n.getString("Main.error.check", e.getMessage()));
        } finally {
            in.close();
        }
    }

    private boolean searachOnly() {
        return ao.getValue(Option.API) == null;
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
