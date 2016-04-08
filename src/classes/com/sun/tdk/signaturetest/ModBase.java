/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tdk.signaturetest;

import com.sun.tdk.signaturetest.core.ModuleDescriptionLoader;
import com.sun.tdk.signaturetest.core.PackageGroup;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.core.context.Options;
import com.sun.tdk.signaturetest.model.ModuleDescription;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Common part for ModTest and ModSetup
 */
public abstract class ModBase extends SigTest {
    protected static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(ModBase.class);
    private PackageGroup modIncl;
    private PackageGroup modExcl;
    private PackageGroup modExact;
    private PackageGroup pkgIncl;
    private PackageGroup pkgExcl;
    protected String apiVer = "";

    protected SimpleErrorFormatter errorFormatter = new SimpleErrorFormatter();

    public ModBase() {
        modIncl = new PackageGroup(true);
        modExcl = new PackageGroup(true);
        modExact = new PackageGroup(false);
        pkgIncl = new PackageGroup(true);
        pkgExcl = new PackageGroup(true);
    }

    protected void setupModulesAndPackages(Options o) {

        String modPatt = o.getValue(Option.MODULES);
        if (modPatt != null) {
            for (String m : modPatt.split(",")) {
                m = m.trim();
                if (!m.isEmpty()) {
                    if (m.startsWith("!")) {
                        assert m.length() > 1;
                        modExcl.addPackage(m.substring(1));
                    } else if (m.endsWith(".*")) {
                        modIncl.addPackage(m.substring(0, m.length() - 2));
                    } else {
                        modExact.addPackage(m);
                    }
                }
            }
        }

        pkgIncl.addPackages(o.getValues(Option.PKG_INCLUDE));
        pkgExcl.addPackages(o.getValues(Option.PKG_EXCLUDE));

        // create arguments
        if (modIncl.isEmpty() && modExact.isEmpty()) {
            modIncl.addPackage("");
        }
        if (pkgIncl.isEmpty()) {
            pkgIncl.addPackage("");
        }

    }

    protected ModuleDescriptionLoader getModuleLoader() {
        String lClassName = "com.sun.tdk.signaturetest.loaders.ModuleLoader";
        try {
            Class<ModuleDescriptionLoader> c = (Class<ModuleDescriptionLoader>) Class.forName(lClassName);
            ModuleDescriptionLoader md = c.newInstance();
            return md;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 1. - Filters out modules from the model which is not meet -modInclude and -modExclude options
     * 2. - For each unfiltered modules calls filterModule for filtering modules content
     * according to -pkgInclude and -pkgExclude options
     * @param model the model (ModuleDescriptions map)
     * @param isReference true for reference model (from data file), false for tested model
     * @param verbose prints extra info (what was filtered out) if -debug option was specified
     * @return
     */
    protected Map<String, ModuleDescription> filterModuleSet(Map<String, ModuleDescription> model, boolean isReference, boolean verbose) {
        assert model != null;
        Iterator<Map.Entry<String, ModuleDescription>> it = model.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ModuleDescription> en = it.next();

            if (!isAPiModule(en.getKey())) {
                if (verbose) {
                    System.out.printf("Module %s removed\n", en.getKey());
                }
                it.remove();
            } else {
                if (verbose) {
                    System.out.printf("Module %s \n", en.getKey());
                }
                filterModule(en.getValue(), isReference, verbose);
            }
        }
        return model;
    }

    /**
     * Filters out ModuleDescription's entities, all "nonApi" classes and packages are removed.
     * the scope is defined by -pkgInclude and -pkgExclude options.
     * it filters out packages, conceals, requires, exports, provides and uses
     * @param md the module for cleaning
     * @param isReference
     * @param verbose
     */
    protected void filterModule(ModuleDescription md, boolean isReference, boolean verbose) {
        filterPackageSet(md.getPackages(), isReference, verbose, "Package");
        filterPackageSet(md.getConceals(), isReference, verbose, "Conceal");
        filterRequires(md.getRequires(), isReference, verbose);
        filterExports(md.getExports(), isReference, verbose);
        filterProvides(md.getProvides(), isReference, verbose);
        filterPackageSet(md.getUses(), isReference, verbose, "Uses");
    }

    private void filterProvides(Map<String, ModuleDescription.Provides> provides, boolean isReference, boolean verbose) {
        assert provides != null;
        Iterator<Map.Entry<String, ModuleDescription.Provides>> it = provides.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ModuleDescription.Provides> en = it.next();
            if (!isApiPackage(en.getKey())) {
                if (verbose) System.out.printf(" - Service %s excluded\n", en.getKey());
                it.remove();
            }
            Iterator<String> itp = en.getValue().providers.iterator();
            while (itp.hasNext()) {
                String prName = itp.next();
                if (!isApiPackage(prName)) {
                    if (verbose) System.out.printf(" - Service provider %s excluded\n", prName);
                    itp.remove();
                }
            }
        }

    }

