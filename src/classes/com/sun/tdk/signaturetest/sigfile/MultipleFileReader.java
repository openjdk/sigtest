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
package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.loaders.VirtualClassDescriptionLoader;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.toyxml.Elem;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Roman Makarchuk
 */
public class MultipleFileReader extends VirtualClassDescriptionLoader implements AutoCloseable {

    private static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(MultipleFileReader.class);
    public static final int CLASSPATH_MODE = 1;
    public static final int MERGE_MODE = 2;
    private Iterator<ClassDescription> classIterator = null;
    private PrintWriter log;
    private int mode;
    private String apiVersion;
    private FileManager fileMan;
    private final BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
    private List<Elem> elements;

    public MultipleFileReader(PrintWriter log, int mode, FileManager f) {
        // Note: Merge mode is not supported yet.
        assert mode == CLASSPATH_MODE;

        this.log = log;
        this.mode = mode;
        this.fileMan = f;

    }

    public boolean readSignatureFiles(String testURL, String sigFileList) {

        assert testURL != null;
        assert sigFileList != null;

        boolean result = true;

        StringTokenizer st = new StringTokenizer(sigFileList, File.pathSeparator);

        while (st.hasMoreElements() && result) {
            String fileName = st.nextToken();
            result = readSignatureFile(testURL, fileName);
        }

        return result;
    }

    public boolean readSignatureFile(String testURL, String sigFileName) {

        assert testURL != null;
        assert sigFileName != null;

        boolean result;

        try {
            URL fileURL = FileManager.getURL(testURL, sigFileName);
            result = readFile(fileURL);
        } catch (MalformedURLException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            String[] invargs = {testURL, e.getMessage()};
            log.println(i18n.getString("MultipleFileReader.error.url.threwerror", invargs));
            return false;
        }
        rewind();
        return result;
    }

    private boolean readFile(URL fileURL) {

        String msg = null;

        //  Open the specified sigfile and read standard headers.
        try (Reader in = fileMan.getReader(fileURL)) {
            if (in == null) {
                return false;
            }

            if (!in.readSignatureFile(fileURL)) {
                msg = i18n.getString("MultipleFileReader.error.sigfile.invalid", fileURL);
            }

            if (mode == MERGE_MODE && !in.hasFeature(FeaturesHolder.MergeModeSupported)) {
                throw new IOException(i18n.getString("MultipleFileReader.error.cannt_merge_old_files") + fileURL);
            }

            if (!isInitialized()) { // first file
                setFeatures(in.getAllSupportedFeatures());
            } else {
                retainFeatures(in.getAllSupportedFeatures());
            }

            apiVersion = in.getApiVersion();

            ClassDescription cl;
            while ((cl = in.readNextClass()) != null) {

                String name = cl.getQualifiedName();
                if (mode == CLASSPATH_MODE) {
                    try {
                        load(name);
                    } catch (ClassNotFoundException ex) {
                        // use only first class description
                        add(cl);
                    }
                } else {
                    assert mode == MERGE_MODE;
                }
            }
            elements = in.getElems();
        } catch (IOException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            msg = i18n.getString("MultipleFileReader.error.sigfile.prob") + "\n" + e;
        } catch (SecurityException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            msg = i18n.getString("MultipleFileReader.error.sigfile.sec") + "\n" + e;
        }

        if (msg != null) {
            log.println(msg);
            return false;
        }

        return true;
    }

    @Override
    public void close() {
        classIterator = null;
        cleanUp();
    }

    public void rewind() {
        classIterator = getClassIterator();
    }

    public List<Elem> getElements() {
        return elements;
    }

    public ClassDescription nextClass() throws IOException {
        ClassDescription cl = null;
        if (classIterator != null && classIterator.hasNext()) {
            cl = classIterator.next();
        }
        return cl; // cl == null ? null : (ClassDescription) cl.clone();
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
