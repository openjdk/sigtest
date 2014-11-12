package com.sun.tdk.signaturetest.core.context;

import com.sun.tdk.signaturetest.util.CommandLineParser;

import java.util.*;

/**
 * @author Mikhail Ershov
 */
public abstract class Options {
    private Map<Option, List<String>> values;

    protected Options() {
        values = new HashMap<>();
    }

    public boolean readOptions(String optionName, String[] args) {
        for (Option option : getOptions()) {
            if (option.accept(optionName)) {
                switch (option.getKind()) {
                    case NONE:
                        values.put(option, null);
                        break;
                    case SINGLE_OPT:
                    case SINGLE_REQ:
                        assert args != null;
                        assert args.length > 0;
                        assert !args[0].isEmpty();
                        values.put(option, Arrays.asList(args[0]));
                        break;
                    case REQ_LIST:
                        assert args != null;
                        assert args.length > 0;
                        assert !args[0].isEmpty();
                        values.put(option, Arrays.asList(CommandLineParser.parseListOption(args)));
                        break;
                    case MANY_OPT:
                        assert args != null;
                        assert args.length > 0;
                        if (!values.containsKey(option)) {
                            values.put(option, new ArrayList<String>());
                        }
                        values.get(option).addAll(Arrays.asList(CommandLineParser.parseListOption(args)));
                        break;
                }
                return true;
            }
        }
        return false;
    }

    /* Returns if the NONE option was specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public boolean isSet(Option option) {
        assert option != null;
        assert option.getKind() == Option.Kind.NONE;
        if (!getOptions().contains(option)) {
            throw new IllegalArgumentException("Option " + option.getKey() + " is not defined in the context");
        }
        return values.containsKey(option);
    }

    /* Returns a value for SINGLE_OPT option
     * or null if the option was not specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public String getValue(Option option) {
        assert option != null;
        List<String> vals = getValues(option);
        if (vals == null) {
            return null;
        } else {
            return vals.get(0);
        }
    }

    /* Returns a list of values for MANY_OPT option
     * or null if the option was not specified
     * Throws IllegalArgumentException
     * if the option is not allowed for the context
     */
    public List<String> getValues(Option option) {
        assert option != null;
        if (!getOptions().contains(option)) {
            throw new IllegalArgumentException("Option " + option.getKey() + " is not defined in the context");
        }
        if (!values.containsKey(option)) {
            return null;
        }
        return values.get(option);
    }

    public abstract EnumSet<Option> getOptions();
}
