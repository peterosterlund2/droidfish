package org.petero.droidfish.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class UCIOptions implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private ArrayList<String> names;
    private Map<String, OptionBase> options;

    public static enum Type {
        CHECK,
        SPIN,
        COMBO,
        BUTTON,
        STRING
    }

    public abstract static class OptionBase implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;
        public String name;
        public Type type;
        public boolean visible = true;

        @Override
        public OptionBase clone() throws CloneNotSupportedException {
            return (OptionBase)super.clone();
        }

        /** Return true if current value != default value. */
        abstract public boolean modified();

        /** Return current value as a string. */
        abstract public String getStringValue();

        /** Set option from string value. Return true if option was modified. */
        public final boolean setFromString(String value) {
            OptionBase o = this;
            switch (o.type) {
            case CHECK:
                if (value.toLowerCase(Locale.US).equals("true"))
                    return ((CheckOption)o).set(true);
                else if (value.toLowerCase(Locale.US).equals("false"))
                    return ((CheckOption)o).set(false);
                return false;
            case SPIN:
                try {
                    int val = Integer.parseInt(value);
                    SpinOption so = (SpinOption)o;
                    return so.set(val);
                } catch (NumberFormatException ex) {
                }
                return false;
            case COMBO:
                return ((ComboOption)o).set(value);
            case BUTTON:
                return false;
            case STRING:
                return ((StringOption)o).set(value);
            }
            return false;
        }
    }

    public static final class CheckOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public boolean value;
        public boolean defaultValue;
        CheckOption(String name, boolean def) {
            this.name = name;
            this.type = Type.CHECK;
            this.value = def;
            this.defaultValue = def;
        }
        @Override
        public boolean modified() {
            return value != defaultValue;
        }
        @Override
        public String getStringValue() {
            return value ? "true" : "false";
        }
        public boolean set(boolean value) {
            if (this.value != value) {
                this.value = value;
                return true;
            }
            return false;
        }
    }

    public static final class SpinOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public int minValue;
        public int maxValue;
        public int value;
        public int defaultValue;
        SpinOption(String name, int minV, int maxV, int def) {
            this.name = name;
            this.type = Type.SPIN;
            this.minValue = minV;
            this.maxValue = maxV;
            this.value = def;
            this.defaultValue = def;
        }
        @Override
        public boolean modified() {
            return value != defaultValue;
        }
        @Override
        public String getStringValue() {
            return String.format(Locale.US, "%d", value);
        }
        public boolean set(int value) {
            if ((value >= minValue) && (value <= maxValue)) {
                if (this.value != value) {
                    this.value = value;
                    return true;
                }
            }
            return false;
        }
    }

    public static final class ComboOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public String[] allowedValues;
        public String value;
        public String defaultValue;
        ComboOption(String name, String[] allowed, String def) {
            this.name = name;
            this.type = Type.COMBO;
            this.allowedValues = allowed;
            this.value = def;
            this.defaultValue = def;
        }
        @Override
        public boolean modified() {
            return !value.equals(defaultValue);
        }
        @Override
        public String getStringValue() {
            return value;
        }
        public boolean set(String value) {
            for (String allowed : allowedValues) {
                if (allowed.toLowerCase(Locale.US).equals(value.toLowerCase(Locale.US))) {
                    if (!this.value.equals(allowed)) {
                        this.value = allowed;
                        return true;
                    }
                    break;
                }
            }
            return false;
        }
    }

    public static final class ButtonOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public boolean trigger;
        ButtonOption(String name) {
            this.name = name;
            this.type = Type.BUTTON;
            this.trigger = false;
        }
        @Override
        public boolean modified() {
            return false;
        }
        @Override
        public String getStringValue() {
            return "";
        }
    }

    public static final class StringOption extends OptionBase {
        private static final long serialVersionUID = 1L;
        public String value;
        public String defaultValue;
        StringOption(String name, String def) {
            this.name = name;
            this.type = Type.STRING;
            this.value = def;
            this.defaultValue = def;
        }
        @Override
        public boolean modified() {
            return !value.equals(defaultValue);
        }
        @Override
        public String getStringValue() {
            return value;
        }
        public boolean set(String value) {
            if (!this.value.equals(value)) {
                this.value = value;
                return true;
            }
            return false;
        }
    }

    UCIOptions() {
        names = new ArrayList<String>();
        options = new TreeMap<String, OptionBase>();
    }

    @Override
    public UCIOptions clone() throws CloneNotSupportedException {
        UCIOptions copy = new UCIOptions();

        copy.names = new ArrayList<String>();
        copy.names.addAll(names);

        copy.options = new TreeMap<String, OptionBase>();
        for (Map.Entry<String, OptionBase> e : options.entrySet())
            copy.options.put(e.getKey(), e.getValue().clone());

        return copy;
    }

    public void clear() {
        names.clear();
        options.clear();
    }

    public boolean contains(String optName) {
        return getOption(optName) != null;
    }

    public final String[] getOptionNames() {
        return names.toArray(new String[names.size()]);
    }

    public final OptionBase getOption(String name) {
        return options.get(name.toLowerCase(Locale.US));
    }

    final void addOption(OptionBase p) {
        String name = p.name.toLowerCase(Locale.US);
        names.add(name);
        options.put(name, p);
    }
}
