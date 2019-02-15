/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.*;

/**
 * This class provides methods to findByName an load a class and to compile a
 * <b>ClassDescription</b> for it. The method
 * <b>Class</b>.<code>forName()</code> is used to findByName a
 * <code>Class</code> object. If the advanced method
 * <code>forName</code>(<b>String</b>, <code>boolean</code>,<b>ClassLoader</b>)
 * is unavailable, the rougher method <code>forName</code>(<b>String</b>) is
 * used.
 *
 * @author Maxim Sokolnikov
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
public class MemberCollectionBuilder {

    private ClassCorrector cc;
    private Erasurator erasurator = new Erasurator();
    private Transformer defaultTransformer = new DefaultAfterBuildMembersTransformer();
    private Log log;
    private String builderName; // for debugging
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(MemberCollectionBuilder.class);
    private BuildMode mode = BuildMode.NORMAL;
    private ClassHierarchy secondCH;


    public MemberCollectionBuilder(Log log) {
        this.cc = new ClassCorrector(log);
        this.log = log;
    }

    public MemberCollectionBuilder(Log log, String builderName) {
        this(log);
        this.builderName = builderName;
    }

    /**
     * Generate <code>members</code> field for the given <b>ClassDescription</b>
     * <code>cl</code>. Recursively findByName all inherited fields, methods,
     * nested classes, and interfaces for the class having the name prescribed
     * by <code>cl</code>.
     *
     * @see MemberDescription
     */
    public void createMembers(ClassDescription cl, boolean addInherited, boolean fixClass, boolean checkHidding) throws ClassNotFoundException {

        MemberCollection members = getMembers(cl, addInherited, checkHidding);

        // add super class
        SuperClass spr = cl.getSuperClass();

        if (spr != null) {
            members.addMember(spr);
        }

        //add constructors
        ConstructorDescr[] constr = cl.getDeclaredConstructors();
        for (ConstructorDescr constructorDescr : constr) {
            members.addMember(constructorDescr);
        }

        cl.setMembers(members);

        Transformer t = PluginAPI.AFTER_BUILD_MEMBERS.getTransformer();
        if (t == null) {
            t = defaultTransformer;
        }

        t.transform(cl);

        if (fixClass) {
            t = PluginAPI.CLASS_CORRECTOR.getTransformer();
            if (t == null) {
                t = cc;
            }
            t.transform(cl);
        }

        if (mode == BuildMode.TESTABLE) {
            prepareMembersForAPICheck(cl);
        }

        t = PluginAPI.AFTER_CLASS_CORRECTOR.getTransformer();
        if (t != null) {
            t.transform(cl);
        }
    }

    private void prepareMembersForAPICheck(ClassDescription cl) {
        // for APICheck
        // we have to remove members inherited from superclasses outside the scope
        // (not included to not transitively closed signature file)
        // or superclasses where inherited chain was interrupted
        //
        // example 1: signature file contains only java.util classes
        // that means that all members from java.lang superclasses (including java.lang.Object)
        // must be removed because them are not existed for signature file
        //
        // example 2: signature file contains only java.lang classes
        // consider by java.lang.RuntimePermission hierarchy:
        // java.lang.Object
        //   extended by java.security.Permission
        //       extended by java.security.BasicPermission
        //           extended by java.lang.RuntimePermission
        // Object is in the scope but inheritance chain was interrupted by the classes
        // outside the java.lang package. So in this case RuntimePermission should have no inherited members
        MemberCollection cleaned = new MemberCollection();
        int memcount = 0;
        for (Iterator e = cl.getMembersIterator(); e.hasNext();) {
            memcount++;
            MemberDescription mr = (MemberDescription) e.next();
            MemberType mt = mr.getMemberType();
            if (mt != MemberType.SUPERCLASS) {
                if (!isAccessible(mr.getDeclaringClassName())) {
                    continue;
                }
                if (!mr.getDeclaringClassName().equals(cl.getQualifiedName())) {
                    String cn = cl.getQualifiedName();
                    String dcn = mr.getDeclaringClassName();
                    if (!isAncestor(cn, dcn)) {
                        continue;
                    }
                }
            }
            cleaned.addMember(mr);
        }
        if (cleaned.getAllMembers().size() != memcount) {
            cl.setMembers(cleaned);
        }
    }

    // gently find ancestors.
    // don't use here ClassHierarchy's methods
    // because they are not stateless!
    private boolean isAncestor(String clName, String superClName) {
        try {
            ClassDescription c = secondCH.load(clName);
            SuperClass superCl = c.getSuperClass();
            if (superCl != null && superClName.equals(superCl.getQualifiedName())) {
                return true;
            }
            SuperInterface[] sis = c.getInterfaces();
            for (SuperInterface si1 : sis) {
                if (superClName.equals(si1.getQualifiedName())) {
                    return true;
                }
            }
            if (superCl != null && isAncestor(superCl.getQualifiedName(), superClName)) {
                return true;
            }
            for (SuperInterface si : sis) {
                if (isAncestor(si.getQualifiedName(), superClName)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
        return false;
    }

    /**
     * Collect <b>MemberDescription</b>s for all fields, methods, and nested
     * classes of the given class described by <code>cl</code>. Recursively
     * findByName all inherited members, as far as members declared by the class
     * <code>cl</code>.
     *
     * @see com.sun.tdk.signaturetest.model.MemberDescription
     */
    private MemberCollection getMembers(ClassDescription cl, boolean addInherited, boolean checkHidding) throws ClassNotFoundException {
        return getMembers(cl, null, true, false, addInherited, checkHidding);
    }

    private MemberCollection getMembers(ClassDescription cl, String actualTypeParams,
            boolean skipRawTypes, boolean callErasurator, boolean addInherited, boolean checkHidding) throws ClassNotFoundException {

        assert cl != null;

        // required for correct overriding checking
        erasurator.parseTypeParameters(cl);

        List paramList = null;
        MemberCollection retVal = new MemberCollection();

        // creates declared members
        MemberDescription[] methods = cl.getDeclaredMethods();
        MemberDescription[] fields = cl.getDeclaredFields();
        MemberDescription[] classes = cl.getDeclaredClasses();
        MemberDescription[] intrfs = cl.getInterfaces();

        String clsName = cl.getQualifiedName();
        ClassHierarchy hierarchy = cl.getClassHierarchy();

        if (actualTypeParams != null) {
            paramList = Erasurator.splitParameters(actualTypeParams);
            methods = Erasurator.replaceFormalParameters(clsName, methods, paramList, skipRawTypes);
            fields = Erasurator.replaceFormalParameters(clsName, fields, paramList, skipRawTypes);
            classes = Erasurator.replaceFormalParameters(clsName, classes, paramList, skipRawTypes);
        } else if (callErasurator && cl.getTypeParameters() != null) {
            List boundsList = cl.getTypeBounds();
            methods = Erasurator.replaceFormalParameters(clsName, methods, boundsList, false);
            fields = Erasurator.replaceFormalParameters(clsName, fields, boundsList, false);
            classes = Erasurator.replaceFormalParameters(clsName, classes, boundsList, false);
        }
        if (paramList != null) {
            intrfs = Erasurator.replaceFormalParameters(clsName, intrfs, paramList, skipRawTypes);
        }

        MethodOverridingChecker overridingChecker = new MethodOverridingChecker(erasurator);
        overridingChecker.addMethods(methods);
        retVal = addSuperMembers(methods, retVal);
        retVal = addSuperMembers(fields, retVal);
        retVal = addSuperMembers(classes, retVal);

        for (MemberDescription intrf : intrfs) {
            SuperInterface s = (SuperInterface) intrf;
            s.setDirect(true);
            s.setDeclaringClass(cl.getQualifiedName());
        }
        retVal = addSuperMembers(intrfs, retVal);

        if (addInherited) {
            addInherited(checkHidding, cl, hierarchy, paramList, skipRawTypes,
                    overridingChecker, retVal);
        } else {
            fixAnnotations(cl, hierarchy);
        }

        return retVal;
    }

    private void addInherited(boolean checkHidding, ClassDescription cl, ClassHierarchy hierarchy, List paramList, boolean skipRawTypes, MethodOverridingChecker overridingChecker, MemberCollection retVal) throws ClassNotFoundException {

        String clsName = cl.getQualifiedName();
        Set internalClasses = cl.getInternalClasses();

        Map inheritedFields = new HashMap();
        SuperClass superClassDescr = cl.getSuperClass();
        if (superClassDescr != null) {
            try {
                // creates members inherited from superclass
                ClassDescription superClass = hierarchy.load(superClassDescr.getQualifiedName());
                MemberCollection superMembers = getMembers(superClass, superClassDescr.getTypeParameters(), false, true, true, checkHidding);
                findInheritableAnnotations(cl, superClass);
                //exclude non-accessible members
                superMembers = getAccessibleMembers(superMembers, cl, superClass);
                // process superclass methods
                Collection coll = superMembers.getAllMembers();
                if (paramList != null) {
                    coll = Erasurator.replaceFormalParameters(clsName, coll, paramList, skipRawTypes);
                }
                for (Object o : coll) {
                    MemberDescription supMD = (MemberDescription) o;
                    if (supMD.isMethod()) {
                        if (addInheritedMethod(supMD, overridingChecker, retVal, hierarchy, superClass, cl)) {
                            continue;
                        }
                    } else if (supMD.isField()) {
                        // store fields in temporary collection
                        supMD.unmark();
                        inheritedFields.put(supMD.getName(), supMD);
                    } else if (supMD.isSuperInterface()) {
                        SuperInterface si = (SuperInterface) supMD.clone();
                        si.setDirect(false);
                        retVal.addMember(si);
                    } else if (supMD.isInner()) {
                        if (!internalClasses.contains(supMD.getName())) {
                            retVal.addMember(supMD);
                        }
                    } else {
                        retVal.addMember(supMD);
                    }
                }
            } catch (ClassNotFoundException ex) {
                if (mode != BuildMode.SIGFILE) {
                    throw ex;
                }
            }
        }
        addInheritedFromInterfaces(cl, hierarchy, checkHidding, paramList,
                skipRawTypes, overridingChecker,
                retVal, inheritedFields, internalClasses);
    }

    // this method invokes recursively
    // addInheritedFromInterfaces - getMembers - addInherited - addInheritedFromInterfaces - ...
    private void addInheritedFromInterfaces(ClassDescription cl,
            ClassHierarchy hierarchy, boolean checkHidding,
            List paramList, boolean skipRawTypes,
            MethodOverridingChecker overridingChecker,
            MemberCollection retVal, Map inheritedFields,
            Set internalClasses) throws ClassNotFoundException {

        String clsName = cl.getQualifiedName();
        // findMember direct interfaces
        SuperInterface[] interfaces = cl.getInterfaces();

        HashSet xfCan = new HashSet();
        for (SuperInterface anInterface : interfaces) {
            try {
                ClassDescription intf = hierarchy.load(anInterface.getQualifiedName());
                MemberCollection h = getMembers(intf, anInterface.getTypeParameters(), false, true, true, checkHidding);
                //MemberCollection h = getMembers(intf, interfaces[i].getTypeParameters(), false, true, false, checkHidding);
                Collection coll = h.getAllMembers();
                if (paramList != null) {
                    coll = Erasurator.replaceFormalParameters(clsName, coll, paramList, skipRawTypes);
                }
                nextMemberToAdd:
                for (Object o : coll) {
                    // for each direct interface member do
                    MemberDescription membToAdd = (MemberDescription) o;
                    if (membToAdd.isMethod()) {
                        MethodDescr m = (MethodDescr) membToAdd;

                        if (m.isStatic()) {
                            // static interface methods are not inheriting (java 8)
                            continue nextMemberToAdd;
                        }

                        if (!m.isAbstract() && m.isPrivate()) {
                            // skip non-abstract private methods (java 8)
                            continue nextMemberToAdd;
                        }

                        MethodDescr overriden = overridingChecker.getOverridingMethod(m, true);
                        MemberDescription erased = erasurator.processMember(membToAdd);

                        if (overriden != null) {
                            if (mode == BuildMode.TESTABLE) {
                                // here "overriden" can be corrected back to null
                                overriden = apiCheckCorrectIntMethod(overriden, hierarchy, retVal, membToAdd);
                            }

                            // special case - interfaces java 8
                            // does not work
                            String membClass = membToAdd.getDeclaringClassName();
                            String overClass = overriden.getDeclaringClassName();
                            int membMods = membToAdd.getModifiers();
                            int overMods = overriden.getModifiers();

                            if (cl.isInterface() && !membClass.equals(overClass) && membMods != overMods) {
                                try {
                                    if (hierarchy.getAllImplementedInterfaces(membClass).contains(overClass)) {
                                        if (retVal.contains(overriden)) {
                                            retVal.changeMember(overriden, membToAdd);
                                            retVal.updateMember(membToAdd);
                                        } else {
                                            retVal.updateMember(membToAdd);
                                        }
                                        continue nextMemberToAdd;  // skip to next member
                                    }
                                } catch (ClassNotFoundException ex) {
                                    if (mode != BuildMode.SIGFILE) {
                                        log.storeWarning(i18n.getString("MemberCollectionBuilder.warn.type.notresolved", ex.getCause()), null);
                                    }
                                }
                            }
                        }

                        if (overriden == null) {
                            retVal.addMember(m);
                        } else if (!PrimitiveTypes.isPrimitive(m.getType()) && !m.getType().endsWith("]")) {
                            try {
                                // more specific return type?
                                String existReturnType = overriden.getType();
                                String newReturnType = erased.getType();
                                if (!existReturnType.equals(newReturnType) && (cl.getClassHierarchy().getSuperClasses(newReturnType).contains(existReturnType) || cl.getClassHierarchy().getAllImplementedInterfaces(newReturnType).contains(existReturnType))) {
                                    retVal.updateMember(membToAdd);
                                }
                            } catch (ClassNotFoundException e) {
                                log.storeWarning(i18n.getString("MemberCollectionBuilder.warn.returntype.notresolved", m.getType()), null);
                            }
                        }
                    } else if (membToAdd.isField()) {
                        MemberDescription storedFid = (MemberDescription) inheritedFields.get(membToAdd.getName());
                        if (storedFid != null) {
                            // the same constant can processed several times (e.g. if the same interface is extended/implemented twice)
                            if (!storedFid.getQualifiedName().equals(membToAdd.getQualifiedName())) {
                                storedFid.mark();
                                if (!hierarchy.isClassVisibleOutside(membToAdd.getDeclaringClassName())) {
                                    xfCan.add(membToAdd.getName());
                                }
                            }
                        } else {
                            membToAdd.unmark();
                            inheritedFields.put(membToAdd.getName(), membToAdd);
                        }
                    } else if (membToAdd.isSuperInterface()) {
                        SuperInterface si = (SuperInterface) membToAdd.clone();
                        si.setDirect(false);
                        retVal.addMember(si);

                    } else if (membToAdd.isInner()) {

                        if (!internalClasses.contains(membToAdd.getName()) && retVal.findSimilar(membToAdd) == null) {
                            retVal.addMember(membToAdd);
                        } else {
                            if (!hierarchy.isClassVisibleOutside(membToAdd.getDeclaringClassName())) {
                                cl.addXClasses(membToAdd.getName());
                            }
                        }

                    } else {
                        retVal.addMember(membToAdd);
                    }
                }
            } catch (ClassNotFoundException ex) {
                if (mode != BuildMode.SIGFILE) {
                    throw ex;
                }
            }
        }
        postProcessInterfaceFields(cl, checkHidding, retVal, inheritedFields, xfCan);

    }

    private void postProcessInterfaceFields(ClassDescription cl, boolean checkHidding, MemberCollection retVal, Map inheritedFields, HashSet xfCan) {
        Set internalFields = Collections.EMPTY_SET;
        Set xFields = Collections.EMPTY_SET;
        if (checkHidding) {
            internalFields = cl.getInternalFields();
            xFields = cl.getXFields();
        }
        // add inherited fields that have no conflicts with each other
        for (Object o : inheritedFields.values()) {
            MemberDescription field = (MemberDescription) o;
            String fiName = field.getName();
            if (!field.isMarked() && !internalFields.contains(fiName) && !xFields.contains(fiName)) {
                retVal.addMember(field);
            } else {
                if (xfCan.contains(field.getName())) {
                    // this code must be in ClassCorrector - here is wrong place !
                    System.err.println("Phantom field found " + field.getQualifiedName());
                    cl.addXFields(fiName);
                }
            }
        }
        if (!cl.getXClasses().isEmpty()) {
            for (Object o : cl.getXClasses()) {
                String xClass = (String) o;
                Iterator rvi = retVal.iterator();
                while (rvi.hasNext()) {
                    MemberDescription rm = (MemberDescription) rvi.next();
                    if (rm.isInner() && rm.getName().equals(xClass)) {
                        System.err.println("Phantom class found " + rm.getQualifiedName());
                        rvi.remove();
                    }
                }
            }
        }
    }

    private boolean addInheritedMethod(MemberDescription supMD,
            MethodOverridingChecker overridingChecker,
            MemberCollection retVal,
            ClassHierarchy hierarchy,
            ClassDescription superClass,
            ClassDescription cl) {

        MethodDescr m = (MethodDescr) supMD;
        MethodDescr overriden = overridingChecker.getOverridingMethod(m, true);
        MemberDescription erased = erasurator.processMember(supMD);

        if (overriden == null) {
            retVal.addMember(m);
            return false;
        }

        if (!PrimitiveTypes.isPrimitive(m.getType()) && !m.getType().endsWith("]")) {
            try {
                if (!hierarchy.isAccessible(superClass)) {
                    return true;
                }
                String existReturnType = overriden.getType();
                String newReturnType = erased.getType();

                if (!existReturnType.equals(newReturnType) && (cl.getClassHierarchy().getSuperClasses(newReturnType).contains(existReturnType) || cl.getClassHierarchy().getAllImplementedInterfaces(newReturnType).contains(existReturnType))) {
                    retVal.updateMember(supMD);
                }
            } catch (ClassNotFoundException e) {
                if (mode != BuildMode.SIGFILE) {
                    log.storeWarning(i18n.getString("MemberCollectionBuilder.warn.returntype.notresolved", m.getType()), null);
                }
            }
        }
        return false;
    }

    private void fixAnnotations(ClassDescription cl, ClassHierarchy hierarchy) throws ClassNotFoundException {
        // see UseAnnotClss025 test. ClassCorrector should also move annotations
        // from invisible superclass
        SuperClass superClassDescr = cl.getSuperClass();
        if (superClassDescr != null) {
            try {
                ClassDescription superClass = hierarchy.load(superClassDescr.getQualifiedName());
                findInheritableAnnotations(cl, superClass);
            } catch (ClassNotFoundException ex) {
                if (mode != BuildMode.SIGFILE) {
                    throw ex;
                }
            }
        }
    }

    private MemberCollection addSuperMembers(MemberDescription[] from,
            MemberCollection to) {
        for (MemberDescription memberDescription : from) {
            to.addMember(memberDescription);
        }
        return to;
    }

    private MethodDescr apiCheckCorrectIntMethod(MethodDescr overriden, ClassHierarchy hierarchy, MemberCollection retVal, MemberDescription memb) {
        if (!isAccessible(overriden.getDeclaringClassName())) {
            boolean doFix = false;
            int mods = 0;
            try {
                mods = hierarchy.getClassModifiers(overriden.getDeclaringClassName());
            } catch (ClassNotFoundException ex) {
                doFix = true;
            }
            if (doFix && Modifier.hasModifier(mods, Modifier.PUBLIC) || Modifier.hasModifier(mods, Modifier.PROTECTED)) {
                retVal.changeMember(overriden, memb);
                overriden = null;
            }
        }
        return overriden;
    }

    public static class BuildMode {

        public static final BuildMode NORMAL = new BuildMode("NORMAL");
        public static final BuildMode SIGFILE = new BuildMode("SIGFILE");   // Used by APICover
        public static final BuildMode TESTABLE = new BuildMode("TESTABLE"); // Used by APICheck
        private String name;

        private BuildMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private boolean isAccessible(String qualifiedName) {
        try {
            secondCH.load(qualifiedName);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Filter those <b>MemberDescription</b> instances found inside the given
     * <code>members</code> collection available for use by the given
     * <code>subclass</code>, provided they are members of the given
     * <code>superClass</code>.
     *
     * @see MemberDescription
     */
    private MemberCollection getAccessibleMembers(MemberCollection members,
            ClassDescription subclass,
            ClassDescription superClass) {

        String pkg = subclass.getPackageName();
        boolean isSamePackage = pkg.equals(superClass.getPackageName());
        MemberCollection retVal = new MemberCollection();

        for (Iterator e = members.iterator(); e.hasNext();) {
            MemberDescription mbr = (MemberDescription) e.next();
            if ((mbr.isPublic() || mbr.isProtected() || isSamePackage || mbr.isSuperInterface()) && !mbr.isPrivate()) {
                retVal.addMember(mbr);
            }
        }

        return retVal;
    }

    //  Find all inheritable annotations
    private void findInheritableAnnotations(ClassDescription subclass, ClassDescription superClass) {

        AnnotationItem[] superClassAnnoList = superClass.getAnnoList();

        if (superClassAnnoList.length != 0) {

            Set tmp = new TreeSet();

            AnnotationItem[] subClassAnnoList = subclass.getAnnoList();

            for (AnnotationItem annotationItem1 : superClassAnnoList) {
                if (annotationItem1.isInheritable()) {
                    tmp.add(annotationItem1);
                }
            }

            tmp.addAll(Arrays.asList(subClassAnnoList));

            if (tmp.size() != subClassAnnoList.length) {
                AnnotationItem[] newAnnoList = new AnnotationItem[tmp.size()];
                tmp.toArray(newAnnoList);
                subclass.setAnnoList(newAnnoList);
            }
        }
    }

    public void setBuildMode(BuildMode bm) {
        mode = bm;
    }

    public void setSecondClassHierarchy(ClassHierarchy signatureClassesHierarchy) {
        secondCH = signatureClassesHierarchy;
    }

    class DefaultAfterBuildMembersTransformer implements Transformer {

        public ClassDescription transform(ClassDescription cls) {

            for (Iterator it = cls.getMembersIterator(); it.hasNext();) {
                MemberDescription mr = (MemberDescription) it.next();

                boolean isSynthetic = mr.hasModifier(Modifier.ACC_SYNTHETIC);

                // skip synthetic methods and constructors
                if (isSynthetic) {
                    it.remove();
                    continue;
                }

                // includes only public and protected constructors, methods, classes, fields
                if (!(mr.isPublic() || mr.isProtected() || mr.isSuperInterface() || mr.isSuperClass())) {
                    it.remove();
                }
            }
            return cls;
        }
    }
}

/**
 * @author Maxim Sokolnikov
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 * @version 05/03/22
 */
class MethodOverridingChecker {

    private Map /*<String, MethodDescr>*/ methodSignatures = new HashMap();
    private Erasurator erasurator;

    public MethodOverridingChecker(Erasurator er) {
        erasurator = er;
    }

    public void addMethod(MethodDescr m) {
        MethodDescr cloned_m = (MethodDescr) erasurator.processMember(m);
        assert !(cloned_m.getSignature().contains("%")) : "wrong member after erasure: " + m;
        methodSignatures.put(cloned_m.getSignature(), cloned_m);
    }

    public MethodDescr getOverridingMethod(MethodDescr m, boolean autoAdd) {
        MethodDescr cloned_m = (MethodDescr) erasurator.processMember(m);
        String signature = cloned_m.getSignature();
        MethodDescr isOverriding = (MethodDescr) methodSignatures.get(signature);
        if (isOverriding == null && autoAdd) {
            methodSignatures.put(signature, cloned_m);
        }
        return isOverriding;
    }

    public void addMethods(MemberDescription[] methods, String name) {
        for (MemberDescription method : methods) {
            MethodDescr md = (MethodDescr) method;
            if (name.equals(md.getName())) {
                addMethod(md);
            }
        }
    }

    public void addMethods(MemberDescription[] methods) {
        for (MemberDescription method : methods) {
            MethodDescr md = (MethodDescr) method;
            addMethod(md);
        }
    }
}
