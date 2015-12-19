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

package org.petero.droidfish;

import java.util.Locale;

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
        TextView name = new TextView(getContext());
        name.setText(getTitle());
        name.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
        name.setGravity(Gravity.LEFT);
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                          LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.LEFT;
        lp.weight  = 1.0f;
        name.setLayoutParams(lp);

        currValBox = new TextView(getContext());
        currValBox.setTextSize(12);
        currValBox.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
        currValBox.setPadding(2, 5, 0, 0);
        currValBox.setText(valToString());
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                           LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        currValBox.setLayoutParams(lp);

        LinearLayout row1 = new LinearLayout(getContext());
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(name);
        row1.addView(currValBox);

        final SeekBar bar = new SeekBar(getContext());
        bar.setMax(maxValue);
        bar.setProgress(currVal);
        bar.setOnSeekBarChangeListener(this);
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                           LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.RIGHT;
        bar.setLayoutParams(lp);

        CharSequence summaryCharSeq = getSummary();
        boolean haveSummary = (summaryCharSeq != null) && (summaryCharSeq.length() > 0);
        TextView summary = null;
        if (haveSummary) {
            summary = new TextView(getContext());
            summary.setText(getSummary());
//            summary.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
            summary.setGravity(Gravity.LEFT);
            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                               LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.LEFT;
            lp.weight  = 1.0f;
            summary.setLayoutParams(lp);
        }

        LinearLayout layout = new LinearLayout(getContext());
        layout.setPadding(25, 5, 25, 5);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(row1);
        layout.addView(bar);
        if (summary != null)
            layout.addView(summary);
        layout.setId(android.R.id.widget_frame);

        currValBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
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
                final EditText valueView = (EditText)content.findViewById(R.id.selpercentage_number);
                valueView.setText(currValBox.getText().toString().replaceAll("%", "").replaceAll(",", "."));
                final Runnable selectValue = new Runnable() {
                    public void run() {
                        try {
                            String txt = valueView.getText().toString();
                            int value = (int)(Double.parseDouble(txt) * 10 + 0.5);
                            if (value < 0) value = 0;
                            if (value > maxValue) value = maxValue;
                            onProgressChanged(bar, value, false);
                        } catch (NumberFormatException nfe) {
                        }
                    }
                };
                valueView.setOnKeyListener(new OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            selectValue.run();
                            return true;
                        }
                        return false;
                    }
                });
                builder.setPositiveButton("Ok", new Dialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        selectValue.run();
                    }
                });
                builder.setNegativeButton("Cancel", null);

                builder.create().show();
            }
        });

        return layout;
    }

    private final String valToString() {
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
        editor.commit();
        if ((progress == 0) && showStrengthHint) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            String engine = settings.getString("engine", "stockfish");
            if ("stockfish".equals(engine)) {
                showStrengthHint = false;
                if (getKey().equals("strength"))
                    Toast.makeText(getContext(), R.string.strength_cuckoo_hint,
                                   Toast.LENGTH_LONG).show();
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
