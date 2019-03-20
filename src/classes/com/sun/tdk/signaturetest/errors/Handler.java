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
import com.sun.tdk.signaturetest.core.ClassHierarchy;
import com.sun.tdk.signaturetest.errors.ErrorFormatter.Message;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * @author Sergey Glazyrin
 * @author Mikhail Ershov
 */
public abstract class Handler {

    private Handler next;
    // Get the level specifying which messages will be processed by this Handler.
    // Message levels lower than this level will be discarded.
    private Level level = Level.SEVERE;

    public Handler setNext(Handler h) {
        next = h;
        return this;
    }

    void process(List<Message> l, Chain ch) {
        if (acceptMessageList(l)) {
            writeMessage(l, ch);
        }
        if (next != null) {
            next.process(l, ch);
        }
    }


    /*
     *  First filtering method. By default we don't process added/missed annotation
     *  messages and "short" lists
     *  handler which have to process such cases have to override this method.
     */
    boolean acceptMessageList(List<Message> l) {
        if (l.size() < 2) {
            return false;
        }
        Message e1 = l.get(0);
        Message e2 = l.get(1);

        return !(isAnnotationMessage(e1) || isAnnotationMessage(e2));

    }

    protected abstract void writeMessage(List<Message> l, Chain ch);

    boolean isAnnotationMessage(Message m) {
        return (m.messageType == MessageType.ADD_ANNO
                || m.messageType == MessageType.MISS_ANNO);
    }

    protected static final ArrayList<String> EMPTY_ARRAY_LIST = new ArrayList<>();

    protected static ArrayList<String> stringToArrayList(String source, String delimiter) {
        if ((source == null) || source.isEmpty()) {
            return EMPTY_ARRAY_LIST;
        }

        String[] strA;
        ArrayList<String> result = new ArrayList<>();
        try {
            strA = source.split(delimiter);
        } catch (PatternSyntaxException e) {
            result.add(source);
            return result;
        }
        result.addAll(Arrays.asList(strA));
        return result;
    }

