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

package com.sun.tdk.signaturetest.sigfile.f43;

import com.sun.tdk.signaturetest.core.PrimitiveTypes;
import com.sun.tdk.signaturetest.core.context.ModFeatures;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MethodDescr;
import com.sun.tdk.signaturetest.model.Modifier;
import com.sun.tdk.signaturetest.model.ModuleDescription;
import com.sun.tdk.signaturetest.sigfile.ModWriter;
import com.sun.tdk.signaturetest.sigfile.f42.F42Writer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import static com.sun.tdk.signaturetest.sigfile.f43.F43Format.*;

/**
 * @author Mike Ershov
 */
public class F43Writer extends F42Writer implements ModWriter {

    private PrintWriter out;

    public F43Writer() {
        super();
        setFormat(new F43Format());
    }


    @Override
    public void init(PrintWriter out) {
        this.out = out;
        super.init(out);
    }

    @Override
    public void write(ClassDescription classDescription) {
        super.write(classDescription);
    }

    @Override
    protected void write(StringBuffer buf, MethodDescr m) {
        writeMeth(buf, m);

        if (m.hasModifier(Modifier.HASDEFAULT)) {
            Object ad = m.getAnnoDef();
            if (ad != null) {
                buf.append(" value= ");
                buf.append(PrimitiveTypes.simpleObjectToString(ad));
            }
        }

        addAnnotations(buf, m);

    }

    @Override
    public void write(ModuleDescription md) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element eModule = doc.createElement(MODULE);

            eModule.setAttribute(NAME, md.getName());

            Set<ModFeatures> features = md.getFeatures();
            eModule.setAttribute(FEATURES, ModFeatures.commaListFromFeatureSet(features));

            if (features.contains(ModFeatures.ALL) || features.contains(ModFeatures.VERSION)) {
                if (!md.getVersion().isEmpty()) {
                    eModule.setAttribute(VERSION, md.getVersion());
                }
            }

            if (features.contains(ModFeatures.ALL) || features.contains(ModFeatures.MAIN_CLASS)) {
                if (!md.getMainClass().isEmpty()) {
                    eModule.setAttribute(MAIN_CLASS, md.getMainClass());
                }
            }

            doc.appendChild(eModule);

            if (features.contains(ModFeatures.ALL) || features.contains(ModFeatures.PACKAGES)) {
                for (String packName : md.getPackages()) {
                    Element ePack = doc.createElement(PACKAGE);
                    ePack.setAttribute(NAME, packName);
                    eModule.appendChild(ePack);
                }
            }
            if (features.contains(ModFeatures.ALL)
                    || features.contains(ModFeatures.EXPORTS_PUBLIC)
                    || features.contains(ModFeatures.EXPORTS_ALL)) {
                for (ModuleDescription.Exports ex : md.getExports()) {

                    if (!ex.targets.isEmpty()) {
                        if (!features.contains(ModFeatures.ALL)
                                && !features.contains(ModFeatures.EXPORTS_ALL)) {
                            continue;
                        }
                    }

                    Element eExp = doc.createElement(EXPORTS);

                    eExp.setAttribute(SOURCE, ex.source);
                    for (String target : ex.targets) {
                        Element eTarget = doc.createElement(TARGET);
                        eTarget.setAttribute(NAME, target);
                        eExp.appendChild(eTarget);
                    }
                    eModule.appendChild(eExp);
                }
            }

            for (ModuleDescription.Requires re : md.getRequires()) {

                if (features.contains(ModFeatures.ALL) || features.contains(ModFeatures.REQUIRES_ALL) ||
                        (features.contains(ModFeatures.REQUIRES_TRANSITIVE) && re.modifiers.contains(ModuleDescription.Requires.Modifier.TRANSITIVE))) {

                    Element eReq = doc.createElement(REQUIRES);
                    eReq.setAttribute(NAME, re.name);
                    for (ModuleDescription.Requires.Modifier m : re.modifiers) {
                        eReq.setAttribute(m.name().toLowerCase(), TRUE);
                    }
                    eModule.appendChild(eReq);
                }
            }

            if (features.contains(ModFeatures.ALL) || features.contains(ModFeatures.SERVICES)) {
                for (ModuleDescription.Provides pr : md.getProvides().values()) {
                    Element ePr = doc.createElement(PROVIDES);
                    ePr.setAttribute(SERVICE, pr.service);
                    eModule.appendChild(ePr);
                }
            }

            if (features.contains(ModFeatures.ALL) || features.contains(ModFeatures.USES)) {
                for (String uses : md.getUses()) {
                    Element eUses = doc.createElement(USES);
                    eUses.setAttribute(NAME, uses);
                    eModule.appendChild(eUses);
                }
            }
            out.println(getStringFromDocument(doc));

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }


    public String getStringFromDocument(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
