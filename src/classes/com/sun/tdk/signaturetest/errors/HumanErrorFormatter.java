/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.errors;

import com.sun.tdk.signaturetest.util.Level;
import com.sun.tdk.signaturetest.model.ConstructorDescr;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.model.MethodDescr;
import com.sun.tdk.signaturetest.plugin.MessageTransformer;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class HumanErrorFormatter extends SortedErrorFormatter {

    private static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(SortedErrorFormatter.class);
    private final Level level;

    /**
     * Assign the given <b>PrintWriter</b> to print error messages.
     */
    public HumanErrorFormatter(PrintWriter out, boolean isv, Level l) {
        super(out, isv);
        level = l;
    }

    public void printErrors() {

        MessageTransformer t = PluginAPI.BEFORE_MESSAGE_SORT.getMessageTransformer();
        if (t != null) {
            failedMessages = t.changeMessageList(failedMessages);
        }

        sortErrors();

        ErrorComparator ec = new ErrorComparator();

        int length = failedMessages.size();
        Chain ch = new Chain(failedMessages);

        for (int i = 0; i < length; i++) {
            Message e1 = failedMessages.get(i);
            if (e1 == null) {
                continue;
            }

            int j = i;

            while (j + 1 < failedMessages.size()) {
                Message e2 = failedMessages.get(j + 1);
                if (ec.compare(e1, e2) == 0) {
                    j++;
                } else {
                    break;
                }
            }

            List<Message> currentGroup = failedMessages.subList(i, j + 1);

            Handler h = constructHandlerChain();

            h.process(currentGroup, ch);

            i = j;

        }

        ch.finishProcessing();

        supressExtraErrors();

        Iterator<Message> it = failedMessages.iterator();
        numErrors = 0;
        numWarnings = 0;

        while (it.hasNext()) {
            Message m = it.next();
            if (level.intValue() <= m.getLevel().intValue()) {
                numErrors++;
            } else {
                numWarnings++;
            }
        }

        sortErrorsForOutput();

        outProcessedErrors();

    }

    protected void outProcessedErrors() {
        boolean hasHeader = false;
        MessageType lastType = null;
        String cl = "";

        for (Message current : failedMessages) {
            if (current == null) {
                continue;
            }

            String ccl = current.className;
            // issue 33
            if (current.errorObject != null && current.errorObject.isInner()) {
                ccl = current.errorObject.getQualifiedName();
            }

            if (current.messageType == MessageType.ADD_CLASSES) {
                lastType = current.messageType;
                out.println("\n+ Class " + ccl);
                cl = ccl;
                continue;
            }

            if (current.messageType == MessageType.MISS_CLASSES) {
                lastType = current.messageType;
                out.println("\n- Class " + ccl);
                cl = ccl;
                continue;
            }

            if (!cl.equals(ccl)) {
                cl = ccl;
                lastType = null;
                out.println("\nClass " + cl);
            }

            if (current.messageType != lastType) {
                hasHeader = true;

                out.println("  " + current.messageType.getLocMessage());
                lastType = current.messageType;
            }
            if (hasHeader) {
                if (current.definition.isEmpty()) {
                    out.println(current.className);
                } else {
                    StringBuffer name = new StringBuffer();
                    if (current.messageType != MessageType.CHNG_CLASSES_MEMBERS) {
                        out.println("    " + current.definition);
                        if (isVerbose() && !current.tail.isEmpty()) {
                            out.println(i18n.getString("SortedErrorFormatter.error.affected", current.tail));
                        }
                    } else {

                        if (current.errorObject.getMemberType() != MemberType.CLASS) {
                            name.append("    ");
                            name.append(current.errorObject);
                            out.println(name);
                        }

                        out.print(current.definition);
                    }
                }
            } else {
                out.println(current);
            }
        }
        if (!failedMessages.isEmpty()) {
            out.println("");
        }
    }

    protected Handler constructHandlerChain() {
        //AnnotationHandler must be last but one
        //Other *Handler may be in any order
        return new ModifiersHandler().setNext(
                new ReturnTypeHandler().setNext(
                        new TypeParametersHandler().setNext(
                                new ThrowsHandler().setNext(
                                        new ConstantValueHandler().setNext(
                                                new AnnotationHandler())))));
    }

    private void sortErrorsForOutput() {
        Collections.sort(failedMessages, new Comparator<Message>() {
            // 1 - By class
            // 2 - By object (CLSS, method, field, other)
            // 3 - By message type
            // 4 - By definition
            @Override
            public int compare(Message m1, Message m2) {
                if (m1 == null && m2 == null) {
                    return 0;
                }
                if (m1 == null) {
                    return -1;
                }
                if (m2 == null) {
                    return 1;
                }

                int comp = m1.className.compareTo(m2.className);

                if (comp == 0) {
                    comp = m1.errorObject.getMemberType().compareTo(m2.errorObject.getMemberType());
                    if (comp == 0) {
                        comp = m1.messageType.compareTo(m2.messageType);
                        if (comp == 0) {
                            comp = m1.definition.compareTo(m2.definition);
                            if (comp == 0) {
                                if (m1.tail != null && m2.tail != null) {
                                    comp = m1.tail.compareTo(m2.tail);
                                } else {
                                    if (m1.tail == null) {
                                        comp = -1;
                                    } else {
                                        comp = 1;
                                    }
                                }
                            }
                        }
                        return comp;
                    }
                }
                return comp;
            }
        });
    }

    protected void sortErrors() {
        Collections.sort(failedMessages, new ErrorComparator());
    }

    // Issue 39 - Suppress similar messages in human-readable formatter
    private void supressExtraErrors() {
        Collections.sort(failedMessages, new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                if (!isSameKind(m1, m2)) {
                    return -1;  //bad practice, but...
                }
                return m1.className.compareTo(m2.className);
            }
        });

        List<Message> toRemove = new ArrayList<>();

        loop:
        for (int i = 0; i < failedMessages.size(); i++) {
            Message m1 = failedMessages.get(i);
            int last = i;
            for (int j = i + 1; j < failedMessages.size(); j++) {
                Message m2 = failedMessages.get(j);
                if (!isSameKind(m1, m2)) {
                    if (last == i) {
                        i = j;
                        continue loop;
                    } else {
                        break;
                    }
                } else {
                    last = j;
                }
            }
            boolean found = false;
            List<Message> rem = new ArrayList<>();
            for (int k = i; k <= last; k++) {
                Message m = failedMessages.get(k);
                if (m.className.equals(m.errorObject.getDeclaringClassName())) {
                    found = true;
                } else {
                    rem.add(m);
                }
            }

            if (found) {
                toRemove.addAll(rem);
            }

            i = last;
        }

        failedMessages.removeAll(toRemove);

    }

    private static boolean isSameKind(Message m1, Message m2) {
        if (m1 == null || m2 == null) {
            return false;
        }
        return m1.errorObject.equals(m2.errorObject)
                && m1.definition.equals(m2.definition)
                && m1.tail.equals(m2.tail)
                && m1.messageType.equals(m2.messageType);
    }

    private static class ErrorComparator implements Comparator<Message> {

        @Override
        public int compare(Message msg1, Message msg2) {
            MemberDescription md1 = msg1.errorObject;
            MemberDescription md2 = msg2.errorObject;

            int comp = md1.getQualifiedName().compareTo(md2.getQualifiedName());

            if (comp == 0) {
                comp = md1.getMemberType().compareTo(md2.getMemberType());
                if (comp == 0 && (md1.getMemberType() == MemberType.METHOD || md1.getMemberType() == MemberType.CONSTRUCTOR)) {

                    if (md1 instanceof MethodDescr && md2 instanceof MethodDescr) {

                        MethodDescr mth1 = (MethodDescr) md1;
                        MethodDescr mth2 = (MethodDescr) md2;
                        comp = mth1.getSignature().compareTo(mth2.getSignature());

                    } else if (md1 instanceof ConstructorDescr && md2 instanceof ConstructorDescr) {

                        ConstructorDescr co1 = (ConstructorDescr) md1;
                        ConstructorDescr co2 = (ConstructorDescr) md2;
                        comp = co1.getSignature().compareTo(co2.getSignature());

                    }
                }
                if (comp == 0) {
                    comp = msg1.className.compareTo(msg2.className);
                }
            }
            return comp;
        }
    }
}