    protected boolean canBeSubclassed(String className, ClassHierarchy clHier) {
        try {
            ClassDescription cd = clHier.load(className);
            return SwissKnife.canBeSubclassed(cd);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    protected boolean isAssignableTo(String origType, String newType, ClassHierarchy clHier) {
        if (origType.equals(newType)) {
            return true;
        }
        try {
            ClassDescription cd = clHier.load(newType);
            SuperInterface[] ints = cd.getInterfaces();
            for (SuperInterface anInt : ints) {
                if (origType.equals(anInt.getQualifiedName())) {
                    return true;
                }
            }
            return clHier.isSubclass(newType, origType);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    protected Level getLevel() {
        return level;
    }

    protected void setLevel(Level level) {
        this.level = level;
    }

    protected void setMessageLevel(Message m) {
        m.setLevel(level);
    }
}

abstract class PairedHandler extends Handler {

    protected Message me1;
    protected Message me2;
    protected MemberDescription m1;
    protected MemberDescription m2;
    protected Message newM;

    final protected void writeMessage(List<Message> l, Chain ch) {

        init(l);

        String clName;
        if (m1.isClass() || m1.isInterface()) {
            clName = m1.getQualifiedName();
        } else {
            clName = m1.getDeclaringClassName();
        }

        newM = new Message(MessageType.CHNG_CLASSES_MEMBERS, clName, "", "", m1);

        if (proc()) {
            ch.setMessageProcessed(me1);
            ch.setMessageProcessed(me2);
            ch.addMessage(newM);
        }

    }

    protected void init(List<Message> l) {
        me1 = l.get(0);
        me2 = l.get(1);

        m1 = me1.errorObject;
        m2 = me2.errorObject;
    }

    protected void addDef(String def) {
        newM.definition += def + "\n";
    }

    abstract protected boolean proc();
}

class ModifiersHandler extends PairedHandler {

    protected boolean proc() {

        Collection<String> c1 = Handler.stringToArrayList(Modifier.toString(m1.getMemberType(), m1.getModifiers(), true), " ");
        Collection<String> c2 = Handler.stringToArrayList(Modifier.toString(m2.getMemberType(), m2.getModifiers(), true), " ");

        if (!c1.equals(c2)) {
            Collection<String> c3 = new ArrayList<>(c2);
            c2.removeAll(c1);
            c1.removeAll(c3);

            if (!c1.isEmpty()) {
                addDef("    - " + c1.toString());
            }
            if (!c2.isEmpty()) {
                addDef("    + " + c2.toString());
            }
            return true;
        }
        return false;
    }
}

class ReturnTypeHandler extends PairedHandler {

    protected boolean proc() {
        String t1 = m1.getType();
        String t2 = m2.getType();

        if (t1 != null && !t1.equals(t2)) {
            if (!t1.equals("")) {
                addDef("    - type: " + t1);
            }
            if ((t2 != null) && (!t2.equals(""))) {
                addDef("    + type: " + t2);
            }
            return true;
        }
        return false;
    }
}

class ConstantValueHandler extends PairedHandler {

    boolean acceptMessageList(List<Message> l) {
        if (l.size() >= 2) {
            MemberDescription m1 = (l.get(0)).errorObject;
            MemberDescription m2 = (l.get(1)).errorObject;

            if (m1 instanceof FieldDescr && m2 instanceof FieldDescr) {
                FieldDescr f1 = (FieldDescr) m1;
                FieldDescr f2 = (FieldDescr) m2;

                return f1.getConstantValue() != null || f2.getConstantValue() != null;
            }
        }
        return false;
    }

    protected boolean proc() {

        FieldDescr f1 = (FieldDescr) m1;
        FieldDescr f2 = (FieldDescr) m2;

        String v1 = f1.getConstantValue() == null ? "" : f1.getConstantValue();
        String v2 = f2.getConstantValue() == null ? "" : f2.getConstantValue();
        if (!v1.equals(v2)) {
            if (!"".equals(v1)) {
                addDef("    - value: " + v1);
            }
            if (!"".equals(v2)) {
                addDef("    + value: " + v2);
            }

            return true;
        }
        return false;
    }
}

class TypeParametersHandler extends PairedHandler {

    protected boolean proc() {

        Collection<String> c1 = Handler.stringToArrayList(trimTypeParameter(m1.getTypeParameters()), ", ");
        Collection<String> c2 = Handler.stringToArrayList(trimTypeParameter(m2.getTypeParameters()), ", ");

        if (!c1.equals(c2)) {
            Collection<String> c3 = new ArrayList<>(c2);
            c2.removeAll(c1);
            c1.removeAll(c3);

            if (!c1.isEmpty()) {
                addDef("    - Type parameters: " + c1.toString().trim());
            }
            if (!c2.isEmpty()) {
                addDef("    + Type parameters: " + c2.toString().trim());
            }
            return true;
        }
        return false;
    }

    private String trimTypeParameter(String s) {
        if ((s == null) || s.isEmpty()) {
            return "";
        }
        StringBuffer sb = new StringBuffer(s);

        if (sb.charAt(0) == '<') {
            sb.deleteCharAt(0);
        }

        if (sb.charAt(sb.length() - 1) == '>') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString().trim();
    }
}

class ThrowsHandler extends PairedHandler {

    protected boolean proc() {

        Collection<String> c1 = Handler.stringToArrayList(m1.getThrowables(), ",");
        Collection<String> c2 = Handler.stringToArrayList(m2.getThrowables(), ",");

        if (c1 != null && !c1.equals(c2)) {

            Collection<String> c3 = new ArrayList<>(c2);
            c2.removeAll(c1);
            c1.removeAll(c3);

            if (!c1.isEmpty()) {
                addDef("    - Throws: " + c1.toString());
            }
            if (!c2.isEmpty()) {
                addDef("    + Throws: " + c2.toString());
            }
            return true;
        }
        return false;
    }
}

class AnnotationHandler extends PairedHandler {

    protected boolean proc() {

        Collection<String> c1 = annotationListToArrayList(m1.getAnnoList());
        Collection<String> c2 = annotationListToArrayList(m2.getAnnoList());

        if (!c1.equals(c2)) {
            Collection<String> c3 = new ArrayList<>(c2);
            c2.removeAll(c1);
            c1.removeAll(c3);

            if (!c1.isEmpty()) {
                addDef("    - Anno: " + c1.toString());
            }
            if (!c2.isEmpty()) {
                addDef("    + Anno: " + c2.toString());
            }
            return true;
        }
        return false;

    }

    private ArrayList<String> annotationListToArrayList(AnnotationItem[] a) {
        if (a == null) {
            return EMPTY_ARRAY_LIST;
        }
        ArrayList<String> result = new ArrayList<>();
        for (AnnotationItem annotationItem : a) {
            result.add(annotationItem.getName());
        }
        return result;
    }
}
