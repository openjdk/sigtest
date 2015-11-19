package com.sun.tdk.signaturetest.sigfile.f43;

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

import static com.sun.tdk.signaturetest.sigfile.f43.F43Format.*;

public class F43Writer extends F42Writer implements ModWriter {

    private PrintWriter out;

    @Override
    public void init(PrintWriter out) {
        this.out = out;
        setFormat(new F43Format());
        super.init(out);
    }

    @Override
    public void write(ModuleDescription md) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element eModule = doc.createElement(MODULE);

            eModule.setAttribute(NAME, md.getName());
            if (!md.getVersion().isEmpty()) {
                eModule.setAttribute(VERSION, md.getVersion());
            }
            if (!md.getMainClass().isEmpty()) {
                eModule.setAttribute(MAIN_CLASS, md.getMainClass());
            }
            doc.appendChild(eModule);

            for (String packName : md.getPackages()) {
                Element ePack = doc.createElement(PACKAGE);
                ePack.setAttribute(NAME, packName);
                eModule.appendChild(ePack);
            }

            for (String packName : md.getConceals()) {
                Element ePack = doc.createElement(CONCEAL);
                ePack.setAttribute(NAME, packName);
                eModule.appendChild(ePack);
            }

            for (ModuleDescription.Exports ex : md.getExports()) {
                Element eExp = doc.createElement(EXPORTS);
                eExp.setAttribute(SOURCE, ex.source);
                for (String target : ex.targets) {
                    Element eTarget = doc.createElement(TARGET);
                    eTarget.setAttribute(NAME, target);
                    eExp.appendChild(eTarget);
                }
                eModule.appendChild(eExp);
            }

            for (ModuleDescription.Requires re : md.getRequires()) {
                Element eReq = doc.createElement(REQUIRES);
                eReq.setAttribute(NAME, re.name);
                for (ModuleDescription.Requires.Modifier m : re.modifiers) {
                    eReq.setAttribute(m.name().toLowerCase(), TRUE);
                }
                eModule.appendChild(eReq);
            }

            for (ModuleDescription.Provides pr : md.getProvides().values()) {
                Element ePr = doc.createElement(PROVIDES);
                ePr.setAttribute(SERVICE, pr.service);
                eModule.appendChild(ePr);
                for (String provider : pr.providers) {
                    Element eProvider = doc.createElement(PROVIDER);
                    eProvider.setAttribute(NAME, provider);
                    ePr.appendChild(eProvider);
                }
            }

            for (String uses : md.getUses()) {
                Element eUses = doc.createElement(USES);
                eUses.setAttribute(NAME, uses);
                eModule.appendChild(eUses);
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
