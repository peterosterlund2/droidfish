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
import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.AbsListView;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import org.petero.droidfish.ColorTheme;
import org.petero.droidfish.DroidFish;
import org.petero.droidfish.DroidFishApp;
import org.petero.droidfish.ObjectCache;
import org.petero.droidfish.R;
import org.petero.droidfish.Util;
import org.petero.droidfish.activities.util.PGNFile.GameInfo;
import org.petero.droidfish.activities.util.GameAdapter;
import org.petero.droidfish.activities.util.PGNFile;
import org.petero.droidfish.databinding.SelectGameBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public abstract class EditPGN extends AppCompatActivity {
    private static ArrayList<GameInfo> gamesInFile = new ArrayList<>();
    private static boolean cacheValid = false;
    private PGNFile pgnFile;
    private ProgressDialog progress;

    private GameInfo selectedGi = null;
    private GameAdapter<GameInfo> aa = null;

    private SharedPreferences settings;
    private long defaultFilePos = 0;
    private boolean updateDefaultFilePos;
    private long currentFilePos = 0;
    private String lastSearchString = "";
    private String lastFileName = "";
    private long lastModTime = -1;
    private boolean useRegExp = false;
    private boolean backup = false; // If true, backup PGN games before overwriting

    private Thread workThread = null;
    private boolean canceled = false;

    private boolean loadGame; // True when loading game, false when saving
    private String pgnToSave;

    private SelectGameBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        Util.setFullScreenMode(this, settings);

        if (savedInstanceState != null) {
            defaultFilePos = savedInstanceState.getLong("defaultFilePos");
            lastSearchString = savedInstanceState.getString("lastSearchString");
            if (lastSearchString == null) lastSearchString = "";
            lastFileName = savedInstanceState.getString("lastFileName");
            if (lastFileName == null) lastFileName = "";
            lastModTime = savedInstanceState.getLong("lastModTime");
            useRegExp = savedInstanceState.getBoolean("useRegExpSearch");
        } else {
            defaultFilePos = settings.getLong("defaultFilePos", 0);
            lastSearchString = settings.getString("lastSearchString", "");
            lastFileName = settings.getString("lastFileName", "");
            lastModTime = settings.getLong("lastModTime", 0);
            useRegExp = settings.getBoolean("useRegExpSearch", false);
        }

        Intent i = getIntent();
        String action = i.getAction();
        String fileName = i.getStringExtra("org.petero.droidfish.pathname");
        backup = i.getBooleanExtra("org.petero.droidfish.backup", false);
        updateDefaultFilePos = i.getBooleanExtra("org.petero.droidfish.updateDefFilePos", true);
        canceled = false;
        if ("org.petero.droidfish.loadFile".equals(action)) {
            pgnFile = new PGNFile(fileName);
            loadGame = true;
            showDialog(PROGRESS_DIALOG);
            workThread = new Thread(() -> {
                if (!readFile())
                    return;
                runOnUiThread(() -> {
                    if (canceled) {
                        setResult(RESULT_CANCELED);
                        finish();
                    } else {
                        showList();
                    }
                });
            });
            workThread.start();
        } else if ("org.petero.droidfish.loadFileNextGame".equals(action) ||
                   "org.petero.droidfish.loadFilePrevGame".equals(action)) {
            pgnFile = new PGNFile(fileName);
            loadGame = true;
            boolean next = action.equals("org.petero.droidfish.loadFileNextGame");
            workThread = new Thread(() -> {
                if (!readFile())
                    return;
                GameAdapter.ItemMatcher<GameInfo> m =
                    GameAdapter.getItemMatcher(lastSearchString, useRegExp);
                int itemNo = getItemNo(gamesInFile, defaultFilePos) + (next ? 1 : -1);
                if (next) {
                    while (itemNo < gamesInFile.size() && !m.matches(gamesInFile.get(itemNo)))
                        itemNo++;
                } else {
                    while (itemNo >= 0 && !m.matches(gamesInFile.get(itemNo)))
                        itemNo--;
                }
                final int loadItem = itemNo;
                runOnUiThread(() -> {
                    if (loadItem < 0) {
                        DroidFishApp.toast(R.string.no_prev_game, Toast.LENGTH_SHORT);
                        setResult(RESULT_CANCELED);
                        finish();
                    } else if (loadItem >= gamesInFile.size()) {
                        DroidFishApp.toast(R.string.no_next_game, Toast.LENGTH_SHORT);
                        setResult(RESULT_CANCELED);
                        finish();
                    } else {
                        GameInfo gi = gamesInFile.get(loadItem);
                        setDefaultFilePos(gi.startPos);
                        sendBackResult(gi);
                    }
                });
            });
            workThread.start();
        } else if ("org.petero.droidfish.saveFile".equals(action)) {
            loadGame = false;
            String token = i.getStringExtra("org.petero.droidfish.pgn");
            pgnToSave = (new ObjectCache()).retrieveString(token);
            pgnFile = new PGNFile(fileName);
            showDialog(PROGRESS_DIALOG);
            workThread = new Thread(() -> {
                if (!readFile())
                    return;
                runOnUiThread(() -> {
                    if (canceled) {
                        setResult(RESULT_CANCELED);
                        finish();
                    } else if (gamesInFile.isEmpty()) {
                        pgnFile.appendPGN(pgnToSave, false);
                        saveFileFinished();
                    } else {
                        showList();
                    }
                });
            });
            workThread.start();
        } else { // Unsupported action
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void saveFileFinished() {
        Intent i = new Intent();
        i.putExtra("org.petero.droidfish.treeHash", Util.stringHash(pgnToSave));
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(DroidFishApp.setLanguage(newBase, false));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("defaultFilePos", defaultFilePos);
        outState.putString("lastSearchString", lastSearchString);
        outState.putString("lastFileName", lastFileName);
        outState.putLong("lastModTime", lastModTime);
        outState.putBoolean("useRegExpSearch", useRegExp);
    }

    @Override
    protected void onPause() {
        Editor editor = settings.edit();
        editor.putLong("defaultFilePos", defaultFilePos);
        editor.putString("lastSearchString", lastSearchString);
        editor.putString("lastFileName", lastFileName);
        editor.putLong("lastModTime", lastModTime);
        editor.putBoolean("useRegExpSearch", useRegExp);
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
        MenuItem item = menu.findItem(R.id.regexp_search);
        item.setChecked(useRegExp);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.item_delete_file:
            reShowDialog(DELETE_PGN_FILE_DIALOG);
            break;
        case R.id.regexp_search:
            useRegExp = !useRegExp;
            item.setChecked(useRegExp);
            if (binding != null) {
                String s = binding.selectGameFilter.getText().toString();
                setFilterString(s);
            }
            break;
        }
        return false;
    }

    private void showList() {
        progress = null;
        removeDialog(PROGRESS_DIALOG);
        binding = DataBindingUtil.setContentView(this, R.layout.select_game);
        Util.overrideViewAttribs(findViewById(android.R.id.content));
        createAdapter();
        ListView lv = binding.listView;
        currentFilePos = defaultFilePos;
        int itemNo = getItemNo(gamesInFile, defaultFilePos);
        if (itemNo >= 0)
            lv.setSelectionFromTop(itemNo, 0);
        lv.setFastScrollEnabled(true);
        lv.setOnItemClickListener((parent, view, pos, id) -> {
            selectedGi = aa.getItem(pos);
            if (selectedGi == null)
                return;
            if (loadGame) {
                setDefaultFilePos(selectedGi.startPos);
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
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (visibleItemCount > 0)
                    currentFilePos = aa.getItem(firstVisibleItem).startPos;
            }
        });

        binding.selectGameFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String fs = s.toString();
                setFilterString(fs);
                lastSearchString = fs;
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
                doBackup(giToReplace);
                pgnFile.replacePGN(pgnToSave, giToReplace, false);
                saveFileFinished();
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

    private void setDefaultFilePos(long pos) {
        if (updateDefaultFilePos)
            defaultFilePos = pos;
    }

    private boolean readFile() {
        String fileName = pgnFile.getName();
        if (!fileName.equals(lastFileName))
            setDefaultFilePos(0);
        long modTime = new File(fileName).lastModified();
        if (cacheValid && (modTime == lastModTime) && fileName.equals(lastFileName))
            return true;
        try {
            gamesInFile = pgnFile.getGameInfo(this, progress);
            if (updateDefaultFilePos) {
                cacheValid = true;
                lastModTime = modTime;
                lastFileName = fileName;
            } else {
                cacheValid = false;
            }
            return true;
        } catch (PGNFile.CancelException ignore) {
        } catch (PGNFile.NotPgnFile ex) {
            runOnUiThread(() -> DroidFishApp.toast(R.string.not_a_pgn_file,
                                                   Toast.LENGTH_SHORT));
        } catch (FileNotFoundException ex) {
            if (!loadGame) {
                gamesInFile = new ArrayList<>();
                return true;
            }
            runOnUiThread(() -> DroidFishApp.toast(ex.getMessage(),
                                                   Toast.LENGTH_LONG));
        } catch (IOException ex) {
            runOnUiThread(() -> DroidFishApp.toast(ex.getMessage(),
                                                   Toast.LENGTH_LONG));
        } catch (OutOfMemoryError ex) {
            runOnUiThread(() -> DroidFishApp.toast(R.string.file_too_large,
                                                   Toast.LENGTH_SHORT));
        }
        setResult(RESULT_CANCELED);
        finish();
        return false;
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
        doBackup(gi);
        if (pgnFile.deleteGame(gi, gamesInFile)) {
            createAdapter();
            String s = binding.selectGameFilter.getText().toString();
            setFilterString(s);
            // Update lastModTime, since current change has already been handled
            String fileName = pgnFile.getName();
            lastModTime = new File(fileName).lastModified();
        }
    }

    private void doBackup(GameInfo gi) {
        if (!backup)
            return;
        String pgn = pgnFile.readOneGame(gi);
        if (pgn == null || pgn.isEmpty())
            return;
        DroidFish.autoSaveGame(pgn);
    }

    private void createAdapter() {
        aa = new GameAdapter<GameInfo>(this, R.layout.select_game_list_item, gamesInFile) {
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
        binding.listView.setAdapter(aa);
    }

    private void setFilterString(String s) {
        boolean regExp = useRegExp;
        Filter.FilterListener listener = (count) -> {
            ArrayList<GameInfo> arr = aa.getValues();
            int itemNo = getItemNo(arr, currentFilePos);
            if (itemNo < 0)
                itemNo = 0;
            GameAdapter.ItemMatcher<GameInfo> m =
                GameAdapter.getItemMatcher(lastSearchString, regExp);
            while (itemNo < arr.size() && !m.matches(arr.get(itemNo)))
                itemNo++;
            if (itemNo < arr.size())
                binding.listView.setSelectionFromTop(itemNo, 0);
        };
        aa.setUseRegExp(regExp);
        aa.getFilter().filter(s, listener);
    }

    /** Return index in "games" corresponding to a file position. */
    private static int getItemNo(ArrayList<GameInfo> games, long filePos) {
        int lo = -1;
        int hi = games.size();
        // games[lo].startPos <= filePos < games[hi].startPos
        while (hi - lo > 1) {
            int mid = (lo + hi) / 2;
            long val = games.get(mid).startPos;
            if (filePos < val)
                hi = mid;
            else
                lo = mid;
        }
        return lo;
    }
}
