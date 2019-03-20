/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.loaders;

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.core.PrimitiveTypes;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.Modifier;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

/**
 * This is J2SE 1.5 (Tiger) loader
 */
public class TigerRefgClassDescrLoader implements ClassDescriptionLoader, LoadingHints {

    private static final String object = "java.lang.Object";
    public static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(TigerRefgClassDescrLoader.class);
    private final ClassLoader ldr;
    private final BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);

    public TigerRefgClassDescrLoader() {
        this(null);
    }

    public TigerRefgClassDescrLoader(ClassLoader l) {
        if (l == null) {
            ldr = getClass().getClassLoader();
        } else {
            ldr = l;
        }
    }

    public ClassDescription load(String name) throws ClassNotFoundException {
        final ClassDescription cd = new ClassDescription();
        final String name2 = ExoticCharTools.decodeExotic(name);
        load2(cd, name2);
        return cd;
    }

    // final because of security reasons
    private void load2(final ClassDescription cd, final String name2) throws ClassNotFoundException, GenericSignatureFormatError {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    readClass(cd, Class.forName(name2, false, ldr));
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            if (pae.getException() instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) pae.getException();
            } else {
                SwissKnife.reportThrowable(pae);
                throw new ClassNotFoundException(name2, pae);
            }
        } // GenericSignatureFormatError is not wrapped with PrivilegedActionException
        catch (GenericSignatureFormatError e) {
            SwissKnife.reportThrowable(e);
            throw e;
        }
    }

    private void readClass(ClassDescription c, Class classObject) {

        //  Setup class name and access modifiers
        int x = classObject.getModifiers();
        c.setModifiers(x);

        Class dclObject = classObject.getDeclaringClass();
        c.setupClassName(classObject.getName(),
                (dclObject == null) ? MemberDescription.NO_DECLARING_CLASS : dclObject.getName());
        c.setTiger(true);

        //  Create type parameters list
        ClassDescription.TypeParameterList enc = null;
        if (dclObject != null) {
            ClassDescription dclCD = new ClassDescription();
            readClass(dclCD, dclObject);
            enc = dclCD.getTypeparamList();
        }

        c.setTypeparamList(new ClassDescription.TypeParameterList(enc));
        c.setTypeParameters(scanFormalTypeParameters(c.getTypeparamList(), classObject.getTypeParameters(), c.getQualifiedName()));

        //  Setup generic superclass
        Type spr = classObject.getGenericSuperclass();
        if (spr != null) {
            SuperClass superClass = new SuperClass();
            superClass.setupGenericClassName(decodeType(c.getTypeparamList(), spr));
            c.setSuperClass(superClass);
        }

        //  Setup generic superinterfaces
        Type[] ifs = classObject.getGenericInterfaces();
        c.createInterfaces(ifs.length);
        for (int i = 0; i < ifs.length; i++) {
            SuperInterface fid = new SuperInterface();
            fid.setupGenericClassName(decodeType(c.getTypeparamList(), ifs[i]));
            c.setInterface(i, fid);
        }

        //  Parse the annotation set present
        c.setAnnoList(AnnotationItem.toArray(parse(c, 0, classObject.getDeclaredAnnotations())));

        //  Process class members
        readFields(c, classObject);
        readCtors(c, classObject);
        readMethods(c, classObject);
        readNested(c, classObject);
    }

    private void readFields(ClassDescription c, Class classObject) {
        Field[] tmp = classObject.getDeclaredFields();
        c.createFields(tmp.length - getSyntheticFieldCount(tmp));

        String fqname = c.getQualifiedName();

        for (int i = 0, j = -1; i < tmp.length; ++i) {
            Field fld = tmp[i];

            // skip synthetic fields
            if (fld.isSynthetic()) {
                if (bo.isSet(Option.DEBUG)) {
                    getLog().println(i18n.getString("TigerRefgClassDescrLoader.message.synthetic_field_skipped", fld));
                }
                continue;
            }

            FieldDescr fid = new FieldDescr(fld.getName(), fqname, fld.getModifiers());
            c.setField(++j, fid);

            try {
                fid.setType(decodeType(c.getTypeparamList(), fld.getGenericType()));
            } catch (GenericSignatureFormatError err) {
                // happends under java 7
                System.err.println("Class format problem, class name = " + c.getQualifiedName());
                throw err;
            }

            //  Get the constant value, if possible
            if (fid.isFinal() && (PrimitiveTypes.isPrimitive(fid.getType()) || "java.lang.String".equals(fid.getType()))) {
                if (!hasHint(LoadingHints.DONT_READ_VALUES)) {
                    try {
                        fld.setAccessible(true);
                        Object v = fld.get(null);
                        fid.setConstantValue(MemberDescription.valueToString(v));
                    } catch (Throwable e) {
                        // catch error or exception that may be thrown during static class initialization
//                            System.err.println("Error during reading field value " + fld.toString());
//                            SwissKnife.reportThrowable(e);
                    }
                }
            }

            //  Parse the annotation set present
            fid.setAnnoList(AnnotationItem.toArray(parse(c, 0, fld.getDeclaredAnnotations())));
        }
    }

    private void readCtors(ClassDescription c, Class classObject) {
        Constructor[] tmp = classObject.getDeclaredConstructors();

        c.createConstructors(tmp.length - getSyntheticConstructorCount(tmp));

        String fqname = c.getQualifiedName();

        for (int i = 0, j = -1; i < tmp.length; i++) {
            Constructor ctor = tmp[i];

            // skip synthetic constructors
            if (ctor.isSynthetic()) {
                if (bo.isSet(Option.DEBUG)) {
                    getLog().println(i18n.getString("TigerRefgClassDescrLoader.message.synthetic_constr_skipped", ctor));
                }
                continue;
            }

            ConstructorDescr fid = new ConstructorDescr(c, ctor.getModifiers());
            c.setConstructor(++j, fid);

            if (ctor.isVarArgs()) {
                fid.addModifier(Modifier.VARARGS);
            }

            fid.setTypeParameters(scanFormalTypeParameters(c.getTypeparamList(), ctor.getTypeParameters(), "%"));

            fid.setArgs(getArgs(c.getTypeparamList(), ctor.getGenericParameterTypes()));
            fid.setThrowables(getThrows(c.getTypeparamList(), ctor.getGenericExceptionTypes()));
            c.getTypeparamList().clear("%");

            //  Parse the annotation set present
            List<AnnotationItem> alist = parse(c, 0, ctor.getDeclaredAnnotations());
            Annotation[][] aas = ctor.getParameterAnnotations();
            if (aas != null && aas.length != 0) {
                for (int k = 0; k < aas.length; k++) {
                    alist.addAll(parse(c, k + 1, aas[k]));
                }
            }
            fid.setAnnoList(AnnotationItem.toArray(alist));
        }
    }

    private int getSyntheticMethodCount(Method[] methods) {
        int count = 0;
        for (Method method : methods) {
            if (method.isSynthetic()) {
                count++;
            }
        }
        return count;
    }

    private int getSyntheticConstructorCount(Constructor[] ctors) {
        int count = 0;
        for (Constructor ctor : ctors) {
            if (ctor.isSynthetic()) {
                count++;
            }
        }
        return count;
    }

    private int getSyntheticFieldCount(Field[] flds) {
        int count = 0;
        for (Field fld : flds) {
            if (fld.isSynthetic()) {
                count++;
            }
        }
        return count;
    }

    private void readMethods(ClassDescription c, Class classObject) {
        Method[] tmp = classObject.getDeclaredMethods();

        c.createMethods(tmp.length - getSyntheticMethodCount(tmp));

        String fqname = c.getQualifiedName();
        ClassDescription.TypeParameterList tp = c.getTypeparamList();

        for (int i = 0, j = -1; i < tmp.length; i++) {
            Method mtd = tmp[i];

            // skip synthetic methods
            if (mtd.isSynthetic()) {
                if (bo.isSet(Option.DEBUG)) {
                    getLog().println(i18n.getString("TigerRefgClassDescrLoader.message.synthetic_method_skipped", mtd));
                }
                continue;
            }

            int mod = mtd.getModifiers();

            MethodDescr fid = new MethodDescr(mtd.getName(), fqname, mod);
            c.setMethod(++j, fid);

            if (mtd.getDefaultValue() != null) {
                fid.addModifier(com.sun.tdk.signaturetest.model.Modifier.HASDEFAULT);
                fid.setDefaultValue(mtd.getDefaultValue().toString());
            }

            if (mtd.isVarArgs()) {
                fid.addModifier(com.sun.tdk.signaturetest.model.Modifier.VARARGS);
            }

            fid.setTypeParameters(scanFormalTypeParameters(c.getTypeparamList(), mtd.getTypeParameters(), "%"));

            fid.setType(decodeType(tp, mtd.getGenericReturnType()));

            fid.setArgs(getArgs(tp, mtd.getGenericParameterTypes()));
            fid.setThrowables(getThrows(tp, mtd.getGenericExceptionTypes()));
            c.getTypeparamList().clear("%");

            //  Parse the annotation set present
            List<AnnotationItem> alist = parse(c, 0, mtd.getDeclaredAnnotations());
            Annotation[][] aas = mtd.getParameterAnnotations();
            if (aas != null && aas.length != 0) {
                for (int k = 0; k < aas.length; k++) {
                    alist.addAll(parse(c, k + 1, aas[k]));
                }
            }
            fid.setAnnoList(AnnotationItem.toArray(alist));
        }
    }

    private int getSyntheticNestedCount(Class[] clss) {
        int count = 0;
        for (Class clss1 : clss) {
            if (clss1.isSynthetic()) {
                count++;
            }
        }
        return count;
    }

    private void readNested(ClassDescription c, Class classObject) {
        Class[] nestedClasses = classObject.getDeclaredClasses();

        if (nestedClasses.length != 0) {
            c.createNested(nestedClasses.length - getSyntheticNestedCount(nestedClasses));
            for (int i = 0, j = -1; i < nestedClasses.length; i++) {
                Class nc = nestedClasses[i];

                if (nc.isSynthetic()) {
                    continue;
                }

                c.setNested(++j, new InnerDescr(nc.getName(), classObject.getName(), nc.getModifiers()));
            }
        }
    }

    //
