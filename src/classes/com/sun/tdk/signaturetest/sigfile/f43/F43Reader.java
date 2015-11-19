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

                NodeList trgs = e.getElementsByTagName(TARGET);
                for (int j = 0; j < trgs.getLength(); j++) {
                    Element t = (Element) trgs.item(j);
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
