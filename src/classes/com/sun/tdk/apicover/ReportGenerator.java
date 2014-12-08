/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.core.Erasurator;
import com.sun.tdk.signaturetest.core.PrimitiveTypes;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.Modifier;
import com.sun.tdk.signaturetest.model.PackageDescr;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.SwissKnife;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

// Plain report generator
public abstract class ReportGenerator extends APIVisitor {

    protected RefCounter refCounter;
    private PrintWriter log;

    public void setLog(PrintWriter log) {
        this.log = log;
    }

    enum FIELD_MODE {

        NOCONST, ALL
    }

    enum EXLUDE_MODE {

        EXCLUDEINTERFACES, EXCLUDEABSTRACTCLASSES,
        EXCLUDEABSTRACTMETHODS, EXCLUDEFIELD
    }
    DETAIL_LEVEL detail = DETAIL_LEVEL.NOT_COVERED_MEMB;
    FIELD_MODE fieldMode = FIELD_MODE.NOCONST;
    Set<EXLUDE_MODE> excludeMode = new HashSet<EXLUDE_MODE>();
    Map<String, String[]> config;
    Map<String, Field> results = new HashMap<String, Field>();
    Collection<String> xList = new ArrayList<String>();
    protected PrintWriter pw;

    public static enum DETAIL_LEVEL {

        PACKAGE, CLASS, NOT_COVERED_MEMB, COVERED_MEMB, ANY_MEMB, COUNT;

        boolean hasMembers() {
            return this.ordinal() > CLASS.ordinal();
        }

        boolean hasTwoColumns() {
            return this.ordinal() >= ANY_MEMB.ordinal();
        }

        boolean isOnlyPackages() {
            return this == PACKAGE;
        }

        boolean isOnlyUncovered() {
            return this == NOT_COVERED_MEMB;
        }

        boolean isOnlyCovered() {
            return this == COVERED_MEMB;
        }

        boolean hasCounters() {
            return this == COUNT;
        }
    }

    protected static class Field {

        int classes;
        int members;
        int tested;

        Field(int members, int tested) {
            this.members = members;
            this.tested = tested;
        }

        Field(int classes, int members, int tested) {
            this.members = members;
            this.tested = tested;
            this.classes = classes;
        }

        String getPercent() {
            return (members == 0) ? "" : tested * 100 / members + "%";
        }
    }

    protected ReportGenerator(RefCounter refCounter) {
        this.setReportfile(null);
        this.refCounter = refCounter;
        this.config = new HashMap<String, String[]>();
    }

    public void setDetail(DETAIL_LEVEL detail) {
        this.detail = detail;
    }

    public void addConfig(String key, String value) {
        this.config.put(key, new String[]{value});
    }