//  Generic types helper methods
//
    private String scanFormalTypeParameters(ClassDescription.TypeParameterList typeparamList, TypeVariable[] params, String declared) {
        if (params == null || params.length == 0) {
            return null;
        }

        typeparamList.reset_count();

        for (TypeVariable param : params) {
            typeparamList.add(param.getName(), declared);
        }

        StringBuffer sb = new StringBuffer("<");

        for (int i = 0; i < params.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }

            //  replace type variable with its ordinal number
            sb.append('%').append(i);

            Type[] bounds = params[i].getBounds();
            List<String> tmp = new ArrayList<>();
            for (Type bound : bounds) {
                tmp.add(decodeType(typeparamList, bound));
            }

            String first = tmp.remove(0);
            sb.append(" extends ").append(first);

            if (tmp.size() != 0) {
                Collections.sort(tmp);
                for (String o : tmp) {
                    sb.append(" & ").append(o);
                }
            }
        }

        sb.append(">");

        return sb.toString();
    }

    private String getArgs(ClassDescription.TypeParameterList typeparamList, Type[] args) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < args.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(decodeType(typeparamList, args[i]));
        }

        return sb.toString();
    }

    private String getThrows(ClassDescription.TypeParameterList typeparamList, Type[] xthrows) {
        String[] tmp = new String[xthrows.length];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = decodeType(typeparamList, xthrows[i]);
        }

        return MemberDescription.getThrows(tmp);
    }

    private String decodeType(ClassDescription.TypeParameterList typeparamList, Type t) {
        if (t instanceof Class) {
            Class x = (Class) t;
            return MemberDescription.getTypeName(x);
        } else if (t instanceof TypeVariable) {
            TypeVariable x = (TypeVariable) t;
            return typeparamList.replace(x.getName());
        } else if (t instanceof GenericArrayType) {
            GenericArrayType x = (GenericArrayType) t;
            return decodeType(typeparamList, x.getGenericComponentType()) + "[]";
        } else if (t instanceof ParameterizedType) {
            ParameterizedType x = (ParameterizedType) t;
            return decodeType(typeparamList, x.getRawType()) + decodeArguments(typeparamList, x);
        } else if (t instanceof WildcardType) {
            WildcardType x = (WildcardType) t;
            StringBuffer sb = new StringBuffer("?");
            Type[] bounds;
            if ((bounds = x.getLowerBounds()) != null && bounds.length != 0) {
                if (bounds[0] != null || bounds.length > 1) {
                    sb.append(" super ").append(decodeBounds(typeparamList, bounds));
                }
            }
            if ((bounds = x.getUpperBounds()) != null) {
                String s = decodeBounds(typeparamList, bounds);
                //  Reduce "? extends java.lang.Object" to just "?"
                if (s.startsWith(object)) {
                    s = s.substring(object.length()).trim();
                }
                if (s.length() > 0) {
                    sb.append(" extends ").append(s);
                }
            }
            return sb.toString();
        } else {
            assert false;  // need to investigate this situation
            return "???" + (t == null ? "notype" : t.getClass().getName());
        }
    }

    private String decodeBounds(ClassDescription.TypeParameterList tp, Type[] bounds) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bounds.length; i++) {
            if (i != 0) {
                sb.append(" & ");
            }
            sb.append(bounds[i] == null ? object : decodeType(tp, bounds[i]));
        }
        return sb.toString();
    }

    private String decodeArguments(ClassDescription.TypeParameterList tp, ParameterizedType d) {
        StringBuffer sb = new StringBuffer();

        Type[] vv = d.getActualTypeArguments();
        if (vv != null) {
            for (int i = 0; i < vv.length; i++) {
                if (i != 0) {
                    sb.append(",");
                }

                if (vv[i] != null) {
                    sb.append(decodeType(tp, vv[i]));
                }
            }
        }

        return (sb.length() == 0) ? "" : '<' + sb.toString() + '>';
    }

    //
