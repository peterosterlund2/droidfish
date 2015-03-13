/*
    DroidFish - An Android chess program.
    Copyright (C) 2013  Peter Österlund, peterosterlund2@gmail.com

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

import java.io.File;
import java.util.ArrayList;

import org.petero.droidfish.ChessBoardPlay;
import org.petero.droidfish.ColorTheme;
import org.petero.droidfish.R;
import org.petero.droidfish.Util;
import org.petero.droidfish.activities.FENFile.FenInfo;
import org.petero.droidfish.activities.FENFile.FenInfoResult;
import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class LoadFEN extends ListActivity {
    private static ArrayList<FenInfo> fensInFile = new ArrayList<FenInfo>();
    private static boolean cacheValid = false;
    private FENFile fenFile;
    private ProgressDialog progress;
    private FenInfo selectedFi = null;
    private ArrayAdapter<FenInfo> aa = null;

    private SharedPreferences settings;
    private int defaultItem = 0;
    private String lastFileName = "";
    private long lastModTime = -1;

    private Thread workThread = null;

    private ChessBoardPlay cb;
    private Button okButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        Util.setFullScreenMode(this, settings);

        if (savedInstanceState != null) {
            defaultItem = savedInstanceState.getInt("defaultItem");
            lastFileName = savedInstanceState.getString("lastFenFileName");
            if (lastFileName == null) lastFileName = "";
            lastModTime = savedInstanceState.getLong("lastFenModTime");
        } else {
            defaultItem = settings.getInt("defaultItem", 0);
            lastFileName = settings.getString("lastFenFileName", "");
            lastModTime = settings.getLong("lastFenModTime", 0);
        }

        Intent i = getIntent();
        String action = i.getAction();
        String fileName = i.getStringExtra("org.petero.droidfish.pathname");
        if (action.equals("org.petero.droidfish.loadFen")) {
            fenFile = new FENFile(fileName);
            showDialog(PROGRESS_DIALOG);
            final LoadFEN lfen = this;
            workThread = new Thread(new Runnable() {
                public void run() {
                    if (!readFile())
                        return;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            lfen.showList();
                        }
                    });
                }
            });
            workThread.start();
        } else if (action.equals("org.petero.droidfish.loadNextFen") ||
                   action.equals("org.petero.droidfish.loadPrevFen")) {
            fenFile = new FENFile(fileName);
            boolean next = action.equals("org.petero.droidfish.loadNextFen");
            final int loadItem = defaultItem + (next ? 1 : -1);
            if (loadItem < 0) {
                Toast.makeText(getApplicationContext(), R.string.no_prev_fen,
                               Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            } else {
                workThread = new Thread(new Runnable() {
                    public void run() {
                        if (!readFile())
                            return;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (loadItem >= fensInFile.size()) {
                                    Toast.makeText(getApplicationContext(), R.string.no_next_fen,
                                                   Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_CANCELED);
                                    finish();
                                } else {
                                    defaultItem = loadItem;
                                    sendBackResult(fensInFile.get(loadItem));
                                }
                            }
                        });
                    }
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
        outState.putString("lastFenFileName", lastFileName);
        outState.putLong("lastFenModTime", lastModTime);
    }

    @Override
    protected void onPause() {
        Editor editor = settings.edit();
        editor.putInt("defaultItem", defaultItem);
        editor.putString("lastFenFileName", lastFileName);
        editor.putLong("lastFenModTime", lastModTime);
        editor.commit();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (workThread != null) {
            workThread.interrupt();
            try {
                workThread.join();
            } catch (InterruptedException e) {
            }
            workThread = null;
        }
        super.onDestroy();
    }

    private final void showList() {
        progress.dismiss();
        setContentView(R.layout.load_fen);

        cb = (ChessBoardPlay)findViewById(R.id.loadfen_chessboard);
        okButton = (Button)findViewById(R.id.loadfen_ok);
        cancelButton = (Button)findViewById(R.id.loadfen_cancel);

        okButton.setEnabled(false);
        okButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (selectedFi != null)
                    sendBackResult(selectedFi);
            }
        });
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        Util.overrideFonts(findViewById(android.R.id.content));
        aa = new ArrayAdapter<FenInfo>(this, R.layout.select_game_list_item, fensInFile) {
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
        final ListView lv = getListView();
        lv.setSelectionFromTop(defaultItem, 0);
        lv.setFastScrollEnabled(true);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                selectedFi = aa.getItem(pos);
                if (selectedFi == null)
                    return;
                defaultItem = pos;
                Position chessPos;
                try {
                    chessPos = TextIO.readFEN(selectedFi.fen);
                } catch (ChessParseError e2) {
                    chessPos = e2.pos;
                }
                if (chessPos != null) {
                    cb.setPosition(chessPos);
                    okButton.setEnabled(true);
                }
            }
        });
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                selectedFi = aa.getItem(pos);
                if (selectedFi == null)
                    return false;
                defaultItem = pos;
                Position chessPos;
                try {
                    chessPos = TextIO.readFEN(selectedFi.fen);
                } catch (ChessParseError e2) {
                    chessPos = e2.pos;
                }
                if (chessPos != null)
                    sendBackResult(selectedFi);
                return true;
            }
        });
        lv.requestFocus();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (cb != null) {
            Position pos = cb.pos;
            showList();
            cb.setPosition(pos);
            okButton.setEnabled(selectedFi != null);
        }
    }

    final static int PROGRESS_DIALOG = 0;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case PROGRESS_DIALOG:
            progress = new ProgressDialog(this);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setTitle(R.string.reading_fen_file);
            progress.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Thread thr = workThread;
                    if (thr != null)
                        thr.interrupt();
                }
            });
            return progress;
        default:
            return null;
        }
    }

    private final boolean readFile() {
        String fileName = fenFile.getName();
        if (!fileName.equals(lastFileName))
            defaultItem = 0;
        long modTime = new File(fileName).lastModified();
        if (cacheValid && (modTime == lastModTime) && fileName.equals(lastFileName))
            return true;
        fenFile = new FENFile(fileName);
        Pair<FenInfoResult, ArrayList<FenInfo>> p = fenFile.getFenInfo(this, progress);
        if (p.first != FenInfoResult.OK) {
            fensInFile = new ArrayList<FenInfo>();
            if (p.first == FenInfoResult.OUT_OF_MEMORY) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.file_too_large,
                                       Toast.LENGTH_SHORT).show();
                    }
                });
            }
            setResult(RESULT_CANCELED);
            finish();
            return false;
        }
        fensInFile = p.second;
        cacheValid = true;
        lastModTime = modTime;
        lastFileName = fileName;
        return true;
    }

    private final void sendBackResult(FenInfo fi) {
        String fen = fi.fen;
        if (fen != null) {
            setResult(RESULT_OK, (new Intent()).setAction(fen));
            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
