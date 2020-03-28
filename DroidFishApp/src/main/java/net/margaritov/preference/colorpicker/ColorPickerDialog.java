/*
 * Copyright (C) 2010 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import org.petero.droidfish.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

public class ColorPickerDialog 
    extends 
        Dialog 
    implements
        ColorPickerView.OnColorChangedListener,
        View.OnClickListener {

    private ColorPickerView mColorPicker;
    private EditText colorCode;
    private ColorPickerPanelView mOldColor;
    private ColorPickerPanelView mNewColor;

    private OnColorChangedListener mListener;

    private CharSequence additionalInfo;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }
    
    public ColorPickerDialog(Context context, int initialColor,
                             CharSequence additionalInfo) {
        super(context);
        this.additionalInfo = additionalInfo;
        init(initialColor);
    }

    private void init(int color) {
        getWindow().setFormat(PixelFormat.RGBA_8888);
        setUp(color, color);
    }

    public void reInitUI() {
        int oldColor = mOldColor.getColor();
        int newColor = mNewColor.getColor();
        setUp(oldColor, newColor);
    }

    private void setUp(int oldColor, int newColor) {
        setContentView(R.layout.dialog_color_picker);

        setTitle(getContext().getText(R.string.prefs_colors_title) + " '"
                + additionalInfo + "'");

        mColorPicker = findViewById(R.id.color_picker_view);
        colorCode = findViewById(R.id.color_code);
        mOldColor = findViewById(R.id.old_color_panel);
        mNewColor = findViewById(R.id.new_color_panel);

        int offs = Math.round(mColorPicker.getDrawingOffset());
        ((LinearLayout) mOldColor.getParent()).setPadding(offs, 0, offs, 0);

        mOldColor.setOnClickListener(this);
        mNewColor.setOnClickListener(this);
        mColorPicker.setOnColorChangedListener(this);
        mOldColor.setColor(oldColor);

        colorCode.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus)
                applyColorCode();
        });
        colorCode.setOnEditorActionListener((v, id, event) -> {
            colorCode.clearFocus();
            String ims = Activity.INPUT_METHOD_SERVICE;
            InputMethodManager imm = (InputMethodManager)getContext().getSystemService(ims);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            return true;
        });

        mColorPicker.setColor(newColor, true);
    }

    @Override
    public void onColorChanged(int color) {
        mNewColor.setColor(color);
        colorCode.setText(String.format("%08x", color));
    }

    private void applyColorCode() {
        String txt = colorCode.getText().toString().trim().toLowerCase();
        if (txt.length() != 8) // Format must be AARRGGBB
            return;
        try {
            long longVal = Long.parseLong(txt, 16);
            int val = (int)longVal;
            if (val != mColorPicker.getColor())
                mColorPicker.setColor(val, true);
        } catch (NumberFormatException ignore) {}
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     */
    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    public int getColor() {
        return mColorPicker.getColor();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.new_color_panel) {
            if (mListener != null)
                mListener.onColorChanged(mNewColor.getColor());
        }
        dismiss();
    }
}