//  Annotation parsing methods
//
    List<AnnotationItem> parse(ClassDescription c, int target, Annotation[] xx) {
        List<AnnotationItem> annolist = new ArrayList<>();

        if (xx != null) {
            for (Annotation xx1 : xx) {
                try {
                    annolist.add(parse(c, target, xx1));
                } catch (RuntimeException ex) {
                    // Annotation can be inaccessible, see CODETOOLS-7901631
                    // So we should ignore java.lang.reflect.InaccessibleObjectException
                    // not referring statically to that class because it exists since 1.9
                    // and this code should be 1.5 compatible
                    if (!"java.lang.reflect.InaccessibleObjectException".equals(ex.getClass().getName())) {
                        throw ex;
                    }
                }
            }
        }

        return annolist;
    }

    AnnotationItem parse(ClassDescription c, int target, Annotation a) {
        Class intf = a.annotationType();
        AnnotationItem anno = new AnnotationItem(target, intf.getName());

        if (intf.isAnnotationPresent(Inherited.class)) {
            anno.setInheritable(true);
        }

        Method[] mm = intf.getDeclaredMethods();
        AccessibleObject.setAccessible(mm, true);

        ClassDescription.TypeParameterList tp = c.getTypeparamList();

        if (mm.length != 0) {

            for (Method method : mm) {
                try {
                    AnnotationItem.Member member = new AnnotationItem.Member(
                            decodeType(tp, method.getGenericReturnType()),
                            method.getName(), null);

                    Object value = method.invoke(a, (Object[]) null);

                    if (value instanceof Annotation) {
                        member.setValue(parse(c, 0, (Annotation) value));
                    } else if (value instanceof Annotation[]) {
                        Annotation[] tmp_value = (Annotation[]) value;
                        AnnotationItem[] items = new AnnotationItem[tmp_value.length];
                        for (int i = 0; i < tmp_value.length; ++i) {
                            items[i] = parse(c, 0, tmp_value[i]);
                        }
                        member.setValue(items);
                    } else {
                        member.setValue(value);
                    }

                    anno.addMember(member);
                } catch (Exception e) {
                    SwissKnife.reportThrowable(e);
                    throw new Error(i18n.getString("TigerRefgClassDescrLoader.error.invalidannot", a));
                }
            }
        }

        return anno;
    }

    private final Set<Hint> hints = new HashSet<>();

    public void addLoadingHint(Hint hint) {
        hints.add(hint);
    }

    private boolean hasHint(Hint hint) {
        return hints.contains(hint);
    }

    private PrintWriter getLog() {
        if (log == null) {
            log = new PrintWriter(System.err);
        }
        return log;
    }

    public void setLog(PrintWriter log) {
        this.log = log;
    }

    private PrintWriter log;
}
