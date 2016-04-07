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

package com.sun.tdk.signaturetest.loaders;

import com.sun.tdk.signaturetest.core.ModuleDescriptionLoader;
import com.sun.tdk.signaturetest.model.ModuleDescription;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Layer;
import java.util.*;


public class ModuleLoader implements ModuleDescriptionLoader {

    @Override
    public Set<ModuleDescription> loadBootModules() {

        Set<ModuleDescription> result = new HashSet<>();

        Layer bootL = Layer.boot();
        Configuration c = bootL.configuration();
        if (c != null) {
            Set<ResolvedModule> rms = c.modules();

            for (ResolvedModule rm : rms) {

                ModuleDescriptor md = rm.reference().descriptor();

                ModuleDescription rmd = new ModuleDescription();
                // 1. name
                rmd.setName(md.name());
                // 2. version
                Optional<ModuleDescriptor.Version> vo = md.version();
                if (vo.isPresent()) {
                    rmd.setVersion(vo.get().toString());
                }

                // 3. packages
                rmd.setPackages(new LinkedHashSet(md.packages()));

                // 4. conceals ??
                rmd.setConceals(new LinkedHashSet(md.conceals()));

                // 5. exports
                Set<ModuleDescriptor.Exports> exports = md.exports();
                //System.out.println("    exports:");
                Set<ModuleDescription.Exports> rexports = new LinkedHashSet<>();

                for (ModuleDescriptor.Exports me : exports) {
                    ModuleDescription.Exports exp = new ModuleDescription.Exports();
                    exp.source = me.source();
                    exp.targets = new HashSet<String>(me.targets());
                    rexports.add(exp);
                }
                rmd.setExports(rexports);

                // 6. requires
                Set<ModuleDescriptor.Requires> reqs = md.requires();
                Set<ModuleDescription.Requires> rereqs = new LinkedHashSet<>();
                for (ModuleDescriptor.Requires r : reqs) {
                    ModuleDescription.Requires req = new ModuleDescription.Requires();
                    req.name = r.name();

                    if (!r.modifiers().isEmpty()) {

                        Set<ModuleDescription.Requires.Modifier> modifs = new LinkedHashSet<ModuleDescription.Requires.Modifier>();

                        for (ModuleDescriptor.Requires.Modifier m : r.modifiers()) {
                            switch (m) {
                                case PUBLIC:
                                    modifs.add(ModuleDescription.Requires.Modifier.PUBLIC);
                                    break;
                                case MANDATED:
                                    modifs.add(ModuleDescription.Requires.Modifier.MANDATED);
                                    break;
                                case SYNTHETIC:
                                    modifs.add(ModuleDescription.Requires.Modifier.SYNTHETIC);
                                    break;
                            }
                        }
                        req.modifiers = modifs;
                    }
                    rereqs.add(req);
                }
                rmd.setRequires(rereqs);

                // 7. provides
                Map<String, ModuleDescriptor.Provides> prvs = md.provides();
                LinkedHashMap<String, ModuleDescription.Provides> reprovides = new LinkedHashMap<>();
                for (Map.Entry<String, ModuleDescriptor.Provides> me : prvs.entrySet()) {
                    ModuleDescription.Provides pr = new ModuleDescription.Provides();
                    pr.service = me.getKey();
                    pr.providers = new LinkedHashSet<>(me.getValue().providers());
                    reprovides.put(pr.service, pr);
                }
                rmd.setProvides(reprovides);

                // 8. uses
                Set<String> us = new LinkedHashSet(md.uses());
                rmd.setUses(us);

                result.add(rmd);
            }

            return result;

        }


        return Collections.emptySet();
    }
}
