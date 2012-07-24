package org.petero.droidfish;

/** Engine options, including endgame tablebase probing options. */
public final class EngineOptions {
    public int hashMB;          // Engine hash table size in MB
    public boolean hints;       // Hints when playing/analyzing
    public boolean hintsEdit;   // Hints in "edit board" mode
    public boolean rootProbe;   // Only search optimal moves at root
    public boolean engineProbe; // Let engine use EGTB
    public String gtbPath;      // GTB directory path

    public EngineOptions() {
        hashMB = 16;
        hints = false;
        hintsEdit = false;
        rootProbe = false;
        engineProbe = false;
        gtbPath = "";
    }

    public EngineOptions(EngineOptions other) {
        hashMB = other.hashMB;
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
        EngineOptions other = (EngineOptions)o;

        return ((hashMB == other.hashMB) &&
                (hints == other.hints) &&
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
