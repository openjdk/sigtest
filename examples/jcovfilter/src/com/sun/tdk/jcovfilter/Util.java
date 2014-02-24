package com.sun.tdk.jcovfilter;

import com.sun.tdk.signaturetest.SigTest;
import com.sun.tdk.signaturetest.core.ClassHierarchy;
import com.sun.tdk.signaturetest.core.Erasurator;
import com.sun.tdk.signaturetest.model.AnnotationItem;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberCollection;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.MethodDescr;
import com.sun.tdk.signaturetest.plugin.Filter;
import com.sun.tdk.signaturetest.plugin.FormatAdapter;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.sigfile.f41.F41Format;
import com.sun.tdk.signaturetest.sigfile.f41.F41Writer;
import java.util.*;
import java.util.regex.Pattern;

class Util {

    private Set<String> excludeSig;
    private Set<String> includeSig;
    private Set<Pattern> excludeSigPatterns;
    private Set<Pattern> includeSigPatterns;
    private Set<String> apiInclude;
    private Set<String> apiExclude;
    private static Filter emptyFilter = new Filter() {

        @Override
        public boolean accept(ClassDescription cls) {
            return true;
        }
    };
    private Transformer markerTransformer = new Transformer() {

        @Override
        public ClassDescription transform(ClassDescription cls) throws ClassNotFoundException {
            if (isAPIClass(cls)) {
                return cls;
            }

            boolean overrides = false;
            ClassHierarchy ch = cls.getClassHierarchy();
            ArrayList<String> supers = new ArrayList();

            try {
                supers = new ArrayList(ch.getSuperClasses(cls.getQualifiedName()));
                supers.addAll(0, ch.getAllImplementedInterfaces(cls.getQualifiedName()));
            } catch (ClassNotFoundException e) {
                System.err.println("Can't process class " + cls.getQualifiedName() + " because can't find superclass " + e.getMessage());
                // clean the class
                cls.setMembers(new MemberCollection());
                return cls;
            }

            Erasurator er = new Erasurator();
            for (MethodDescr m : cls.getDeclaredMethods()) {
                if (ch.isMethodOverriden(m) || ch.isMethodImplements(m)) {
                    MethodDescr cloned_m = (MethodDescr) er.processMember(m);
                    String signature = cloned_m.getSignature();
                    for (int i = supers.size() - 1; i >= 0; i--) {
                        ClassDescription suc = ch.load(supers.get(i));
                        if (isAPIClass(suc)) {
                            er.erasure(suc);
                            for (MethodDescr ms : suc.getDeclaredMethods()) {
                                // try both erased and row cases
                                if (methodIncluded(ms) || methodIncluded((MethodDescr) er.processMember(ms))) {
                                    //MethodDescr cloned_ms = (MethodDescr) er.processMember(ms);
                                    //String su_signature = cloned_ms.getSignature();
                                    //if (su_signature.equals(signature)) {
                                    AnnotationItem[] list = m.getAnnoList();
                                    AnnotationItem nA = new AnnotationItem(0, OVERRIDESAPIFROM);
                                    AnnotationItem.Member aim = new AnnotationItem.Member("String", "className", suc.getQualifiedName());
                                    nA.addMember(aim);
                                    AnnotationItem[] newList = Arrays.copyOf(list, list.length + 1);
                                    newList[list.length] = nA;
                                    m.setAnnoList(newList);
                                    overrides = true;
                                    continue;
                                    //}
                                }
                            }
                        }
                    }

                }
            }
            if (overrides) {
                AnnotationItem[] list = cls.getAnnoList();
                AnnotationItem nA = new AnnotationItem(0, OVERRIDESAPI);
                AnnotationItem[] newList = Arrays.copyOf(list, list.length + 1);
                newList[list.length] = nA;
                cls.setAnnoList(newList);
            }
            return cls;
        }
    };
    private static Transformer cleanTransformer = new Transformer() {

        @Override
        public ClassDescription transform(ClassDescription cls) {
            MemberCollection cleaned = new MemberCollection();
            for (Iterator e = cls.getMembersIterator(); e.hasNext();) {
                MemberDescription mr = (MemberDescription) e.next();
                if (!mr.getDeclaringClassName().equals(cls.getQualifiedName())) {
                    continue;
                }
                cleaned.addMember(mr);
            }
            cls.setMembers(cleaned);
            return cls;
        }
    };
    private Transformer remTransformer = new Transformer() {

        @Override
        public ClassDescription transform(ClassDescription cls) {

            if (isAPIClass(cls)) {
                // remove non-public stuff from API classes
                MemberCollection cleaned = new MemberCollection();
                for (Iterator e = cls.getMembersIterator(); e.hasNext();) {
                    MemberDescription mr = (MemberDescription) e.next();
                    if (mr.isPublic() || mr.isProtected()) {
                        if (mr.isMethod() || mr.isConstructor()) {
                            cleaned.addMember(mr);
                        }
                    }
                }
                cls.setMembers(cleaned);
                return cls;
            }
            MemberCollection cleaned = new MemberCollection();
            boolean found = false;
            for (AnnotationItem ai : cls.getAnnoList()) {
                if (OVERRIDESAPI.equals(ai.getName())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                cls.setMembers(cleaned);
                return cls;
            }

            nextMember:
            for (Iterator e = cls.getMembersIterator(); e.hasNext();) {
                MemberDescription mr = (MemberDescription) e.next();
                for (AnnotationItem ai : mr.getAnnoList()) {
                    if (OVERRIDESAPIFROM.equals(ai.getName())) {
                        cleaned.addMember(mr);
                        continue nextMember;
                    }
                }
            }

            cls.setMembers(cleaned);
            return cls;
        }
    };

    private boolean methodIncluded(MethodDescr ms) {
        String exSig = ms.getDeclaringClassName() + "::" + ms.getSignature();
        initPatterns();
        if (includeSig.isEmpty()) {
            if (excludeSig.contains(exSig)) {
                return false;
            }
            // try it as regexp
            for (Pattern p : excludeSigPatterns) {
                if (p.matcher(exSig).matches()) {
                    return false;
                }
            }
            return true;
        } else {
            if (includeSig.contains(exSig)) {
                return true;
            }
            // try it as regexp
            for (Pattern p : includeSigPatterns) {
                if (p.matcher(exSig).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean isAPIClass(ClassDescription cls) {
        String fqn = cls.getQualifiedName();
        if (cls.isPublic() || cls.isProtected()) {
            for (String prefix : apiInclude) {
                if (fqn.equals(prefix)
                        || fqn.startsWith(prefix + ".")
                        || fqn.startsWith(prefix + "$")) {
                    for (String exclPrefix : apiExclude) {
                        if (fqn.startsWith(exclPrefix)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    void init(SigTest st, Set<String> excludes, Set<String> includes, Set<String> apiInclude, Set<String> apiExclude) {
        this.excludeSig = excludes;
        this.includeSig = includes;

        this.apiExclude = apiExclude;
        this.apiInclude = apiInclude;
        PluginAPI.IS_CLASS_ACCESSIBLE.setFilter(emptyFilter);
        PluginAPI.AFTER_BUILD_MEMBERS.setTransformer(cleanTransformer);
        PluginAPI.CLASS_CORRECTOR.setTransformer(markerTransformer);
        PluginAPI.AFTER_CLASS_CORRECTOR.setTransformer(remTransformer);
        FormatAdapter fa = new FormatAdapter(new F41Format().getVersion());
        Writer w = new F41Writer() {

            @Override
            public void write(ClassDescription cls) {
                if (cls.getMembersIterator().hasNext()) {
                    super.write(cls);
                }
            }

            @Override
            protected void writeHiders(ClassDescription classDescription, StringBuffer buf) {

            }
        };

        fa.setWriter(w);
        st.setFormat(fa);

    }

    void initPatterns() {
        if (excludeSigPatterns == null) {
            excludeSigPatterns = new HashSet<Pattern>();
            for (String s : excludeSig) {
                excludeSigPatterns.add(Pattern.compile(s));
            }

            includeSigPatterns = new HashSet<Pattern>();
            for (String s : includeSig) {
                includeSigPatterns.add(Pattern.compile(s));
            }
        }
    }

    public static final String OVERRIDESAPI = "OverridesAPI";
    public static final String OVERRIDESAPIFROM = "OverridesAPIFrom";
}
