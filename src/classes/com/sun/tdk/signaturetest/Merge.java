/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.core.*;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.MergeOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.loaders.VirtualClassDescriptionLoader;
import com.sun.tdk.signaturetest.merge.JSR68Merger;
import com.sun.tdk.signaturetest.merge.MergedSigFile;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.sigfile.*;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.util.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Merge extends SigTest {

    // Command line options
    private static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Merge.class);

    /**
     * Run the test using command-line; return status via numeric exit code.
     *
     * @see #run(String[], PrintWriter, PrintWriter)
     */
    public static void main(String[] args) {
        Merge m = getInstance();
        m.run(args, new PrintWriter(System.err, true), null);
        m.exit();
    }

    protected static Merge getInstance() {
        return new Merge();
    }

    /**
     * This is the gate to run the test with the JavaTest application.
     *
     * @param pw  This log-file is used for error messages.
     * @param ref This reference-file is ignored here.
     * @see #main(String[])
     */
    public void run(String[] args, PrintWriter pw, PrintWriter ref) {

        setLog(pw);

        if (parseParameters(args)) {
            perform();
            getLog().flush();
        } else if (args.length > 0 && Option.VERSION.accept(args[0])) {
            System.err.println(Version.getVersionInfo());
        } else {
            usage();
        }
    }

    private boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");
        initErrors();

        // Print help text only and exit.
        if (args == null || args.length == 0 || Option.HELP.accept(args[0])) {
            return false;
        }

        final String optionsDecoder = "decodeOptions";
        MergeOptions mo = AppContext.getContext().getBean(MergeOptions.class);
        parser.addOptions(mo.getOptions(), optionsDecoder);

        try {
            parser.processArgs(args);
            if (mo.getValue(Option.WRITE) != null) {
                checkValidWriteFile();
            }
        } catch (CommandLineParserException e) {
            getLog().println(e.getMessage());
            return failed(e.getMessage());
        }

        return passed();
    }

    private void checkValidWriteFile() throws CommandLineParserException {
        File canonicalFile = null;
        MergeOptions mo = AppContext.getContext().getBean(MergeOptions.class);
        try {
            canonicalFile = (new File(mo.getValue(Option.WRITE))).getCanonicalFile();
        } catch (IOException e) {
            throw new CommandLineParserException(i18n.getString("Merge.could.not.resolve.file", mo.getValue(Option.WRITE)));
        }

        for (String inFile : mo.getValues(Option.FILES)) {
            try {
                File sigFile = (new File(inFile)).getCanonicalFile();
                if (canonicalFile.equals(sigFile)) {
                    throw new CommandLineParserException(i18n.getString("Merge.notunique.writefile"));
                }
            } catch (IOException ex) {
                throw new CommandLineParserException(i18n.getString("Merge.could.not.resolve.file", inFile));
            }
        }

        try {
            FileOutputStream f = new FileOutputStream(mo.getValue(Option.WRITE));
            f.close();
        } catch (IOException e) {
            throw new CommandLineParserException(i18n.getString("Merge.could.not.create.write.file"));
        }
    }

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        MergeOptions mo = AppContext.getContext().getBean(MergeOptions.class);
        mo.readOptions(optionName, args);
    }

    void perform() {

        String msg;
        MergeOptions mo = AppContext.getContext().getBean(MergeOptions.class);
        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        String testURL = bo.getValue(Option.TEST_URL);
        if (testURL == null) {
            testURL = "";
        }

        MergedSigFile[] files = new MergedSigFile[mo.getValues(Option.FILES).size()];
        PrintWriter log = new PrintWriter(System.out);
        FeaturesHolder fh = new FeaturesHolder();
        for (int i = 0; i < mo.getValues(Option.FILES).size(); i++) {
            String sigFiles = mo.getValues(Option.FILES).get(i);
            MultipleFileReader in = new MultipleFileReader(log, MultipleFileReader.CLASSPATH_MODE, getFileManager());
            if (!in.readSignatureFiles(testURL, sigFiles)) {
                msg = i18n.getString("SignatureTest.error.sigfile.invalid", sigFiles);
                in.close();
                error(msg);
            }
            files[i] = new MergedSigFile(in, this);
            if (i == 0) {
                fh.setFeatures(in.getSupportedFeatures());
            } else {
                fh.retainFeatures(in.getSupportedFeatures());
            }

            // why do we need to build members here ????
            MemberCollectionBuilder builder = new MemberCollectionBuilder(new SilentLog());

            for (ClassDescription c : files[i].getClassSet().values()) {
                c.setHierarchy(files[i].getClassHierarchy());
                try {
                    if (in.isFeatureSupported(FeaturesHolder.BuildMembers)) {
                        builder.createMembers(c, true, true, false);
                    }
                    normalizer.normThrows(c, true);
                } catch (ClassNotFoundException e) {
                    //storeError(i18n.getString("Setup.error.message.classnotfound", e.getMessage()));
                }
            }
        }

        JSR68Merger merger = new JSR68Merger(this, this, fh);
        VirtualClassDescriptionLoader result = merger.merge(files);

        if (!isPassed()) {
            printErrors();
            return;
        }

        ClassHierarchy ch = new ClassHierarchyImpl(result, ClassHierarchy.ALL_PUBLIC);
        for (Iterator<ClassDescription> i = result.getClassIterator(); i.hasNext(); ) {
            ClassDescription c = i.next();
            c.setHierarchy(ch);
        }
        MemberCollectionBuilder builder = new MemberCollectionBuilder(new SilentLog());
        for (Iterator<ClassDescription> i = result.getClassIterator(); i.hasNext(); ) {
            ClassDescription c = i.next();
            try {
                builder.createMembers(c, false, true, false);
                normalizer.normThrows(c, true);
            } catch (ClassNotFoundException e) {
                storeError(i18n.getString("Merge.warning.message.classnotfound", e.getMessage()), null);
            }
        }

        Writer writer = null;
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        PrintWriter pw = null;

        try {
            //write header to the signature file
            writer = getFileManager().getWriter(merger.getSupportedFeatures());
            if (writer == null) {
                failed("Could not find a writer for given sigtest file formats.");
                return;
            }

            writer.setApiVersion("");
            if (mo.getValue(Option.WRITE) != null) {
                fos = new FileOutputStream(mo.getValue(Option.WRITE));
                osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                pw = new PrintWriter(osw);
            } else {
                pw = new PrintWriter(System.out);
            }
            writer.init(pw);
            for (Format.Feature f : merger.getSupportedFeatures()) {
                if (f != FeaturesHolder.CopyRight) // don't put copyright int merged file
                {
                    writer.addFeature(f);
                }
            }
            writer.writeHeader();

            // scan class and writes definition to the signature file
            // 1st analyze all the classes
            for (Iterator<ClassDescription> i = result.getClassIterator(); i.hasNext(); ) {
                ClassDescription c = i.next();
                writer.write(c);
            }

        } catch (IOException e) {
            SwissKnife.reportThrowable(e);
            error(e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
            try {
                if (fos != null) {
                    fos.close();
                }
                if (osw != null) {
                    osw.close();
                }
            } catch (IOException ex) {
                SwissKnife.reportThrowable(ex);
            }
            if (pw != null) {
                pw.close();
            }
        }
        printErrors();
    }

    /**
     * Prints help text.
     */
    protected void usage() {
        String nl = System.getProperty("line.separator");
        String sb = nl + getComponentName() + " - " + i18n.getString("SignatureTest.usage.version", Version.Number) +
                nl + i18n.getString("Setup.usage.start") +
                nl + i18n.getString("Sigtest.usage.delimiter") +
                nl + i18n.getString("Merge.usage.files", Option.FILES.getKey()) +
                nl + i18n.getString("Merge.usage.write", Option.WRITE.getKey()) +
                nl + i18n.getString("Merge.usage.binary", Option.BINARY.getKey()) +
                nl + i18n.getString("Sigtest.usage.delimiter") +
                nl + i18n.getString("SetupAndTest.helpusage.version", Option.VERSION.getKey()) +
                nl + i18n.getString("Setup.usage.help", Option.HELP.getKey()) +
                nl + i18n.getString("Sigtest.usage.delimiter") +
                nl + i18n.getString("Setup.usage.end");
        System.err.println(sb);
    }

    protected String getComponentName() {
        return "Merge";
    }

    static class SilentLog implements Log {

        public void storeError(String s, Logger utilLogger) {
        }

        public void storeWarning(String s, Logger utilLogger) {
        }
    }
}
