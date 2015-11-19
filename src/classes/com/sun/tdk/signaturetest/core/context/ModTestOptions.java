package com.sun.tdk.signaturetest.core.context;

import java.util.EnumSet;

public class ModTestOptions extends Options {

    private EnumSet<Option> options = EnumSet.of(Option.DEBUG,
            Option.FILE_NAME, Option.TEST_URL,
            Option.MOD_EXCLUDE, Option.MOD_INCLUDE,
            Option.PKG_INCLUDE, Option.PKG_EXCLUDE,
            Option.HELP, Option.VERSION, Option.APIVERSION);

    @Override
    public EnumSet<Option> getOptions() {
        return options;
    }

}
