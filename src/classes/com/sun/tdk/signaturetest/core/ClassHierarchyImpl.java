/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.classpath.Classpath;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.plugin.Filter;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Roman Makarchuk
 * @author Mikhail Ershov
 */
public class ClassHierarchyImpl implements ClassHierarchy {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private final ClassDescriptionLoader loader;
    /**
     * If the {@code trackMode} field equals to {@code ALL_PUBLIC},
     * every {@code public} or {@code protected} class is considered
     * to be accessible. Otherwise, ordinal accessibility rules are applied.
     * These rules imply, that {@code public} or {@code protected}
     * nested class may become inaccessible because of stronger accessibility
     * limitations assigned to its declaring class, or to class declaring its
     * declaring class, and so on.
     *
     * @see #trackMode
     */
    private final int trackMode;
    private final Filter defaultFilter = new DefaultIsAccessibleFilter();

    public ClassHierarchyImpl(ClassDescriptionLoader loader) {
        this.loader = loader;
        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        if (bo.isSet(Option.ALL_PUBLIC)) {
            trackMode = ALL_PUBLIC;
        } else {
            trackMode = 0;
        }
    }

    public ClassHierarchyImpl(ClassDescriptionLoader loader, int trackMode) {
        this.loader = loader;
        this.trackMode = trackMode;
    }

    public String getSuperClass(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.superClass;
    }

    public List<String> getSuperClasses(String fqClassName) throws ClassNotFoundException {
        List<String> superclasses = new ArrayList<>();
        findSuperclasses(fqClassName, superclasses);
        return superclasses;
    }

    public String[] getSuperInterfaces(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.superInterfaces;
    }

    public String[] getPermittedSubClasses(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.permittedSubClasses;
    }

    public Set<String> getAllImplementedInterfaces(String fqClassName) throws ClassNotFoundException {
        Set<String> intfs = new HashSet<>();
        findAllImplementedInterfaces(fqClassName, intfs);
        return intfs;
    }

    private void findSuperclasses(String fqname, List<String> supers) throws ClassNotFoundException {
        if (fqname.startsWith("java") ||  // DO NOT MERGE this hack
            fqname.startsWith("javax")) { // DO NOT MERGE this hack
            return;                       // DO NOT MERGE this hack
            }

        ClassInfo info = getClassInfo(fqname);
        String supr = info.superClass;
        if (supr != null) {
            supers.add(supr);
            findSuperclasses(supr, supers);
        }
    }

    private void findAllImplementedInterfaces(String fqname, Set<String> implementedInterfaces) throws ClassNotFoundException {

        List<String> superClasses = new ArrayList<>();

        ClassInfo info = getClassInfo(fqname);

        String[] intfs = info.superInterfaces;
        for (String intf : intfs) {
            implementedInterfaces.add(intf);
            superClasses.add(intf);
        }

        findSuperclasses(fqname, superClasses);

        for (String superClass : superClasses) {
            findSuperInterfaces(superClass, implementedInterfaces);
        }
    }

    private void findSuperInterfaces(String fqname, Set<String> supers) throws ClassNotFoundException {

        ClassInfo info = getClassInfo(fqname);

        String[] intf = info.superInterfaces;
        for (String s : intf) {
            supers.add(s);
            findSuperInterfaces(s, supers);
        }
    }

    public String[] getDirectSubclasses(String fqClassName) {

        String[] result = EMPTY_STRING_ARRAY;

        List<String> subClasses = directSubClasses.get(fqClassName);
        if (subClasses != null) {
            result = subClasses.toArray(EMPTY_STRING_ARRAY);
        }

        return result;
    }

    public String[] getAllSubclasses(String fqClassName) {
        throw new UnsupportedOperationException("This method is not implemented");
    }

    public String[] getNestedClasses(String fqClassName) {
        throw new UnsupportedOperationException("This method is not implemented");
    }

    public boolean isSubclass(String subClassName, String superClassName) throws ClassNotFoundException {

        assert subClassName != null && superClassName != null;

        if (subClassName.charAt(0) == '{' || superClassName.charAt(0) == '{') {
            return false;
        }

        String name = subClassName;
        do {
            try {
                ClassInfo info = getClassInfo(name);
                if (superClassName.equals(info.superClass)) {
                    return true;
                }
                name = info.superClass;
            } catch (ClassNotFoundException cnfe) {
                if (bo.isSet(Option.DEBUG)) {
                    SwissKnife.reportThrowable(cnfe);
                }
                return false;
            }

        } while (name != null);

        return false;
    }

    public ClassDescription load(String name) throws ClassNotFoundException {
        return load(name, false);
    }

