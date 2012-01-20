package org.petero.droidfish;

/** Endgame tablebase probing options. */
public final class EGTBOptions {
    public boolean hints;       // Hints when playing/analyzing
    public boolean hintsEdit;   // Hints in "edit board" mode
    public boolean rootProbe;   // Only search optimal moves at root
    public boolean engineProbe; // Let engine use EGTB
    public String gtbPath;      // GTB directory path

    public EGTBOptions() {
        hints = false;
        hintsEdit = false;
        rootProbe = false;
        engineProbe = false;
        gtbPath = "";
    }

    public EGTBOptions(EGTBOptions other) {
        hints = other.hints;
        hintsEdit = other.hintsEdit;
        rootProbe = other.rootProbe;
        engineProbe = other.engineProbe;
        gtbPath = other.gtbPath;
    }

    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass()))
            return false;
        EGTBOptions other = (EGTBOptions)o;

        return ((hints == other.hints) &&
                (hintsEdit == other.hintsEdit) &&
                (rootProbe == other.rootProbe) &&
                (engineProbe == other.engineProbe) &&
                gtbPath.equals(other.gtbPath));
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
