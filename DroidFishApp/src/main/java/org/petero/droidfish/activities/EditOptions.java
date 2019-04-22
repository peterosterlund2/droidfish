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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;

import org.petero.droidfish.R;
import org.petero.droidfish.Util;
import org.petero.droidfish.databinding.UciOptionButtonBinding;
import org.petero.droidfish.databinding.UciOptionCheckBinding;
import org.petero.droidfish.databinding.UciOptionComboBinding;
import org.petero.droidfish.databinding.UciOptionSpinBinding;
import org.petero.droidfish.databinding.UciOptionStringBinding;
import org.petero.droidfish.engine.UCIOptions;

import java.util.Locale;
import java.util.TreeMap;

/**
 * Edit UCI options.
 */
public class EditOptions extends Activity {
    private UCIOptions uciOpts = null;
    private String engineName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Util.setFullScreenMode(this, settings);

        Intent i = getIntent();
        uciOpts = (UCIOptions) i.getSerializableExtra("org.petero.droidfish.ucioptions");
        engineName = (String) i.getSerializableExtra("org.petero.droidfish.enginename");
        if (uciOpts != null) {
            initUI();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
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

        View view = View.inflate(this, R.layout.editoptions, null);

        if (uciOpts != null) {
            LinearLayout content = view.findViewById(R.id.eo_content);
            for (String name : uciOpts.getOptionNames()) {
                UCIOptions.OptionBase o = uciOpts.getOption(name);
                if (!o.visible)
                    continue;
                switch (o.type) {
                    case CHECK: {
                        UciOptionCheckBinding binding = UciOptionCheckBinding.inflate(getLayoutInflater(), null, false);
                        binding.eoValue.setText(o.name);
                        final UCIOptions.CheckOption co = (UCIOptions.CheckOption) o;
                        binding.eoValue.setChecked(co.value);
                        binding.eoValue.setOnCheckedChangeListener((buttonView, isChecked) -> co.set(isChecked));
                        content.addView(binding.getRoot());
                        break;
                    }
                    case SPIN: {
                        UciOptionSpinBinding binding = UciOptionSpinBinding.inflate(getLayoutInflater(), null, false);
                        final UCIOptions.SpinOption so = (UCIOptions.SpinOption) o;
                        String labelText = String.format(Locale.US, "%s (%d\u2013%d)", so.name, so.minValue, so.maxValue);
                        binding.eoLabel.setText(labelText);
                        binding.eoValue.setText(so.getStringValue());
                        if (so.minValue >= 0)
                            binding.eoValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                        binding.eoValue.addTextChangedListener(new TextWatcher() {
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                            }

                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            }

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
                        content.addView(binding.getRoot());
                        break;
                    }
                    case COMBO: {
                        UciOptionComboBinding binding = UciOptionComboBinding.inflate(getLayoutInflater(), null, false);
                        binding.eoLabel.setText(o.name);
                        final UCIOptions.ComboOption co = (UCIOptions.ComboOption) o;
                        ArrayAdapter<CharSequence> adapter =
                                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                                        co.allowedValues);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        binding.eoValue.setAdapter(adapter);
                        binding.eoValue.setSelection(adapter.getPosition(co.value));
                        binding.eoValue.setOnItemSelectedListener(new OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> av, View view, int position, long id) {
                                if ((position >= 0) && (position < co.allowedValues.length))
                                    co.set(co.allowedValues[position]);
                            }

                            public void onNothingSelected(AdapterView<?> arg0) {
                            }
                        });
                        content.addView(binding.getRoot());
                        break;
                    }
                    case BUTTON: {
                        UciOptionButtonBinding binding = UciOptionButtonBinding.inflate(getLayoutInflater(), null, false);
                        final UCIOptions.ButtonOption bo = (UCIOptions.ButtonOption) o;
                        bo.trigger = false;
                        binding.eoLabel.setText(o.name);
                        binding.eoLabel.setTextOn(o.name);
                        binding.eoLabel.setTextOff(o.name);
                        binding.eoLabel.setOnCheckedChangeListener((buttonView, isChecked) -> bo.trigger = isChecked);
                        content.addView(binding.getRoot());
                        break;
                    }
                    case STRING: {
                        UciOptionStringBinding binding = UciOptionStringBinding.inflate(getLayoutInflater(), null, false);
                        binding.eoLabel.setText(String.format("%s ", o.name));
                        final UCIOptions.StringOption so = (UCIOptions.StringOption) o;
                        binding.eoValue.setText(so.value);
                        binding.eoValue.addTextChangedListener(new TextWatcher() {
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                            }

                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                so.set(s.toString());
                            }
                        });
                        content.addView(binding.getRoot());
                        break;
                    }
                }
            }
        }

        setContentView(view);
        Util.overrideViewAttribs(findViewById(android.R.id.content));
        Button okButton = findViewById(R.id.eo_ok);
        Button cancelButton = findViewById(R.id.eo_cancel);
        Button resetButton = findViewById(R.id.eo_reset);

        okButton.setOnClickListener(v -> sendBackResult());
        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        resetButton.setOnClickListener(v -> {
            if (uciOpts != null) {
                boolean modified = false;
                for (String name : uciOpts.getOptionNames()) {
                    UCIOptions.OptionBase o = uciOpts.getOption(name);
                    if (!o.visible)
                        continue;
                    switch (o.type) {
                        case CHECK: {
                            UCIOptions.CheckOption co = (UCIOptions.CheckOption) o;
                            if (co.set(co.defaultValue))
                                modified = true;
                            break;
                        }
                        case SPIN: {
                            UCIOptions.SpinOption so = (UCIOptions.SpinOption) o;
                            if (so.set(so.defaultValue))
                                modified = true;
                            break;
                        }
                        case COMBO: {
                            UCIOptions.ComboOption co = (UCIOptions.ComboOption) o;
                            if (co.set(co.defaultValue))
                                modified = true;
                            break;
                        }
                        case STRING: {
                            UCIOptions.StringOption so = (UCIOptions.StringOption) o;
                            if (so.set(so.defaultValue))
                                modified = true;
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
