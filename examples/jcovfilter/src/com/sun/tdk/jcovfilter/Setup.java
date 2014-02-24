package com.sun.tdk.jcovfilter;

import com.sun.tdk.signaturetest.core.ClassHierarchy;
import com.sun.tdk.signaturetest.model.AnnotationItem;
import com.sun.tdk.signaturetest.util.BatchFileParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.SwissKnife;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

public class Setup extends com.sun.tdk.signaturetest.Setup {

    private HashSet<String> excludes = new HashSet<String>();
    private HashSet<String> includes = new HashSet<String>();
    private HashSet<String> apiInclude = new HashSet<String>();
    private HashSet<String> apiExclude = new HashSet<String>();

    public static void main(String[] args) {
        Setup t = new Setup();
        new Util().init(t, t.excludes, t.includes, t.apiInclude, t.apiExclude);
        long start = System.currentTimeMillis();
        t.run(args, new PrintWriter(System.err, true), null);
        long time = (System.currentTimeMillis() - start) / 1000;
        System.out.println("Elasped time " + time + " sec.");
        t.exit();
    }

    @Override
    protected boolean parseParameters(String[] args) {
        try {
            args = BatchFileParser.processParameters(args);
        } catch (CommandLineParserException ex) {
            SwissKnife.reportThrowable(ex);
        }
        ArrayList<String> rest = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            String par = args[i];
            if (par.equalsIgnoreCase("-excludesig")) {
                excludes.add(args[++i]);
            } else if (par.equalsIgnoreCase("-includesig")) {
                includes.add(args[++i]);
            } else if (par.equalsIgnoreCase("-apiinclude")) {
                apiInclude.add(args[++i]);
            } else if (par.equalsIgnoreCase("-apiexclude")) {
                apiExclude.add(args[++i]);
            } else {
                rest.add(par);
            }
        }
        // by default API is java and javax packages
        if (apiInclude.isEmpty()) {
            apiInclude.add("java");
            apiInclude.add("javax");
        }
        return super.parseParameters(rest.toArray(new String[]{}));
    }

    @Override
    protected String getComponentName() {
        return "JCov filter setup";
    }

    @Override
    protected boolean addInherited() {
        return false;
    }

    @Override
    protected AnnotationItem[] removeUndocumentedAnnotations(AnnotationItem[] annotations, ClassHierarchy h) {
        return annotations;
    }

}
