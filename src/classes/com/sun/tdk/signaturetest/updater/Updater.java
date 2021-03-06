/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.updater;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.util.*;
import java.io.*;
import java.lang.reflect.Field;

import com.sun.tdk.signaturetest.model.AnnotationItem;
import com.sun.tdk.signaturetest.util.SwissKnife;

/**
 * @author Mikhail Ershov
 */
public class Updater extends DefaultHandler {

    private UpdateRecord ur;
    private List<Command> commands;
    private String lastData;
    private PrintWriter log;

    public boolean perform(String updFile, String fromFile, String toFile, PrintWriter log) {
        if (log != null) {
            this.log = log;
        }
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            SAXParser sp = spf.newSAXParser();
            sp.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            sp.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            sp.parse(updFile, this);
            return applyUpdate(fromFile, toFile);
        } catch (Exception e) {
            SwissKnife.reportThrowable(e);
            return false;
        }
    }

    public void startDocument() throws SAXException {
        commands = new LinkedList<>();
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("update")) {
            ur = new UpdateRecord();
            fillUR(ur, attributes);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (lastData == null) {
            lastData = new String(ch, start, length);
        } else {
            lastData += new String(ch, start, length);
        }
        lastData = lastData.trim();
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        Command c = null;
        if (ur != null) {
            if (ur.atype.equalsIgnoreCase("removeclass")) {
                RemoveClass rc = new RemoveClass(log);
                rc.comments = ur.acomments;
                rc.id = ur.aid;
                rc.className = ur.aclassname;
                c = rc;
            } else if (ur.atype.equalsIgnoreCase("removepackage")) {
                RemovePackage rp = new RemovePackage(log);
                rp.comments = ur.acomments;
                rp.id = ur.aid;
                rp.packageName = ur.apackagename;
                c = rp;
            } else if (ur.atype.equalsIgnoreCase("addclass")) {
                AddClass ac = new AddClass(log);
                ac.comments = ur.acomments;
                ac.id = ur.aid;
                ac.className = ur.aclassname;
                ac.body = lastData;
                c = ac;
            } else if (ur.atype.equalsIgnoreCase("removemember")) {
                RemoveMember rm = new RemoveMember(log);
                rm.comments = ur.acomments;
                rm.id = ur.aid;
                rm.className = ur.aclassname;
                rm.memberName = ur.amember;
                c = rm;
            } else if (ur.atype.equalsIgnoreCase("addmember")) {
                AddMember am = new AddMember(log);
                am.comments = ur.acomments;
                am.id = ur.aid;
                am.className = ur.aclassname;
                am.memberName = ur.amember;
                c = am;
            } else if (ur.atype.equalsIgnoreCase("changemember")) {
                ChangeMember cm = new ChangeMember(log);
                cm.comments = ur.acomments;
                cm.id = ur.aid;
                cm.className = ur.aclassname;
                cm.memberName = ur.amember;
                cm.newMemberName = ur.anewmember;
                c = cm;
            }
            if (c == null) {
                throw new IllegalArgumentException("Unknown type \"" + ur.atype + "\" for update");
            }
            c.validate(); // IllegalArgumentException can be thrown
            commands.add(c);
        }
        ur = null;
        lastData = null;
    }

    private static void fillUR(UpdateRecord ur, Attributes attributes) {
        Field[] fs = UpdateRecord.class.getDeclaredFields();
        try {
            for (Field f : fs) {
                String fName = f.getName();
                if (fName.startsWith("a")) {
                    f.set(ur, attributes.getValue(fName.substring(1)));
                }
            }
        } catch (IllegalAccessException e) {
            SwissKnife.reportThrowable(e);
        }
    }

    private void processCommands(Collection<Command> commands, Updater.SigList sl) {
        for (Command command : commands) {
            command.perform(sl);
        }
    }

    private boolean applyUpdate(String from, String to) {
        try {
            // read src
            SigList sl = readInput(from);

            // transform
            processCommands(commands, sl);
            commands.clear();

            // remove some empty lines
            sl.pack();

            // write result
            writeOut(to, sl);

            return true;

        } catch (IOException e) {
            SwissKnife.reportThrowable(e);
            return false;
        }
    }

    private void writeOut(String to, SigList sl) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(to))) {
            sl.print(pw);
        }
    }

    private SigList readInput(String from) throws IOException {
        try (FileReader fr = new FileReader(from);
             LineNumberReader r = new LineNumberReader(new BufferedReader(fr))) {
            SigList sl = new SigList();
            String s;
            while ((s = r.readLine()) != null) {
                sl.add(s);
            }

            return sl;
        }
    }

    class SigList {

        private final List<String> sigList = new ArrayList<>();
        private int startPos = -1;

        public boolean findClass(String className) {
            startPos = -1;
            int i = 0;
            while (i < sigList.size()) {
                String l = sigList.get(i);
                if (l.startsWith("CLSS ") && l.endsWith(" " + className)) {
                    startPos = i;
                    return true;
                }
                i++;
            }
            return false;
        }

        public void removeCurrentClass() {
            if (startPos >= 0) {
                while (!sigList.get(startPos).trim().isEmpty()) {
                    sigList.remove(startPos);
                }
            }
        }

        public void addText(String body) {
            StringTokenizer st = new StringTokenizer(body, "\n");
            sigList.add("");
            while (st.hasMoreTokens()) {
                sigList.add(st.nextToken().trim());
            }
            sigList.add("");
        }

        public boolean findPackageMember(String packageName) {
            startPos = -1;
            int i = 0;
            final String pSig = " " + packageName + ".";
            while (i < sigList.size()) {
                String l = sigList.get(i);
                if (l.startsWith("CLSS ")) {
                    int x = l.indexOf('<');
                    int y = l.indexOf(pSig);
                    if (y > 0 && ((y < x) || (x == -1))) {
                        startPos = i;
                        return true;
                    }
                }
                i++;
            }
            return false;
        }

        public boolean removeMember(String memberName) {
            if (startPos >= 0) {
                for (int i = startPos; i < sigList.size(); i++) {
                    String l = sigList.get(i).trim();
                    if (memberName.equals(l)) {
                        sigList.remove(i);
                        return true;
                    } else {
                        if (l.isEmpty()) {
                            break;
                        }
                    }
                }
            }
            return false;
        }

        public boolean changeMember(String oldMember, String newMember) {
            if (startPos >= 0) {
                for (int i = startPos; i < sigList.size(); i++) {
                    String l = sigList.get(i).trim();
                    if (oldMember.equals(l)) {
                        sigList.set(i, newMember);
                        return true;
                    } else {
                        if (l.isEmpty()) {
                            break;
                        }
                    }
                }
            }
            return false;
        }

        public void pack() {
            boolean empty = false;
            for (int i = 0; i < sigList.size(); i++) {
                String l = sigList.get(i);
                if (l.trim().isEmpty()) {
                    if (empty) {
                        sigList.remove(i--);
                        continue;
                    } else {
                        empty = true;
                    }
                } else {
                    empty = false;
                }
            }
        }

        public boolean addMember(String memberName) {
            if (startPos >= 0) {
                for (int i = startPos + 1; i < sigList.size(); i++) {
                    String l = sigList.get(i).trim();
                    if (!l.startsWith(AnnotationItem.ANNOTATION_PREFIX)) {
                        sigList.add(i, memberName);
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean add(String s) {
            return sigList.add(s);
        }

        public void print(PrintWriter pw) {
            for (String o : sigList) {
                pw.write(o + '\n');
            }
        }
    }

    // data bean
    private static class UpdateRecord {

        String atype;
        String aclassname;
        String aid;
        String acomments;
        String apackagename;
        String amember;
        String anewmember;
    }
}
