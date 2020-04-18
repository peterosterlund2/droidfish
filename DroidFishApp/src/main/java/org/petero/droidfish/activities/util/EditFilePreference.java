/*
    DroidFish - An Android chess program.
    Copyright (C) 2020  Peter Österlund, peterosterlund2@gmail.com

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

package org.petero.droidfish.activities.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.petero.droidfish.FileUtil;
import org.petero.droidfish.R;
import org.petero.droidfish.activities.Preferences;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/** A text preference representing a file or directory, with a corresponding browse button. */
public class EditFilePreference extends EditTextPreference {
    private boolean pickDirectory = false; // True to pick a directory, false to pick a file
    private String defaultPath = "";   // Default path when current value does not define a path
    private String ignorePattern = ""; // Regexp for values to be treated as non-paths
    private View view;

    public EditFilePreference(Context context) {
        super(context);
        init(null);
    }

    public EditFilePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public EditFilePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            pickDirectory = attrs.getAttributeBooleanValue(null, "pickDirectory", false);
            defaultPath = getStringValue(attrs, "defaultPath");
            ignorePattern = getStringValue(attrs, "ignorePattern");
        }
    }

    private static String getStringValue(AttributeSet attrs, String name) {
        String val = attrs.getAttributeValue(null, name);
        return val == null ? "" : val;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        this.view = view;
        addBrowseButton();
    }

    private void addBrowseButton() {
        if (view == null)
            return;

        LinearLayout widgetFrameView = view.findViewById(android.R.id.widget_frame);
        if (widgetFrameView == null)
            return;
        widgetFrameView.setVisibility(View.VISIBLE);
        int count = widgetFrameView.getChildCount();
        if (count > 0)
            widgetFrameView.removeViews(0, count);

        ImageView button = new ImageView(getContext());
        widgetFrameView.addView(button);
        widgetFrameView.setMinimumWidth(0);

        boolean hasBrowser = FileBrowseUtil.hasBrowser(getContext().getPackageManager(),
                                                       pickDirectory);
        FileBrowseUtil.setBrowseImage(getContext().getResources(), button, hasBrowser);
        button.setOnClickListener(view -> browseFile());
    }

    private void browseFile() {
        String currentPath = getText();
        if (matchPattern(currentPath))
            currentPath = "";
        String sep = File.separator;
        if (currentPath.isEmpty() || !currentPath.contains(sep)) {
            String extDir = Environment.getExternalStorageDirectory().getAbsolutePath();
            String newPath = extDir + sep + defaultPath;
            if (!currentPath.isEmpty())
                newPath += sep + currentPath;
            currentPath = newPath;
        }

        String title = getContext().getString(pickDirectory ? R.string.select_directory
                                                            : R.string.select_file);
        Intent i = new Intent(FileBrowseUtil.getPickAction(pickDirectory));
        i.setData(Uri.fromFile(new File(currentPath)));
        i.putExtra("org.openintents.extra.TITLE", title);
        try {
            Context context = getContext();
            if (context instanceof Preferences) {
                Preferences prefs = ((Preferences)context);
                prefs.runActivity(i, (resultCode, data) -> {
                    if (resultCode == Activity.RESULT_OK) {
                        String pathName = FileUtil.getFilePathFromUri(data.getData());
                        if (pathName != null)
                            setText(pathName);
                    }
                });
            }
        } catch (ActivityNotFoundException ignore) {
        }
    }

    private boolean matchPattern(String s) {
        if (ignorePattern.isEmpty())
            return false;
        try {
            Pattern p = Pattern.compile(ignorePattern);
            return p.matcher(s).find();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }
}
