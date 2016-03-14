package com.sun.tdk.signaturetest.loaders;

import com.sun.tdk.signaturetest.core.ModuleDescriptionLoader;
import com.sun.tdk.signaturetest.model.ModuleDescription;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Layer;
import java.util.*;


public class ModuleLoader implements ModuleDescriptionLoader {

    @Override
    public Set<ModuleDescription> loadBootModules() {

        Set<ModuleDescription> result = new HashSet<>();

        Layer bootL = Layer.boot();
        Configuration c = bootL.configuration();
        if (c != null) {
            Set<ModuleDescriptor> mds = c.descriptors();
            for (ModuleDescriptor md : mds ) {

                ModuleDescription rmd = new ModuleDescription();
                // 1. name
                rmd.setName(md.name());
                // 2. version
                Optional<ModuleDescriptor.Version> vo = md.version();
                if (vo.isPresent()) {
                    rmd.setVersion(vo.get().toString());
                }

                // 3. packages
                rmd.setPackages(md.packages());

                // 4. conceals ??
                rmd.setConceals(md.conceals());

                // 5. exports
                Set<ModuleDescriptor.Exports> exports = md.exports();
                //System.out.println("    exports:");
                Set<ModuleDescription.Exports> rexports = new java.util.HashSet<>();

                for (ModuleDescriptor.Exports me : exports) {
                    ModuleDescription.Exports exp = new ModuleDescription.Exports();
                    exp.source = me.source();
                    exp.targets = new HashSet<String>(me.targets());
                    rexports.add(exp);
                }
                rmd.setExports(rexports);

                // 6. requires
                Set<ModuleDescriptor.Requires> reqs = md.requires();
                Set<ModuleDescription.Requires> rereqs = new HashSet<>();
                for (ModuleDescriptor.Requires r : reqs) {
                    ModuleDescription.Requires req = new ModuleDescription.Requires();
                    req.name = r.name();

                    if (!r.modifiers().isEmpty()) {

                        Set<ModuleDescription.Requires.Modifier> modifs = new HashSet<ModuleDescription.Requires.Modifier>();

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
                HashMap<String, ModuleDescription.Provides> reprovides = new java.util.HashMap<>();
                for (Map.Entry<String, ModuleDescriptor.Provides> me : prvs.entrySet()) {
                    ModuleDescription.Provides pr = new ModuleDescription.Provides();
                    pr.service = me.getKey();
                    pr.providers = new HashSet<String>(me.getValue().providers());
                    reprovides.put(pr.service, pr);
                }
                rmd.setProvides(reprovides);

                // 8. uses
                Set<String> us = new HashSet(md.uses());
                rmd.setUses(us);

                result.add(rmd);
            }

            return result;

        }


        return Collections.emptySet();
    }
}
