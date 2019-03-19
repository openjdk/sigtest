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

import com.sun.tdk.signaturetest.sigfile.f42.F42Format;
import com.sun.tdk.signaturetest.sigfile.f41.F41Format;
import com.sun.tdk.signaturetest.sigfile.f40.F40Format;
import com.sun.tdk.signaturetest.sigfile.f31.F31Format;
import com.sun.tdk.signaturetest.sigfile.f21.F21Format;
import com.sun.tdk.signaturetest.sigfile.f43.F43Format;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Mikhail Ershov
 */
public class FileManager {

    private static final String DEFAULT_PROTOCOL = "file:";

    public static URL getURL(String testURL, String fileName) throws MalformedURLException {

        URL result;
        File f = new File(fileName);

        if (testURL == null) {
            testURL = "";
        }

        if (f.isAbsolute()) {
            result = f.toURI().toURL();
        } else {
            // check that protocol specified
            if (testURL.indexOf(':') == -1) {
                testURL = DEFAULT_PROTOCOL + testURL;
            }
            result = new URL(new URL(testURL), fileName);
        }

        return result;
    }

    private String getFormat(URL fileURL) {
        String currentLine;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(fileURL.openStream(), StandardCharsets.UTF_8))) {
            if ((currentLine = in.readLine()) == null) {
                return null;
            }
            currentLine = currentLine.trim();
        } catch (IOException e) {
            return null;
        }
        return currentLine;
    }

    /**
     * Returns the latest Writer for format supported given set of features
     */
    public Writer getWriter(Set<FeaturesHolder.Feature> features) {

        List<Format> applicableFormats = new ArrayList<>(formats.size());

        for (Format format : formats) {
            Set<FeaturesHolder.Feature> formatFeatures = format.getSupportedFeatures();
            if (features.equals(formatFeatures)) {
                applicableFormats.add(format);
            }
        }

        double latestVersion = 0;
        Writer latestWriter = null;

        for (Format f : applicableFormats) {

            String[] sv = f.getVersion().split(" ");
            double v = Double.parseDouble(sv[sv.length - 1].substring(1));

            if (v > latestVersion) {
                latestVersion = v;
                latestWriter = f.getWriter();
            }
        }
        return latestWriter;
    }

    public Reader getReader(URL fileURL) {
        String format = getFormat(fileURL);
        if (format != null) {
            for (Format f : formats) {
                if (f.isApplicable(format)) {
                    return f.getReader();
                }
            }
        }
        return null;
    }

    public Format getDefaultFormat() {
        return defaultFormat;
    }

    public void addFormat(Format frm, boolean useByDefault) {
        formats.add(frm);
        if (useByDefault) {
            defaultFormat = frm;
        }
    }

    public void setFormat(Format frm) {
        formats.clear();
        formats.add(frm);
        defaultFormat = frm;
    }
    private Format defaultFormat = new F43Format();
    private final List<Format> formats = new ArrayList<>();

    public FileManager() {
        formats.add(defaultFormat);
        formats.add(new F21Format());
        formats.add(new F31Format());
        formats.add(new F40Format());
        formats.add(new F41Format());
        formats.add(new F42Format());
    }
}
