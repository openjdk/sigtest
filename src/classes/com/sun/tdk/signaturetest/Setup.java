/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.classpath.Classpath;
import com.sun.tdk.signaturetest.classpath.ClasspathImpl;
import com.sun.tdk.signaturetest.core.*;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.plugin.Filter;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.OptionInfo;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class creates signature file. The classes in the signature file are
 * grouped by type, and alphabetized by class name.<br> The following signature
 * files could be created:
 * Usage: java com.sun.tdk.signaturetest.setup.Setup &lt;options&gt;
 * <p>
 * where &lt;options&gt; includes:
 * <dl> <dt><code><b>-TestURL</b></code> &lt;URL&gt; <dd> URL of signature file.
 * <dt><code><b>-FileName</b></code> &lt;n&gt; <dd> Path name of signature file
 * name.
 * <dt><code><b>-Package</b></code> &lt;package name&gt; <dd> Package which are
 * needed to be tracked (several options can be specified).
 * <dt><code><b>-PackageWithoutSubpackages</b></code> &lt;package&gt; <dd> Name
 * of the package, which is to be tracked itself excluding its subpackages. Such
 * option should be included for each package required to be tracked excluding
 * subpackages.
 * <dt><code><b>-Exclude</b></code> &lt;package or class name&gt; <dd> package
 * or class which is not needed to be tracked.(several options can be specified)
 * <dt><code><b>-static</b></code> <dd> Track in the static mode. In this mode
 * test uses class file parsing instead of the reflection for. The path
 * specified by -Classpath options is required in this mode.
 * <dt><code><b>-CheckValue</b></code> <dd> Writes values of the primitive
 * constants in signature file. This options could be used in the static mode
 * only.
 * <dt><code><b>-AllPublic</b></code> <dd> track unaccessible nested classes
 * (I.e. which are public or protected but are members of default or private
 * access class).
 * <dt><code><b>-Classpath</b></code> &lt;path&gt; <dd> specify the path, which
 * includes tracked classes.
 * <dt><code><b>-Version</b></code> &lt;version&gt; <dd> Specify API version. If
 * this parameter is not specified, API version is assumed to be that reported
 * by <code>getProperty("java.version")</code>.
 * <dt><code><b>-Verbose</b></code> <dd> Print names of ignored classes. </dl>
 *
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 * @author Mikhail Ershov
 */
public class Setup extends SigTest {

