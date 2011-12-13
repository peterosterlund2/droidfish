/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.activities;

import org.petero.droidfish.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;

public class CPUWarning extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showDialog(CPU_WARNING_DIALOG);
    }

    static final int CPU_WARNING_DIALOG = 1;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case CPU_WARNING_DIALOG:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name).setMessage(R.string.cpu_warning);
            AlertDialog alert = builder.create();
            alert.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });
            return alert;
        }
        return null;
    }
}
