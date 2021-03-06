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
package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.toyxml.Elem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <b>SignatureClassLoader</b> implements input stream sequentially reading
 * <b>ClassDescription</b> instances from signature file. This program merges
 * several signature files into a single one.
 *
 * @author Maxim Sokolnikov
 * @version 05/09/09
 * @see ClassDescription
 */
public abstract class SignatureClassLoader implements Reader {

    protected final Format format;
    protected final Set<FeaturesHolder.Feature> features;
    private BufferedReader in;
    private final Parser parser;
    private static final int BUFSIZE = 0x8000;
    private final List<Elem> elems;
    /**
     * API version found in {@code this} signature file.
     */
    protected String apiVersion = "";
    /**
     * Sigfile format version found in {@code this} signature file.
     */
    protected String signatureFileFormat = "";

    protected SignatureClassLoader(Format format) {
        this.format = format;
        features = format.getSupportedFeatures();
        parser = getParser();
        elems = new ArrayList<>();
    }

    protected abstract Parser getParser();

    public boolean hasFeature(Format.Feature feature) {
        return features.contains(feature);
    }

    public Set<FeaturesHolder.Feature> getAllSupportedFeatures() {
        return features;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * Return the next {@code SigFileClassDescription} read from
     * {@code this} signature file.
     *
     * @see ClassDescription
     */
    public ClassDescription readNextClass() throws IOException {

        String currentLine;
        String classDescr = null;
        List<String> definitions = new ArrayList<>();

        for (; ; ) {
            in.mark(BUFSIZE);
            if ((currentLine = in.readLine()) == null) {
                break;
            }

            currentLine = currentLine.trim();
            currentLine = preprocessLine(currentLine);
            if (currentLine.isEmpty() || currentLine.startsWith("#")) {
                continue;
            }
            MemberType type = MemberType.getItemType(currentLine);

            if (type == MemberType.CLASS) {
                if (classDescr == null) {
                    classDescr = currentLine;
                } else {
                    break;
                }
            } else if (type == MemberType.MODULE) {
                readXML("module", currentLine);
            } else {
                if (classDescr == null) {
                    throw new Error();
                }

                definitions.add(currentLine);
            }
        }
        in.reset();

        if (classDescr == null && definitions.isEmpty()) {
            return null;
        }

        classDescr = convertClassDescr(classDescr);
        definitions = convertClassDefinitions(definitions);

        return parser.parseClassDescription(classDescr, definitions);
    }

    public List<Elem> getElems() {
        return elems;
    }

    protected void readXML(String elName, String line) throws IOException {
        StringBuilder xmlTxt = new StringBuilder();
        while (line != null && !line.trim().isEmpty()) {
            line = line.trim();
            line = preprocessLine(line);
            if (line.isEmpty() || line.startsWith("#")) {
                line = in.readLine();
                continue;
            }
            xmlTxt.append(line);
            line = in.readLine();
        }
        Elem d = processXMLFragment(xmlTxt.toString());
        if (d != null) {
            elems.add(d);
        }
    }

    protected Elem processXMLFragment(String s) {
        return null;
    }

    protected String preprocessLine(String currentLine) {
        return currentLine;
    }

    protected abstract String convertClassDescr(String descr);

    protected abstract List<String> convertClassDefinitions(List<String> definitions);

    /**
     * Open {@code fileURL} for input, and parse comments to initialize
     * fields
     */
    public boolean readSignatureFile(URL fileURL) throws IOException {
        in = new BufferedReader(new InputStreamReader(fileURL.openStream(), StandardCharsets.UTF_8), BUFSIZE);
        assert in.markSupported();
        return readHeaders(in);
    }

    protected boolean readHeaders(BufferedReader in) throws IOException {

        String currentLine;

        if ((currentLine = in.readLine()) == null) {
            return false;
        }

        //  Check for the required headers (first two lines)
        signatureFileFormat = currentLine.trim();

        if (!signatureFileFormat.equals(format.getVersion())) {
            return false;
        }

        if ((currentLine = in.readLine()) == null) {
            return false;
        }

        currentLine += ' ';
        if (!currentLine.startsWith(Format.VERSION)) {
            return false;
        }

        apiVersion = currentLine.substring(Format.VERSION.length()).trim();

        in.mark(BUFSIZE);
        while ((currentLine = in.readLine()) != null && currentLine.startsWith("#")) {
            removeMissingFeature(currentLine);
        }
        in.reset();

        return true;
    }

    private void removeMissingFeature(String currentLine) {

        Format.Feature f = null;
        boolean remove = false;
        for (Format.Feature feature : features) {
            f = feature;
            if (f.match(currentLine)) {
                remove = true;
                break;
            }
        }

        if (f != null && remove) {
            features.remove(f);
        }
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
