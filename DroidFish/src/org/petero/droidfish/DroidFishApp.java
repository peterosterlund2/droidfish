package org.petero.droidfish;

import android.app.Application;
import android.content.Context;

public class DroidFishApp extends Application {
    private static Context appContext;
    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    /** Get the application context. */
    public static Context getContext() {
        return appContext;
    }
}