    public boolean isMethodOverriden(MethodDescr md) throws ClassNotFoundException {
        Erasurator erasurator = new Erasurator();
        MethodOverridingChecker moc = new MethodOverridingChecker(erasurator);
        for (String sup : getSuperClasses(md.getDeclaringClassName())) {
            ClassDescription sc = load(sup);
            erasurator.erasure(sc);
            moc.addMethods(sc.getDeclaredMethods());
        }
        return moc.getOverridingMethod(md, false) != null;
    }

    public boolean isMethodImplements(MethodDescr md) throws ClassNotFoundException {
        Erasurator erasurator = new Erasurator();
        MethodOverridingChecker moc = new MethodOverridingChecker(erasurator);
        String name = md.getName();
        for (String inf : getAllImplementedInterfaces(md.getDeclaringClassName())) {
            ClassDescription sc = load(inf);
            erasurator.erasure(sc);
            moc.addMethods(sc.getDeclaredMethods(), name);
        }
        if (moc.getOverridingMethod(md, false) != null) {
            return true;
        }
        return isAnonimouse(md.getDeclaringClassName());
    }

    private final Pattern anonimouse = Pattern.compile("\\$\\d+$");

    private boolean isAnonimouse(String clName) {
        return anonimouse.matcher(clName).find();
    }

    private static final Pattern simpleParamUsage = Pattern.compile("<[^<>]+?>");

    private ClassDescription load(String name, boolean no_cache) throws ClassNotFoundException {

        while (name.indexOf('<') != -1 && name.indexOf('>') != -1) {
            Matcher m;
            m = simpleParamUsage.matcher(name);
            name = m.replaceAll("");
        }

        ClassDescription c;
        try {
            c = loader.load(name);
        } catch (ClassNotFoundException ce) {
            assert AppContext.getContext() != null;
            Classpath cp = AppContext.getContext().getInputClasspath();
            if (cp != null) {
                c = cp.findClassDescription(name);
            } else {
                throw new ClassNotFoundException(name);
            }
        }

        Transformer t = PluginAPI.ON_CLASS_LOAD.getTransformer();
        if (t != null) {
            t.transform(c);
        }

        if (!no_cache) {
            // store class info!
            getClassInfo(name);
        }
        c.setHierarchy(this);
        return c;

    }

    /**
     * Check if the class described by {@code c} is to be traced
     * accordingly to {@code trackMode} set for {@code this} instance.
     * Every {@code public} or {@code protected} class is accessible,
     * if it is not nested to another class having stronger accessibility
     * limitations. However, if {@code trackMode} is set to
     * {@code ALL_PUBLIC} for {@code this} instance, every
     * {@code public} or {@code protected} class is considered to be
     * accessible despite of its accessibility limitations possibly inherited.
     */
    public boolean isAccessible(ClassDescription c) {
        return isAccessible(c, false);
    }

    public boolean isDocumentedAnnotation(String fqname) throws ClassNotFoundException {

        ClassInfo info = processedClasses.get(fqname);
        if (info != null) {
            return info.isDocumentedAnnotation;
        }

        ClassDescription c = load(fqname);
        return c.isDocumentedAnnotation();
    }

