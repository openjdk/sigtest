/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.*;

/**
 * <b>ClassCorrector</b> is the main part of solving problems related with
 * hidden language elements<p>
 *
 * <li><b>public class (interface) extends package local class
 * (interface)</b><br> Sigtest should ignore base class and/or implemented
 * interfaces and move all visible base's members to the nearest visible
 * SUBclass like Javadoc do since version 1.5.</li> <li><b>public inner class
 * extends private inner class</b><br> Similar solution. But Javadoc ignores
 * such classes and it looks like a bug in Javadoc </li> <li><b>public method
 * throws private exception</b><br> Sigtest should substitute invisible
 * exception to the nearest visible SUPERclass. Javadoc doesn't do it and as
 * result it generates insufficient documentation </li>
 *
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
public class ClassCorrector implements Transformer {

    protected ClassHierarchy classHierarchy = null;
    private Log log;
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(ClassCorrector.class);

    public ClassCorrector(Log log) {
        this.log = log;
    }

    public ClassDescription transform(ClassDescription cl) throws ClassNotFoundException {

        classHierarchy = cl.getClassHierarchy();

        replaceInvisibleExceptions(cl);
        replaceInvisibleInMembers(cl);
        // 1)replace invisible return-types
        // 2)fix invisible parameter types
        fixMethods(cl);
        removeInvisibleInterfaces(cl);
        fixInvisibleSuperclasses(cl);
        removeDuplicatedConstants(cl);
        checkClassTypeParameters(cl);
        removeInvisibleAnnotations(cl);
        additionalChecks(cl);

        return cl;
    }

    private void additionalChecks(ClassDescription cl) throws ClassNotFoundException {
        if (classHierarchy.isClassVisibleOutside(cl)) {

            if (!cl.hasModifier(Modifier.ABSTRACT)) {
                return;
            }

            boolean ctorExists = false;

            ConstructorDescr[] ctors = cl.getDeclaredConstructors();
            for (ConstructorDescr c : ctors) {
                if (c.hasModifier(Modifier.PUBLIC) || c.hasModifier(Modifier.PROTECTED)) {
                    ctorExists = true;
                    break;
                }
            }

            if (!ctorExists) {
                return;
            }

            MethodDescr[] methods = cl.getDeclaredMethods();
            for (MethodDescr mr : methods) {
                if ((mr.isMethod()) && !mr.hasModifier(Modifier.PUBLIC)
                        && !mr.hasModifier(Modifier.PROTECTED) && mr.hasModifier(Modifier.ABSTRACT)) {
                    String[] invargs = {cl.getQualifiedName(), mr.toString()};
                    log.storeWarning(i18n.getString("ClassCorrector.error.class.useless_abst_public_class", invargs), null);
                }
            }
        }
    }

    /**
     * Sigtest should substitute invisible exception to the nearest visible
     * SUPERclass.
     */
    private void replaceInvisibleExceptions(ClassDescription c) throws ClassNotFoundException {

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isMethod() || mr.isConstructor()) {
                replaceInvisibleExceptions(mr);
            }
        }
    }

    private void replaceInvisibleExceptions(MemberDescription mr) throws ClassNotFoundException {

        String throwables = mr.getThrowables();

        if (!MemberDescription.EMPTY_THROW_LIST.equals(throwables)) {
            boolean mustCorrect = false;
            StringBuffer sb = new StringBuffer();

            int startPos = 0, pos;
            do {
                if (sb.length() != 0) {
                    sb.append(MemberDescription.THROWS_DELIMITER);
                }

                String exceptionName;
                pos = throwables.indexOf(MemberDescription.THROWS_DELIMITER, startPos);
                if (pos != -1) {
                    exceptionName = throwables.substring(startPos, pos);
                    startPos = pos + 1;
                } else {
                    exceptionName = throwables.substring(startPos);
                }

                if (isInvisibleClass(exceptionName)) {
                    List supers = classHierarchy.getSuperClasses(exceptionName);
                    exceptionName = findVisibleReplacement(exceptionName, supers, "java.lang.Throwable", true);
                    mustCorrect = true;
                }

                sb.append(exceptionName);

            } while (pos != -1);

            if (mustCorrect) {
                String[] invargs = {mr.getQualifiedName(), throwables, sb.toString()};
                log.storeWarning(i18n.getString("ClassCorrector.message.throwslist.changed", invargs), null);

                mr.setThrowables(sb.toString());
            }
        }
    }

    private String findVisibleReplacementAndCheckInterfaces(String clName, List supers, String replaceWithClassName) throws ClassNotFoundException {

        // is it public inner class of hidden outer?
        if (isPublicInner(clName)) {
            return null;
        }

        String replacement = findVisibleReplacement(clName, supers, replaceWithClassName, true);

        Set oldInt = classHierarchy.getAllImplementedInterfaces(clName);

        if (oldInt.size() != 0) {

            Set<?> newInt = classHierarchy.getAllImplementedInterfaces(replacement);

            oldInt.removeAll(newInt); // diff

            // remove all superinterfaces from the diff
            removeSuperInterfaces(oldInt);
            int visibleInterfaces = 0;
            String iName = null;

            for (Object o : oldInt) {
                String nextInt = (String) o;
                if (!isInvisibleClass(nextInt)) {
                    visibleInterfaces++;
                    iName = nextInt;
                }
            }

            if ("java.lang.Object".equals(replacement) && visibleInterfaces == 1) {
                return iName;
            }

            if (visibleInterfaces > 0) {
                return null;
            }
        }

        return replacement;
    }

    private void getPaths2(List<ArrayList<String>> paths, List<String> currentPath, String intFrom, String intTo) {
        String[] sis = new String[]{};
        try {
            sis = classHierarchy.getSuperInterfaces(intFrom);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        currentPath.add(intFrom);
        for (String si : sis) {
            if (!si.equals(intTo)) {
                getPaths2(paths, currentPath, si, intTo);
                currentPath.remove(currentPath.size() - 1);
            } else {
                paths.add(new ArrayList<String>(currentPath));
            }
        }
    }

    private void getPaths(List<ArrayList<String>> paths, List<String> currentPath, String intFrom, String intTo) {
        getPaths2(paths, currentPath, intFrom, intTo);
        // remove invisible elements
        for (ArrayList<String> path : paths) {
            for (Iterator<String> it2 = path.iterator(); it2.hasNext();) {
                String cl = it2.next();
                try {
                    if (!classHierarchy.isAccessible(cl)) {
                        it2.remove();
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        // sort - shorten paths first
        Collections.sort(paths, new Comparator() {
            public int compare(Object o1, Object o2) {
                Integer s1 = new Integer(((ArrayList) o1).size());
                Integer s2 = new Integer(((ArrayList) o2).size());
                return s1.compareTo(s2);
            }
        });
    }

    private String findVisibleReplacement(String clName, List supers, String replaceWithClassName, boolean findToSuper) {

        // if this member is from interface...
        try {
            if (classHierarchy.isInterface(clName)) {
                ArrayList<ArrayList<String>> paths = new ArrayList<ArrayList<String>>();
                ArrayList<String> currentPath = new ArrayList<String>();
                getPaths(paths, currentPath, replaceWithClassName, clName);
                if (paths.size() > 0) {
                    ArrayList<String> shorterPath = paths.get(0);
                    if (shorterPath.size() > 0) {
                        return shorterPath.get(shorterPath.size() - 1);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // supers sorted from analyzed class to superclass
        if (supers.size() > 0) {
            // used for members - finds nearest visible subclass
            if (!findToSuper) {
                int i = supers.indexOf(clName);
                if (i <= 0) {
                    return replaceWithClassName;
                }

                for (int pos = i - 1; pos >= 0; pos--) {
                    String name = (String) supers.get(pos);
                    if (!isInvisibleClass(name)) {
                        return name;
                    }
                }

            } else {
                // used for exception - finds nearest visible superclass
                for (Object aSuper : supers) {
                    String name = (String) aSuper;
                    if (!isInvisibleClass(name)) {
                        return name;
                    }
                }
            }
        }
        return replaceWithClassName;
    }

    /**
     * 1) replaces invisible return-types 2) fixes invisible parameter types 3)
     * fixes invisible attribute types
     */
    private void fixMethods(ClassDescription cl) throws ClassNotFoundException {

        for (Iterator e = cl.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isMethod() || mr.isField()) {
                fixType(cl, mr);
            }
            if (mr.isConstructor() || mr.isMethod()) {
                checkMethodParameters(cl, mr);
            }
        }
    }

    private void fixType(ClassDescription cl, MemberDescription mr) throws ClassNotFoundException {
        String returnType = mr.getType();
        if (!MemberDescription.NO_TYPE.equals(returnType) && isInvisibleClass(returnType)) {

            String cleanReturnType = stripTypesAndArrays(returnType);

            List supers = Collections.EMPTY_LIST;

            // is it interface or class ? If invisible interface found replace it with java.lang.Object!!!
            if (!classHierarchy.isInterface(cleanReturnType)) {
                supers = classHierarchy.getSuperClasses(cleanReturnType);
            }

            String newName = findVisibleReplacementAndCheckInterfaces(cleanReturnType, supers, "java.lang.Object");

            if (newName != null) {

                newName = wrapTypesAndArrays(returnType, newName);

                mr.setType(newName);

//                if (verboseCorrector) {
                if (!mr.isField()) {
                    String[] invargs = {cl.getName(), mr.getName(), returnType, newName};
                    log.storeWarning(i18n.getString("ClassCorrector.message.returntype.changed", invargs), null);
                } else {
                    String[] invargs = {cl.getName(), mr.getName(), returnType, newName};
                    log.storeWarning(i18n.getString("ClassCorrector.message.fieldtype.changed", invargs), null);
                }
//                }
            } else {
                if (!mr.isField()) {
                    String[] invargs = {returnType, mr.toString()};
                    log.storeError(i18n.getString("ClassCorrector.error.returntype.hidden", invargs), null);
                } else {
                    String[] invargs = {returnType, mr.toString()};
                    log.storeError(i18n.getString("ClassCorrector.error.fieldtype.hidden", invargs), null);
                }

            }
        }
        checkType(cl, mr);
    }

    private void checkMethodParameters(ClassDescription cl, MemberDescription mr) {
        String args = mr.getArgs();
        if (MemberDescription.NO_ARGS.equals(args)) {
            return;
        }

        checkActualParameters(cl, mr, args);
    }

    private void checkType(ClassDescription cl, MemberDescription mr) {
        String type = mr.getType();

        int pos = type.indexOf('<');
        if (pos != -1) {
            checkActualParameters(cl, mr, type.substring(pos));
        }
    }

    private void checkActualParameters(ClassDescription cl, MemberDescription mr, String actualParameters) {
        StringTokenizer tz = new StringTokenizer(actualParameters, ",<>[]&", false);

        boolean firstParameter = true;

        while (tz.hasMoreTokens()) {
            String param = tz.nextToken().trim();

            if (param.length() > 0) {

                String prefix = "? super ";
                if (param.indexOf(prefix) == 0) {
                    param = param.substring(prefix.length());
                }

                prefix = "? extends ";
                if (param.indexOf(prefix) == 0) {
                    param = param.substring(prefix.length());
                }

                if (isInvisibleClass(param)) {

                    // let's ignore first synthetic parameter in nested class' constructor
                    // -allpublic option allows tracking classes like the following:
                    // class A {
                    //     public class B {}  // this class
                    // }
                    //
                    boolean isInner = cl.getQualifiedName().indexOf('$') >= 0;
                    if (mr.isConstructor() && isInner && !cl.hasModifier(Modifier.STATIC) && firstParameter) {
                        // it's ok. well, it's almost ok :-)
                        firstParameter = false;
                        continue;
                    }

                    String[] invargs = {param, mr.toString(), cl.getQualifiedName()};
                    log.storeError(i18n.getString("ClassCorrector.error.parametertype.hidden", invargs), null);
                }
            }

        }
    }

    /**
     * This method changes "declared class" for merged visible class members,
     * which are declared in invisible superclasses.
     */
    private void replaceInvisibleInMembers(ClassDescription c) throws ClassNotFoundException {

        String className = c.getQualifiedName();

        List supers = classHierarchy.getSuperClasses(c.getQualifiedName());

        ArrayList<MemberDescription> newMembers = new ArrayList<MemberDescription>();

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {

            MemberDescription mr = (MemberDescription) e.next();

            // process methods, constructors and fields only
            if (mr.isSuperClass() || mr.isSuperInterface()) {
                continue;
            }

            if (isInvisibleClass(mr.getDeclaringClassName())) {

                String newPar = findVisibleReplacement(mr.getDeclaringClassName(), supers, className, false);
                MemberDescription newMember = (MemberDescription) mr.clone();
                newMember.setDeclaringClass(newPar);

                e.remove();

                // check for existing the same. For example:
                // public interface I extends hidden { void foo(); }
                // interface hidden { void foo(); }
                if (!c.containsMember(newMember)) {

                    newMembers.add(newMember);

                } else {
                }
            }
        }

        for (MemberDescription newMember : newMembers) {
            c.add(newMember);
        }
    }

    private void removeInvisibleInterfaces(ClassDescription c) throws ClassNotFoundException {

        List<String> makeThemDirect = null;

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isSuperInterface()) {

                SuperInterface si = (SuperInterface) mr;
                String siName = si.getQualifiedName();

                if (isInvisibleClass(siName)) {
                    e.remove();

                    if (si.isDirect()) {

                        if (makeThemDirect == null) {
                            makeThemDirect = new ArrayList<String>();
                        }

                        String[] intfs = classHierarchy.getSuperInterfaces(siName);

                        makeThemDirect.addAll(Arrays.asList(intfs));
                    }
                }

                if (mr.getTypeParameters() != null) {
                    checkActualParameters(c, mr, mr.getTypeParameters());
                }
            }
        }

        if (makeThemDirect != null) {

            for (Iterator it = c.getMembersIterator(); it.hasNext();) {
                MemberDescription mr = (MemberDescription) it.next();
                if (mr.isSuperInterface() && makeThemDirect.contains(mr.getQualifiedName())) {
                    // NOTE: clone not required here, because MemberCollectionBuilder clone
                    // all non-direct superinterfaces!
                    ((SuperInterface) mr).setDirect(true);
                }
            }
        }
    }

    private void fixInvisibleSuperclasses(ClassDescription c) throws ClassNotFoundException {

        SuperInterface[] intfs = null;
        MemberDescription newMember = null;

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isSuperClass()) {
                if (isInvisibleClass(mr.getQualifiedName())) {

                    ClassDescription cS = classHierarchy.load(mr.getQualifiedName());

                    List supers = classHierarchy.getSuperClasses(cS.getQualifiedName());
                    String newName = findVisibleReplacement(mr.getQualifiedName(), supers, "java.lang.Object", true);
                    newMember = (MemberDescription) mr.clone();
                    newMember.setupClassName(newName);

                    e.remove();

                    intfs = cS.getInterfaces();
                }

                if (mr.getTypeParameters() != null) {
                    checkActualParameters(c, mr, mr.getTypeParameters());
                }

                break;  // only one superclass may exist!
            }
        }

        if (newMember != null) {
            c.add(newMember);
        }

        if (intfs != null) {

            for (SuperInterface intf : intfs) {
                SuperInterface m = (SuperInterface) c.findMember(intf);
                if (m != null) {
                    m.setDirect(true);
                    m.setDeclaringClass(c.getQualifiedName());
                }
            }
        }
    }

    protected void removeSuperInterfaces(Set interfaces) throws ClassNotFoundException {

        List intfs = new ArrayList(interfaces);
        List su = new ArrayList();

        for (int i = 0; i < intfs.size(); i++) {
            String intfName = (String) intfs.get(i);

            if (intfName == null || isInvisibleClass(intfName)) {
                continue;
            }

            su.clear();

            su.addAll(classHierarchy.getAllImplementedInterfaces(intfName));

            for (Object o : su) {

                String sui = (String) o;

                if (sui.equals(intfName)) {
                    continue;
                }

                int pos;
                while ((pos = intfs.indexOf(sui)) >= 0) {
                    intfs.set(pos, null);
                }
            }
        }

        interfaces.clear();
        // remove nulls
        for (Object intf : intfs) {
            if (intf != null && !interfaces.contains(intf)) {
                interfaces.add(intf);
            }
        }
    }


    /*
     * After removing invisible interfaces we can have duplicated constants
     * public class P implements I1, I2 {}
     * interface I1 { int I = 0; }
     * interface I2 { int I = 1; }
     * in this case we must remove constants I from resulted set because
     * reference by simple name is impossible due to ambiguity,
     * and reference by qualified name is impossible also
     * due to I1 and I2 are invisible outside the package
     */
    private void removeDuplicatedConstants(ClassDescription c) {

        Set<String> constantNames = new HashSet<String>();

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isField() && mr.isPublic()) {
                if (((FieldDescr) mr).isConstant()) {
                    String constName = mr.getQualifiedName();
                    if (c.getMembersCount(MemberType.FIELD, constName) > 1) {
                        constantNames.add(constName);
                    }
                }
            }
        }

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isField()) {
                if (((FieldDescr) mr).isConstant() && constantNames.contains(mr.getQualifiedName())) {
                    e.remove();
                }
            }
        }
    }

    private void checkClassTypeParameters(ClassDescription cl) {
        checkTypeParameters(cl, cl);
        for (Iterator e = cl.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isMethod() || mr.isConstructor()) {
                checkTypeParameters(cl, mr);
            }
        }
    }

    private void checkTypeParameters(ClassDescription cl, MemberDescription mr) {

        final String ext = "extends";
        String typeparams = mr.getTypeParameters();

        if (typeparams != null) {
            ArrayList<String> params = Erasurator.splitParameters(typeparams);
            for (String param : params) {

                String temp = param.substring(param.indexOf(ext) + ext.length());
                StringTokenizer st = new StringTokenizer(temp, "&");

                while (st.hasMoreTokens()) {
                    String className = st.nextToken().trim();
                    int pos = className.indexOf('<');
                    if (pos != -1) {
                        checkActualParameters(cl, mr, className.substring(pos));
                    }
                    if (isInvisibleClass(className) && !className.equals(mr.getDeclaringClassName())) {
                        if (mr.isMethod() || mr.isConstructor()) {
                            String[] invargs = {className, mr.toString(), mr.getDeclaringClassName()};
                            log.storeError(i18n.getString("ClassCorrector.error.parametertype.hidden", invargs), null);
                        } else {
                            String[] invargs = {className, mr.getQualifiedName()};
                            log.storeError(i18n.getString("ClassCorrector.error.parametertype.hidden2", invargs), null);
                        }
                    }
                }
            }
        }
    }

    private static String wrapTypesAndArrays(String oldT, String newT) {
        int pos = minPos(oldT.indexOf('['), oldT.indexOf('<'));
        if (pos != -1) {
            return newT + oldT.substring(pos);
        }
        return newT;
    }

    private static String stripTypesAndArrays(String name) {
        int pos = minPos(name.indexOf('['), name.indexOf('<'));
        if (pos != -1) {
            return name.substring(0, pos);
        }
        return name;
    }

    // returns minimal not negative int
    private static int minPos(int i, int j) {
        if (i < 0 && j < 0) {
            return -1;
        }
        if (i < 0 && j >= 0) {
            return j;
        }
        if (i >= 0 && j < 0) {
            return i;
        } else {
            return Math.min(i, j);
        }
    }

    private boolean isPublicInner(String clName) throws ClassNotFoundException {
        if (clName.indexOf('$') < 0) {
            return false;
        }

        ClassDescription cd = classHierarchy.load(clName);
        return cd.isPublic() || cd.isProtected();
    }

    private boolean isInvisibleClass(String fqname) {

        if (fqname.length() == 0) // constructors' return type
        {
            return false;
        }

        // Is this a type parameter ?
        if (fqname.startsWith("{")) {
            return false;
        }

        if (fqname.startsWith("?")) {
            return false;
        }

        String pname = ClassCorrector.stripTypesAndArrays(fqname);

        if (PrimitiveTypes.isPrimitive(pname)) {
            return false;
        }

        boolean accessible = true;

        try {
            accessible = classHierarchy.isAccessible(pname);
        } catch (ClassNotFoundException e) {
            log.storeError(i18n.getString("ClassCorrector.error.missingclass", new String[]{pname}), null);
        }

        return !accessible;
    }

    private void removeInvisibleAnnotations(ClassDescription cl) throws ClassNotFoundException {

        int count = 0;
        AnnotationItem[] annotations = cl.getAnnoList();

        int len = annotations.length;

        if (len == 0) {
            return;
        }

        for (int i = 0; i < len; ++i) {
            String annoName = annotations[i].getName();

            boolean documented = classHierarchy.isDocumentedAnnotation(annoName);

            if (isInvisibleClass(annoName)) {
                if (documented) {
                    System.out.println(i18n.getString("ClassCorrector.error.invisible_documented_annotation", annoName));
                }
                annotations[i] = null;
            } else {
                ++count;
            }
        }

        if (count == len) {
            return;   // nothing to do
        }
        AnnotationItem[] visibleAnnotations = AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY;

        if (count != 0) {
            visibleAnnotations = new AnnotationItem[count];
            count = 0;
            for (AnnotationItem annotation : annotations) {
                if (annotation != null) {
                    visibleAnnotations[count++] = annotation;
                }
            }
        }

        cl.setAnnoList(visibleAnnotations);
    }
}
