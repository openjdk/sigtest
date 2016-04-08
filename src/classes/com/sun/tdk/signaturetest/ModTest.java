/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tdk.signaturetest.core.ModuleDescriptionLoader;
import com.sun.tdk.signaturetest.core.context.ModFeatures;
import com.sun.tdk.signaturetest.core.context.ModTestOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ModuleDescription;
import com.sun.tdk.signaturetest.sigfile.MultipleFileReader;
import com.sun.tdk.signaturetest.sigfile.f43.F43Reader;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.OptionInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

public class ModTest extends ModBase {

    private ModTestOptions mo = AppContext.getContext().getBean(ModTestOptions.class);
    private EnumSet<ModFeatures> checkers = EnumSet.of(ModFeatures.REQUIRES_PUBLIC, ModFeatures.EXPORTS_PUBLIC);

    public ModTest() {
    }

    public static void main(String[] args) {
        ModTest t = new ModTest();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    public boolean run(String[] args, PrintWriter pw, PrintWriter ref) {

        setLog(pw);

        Map<String, ModuleDescription> thisModel;
        Map<String, ModuleDescription> fileModel;

        if (parseParameters(args)) {
            thisModel = filterModuleSet(createModel(), false, mo.isSet(Option.DEBUG));
            fileModel = filterModuleSet(readFile(), true, mo.isSet(Option.DEBUG));
            passed();
            check(thisModel, fileModel);
            errorFormatter.out(pw);

            getLog().flush();
        } else if (args.length > 0 && Option.VERSION.accept(args[0])) {
            pw.println(Version.getVersionInfo());
        } else {
            usage();
        }

        if (errorFormatter.getNunErrors() == 0) {
            if (isPassed()) {
                return passed();
            } else {
                return false;
            }
        } else {
            return failed(i18n.getString("MTest.msg.failed",
                    Integer.toString(errorFormatter.getNunErrors())));
        }

    }

    private boolean check(Map<String, ModuleDescription> thisModel, Map<String, ModuleDescription> thatModel) {

        boolean result = checkModuleList(thisModel, thatModel);

        for (String mN : thisModel.keySet()) {

            ModuleDescription thisModule = thisModel.get(mN);
            ModuleDescription thatModule = thatModel.get(mN);

            if (thatModule == null) {
                continue;
            }

            result &= checkVersion(thisModule, thatModule);
            result &= checkMainClass(thisModule, thatModule);
            result &= checkPackages(thisModule, thatModule);
            result &= checkConceals(thisModule, thatModule);
            result &= checkExports(thisModule, thatModule);

            result &= checkRequires(thisModule, thatModule);
            result &= checkServices(thisModule, thatModule);
            result &= checkUses(thisModule, thatModule);
        }
        return result;
    }

    private boolean checkModuleList(Map<String, ModuleDescription> thisModel, Map<String, ModuleDescription> thatModel) {
        Set<String> onlyHere = new HashSet<>(thisModel.keySet());
        onlyHere.removeAll(thatModel.keySet());

        Set<String> onlyThere = new HashSet<>(thatModel.keySet());
        onlyThere.removeAll(thisModel.keySet());

        boolean result = true;

        for (String mName : onlyHere) {
            // i18n "Extra module %s found\n"
            errorFormatter.addError(i18n.getString("MTest.error.module.extra"), mName);
            result = false;
        }

        for (String mName : onlyThere) {
            // i18n "Required module %s is not found\n"
            errorFormatter.addError(i18n.getString("MTest.error.module.not.found"), mName);
            result = false;
        }

        return result;
    }

    private boolean checkMainClass(ModuleDescription thisModule, ModuleDescription thatModule) {
        if (!supports(ModFeatures.MAIN_CLASS)) return true;
        if (!assertChecker(ModFeatures.MAIN_CLASS, thatModule)) return false;
        String s1 = thisModule.getMainClass();
        String s2 = thatModule.getMainClass();
        if (s1 == null ? s2 == null : s1.equals(s2)) {
            return true;
        } else {
            if (s1 == null || s1.isEmpty()) s1="n/a";
            if (s2 == null || s2.isEmpty()) s2="n/a";

            // i18n "Module %s - different main class: %s and %s\n"
            errorFormatter.addError(i18n.getString("MTest.error.main.class"),
                    thatModule.getName(), s1, s2);
            return false;
        }
    }

    private boolean checkVersion(ModuleDescription thisModule, ModuleDescription thatModule) {
        if (!supports(ModFeatures.VERSION)) return true;
        if (!assertChecker(ModFeatures.VERSION, thatModule)) return false;
        String s1 = thisModule.getVersion();
        String s2 = thatModule.getVersion();
        if (s1 == null ? s2 == null : s1.equals(s2)) {
            return true;
        } else {
            if (s1 == null || s1.isEmpty()) s1="n/a";
            if (s2 == null || s2.isEmpty()) s2="n/a";

            // i18n "Module %s - different versions: %s and %s\n"
            errorFormatter.addError(i18n.getString("MTest.error.version"),
                    thatModule.getName(), s1, s2);
            return false;
        }

    }

    private boolean checkPackages(ModuleDescription thisModule, ModuleDescription thatModule) {
        if (!supports(ModFeatures.PACKAGES)) return true;
        if (!assertChecker(ModFeatures.PACKAGES, thatModule)) return false;
        return compareStringSets(thisModule, thisModule.getPackages(), thatModule.getPackages(), "package");
    }

    private boolean checkConceals(ModuleDescription thisModule, ModuleDescription thatModule) {
        if (!supports(ModFeatures.CONCEAL)) return true;
        if (!assertChecker(ModFeatures.CONCEAL, thatModule)) return false;
        return compareStringSets(thisModule, thisModule.getConceals(), thatModule.getConceals(), "conceal");
    }


    private boolean checkExports(ModuleDescription thisModule, ModuleDescription thatModule) {

        if (!supports(ModFeatures.EXPORTS_PUBLIC) && !supports(ModFeatures.EXPORTS_ALL)) return true;

        boolean checkAll = (checkers.contains(ModFeatures.EXPORTS_ALL) || checkers.contains(ModFeatures.ALL));

        if (checkAll) {
            if (!assertChecker(ModFeatures.EXPORTS_ALL, thatModule)) return false;
        } else {
            if (!assertChecker(ModFeatures.EXPORTS_PUBLIC, thatModule)) return false;
        }

        boolean retVal = true;


        Set<ModuleDescription.Exports> thisExports = thisModule.getExports();
        Set<ModuleDescription.Exports> thatExports = thatModule.getExports();

        for (ModuleDescription.Exports thisEx : thisExports) {
            ModuleDescription.Exports thatEx = null;

            if (!checkAll && !thisEx.targets.isEmpty()) continue;

            for (ModuleDescription.Exports t : thatExports) {
                if (t.source.equals(thisEx.source)) {
                    thatEx = t;
                    break;
                }
            }
            if (thatEx == null) {
                // i18n "Extra export %s found in module %s\n"
                errorFormatter.addError(i18n.getString("MTest.error.export.not.found"),
                        thisEx.source, thisModule.getName());
                continue;
            }
            if (!thisEx.targets.equals(thatEx.targets)) {
                // i18n "Different targets for export %s in module %s: %s and %s\n"
                errorFormatter.addError(i18n.getString("MTest.error.export.targets"),
                        thisEx.source, thisModule.getName(),
                        thisEx.targets.toString(), thatEx.targets.toString());
                continue;
            }
        }

        for (ModuleDescription.Exports thatEx : thatExports) {
            ModuleDescription.Exports thisEx = null;

            if (!checkAll && !thatEx.targets.isEmpty()) continue;

            for (ModuleDescription.Exports t : thisExports) {
                if (t.source.equals(thatEx.source)) {
                    thisEx = t;
                    break;
                }
            }
            if (thisEx == null) {
                // i18n "Missing export %s found in module %s\n"
                errorFormatter.addError(i18n.getString("MTest.error.export.missing"),
                        thatEx.source, thatModule.getName());
                continue;
            }
        }

        return retVal;
    }

    private boolean checkRequires(ModuleDescription thisModule, ModuleDescription thatModule) {
        if (!supports(ModFeatures.REQUIRES_PUBLIC) && !supports(ModFeatures.REQUIRES_ALL)) return true;

        boolean checkAll = (checkers.contains(ModFeatures.REQUIRES_ALL) || checkers.contains(ModFeatures.ALL));

        if (checkAll) {
            if (!assertChecker(ModFeatures.REQUIRES_ALL, thatModule)) return false;
        } else {
            if (!assertChecker(ModFeatures.REQUIRES_PUBLIC, thatModule)) return false;
        }

        Set<String> thisPublicRequires = new HashSet<>();
        for (ModuleDescription.Requires rq : thisModule.getRequires()) {
            if (!checkAll && !rq.modifiers.contains(ModuleDescription.Requires.Modifier.PUBLIC)) {
                continue;
            }
            thisPublicRequires.add(rq.getName());
        }

        Set<String> thatPublicRequires = new HashSet<>();
        for (ModuleDescription.Requires rq : thatModule.getRequires()) {
            if (!checkAll && !rq.modifiers.contains(ModuleDescription.Requires.Modifier.PUBLIC)) {
                continue;
            }
            thatPublicRequires.add(rq.getName());
        }
        return compareStringSets(thisModule, thisPublicRequires, thatPublicRequires, checkAll ? "requires" : "requires public");
    }

    private boolean checkServices(ModuleDescription thisModule, ModuleDescription thatModule) {
        if (!supports(ModFeatures.SERVICES)) return true;
        if (!assertChecker(ModFeatures.SERVICES, thatModule)) return false;

        Set<String> thisProvides = thisModule.getProvides().keySet();
        Set<String> thatProvides = thatModule.getProvides().keySet();
        return compareStringSets(thisModule, thisProvides, thatProvides, "service");
    }

    private boolean checkUses(ModuleDescription thisModule, ModuleDescription thatModule) {
        if (!supports(ModFeatures.USES)) return true;
        if (!assertChecker(ModFeatures.USES, thatModule)) return false;

        Set<String> thisUses = thisModule.getUses();
        Set<String> thatUses = thatModule.getUses();
        return compareStringSets(thisModule, thisUses, thatUses, "uses");
    }

    private boolean supports(ModFeatures checker) {
        return checkers.contains(ModFeatures.ALL) || checkers.contains(checker);
    }

    private boolean assertChecker(ModFeatures checker, ModuleDescription thatModule) {
        assert checker != null;
        assert thatModule.getFeatures() != null;
        if (!thatModule.getFeatures().contains(ModFeatures.ALL) &&
                !thatModule.getFeatures().contains(checker)) {
            errorFormatter.addError(i18n.getString("MTest.checker.not.supported"), thatModule.getName(), checker.name());
            return false;
        }
        return true;
    }

    private boolean compareStringSets(ModuleDescription thisModule, Set<String> thisSet, Set<String> thatSet, String objName) {
        boolean retVal = true;
        if (!thisSet.equals(thatSet)) {

            Set<String> onlyHere = new HashSet<>(thisSet);
            onlyHere.removeAll(thatSet);

            Set<String> onlyThere = new HashSet<>(thatSet);
            onlyThere.removeAll(thisSet);

            for (String sName : onlyHere) {
                // i18n "Extra %s %s provided by module %s\n"
                errorFormatter.addError(i18n.getString("MTest.error.extra.entity"),
                        objName, sName, thisModule.getName());
                retVal = false;
            }
            for (String sName : onlyThere) {
                // i18n "Required %s %s is not provided by module %s\n"
                errorFormatter.addError(i18n.getString("MTest.error.missing.entity"),
                        objName, sName, thisModule.getName());
                retVal = false;
            }
        }
        return retVal;
    }

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        mo.readOptions(optionName, args);
    }