    public boolean isContainerAnnotation(String fqname) throws ClassNotFoundException {

        ClassDescription c = load(fqname);

        try {
            if (c.hasModifier(Modifier.ANNOTATION)) {
                // 1. Find the T[] values() method
                MethodDescr[] mds = c.getDeclaredMethods();

                String aType = null;

                for (MethodDescr md : mds) {
                    if (md.getName().equals("value")
                            && MemberDescription.NO_ARGS.equals(c.getArgs())) {
                        aType = md.getType();
                    } else if (!md.hasModifier(Modifier.HASDEFAULT)) {
                        // any methods declared by CONTAINING ANNOTAION
                        // other then value() must have a default value
                        return false;
                    }
                }

                // strip and []
                if (aType == null || !aType.endsWith("[]")) {
                    return false;
                } else {
                    aType = aType.substring(0, aType.length() - 2);
                }

                // 2. Find T
                ClassDescription a = load(aType);
                // 3. Is T annotation type?
                if (!a.hasModifier(Modifier.ANNOTATION)) {
                    return false;
                }

                // 4. Is it @Repeatable type?
                AnnotationItem[] alist = a.getAnnoList();
                for (AnnotationItem annotationItem : alist) {
                    if (annotationItem.getName().equals(AnnotationItem.ANNOTATION_REPEATABLE)) {
                        return true;
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            return false;
        }
        return false;

    }

    public boolean isAccessible(String fqname) throws ClassNotFoundException {

        if (fqname == null) {
            throw new NullPointerException("Parameter fqname can't be null!");
        }

        ClassInfo info = processedClasses.get(fqname);
        if (info != null) {
            return info.accessable;
        }

        ClassDescription c = load(fqname);
        return isAccessible(c, false);
    }

    private boolean isAccessible(ClassDescription c, boolean no_cache) {

        if (!no_cache) {
            ClassInfo info = processedClasses.get(c.getQualifiedName());
            if (info != null) {
                return info.accessable;
            }
        }

        // Anonymous class can't be part of any API!
        if (c.isAnonymousClass()) {
            return false;
        }

        Filter f = PluginAPI.IS_CLASS_ACCESSIBLE.getFilter();
        if (f == null) {
            f = defaultFilter;
        }
        return f.accept(c);
    }

    // returns true if the class is visible outside the package
    public boolean isClassVisibleOutside(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.isVisibleOutside;
    }

    public boolean isClassVisibleOutside(ClassDescription cls) throws ClassNotFoundException {

        boolean visible = cls.hasModifier(Modifier.PUBLIC) || cls.hasModifier(Modifier.PROTECTED);

        if (visible && !cls.isTopClass()) {
            visible = isClassVisibleOutside(cls.getDeclaringClassName());
        }
        return visible;
    }

    public boolean isInterface(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return Modifier.hasModifier(info.modifiers, Modifier.INTERFACE);
    }

    public boolean isAnnotation(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return Modifier.hasModifier(info.modifiers, Modifier.ANNOTATION);
    }

    public int getClassModifiers(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.modifiers;

    }

    private ClassInfo getClassInfo(String fqname) throws ClassNotFoundException {

        ClassInfo info = processedClasses.get(fqname);
        if (info == null) {

            ClassDescription c = load(fqname, true);
            info = new ClassInfo(c, isAccessible(c, true), isClassVisibleOutside(c));

            if (info.superClass != null) {
                addSubClass(info.superClass, fqname);
            }

            if (info.superInterfaces != null && info.superInterfaces.length > 0) {
                for (int i = 0; i < info.superInterfaces.length; i++) {
                    addSubClass(info.superInterfaces[i], fqname);
                }
            }

            processedClasses.put(fqname, info);
        }
        return info;
    }

    private final Map<String, List<String>> directSubClasses = new HashMap<>();

    private void addSubClass(String superClass, String subClass) {

        List<String> subClasses = directSubClasses.get(superClass);

        if (subClasses == null) {
            subClasses = new ArrayList<>(3);
            directSubClasses.put(superClass, subClasses);
        }

        subClasses.add(subClass);
    }

    private static class ClassInfo {

        private static final String[] EMPTY_INTERFACES = new String[0];
        String superClass = null;
        String[] superInterfaces = EMPTY_INTERFACES;
        String[] permittedSubClasses = new String[0];
        boolean accessable = false;
        boolean isDocumentedAnnotation = false;
        int modifiers = 0;
        final boolean isVisibleOutside;

        public ClassInfo(ClassDescription c, boolean accessable, boolean visible) {

            modifiers = c.getModifiers();

            SuperClass sc = c.getSuperClass();
            if (sc != null) {
                superClass = sc.getQualifiedName();
            }

            SuperInterface[] intfs = c.getInterfaces();
            int len = intfs.length;
            if (len > 0) {
                superInterfaces = new String[len];
                for (int i = 0; i < len; ++i) {
                    superInterfaces[i] = intfs[i].getQualifiedName();
                }
            }

            PermittedSubClass[] pClss = c.getPermittedSubclasses();
            len = pClss.length;
            if (len > 0) {
                permittedSubClasses = new String[len];
                for (int i = 0; i < len; ++i) {
                    permittedSubClasses[i] = pClss[i].getQualifiedName();
                }
            }

            this.accessable = accessable;
            this.isVisibleOutside = visible;
            this.isDocumentedAnnotation = c.isDocumentedAnnotation();
        }
    }

    public int getTrackMode() {
        return trackMode;
    }

    private final HashMap<String, ClassInfo> processedClasses = new HashMap<>();
    private final BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);

    class DefaultIsAccessibleFilter implements Filter {

        private boolean isAccessible(ClassDescription c) {

            if (c.isModuleOrPackaheInfo()) {
                return true;
            }

            if (trackMode == ALL_PUBLIC) {
                return c.isPublic() || c.isProtected();
            }

            boolean result = false;

            try {
                result = c.getClassHierarchy().isClassVisibleOutside(c);
            } catch (ClassNotFoundException e) {
                if (bo.isSet(Option.DEBUG)) {
                    SwissKnife.reportThrowable(e);
                }
            }
            return result;
        }

        public boolean accept(ClassDescription cls) {
            return isAccessible(cls);
        }
    }
}