    public void addXList(String[] names) {
        for (String name : names) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(name));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.trim().startsWith("#")) {
                        continue;
                    }
                    xList.add(line);
                }
                this.addConfig(Option.EXCLUDE_LIST.getKey(), name);
            } catch (IOException e) {
                log.println(e.getMessage());
                //Main.debug(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.println(e.getMessage());
                        //Main.debug(e);
                    }
                }
            }
        }
    }

    public abstract void print();

    ReportGenerator createReportGenerator(String type, PrintWriter log) {
        ReportGenerator newReportGenerator;
        if (type.equals(Main.FORMAT_VALUE_PLAIN)) {
            newReportGenerator = new ReportPlain(this.refCounter);
        } else {
            newReportGenerator = new ReportXML(this.refCounter);
        }
        newReportGenerator.config = this.config;
        newReportGenerator.detail = this.detail;
        newReportGenerator.excludeMode = this.excludeMode;
        newReportGenerator.fieldMode = this.fieldMode;
        newReportGenerator.pw = this.pw;
        newReportGenerator.results = this.results;
        newReportGenerator.top = this.top;
        newReportGenerator.xList = this.xList;
        newReportGenerator.setLog(log);
        return newReportGenerator;
    }

    static ReportGenerator createReportGenerator(RefCounter ref, PrintWriter log) {
        ReportGenerator rg = new ReportPlain(ref);
        rg.setLog(log);
        return rg;
    }

    protected static String classRepr(ClassDescription cd) {
        String name = cd.getQualifiedName();
        name = name.substring(name.lastIndexOf(MemberDescription.MEMBER_DELIMITER) + 1);
        return name;
    }

    /*
     * The possible types of consts. They are included/excluded by option -includeConstantFields
     */
    private static final String[] consttypes = {
        "boolean",
        "byte",
        "short",
        "int",
        "long",
        "char",
        "float",
        "double",
        "java.lang.String",};

    protected boolean isConstType(String s) {
        for (String consttype : consttypes) {
            if (consttype.equals(s)) {
                return true;
            }
        }
        return false;
    }

    public void setConstatnChecking(boolean check) {
        this.fieldMode = check ? FIELD_MODE.ALL : FIELD_MODE.NOCONST;
    }

    public void excludeInterfaces() {
        excludeMode.add(EXLUDE_MODE.EXCLUDEINTERFACES);
    }

    public void excludeAbstractClasses() {
        excludeMode.add(EXLUDE_MODE.EXCLUDEABSTRACTCLASSES);
    }

    public void excludeAbstractMethods() {
        excludeMode.add(EXLUDE_MODE.EXCLUDEABSTRACTMETHODS);
    }

    public void excludeFields() {
        excludeMode.add(EXLUDE_MODE.EXCLUDEFIELD);
    }

    public void setReportfile(String reportfile) {
        try {
            if (reportfile == null) {
                pw = new PrintWriter(System.out);
            } else {
                pw = new PrintWriter(new FileOutputStream(reportfile));
            }
        } catch (FileNotFoundException e) {
            log.println(e);
        }
    }

    private boolean isExcluded(String str) {
        return xList.contains(str);
    }

    // check for excluded superpackages
    private boolean isPackageExcluded(String str) {
        if (xList.contains(str)) {
            return true;
        }
        int i = str.lastIndexOf(".");
        if (i > 0) {
            return isPackageExcluded(str.substring(0, i));
        }

        return false;
    }

    private void filter() {
        APIVisitor calc = new APIVisitor() {
            protected void visit(ClassDescription cd) {
                for (Iterator i = cd.getMembersIterator(); i.hasNext();) {
                    MemberDescription md = (MemberDescription) i.next();
                    if (!(md.isConstructor() || md.isField() || md.isMethod())) {
                        i.remove();
                        continue;
                    }
                    if (fieldMode == FIELD_MODE.NOCONST && md.isField() && md.hasModifier(Modifier.FINAL)
                            && md.hasModifier(Modifier.STATIC) && isConstType(md.getType())) {
                        i.remove();
                        continue;
                    }
                    if (excludeMode.contains(EXLUDE_MODE.EXCLUDEFIELD) && md.isField()) {
                        i.remove();
                        continue;
                    }
                    if (excludeMode.contains(EXLUDE_MODE.EXCLUDEABSTRACTMETHODS) && md.isAbstract()) {
                        i.remove();
                        continue;
                    }
                    if (md.hasModifier(Modifier.FINAL)
                            && !md.getDeclaringClassName().equals(
                                    cd.getQualifiedName())) {
                        i.remove();
                        continue;
                    }
                    if (isExcluded(cd.getQualifiedName() + "."
                            + md.getName() + (md.isField() ? "" : "(" + md.getArgs() + ")"))) {
                        i.remove();
                        continue;
                    }

                }
                int members = 0;
                int tested = 0;
                for (Iterator i = cd.getMembersIterator(); i.hasNext();) {
                    MemberDescription md = (MemberDescription) i.next();
                    members++;
                    if (refCounter.isCovered(md)) {
                        tested++;
                    }
                }
                results.put(cd.toString(), new Field(members, tested));
            }

            protected void visit(PackageDescr pd) {
                int members = 0;
                int tested = 0;
                int classes = 0;

                for (Object o : pd.getDeclaredClasses()) {
                    ClassDescription cd = (ClassDescription) o;
                    visit(cd);
                    members += results.get(cd.toString()).members;
                    tested += results.get(cd.toString()).tested;
                    classes++;
                }

                for (Object o : pd.getDeclaredPackages()) {
                    PackageDescr sub = (PackageDescr) o;
                    visit(sub);
                    members += results.get(sub.toString()).members;
                    tested += results.get(sub.toString()).tested;
                    classes += results.get(sub.toString()).classes;
                }
                results.put(pd.toString(), new Field(classes, members, tested));
            }
        };

        for (Iterator<ClassDescription> it = api.iterator(); it.hasNext();) {
            ClassDescription cd = it.next();
            if (excludeMode.contains(EXLUDE_MODE.EXCLUDEABSTRACTCLASSES)
                    && cd.isAbstract()) {
                it.remove();
                continue;
            }
            if (excludeMode.contains(EXLUDE_MODE.EXCLUDEINTERFACES)
                    && cd.isInterface()) {
                it.remove();
                continue;
            }
            if (isExcluded(cd.getQualifiedName())
                    || isPackageExcluded(cd.getPackageName())) {
                it.remove();
                continue;
            }
        }
        calc.visit(api);
    }

    void out() {
        this.api = new ArrayList<ClassDescription>();
        api.addAll(refCounter.getClasses());
        filter();
        print();
    }
}

