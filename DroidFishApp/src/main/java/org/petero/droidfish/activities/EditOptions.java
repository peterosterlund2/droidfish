/*
    DroidFish - An Android chess program.
    Copyright (C) 2014  Peter Österlund, peterosterlund2@gmail.com

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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import androidx.databinding.DataBindingUtil;

import org.petero.droidfish.DroidFishApp;
import org.petero.droidfish.FileUtil;
import org.petero.droidfish.R;
import org.petero.droidfish.SVGPictureDrawable;
import org.petero.droidfish.Util;
import org.petero.droidfish.databinding.EditoptionsBinding;
import org.petero.droidfish.databinding.UciOptionButtonBinding;
import org.petero.droidfish.databinding.UciOptionCheckBinding;
import org.petero.droidfish.databinding.UciOptionComboBinding;
import org.petero.droidfish.databinding.UciOptionSpinBinding;
import org.petero.droidfish.databinding.UciOptionStringBinding;
import org.petero.droidfish.engine.UCIOptions;

import java.io.File;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Edit UCI options.
 */
public class EditOptions extends Activity {
    private UCIOptions uciOpts = null;
    private String engineName = "";
    private String workDir = "";
    private boolean hasBrowser = false; // True if OI file manager available

    private UCIOptions.StringOption currentStringOption; // Option that triggered file browsing
    private EditText currentTextField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Util.setFullScreenMode(this, settings);

