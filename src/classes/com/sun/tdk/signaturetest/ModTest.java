package com.sun.tdk.signaturetest;

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.ModuleDescriptionLoader;
import com.sun.tdk.signaturetest.core.PackageGroup;
import com.sun.tdk.signaturetest.core.context.ModTestOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ModuleDescription;
import com.sun.tdk.signaturetest.sigfile.MultipleFileReader;
import com.sun.tdk.signaturetest.sigfile.f43.F43Reader;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.OptionInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.PrintWriter;

import java.util.*;

public class ModTest extends ModBase {

    private ModTestOptions mo = AppContext.getContext().getBean(ModTestOptions.class);
    protected PackageGroup pkgIncl;
    protected PackageGroup pkgExcl;

    protected ModTest() {
        pkgIncl = new PackageGroup(true);
        pkgExcl = new PackageGroup(true);
    }

    public static void main(String[] args) {
        ModTest t = new ModTest();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    public boolean run(String[] args, PrintWriter pw, PrintWriter ref) {

        setLog(pw);

        Map<String, ModuleDescription> thisModel;
        Map<String, ModuleDescription> fileModel;

        if (parseParameters(args)) {
            thisModel = filterModuleSet(createModel(), false);
            fileModel = filterModuleSet(readFile(), true);
            check(thisModel, fileModel);
            errorFormatter.out(pw);

            getLog().flush();
        } else if (args.length > 0 && Option.VERSION.accept(args[0])) {
            pw.println(Version.getVersionInfo());
        } else {
            usage();
        }

        if (errorFormatter.getNunErrors() == 0) {
            return passed();
        } else {
            return failed(i18n.getString("SignatureTest.mesg.failed",
                    Integer.toString(errorFormatter.getNunErrors())));
        }

    }

    private boolean check(Map<String, ModuleDescription> thisModel, Map<String, ModuleDescription> thatModel) {

        // 1. module list

        {
            Set<String> onlyHere = new HashSet<>(thisModel.keySet());
            onlyHere.removeAll(thatModel.keySet());

            Set<String> onlyThere = new HashSet<>(thatModel.keySet());
            onlyThere.removeAll(thisModel.keySet());


            for (String mName : onlyHere) {
                errorFormatter.addError("Extra module " + mName + " found");
            }

            for (String mName : onlyThere) {
                errorFormatter.addError("Required module " + mName + " is not found");
            }
        }

        for (String mN : thisModel.keySet() ) {

            ModuleDescription thisModule = thisModel.get(mN);
            ModuleDescription thatModule = thatModel.get(mN);

            if (thatModule == null) {
                //System.out.println(" - " + mN);
                continue;
            }

            // 2. exports

            Set<ModuleDescription.Exports> thisExports = thisModule.getExports();
            Set<ModuleDescription.Exports> thatExports = thatModule.getExports();

            thisExports = filterExports(thisExports);
            thatExports = filterExports(thatExports);

            for(ModuleDescription.Exports thisEx : thisExports) {
                ModuleDescription.Exports thatEx = null;
                for (ModuleDescription.Exports t : thatExports) {
                    if (t.source.equals(thisEx.source)) {
                        thatEx = t;
                        break;
                    }
                }
                if (thatEx == null) {
                    errorFormatter.addError("Extra export " + thisEx.source + " found in module " + thisModule.getName());
                    continue;
                }
                if (!thisEx.targets.equals(thatEx.targets)) {
                    errorFormatter.addError("Different targets for export " + thisEx.source + " in module " + thisModule.getName() + ": " + thisEx.targets + " and " + thatEx.targets);
                    continue;
                }
            }
            for(ModuleDescription.Exports thatEx : thatExports) {
                ModuleDescription.Exports thisEx = null;
                for (ModuleDescription.Exports t : thisExports) {
                    if (t.source.equals(thatEx.source)) {
                        thisEx = t;
                        break;
                    }
                }
                if (thisEx == null) {
                    errorFormatter.addError("Missing export " + thatEx.source + " found in module " + thatModule.getName());
                    continue;
                }
            }

            // 3. provides
            Set<String> thisProvides = thisModule.getProvides().keySet();
            Set<String> thatProvides = thatModule.getProvides().keySet();
            thisProvides = filterProvides(thisProvides);
            thatProvides = filterProvides(thatProvides);
            if (!thisProvides.equals(thatProvides)) {

                Set<String> onlyHere = new HashSet<>(thisProvides);
                onlyHere.removeAll(thatProvides);

                Set<String> onlyThere = new HashSet<>(thatProvides);
                onlyThere.removeAll(thisProvides);

                for (String sName : onlyHere) {
                    errorFormatter.addError("Extra service " + sName + " provided by module " + thisModule.getName() );
                }
                for (String sName : onlyThere) {
                    errorFormatter.addError("Required service " + sName + " is not provided by module " + thisModule.getName() );
                }
            }
        }
        return errorFormatter.getNunErrors() == 0;
    }

    private Set<String> filterProvides(Set<String> provides) {
        Iterator<String> it = provides.iterator();
        while(it.hasNext()) {
            String service = it.next();
            if (!isApiPackage(service)) {
                //System.out.println(" - service " + service);
                it.remove();
            }
        }
        return provides;
    }

    private Set<ModuleDescription.Exports> filterExports(Set<ModuleDescription.Exports> exports) {

        Iterator<ModuleDescription.Exports> it = exports.iterator();

        // remove non-api sources
        while( it.hasNext()) {
            ModuleDescription.Exports ex = it.next();
            if (!isApiPackage(ex.source)) {
                //System.out.println(" - source " + ex.source);
                it.remove();
            }
        }

        // remove non-api targets
        it = exports.iterator();
        while( it.hasNext()) {
            ModuleDescription.Exports ex = it.next();
            if (ex.targets.isEmpty()) {
                continue;
            }
            Iterator<String> ti = ex.targets.iterator();
            while (ti.hasNext()) {
                String t = ti.next();
                if (!isAPiModule(t)) {
                    //System.out.println(" - target " + t);
                    ti.remove();
                }
            }
            if (ex.targets.isEmpty()) {
                // no visible targets
                //System.out.println(" - export not visible " + ex.source);
                it.remove();
            }
        }

        return exports;
    }

    private boolean isApiPackage(String packName) {
        return pkgIncl.checkName(packName) && !pkgExcl.checkName(packName);
    }


    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        mo.readOptions(optionName, args);
    }