class ReportPlain extends ReportGenerator {

    private final static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(ReportPlain.class);
    final int p0 = 0,
            p1 = 4,
            p2 = 8,
            p3 = 40,
            p4 = 50,
            p5 = 58,
            p6 = 66,
            p7 = 74;
    int packnb = 0;

    public ReportPlain(RefCounter reporter) {
        super(reporter);
    }

    public void print() {
        tab(p0).append(i18n.getString("ReportPlain.report.Coverage"));
        println();
        println();

        tab(p7, '=');
        println();

        tab(p0).append(i18n.getString("ReportPlain.report.Package"));
        tab(p3).append(i18n.getString("ReportPlain.report.classes"));
        println();

        tab(p1).append(i18n.getString("ReportPlain.report.Class"));
        tab(p4).append(i18n.getString("ReportPlain.report.members"));
        tab(p5).append(i18n.getString("ReportPlain.report.tested"));
        tab(p6).append("%%");
        println();

        if (detail.hasMembers()) {
            tab(p2).append(i18n.getString("ReportPlain.report.Member"));
            if (detail.hasCounters()) {
                tab(p6).append(i18n.getString("ReportPlain.report.count"));
            }
            println();
        }

        tab(p7, '=');
        println();

        visit(api);

        tab(p7, '=');
        println();

        tab(p0).append(i18n.getString("ReportPlain.report.Overall"));
        int members = 0;
        int tested = 0;
        int classes = 0;
        for (ClassDescription cd : api) {
            members += results.get(cd.toString()).members;
            tested += results.get(cd.toString()).tested;
            classes++;
        }
        Field all = new Field(classes, members, tested);
        tab(p3).append(all.classes);
        tab(p4).append(all.members);
        tab(p5).append(all.tested);
        if (all.members != 0) {
            tab(p6).append(all.getPercent());
        }
        println();

        tab(p7, '=');
        println();
        println();

        if (detail.hasTwoColumns()) {
            tab(0).append(i18n.getString("ReportPlain.report.Legend"));
            println();
            tab(0).append("+ ").append(i18n.getString("ReportPlain.report.covered"));
            println();
            tab(0).append("- ").append(i18n.getString("ReportPlain.report.uncovered"));
            println();
            println();
        }

        tab(0).append(i18n.getString("ReportPlain.report.Configuration"));

        Option[] options = {Option.TS,
                Option.EXCLUDE_LIST,
                Option.API,
                Option.EXCLUDE_INTERFACES,
                Option.EXCLUDE_ABSTRACT_CLASSES,
                Option.EXCLUDE_ABSTRACT_METHODS,
                Option.EXCLUDE_FIELDS,
                Option.INCLUDE_CONSTANT_FIELDS,
                Option.MODE};

        int t = 0;

        for (Option opt : options) {
            String key = opt.getKey();
            if (config.get(key) == null) {
                continue;
            }
            int l = key.length() - 1;
            if (l > t) {
                t = l;
            }
        }

        t++;

        for (Option opt : options) {
            String key = opt.getKey();
            if (config.get(key) == null) {
                continue;
            }
            println();
            tab(0).append(key.substring(1));
            tab(t).append(config.get(key)[0]);
        }

        println();
        pw.close();
    }

    @Override
    protected void visit(PackageDescr pd) {
        if (pd.equals(top)) {
            super.visit(pd);
            return;
        }
        if (packnb++ > 0 && detail.hasMembers()) {
            tab(p7, '-');
            println();
        }
        int members = results.get(pd.toString()).members;
        int tested = results.get(pd.toString()).tested;
        String percent = results.get(pd.toString()).getPercent();
        tab(p0).append(pd.getQualifiedName());
        tab(p3).append(results.get(pd.toString()).classes);
        tab(p4).append(members);
        tab(p5).append(tested);

        if (members != 0) {
            tab(p6).append(percent);
        }
        println();

        super.visit(pd);
    }
    Erasurator erasurator = new Erasurator();