    // Setup specific options
    public static final String CLOSEDFILE_OPTION = "-ClosedFile";
    public static final String NONCLOSEDFILE_OPTION = "-NonClosedFile";
    public static final String CHECKVALUE_OPTION = "-CheckValue";
    public static final String XGENCONSTS_OPTION = "-XgenConsts";
    public static final String COPYRIGHT_OPTION = "-CopyRight";
    // -KeepFile option keeps signature file even if some error occured during setup
    // needs for compatibility between 2.0 and 2.1
    public static final String KEEP_SIGFILE_OPTION = "-KeepFile";
    // This option is used only for debugging purposes. It's not recommended
    // to use it to create signature files for production!
    public static final String XREFLECTION_OPTION = "-Xreflection";
    /**
     * contains signature file.
     */
    protected URL signatureFile;
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Setup.class);
    protected boolean isClosedFile = true;
    private Boolean explicitlyGenConsts = null;
    private boolean keepSigFile = false;
    private String copyrightStr = null;

    /**
     * runs test in from command line.
     */
    public static void main(String[] args) {
        Setup t = new Setup();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    /**
     * runs test with the given arguments.
     */
    public void run(String[] args, PrintWriter pw, PrintWriter ref) {

        setLog(pw);

        outerClassesNumber = 0;
        innerClassesNumber = 0;
        includedClassesNumber = 0;
        excludedClassesNumber = 0;

        MemberType.setMode(false);

        if (parseParameters(args)) {
            afterParseParameters();
            create(signatureFile);
            getLog().flush();
        } else if (args.length > 0 && Option.VERSION.accept(args[0])) {
            pw.println(Version.getVersionInfo());
        }
    }

    /**
     * parses parameters and initialize fields as specified by arguments
     *
     * @param args contains arguments required to be parsed.
     */
    protected boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");
        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);


        // Print help text only and exit.
        if (args == null || args.length == 0
                || (args.length == 1 && Option.VERSION.accept(args[0]))) {
            return false;
        }

        final String optionsDecoder = "decodeOptions";

        parser.addOption(APIVERSION_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(CLASSCACHESIZE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(XNOTIGER_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(XVERBOSE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(SigTest.VERBOSE_OPTION, OptionInfo.optionVariableParams(0, 1), optionsDecoder);
        parser.addOption(CLOSEDFILE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(NONCLOSEDFILE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(KEEP_SIGFILE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(CHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(XGENCONSTS_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(XREFLECTION_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(PLUGIN_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(ERRORALL_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(COPYRIGHT_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOptions(bo.getOptions(), optionsDecoder);

        try {
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            getLog().println(e.getMessage());
            return failed(e.getMessage());
        }

        if (!processHelpOptions()) {
            return false;
        }

        // since 2.1 - static mode by default
        if (!parser.isOptionSpecified(XREFLECTION_OPTION)) {
            bo.readOptions(Option.STATIC.getKey(), null);
        }

        if (parser.isOptionSpecified(NONCLOSEDFILE_OPTION) && parser.isOptionSpecified(CLOSEDFILE_OPTION)) {
            return error(i18n.getString("Setup.error.mode.contradict", new Object[]{NONCLOSEDFILE_OPTION, CLOSEDFILE_OPTION}));
        }

        packages.addPackages(bo.getValues(Option.PACKAGE));
        purePackages.addPackages(bo.getValues(Option.PURE_PACKAGE));
        excludedPackages.addPackages(bo.getValues(Option.EXCLUDE));
        apiIncl.addPackages(bo.getValues(Option.API_INCLUDE));
        apiExcl.addPackages(bo.getValues(Option.API_EXCLUDE));

        // create arguments
        if (packages.isEmpty() && purePackages.isEmpty() && apiIncl.isEmpty()) {
            packages.addPackage("");
        }

        if (bo.getValue(Option.FILE_NAME) == null) {
            return error(i18n.getString("MTest.error.filename.missing"));
        }

        if (bo.getValue(Option.TEST_URL) != null) {
            if (new File(bo.getValue(Option.FILE_NAME)).isAbsolute()) {
                return error(i18n.getString("MTest.error.testurl.absolutepath", new Object[]{Option.TEST_URL.getKey(), bo.getValue(Option.FILE_NAME)}));
            }
        }

        try {
            signatureFile = FileManager.getURL(bo.getValue(Option.TEST_URL), bo.getValue(Option.FILE_NAME));
        } catch (MalformedURLException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            System.err.println(e);
            return error(i18n.getString("Setup.error.url.invalid"));
        }

        if (bo.getValue(Option.CLASSPATH) == null) {
            return error(i18n.getString("Setup.error.arg.unspecified", Option.CLASSPATH.getKey()));
        }

        return passed();
    }

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        if (optionName.equalsIgnoreCase(CLOSEDFILE_OPTION)) {
            isClosedFile = true;
        } else if (optionName.equalsIgnoreCase(NONCLOSEDFILE_OPTION)) {
            isClosedFile = false;
        } else if (optionName.equalsIgnoreCase(KEEP_SIGFILE_OPTION)) {
            keepSigFile = true;
        } else if (optionName.equalsIgnoreCase(CHECKVALUE_OPTION)) {
            // do nothing, just for back. comp.
        } else if (optionName.equalsIgnoreCase(XGENCONSTS_OPTION)) {
            String v = args[0];
            if ("on".equalsIgnoreCase(v)) {
                explicitlyGenConsts = Boolean.TRUE;
            } else if ("off".equalsIgnoreCase(v)) {
                explicitlyGenConsts = Boolean.FALSE;
            } else {
                throw new CommandLineParserException(i18n.getString("Setup.error.arg.invalidval", XGENCONSTS_OPTION));
            }
        } else if (optionName.equalsIgnoreCase(COPYRIGHT_OPTION)) {
            copyrightStr = args[0];
        } else {
            super.decodeCommonOptions(optionName, args);
        }
    }

    private void afterParseParameters() {
        if (explicitlyGenConsts != null) {
            setConstantValuesTracked(explicitlyGenConsts);
        } else {
            BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
            setConstantValuesTracked(bo.isSet(Option.STATIC));
        }
    }

    /**
     * Prints help text.
     */
    protected void usage() {
        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();

        sb.append(getComponentName()).append(" - ").append(i18n.getString("Setup.usage.version", Version.Number));
        sb.append(nl).append(i18n.getString("Setup.usage.start"));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("Setup.usage.classpath", Option.CLASSPATH));
        sb.append(nl).append(i18n.getString("Setup.usage.package", Option.PACKAGE));
        sb.append(nl).append(i18n.getString("Setup.usage.filename", Option.FILE_NAME));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));

        sb.append(nl).append(i18n.getString("Setup.usage.testurl", Option.TEST_URL));
        sb.append(nl).append(i18n.getString("Setup.usage.packagewithoutsubpackages", Option.PURE_PACKAGE));
        sb.append(nl).append(i18n.getString("Setup.usage.exclude", Option.EXCLUDE));
        sb.append(nl).append(i18n.getString("Setup.usage.nonclosedfile", NONCLOSEDFILE_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.apiversion", APIVERSION_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("Setup.usage.verbose", new Object[]{VERBOSE_OPTION, NOWARN}));
        sb.append(nl).append(i18n.getString("Setup.usage.debug", Option.DEBUG));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("Setup.helpusage.version", Option.VERSION));
        sb.append(nl).append(i18n.getString("Setup.usage.help", Option.HELP));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("Setup.usage.end"));
        System.err.println(sb.toString());
    }

    protected String getComponentName() {
        return "Setup";
    }
    private int outerClassesNumber = 0,
            innerClassesNumber = 0,
            includedClassesNumber = 0,
            excludedClassesNumber = 0;

    /**
     * creates signature file.
     */
    private boolean create(URL sigFile) {
        initErrors();

        if (pluginClass != null) {
            pluginClass.init(this);
        }

        // create list of all classes available
        HashSet<String> allClasses = new HashSet<>();
        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        getLog().println(i18n.getString("Setup.log.classpath", bo.getValue(Option.CLASSPATH)));

        try {
            setClasspath(new ClasspathImpl(bo.getValue(Option.CLASSPATH)));
        } catch (SecurityException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            getLog().println(i18n.getString("Setup.log.invalid.security.classpath"));
            getLog().println(e);
            return error(i18n.getString("Setup.log.invalid.security.classpath"));
        }

        getClasspath().printErrors(getLog());

        SortedSet<String> excludedClasses = new TreeSet<>();

        try(Classpath cp = getClasspath()) {
            while (cp.hasNext()) {
                String name = cp.nextClassName();
                if (!allClasses.add(name)) {
                    getLog().println(i18n.getString("Setup.log.duplicate.class", name));
                }
            }

            cp.setListToBegin();

            ClassDescriptionLoader testableLoader = getClassDescrLoader();
            testableHierarchy = new ClassHierarchyImpl(testableLoader);
            testableMCBuilder = new MemberCollectionBuilder(this, "source:setup");

            // adds classes which are member of classes from tracked package
            // and sorts class names
            getLog().println(i18n.getString("Setup.log.constantchecking",
                    isConstantValuesTracked() ? i18n.getString("Setup.msg.ConstantValuesTracked.on")
                            : i18n.getString("Setup.msg.ConstantValuesTracked.off")));
            getLog().println(i18n.getString("Setup.log.message.numclasses", Integer.toString(allClasses.size())));

            List<String> sortedClasses;
            Collection<String> packageClasses = getPackageClasses(allClasses);

            if (isClosedFile) {
                ClassSet closedSetOfClasses = new ClassSet(testableHierarchy, true);

                // add all classes including non-accessible
                for (String name : packageClasses) {
                    closedSetOfClasses.addClass(name);
                }
                // remove not accessible classes

                Set<String> invisibleClasses = new HashSet<>();
                Set<String> classes = closedSetOfClasses.getClasses();
                for (String name : classes) {
                    ClassDescription c = load(name);

                    if (!testableHierarchy.isAccessible(c)) {
                        invisibleClasses.add(name);
                    }
                }

                for (String invisibleClass : invisibleClasses) {
                    closedSetOfClasses.removeClass(invisibleClass);
                }

                sortedClasses = sortClasses(closedSetOfClasses.getClasses());
            } else {
                sortedClasses = sortClasses(packageClasses);
            }

            try (Writer writer = getFileManager().getDefaultFormat().getWriter();
                 FileOutputStream fos = new FileOutputStream(sigFile.getFile());
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                //write header to the signature file

                writer.init(new PrintWriter(osw));

                writer.setApiVersion(apiVersion);
                if (isConstantValuesTracked()) {
                    writer.addFeature(FeaturesHolder.ConstInfo);
                }

                if (isTigerFeaturesTracked) {
                    writer.addFeature(FeaturesHolder.TigerInfo);
                }

                if (copyrightStr != null) {
                    FeaturesHolder.CopyRight.setText("# " + copyrightStr);
                    writer.addFeature(FeaturesHolder.CopyRight);
                }

                writer.writeHeader();

                Erasurator erasurator = new Erasurator();
                // scan class and writes definition to the signature file

                // 1st analyze all the classes
                for (String name : sortedClasses) {
                    ClassDescription c = load(name);

                    if (!testableHierarchy.isAccessible(c)) {
                        continue;
                    }

                    // do not write excluded classes
                    if (excludedPackages.checkName(name) || apiExcl.checkName(name)) {
                        excludedClasses.add(name);
                        continue;
                    }

                    if (name.indexOf('$') < 0) {
                        outerClassesNumber++;
                    } else {
                        innerClassesNumber++;
                    }

                    try {
                        testableMCBuilder.createMembers(c, addInherited(), true, false);
                        normalizer.normThrows(c, true);
                        removeUndocumentedAnnotations(c, testableHierarchy);
                    } catch (ClassNotFoundException e) {
                        if (bo.isSet(Option.DEBUG)) {
                            SwissKnife.reportThrowable(e);
                        }
                        setupProblem(i18n.getString("Setup.error.message.classnotfound", e.getMessage()));
                    }

                    if (useErasurator()) {
                        c = erasurator.erasure(c);
                    }

                    Transformer t = PluginAPI.BEFORE_WRITE.getTransformer();
                    if (t != null) {
                        try {
                            c = t.transform(c);
                        } catch (ClassNotFoundException ex) {
                            // nothing
                        }
                    }

                    Filter f = PluginAPI.BEFORE_WRITE.getFilter();
                    if (f == null || f.accept(c)) {
                        writer.write(c);
                    }
                }

            } catch (IOException e) {
                if (bo.isSet(Option.DEBUG)) {
                    SwissKnife.reportThrowable(e);
                }
                getLog().println(i18n.getString("Setup.error.message.cantcreatesigfile"));
                getLog().println(e);
                return error(i18n.getString("Setup.error.message.cantcreatesigfile"));
            }
        } // cp t-w-r

        printErrors();

        // prints report
        getLog().println(i18n.getString("Setup.report.message.selectedbypackageclasses",
                Integer.toString(includedClassesNumber + excludedClassesNumber)));

        if (!excludedPackages.isEmpty() || !apiExcl.isEmpty()) {
            getLog().println(i18n.getString("Setup.report.message.excludedbypackageclasses",
                    Integer.toString(excludedClassesNumber)));
        }

        // print warnings
        if (isClosedFile && excludedClasses.size() != 0) {

            boolean printHeader = true;

            for (String clsName : excludedClasses) {

                String[] subClasses = testableHierarchy.getDirectSubclasses(clsName);

                if (subClasses.length > 0) {

                    int count = 0;
                    for (String subClass : subClasses) {
                        if (!excludedClasses.contains(subClass)) {

                            if (count != 0) {
                                getLog().print(", ");
                            } else {
                                if (printHeader) {
                                    getLog().println(i18n.getString("Setup.log.message.exclude_warning_header"));
                                    printHeader = false;
                                }
                                getLog().println(i18n.getString("Setup.log.message.exclude_warning", clsName));
                            }

                            getLog().print(subClass);
                            ++count;
                        }
                    }
                    getLog().println();
                }
            }
        }

        getLog().print(i18n.getString("Setup.report.message.outerclasses", Integer.toString(outerClassesNumber)));
        if (innerClassesNumber != 0) {
            getLog().println(i18n.getString("Setup.report.message.innerclasses", Integer.toString(innerClassesNumber)));
        } else {
            getLog().println();
        }

        if (errors == 0) {
            return passed(outerClassesNumber == 0 ? i18n.getString("Setup.report.message.emptysigfile") : "");
        }

        if (!keepSigFile) {
            new File(sigFile.getFile()).delete();
        }
        return failed(i18n.getString("Setup.report.message.numerrors", Integer.toString(errors)));
    }

    private void removeUndocumentedAnnotations(ClassDescription c, ClassHierarchy classHierarchy) {
        c.setAnnoList(removeUndocumentedAnnotations(c.getAnnoList(), classHierarchy));
        for (Iterator<MemberDescription> e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = e.next();
            mr.setAnnoList(removeUndocumentedAnnotations(mr.getAnnoList(), classHierarchy));
        }
    }

    /**
     * initialize table of the nested classes and returns Vector of the names
     * required to be tracked.
     */
    private Collection<String> getPackageClasses(Collection<String> classes) {
        HashSet<String> packageClasses = new HashSet<>();
        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        int nonTigerCount = 0;

        // create table of the nested packageClasses.
        for (String name : classes) {

            if (isPackageMember(name)) {
                includedClassesNumber++;
                try {
                    ClassDescription c = testableHierarchy.load(name);
                    if (testableHierarchy.isAccessible(c)) {
                        packageClasses.add(name);
                        if (!c.isTiger()) {
                            nonTigerCount++;
                            if (Xverbose && isTigerFeaturesTracked) {
                                getLog().println(i18n.getString("Setup.report.message.nontigerclass", name));
                            }
                        }
                    } else {
                        ignore(i18n.getString("Setup.report.ignore.protect", name));
                    }
                } catch (ClassNotFoundException ex) {
                    if (bo.isSet(Option.DEBUG)) {
                        SwissKnife.reportThrowable(ex);
                    }
                    setupProblem(i18n.getString("Setup.error.message.classnotfound", name));
                } catch (LinkageError ex1) {
                    if (bo.isSet(Option.DEBUG)) {
                        SwissKnife.reportThrowable(ex1);
                    }
                    setupProblem(i18n.getString("Setup.error.message.classnotlinked", ex1.getMessage()));
                }
            } else {
                if (!excludedPackages.isEmpty() && excludedPackages.checkName(name)) {
                    excludedClassesNumber++;
                } else if (!apiExcl.isEmpty() && apiExcl.checkName(name)) {
                    excludedClassesNumber++;
                }
                ignore(i18n.getString("Setup.report.ignore.notreqpackage", name));
            }
        }

        return packageClasses;
    }

    /**
     * returns list of the sorted classes.
     *
     * @param classes MemberCollection which stores occurred errors. *
     */
    private List<String> sortClasses(Collection<String> classes) {
        ArrayList<String> retVal = new ArrayList<>(classes);
        Collections.sort(retVal);
        return retVal;
    }

    /**
     * ignore class with given message.
     *
     * @param message given message.
     */
    protected void ignore(String message) {
        if (isVerbose) {
            getLog().println(message);
        }
    }
}