    private Map<String, ModuleDescription> readFile() {

        MultipleFileReader in = new MultipleFileReader(getLog(), MultipleFileReader.CLASSPATH_MODE, getFileManager());
        String testUrl = "";
        if (mo.getValue(Option.TEST_URL) != null) {
            testUrl = mo.getValue(Option.TEST_URL);
        }
        in.readSignatureFile(testUrl, mo.getValue(Option.FILE_NAME));

        Map<String, ModuleDescription> modules = new HashMap<>();
        List<Document> docs = in.getDocuments();
        if (docs != null) {
            for (Document d : docs) {
                Element m = d.getDocumentElement();
                ModuleDescription md = F43Reader.fromDom(m);
                modules.put(md.getName(), md);
            }
        }

        return modules;
    }

    private Map<String, ModuleDescription> createModel() {
        ModuleDescriptionLoader mdl = getModuleLoader();
        Set<ModuleDescription> modules = mdl.loadBootModules();
        Map<String, ModuleDescription> model = new HashMap<>();
        for (ModuleDescription md : modules) {
            model.put(md.getName(), md);
        }
        return model;
    }

    protected boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");


        // Print help text only and exit.
        if (args == null || args.length == 0
                || (args.length == 1 && Option.VERSION.accept(args[0]))) {
            return false;
        }