    @Override
    protected void visit(ClassDescription cd) {
        if (detail.isOnlyPackages()) {
            return;
        }
        if (detail.hasMembers()) {
            println();
        }

        tab(p1).append(classRepr(cd));
        int members = results.get(cd.toString()).members;
        int tested = results.get(cd.toString()).tested;
        String percent = results.get(cd.toString()).getPercent();
        tab(p4).append(members);
        tab(p5).append(tested);
        if (members != 0) {
            tab(p6).append(percent);
        }
        println();
        erasurator.parseTypeParameters(cd);
        super.visit(cd);
    }

    @Override
    protected void visit(MemberDescription x) {
        printMember(x, refCounter.getCoverCount(x));
    }

    void printMember(MemberDescription md, int coverCount) {
        if (!detail.hasMembers()) { //XXX|| !isCounted(x))
            return;
        }

        if (detail.isOnlyUncovered() && coverCount > 0) {
            return;
        }

        if (detail.isOnlyCovered() && coverCount == 0) {
            return;
        }
        md = erasurator.processMember(md);
        tab(p2 - 2).append(coverCount > 0 ? '+' : '-');
        tab(p2).append(md.getName()
                + (md.isField() ? "" : "(" + md.getSimplifiedArgs() + ")"));

        if (detail.hasCounters() && coverCount > 0) {
            tab(p5).append(String.format("%11s", coverCount));
        }

        println();
    }
    StringBuffer line = new StringBuffer(120);

    StringBuffer tab(int p) {
        return tab(p, ' ');
    }

    StringBuffer tab(int p, char c) {
        for (int n = p - line.length(); n > 0; n--) {
            line.append(c);
        }

        return line;
    }

    void println() {
        pw.println(line.toString());
        line.setLength(0);
    }
}

interface XC {

    String REPORT = "report";
    String HEAD = "head";
    String HEAD_PROPERTY = "property";
    String HEAD_PROPERTY_NAME = "name";
    String HEAD_PROPERTY_VALUE = "value";
    String PACKAGE = "package";
    String PACKAGE_NAME = "name";
    String PACKAGE_QNAME = "qname";
    String PACKAGE_MEMBERS = "members";
    String PACKAGE_TESTED = "tested";
    String CLASS = "class";
    String CLASS_NAME = "name";
    String CLASS_MEMBERS = "members";
    String CLASS_TESTED = "tested";
    String CLASS_TYPEARGS = "typeArgs";
    String MEMBER_NAME = "name";
    String MEMBER_VMSIG = "vmsig";
    String MEMBER_SIG = "sig";
    String MEMBER_TYPE = "type";
    String MEMBER_TESTED = "tested";
    String MEMBER_REFCOUNT = "refcount";
}

class ReportXML extends ReportGenerator {

    TransformerHandler ser;
    Erasurator erasurator = new Erasurator();

    public ReportXML(RefCounter reporter) {
        super(reporter);
    }

    public void printHead() {
        startElement(XC.HEAD);
        for (String key : config.keySet()) {
            for (String value : config.get(key)) {
                // XXX to think about this repr
                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute("", "", XC.HEAD_PROPERTY_NAME, "", key);
                atts.addAttribute("", "", XC.HEAD_PROPERTY_VALUE, "", value);
                startElement(XC.HEAD_PROPERTY, atts);
                endElement(XC.HEAD_PROPERTY);
            }
        }
        endElement(XC.HEAD);
    }

    @Override
    public void print() {
        SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
        stf.setAttribute("indent-number", 4);
        Properties outputProps = new Properties();
        Result result;
        outputProps.put(OutputKeys.INDENT, "yes");
        outputProps.put(OutputKeys.ENCODING, "UTF-8");
        try {
            ser = stf.newTransformerHandler();
            ser.getTransformer().setOutputProperties(outputProps);
            result = new StreamResult(pw);
            ser.setResult(result);
            ser.startDocument();
            startElement(XC.REPORT);
            printHead();
            visit(api);
            endElement(XC.REPORT);
            ser.endDocument();

            pw.flush();
            pw.close();

            /////////////////////
            //ser = stf.newTransformerHandler(new StreamSource(new File("pp.xsl")));
            //ser.getTransformer().transform(new StreamSource("out.xml"), new StreamResult(System.out));
        } catch (TransformerException ex) {
            SwissKnife.reportThrowable(ex);
        } catch (SAXException ex) {
            SwissKnife.reportThrowable(ex);
        }
    }

    @Override
    protected void visit(PackageDescr pd) {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", XC.PACKAGE_NAME, "", pd.getName());
        atts.addAttribute("", "", XC.PACKAGE_QNAME, "", pd.getQualifiedName());
        atts.addAttribute("", "", XC.PACKAGE_MEMBERS, "", String.valueOf(results.get(pd.toString()).members));
        atts.addAttribute("", "", XC.PACKAGE_TESTED, "", String.valueOf(results.get(pd.toString()).tested));
        startElement(XC.PACKAGE, atts);
        super.visit(pd);
        endElement(XC.PACKAGE);
    }

