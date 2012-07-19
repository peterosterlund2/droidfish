package org.petero.droidfish;

import android.os.Build;

public final class Util {
    public final static String boldStart;
    public final static String boldStop;

    static {
        // Using bold face causes crashes in android 4.1, see:
        // http://code.google.com/p/android/issues/detail?id=34872
        final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        if (sdkVersion >= 16) {
            boldStart = "";
            boldStop = "";
        } else {
            boldStart = "<b>";
            boldStop = "</b>";
        }
    }
}
