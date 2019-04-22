/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2012  Peter Österlund, peterosterlund2@gmail.com

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

import java.util.Locale;

import org.petero.droidfish.DroidFishApp;
import org.petero.droidfish.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

/** Lets user enter a percentage value using a seek bar. */
public class SeekBarPreference extends Preference
                               implements OnSeekBarChangeListener {
    private final static int maxValue = 1000;
    private final static int DEFAULT_VALUE = 1000;
    private int currVal = DEFAULT_VALUE;
    private TextView currValBox;
    private boolean showStrengthHint = true;

    public SeekBarPreference(Context context) {
        super(context);
    }
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);

        LinearLayout layout = (LinearLayout)View.inflate(getContext(), R.layout.seekbar_preference, null);
        TextView name = layout.findViewById(R.id.seekbar_title);
        name.setText(getTitle());

        currValBox = layout.findViewById(R.id.seekbar_value);
        currValBox.setText(valToString());

        final SeekBar bar = layout.findViewById(R.id.seekbar_bar);
        bar.setMax(maxValue);
        bar.setProgress(currVal);
        bar.setOnSeekBarChangeListener(this);

        TextView summary = layout.findViewById(R.id.seekbar_summary);
        CharSequence summaryCharSeq = getSummary();
        boolean haveSummary = (summaryCharSeq != null) && (summaryCharSeq.length() > 0);
        if (haveSummary) {
            summary.setText(getSummary());
        } else {
            summary.setVisibility(View.GONE);
        }

        currValBox.setOnClickListener(v -> {
            View content = View.inflate(SeekBarPreference.this.getContext(), R.layout.select_percentage, null);
            final AlertDialog.Builder builder = new AlertDialog.Builder(SeekBarPreference.this.getContext());
            builder.setView(content);
            String title = "";
            String key = getKey();
            if (key.equals("strength")) {
                title = getContext().getString(R.string.edit_strength);
            } else if (key.equals("bookRandom")) {
                title = getContext().getString(R.string.edit_randomization);
            }
            builder.setTitle(title);
            final EditText valueView = content.findViewById(R.id.selpercentage_number);
            valueView.setText(currValBox.getText().toString().replaceAll("%", "").replaceAll(",", "."));
            final Runnable selectValue = () -> {
                try {
                    String txt = valueView.getText().toString();
                    int value = (int) (Double.parseDouble(txt) * 10 + 0.5);
                    if (value < 0) value = 0;
                    if (value > maxValue) value = maxValue;
                    onProgressChanged(bar, value, false);
                } catch (NumberFormatException ignore) {
                }
            };
            valueView.setOnKeyListener((v1, keyCode, event) -> {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    selectValue.run();
                    return true;
                }
                return false;
            });
            builder.setPositiveButton("Ok", (dialog, which) -> selectValue.run());
            builder.setNegativeButton("Cancel", null);

            builder.create().show();
        });

        return layout;
    }

    private String valToString() {
        return String.format(Locale.US, "%.1f%%", currVal*0.1);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
        if (!callChangeListener(progress)) {
            if (currVal != seekBar.getProgress())
                seekBar.setProgress(currVal);
            return;
        }
        if (progress != seekBar.getProgress())
            seekBar.setProgress(progress);
        currVal = progress;
        currValBox.setText(valToString());
        SharedPreferences.Editor editor =  getEditor();
        editor.putInt(getKey(), progress);
        editor.apply();
        if ((progress == 0) && showStrengthHint) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            String engine = settings.getString("engine", "stockfish");
            if ("stockfish".equals(engine)) {
                showStrengthHint = false;
                if (getKey().equals("strength"))
                    DroidFishApp.toast(R.string.strength_cuckoo_hint, Toast.LENGTH_LONG);
            }
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        int defVal = a.getInt(index, DEFAULT_VALUE);
        if (defVal > maxValue) defVal = maxValue;
        if (defVal < 0) defVal = 0;
        return defVal;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        int val = restorePersistedValue ? getPersistedInt(DEFAULT_VALUE) : (Integer)defaultValue;
        if (!restorePersistedValue)
            persistInt(val);
        currVal = val;
    }
}
