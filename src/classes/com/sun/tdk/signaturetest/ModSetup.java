package com.sun.tdk.signaturetest;

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.ModuleDescriptionLoader;
import com.sun.tdk.signaturetest.core.context.ModSetupOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ModuleDescription;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.sigfile.ModWriter;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.util.*;

import java.io.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ModSetup extends ModBase {

    private ModSetupOptions mo = AppContext.getContext().getBean(ModSetupOptions.class);
    private String copyrightStr = null;


    protected ModSetup() {
        super();
    }

    /**
     * runs test in from command line.
     */
    public static void main(String[] args) {
        ModSetup t = new ModSetup();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    /**
     * runs test with the given arguments.
     */
    public void run(String[] args, PrintWriter pw, PrintWriter ref) {

        setLog(pw);

        if (parseParameters(args)) {
            create();
            getLog().flush();
        } else if (args.length > 0 && Option.VERSION.accept(args[0])) {
            pw.println(Version.getVersionInfo());
        } else {
            usage();
        }

    }


    /**
     * parses parameters and initialize fields as specified by arguments
     *
     * @param args contains arguments required to be parsed.
     */
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

        // create arguments
        if (modIncl.isEmpty()) {
            modIncl.addPackage("");
        }

        if (mo.getValue(Option.FILE_NAME) == null) {
            return error(i18n.getString("Setup.error.filename.missing"));
        }

        copyrightStr = mo.getValue(Option.COPYRIGHT);
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

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        mo.readOptions(optionName, args);
    }

    /**
     * Prints help text.
     */
    protected void usage() {
        StringBuffer sb = new StringBuffer();

        sb.append(getComponentName() + " - " + i18n.getString("MSetup.usage.version", Version.Number));
        System.err.println(sb.toString());
    }

    protected String getComponentName() {
        return "Mod-Setup";
    }

    /**
     * creates signature file.
     */
    private boolean create() {
        URL signatureFile;

        ModWriter writer = null;
        FileOutputStream fos = null;

        try {
            HashMap<String, ModuleDescription> model = new HashMap<>();
            {
                signatureFile = FileManager.getURL(mo.getValue(Option.TEST_URL), mo.getValue(Option.FILE_NAME));
                ModuleDescriptionLoader mdl = getModuleLoader();
                Set<ModuleDescription> modules = mdl.loadBootModules();
                for (ModuleDescription md : modules) {
                    model.put(md.getName(), md);
                }
            }
            filterModuleSet(model, true);

            Writer w = getFileManager().getDefaultFormat().getWriter();
            if (w instanceof ModWriter) {
                writer = (ModWriter) w;
            } else {
                throw new IllegalStateException();
            }
            fos = new FileOutputStream(signatureFile.getFile());
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            writer.init(new PrintWriter(osw));
            writer.addFeature(FeaturesHolder.ConstInfo);
            writer.addFeature(FeaturesHolder.TigerInfo);
            writer.addFeature(FeaturesHolder.ModuleInfo);


            if (apiVer != null) {
                writer.setApiVersion(apiVer);
            }

            if (copyrightStr != null) {
                writer.addFeature(FeaturesHolder.CopyRight);
                FeaturesHolder.CopyRight.setText("# " + copyrightStr);
            }

            writer.writeHeader();
            Iterator<ModuleDescription> it = model.values().iterator();
            while (it.hasNext()) {
                ModuleDescription md = it.next();
                writer.write(md);
            }

        } catch (IOException e) {
            if (mo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            getLog().println(i18n.getString("Setup.error.message.cantcreatesigfile"));
            getLog().println(e);
            return error(i18n.getString("Setup.error.message.cantcreatesigfile"));
        } finally {
            if (writer != null) {
                writer.close();
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                SwissKnife.reportThrowable(ex);
            }
        }


        return passed();
    }


}