    @Override
    protected void visit(ClassDescription cd) {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", XC.CLASS_NAME, "", constructClassName(cd));
        if (cd.getTypeParameters() != null) {
            atts.addAttribute("", "", XC.CLASS_TYPEARGS, "", cd.getTypeParameters());
        }
        String[] modifiers = Modifier.toString(cd.getMemberType(), cd.getModifiers(), false).split(" ");
        for (String modifier : modifiers) {
            // XXX should be rewitten
            if (!modifier.startsWith("!") && !modifier.startsWith("acc_")) {
                atts.addAttribute("", "", modifier, "", "true");
            }
        }
        atts.addAttribute("", "", XC.CLASS_MEMBERS, "", String.valueOf(results.get(cd.toString()).members));
        atts.addAttribute("", "", XC.CLASS_TESTED, "", String.valueOf(results.get(cd.toString()).tested));
        startElement(XC.CLASS, atts);
        erasurator.parseTypeParameters(cd);
        super.visit(cd);
        endElement(XC.CLASS);
    }

    private String constructClassName(ClassDescription cd) {
        int pos = cd.getQualifiedName().lastIndexOf('.');
        if (pos == -1) {
            return cd.getName();
        } else {
            return cd.getQualifiedName().substring(++pos);
        }
    }

    @Override
    protected void visit(MemberDescription x) {
        printMember(x, refCounter.getCoverCount(x));
    }

    private static String convertArgsToVM(String str) {
        String[] types = str.split(",");
        StringBuffer result = new StringBuffer("(");
        for (String type : types) {
            result.append(convertTypeToVM(type));
        }
        result.append(")");
        return result.toString();
    }

    private static String convertTypeToVM(String type) {
        String res = PrimitiveTypes.getVMPrimitiveType(type);
        if (res != null) {
            return res;
        }
        return (type.length() == 0) ? "" : "L" + type.replace('.', '/') + ";";
    }

    void printMember(MemberDescription md, int coverCount) {
        String type = md.getMemberType().toString();
        String sig = md.isField() ? md.getSimplifiedType()
                : md.isConstructor() ? "(" + md.getSimplifiedArgs() + ")"
                : /* meth */ "(" + md.getSimplifiedArgs() + ")";

        md = erasurator.processMember(md);
        String vmsig = md.isField() ? convertTypeToVM(md.getType())
                : md.isConstructor() ? convertArgsToVM(md.getArgs())
                : /* meth */ convertArgsToVM(md.getArgs()) + convertTypeToVM(md.getType());
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", XC.MEMBER_NAME, "", md.getName());
        atts.addAttribute("", "", XC.MEMBER_VMSIG, "", vmsig);
        atts.addAttribute("", "", XC.MEMBER_SIG, "", sig);

        if (md.isMethod()) {
            atts.addAttribute("", "", XC.MEMBER_TYPE, "", md.getType());
        }

        String[] modifiers = Modifier.toString(md.getMemberType(), md.getModifiers(), false).split(" ");
        for (String modifier : modifiers) {
            // XXX should be rewitten
            if (!modifier.startsWith("!") && !modifier.startsWith("acc_")) {
                atts.addAttribute("", "", modifier, "", "true");
            }
        }
        atts.addAttribute("", "", XC.MEMBER_TESTED, "", coverCount > 0 ? "1" : "0");
        atts.addAttribute("", "", XC.MEMBER_REFCOUNT, "", "" + coverCount);
        startElement(type, atts);
        endElement(type);
    }

    private void startElement(String name, AttributesImpl attrs) {
        try {
            ser.startElement("", "", name, attrs);
        } catch (SAXException e) {
            SwissKnife.reportThrowable(e);
        }
    }

    private void startElement(String name, String... attrs) {
        AttributesImpl attrImpl = new AttributesImpl();
        assert attrs.length % 2 == 0;
        for (int i = 0; i < attrs.length; i += 2) {
            attrImpl.addAttribute("", "'", attrs[i], "", attrs[i + 1]);
        }
        try {
            ser.startElement("", "", name, attrImpl);
        } catch (SAXException e) {
            SwissKnife.reportThrowable(e);
        }
    }

    private void endElement(String name) {
        try {
            ser.endElement("", "", name);
        } catch (SAXException e) {
            SwissKnife.reportThrowable(e);
        }
    }
}
