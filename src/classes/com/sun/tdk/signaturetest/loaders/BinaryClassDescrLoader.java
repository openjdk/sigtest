/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.SigTest;
import com.sun.tdk.signaturetest.classpath.Classpath;
import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.core.PrimitiveTypes;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.LRUCache;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.io.*;
import java.util.*;

/**
 * This is subclass of the MemberCollectionBuilder provides searching class
 * files in the specified class path and loading ClassDescription created via
 * class file parsing. This class contains cache of the parsed classes. This
 * cache is changed using LRU algorithm.
 *
 * @author Maxim Sokolnikov
 * @author Roman Makarchuk
 */
public class BinaryClassDescrLoader implements ClassDescriptionLoader, LoadingHints {

    private final BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);

    private static class BinaryClassDescription extends ClassDescription implements AutoCloseable {

        private int major_version;          // class file format versions
        private int minor_version;
        private Constant[] constants;       // in-memory copy of the constant pool
        private String[] sigctors,
                sigfields,
                sigmethods;

        private void readCP(DataInput classData) throws IOException {
            int n = classData.readUnsignedShort();
            constants = new Constant[n];
            constants[0] = null;
            for (int i = 1; i < n; i++) {
                constants[i] = new Constant();
                constants[i].read(classData);
                if (constants[i].tag == CONSTANT_Long || constants[i].tag == CONSTANT_Double) {
                    i++;
                }
            }
        }

        List<MemberDescription> getMethodRefs() {
            List<MemberDescription> memberList = new ArrayList<>();
            int n = constants.length;
            for (int i = 1; i < n; i++) {
                if (constants[i].tag == CONSTANT_Long || constants[i].tag == CONSTANT_Double) {
                    i++;
                    continue;
                }
                if (constants[i].tag != CONSTANT_Methodref && constants[i].tag != CONSTANT_InterfaceMethodref
                        && constants[i].tag != CONSTANT_Fieldref) {
                    continue;
                }
                int refs = (Integer) getConstant(i).info;
                short nameAndType = (short) refs;
                short decl = (short) (refs >> 16);

                String methodName = getMethodName(nameAndType);
                String className = getClassName(decl);
                boolean isConstructor = "<init>".equals(methodName);
                MemberDescription fid;

                if (constants[i].tag == CONSTANT_Fieldref) {
                    fid = new FieldDescr(methodName, className, 1);
                } else {
                    if (isConstructor) {
                        fid = new ConstructorDescr(this, 1);
                        fid.setDeclaringClass(className);
                    } else {
                        fid = new MethodDescr(methodName, className, 1);
                    }

                    String descr = getMethodType(nameAndType);
                    int pos = descr.indexOf(')');

                    try {
                        fid.setArgs(BinaryClassDescrLoader.getArgs(descr.substring(1, pos)));
                    } catch (IllegalArgumentException e) {
                        err(i18n.getString("BinaryClassDescrLoader.message.incorrectformat", Short.toString(decl)));
                    }
                }
                memberList.add(fid);
            }
            return memberList;
        }

        private Constant getConstant(int i) {
            if (i <= 0 || i >= constants.length) {
                throw new ClassFormatError(i18n.getString("BinaryClassDescrLoader.error.cpoutofbounds"));
            }

            return constants[i];
        }

        //  Read and store constant pool
        //
        String getClassName(int i) {
            if (i == 0) {
                return null;
            }

            Constant c = getConstant(i);
            c.checkConstant(CONSTANT_Class);

            c = getConstant((Integer) c.info);
            c.checkConstant(CONSTANT_Utf8);

            return ((String) c.info).replace('/', '.');
        }

        private String getMethodName(int i) {
            if (i == 0) {
                return null;
            }

            Constant c = getConstant(i);
            c.checkConstant(CONSTANT_NameAndType);
            return getName((Integer) c.info >> 16);
        }

        private String getMethodType(int i) {
            if (i == 0) {
                return null;
            }
            Constant c = getConstant(i);
            c.checkConstant(CONSTANT_NameAndType);
            return getName(((Integer) c.info).shortValue());
        }

        private Object getConstantValue(int i) {
            Constant c = getConstant(i);
            if (c.tag == CONSTANT_String) {
                return getName(((Short) c.info).intValue() & 0xFFFF);
            }

            return c.info;
        }

        private String getName(int i) {
            Constant c = getConstant(i);
            c.checkConstant(CONSTANT_Utf8);

            return (String) c.info;
        }

        private void cleanup() {
            sigctors = null;
            sigfields = null;
            sigmethods = null;
            constants = null;
        }

        @Override
        public void close() {
            cleanup();
        }
    }

    private boolean ignoreAnnotations = false;
    /**
     * findByName and open class files as InputStream.
     */
    private final Classpath classpath;
    /**
     * cache of the loaded classes.
     */
    private final LRUCache<String, BinaryClassDescription> cache;
    /**
     * This stack is used to prevent infinite recursive calls of load(String
     * name) method. E.g. the annotation Documented is one example of such
     * recursion
     */
    private final Map<String, BinaryClassDescription> stack = new HashMap<>();

    /**
     * creates new instance.
     *
     * @param classpath  contains class files.
     * @param bufferSize size of the class cache.
     */
    public BinaryClassDescrLoader(Classpath classpath, Integer bufferSize) {
        this.classpath = classpath;
        cache = new LRUCache<>(bufferSize);
    }

    /**
     * loads class with the given className
     *
     * @param className className of the class required to be found.
     */
    public ClassDescription load(String className) throws ClassNotFoundException {
        className = ExoticCharTools.decodeExotic(className);

        assert className.indexOf('<') == -1 : className;

        // search in the cache
        BinaryClassDescription c = cache.get(className);

        if (c != null) {
            return c;
        }

        // check recursive call
        c = stack.get(className);
        if (c != null) {
            return c;
        }

        // load class if the cache does not contains required class.
        InputStream is = null;
        try {
            c = new BinaryClassDescription();

            stack.put(className, c);
            is = classpath.findClass(className);
            readClass(c, is, className);
            cache.put(className, c);
        } catch (IOException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            throw new ClassNotFoundException(className);
        } finally {
            stack.remove(className);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return c;
    }

    public ClassDescription altLoad(String className) throws ClassNotFoundException {
        Classpath cp = AppContext.getContext().getInputClasspath();
        if (cp != null) {
            try {
                return cp.findClassDescription(className);
            } catch (ClassNotFoundException e) {
                ClassDescriptionLoader l = AppContext.getContext().getClassLoader();
                if (l != null) {
                    return l.load(className);
                }
            }
        }
        throw new ClassNotFoundException(className);
    }

    private static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(BinaryClassDescrLoader.class);
    // Magic number identifying class file format
    private static final int MAGIC = 0b11001010111111101011101010111110;
    private static final int TIGER_CLASS_VERSION = 49;
    private static final int J7_CLASS_VERSION = 51;
    private static final int J15_CLASS_VERSION = 59;
    //  Constant pool tags (see the JVM II 4.4, p. 103)
    private static final int CONSTANT_Utf8 = 1,
            CONSTANT_Integer = 3,
            CONSTANT_Float = 4,
            CONSTANT_Long = 5,
            CONSTANT_Double = 6,
            CONSTANT_Class = 7,
            CONSTANT_String = 8,
            CONSTANT_Fieldref = 9,
            CONSTANT_Methodref = 10,
            CONSTANT_InterfaceMethodref = 11,
            CONSTANT_NameAndType = 12,
            CONSTANT_MethodHandle = 15,
            CONSTANT_MethodType = 16,
            CONSTANT_Dynamic = 17,
            CONSTANT_InvokeDynamic = 18,
            CONSTANT_ModuleId = 19,
            CONSTANT_ModuleQuery = 20;

    //  Constant pool entry
    private static class Constant {

        byte tag;
        Object info;

        void checkConstant(int exp) {
            if (tag != exp) {
                String[] consts = {Integer.toString(exp & 0xFF), Integer.toString(tag & 0xFF)};
                throw new ClassFormatError(i18n.getString("BinaryClassDescrLoader.error.const", consts));
            }
        }

        String getName() {
            checkConstant(CONSTANT_Utf8);
            return (String) info;
        }

        void read(DataInput classData) throws IOException {
            tag = classData.readByte();
            switch (tag) {
                case CONSTANT_Class:
                    info = classData.readUnsignedShort();
                    break;

                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    info = classData.readInt();
                    break;

                case CONSTANT_String:
                    info = classData.readShort();
                    break;

                case CONSTANT_Integer:
                    info = classData.readInt();
                    break;

                case CONSTANT_Float:
                    info = classData.readFloat();
                    break;

                case CONSTANT_Long:
                    info = classData.readLong();
                    break;

                case CONSTANT_Double:
                    info = classData.readDouble();
                    break;

                case CONSTANT_NameAndType:
                    info = classData.readInt();
                    break;

                case CONSTANT_Utf8:
                    info = classData.readUTF();
                    break;

                case CONSTANT_MethodHandle:
                    //int reference_kind
                    classData.readUnsignedByte();
                    //int reference_index
                    classData.readUnsignedShort();
                    info = "CONSTANT_MethodHandle";
                    break;

                case CONSTANT_MethodType:
                    //int descriptor_index
                    classData.readUnsignedShort();
                    info = "CONSTANT_MethodType";
                    break;

                case CONSTANT_Dynamic:
                    // don't we need these values?
                    //int bootstrap_method_attr_index
                    classData.readUnsignedShort();
                    //int name_and_type_index
                    classData.readUnsignedShort();
                    info = "CONSTANT_Dynamic";
                    break;

                case CONSTANT_InvokeDynamic:
                    // don't we need these values?
                    //int bootstrap_method_attr_index
                    classData.readUnsignedShort();
                    //int name_and_type_index
                    classData.readUnsignedShort();
                    info = "CONSTANT_InvokeDynamic";
                    break;

                case CONSTANT_ModuleId:
                case CONSTANT_ModuleQuery:
                    // don't we need these values?
                    //int name_index
                    classData.readUnsignedShort();
                    //int version_index
                    classData.readUnsignedShort();
                    info = "CONSTANT_ModuleId";
                    break;

                default:
                    throw new ClassFormatError(i18n.getString("BinaryClassDescrLoader.error.unknownconst",
                            Integer.toString(tag)));
            }
        }
    }

    private void readClass(BinaryClassDescription c, InputStream is, String className) {
        try(DataInputStream classData = new DataInputStream(is)) {
            readClass(c, classData);
        } catch (Throwable t) {
            System.err.println(i18n.getString("BinaryClassDescrLoader.error.classname", className));
            SwissKnife.reportThrowable(t);
        } finally {
            c.cleanup();
        }
    }

    private void readClass(BinaryClassDescription c, DataInput classData) throws IOException {

        int magic = classData.readInt();
        if (magic != MAGIC) {
            String[] invargs = {Integer.toString(magic), Integer.toString(MAGIC)};
            throw new ClassFormatError(i18n.getString("BinaryClassDescrLoader.error.magicnum", invargs));
        }

        c.minor_version = classData.readUnsignedShort();
        c.major_version = classData.readUnsignedShort();

        c.setTiger(c.major_version >= TIGER_CLASS_VERSION);

        c.readCP(classData);

        int flags = classData.readUnsignedShort();
        c.setModifiers(flags);

        //  for nested class, name of the declaring class can be obtained from
        //  the 'InnerClasses' attribute only.
        String clName = c.getClassName(classData.readUnsignedShort());
        c.setupClassName(clName, MemberDescription.NO_DECLARING_CLASS);   // can be reassigned later

        String s = c.getClassName(classData.readUnsignedShort());
        if (s != null && (!c.hasModifier(Modifier.INTERFACE))) {
            SuperClass superClass = new SuperClass();
            superClass.setupClassName(s);
            c.setSuperClass(superClass);
        }

        String fqname = c.getQualifiedName();

        int n = classData.readUnsignedShort();
        c.createInterfaces(n);
        for (int i = 0; i < n; i++) {
            s = c.getClassName(classData.readUnsignedShort());
            SuperInterface fid = new SuperInterface();
            fid.setupGenericClassName(s);
            fid.setDirect(true);
            c.setInterface(i, fid);
        }

        readFields(c, classData);

        readMethods(c, classData);

        ClassAttrs attrs = new ClassAttrs();
        attrs.read(c, classData);

        c.setAnnoList(AnnotationItem.toArray(attrs.annolist));

        //  Generic-specific processing
        SignatureParser parser = null;

        if (c.major_version >= TIGER_CLASS_VERSION) {
            //  (JDK 1.5-b35) :
            //  workaround for class 'javax.crypto.SunJCE_c' with version 45:3
            //  and meaningless signature attribute.

            ClassDescription.TypeParameterList tp = null;
            String declaringClass = c.getDeclaringClassName();
            if (!MemberDescription.NO_DECLARING_CLASS.equals(declaringClass)) {

                Classpath.KIND_CLASS_DATA k = classpath.isClassPresent(declaringClass);

                if (k == Classpath.KIND_CLASS_DATA.NOT_FOUND) {
                    c.setNoDeclaringClass();
                    warning(i18n.getString("BinaryClassDescrLoader.error.enclosing_class_not_found", c.getQualifiedName()));
                } else {
                    try {
                        ClassDescription enc;
                        if (k == Classpath.KIND_CLASS_DATA.DESCRIPTION) {
                            enc = classpath.findClassDescription(declaringClass);
                        } else {
                            try {
                                enc = load(declaringClass);
                            } catch (ClassNotFoundException ex) {
                                enc = altLoad(declaringClass);
                            }
                        }
                        tp = enc.getTypeparamList();
                    } catch (ClassNotFoundException e) {
                        throw new ClassFormatError(i18n.getString("BinaryClassDescrLoader.error.enclosing", fqname));
                    }
                }

            }

            ClassDescription.TypeParameterList typeparamList = new ClassDescription.TypeParameterList(tp);
            c.setTypeparamList(typeparamList);
            parser = new SignatureParser(fqname, typeparamList);
        }

        //  Process the 'Signature' attributes
        if (parser != null) {

            if (attrs.signature != null) {
                try {
                    parser.scanClass(attrs.signature);
                    c.setTypeParameters(parser.generic_pars);

                    SuperClass superClass = c.getSuperClass();
                    if (superClass != null) {
                        superClass.setupGenericClassName(parser.class_supr);
                    }

                    int num = parser.class_intfs.size();
                    c.createInterfaces(num);
                    for (int i = 0; i < num; i++) {
                        SuperInterface fid = new SuperInterface();
                        fid.setupGenericClassName(parser.class_intfs.get(i));
                        fid.setDirect(true);
                        c.setInterface(i, fid);
                    }
                } catch (SigAttrError e) {
                    warning(e.getMessage());
                }
            }

            //  The 'Signature' attributes of fields, methods and constructors
            //  must be processed only after the corresponding class attribute
            postFields(c, parser);
            postMethods(c, parser);
        }
    }

    public List<MemberDescription> loadCalls(String name) throws ClassNotFoundException {

        // String name = ClassCorrector.stripGenerics(className);
        List<MemberDescription> result;
        try {
            BinaryClassDescription c = new BinaryClassDescription();
            try (DataInputStream classData = new DataInputStream(classpath.findClass(name))) {
                readClass(c, classData);
                result = c.getMethodRefs();
            } finally {
                c.cleanup();
            }
        } catch (Throwable e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            throw new ClassNotFoundException(name, e);
        }
        return result;
    }

    private class ClassAttrs extends AttrsIter {

        int access = -1;

        void check(BinaryClassDescription c, String attrName) throws IOException {
            if ("InnerClasses".equals(attrName)) {
                List<InnerDescr> tmp = null;

                String fqname = c.getQualifiedName();

                int n = is.readUnsignedShort();
                for (int i = 0; i < n; i++) {
                    String inner = c.getClassName(is.readUnsignedShort());
                    String outer = c.getClassName(is.readUnsignedShort());
                    is.readUnsignedShort();
                    int x = is.readUnsignedShort();

                    if (inner != null && inner.equals(fqname)) {
                        if (access != -1) {
                            err(null);
                        }
                        access = x;
                        c.setModifiers(x);
                        c.setupClassName(fqname, outer);
                    }

                    if (!hasHint(LoadingHints.READ_SYNTETHIC)) {
                        // skip synthetic inner classes
                        if (Modifier.hasModifier(x, Modifier.ACC_SYNTHETIC)) {
                            continue;
                        }
                    }

                    // Warning: javax.crypto.SunJCE_m reported as inner class of javax.crypto.Cipher
                    // so additional check inner.indexOf(outer) added!
                    if (outer != null && outer.equals(fqname) && inner != null && inner.indexOf(outer) == 0) {
                        if (tmp == null) {
                            tmp = new ArrayList<>();
                        }

                        tmp.add(new InnerDescr(inner, outer, x));
                    }
                }

                if (tmp != null) {
                    c.setNestedClasses(tmp.toArray(InnerDescr.EMPTY_ARRAY));
                }
            } else if (SigTest.isTigerFeaturesTracked && "PermittedSubclasses".equals(attrName)) {
                checkVersion(c, attrName, J15_CLASS_VERSION);

                int n = is.readUnsignedShort();
                c.createPermittedSubclasses(n);
                for (int i = 0; i < n; i++) {
                    String permittedSubClassName = c.getClassName(is.readUnsignedShort());
                    PermittedSubClass permittedSubClass = new PermittedSubClass();
                    permittedSubClass.setupGenericClassName(permittedSubClassName);
                    c.setPermittedSubclass(i, permittedSubClass);
                }
            }
        }
    }

    //  Process fields
    private void readFields(BinaryClassDescription c, DataInput classData) throws IOException {

        int n = classData.readUnsignedShort();

        List<FieldDescr> flds = new ArrayList<>();
        List<String> tmpflds = new ArrayList<>();

        for (int i = 0; i < n; i++) {

            int mod = classData.readUnsignedShort();
            FieldDescr fid = new FieldDescr(c.getName(classData.readUnsignedShort()), c.getQualifiedName(), mod);

            String type = convertVMType(c.getName(classData.readUnsignedShort()));
            fid.setType(type);

            FieldAttrs attrs = new FieldAttrs();
            attrs.read(c, classData);

            if (!hasHint(LoadingHints.READ_SYNTETHIC)) {
                if (fid.hasModifier(Modifier.ACC_SYNTHETIC)) {
                    if (bo.isSet(Option.DEBUG)) {
                        getLog().println(i18n.getString("BinaryClassDescrLoader.message.synthetic_field_skipped",
                                fid.getType() + " " + fid.getQualifiedName()));
                    }
                    continue;
                }
            }

            flds.add(fid);

            fid.setAnnoList(AnnotationItem.toArray(attrs.annolist));
            tmpflds.add(attrs.signature);

            if (fid.isFinal() && attrs.value != null) {
                switch (type) {
                    case "boolean":
                        attrs.value = (Integer) attrs.value != 0;
                        break;
                    case "byte":
                        attrs.value = ((Integer) attrs.value).byteValue();
                        break;
                    case "char":
                        attrs.value = (char) ((Integer) attrs.value).shortValue();
                        break;
                }

                fid.setConstantValue(MemberDescription.valueToString(attrs.value));
            }
        }

        if ((n = flds.size()) != 0) {
            c.setFields(flds.toArray(FieldDescr.EMPTY_ARRAY));
            c.sigfields = tmpflds.toArray(EMPTY_STRING_ARRAY);
        }
    }

    private void postFields(BinaryClassDescription c, SignatureParser parser) {
        String sig;

        if (c.sigfields != null) {
            for (int i = 0; i < c.sigfields.length; i++) {
                if ((sig = c.sigfields[i]) != null) {
                    try {
                        parser.scanField(sig);
                        c.getField(i).setType(parser.field_type);
                    } catch (SigAttrError e) {
                        warning(e.getMessage());
                    }
                }
            }
        }
    }

    class FieldAttrs extends AttrsIter {

        Object value = null;

        void check(BinaryClassDescription c, String s) throws IOException {
            if ("ConstantValue".equals(s)) {
                if (value != null) {
                    err(null);
                }

                value = c.getConstantValue(is.readUnsignedShort());
            }
        }
    }

    //  Process methods and constructors
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private void readMethods(BinaryClassDescription c, DataInput classData) throws IOException {
        List<MemberDescription> ctors = new ArrayList<>(),
                mthds = new ArrayList<>();

        List<String> tmpctors = new ArrayList<>(),
                tmpmthds = new ArrayList<>();

        int n = classData.readUnsignedShort();
        for (int i = 0; i < n; i++) {

            int modif = classData.readUnsignedShort();
            String methodName = c.getName(classData.readUnsignedShort());

            boolean isConstructor = "<init>".equals(methodName);
            MemberDescription memberD;

            if (isConstructor) {
                memberD = new ConstructorDescr(c, modif);
            } else {
                memberD = new MethodDescr(methodName, c.getQualifiedName(), modif);
            }

            boolean isBridgeMethod = memberD.hasModifier(Modifier.BRIDGE);
            boolean isSynthetic = memberD.hasModifier(Modifier.ACC_SYNTHETIC);
//            boolean isSyntheticConstuctor = isConstructor && fid.hasModifier(Modifier.ACC_SYNTHETIC);

            String descr = c.getName(classData.readUnsignedShort());
            int pos = descr.indexOf(')');

            try {
                memberD.setArgs(getArgs(descr.substring(1, pos)));
            } catch (IllegalArgumentException e) {
                err(i18n.getString("BinaryClassDescrLoader.message.incorrectformat", c.getQualifiedName()));
            }

            memberD.setType(convertVMType(descr.substring(pos + 1)));

            MethodAttrs attrs = new MethodAttrs();
            attrs.read(c, classData);

            // skip synthetic methods and constructors
            if (!hasHint(LoadingHints.READ_SYNTETHIC) && isSynthetic) {
                if (bo.isSet(Option.DEBUG)) {
                    if (isConstructor) {
                        getLog().println(i18n.getString("BinaryClassDescrLoader.message.synthetic_constr_skipped",
                                memberD.getQualifiedName() + "(" + memberD.getArgs() + ")"));
                    } else {
                        String signature = memberD.getType() + " " + memberD.getQualifiedName() + "(" + memberD.getArgs() + ")";
                        if (isBridgeMethod) {
                            getLog().println(i18n.getString("BinaryClassDescrLoader.message.bridge", signature));
                        } else {
                            getLog().println(i18n.getString("BinaryClassDescrLoader.message.synthetic_method_skipped",
                                    signature));
                        }
                    }
                }
                continue;
            }

            if (attrs.annodef != null) {
                memberD.addModifier(Modifier.HASDEFAULT);
                if (memberD instanceof MethodDescr) {
                    ((MethodDescr) memberD).setAnnoDef(attrs.annodef);
                }
            }

            memberD.setThrowables(MemberDescription.getThrows(attrs.xthrows));

            if (isConstructor) {
                memberD.setType(MemberDescription.NO_TYPE);
                ctors.add(memberD);
                tmpctors.add(attrs.signature);
            } else if ("<clinit>".equals(memberD.getName())) {
                //  ignore
            } else {
                mthds.add(memberD);
                tmpmthds.add(attrs.signature);
            }

            memberD.setAnnoList(AnnotationItem.toArray(attrs.annolist));
        }

        if ((n = ctors.size()) != 0) {
            c.setConstructors(ctors.toArray(ConstructorDescr.EMPTY_ARRAY));
            c.sigctors = tmpctors.toArray(EMPTY_STRING_ARRAY);
        }

        if ((n = mthds.size()) != 0) {
            c.setMethods(mthds.toArray(MethodDescr.EMPTY_ARRAY));
            c.sigmethods = tmpmthds.toArray(EMPTY_STRING_ARRAY);
        }
    }

    private void postMethods(BinaryClassDescription cls, SignatureParser parser) {
        String sig;

        if (cls.sigctors != null) {
            for (int i = 0; i < cls.sigctors.length; i++) {
                if ((sig = cls.sigctors[i]) != null) {
                    try {
                        parser.scanMethod(sig);
                        ConstructorDescr c = cls.getConstructor(i);
                        postMethod(parser, c);
                        c.setType(MemberDescription.NO_TYPE);
                    } catch (SigAttrError e) {
                        warning(e.getMessage());
                    }
                }
            }
        }

        if (cls.sigmethods != null) {
            for (int i = 0; i < cls.sigmethods.length; i++) {
                if ((sig = cls.sigmethods[i]) != null) {
                    try {
                        if (!isAnonimouseSimple(cls)) {
                            parser.scanMethod(sig);
                            postMethod(parser, cls.getMethod(i));
                        }
                    } catch (SigAttrError e) {
                        warning(e.getMessage());
                    }
                }
            }
        }
    }

    // simplified logic because of
    // cls initialization is not complited
    private static boolean isAnonimouseSimple(ClassDescription cls) {
        return Character.isDigit(cls.getName().charAt(0));
    }

    private static void postMethod(SignatureParser parser, MemberDescription fid) {
        fid.setTypeParameters(parser.generic_pars);
        fid.setType(parser.field_type);

        StringBuffer sb = new StringBuffer();

        if (parser.method_args != null && !parser.method_args.isEmpty()) {
            sb.append(parser.method_args.get(0));
            for (int i = 1; i < parser.method_args.size(); i++) {
                sb.append(MemberDescription.ARGS_DELIMITER).append(parser.method_args.get(i));
            }
        }

        fid.setArgs(sb.toString());

        sb.setLength(0);
        if (parser.method_throws != null && !parser.method_throws.isEmpty()) {
            sb.append(parser.method_throws.get(0));
            for (int i = 1; i < parser.method_throws.size(); i++) {
                sb.append(MemberDescription.THROWS_DELIMITER).append(parser.method_throws.get(i));
            }
            fid.setThrowables(sb.toString());
        }
    }

    //  Pack arguments the following way:
    //      (<a1>, ... <aN>)
    //  arguments are separated by ',' without any blanks, or
    //      ()
    //  if there are no arguments.
    //
    //  Note: this method has side-effect - its parameter (descr) gets changed.
    //
    private static String getArgs(String descr) throws IllegalArgumentException {

        if (descr.isEmpty()) {
            return descr;
        }

        int pos = 0;
        int lastPos = descr.length();

        String type;
        StringBuffer args = new StringBuffer();

        int dims = 0;

        while (pos < lastPos) {

            char ch = descr.charAt(pos);

            if (ch == 'L') {
                int delimPos = descr.indexOf(';', pos);
                if (delimPos == -1) {
                    delimPos = lastPos;
                }
                type = convertVMType(descr.substring(pos, delimPos + 1));
                pos = delimPos + 1;
            } else if (ch == '[') {
                dims++;
                pos++;
                continue;
            } else {
                type = PrimitiveTypes.getPrimitiveType(ch);
                pos++;
            }

            args.append(type);

            for (int i = 0; i < dims; ++i) {
                args.append("[]");
            }
            dims = 0;

            if (pos < lastPos) {
                args.append(',');
            }
        }

        return args.toString();
    }

    class MethodAttrs extends AttrsIter {

        String[] xthrows;

        void check(BinaryClassDescription c, String s) throws IOException {
            if ("Exceptions".equals(s)) {
                int n = is.readUnsignedShort();
                xthrows = new String[n];
                for (int i = 0; i < n; i++) {
                    xthrows[i] = c.getClassName(is.readUnsignedShort());
                }
            }
        }
    }

    //TreeSet<Integer> ts = new TreeSet<Integer>();
//  Commons
    //  Utility class to help in attributes processing.
    //
    private abstract class AttrsIter {

        DataInputStream is;
        boolean synthetic = false,
                deprecated = false;
        String signature = null;
        List<AnnotationItem> annolist = null;
        Object annodef = null;

        void read(BinaryClassDescription c, DataInput classData) throws IOException {
            int n = classData.readUnsignedShort();

            for (int i = 0; i < n; i++) {
                String name = c.getName(classData.readUnsignedShort());
                int count = classData.readInt();

                if (count != 0) {
                    byte[] info = new byte[count];
                    classData.readFully(info);
                    is = new DataInputStream(new ByteArrayInputStream(info));
                }

                if ("Synthetic".equals(name)) {
                    synthetic = true;
                } else if ("Deprecated".equals(name)) {
                    deprecated = true;
                } else if ("Signature".equals(name)) {
                    checkVersion(c, name, TIGER_CLASS_VERSION);
                    signature = c.getName(is.readUnsignedShort());
                } else if (SigTest.isTigerFeaturesTracked && "RuntimeVisibleAnnotations".equals(name)) {
                    checkVersion(c, name, TIGER_CLASS_VERSION);
                    readAnnotations(c, 0);

                } else if (SigTest.isTigerFeaturesTracked && "RuntimeInvisibleAnnotations".equals(name)) {
                    checkVersion(c, name, TIGER_CLASS_VERSION);
                    readAnnotations(c, 0);
                } else if (SigTest.isTigerFeaturesTracked && "RuntimeVisibleTypeAnnotations".equals(name)) {
                    checkVersion(c, name, J7_CLASS_VERSION);
                    readExtAnnotations(c, 0);
                } else if (SigTest.isTigerFeaturesTracked && "RuntimeInvisibleTypeAnnotations".equals(name)) {
                    checkVersion(c, name, J7_CLASS_VERSION);
                    readExtAnnotations(c, 0);
                } else if (SigTest.isTigerFeaturesTracked
                        && ("RuntimeVisibleParameterAnnotations".equals(name) || "RuntimeInvisibleParameterAnnotations".equals(name))) {
                    checkVersion(c, name, TIGER_CLASS_VERSION);
                    int m = is.readUnsignedByte();
                    for (int l = 0; l < m; l++) {
                        readAnnotations(c, l + 1);
                    }
                } else if (SigTest.isTigerFeaturesTracked && "AnnotationDefault".equals(name)) {
                    checkVersion(c, name, TIGER_CLASS_VERSION);
                    annodef = read_member_value(c);
                } else {
                    check(c, name);
                }

                if (count != 0) {
                    try {
                        is.close();
                    } catch (IOException x) {
                    }
                }
            }
        }

        void readAnnotations(BinaryClassDescription c, int target) throws IOException {
            if (ignoreAnnotations) {
                return;
            }
            if (annolist == null) {
                annolist = new ArrayList<>();
            }

            int m = is.readUnsignedShort();
            for (int l = 0; l < m; l++) {
                AnnotationItem anno = readAnnotation(c, target, false);
                annolist.add(anno);
            }
        }

        void readExtAnnotations(BinaryClassDescription c, int target) throws IOException {
            if (ignoreAnnotations) {
                return;
            }
            if (annolist == null) {
                annolist = new ArrayList<>();
            }
            int m = readShort();
            for (int l = 0; l < m; l++) {
                AnnotationItemEx anno = (AnnotationItemEx) readAnnotation(c, target, true);
                if (anno.getTracked() || hasHint(LoadingHints.READ_ANY_ANNOTATIONS)) {
                    annolist.add(anno);
                }
            }
        }

        private int readByte() throws IOException {
            return is.readUnsignedByte();
        }

        private int readChar() throws IOException {
            return is.readChar();
        }

        private int readShort() throws IOException {
            return is.readUnsignedShort();
        }

        private int[] readLocations() throws IOException {
            int location_length = is.readUnsignedShort();
            int[] loc = new int[location_length];
            for (int i = 0; i < location_length; i++) {
                loc[i] = is.readUnsignedByte();
            }
            return loc;
        }

        private AnnotationItem readAnnotation(BinaryClassDescription c, int target, boolean isExtended) throws IOException {
            AnnotationItem anno;
            if (isExtended) {
                AnnotationItemEx annox = new AnnotationItemEx(target);
                annox.parseBinaryDescription(is);
                anno = annox;
            } else {
                anno = new AnnotationItem(target);
            }

            String name = c.getName(readShort());
            name = getArgs(name);
            // convert from VM Ljavasoft/sqe/tests/lang/annot103/annot10301m0438/Simple;
            // to javasoft.sqe.tests.lang.annot103.annot10301m0438.Simple
            anno.setName(name);
            int k = is.readUnsignedShort();
            for (int j = 0; j < k; j++) {
                anno.addMember(new AnnotationItem.Member(c.getName(is.readUnsignedShort()),
                        read_member_value(c)));
            }

            completeAnnotation(anno);
            return anno;
        }

        void completeAnnotation(AnnotationItem anno) {
            try {
                ClassDescription c;
                try {
                    c = load(anno.getName());
                } catch (ClassNotFoundException ex) {
                    c = altLoad(anno.getName());
                }
                AnnotationItem[] annoList = c.getAnnoList();

                for (AnnotationItem annotationItem : annoList) {
                    if ("java.lang.annotation.Inherited".equals(annotationItem.getName())) {
                        anno.setInheritable(true);
                    }
                }

                MethodDescr[] fids = c.getDeclaredMethods();
                if (fids != null) {
                    for (MethodDescr fid : fids) {
                        AnnotationItem.Member member = anno.findByName(fid.getName());
                        if (member != null) {
                            anno.removeMember(member);
                            member.setType(fid.getType());
                        } else {
                            member = new AnnotationItem.Member(fid.getType(), fid.getName(), fid.getAnnoDef());
                        }
                        anno.addMember(member);
                    }
                }
            } catch (ClassNotFoundException e) {
                if (!notFoundAnnotations.contains(anno.getName())) {
                    getLog().println("Warning: " + i18n.getString("BinaryClassDescrLoader.error.annotnotfound", anno.getName()));
                    notFoundAnnotations.add(anno.getName());
                }
                //throw new ClassFormatError(i18n.getString("BinaryClassDescrLoader.error.annotnotfound", anno.getName()));
            }
        }

        Object read_member_value(BinaryClassDescription c) throws IOException {
            Object v;
            byte tag = is.readByte();
            switch (tag) {
                case 'Z':
                    v = c.getConstantValue(is.readUnsignedShort());
                    v = (Integer) v != 0;
                    break;

                case 'B':
                    v = c.getConstantValue(is.readUnsignedShort());
                    v = ((Integer) v).byteValue();
                    break;

                case 'C':
                    v = c.getConstantValue(is.readUnsignedShort());
                    v = (char) ((Integer) v).shortValue();
                    break;

                case 'D':
                case 'F':
                case 'I':
                case 'J':
                case 'S':
                case 's':
                    v = c.getConstantValue(is.readUnsignedShort());
                    break;

                case 'e': {
                    // Not used in fact
//                    String s = getName(is.readUnsignedShort());
                    is.readUnsignedShort();
                    v = new AnnotationItem.ValueWrap(c.getName(is.readUnsignedShort()));
                    break;
                }

                case 'c': {
                    String s = convertVMType(c.getName(is.readUnsignedShort()));
                    v = new AnnotationItem.ValueWrap("class " + s);
                    break;
                }

                case '@':
                    v = readAnnotation(c, 0, false);
                    break;

                case '[': {
                    int n = is.readUnsignedShort();
                    Object[] tmp = new Object[n];
                    for (int i = 0; i < n; i++) {
                        tmp[i] = read_member_value(c);
                    }
                    v = tmp;
                    break;
                }

                default:
                    throw new ClassFormatError(i18n.getString("BinaryClassDescrLoader.error.unknownannot",
                            Integer.toString(tag)));
            }

            return v;
        }

        void checkVersion(BinaryClassDescription c, String name, int vnbr) {
            String[] args = {name, c.getQualifiedName(), Integer.toString(c.major_version), Integer.toString(c.minor_version)};
            if (c.major_version < vnbr) {
                getLog().println(i18n.getString("BinaryClassDescrLoader.message.attribute", args));
            }
        }

        //  Process attribute with the name given.
        //  Attribute data can be read using the 'is' stream.
        //
        abstract void check(BinaryClassDescription c, String name) throws IOException;
    } //end of abstract class AttrsIter

    //    private Constant getConstant(short I) {
//        return getConstant(((int) I) & 0xFFFF);
//    }
    //  Convert JVM type notation (as described in the JVM II 4.3.2, p.100)
    //  to JLS type notation :
    //
    //      [<s>  -> <s>[]      <s> is converted recursively
    //      L<s>; -> <s>        characters '/' are replaced by '.' in <s>
    //      B     -> byte
    //      C     -> char
    //      D     -> double
    //      F     -> float
    //      I     -> int
    //      J     -> long
    //      S     -> short
    //      Z     -> boolean
    //      V     -> void       valid only in method return type
    //
    //
    static String convertVMType(String s) {
        return MemberDescription.getTypeName(s.replace('/', '.'));
    }

    static class SignatureParser {
        //  Parse results area -

        String generic_pars;
        String class_supr;
        List<String> class_intfs;
        String field_type;
        List<String> method_args,
                method_throws;
        //  - end of results area
        final String ownname;
        final ClassDescription.TypeParameterList typeparams;
        String sig;
        int siglength;
        char chr;
        int idx;

        SignatureParser(String name, final ClassDescription.TypeParameterList pars) {
            ownname = name;
            typeparams = pars;
        }

        void scanClass(String s) {

            sig = s + "\n";
            siglength = sig.length();
            idx = 0;
            chr = sig.charAt(idx++);

            generic_pars = scanFormalTypeParameters(ownname);
            class_supr = scanFieldTypeSignature(true);

            class_intfs = new ArrayList<>();
            while (!eol()) {
                String intf = scanFieldTypeSignature(true);
                class_intfs.add(intf);
            }
        }

        void scanField(String s) {

            sig = s + "\n";
            siglength = sig.length();
            idx = 0;
            chr = sig.charAt(idx++);

            field_type = scanFieldTypeSignature(true);
        }

        void scanMethod(String s) {

            sig = s + "\n";
            siglength = sig.length();
            idx = 0;
            chr = sig.charAt(idx++);

            generic_pars = scanFormalTypeParameters("%");

            method_args = new ArrayList<>();
            scanChar('(');
            while (chr != ')') {
                String t = scanFieldTypeSignature(true);
                method_args.add(t);
            }
            scanChar(')');

            field_type = scanFieldTypeSignature(true);

            method_throws = new ArrayList<>();
            while (chr == '^') {
                scanChar();
                String t = scanFieldTypeSignature(true);
                method_throws.add(t);
            }

            typeparams.clear("%");
        }

        String scanFormalTypeParameters(String declared) {
            if (chr != '<') {
                return null;
            }

            List<List<String>> parameters = new ArrayList<>();

            //  First pass:scan all parameters and store bounds because they may
            //  contain forward references
            typeparams.reset_count();
            scanChar('<');
            do {
                String ident = scanIdent(":>");
                List<String> bounds = new ArrayList<>();

                while (chr == ':') {
                    scanChar();

                    //  first bound (class bound) can be omitted
                    String bound = null;
                    if (chr != ':') {
                        bound = scanFieldTypeSignature(false);
                    }

                    if (bound != null) {
                        bounds.add(bound);
                    }
                }

                parameters.add(bounds);
                typeparams.add(ident, declared);

            } while (chr != '>');
            scanChar('>');

            //  Second pass: findByName and replace possible forward links and sort bounds
            StringBuffer sb = new StringBuffer();
            sb.append('<');
            for (int i = 0; i < parameters.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }

                //  replace type variable with its ordinal number
                sb.append('%').append(i);

                List<String> bounds = parameters.get(i);

                //  replace possible forward refs '{ident}'
                for (int k = 0; k < bounds.size(); k++) {
                    bounds.set(k, typeparams.replaceForwards(bounds.get(k)));
                }

                //  first bound is erasure and must stay in place
                //  remaining bounds (if any) are sorted
                if (!bounds.isEmpty()) {
                    String first = bounds.remove(0);
                    sb.append(" extends ").append(first);

                    if (!bounds.isEmpty()) {
                        Collections.sort(bounds);
                        for (String bound : bounds) {
                            sb.append(" & ").append(bound);
                        }
                    }
                }
            }
            sb.append('>');
            return sb.toString();
        }

        String scanFieldTypeSignature(boolean repl) {
            switch (chr) {
                case '[':
                    scanChar();
                    return scanFieldTypeSignature(repl) + "[]";

                case 'L': {
                    scanChar();
                    StringBuffer sb = new StringBuffer();
                    StringBuffer sb1 = new StringBuffer();

                    for (; ; ) {
                        sb.append(scanIdent("<.;").replace('/', '.'));

                        if (chr == '<') {
                            scanChar();
                            sb1.append('<');

                            sb1.append(scanTypeArgument(repl));

                            while (chr != '>') {
                                sb1.append(',').append(scanTypeArgument(repl));
                            }

                            scanChar('>');
                            sb1.append('>');
                        }

                        if (chr != '.') {
                            if (sb1.length() != 0) {
                                sb.append(sb1);
                            }
                            break;
                        }
                        sb1.setLength(0);

                        scanChar();
                        sb.append('$');
                    }

                    scanChar(';');
                    return sb.toString();
                }

                case 'T': {
                    scanChar();
                    String ident = scanIdent(";");
                    scanChar(';');
                    return repl ? typeparams.replace(ident) : ClassDescription.TypeParameterList.replaceNone(ident);
                }

                default: {
                    String t = PrimitiveTypes.getPrimitiveType(chr);
                    if (t != null) {
                        scanChar();
                        return t;
                    }

                    err("?TypeChar " + chr);
                    return null;
                }
            }
        }

        String scanTypeArgument(boolean repl) {

            final String object = "java.lang.Object";

            switch (chr) {
                case '*':
                    scanChar();
                    return "?";

                case '+': {
                    scanChar();

                    String s = scanFieldTypeSignature(repl);
                    //  Reduce "? extends java.lang.Object" to just "?"
                    if (s.startsWith(object)) {
                        s = s.substring(object.length()).trim();
                    }

                    return (!s.isEmpty()) ? "? extends " + s : "?";
                }

                case '-':
                    scanChar();
                    return "? super " + scanFieldTypeSignature(repl);

                default:
                    return scanFieldTypeSignature(repl);
            }
        }

        String scanIdent(final String term) {
            StringBuffer sb = new StringBuffer();

            while (term.indexOf(chr) == -1) {
                sb.append(chr);
                scanChar();
            }

            if (sb.length() == 0) {
                err(null);
            }

            return sb.toString();
        }

        char scanChar(char c) {
            if (chr != c) {
                err(null);
            }

            return scanChar();
        }

        char scanChar() {
            if (idx >= siglength) {
                err(null);
            }

            return chr = sig.charAt(idx++);
        }

        boolean eol() {
            return idx >= siglength;
        }

        void err(String msg) throws Error {
            throw new SigAttrError(showerr() + (msg == null ? "" : "\n" + msg));
        }

        String showerr() {
            String[] args = {sig.substring(0, siglength - 1), ownname};
            return i18n.getString("BinaryClassDescrLoader.error.attribute", args);
        }
    }

    private static void err(String s) {
        throw new ClassFormatError(s == null ? "???" : s);
    }

    private static class SigAttrError extends Error {

        SigAttrError(String msg) {
            super(msg);
        }
    }

    public void warning(String msg) {
        getLog().println(msg);
    }

    public void setIgnoreAnnotations(boolean value) {
        ignoreAnnotations = value;
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
    private final Set<String> notFoundAnnotations = new HashSet<>();
}
