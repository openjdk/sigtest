/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tdk.signaturetest.core.context.ModSetupOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ModuleDescription;
import com.sun.tdk.signaturetest.sigfile.*;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.OptionInfo;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

public class ModSetup extends ModBase {

    private ModSetupOptions mo = AppContext.getContext().getBean(ModSetupOptions.class);
    private String copyrightStr = null;
    private EnumSet<ModFeatures> features = EnumSet.of(ModFeatures.ALL);
    private WriteMode wm = WriteMode.SIGFILE;


    public ModSetup() {
    }

    /**
     * runs test in from command line.
     */
    public static void main(String[] args) {
        ModSetup t = new ModSetup();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    /**
     * runs test with the given arguments.
     */
    public void run(String[] args, PrintWriter pw, PrintWriter ref) {

        setLog(pw);

        if (parseParameters(args)) {
            create();
            getLog().flush();
        } else if (args.length > 0 && Option.VERSION.accept(args[0])) {
            pw.println(Version.getVersionInfo());
        } else {
            usage();
        }

    }


    /**
     * parses parameters and initialize fields as specified by arguments
     *
     * @param args contains arguments required to be parsed.
     */
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

        copyrightStr = mo.getValue(Option.COPYRIGHT);
        if (mo.getValue(Option.APIVERSION) != null) {
            apiVer = mo.getValue(Option.APIVERSION);
        }

        {
            String mode = mo.getValue(Option.MODE);
            if (mode != null) {
                if (WriteMode.STRUCT_CUMULATIVE.name().equalsIgnoreCase(mode)) {
                    wm = WriteMode.STRUCT_CUMULATIVE;
                } else if (WriteMode.STRUCT_PLAIN.name().equalsIgnoreCase(mode)) {
                    wm = WriteMode.STRUCT_PLAIN;
                }
            }
        }


        if (!processFeatureList(mo)) {
            return false;
        }

        if (mo.getValue(Option.TEST_URL) != null) {
            if (new File(mo.getValue(Option.FILE_NAME)).isAbsolute()) {
                return error(i18n.getString("MTest.error.testurl.absolutepath", new Object[]{Option.TEST_URL.getKey(), mo.getValue(Option.FILE_NAME)}));
            }
        }

        return true;

    }

    private boolean processFeatureList(ModSetupOptions mo) {
        try {
            features = ModFeatures.featureSetFromCommaList(mo.getValue(Option.FEATURES));
        } catch (IllegalArgumentException ex) {
            error(i18n.getString("Setup.error.wrongfeature", ex.getMessage()));
            return false;
        }
        return true;
    }

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        mo.readOptions(optionName, args);
    }

    /**
     * Prints help text.
     */
    protected void usage() {
        StringBuffer sb = new StringBuffer();
        sb.append(getComponentName()).append(" - ").append(i18n.getString("MSetup.usage.version", Version.Number));
        sb.append(i18n.getString("MSetup.usage"));
        System.err.println(sb.toString());
    }

    protected String getComponentName() {
        return "Mod-Setup";
    }

    /**
     * creates signature file.
     */
    private boolean create() {
        URL signatureFile;

        ModWriter writer = null;

        ModuleDescriptionLoader mdl = getModuleLoader();
        HashMap<String, ModuleDescription> model = new HashMap<>();
        {
            Set<ModuleDescription> modules = mdl.loadBootModules();
            for (ModuleDescription md : modules) {
                md.setFeatures(features);
                model.put(md.getName(), md);
            }
        }
        filterModuleSet(model, true, mo.isSet(Option.DEBUG));

        if (wm == WriteMode.STRUCT_PLAIN || wm == WriteMode.STRUCT_CUMULATIVE) {
            if ( new StructWriter().createStructFile(wm, model, mdl.loadBootModules()) ) {
                return passed();
            } else {
                return failed("");
            }
        }

        try (Writer w = getFileManager().getDefaultFormat().getWriter();
             FileOutputStream fos = new FileOutputStream(FileManager.getURL(mo.getValue(Option.TEST_URL), mo.getValue(Option.FILE_NAME)).getFile());
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            if (w instanceof ModWriter) {
                writer = (ModWriter) w;
            } else {
                throw new IllegalStateException();
            }

            writer.init(new PrintWriter(osw));
            writer.addFeature(FeaturesHolder.ConstInfo);
            writer.addFeature(FeaturesHolder.TigerInfo);
            writer.addFeature(FeaturesHolder.ModuleInfo);


            if (apiVer != null) {
                writer.setApiVersion(apiVer);
            }

            if (copyrightStr != null) {
                writer.addFeature(FeaturesHolder.CopyRight);
                FeaturesHolder.CopyRight.setText("# " + copyrightStr);
            }

            writer.writeHeader();
            for (ModuleDescription md : model.values()) {
                writer.write(md);
            }
        } catch (IOException e) {
            if (mo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            getLog().println(i18n.getString("Setup.error.message.cantcreatesigfile"));
            getLog().println(e);
            return error(i18n.getString("Setup.error.message.cantcreatesigfile"));
        }

        return passed();
    }


    public enum WriteMode {
        SIGFILE, STRUCT_CUMULATIVE, STRUCT_PLAIN
    }


}