        Intent i = getIntent();
        uciOpts = (UCIOptions) i.getSerializableExtra("org.petero.droidfish.ucioptions");
        engineName = (String) i.getSerializableExtra("org.petero.droidfish.enginename");
        workDir = (String) i.getSerializableExtra("org.petero.droidfish.workDir");
        hasBrowser = (Boolean) i.getSerializableExtra("org.petero.droidfish.localEngine");
        if (uciOpts != null) {
            if (hasBrowser) {
                Intent browser = new Intent("org.openintents.action.PICK_FILE");
                hasBrowser = browser.resolveActivity(getPackageManager()) != null;
            }
            initUI();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(DroidFishApp.setLanguage(newBase, false));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initUI();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            sendBackResult();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initUI() {
        String title = getString(R.string.edit_options_title);
        if (engineName != null)
            title = title + ": " + engineName;
        setTitle(title);

        EditoptionsBinding binding = DataBindingUtil.setContentView(this, R.layout.editoptions);

        if (uciOpts != null) {
            for (String name : uciOpts.getOptionNames()) {
                UCIOptions.OptionBase o = uciOpts.getOption(name);
                if (o.visible) {
                    View v = getViewForOption(o);
                    if (v != null)
                        binding.eoContent.addView(v);
                }
            }
        }

        Util.overrideViewAttribs(binding.eoContent);

        binding.eoOk.setOnClickListener(v -> sendBackResult());
        binding.eoCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        binding.eoReset.setOnClickListener(v -> {
            if (uciOpts != null) {
                boolean modified = false;
                for (String name : uciOpts.getOptionNames()) {
                    UCIOptions.OptionBase o = uciOpts.getOption(name);
                    if (!o.visible)
                        continue;
                    switch (o.type) {
                    case CHECK: {
                        UCIOptions.CheckOption co = (UCIOptions.CheckOption) o;
                        modified |= co.set(co.defaultValue);
                        break;
                    }
                    case SPIN: {
                        UCIOptions.SpinOption so = (UCIOptions.SpinOption) o;
                        modified |= so.set(so.defaultValue);
                        break;
                    }
                    case COMBO: {
                        UCIOptions.ComboOption co = (UCIOptions.ComboOption) o;
                        modified |= co.set(co.defaultValue);
                        break;
                    }
                    case STRING: {
                        UCIOptions.StringOption so = (UCIOptions.StringOption) o;
                        modified |=  so.set(so.defaultValue);
                        break;
                    }
                    case BUTTON:
                        break;
                    }
                }
                if (modified)
                    initUI();
            }
        });
    }

    private View getViewForOption(UCIOptions.OptionBase o) {
        switch (o.type) {
        case CHECK: {
            UciOptionCheckBinding holder = UciOptionCheckBinding.inflate(getLayoutInflater(), null, false);
            holder.eoValue.setText(o.name);
            final UCIOptions.CheckOption co = (UCIOptions.CheckOption) o;
            holder.eoValue.setChecked(co.value);
            holder.eoValue.setOnCheckedChangeListener((buttonView, isChecked) -> co.set(isChecked));
            return holder.getRoot();
        }
        case SPIN: {
            UciOptionSpinBinding holder = UciOptionSpinBinding.inflate(getLayoutInflater(), null, false);
            final UCIOptions.SpinOption so = (UCIOptions.SpinOption) o;
            String labelText = String.format(Locale.US, "%s (%d\u2013%d)", so.name, so.minValue, so.maxValue);
            holder.eoLabel.setText(labelText);
            holder.eoValue.setText(so.getStringValue());
            if (so.minValue >= 0)
                holder.eoValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            holder.eoValue.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int newVal = Integer.parseInt(s.toString());
                        if (newVal < so.minValue)
                            so.set(so.minValue);
                        else if (newVal > so.maxValue)
                            so.set(so.maxValue);
                        else
                            so.set(newVal);
                    } catch (NumberFormatException ignore) {
                    }
                }
            });
            return holder.getRoot();
        }
        case COMBO: {
            UciOptionComboBinding holder = UciOptionComboBinding.inflate(getLayoutInflater(), null, false);
            holder.eoLabel.setText(o.name);
            final UCIOptions.ComboOption co = (UCIOptions.ComboOption) o;
            ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, co.allowedValues);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.eoValue.setAdapter(adapter);
            holder.eoValue.setSelection(adapter.getPosition(co.value));
            holder.eoValue.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> av, View view, int position, long id) {
                    if ((position >= 0) && (position < co.allowedValues.length))
                        co.set(co.allowedValues[position]);
                }

                public void onNothingSelected(AdapterView<?> arg0) { }
            });
            return holder.getRoot();
        }
        case BUTTON: {
            UciOptionButtonBinding holder = UciOptionButtonBinding.inflate(getLayoutInflater(), null, false);
            final UCIOptions.ButtonOption bo = (UCIOptions.ButtonOption) o;
            bo.trigger = false;
            holder.eoLabel.setText(o.name);
            holder.eoLabel.setTextOn(o.name);
            holder.eoLabel.setTextOff(o.name);
            holder.eoLabel.setOnCheckedChangeListener((buttonView, isChecked) -> bo.trigger = isChecked);
            return holder.getRoot();
        }
        case STRING: {
            UciOptionStringBinding holder = UciOptionStringBinding.inflate(getLayoutInflater(), null, false);
            holder.eoLabel.setText(String.format("%s ", o.name));
            final UCIOptions.StringOption so = (UCIOptions.StringOption) o;
            holder.eoValue.setText(so.value);
            holder.eoValue.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) { }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override
                public void afterTextChanged(Editable s) {
                    so.set(s.toString());
                }
            });
            boolean isFileOption = hasBrowser && (o.name.toLowerCase().contains("file") ||
                                                  o.name.toLowerCase().contains("path"));
            setBrowseImage(holder.eoBrowse, isFileOption);
            holder.eoBrowse.setOnClickListener(view -> {
                browseFile(so, holder.eoValue);
            });
            return holder.getRoot();
        }
        default:
            return null;
        }
    }

    private void setBrowseImage(ImageButton button, boolean visible) {
        button.setVisibility(visible ? View.VISIBLE : View.GONE);

        Resources r = getResources();
        try {
            SVG svg = SVG.getFromResource(r, R.raw.open_last_file);
            button.setBackgroundDrawable(new SVGPictureDrawable(svg));
        } catch (SVGParseException ignore) {
        }

        try {
            SVG touched = SVG.getFromResource(r, R.raw.touch);
            StateListDrawable sld = new StateListDrawable();
            sld.addState(new int[]{android.R.attr.state_pressed}, new SVGPictureDrawable(touched));
            button.setImageDrawable(sld);
        } catch (SVGParseException ignore) {
        }

        int bWidth  = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                           36, r.getDisplayMetrics()));
        int bHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                           32, r.getDisplayMetrics()));
        ViewGroup.LayoutParams lp = button.getLayoutParams();
        lp.width = bWidth;
        lp.height = bHeight;
        button.setLayoutParams(lp);
        button.setPadding(0,0,0,0);
        button.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    private void browseFile(UCIOptions.StringOption so, EditText textField) {
        String currentFile = so.getStringValue();
        String sep = File.separator;
        if (!currentFile.contains(sep))
            currentFile = workDir + sep + currentFile;
        Intent i = new Intent("org.openintents.action.PICK_FILE");
        i.setData(Uri.fromFile(new File(currentFile)));
        i.putExtra("org.openintents.extra.TITLE", getString(R.string.select_file));
        try {
            startActivityForResult(i, RESULT_OI_SELECT_FILE);
            currentStringOption = so;
            currentTextField = textField;
        } catch (ActivityNotFoundException ignore) {
        }
    }

    static private final int RESULT_OI_SELECT_FILE = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case RESULT_OI_SELECT_FILE:
            if (resultCode == RESULT_OK && currentStringOption != null) {
                String pathName = FileUtil.getFilePathFromUri(data.getData());
                if (pathName != null && currentTextField != null) {
                    if (currentStringOption.set(pathName))
                        currentTextField.setText(pathName);
                }
            }
            currentStringOption = null;
            currentTextField = null;
            break;
        }
    }

    private void sendBackResult() {
        if (uciOpts != null) {
            TreeMap<String, String> uciMap = new TreeMap<>();
            for (String name : uciOpts.getOptionNames()) {
                UCIOptions.OptionBase o = uciOpts.getOption(name);
                if (o != null) {
                    if (o instanceof UCIOptions.ButtonOption) {
                        UCIOptions.ButtonOption bo = (UCIOptions.ButtonOption) o;
                        if (bo.trigger)
                            uciMap.put(name, "");
                    } else {
                        uciMap.put(name, o.getStringValue());
                    }
                }
            }
            Intent i = new Intent();
            i.putExtra("org.petero.droidfish.ucioptions", uciMap);
            setResult(RESULT_OK, i);
            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
