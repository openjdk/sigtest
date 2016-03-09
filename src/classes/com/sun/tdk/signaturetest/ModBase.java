package com.sun.tdk.signaturetest;

import com.sun.tdk.signaturetest.core.ModuleDescriptionLoader;
import com.sun.tdk.signaturetest.core.PackageGroup;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.errors.ErrorFormatter;
import com.sun.tdk.signaturetest.errors.SortedErrorFormatter;
import com.sun.tdk.signaturetest.model.ModuleDescription;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class ModBase extends SigTest {
    protected static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(ModBase.class);
    protected PackageGroup modIncl;
    protected PackageGroup modExcl;
    protected String apiVer = "";

    protected SimpleErrorFormatter errorFormatter = new SimpleErrorFormatter();

    public ModBase() {
        modIncl = new PackageGroup(true);
        modExcl = new PackageGroup(true);
    }

    protected ModuleDescriptionLoader getModuleLoader() {
        String lClassName= "com.sun.tdk.signaturetest.loaders.ModuleLoader";
        try {
            Class<ModuleDescriptionLoader> c = (Class<ModuleDescriptionLoader>) Class.forName(lClassName);
            ModuleDescriptionLoader md = c.newInstance();
            return md;
        } catch (Throwable  e ) {
            e.printStackTrace();
        }
        return null;
    }

    protected Map<String, ModuleDescription> filterModuleSet(Map<String, ModuleDescription> model, boolean isReference) {
        assert model != null;
        Iterator<Map.Entry<String, ModuleDescription>> it = model.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ModuleDescription> en = it.next();

            if (!isAPiModule(en.getKey())) {
                it.remove();
            }
        }
        return model;
    }

    protected boolean isAPiModule(String modName) {
        return modIncl.checkName(modName) && !modExcl.checkName(modName);
    }

    class SimpleErrorFormatter {

        private ArrayList<String> errors = new ArrayList<>();

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
