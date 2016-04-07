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

package com.sun.tdk.signaturetest.sigfile.f43;

import com.sun.tdk.signaturetest.model.ModuleDescription;
import com.sun.tdk.signaturetest.sigfile.Format;
import com.sun.tdk.signaturetest.sigfile.Parser;
import com.sun.tdk.signaturetest.sigfile.f42.F42Reader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static com.sun.tdk.signaturetest.core.context.ModFeatures.featureSetFromCommaList;
import static com.sun.tdk.signaturetest.sigfile.f43.F43Format.*;

public class F43Reader extends F42Reader {

    public F43Reader(Format format) {
        super(format);
    }

    protected Parser getParser() {
        return new F43Parser();
    }

    @Override
    protected Document processXMLFragment(String line) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return db.parse( new InputSource( new StringReader( line )) );
        } catch (ParserConfigurationException | SAXException | IOException  e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ModuleDescription fromDom(Element m) {
        ModuleDescription md = new ModuleDescription();
        {
            assert MODULE.equals(m.getNodeName());
            String name = m.getAttribute(NAME);
            if (name != null && !name.isEmpty()) {
                md.setName(name);
            }
        }
        {
            String features = m.getAttribute(FEATURES);
            if (features == null ) {
                features = "";
            }
            md.setFeatures(featureSetFromCommaList(features));
        }
        {
            String version = m.getAttribute(VERSION);
            if (version != null && !version.isEmpty()) {
                md.setVersion(version);
            }
        }
        {
            String mainC = m.getAttribute(MAIN_CLASS);
            if (mainC != null && !mainC.isEmpty()) {
                md.setMainClass(mainC);
            }
        }
        {
            NodeList pkgs = m.getElementsByTagName(PACKAGE);
            HashSet<String> pkSet = new HashSet<>();
            for (int i = 0; i < pkgs.getLength(); i++) {
                Element p = (Element) pkgs.item(i);
                String pkgName = p.getAttribute(NAME);
                if (!pkgName.isEmpty()) {
                    pkSet.add(pkgName);
                }
            }
            md.setPackages(pkSet);
        }
        {
            NodeList cons = m.getElementsByTagName(CONCEAL);
            HashSet<String> coSet = new HashSet<>();
            for (int i = 0; i < cons.getLength(); i++) {
                Element c = (Element) cons.item(i);
                String coName = c.getAttribute(NAME);
                if (!coName.isEmpty()) {
                    coSet.add(coName);
                }
            }
            md.setConceals(coSet);
        }
        {
            NodeList exps = m.getElementsByTagName(EXPORTS);
            HashSet<ModuleDescription.Exports> exSet = new HashSet<>();

            for (int i = 0; i < exps.getLength(); i++) {
                Element e = (Element) exps.item(i);
                ModuleDescription.Exports export = new ModuleDescription.Exports();
                export.source = e.getAttribute(SOURCE);
                HashSet<String> taSet = new HashSet<>();

                NodeList targs = e.getElementsByTagName(TARGET);
                for (int j = 0; j < targs.getLength(); j++) {
                    Element t = (Element) targs.item(j);
                    taSet.add(t.getAttribute(NAME));
                }
                export.targets = taSet;
                exSet.add(export);
            }
            md.setExports(exSet);
        }
        {
            NodeList reqs = m.getElementsByTagName(REQUIRES);
            HashSet<ModuleDescription.Requires> rqSet = new HashSet<>();
            for (int i = 0; i < reqs.getLength(); i++) {
                Element r = (Element) reqs.item(i);
                ModuleDescription.Requires rq = new ModuleDescription.Requires();
                rq.name =  r.getAttribute(NAME);
                Set<ModuleDescription.Requires.Modifier> ms = new HashSet<>();
                if (TRUE.equals(r.getAttribute(ModuleDescription.Requires.Modifier.MANDATED.name().toLowerCase()))) {
                    ms.add(ModuleDescription.Requires.Modifier.MANDATED);
                }
                if (TRUE.equals(r.getAttribute(ModuleDescription.Requires.Modifier.PUBLIC.name().toLowerCase()))) {
                    ms.add(ModuleDescription.Requires.Modifier.PUBLIC);
                }
                if (TRUE.equals(r.getAttribute(ModuleDescription.Requires.Modifier.SYNTHETIC.name().toLowerCase()))) {
                    ms.add(ModuleDescription.Requires.Modifier.SYNTHETIC);
                }
                rq.modifiers = ms;
                rqSet.add(rq);
            }

            md.setRequires(rqSet);
        }
        {
            NodeList prs = m.getElementsByTagName(PROVIDES);
            Map<String, ModuleDescription.Provides> prMap = new HashMap<>();
            for (int i = 0; i < prs.getLength(); i++) {
                Element p = (Element) prs.item(i);
                ModuleDescription.Provides pr = new ModuleDescription.Provides();
                pr.service =  p.getAttribute(SERVICE);
                pr.providers = new HashSet<>();
                NodeList ps = p.getElementsByTagName(PROVIDER);
                for (int j = 0; j < ps.getLength(); j++) {
                    Element t = (Element) ps.item(j);
                    pr.providers.add(t.getAttribute(NAME));
                }
                prMap.put(pr.service, pr);
            }
            md.setProvides(prMap);
        }
        {
            NodeList uses = m.getElementsByTagName(USES);
            Set<String> usSet = new HashSet<>();
            for (int i = 0; i < uses.getLength(); i++) {
                Element c = (Element) uses.item(i);
                usSet.add(c.getAttribute(NAME));
            }
            md.setUses(usSet);
        }
        return md;
    }
}