    private Map<String, ModuleDescription> readFile() {

        MultipleFileReader in = new MultipleFileReader(getLog(), MultipleFileReader.CLASSPATH_MODE, getFileManager());
        String testUrl = "";
        if (mo.getValue(Option.TEST_URL) != null) {
            testUrl = mo.getValue(Option.TEST_URL);
        }
        in.readSignatureFile(testUrl, mo.getValue(Option.FILE_NAME));

        Map<String, ModuleDescription> modules = new HashMap<>();
        List<Document> docs = in.getDocuments();
        if (docs != null) {
            for (Document d : docs) {
                Element m = d.getDocumentElement();
                ModuleDescription md = F43Reader.fromDom(m);
                modules.put(md.getName(), md);
            }
        }

        return modules;
    }

    private Map<String, ModuleDescription> createModel() {
        ModuleDescriptionLoader mdl = getModuleLoader();
        Set<ModuleDescription> modules = mdl.loadBootModules();
        Map<String, ModuleDescription> model = new HashMap<>();
        for (ModuleDescription md : modules) {
            model.put(md.getName(), md);
        }
        return model;
    }

    protected boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");


        // Print help text only and exit.
        if (args == null || args.length == 0
                || (args.length == 1 && Option.VERSION.accept(args[0]))) {
            return false;
        }

        final String optionsDecoder = "decodeOptions";

        parser.addOption(SigTest.VERBOSE_OPTION, OptionInfo.optionVariableParams(0, 1), optionsDecoder);
        parser.addOptions(mo.getOptions(), optionsDecoder);

        try {
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            getLog().println(e.getMessage());
            return failed(e.getMessage());
        }

        if (!processHelpOptions()) {
            return false;
        }

        modIncl.addPackages(mo.getValues(Option.MOD_INCLUDE));
        modExcl.addPackages(mo.getValues(Option.MOD_EXCLUDE));
        pkgIncl.addPackages(mo.getValues(Option.PKG_INCLUDE));
        pkgExcl.addPackages(mo.getValues(Option.PKG_EXCLUDE));

        // create arguments
        if (modIncl.isEmpty()) {
            modIncl.addPackage("");
        }

        if (mo.getValue(Option.FILE_NAME) == null) {
            return error(i18n.getString("Setup.error.filename.missing"));
        }

        if (mo.getValue(Option.APIVERSION) != null) {
            apiVer = mo.getValue(Option.APIVERSION);
        }

        if (mo.getValue(Option.TEST_URL) != null) {
            if (new File(mo.getValue(Option.FILE_NAME)).isAbsolute()) {
                return error(i18n.getString("Setup.error.testurl.absolutepath", new Object[]{Option.TEST_URL.getKey(), mo.getValue(Option.FILE_NAME)}));
            }
        }
        return true;
    }

    @Override
    protected void usage() {
        StringBuffer sb = new StringBuffer();

        sb.append(getComponentName() + " - " + i18n.getString("MTest.usage.version", Version.Number));
        System.err.println(sb.toString());
    }

    @Override
    protected String getComponentName() {
        return "Mod-Test";
    }
}
