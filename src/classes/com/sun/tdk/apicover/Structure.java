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


package com.sun.tdk.apicover;

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.context.Option;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structure for a report
 *
 * @author Mikhail Ershov
 */
public class Structure {

    private ApicovOptions ao = AppContext.getContext().getBean(ApicovOptions.class);
    private boolean active = false;
    private List<Section> sections;
    private String title = null;

    public Structure() throws IOException, SAXException, ParserConfigurationException {
        init();
    }

    public boolean isActive() {
        return active;
    }

    public String getTitle() {
        if (title == null || title.isEmpty()) {
            return null;
        } else {
            return title;
        }
    }

    public List<Section> getSections() {
        if (!isActive()) {
            return Collections.emptyList();
        } else {
            return sections;
        }
    }

    private void init() throws ParserConfigurationException, IOException, SAXException {
        String strFile;
        if (ao.getValue(Option.STRUCTURE) != null) {
            strFile = ao.getValue(Option.STRUCTURE);
            Document d;
            try (FileReader in = new FileReader(strFile)) {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                d = db.parse(new InputSource(in));
            }
            title = d.getDocumentElement().getAttribute("title");
            NodeList sectionList = d.getElementsByTagName("section");
            active = true;
            sections = new ArrayList<>();
            for (int i = 0; i < sectionList.getLength(); i++) {

                Element secDOM = (Element) sectionList.item(i);
                Section sec = new Section();

                String secName = secDOM.getAttribute("name");
                if (secName != null && !secName.isEmpty()) {
                    sec.name = secName;
                } else {
                    throw new IllegalStateException("Section name must be specified (" + strFile + ")");
                }

                String secTitle = secDOM.getAttribute("title");
                if (secTitle != null && !secTitle.isEmpty()) {
                    sec.title = secTitle;
                }

                String secHide = secDOM.getAttribute("hidden");
                if (secHide != null && !secHide.isEmpty()) {
                    sec.hidden = Boolean.parseBoolean(secHide);
                }

                NodeList inclList = secDOM.getElementsByTagName("include");
                for (int j = 0; j < inclList.getLength(); j++) {
                    Element inclDOM = (Element) inclList.item(j);

                    String pkgs = inclDOM.getAttribute("packages");
                    if (pkgs != null && !pkgs.isEmpty()) {
                        sec.pkgInclude.add(pkgs + ".");
                    }

                    String secInc = inclDOM.getAttribute("section");
                    if (secInc != null && !secInc.isEmpty()) {
                        Section ref = findSecByName(secInc);
                        if (ref == null) {
                            throw new IllegalStateException("Section " + secInc + " not found or forward (" + strFile + ", " + secName + ")");
                        }
                        sec.pkgInclude.addAll(ref.pkgInclude);
                    }
                }

                sections.add(sec);
            }

        }
    }

    private Section findSecByName(String secName) {
        assert secName != null && !secName.isEmpty();
        for (Section s : sections) {
            if (s.name.equals(secName)) {
                return s;
            }
        }
        return null;
    }

    public boolean isDefined() {
        return active;
    }

    public static class Section {
        private String name;
        private String title;
        private List<String> pkgInclude = new ArrayList<>();
        private boolean hidden;

        public String getTitle() {
            return title != null && !title.isEmpty() ? title : null;
        }

        public boolean isHidden() {
            return hidden;
        }

        public String getName() {
            return name;
        }

        public List<String> getPkgInclude() {
            return pkgInclude;
        }
    }


}
