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

package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.ModSetup;
import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.context.ModSetupOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ModuleDescription;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Writes APIcover report structure definition file
 *
 * @author Mikhail Ershov
 */
public class StructWriter {

    private ModSetupOptions mo = AppContext.getContext().getBean(ModSetupOptions.class);
    private ArrayDeque<ModuleDescription> modulesToWrite = new ArrayDeque<>();
    private HashSet<String> moduleNames = new HashSet<>();
    private HashSet<String> reportedModuleNames = new HashSet<>();

    // Generate structure file for APICover
    public boolean createStructFile(ModSetup.WriteMode wm, HashMap<String, ModuleDescription> model, Set<ModuleDescription> allModules) {

        URL signatureFile = null;
        Map<String, ModuleDescription> allModulesMap = new HashMap<>();
        for (ModuleDescription md : allModules) {
            allModulesMap.put(md.getName(), md);
        }

        try {
            signatureFile = FileManager.getURL(mo.getValue(Option.TEST_URL), mo.getValue(Option.FILE_NAME));

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element str = doc.createElement("structure");
            str.setAttribute("title", "Static coverage report");
            doc.appendChild(str);

            Iterator<ModuleDescription> it = model.values().iterator();
            while (it.hasNext()) {
                ModuleDescription md = it.next();
                reportedModuleNames.add(md.getName());
                processModule(md, allModulesMap, wm);
            }

            for (ModuleDescription md : modulesToWrite) {
                Element sec = doc.createElement("section");
                sec.setAttribute("name", md.getName());
                sec.setAttribute("title", "Module " + md.getName());
                sec.setAttribute("hidden", reportedModuleNames.contains(md.getName()) ? "false" : "true");
                for (String pkg : md.getPackages()) {
                    Element inc = doc.createElement("include");
                    inc.setAttribute("packages", pkg);
                    sec.appendChild(inc);
                }
                if (wm == ModSetup.WriteMode.STRUCT_CUMULATIVE) {
                    for (ModuleDescription.Requires rq : md.getRequires()) {
                        Element inc = doc.createElement("include");
                        inc.setAttribute("section", rq.getName());
                        sec.appendChild(inc);
                    }
                }

                str.appendChild(sec);
            }


            DOMSource domSource = new DOMSource(doc);
            FileWriter writer = new FileWriter(signatureFile.getFile());
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(domSource, result);
            writer.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private void processModule(ModuleDescription md, Map<String, ModuleDescription> allModulesMap, ModSetup.WriteMode wm) {
        if (moduleNames.contains(md.getName())) {
            return;
        }

        if (wm == ModSetup.WriteMode.STRUCT_CUMULATIVE) {
            for (ModuleDescription.Requires rq : md.getRequires()) {
                ModuleDescription mdr = allModulesMap.get(rq.getName());
                if (mdr != null) {
                    processModule(mdr, allModulesMap, wm);
                }
            }
        }
        modulesToWrite.addLast(md);
        moduleNames.add(md.getName());
    }

}