    private void filterRequires(Set<ModuleDescription.Requires> requires, boolean isReference, boolean verbose) {
        // nothing to filter ?
    }

    /**
     * Checks module name for consistency according to -modInclude and -modExclude
     * @param modName
     * @return
     */
    protected boolean isAPiModule(String modName) {
        boolean excluded = modExcl.checkModuleName(modName);
        boolean included = modIncl.checkModuleName(modName) || modExact.checkModuleName(modName);

        return included && !excluded;
    }

    /**
     * Checks package name for consistency according to -modInclude and -modExclude
     * @param packName
     * @return
     */
    protected boolean isApiPackage(String packName) {
        return pkgIncl.checkName(packName) && !pkgExcl.checkName(packName);
    }

    private Set<String> filterPackageSet(Set<String> pkgSet, boolean isReference, boolean verbose, String object) {
        Iterator<String> it = pkgSet.iterator();
        while (it.hasNext()) {
            String pkgName = it.next();
            if (!isApiPackage(pkgName)) {
                if (verbose) {
                    System.out.printf(" - %s %s excluded\n", object, pkgName);
                }
                it.remove();
            }
        }
        return pkgSet;
    }

    private Set<String> filterApiModuleSet(Set<String> pkgSet, boolean isReference, boolean verbose, String object) {
        Iterator<String> it = pkgSet.iterator();
        while (it.hasNext()) {
            String mName = it.next();
            if (!isAPiModule(mName)) {
                if (verbose) {
                    System.out.printf(" - %s %s excluded\n", object, mName);
                }
                it.remove();
            }
        }
        return pkgSet;
    }


    private Set<ModuleDescription.Exports> filterExports(Set<ModuleDescription.Exports> exports, boolean isReference, boolean verbose) {

        Iterator<ModuleDescription.Exports> it = exports.iterator();

        // remove non-api sources
        while (it.hasNext()) {
            ModuleDescription.Exports ex = it.next();
            if (!isApiPackage(ex.source)) {
                if (verbose) {
                    System.out.printf(" - Export %s excluded\n", ex.source);
                }
                it.remove();
            }
        }

        // remove non-api targets
        it = exports.iterator();
        while (it.hasNext()) {
            ModuleDescription.Exports ex = it.next();
            if (!ex.targets.isEmpty()) {
                filterApiModuleSet(ex.targets, isReference, verbose, "Export target");
                if (ex.targets.isEmpty()) {
                    if (verbose) {
                        System.out.printf(" - Export %s excluded because all targets were excluded\n", ex.source);
                    }
                    it.remove();
                }
            }
        }

        return exports;
    }

    class SimpleErrorFormatter {

        private ArrayList<String> errors = new ArrayList<>();

        void addError(String formatStr, Object... args) {
            errors.add(String.format(formatStr, args));
        }

        void addError(String msg) {
            errors.add(msg);
        }

        void out(PrintWriter log) {
            for (String m : errors) {
                log.println(m);
            }
        }

        int getNunErrors() {
            return errors.size();
        }
    }

}
