/*
    DroidFish - An Android chess program.
    Copyright (C) 2016  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

public class DroidFishApp extends Application {
    private static Context appContext;
    private static Toast toast;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    /** Get the application context. */
    public static Context getContext() {
        return appContext;
    }

    /** Show a toast after canceling current toast. */
    public static void toast(int resId, int duration) {
        if (toast != null) {
            toast.cancel();
            toast = null;
        }
        if (appContext != null) {
            toast = Toast.makeText(appContext, resId, duration);
            toast.show();
        }
    }

    /** Show a toast after canceling current toast. */
    public static void toast(CharSequence text, int duration) {
        if (toast != null) {
            toast.cancel();
            toast = null;
        }
        if (appContext != null) {
            toast = Toast.makeText(appContext, text, duration);
            toast.show();
        }
    }
}