        final String optionsDecoder = "decodeOptions";

        parser.addOption(SigTest.VERBOSE_OPTION, OptionInfo.optionVariableParams(0, 1), optionsDecoder);
        parser.addOptions(mo.getOptions(), optionsDecoder);

        try {
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            getLog().println(e.getMessage());
            return failed(e.getMessage());
        }

        if (!processHelpOptions()) {
            return false;
        }

        setupModulesAndPackages(mo);

        if (mo.getValue(Option.FILE_NAME) == null) {
            return error(i18n.getString("MTest.error.filename.missing"));
        }

        if (mo.getValue(Option.APIVERSION) != null) {
            apiVer = mo.getValue(Option.APIVERSION);
        }

        if (mo.getValue(Option.TEST_URL) != null) {
            if (new File(mo.getValue(Option.FILE_NAME)).isAbsolute()) {
                return error(i18n.getString("MTest.error.testurl.absolutepath", new Object[]{Option.TEST_URL.getKey(), mo.getValue(Option.FILE_NAME)}));
            }
        }

        if (!processCheckersList(mo)) {
            return false;
        }

        return true;
    }

    private boolean processCheckersList(ModTestOptions mo) {
        try {
            checkers = ModFeatures.featureSetFromCommaList(mo.getValue(Option.CHECKS));
        } catch (IllegalArgumentException ex) {
            error(i18n.getString("MTest.error.wrongcheck", ex.getMessage()));
            return false;
        }
        return true;
    }


    @Override
    protected void usage() {
        StringBuffer sb = new StringBuffer();
        sb.append(getComponentName() + " - " + i18n.getString("MTest.usage.version", Version.Number));
        sb.append(i18n.getString("MTest.usage"));
        System.err.println(sb.toString());
    }

    @Override
    protected String getComponentName() {
        return "Mod-Test";
    }
}
