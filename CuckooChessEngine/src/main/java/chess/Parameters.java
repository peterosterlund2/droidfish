package chess;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Parameters {
    public static enum Type {
        CHECK,
        SPIN,
        COMBO,
        BUTTON,
        STRING
    }

    public static class ParamBase {
        public String name;
        public Type type;
        public boolean visible;
    }

    public static final class CheckParam extends ParamBase {
        public boolean value;
        public boolean defaultValue;
        CheckParam(String name, boolean visible, boolean def) {
            this.name = name;
            this.type = Type.CHECK;
            this.visible = visible;
            this.value = def;
            this.defaultValue = def;
        }
    }

    public static final class SpinParam extends ParamBase {
        public int minValue;
        public int maxValue;
        public int value;
        public int defaultValue;
        SpinParam(String name, boolean visible, int minV, int maxV, int def) {
            this.name = name;
            this.type = Type.SPIN;
            this.visible = visible;
            this.minValue = minV;
            this.maxValue = maxV;
            this.value = def;
            this.defaultValue = def;
        }
    }

    public static final class ComboParam extends ParamBase {
        public String[] allowedValues;
        public String value;
        public String defaultValue;
        ComboParam(String name, boolean visible, String[] allowed, String def) {
            this.name = name;
            this.type = Type.COMBO;
            this.visible = visible;
            this.allowedValues = allowed;
            this.value = def;
            this.defaultValue = def;
        }
    }

    public static final class ButtonParam extends ParamBase {
        ButtonParam(String name, boolean visible) {
            this.name = name;
            this.type = Type.BUTTON;
            this.visible = visible;
        }
    }

    public static final class StringParam extends ParamBase {
        public String value;
        public String defaultValue;
        StringParam(String name, boolean visible, String def) {
            this.name = name;
            this.type = Type.STRING;
            this.visible = visible;
            this.value = def;
            this.defaultValue = def;
        }
    }

    public static Parameters instance() {
        return inst;
    }
    public final String[] getParamNames() {
        ArrayList<String> parNames = new ArrayList<String>();
        for (Map.Entry<String, ParamBase> e : params.entrySet())
            if (e.getValue().visible)
                parNames.add(e.getKey());
        return parNames.toArray(new String[parNames.size()]);
    }

    public final ParamBase getParam(String name) {
        return params.get(name);
    }

    private static final Parameters inst = new Parameters();
    private Map<String, ParamBase> params = new TreeMap<String, ParamBase>();

    private Parameters() {
        addPar(new SpinParam("qV", false, -200, 200, 0));
        addPar(new SpinParam("rV", false, -200, 200, 0));
        addPar(new SpinParam("bV", false, -200, 200, 0));
        addPar(new SpinParam("nV", false, -200, 200, 0));
        addPar(new SpinParam("pV", false, -200, 200, 0));
    }

    private final void addPar(ParamBase p) {
        params.put(p.name.toLowerCase(), p);
    }

    final boolean getBooleanPar(String name) {
        return ((CheckParam)params.get(name.toLowerCase())).value;
    }
    final int getIntPar(String name) {
        int ret = ((SpinParam)params.get(name.toLowerCase())).value;
        return ret;
    }
    final String getStringPar(String name) {
        return ((StringParam)params.get(name.toLowerCase())).value;
    }

    public final void set(String name, String value) {
        ParamBase p = params.get(name.toLowerCase());
        if (p == null)
            return;
        switch (p.type) {
        case CHECK: {
            CheckParam cp = (CheckParam)p;
            if (value.toLowerCase().equals("true"))
                cp.value = true;
            else if (value.toLowerCase().equals("false"))
                cp.value = false;
            break;
        }
        case SPIN: {
            SpinParam sp = (SpinParam)p;
            try {
                int val = Integer.parseInt(value);
                if ((val >= sp.minValue) && (val <= sp.maxValue))
                    sp.value = val;
            } catch (NumberFormatException ex) {
            }
            break;
        }
        case COMBO: {
            ComboParam cp = (ComboParam)p;
            for (String allowed : cp.allowedValues)
                if (allowed.toLowerCase().equals(value.toLowerCase())) {
                    cp.value = allowed;
                    break;
                }
            break;
        }
        case BUTTON:
            break;
        case STRING: {
            StringParam sp = (StringParam)p;
            sp.value = value;
            break;
        }
        }
    }
}
