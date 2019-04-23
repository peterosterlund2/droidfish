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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import org.petero.droidfish.ColorTheme;
import org.petero.droidfish.DroidFishApp;
import org.petero.droidfish.ObjectCache;
import org.petero.droidfish.R;
import org.petero.droidfish.Util;
import org.petero.droidfish.activities.PGNFile.GameInfo;
import org.petero.droidfish.activities.PGNFile.GameInfoResult;
import org.petero.droidfish.databinding.SelectGameBinding;
import org.petero.droidfish.gamelogic.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public abstract class EditPGN extends ListActivity {
    static ArrayList<GameInfo> gamesInFile = new ArrayList<>();
    static boolean cacheValid = false;
    PGNFile pgnFile;
    ProgressDialog progress;
    private GameInfo selectedGi = null;
    ArrayAdapter<GameInfo> aa = null;

    SharedPreferences settings;
    int defaultItem = 0;
    String lastSearchString = "";
    String lastFileName = "";
    long lastModTime = -1;

    Thread workThread = null;
    boolean canceled = false;

    boolean loadGame; // True when loading game, false when saving
    String pgnToSave;

    private SelectGameBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        Util.setFullScreenMode(this, settings);

        if (savedInstanceState != null) {
            defaultItem = savedInstanceState.getInt("defaultItem");
            lastSearchString = savedInstanceState.getString("lastSearchString");
            if (lastSearchString == null) lastSearchString = "";
            lastFileName = savedInstanceState.getString("lastFileName");
            if (lastFileName == null) lastFileName = "";
            lastModTime = savedInstanceState.getLong("lastModTime");
        } else {
            defaultItem = settings.getInt("defaultItem", 0);
            lastSearchString = settings.getString("lastSearchString", "");
            lastFileName = settings.getString("lastFileName", "");
            lastModTime = settings.getLong("lastModTime", 0);
        }

        Intent i = getIntent();
        String action = i.getAction();
        String fileName = i.getStringExtra("org.petero.droidfish.pathname");
        canceled = false;
        if ("org.petero.droidfish.loadFile".equals(action)) {
            pgnFile = new PGNFile(fileName);
            loadGame = true;
            showDialog(PROGRESS_DIALOG);
            final EditPGN lpgn = this;
            workThread = new Thread(() -> {
                if (!readFile())
                    return;
                runOnUiThread(() -> {
                    if (canceled) {
                        setResult(RESULT_CANCELED);
                        finish();
                    } else {
                        lpgn.showList();
                    }
                });
            });
            workThread.start();
        } else if ("org.petero.droidfish.loadFileNextGame".equals(action) ||
                "org.petero.droidfish.loadFilePrevGame".equals(action)) {
            pgnFile = new PGNFile(fileName);
            loadGame = true;
            boolean next = action.equals("org.petero.droidfish.loadFileNextGame");
            final int loadItem = defaultItem + (next ? 1 : -1);
            if (loadItem < 0) {
                DroidFishApp.toast(R.string.no_prev_game, Toast.LENGTH_SHORT);
                setResult(RESULT_CANCELED);
                finish();
            } else {
                workThread = new Thread(() -> {
                    if (!readFile())
                        return;
                    runOnUiThread(() -> {
                        if (loadItem >= gamesInFile.size()) {
                            DroidFishApp.toast(R.string.no_next_game, Toast.LENGTH_SHORT);
                            setResult(RESULT_CANCELED);
                            finish();
                        } else {
                            defaultItem = loadItem;
                            sendBackResult(gamesInFile.get(loadItem));
                        }
                    });
                });
                workThread.start();
            }
        } else if ("org.petero.droidfish.saveFile".equals(action)) {
            loadGame = false;
            String token = i.getStringExtra("org.petero.droidfish.pgn");
            pgnToSave = (new ObjectCache()).retrieveString(token);
            boolean silent = i.getBooleanExtra("org.petero.droidfish.silent", false);
            if (silent) { // Silently append to file
                PGNFile pgnFile2 = new PGNFile(fileName);
                pgnFile2.appendPGN(pgnToSave);
            } else {
                pgnFile = new PGNFile(fileName);
                showDialog(PROGRESS_DIALOG);
                final EditPGN lpgn = this;
                workThread = new Thread(() -> {
                    if (!readFile())
                        return;
                    runOnUiThread(() -> {
                        if (canceled) {
                            setResult(RESULT_CANCELED);
                            finish();
                        } else if (gamesInFile.size() == 0) {
                            pgnFile.appendPGN(pgnToSave);
                            finish();
                        } else {
                            lpgn.showList();
                        }
                    });
                });
                workThread.start();
            }
        } else { // Unsupported action
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("defaultItem", defaultItem);
        outState.putString("lastSearchString", lastSearchString);
        outState.putString("lastFileName", lastFileName);
        outState.putLong("lastModTime", lastModTime);
    }

    @Override
    protected void onPause() {
        Editor editor = settings.edit();
        editor.putInt("defaultItem", defaultItem);
        editor.putString("lastSearchString", lastSearchString);
        editor.putString("lastFileName", lastFileName);
        editor.putLong("lastModTime", lastModTime);
        editor.apply();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (workThread != null) {
            workThread.interrupt();
            try {
                workThread.join();
            } catch (InterruptedException ignore) {
            }
            workThread = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_file_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_delete_file:
                reShowDialog(DELETE_PGN_FILE_DIALOG);
                break;
        }
        return false;
    }

    private void showList() {
        progress = null;
        removeDialog(PROGRESS_DIALOG);
        binding = DataBindingUtil.setContentView(this, R.layout.select_game);
        Util.overrideViewAttribs(findViewById(android.R.id.content));
        aa = new ArrayAdapter<GameInfo>(this, R.layout.select_game_list_item, gamesInFile) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    int fg = ColorTheme.instance().getColor(ColorTheme.FONT_FOREGROUND);
                    ((TextView) view).setTextColor(fg);
                }
                return view;
            }
        };
        setListAdapter(aa);
        ListView lv = getListView();
        lv.setSelectionFromTop(defaultItem, 0);
        lv.setFastScrollEnabled(true);
        lv.setOnItemClickListener((parent, view, pos, id) -> {
            selectedGi = aa.getItem(pos);
            if (selectedGi == null)
                return;
            if (loadGame) {
                defaultItem = pos;
                sendBackResult(selectedGi);
            } else {
                reShowDialog(SAVE_GAME_DIALOG);
            }
        });
        lv.setOnItemLongClickListener((parent, view, pos, id) -> {
            selectedGi = aa.getItem(pos);
            if (selectedGi != null && !selectedGi.isNull())
                reShowDialog(DELETE_GAME_DIALOG);
            return true;
        });

        binding.selectGameFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aa.getFilter().filter(s);
                lastSearchString = s.toString();
            }
        });
        binding.selectGameFilter.setText(lastSearchString);
        if (loadGame) {
            binding.selectGameHint.setVisibility(View.GONE);
        } else {
            binding.selectGameHint.setText(R.string.save_game_hint);
        }
        lv.requestFocus();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    final static int PROGRESS_DIALOG = 0;
    final static int DELETE_GAME_DIALOG = 1;
    final static int SAVE_GAME_DIALOG = 2;
    final static int DELETE_PGN_FILE_DIALOG = 3;

    /**
     * Remove and show a dialog.
     */
    private void reShowDialog(int id) {
        removeDialog(id);
        showDialog(id);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                progress = new ProgressDialog(this);
                progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progress.setTitle(R.string.reading_pgn_file);
                progress.setOnCancelListener(dialog -> {
                    canceled = true;
                    Thread thr = workThread;
                    if (thr != null)
                        thr.interrupt();
                });
                return progress;
            case DELETE_GAME_DIALOG: {
                final GameInfo gi = selectedGi;
                selectedGi = null;
                if (gi == null)
                    return null;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.delete_game);
                String msg = gi.toString();
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.yes, (dialog, id14) -> {
                    deleteGame(gi);
                    dialog.cancel();
                });
                builder.setNegativeButton(R.string.no, (dialog, id13) -> dialog.cancel());
                return builder.create();
            }
            case SAVE_GAME_DIALOG: {
                final GameInfo gi = selectedGi;
                selectedGi = null;
                if (gi == null)
                    return null;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.save_game_question);
                final CharSequence[] items = {
                        getString(R.string.before_selected),
                        getString(R.string.after_selected),
                        getString(R.string.replace_selected),
                };
                builder.setItems(items, (dialog, item) -> {
                    GameInfo giToReplace;
                    switch (item) {
                        case 0:
                            giToReplace = new GameInfo().setNull(gi.startPos);
                            break;
                        case 1:
                            giToReplace = new GameInfo().setNull(gi.endPos);
                            break;
                        case 2:
                            giToReplace = gi;
                            break;
                        default:
                            finish();
                            return;
                    }
                    pgnFile.replacePGN(pgnToSave, giToReplace);
                    finish();
                });
                return builder.create();
            }
            case DELETE_PGN_FILE_DIALOG: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.delete_file_question);
                String name = new File(pgnFile.getName()).getName();
                String msg = String.format(Locale.US, getString(R.string.delete_named_file), name);
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.yes, (dialog, id12) -> {
                    pgnFile.delete();
                    finish();
                });
                builder.setNegativeButton(R.string.no, (dialog, id1) -> dialog.cancel());
                return builder.create();
            }
            default:
                return null;
        }
    }

    private boolean readFile() {
        String fileName = pgnFile.getName();
        if (!fileName.equals(lastFileName))
            defaultItem = 0;
        long modTime = new File(fileName).lastModified();
        if (cacheValid && (modTime == lastModTime) && fileName.equals(lastFileName))
            return true;
        Pair<GameInfoResult, ArrayList<GameInfo>> p = pgnFile.getGameInfo(this, progress);
        if (p.first != GameInfoResult.OK) {
            gamesInFile = new ArrayList<>();
            switch (p.first) {
                case OUT_OF_MEMORY:
                    runOnUiThread(() -> DroidFishApp.toast(R.string.file_too_large, Toast.LENGTH_SHORT));
                    break;
                case NOT_PGN:
                    runOnUiThread(() -> DroidFishApp.toast(R.string.not_a_pgn_file, Toast.LENGTH_SHORT));
                    break;
                case CANCEL:
                case OK:
                    break;
            }
            setResult(RESULT_CANCELED);
            finish();
            return false;
        }
        gamesInFile = p.second;
        cacheValid = true;
        lastModTime = modTime;
        lastFileName = fileName;
        return true;
    }

    private void sendBackResult(GameInfo gi) {
        String pgn = pgnFile.readOneGame(gi);
        if (pgn != null) {
            String pgnToken = (new ObjectCache()).storeString(pgn);
            setResult(RESULT_OK, (new Intent()).setAction(pgnToken));
            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void deleteGame(GameInfo gi) {
        if (pgnFile.deleteGame(gi, gamesInFile)) {
            ListView lv = getListView();
            int pos = lv.pointToPosition(0, 0);
            aa = new ArrayAdapter<>(this, R.layout.select_game_list_item, gamesInFile);
            setListAdapter(aa);
            String s = binding.selectGameFilter.getText().toString();
            aa.getFilter().filter(s);
            lv.setSelection(pos);
            // Update lastModTime, since current change has already been handled
            String fileName = pgnFile.getName();
            lastModTime = new File(fileName).lastModified();
        }
    }
}
