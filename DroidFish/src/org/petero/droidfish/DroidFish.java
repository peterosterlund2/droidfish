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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.petero.droidfish.ChessBoard.SquareDecoration;
import org.petero.droidfish.activities.CPUWarning;
import org.petero.droidfish.activities.EditBoard;
import org.petero.droidfish.activities.EditPGNLoad;
import org.petero.droidfish.activities.EditPGNSave;
import org.petero.droidfish.activities.LoadScid;
import org.petero.droidfish.activities.Preferences;
import org.petero.droidfish.book.BookOptions;
import org.petero.droidfish.engine.EngineUtil;
import org.petero.droidfish.gamelogic.DroidChessController;
import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.PgnToken;
import org.petero.droidfish.gamelogic.GameTree.Node;
import org.petero.droidfish.gtb.Probe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DroidFish extends Activity implements GUIInterface {
    // FIXME!!! book.txt (and test classes) should not be included in apk

    // FIXME!!! PGN view option: game continuation (for training)
    // FIXME!!! Remove invalid playerActions in PGN import (should be done in verifyChildren)
    // FIXME!!! Implement bookmark mechanism for positions in pgn files
    // FIXME!!! Display chess notation in local language
    // FIXME!!! Add support for "Chess Leipzig" font
    // FIXME!!! Implement figurine notation

    // FIXME!!! Computer clock should stop if phone turned off (computer stops thinking if unplugged)
    // FIXME!!! Add support for all time controls defined by the PGN standard
    // FIXME!!! How to handle hour-glass time control?
    // FIXME!!! What should happen if you change time controls in the middle of a game?

    // FIXME!!! Online play on FICS
    // FIXME!!! Add chess960 support
    // FIXME!!! Implement "hint" feature

    // FIXME!!! Show extended book info. (Win percent, number of games, performance rating, etc.)
    // FIXME!!! Green color for "main move". Red color for "don't play in tournaments" moves.

    // FIXME!!! Add anti-lamer test: 8/8/8/8/8/8/3R3r/k3K2R w K - 0 1 bm O-O
    // FIXME!!! Add anti-lamer test: 4kr2/8/8/4PpN1/8/8/4Q3/3RK3 w - f6 0 2 bm exf6

    // FIXME!!! Remember multi-PV analysis setting when program restarted.
    // FIXME!!! Use high-res buttons from Scid on the go.
    // FIXME!!! Auto-swap sides is not good in combination with analysis mode.

    private ChessBoard cb;
    private static DroidChessController ctrl = null;
    private boolean mShowThinking;
    private boolean mShowStats;
    private boolean mWhiteBasedScores;
    private boolean mShowBookHints;
    private int maxNumArrows;
    private GameMode gameMode;
    private boolean mPonderMode;
    private int mEngineThreads;
    private boolean boardFlipped;
    private boolean autoSwapSides;

    private TextView status;
    private ScrollView moveListScroll;
    private TextView moveList;
    private TextView thinking;
    private ImageButton modeButton, undoButton, redoButton;
    private TextView whiteClock, blackClock, titleText;

    SharedPreferences settings;

    private float scrollSensitivity;
    private boolean invertScrollDirection;
    private boolean soundEnabled;
    private MediaPlayer moveSound;
    private boolean animateMoves;

    private final static String bookDir = "DroidFish";
    private final static String pgnDir = "DroidFish" + File.separator + "pgn";
    private final static String engineDir = "DroidFish" + File.separator + "uci";
    private final static String gtbDefaultDir = "DroidFish" + File.separator + "gtb";
    private BookOptions bookOptions = new BookOptions();
    private PGNOptions pgnOptions = new PGNOptions();
    private EGTBOptions egtbOptions = new EGTBOptions();

    private long lastVisibleMillis; // Time when GUI became invisible. 0 if currently visible.
    private long lastComputationMillis; // Time when engine last showed that it was computing.

    PgnScreenText gameTextListener;

    private WakeLock wakeLock = null;
    private boolean useWakeLock = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String pgn = getPgnIntent();

        createDirectories();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                readPrefs();
                ctrl.setGameMode(gameMode);
            }
        });

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        setWakeLock(false);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "droidfish");
        wakeLock.setReferenceCounted(false);

        initUI(true);

        gameTextListener = new PgnScreenText(pgnOptions);
        if (ctrl != null)
            ctrl.shutdownEngine();
        ctrl = new DroidChessController(this, gameTextListener, pgnOptions);
        egtbForceReload = true;
        readPrefs();
        ctrl.newGame(gameMode);
        {
            byte[] data = null;
            if (savedInstanceState != null) {
                data = savedInstanceState.getByteArray("gameState");
            } else {
                String dataStr = settings.getString("gameState", null);
                if (dataStr != null)
                    data = strToByteArr(dataStr);
            }
            if (data != null)
                ctrl.fromByteArray(data);
        }
        ctrl.setGuiPaused(true);
        ctrl.setGuiPaused(false);
        ctrl.startGame();
        if (pgn != null) {
            try {
                ctrl.setFENOrPGN(pgn);
            } catch (ChessParseError e) {
            }
        }
    }

    /** Create directory structure on SD card. */
    private void createDirectories() {
        File extDir = Environment.getExternalStorageDirectory();
        String sep = File.separator;
        new File(extDir + sep + bookDir).mkdirs();
        new File(extDir + sep + pgnDir).mkdirs();
        new File(extDir + sep + engineDir).mkdirs();
        new File(extDir + sep + gtbDefaultDir).mkdirs();
    }

    private String getPgnIntent() {
        String pgn = null;
        try {
            Intent intent = getIntent();
            if ((intent.getData() != null) && intent.getScheme().equals("content")) {
                ContentResolver resolver = getContentResolver();
                InputStream in = resolver.openInputStream(intent.getData());
                StringBuilder sb = new StringBuilder();
                while (true) {
                    byte[] buffer = new byte[16384];
                    int len = in.read(buffer);
                    if (len <= 0)
                        break;
                    sb.append(new String(buffer, 0, len));
                }
                pgn = sb.toString();
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), R.string.failed_to_read_pgn_data,
                           Toast.LENGTH_SHORT).show();
        }
        return pgn;
    }

    private final byte[] strToByteArr(String str) {
        int nBytes = str.length() / 2;
        byte[] ret = new byte[nBytes];
        for (int i = 0; i < nBytes; i++) {
            int c1 = str.charAt(i * 2) - 'A';
            int c2 = str.charAt(i * 2 + 1) - 'A';
            ret[i] = (byte)(c1 * 16 + c2);
        }
        return ret;
    }

    private final String byteArrToString(byte[] data) {
        StringBuilder ret = new StringBuilder(32768);
        int nBytes = data.length;
        for (int i = 0; i < nBytes; i++) {
            int b = data[i]; if (b < 0) b += 256;
            char c1 = (char)('A' + (b / 16));
            char c2 = (char)('A' + (b & 15));
            ret.append(c1);
            ret.append(c2);
        }
        return ret.toString();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ChessBoard oldCB = cb;
        String statusStr = status.getText().toString();
        initUI(false);
        readPrefs();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        cb.setPosition(oldCB.pos);
        cb.setFlipped(oldCB.flipped);
        cb.setDrawSquareLabels(oldCB.drawSquareLabels);
        cb.oneTouchMoves = oldCB.oneTouchMoves;
        setSelection(oldCB.selectedSquare);
        setStatusString(statusStr);
        moveListUpdated();
        updateThinkingInfo();
    }

    private final void initUI(boolean initTitle) {
        if (initTitle)
            requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        if (initTitle) {
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);
            whiteClock = (TextView)findViewById(R.id.white_clock);
            blackClock = (TextView)findViewById(R.id.black_clock);
            titleText  = (TextView)findViewById(R.id.title_text);
        }
        status = (TextView)findViewById(R.id.status);
        moveListScroll = (ScrollView)findViewById(R.id.scrollView);
        moveList = (TextView)findViewById(R.id.moveList);
        thinking = (TextView)findViewById(R.id.thinking);
        status.setFocusable(false);
        moveListScroll.setFocusable(false);
        moveList.setFocusable(false);
        moveList.setMovementMethod(LinkMovementMethod.getInstance());
        thinking.setFocusable(false);

        cb = (ChessBoard)findViewById(R.id.chessboard);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);

        final GestureDetector gd = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            private float scrollX = 0;
            private float scrollY = 0;
            public boolean onDown(MotionEvent e) {
                scrollX = 0;
                scrollY = 0;
                return false;
            }
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                cb.cancelLongPress();
                if (invertScrollDirection) {
                    distanceX = -distanceX;
                    distanceY = -distanceY;
                }
                if ((scrollSensitivity > 0) && (cb.sqSize > 0)) {
                    scrollX += distanceX;
                    scrollY += distanceY;
                    float scrollUnit = cb.sqSize * scrollSensitivity;
                    if (Math.abs(scrollX) >= Math.abs(scrollY)) {
                        // Undo/redo
                        int nRedo = 0, nUndo = 0;
                        while (scrollX > scrollUnit) {
                            nRedo++;
                            scrollX -= scrollUnit;
                        }
                        while (scrollX < -scrollUnit) {
                            nUndo++;
                            scrollX += scrollUnit;
                        }
                        if (nUndo + nRedo > 0)
                            scrollY = 0;
                        if (nRedo + nUndo > 1) {
                            boolean analysis = gameMode.analysisMode();
                            boolean human = gameMode.playerWhite() || gameMode.playerBlack();
                            if (analysis || !human)
                                ctrl.setGameMode(new GameMode(GameMode.TWO_PLAYERS));
                        }
                        for (int i = 0; i < nRedo; i++) ctrl.redoMove();
                        for (int i = 0; i < nUndo; i++) ctrl.undoMove();
                        ctrl.setGameMode(gameMode);
                    } else {
                        // Next/previous variation
                        int varDelta = 0;
                        while (scrollY > scrollUnit) {
                            varDelta++;
                            scrollY -= scrollUnit;
                        }
                        while (scrollY < -scrollUnit) {
                            varDelta--;
                            scrollY += scrollUnit;
                        }
                        if (varDelta != 0)
                            scrollX = 0;
                        ctrl.changeVariation(varDelta);
                    }
                }
                return true;
            }
            public boolean onSingleTapUp(MotionEvent e) {
                cb.cancelLongPress();
                handleClick(e);
                return true;
            }
            public boolean onDoubleTapEvent(MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP)
                    handleClick(e);
                return true;
            }
            private final void handleClick(MotionEvent e) {
                if (ctrl.humansTurn()) {
                    int sq = cb.eventToSquare(e);
                    Move m = cb.mousePressed(sq);
                    if (m != null)
                        ctrl.makeHumanMove(m);
                    setEgtbHints(cb.getSelectedSquare());
                }
            }
        });
        cb.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gd.onTouchEvent(event);
            }
        });
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
            public void onTrackballEvent(MotionEvent event) {
                if (ctrl.humansTurn()) {
                    Move m = cb.handleTrackballEvent(event);
                    if (m != null)
                        ctrl.makeHumanMove(m);
                    setEgtbHints(cb.getSelectedSquare());
                }
            }
        });
        cb.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                removeDialog(BOARD_MENU_DIALOG);
                showDialog(BOARD_MENU_DIALOG);
                return true;
            }
        });

        moveList.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                removeDialog(MOVELIST_MENU_DIALOG);
                showDialog(MOVELIST_MENU_DIALOG);
                return true;
            }
        });
        thinking.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (mShowThinking || gameMode.analysisMode()) {
                    if (!pvMoves.isEmpty()) {
                        removeDialog(THINKING_MENU_DIALOG);
                        showDialog(THINKING_MENU_DIALOG);
                    }
                }
                return true;
            }
        });

        modeButton = (ImageButton)findViewById(R.id.modeButton);
        modeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(GAME_MODE_DIALOG);
            }
        });
        undoButton = (ImageButton)findViewById(R.id.undoButton);
        undoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ctrl.undoMove();
            }
        });
        undoButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                removeDialog(GO_BACK_MENU_DIALOG);
                showDialog(GO_BACK_MENU_DIALOG);
                return true;
            }
        });
        redoButton = (ImageButton)findViewById(R.id.redoButton);
        redoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ctrl.redoMove();
            }
        });
        redoButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                removeDialog(GO_FORWARD_MENU_DIALOG);
                showDialog(GO_FORWARD_MENU_DIALOG);
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (ctrl != null) {
            byte[] data = ctrl.toByteArray();
            outState.putByteArray("gameState", data);
        }
    }

    @Override
    protected void onResume() {
        lastVisibleMillis = 0;
        if (ctrl != null)
            ctrl.setGuiPaused(false);
        notificationActive = true;
        updateNotification();
        setWakeLock(useWakeLock);
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (ctrl != null) {
            ctrl.setGuiPaused(true);
            byte[] data = ctrl.toByteArray();
            Editor editor = settings.edit();
            String dataStr = byteArrToString(data);
            editor.putString("gameState", dataStr);
            editor.commit();
        }
        lastVisibleMillis = System.currentTimeMillis();
        updateNotification();
        setWakeLock(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (ctrl != null)
            ctrl.shutdownEngine();
        setNotification(false);
        super.onDestroy();
    }

    private final int getIntSetting(String settingName, int defaultValue) {
        String tmp = settings.getString(settingName, String.format("%d", defaultValue));
        int value = Integer.parseInt(tmp);
        return value;
    }

    private final void readPrefs() {
        int modeNr = getIntSetting("gameMode", 1);
        gameMode = new GameMode(modeNr);
        boardFlipped = settings.getBoolean("boardFlipped", false);
        autoSwapSides = settings.getBoolean("autoSwapSides", false);
        setBoardFlip();
        boolean drawSquareLabels = settings.getBoolean("drawSquareLabels", false);
        cb.setDrawSquareLabels(drawSquareLabels);
        cb.oneTouchMoves = settings.getBoolean("oneTouchMoves", false);

        mShowThinking = settings.getBoolean("showThinking", false);
        mShowStats = settings.getBoolean("showStats", true);
        mWhiteBasedScores = settings.getBoolean("whiteBasedScores", false);
        maxNumArrows = getIntSetting("thinkingArrows", 2);
        mShowBookHints = settings.getBoolean("bookHints", false);

        mEngineThreads = getIntSetting("threads", 0);

        String engine = settings.getString("engine", "stockfish");
        int strength = settings.getInt("strength", 1000);
        setEngineStrength(engine, strength);

        mPonderMode = settings.getBoolean("ponderMode", false);
        if (!mPonderMode)
            ctrl.stopPonder();

        int timeControl = getIntSetting("timeControl", 120000);
        int movesPerSession = getIntSetting("movesPerSession", 60);
        int timeIncrement = getIntSetting("timeIncrement", 0);
        ctrl.setTimeLimit(timeControl, movesPerSession, timeIncrement);

        scrollSensitivity = Float.parseFloat(settings.getString("scrollSensitivity", "2"));
        invertScrollDirection = settings.getBoolean("invertScrollDirection", false);
        boolean fullScreenMode = settings.getBoolean("fullScreenMode", false);
        setFullScreenMode(fullScreenMode);
        useWakeLock = settings.getBoolean("wakeLock", false);
        setWakeLock(useWakeLock);

        int fontSize = getIntSetting("fontSize", 12);
        status.setTextSize(fontSize);
        moveList.setTextSize(fontSize);
        thinking.setTextSize(fontSize);
        soundEnabled = settings.getBoolean("soundEnabled", false);
        animateMoves = settings.getBoolean("animateMoves", true);

        boolean largeButtons = settings.getBoolean("largeButtons", false);
        Resources r = getResources();
        int bWidth  = (int)Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, r.getDisplayMetrics()));
        int bHeight = (int)Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics()));
        if (largeButtons) {
            bWidth  = bWidth  * 3 / 2;
            bHeight = bHeight * 3 / 2;
            modeButton.setImageResource(R.drawable.mode_large);
            undoButton.setImageResource(R.drawable.left_large);
            redoButton.setImageResource(R.drawable.right_large);
        } else {
            modeButton.setImageResource(R.drawable.mode);
            undoButton.setImageResource(R.drawable.left);
            redoButton.setImageResource(R.drawable.right);
        }
        modeButton.setLayoutParams(new LinearLayout.LayoutParams(bWidth, bHeight));
        undoButton.setLayoutParams(new LinearLayout.LayoutParams(bWidth, bHeight));
        redoButton.setLayoutParams(new LinearLayout.LayoutParams(bWidth, bHeight));

        bookOptions.filename = settings.getString("bookFile", "");
        bookOptions.maxLength = getIntSetting("bookMaxLength", 1000000);
        bookOptions.preferMainLines = settings.getBoolean("bookPreferMainLines", false);
        bookOptions.tournamentMode = settings.getBoolean("bookTournamentMode", false);
        bookOptions.random = (settings.getInt("bookRandom", 500) - 500) * (3.0 / 500);
        setBookOptions();

        egtbOptions.hints = settings.getBoolean("tbHints", false);
        egtbOptions.hintsEdit = settings.getBoolean("tbHintsEdit", false);
        egtbOptions.rootProbe = settings.getBoolean("tbRootProbe", false);
        egtbOptions.engineProbe = settings.getBoolean("tbEngineProbe", true);
        String gtbPath = settings.getString("gtbPath", "");
        if (gtbPath.length() == 0) {
            File extDir = Environment.getExternalStorageDirectory();
            String sep = File.separator;
            gtbPath = extDir.getAbsolutePath() + sep + gtbDefaultDir;
        }
        egtbOptions.gtbPath = gtbPath;
        setEgtbOptions();
        setEgtbHints(cb.getSelectedSquare());

        updateThinkingInfo();

        pgnOptions.view.variations  = settings.getBoolean("viewVariations",     true);
        pgnOptions.view.comments    = settings.getBoolean("viewComments",       true);
        pgnOptions.view.nag         = settings.getBoolean("viewNAG",            true);
        pgnOptions.view.headers     = settings.getBoolean("viewHeaders",        false);
        pgnOptions.imp.variations   = settings.getBoolean("importVariations",   true);
        pgnOptions.imp.comments     = settings.getBoolean("importComments",     true);
        pgnOptions.imp.nag          = settings.getBoolean("importNAG",          true);
        pgnOptions.exp.variations   = settings.getBoolean("exportVariations",   true);
        pgnOptions.exp.comments     = settings.getBoolean("exportComments",     true);
        pgnOptions.exp.nag          = settings.getBoolean("exportNAG",          true);
        pgnOptions.exp.playerAction = settings.getBoolean("exportPlayerAction", false);
        pgnOptions.exp.clockInfo    = settings.getBoolean("exportTime",         false);

        ColorTheme.instance().readColors(settings);
        cb.setColors();

        gameTextListener.clear();
        ctrl.prefsChanged();
    }

    private synchronized final void setWakeLock(boolean enableLock) {
        WakeLock wl = wakeLock;
        if (wl != null) {
            if (wl.isHeld())
                wl.release();
            if (enableLock)
                wl.acquire();
        }
    }

    private final void setEngineStrength(String engine, int strength) {
        ctrl.setEngineStrength(engine, strength);
        if (engine.contains("/")) {
            int idx = engine.lastIndexOf('/');
            String eName = engine.substring(idx + 1);
            titleText.setText(eName);
        } else {
            String eName = getString(engine.equals("cuckoochess") ?
                                     R.string.cuckoochess_engine :
                                     R.string.stockfish_engine);
            if (strength < 1000) {
                titleText.setText(String.format("%s: %d%%", eName, strength / 10));
            } else {
                titleText.setText(eName);
            }
        }
    }

    private final void setFullScreenMode(boolean fullScreenMode) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullScreenMode) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        getWindow().setAttributes(attrs);
    }

    private final void setBookOptions() {
        BookOptions options = new BookOptions(bookOptions);
        if (options.filename.length() > 0) {
            File extDir = Environment.getExternalStorageDirectory();
            String sep = File.separator;
            options.filename = extDir.getAbsolutePath() + sep + bookDir + sep + options.filename;
        }
        ctrl.setBookOptions(options);
    }

    private boolean egtbForceReload = false;

    private final void setEgtbOptions() {
        ctrl.setEgtbOptions(new EGTBOptions(egtbOptions));
        Probe.getInstance().setPath(egtbOptions.gtbPath, egtbForceReload);
        egtbForceReload = false;
    }

    private final void setEgtbHints(int sq) {
        if (!egtbOptions.hints || (sq < 0)) {
            cb.setSquareDecorations(null);
            return;
        }

        Probe gtbProbe = Probe.getInstance();
        ArrayList<Pair<Integer, Integer>> x = gtbProbe.movePieceProbe(cb.pos, sq);
        if (x == null) {
            cb.setSquareDecorations(null);
            return;
        }

        ArrayList<SquareDecoration> sd = new ArrayList<SquareDecoration>();
        for (Pair<Integer,Integer> p : x)
            sd.add(new SquareDecoration(p.first, p.second));
        cb.setSquareDecorations(sd);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    static private final int RESULT_EDITBOARD = 0;
    static private final int RESULT_SETTINGS = 1;
    static private final int RESULT_LOAD_PGN = 2;
    static private final int RESULT_SELECT_SCID = 3;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.item_new_game:
            if (autoSwapSides && (gameMode.playerWhite() != gameMode.playerBlack())) {
                int gameModeType;
                if (gameMode.playerWhite()) {
                    gameModeType = GameMode.PLAYER_BLACK;
                } else {
                    gameModeType = GameMode.PLAYER_WHITE;
                }
                Editor editor = settings.edit();
                String gameModeStr = String.format("%d", gameModeType);
                editor.putString("gameMode", gameModeStr);
                editor.commit();
                gameMode = new GameMode(gameModeType);
            }
//            savePGNToFile(ctrl.getPGN(), ".autosave.pgn", true);
            ctrl.newGame(gameMode);
            ctrl.startGame();
            return true;
        case R.id.item_editboard: {
            Intent i = new Intent(DroidFish.this, EditBoard.class);
            i.setAction(ctrl.getFEN());
            startActivityForResult(i, RESULT_EDITBOARD);
            return true;
        }
        case R.id.item_settings: {
            Intent i = new Intent(DroidFish.this, Preferences.class);
            startActivityForResult(i, RESULT_SETTINGS);
            return true;
        }
        case R.id.item_file_menu: {
            removeDialog(FILE_MENU_DIALOG);
            showDialog(FILE_MENU_DIALOG);
            return true;
        }
        case R.id.item_goto_move: {
            showDialog(SELECT_MOVE_DIALOG);
            return true;
        }
        case R.id.item_force_move: {
            ctrl.stopSearch();
            return true;
        }
        case R.id.item_draw: {
            if (ctrl.humansTurn()) {
                if (ctrl.claimDrawIfPossible()) {
                    ctrl.stopPonder();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.offer_draw, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        }
        case R.id.item_resign: {
            if (ctrl.humansTurn()) {
                ctrl.resignGame();
            }
            return true;
        }
        case R.id.select_book:
            removeDialog(SELECT_BOOK_DIALOG);
            showDialog(SELECT_BOOK_DIALOG);
            return true;
        case R.id.select_engine:
            removeDialog(SELECT_ENGINE_DIALOG);
            showDialog(SELECT_ENGINE_DIALOG);
            return true;
        case R.id.set_color_theme:
            showDialog(SET_COLOR_THEME_DIALOG);
            return true;
        case R.id.item_about:
            showDialog(ABOUT_DIALOG);
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case RESULT_SETTINGS:
            readPrefs();
            ctrl.setGameMode(gameMode);
            break;
        case RESULT_EDITBOARD:
            if (resultCode == RESULT_OK) {
                try {
                    String fen = data.getAction();
                    ctrl.setFENOrPGN(fen);
                } catch (ChessParseError e) {
                }
            }
            break;
        case RESULT_LOAD_PGN:
            if (resultCode == RESULT_OK) {
                try {
                    String pgn = data.getAction();
                    ctrl.setFENOrPGN(pgn);
                } catch (ChessParseError e) {
                    Toast.makeText(getApplicationContext(), getParseErrString(e), Toast.LENGTH_SHORT).show();
                }
            }
            break;
        case RESULT_SELECT_SCID:
            if (resultCode == RESULT_OK) {
                String pathName = data.getAction();
                if (pathName != null) {
                    Editor editor = settings.edit();
                    editor.putString("currentScidFile", pathName);
                    editor.putInt("currFT", FT_SCID);
                    editor.commit();
                    Intent i = new Intent(DroidFish.this, LoadScid.class);
                    i.setAction("org.petero.droidfish.loadScid");
                    i.putExtra("org.petero.droidfish.pathname", pathName);
                    startActivityForResult(i, RESULT_LOAD_PGN);
                }
            }
            break;
        }
    }

    private final String getParseErrString(ChessParseError e) {
        if (e.resourceId == -1)
            return e.getMessage();
        else
            return getString(e.resourceId);
    }

    private final void setBoardFlip() {
        boolean flipped = boardFlipped;
        if (autoSwapSides) {
            if (gameMode.analysisMode()) {
                flipped = !cb.pos.whiteMove;
            } else if (gameMode.playerWhite() && gameMode.playerBlack()) {
                flipped = !cb.pos.whiteMove;
            } else if (gameMode.playerWhite()) {
                flipped = false;
            } else if (gameMode.playerBlack()) {
                flipped = true;
            } else { // two computers
                flipped = !cb.pos.whiteMove;
            }
        }
        cb.setFlipped(flipped);
    }

    @Override
    public void setSelection(int sq) {
        cb.setSelection(sq);
        setEgtbHints(sq);
    }

    @Override
    public void setStatus(GameStatus s) {
        String str;
        switch (s.state) {
        case ALIVE:
            str = Integer.valueOf(s.moveNr).toString();
            if (s.white)
                str += ". " + getString(R.string.whites_move);
            else
                str += "... " + getString(R.string.blacks_move);
            if (s.ponder) str += " (" + getString(R.string.ponder) + ")";
            if (s.thinking) str += " (" + getString(R.string.thinking) + ")";
            if (s.analyzing) str += " (" + getString(R.string.analyzing) + ")";
            break;
        case WHITE_MATE:
            str = getString(R.string.white_mate);
            break;
        case BLACK_MATE:
            str = getString(R.string.black_mate);
            break;
        case WHITE_STALEMATE:
        case BLACK_STALEMATE:
            str = getString(R.string.stalemate);
            break;
        case DRAW_REP: {
            str = getString(R.string.draw_rep);
            if (s.drawInfo.length() > 0)
                str = str + " [" + s.drawInfo + "]";
            break;
        }
        case DRAW_50: {
            str = getString(R.string.draw_50);
            if (s.drawInfo.length() > 0)
                str = str + " [" + s.drawInfo + "]";
            break;
        }
        case DRAW_NO_MATE:
            str = getString(R.string.draw_no_mate);
            break;
        case DRAW_AGREE:
            str = getString(R.string.draw_agree);
            break;
        case RESIGN_WHITE:
            str = getString(R.string.resign_white);
            break;
        case RESIGN_BLACK:
            str = getString(R.string.resign_black);
            break;
        default:
            throw new RuntimeException();
        }
        setStatusString(str);
    }

    private final void setStatusString(String str) {
        status.setText(str);
    }

    @Override
    public void moveListUpdated() {
        moveList.setText(gameTextListener.getSpannableData());
        Layout layout = moveList.getLayout();
        if (layout != null) {
            int currPos = gameTextListener.getCurrPos();
            int line = layout.getLineForOffset(currPos);
            int y = (int) ((line - 1.5) * moveList.getLineHeight());
            moveListScroll.scrollTo(0, y);
        }
    }

    @Override
    public boolean whiteBasedScores() {
        return mWhiteBasedScores;
    }

    @Override
    public boolean ponderMode() {
        return mPonderMode;
    }

    @Override
    public int engineThreads() {
        return mEngineThreads;
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    /** Report a move made that is a candidate for GUI animation. */
    public void setAnimMove(Position sourcePos, Move move, boolean forward) {
        if (animateMoves && (move != null))
            cb.setAnimMove(sourcePos, move, forward);
    }

    @Override
    public void setPosition(Position pos, String variantInfo, ArrayList<Move> variantMoves) {
        variantStr = variantInfo;
        this.variantMoves = variantMoves;
        cb.setPosition(pos);
        setBoardFlip();
        updateThinkingInfo();
        setEgtbHints(cb.getSelectedSquare());
    }

    private String thinkingStr1 = "";
    private String thinkingStr2 = "";
    private String bookInfoStr = "";
    private String variantStr = "";
    private ArrayList<ArrayList<Move>> pvMoves = new ArrayList<ArrayList<Move>>();
    private ArrayList<Move> bookMoves = null;
    private ArrayList<Move> variantMoves = null;

    @Override
    public void setThinkingInfo(String pvStr, String statStr, String bookInfo,
                                ArrayList<ArrayList<Move>> pvMoves, ArrayList<Move> bookMoves) {
        thinkingStr1 = pvStr;
        thinkingStr2 = statStr;
        bookInfoStr = bookInfo;
        this.pvMoves = pvMoves;
        this.bookMoves = bookMoves;
        updateThinkingInfo();

        if (ctrl.computerBusy()) {
            lastComputationMillis = System.currentTimeMillis();
        } else {
            lastComputationMillis = 0;
        }
        updateNotification();
    }

    private final void updateThinkingInfo() {
        boolean thinkingEmpty = true;
        {
            String s = "";
            if (mShowThinking || gameMode.analysisMode()) {
                s = thinkingStr1;
                if (s.length() > 0) thinkingEmpty = false;
                if (mShowStats) {
                    if (!thinkingEmpty)
                        s += "\n";
                    s += thinkingStr2;
                    if (s.length() > 0) thinkingEmpty = false;
                }
            }
            thinking.setText(s, TextView.BufferType.SPANNABLE);
        }
        if (mShowBookHints && (bookInfoStr.length() > 0)) {
            String s = "";
            if (!thinkingEmpty)
                s += "<br>";
            s += "<b>" + getString(R.string.book) + "</b>" + bookInfoStr;
            thinking.append(Html.fromHtml(s));
            thinkingEmpty = false;
        }
        if (variantStr.indexOf(' ') >= 0) {
            String s = "";
            if (!thinkingEmpty)
                s += "<br>";
            s += "<b>" + getString(R.string.variation) + "</b> " + variantStr;
            thinking.append(Html.fromHtml(s));
            thinkingEmpty = false;
        }
        thinking.setVisibility(thinkingEmpty ? View.GONE : View.VISIBLE);

        List<Move> hints = null;
        if (mShowThinking || gameMode.analysisMode()) {
            ArrayList<ArrayList<Move>> pvMovesTmp = pvMoves;
            if (pvMovesTmp.size() == 1) {
                hints = pvMovesTmp.get(0);
            } else if (pvMovesTmp.size() > 1) {
                hints = new ArrayList<Move>();
                for (ArrayList<Move> pv : pvMovesTmp)
                    if (!pv.isEmpty())
                        hints.add(pv.get(0));
            }
        }
        if ((hints == null) && mShowBookHints)
            hints = bookMoves;
        if ((variantMoves != null) && variantMoves.size() > 1) {
            hints = variantMoves;
        }
        if ((hints != null) && (hints.size() > maxNumArrows)) {
            hints = hints.subList(0, maxNumArrows);
        }
        cb.setMoveHints(hints);
    }

    static private final int PROMOTE_DIALOG = 0;
    static private final int BOARD_MENU_DIALOG = 1;
    static private final int ABOUT_DIALOG = 2;
    static private final int SELECT_MOVE_DIALOG = 3;
    static private final int SELECT_BOOK_DIALOG = 4;
    static private final int SELECT_ENGINE_DIALOG = 5;
    static private final int SELECT_PGN_FILE_DIALOG = 6;
    static private final int SELECT_PGN_FILE_SAVE_DIALOG = 7;
    static private final int SET_COLOR_THEME_DIALOG = 8;
    static private final int GAME_MODE_DIALOG = 9;
    static private final int SELECT_PGN_SAVE_NEWFILE_DIALOG = 10;
    static private final int MOVELIST_MENU_DIALOG = 11;
    static private final int THINKING_MENU_DIALOG = 12;
    static private final int GO_BACK_MENU_DIALOG = 13;
    static private final int GO_FORWARD_MENU_DIALOG = 14;
    static private final int FILE_MENU_DIALOG = 15;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case PROMOTE_DIALOG: {
            final CharSequence[] items = {
                getString(R.string.queen), getString(R.string.rook),
                getString(R.string.bishop), getString(R.string.knight)
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.promote_pawn_to);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    ctrl.reportPromotePiece(item);
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case BOARD_MENU_DIALOG: {
            final int COPY_GAME      = 0;
            final int COPY_POSITION  = 1;
            final int PASTE          = 2;
            final int LOAD_GAME      = 3;
            final int SAVE_GAME      = 4;
            final int LOAD_SCID_GAME = 5;

            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(getString(R.string.copy_game));     actions.add(COPY_GAME);
            lst.add(getString(R.string.copy_position)); actions.add(COPY_POSITION);
            lst.add(getString(R.string.paste));         actions.add(PASTE);
            lst.add(getString(R.string.load_game));     actions.add(LOAD_GAME);
            lst.add(getString(R.string.save_game));     actions.add(SAVE_GAME);
            if (hasScidProvider()) {
                lst.add(getString(R.string.load_scid_game)); actions.add(LOAD_SCID_GAME);
            }
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tools_menu);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (finalActions.get(item)) {
                    case COPY_GAME: {
                        String pgn = ctrl.getPGN();
                        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(pgn);
                        break;
                    }
                    case COPY_POSITION: {
                        String fen = ctrl.getFEN() + "\n";
                        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(fen);
                        break;
                    }
                    case PASTE: {
                        ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        if (clipboard.hasText()) {
                            String fenPgn = clipboard.getText().toString();
                            try {
                                ctrl.setFENOrPGN(fenPgn);
                            } catch (ChessParseError e) {
                                Toast.makeText(getApplicationContext(), getParseErrString(e), Toast.LENGTH_SHORT).show();
                            }
                        }
                        break;
                    }
                    case LOAD_GAME:
                        removeDialog(SELECT_PGN_FILE_DIALOG);
                        showDialog(SELECT_PGN_FILE_DIALOG);
                        break;
                    case LOAD_SCID_GAME:
                        selectScidFile();
                        break;
                    case SAVE_GAME:
                        removeDialog(SELECT_PGN_FILE_SAVE_DIALOG);
                        showDialog(SELECT_PGN_FILE_SAVE_DIALOG);
                        break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case FILE_MENU_DIALOG: {
            final int LOAD_GAME      = 0;
            final int SAVE_GAME      = 1;
            final int LOAD_SCID_GAME = 2;

            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(getString(R.string.load_game));     actions.add(LOAD_GAME);
            lst.add(getString(R.string.save_game));     actions.add(SAVE_GAME);
            if (hasScidProvider()) {
                lst.add(getString(R.string.load_scid_game)); actions.add(LOAD_SCID_GAME);
            }
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.load_save_menu);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (finalActions.get(item)) {
                    case LOAD_GAME:
                        removeDialog(SELECT_PGN_FILE_DIALOG);
                        showDialog(SELECT_PGN_FILE_DIALOG);
                        break;
                    case SAVE_GAME:
                        removeDialog(SELECT_PGN_FILE_SAVE_DIALOG);
                        showDialog(SELECT_PGN_FILE_SAVE_DIALOG);
                        break;
                    case LOAD_SCID_GAME:
                        selectScidFile();
                        break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case ABOUT_DIALOG: {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String title = getString(R.string.app_name);
            try {
                PackageInfo pi = getPackageManager().getPackageInfo("org.petero.droidfish", 0);
                title += " " + pi.versionName;
            } catch (NameNotFoundException e) {
            }
            builder.setTitle(title).setMessage(R.string.about_info);
            AlertDialog alert = builder.create();
            return alert;
        }
        case SELECT_MOVE_DIALOG: {
            View content = View.inflate(this, R.layout.select_move_number, null);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(content);
            builder.setTitle(R.string.goto_move);
            final EditText moveNrView = (EditText)content.findViewById(R.id.selmove_number);
            moveNrView.setText("1");
            final Runnable gotoMove = new Runnable() {
                public void run() {
                    try {
                        int moveNr = Integer.parseInt(moveNrView.getText().toString());
                        ctrl.gotoMove(moveNr);
                    } catch (NumberFormatException nfe) {
                        Toast.makeText(getApplicationContext(), R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
                    }
                }
            };
            builder.setPositiveButton(R.string.ok, new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    gotoMove.run();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            final AlertDialog dialog = builder.create();

            moveNrView.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        gotoMove.run();
                        dialog.cancel();
                        return true;
                    }
                    return false;
                }
            });
            return dialog;
        }
        case SELECT_BOOK_DIALOG: {
            String[] fileNames = findFilesInDirectory(bookDir, new FileNameFilter() {
                @Override
                public boolean accept(String filename) {
                    int dotIdx = filename.lastIndexOf(".");
                    if (dotIdx < 0)
                        return false;
                    String ext = filename.substring(dotIdx+1);
                    return (ext.equals("ctg") || ext.equals("bin"));
                }
            });
            final int numFiles = fileNames.length;
            CharSequence[] items = new CharSequence[numFiles + 1];
            for (int i = 0; i < numFiles; i++)
                items[i] = fileNames[i];
            items[numFiles] = getString(R.string.internal_book);
            final CharSequence[] finalItems = items;
            int defaultItem = numFiles;
            for (int i = 0; i < numFiles; i++) {
                if (bookOptions.filename.equals(items[i])) {
                    defaultItem = i;
                    break;
                }
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_opening_book_file);
            builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Editor editor = settings.edit();
                    String bookFile = "";
                    if (item < numFiles)
                        bookFile = finalItems[item].toString();
                    editor.putString("bookFile", bookFile);
                    editor.commit();
                    bookOptions.filename = bookFile;
                    setBookOptions();
                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case SELECT_ENGINE_DIALOG: {
            String[] fileNames = findFilesInDirectory(engineDir, null);
            final int numFiles = fileNames.length;
            boolean haveSf = EngineUtil.internalStockFishName() != null;
            final int nEngines = numFiles + (haveSf ? 2 : 1);
            final String[] items = new String[nEngines];
            final String[] ids = new String[nEngines];
            int idx = 0;
            if (haveSf) {
                ids[idx] = "stockfish"; items[idx] = getString(R.string.stockfish_engine); idx++;
            }
            ids[idx] = "cuckoochess"; items[idx] = getString(R.string.cuckoochess_engine); idx++;
            String sep = File.separator;
            String base = Environment.getExternalStorageDirectory() + sep + engineDir + sep;
            for (int i = 0; i < numFiles; i++) {
                ids[idx] = base + fileNames[i];
                items[idx] = fileNames[i];
                idx++;
            }
            String currEngine = ctrl.getEngine();
            int defaultItem = 0;
            for (int i = 0; i < nEngines; i++) {
                if (ids[i].equals(currEngine)) {
                    defaultItem = i;
                    break;
                }
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_chess_engine);
            builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if ((item < 0) || (item >= nEngines))
                        return;
                    Editor editor = settings.edit();
                    String engine = ids[item];
                    editor.putString("engine", engine);
                    editor.commit();
                    dialog.dismiss();
                    int strength = settings.getInt("strength", 1000);
                    setEngineStrength(engine, strength);
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case SELECT_PGN_FILE_DIALOG: {
            final String[] fileNames = findFilesInDirectory(pgnDir, null);
            final int numFiles = fileNames.length;
            if (numFiles == 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.app_name).setMessage(R.string.no_pgn_files);
                AlertDialog alert = builder.create();
                return alert;
            }
            int defaultItem = 0;
            String currentPGNFile = settings.getString("currentPGNFile", "");
            for (int i = 0; i < numFiles; i++) {
                if (currentPGNFile.equals(fileNames[i])) {
                    defaultItem = i;
                    break;
                }
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_pgn_file);
            builder.setSingleChoiceItems(fileNames, defaultItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Editor editor = settings.edit();
                    String pgnFile = fileNames[item].toString();
                    editor.putString("currentPGNFile", pgnFile);
                    editor.putInt("currFT", FT_PGN);
                    editor.commit();
                    String sep = File.separator;
                    String pathName = Environment.getExternalStorageDirectory() + sep + pgnDir + sep + pgnFile;
                    Intent i = new Intent(DroidFish.this, EditPGNLoad.class);
                    i.setAction("org.petero.droidfish.loadFile");
                    i.putExtra("org.petero.droidfish.pathname", pathName);
                    startActivityForResult(i, RESULT_LOAD_PGN);
                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case SELECT_PGN_FILE_SAVE_DIALOG: {
            final String[] fileNames = findFilesInDirectory(pgnDir, null);
            final int numFiles = fileNames.length;
            int defaultItem = 0;
            String currentPGNFile = settings.getString("currentPGNFile", "");
            for (int i = 0; i < numFiles; i++) {
                if (currentPGNFile.equals(fileNames[i])) {
                    defaultItem = i;
                    break;
                }
            }
            CharSequence[] items = new CharSequence[numFiles + 1];
            for (int i = 0; i < numFiles; i++)
                items[i] = fileNames[i];
            items[numFiles] = getString(R.string.new_file);
            final CharSequence[] finalItems = items;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_pgn_file_save);
            builder.setSingleChoiceItems(finalItems, defaultItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    String pgnFile;
                    if (item >= numFiles) {
                        dialog.dismiss();
                        showDialog(SELECT_PGN_SAVE_NEWFILE_DIALOG);
                    } else {
                        Editor editor = settings.edit();
                        pgnFile = fileNames[item].toString();
                        editor.putString("currentPGNFile", pgnFile);
                        editor.commit();
                        dialog.dismiss();
                        savePGNToFile(ctrl.getPGN(), pgnFile, false);
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case SELECT_PGN_SAVE_NEWFILE_DIALOG: {
            View content = View.inflate(this, R.layout.create_pgn_file, null);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(content);
            builder.setTitle(R.string.select_pgn_file_save);
            final EditText fileNameView = (EditText)content.findViewById(R.id.create_pgn_filename);
            fileNameView.setText("");
            final Runnable savePGN = new Runnable() {
                public void run() {
                    String pgnFile = fileNameView.getText().toString();
                    if ((pgnFile.length() > 0) && !pgnFile.contains("."))
                        pgnFile += ".pgn";
                    savePGNToFile(ctrl.getPGN(), pgnFile, false);
                }
            };
            builder.setPositiveButton(R.string.ok, new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    savePGN.run();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            final Dialog dialog = builder.create();
            fileNameView.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        savePGN.run();
                        dialog.cancel();
                        return true;
                    }
                    return false;
                }
            });
            return dialog;
        }

        case SET_COLOR_THEME_DIALOG: {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_color_theme);
            builder.setSingleChoiceItems(ColorTheme.themeNames, -1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    ColorTheme.instance().setTheme(settings, item);
                    cb.setColors();
                    gameTextListener.setCurrent(gameTextListener.currNode);
                    moveListUpdated();
                    dialog.dismiss();
                }
            });
            return builder.create();
        }
        case GAME_MODE_DIALOG: {
            final CharSequence[] items = {
                getString(R.string.analysis_mode),
                getString(R.string.edit_replay_game),
                getString(R.string.play_white),
                getString(R.string.play_black),
                getString(R.string.two_players),
                getString(R.string.comp_vs_comp)
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_game_mode);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    int gameModeType = -1;
                    switch (item) {
                    case 0: gameModeType = GameMode.ANALYSIS;      break;
                    case 1: gameModeType = GameMode.EDIT_GAME;     break;
                    case 2: gameModeType = GameMode.PLAYER_WHITE;  break;
                    case 3: gameModeType = GameMode.PLAYER_BLACK;  break;
                    case 4: gameModeType = GameMode.TWO_PLAYERS;   break;
                    case 5: gameModeType = GameMode.TWO_COMPUTERS; break;
                    default: break;
                    }
                    dialog.dismiss();
                    if (gameModeType >= 0) {
                        Editor editor = settings.edit();
                        String gameModeStr = String.format("%d", gameModeType);
                        editor.putString("gameMode", gameModeStr);
                        editor.commit();
                        gameMode = new GameMode(gameModeType);
                        ctrl.setGameMode(gameMode);
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case MOVELIST_MENU_DIALOG: {
            final int EDIT_HEADERS   = 0;
            final int EDIT_COMMENTS  = 1;
            final int REMOVE_SUBTREE = 2;
            final int MOVE_VAR_UP    = 3;
            final int MOVE_VAR_DOWN  = 4;
            final int ADD_NULL_MOVE  = 5;

            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(getString(R.string.edit_headers));      actions.add(EDIT_HEADERS);
            if (ctrl.humansTurn()) {
                lst.add(getString(R.string.edit_comments)); actions.add(EDIT_COMMENTS);
            }
            lst.add(getString(R.string.truncate_gametree)); actions.add(REMOVE_SUBTREE);
            if (ctrl.numVariations() > 1) {
                lst.add(getString(R.string.move_var_up));   actions.add(MOVE_VAR_UP);
                lst.add(getString(R.string.move_var_down)); actions.add(MOVE_VAR_DOWN);
            }

            boolean allowNullMove =
                gameMode.analysisMode() ||
                (gameMode.playerWhite() && gameMode.playerBlack() && !gameMode.clocksActive());
            if (allowNullMove) {
                lst.add(getString(R.string.add_null_move)); actions.add(ADD_NULL_MOVE);
            }
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.edit_game);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (finalActions.get(item)) {
                    case EDIT_HEADERS: {
                        final TreeMap<String,String> headers = new TreeMap<String,String>();
                        ctrl.getHeaders(headers);

                        AlertDialog.Builder builder = new AlertDialog.Builder(DroidFish.this);
                        builder.setTitle(R.string.edit_headers);
                        View content = View.inflate(DroidFish.this, R.layout.edit_headers, null);
                        builder.setView(content);

                        final TextView event, site, date, round, white, black;

                        event = (TextView)content.findViewById(R.id.ed_header_event);
                        site = (TextView)content.findViewById(R.id.ed_header_site);
                        date = (TextView)content.findViewById(R.id.ed_header_date);
                        round = (TextView)content.findViewById(R.id.ed_header_round);
                        white = (TextView)content.findViewById(R.id.ed_header_white);
                        black = (TextView)content.findViewById(R.id.ed_header_black);

                        event.setText(headers.get("Event"));
                        site .setText(headers.get("Site"));
                        date .setText(headers.get("Date"));
                        round.setText(headers.get("Round"));
                        white.setText(headers.get("White"));
                        black.setText(headers.get("Black"));

                        builder.setNegativeButton(R.string.cancel, null);
                        builder.setPositiveButton(R.string.ok, new Dialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                headers.put("Event", event.getText().toString().trim());
                                headers.put("Site",  site .getText().toString().trim());
                                headers.put("Date",  date .getText().toString().trim());
                                headers.put("Round", round.getText().toString().trim());
                                headers.put("White", white.getText().toString().trim());
                                headers.put("Black", black.getText().toString().trim());
                                ctrl.setHeaders(headers);
                            }
                        });

                        builder.show();
                        break;
                    }
                    case EDIT_COMMENTS: {
                        AlertDialog.Builder builder = new AlertDialog.Builder(DroidFish.this);
                        builder.setTitle(R.string.edit_comments);
                        View content = View.inflate(DroidFish.this, R.layout.edit_comments, null);
                        builder.setView(content);

                        DroidChessController.CommentInfo commInfo = ctrl.getComments();

                        final TextView preComment, moveView, nag, postComment;
                        preComment = (TextView)content.findViewById(R.id.ed_comments_pre);
                        moveView = (TextView)content.findViewById(R.id.ed_comments_move);
                        nag = (TextView)content.findViewById(R.id.ed_comments_nag);
                        postComment = (TextView)content.findViewById(R.id.ed_comments_post);

                        preComment.setText(commInfo.preComment);
                        postComment.setText(commInfo.postComment);
                        moveView.setText(commInfo.move);
                        String nagStr = Node.nagStr(commInfo.nag).trim();
                        if ((nagStr.length() == 0) && (commInfo.nag > 0))
                            nagStr = String.format("%d", commInfo.nag);
                        nag.setText(nagStr);

                        builder.setNegativeButton(R.string.cancel, null);
                        builder.setPositiveButton(R.string.ok, new Dialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String pre = preComment.getText().toString().trim();
                                String post = postComment.getText().toString().trim();
                                int nagVal = Node.strToNag(nag.getText().toString());

                                DroidChessController.CommentInfo commInfo = new DroidChessController.CommentInfo();
                                commInfo.preComment = pre;
                                commInfo.postComment = post;
                                commInfo.nag = nagVal;
                                ctrl.setComments(commInfo);
                            }
                        });

                        builder.show();
                        break;
                    }
                    case REMOVE_SUBTREE:
                        ctrl.removeSubTree();
                        break;
                    case MOVE_VAR_UP:
                        ctrl.moveVariation(-1);
                        break;
                    case MOVE_VAR_DOWN:
                        ctrl.moveVariation(1);
                        break;
                    case ADD_NULL_MOVE:
                        ctrl.makeHumanNullMove();
                        break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case THINKING_MENU_DIALOG: {
            final int ADD_ANALYSIS = 0;
            final int MULTIPV_DEC = 1;
            final int MULTIPV_INC = 2;
            final int HIDE_STATISTICS = 3;
            final int SHOW_STATISTICS = 4;
            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(getString(R.string.add_analysis)); actions.add(ADD_ANALYSIS);
            final int numPV = ctrl.getNumPV();
            if (gameMode.analysisMode()) {
                int maxPV = ctrl.maxPV();
                if (numPV > 1) {
                    lst.add(getString(R.string.fewer_variations)); actions.add(MULTIPV_DEC);
                }
                if (numPV < maxPV) {
                    lst.add(getString(R.string.more_variations)); actions.add(MULTIPV_INC);
                }
            }
            if (thinkingStr1.length() > 0) {
                if (mShowStats) {
                    lst.add(getString(R.string.hide_statistics)); actions.add(HIDE_STATISTICS);
                } else {
                    lst.add(getString(R.string.show_statistics)); actions.add(SHOW_STATISTICS);
                }
            }
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.analysis);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (finalActions.get(item)) {
                    case ADD_ANALYSIS: {
                        ArrayList<ArrayList<Move>> pvMovesTmp = pvMoves;
                        String[] pvStrs = thinkingStr1.split("\n");
                        for (int i = 0; i < pvMovesTmp.size(); i++) {
                            ArrayList<Move> pv = pvMovesTmp.get(i);
                            StringBuilder preComment = new StringBuilder();
                            if (i < pvStrs.length) {
                                String[] tmp = pvStrs[i].split(" ");
                                for (int j = 0; j < 2; j++) {
                                    if (j < tmp.length) {
                                        if (j > 0) preComment.append(' ');
                                        preComment.append(tmp[j]);
                                    }
                                }
                                if (preComment.length() > 0) preComment.append(':');
                            }
                            boolean updateDefault = (i == 0);
                            ctrl.addVariation(preComment.toString(), pv, updateDefault);
                        }
                        break;
                    }
                    case MULTIPV_DEC:
                        ctrl.setMultiPVMode(numPV - 1);
                        break;
                    case MULTIPV_INC:
                        ctrl.setMultiPVMode(numPV + 1);
                        break;
                    case HIDE_STATISTICS:
                    case SHOW_STATISTICS: {
                        mShowStats = finalActions.get(item) == SHOW_STATISTICS;
                        Editor editor = settings.edit();
                        editor.putBoolean("showStats", mShowStats);
                        editor.commit();
                        updateThinkingInfo();
                        break;
                    }
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case GO_BACK_MENU_DIALOG: {
            final int GOTO_START_GAME = 0;
            final int GOTO_START_VAR  = 1;
            final int GOTO_PREV_VAR   = 2;
            final int LOAD_PREV_GAME  = 3;

            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(getString(R.string.goto_start_game));      actions.add(GOTO_START_GAME);
            lst.add(getString(R.string.goto_start_variation)); actions.add(GOTO_START_VAR);
            if (ctrl.currVariation() > 0) {
                lst.add(getString(R.string.goto_prev_variation)); actions.add(GOTO_PREV_VAR);
            }
            final int currFT = currFileType();
            final String currFileName = currFileName();
            if (currFT != FT_NONE) {
                lst.add(getString(R.string.load_prev_game)); actions.add(LOAD_PREV_GAME);
            }
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.go_back);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (finalActions.get(item)) {
                    case GOTO_START_GAME: ctrl.gotoMove(0); break;
                    case GOTO_START_VAR:  ctrl.gotoStartOfVariation(); break;
                    case GOTO_PREV_VAR:   ctrl.changeVariation(-1); break;
                    case LOAD_PREV_GAME:
                        String sep = File.separator;
                        String pathName = Environment.getExternalStorageDirectory() + sep;
                        Intent i;
                        if (currFT == FT_PGN) {
                            i = new Intent(DroidFish.this, EditPGNLoad.class);
                            i.setAction("org.petero.droidfish.loadFilePrevGame");
                            i.putExtra("org.petero.droidfish.pathname", pathName + pgnDir + sep + currFileName);
                        } else {
                            i = new Intent(DroidFish.this, LoadScid.class);
                            i.setAction("org.petero.droidfish.loadScidPrevGame");
                            i.putExtra("org.petero.droidfish.pathname", currFileName);
                        }
                        startActivityForResult(i, RESULT_LOAD_PGN);
                        break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case GO_FORWARD_MENU_DIALOG: {
            final int GOTO_END_VAR   = 0;
            final int GOTO_NEXT_VAR  = 1;
            final int LOAD_NEXT_GAME = 2;

            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(getString(R.string.goto_end_variation)); actions.add(GOTO_END_VAR);
            if (ctrl.currVariation() < ctrl.numVariations() - 1) {
                lst.add(getString(R.string.goto_next_variation)); actions.add(GOTO_NEXT_VAR);
            }
            final int currFT = currFileType();
            final String currFileName = currFileName();
            if (currFT != FT_NONE) {
                lst.add(getString(R.string.load_next_game)); actions.add(LOAD_NEXT_GAME);
            }
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.go_forward);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch (finalActions.get(item)) {
                    case GOTO_END_VAR:  ctrl.gotoMove(Integer.MAX_VALUE); break;
                    case GOTO_NEXT_VAR: ctrl.changeVariation(1); break;
                    case LOAD_NEXT_GAME:
                        String sep = File.separator;
                        String pathName = Environment.getExternalStorageDirectory() + sep;
                        Intent i;
                        if (currFT == FT_PGN) {
                            i = new Intent(DroidFish.this, EditPGNLoad.class);
                            i.setAction("org.petero.droidfish.loadFileNextGame");
                            i.putExtra("org.petero.droidfish.pathname", pathName + pgnDir + sep + currFileName);
                        } else {
                            i = new Intent(DroidFish.this, LoadScid.class);
                            i.setAction("org.petero.droidfish.loadScidNextGame");
                            i.putExtra("org.petero.droidfish.pathname", currFileName);
                        }
                        startActivityForResult(i, RESULT_LOAD_PGN);
                        break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        }
        return null;
    }

    private final void selectScidFile() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("org.scid.android",
                                              "org.scid.android.SelectFileActivity"));
        intent.setAction(".si4");
        startActivityForResult(intent, RESULT_SELECT_SCID);
    }

    final static int FT_NONE = 0;
    final static int FT_PGN  = 1;
    final static int FT_SCID = 2;

    private final int currFileType() {
        if (gameMode.clocksActive())
            return FT_NONE;
        int ft = settings.getInt("currFT", FT_NONE);
        return ft;
    }

    private final String currFileName() {
        int ft = settings.getInt("currFT", FT_NONE);
        switch (ft) {
        case FT_PGN:  return settings.getString("currentPGNFile", "");
        case FT_SCID: return settings.getString("currentScidFile", "");
        default: return "";
        }
    }

    private static interface FileNameFilter {
        boolean accept(String filename);
    }

    private final String[] findFilesInDirectory(String dirName, final FileNameFilter filter) {
        File extDir = Environment.getExternalStorageDirectory();
        String sep = File.separator;
        File dir = new File(extDir.getAbsolutePath() + sep + dirName);
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (!pathname.isFile())
                    return false;
                return (filter == null) || filter.accept(pathname.getAbsolutePath());
            }
        });
        if (files == null)
            files = new File[0];
        final int numFiles = files.length;
        String[] fileNames = new String[numFiles];
        for (int i = 0; i < files.length; i++)
            fileNames[i] = files[i].getName();
        Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
        return fileNames;
    }

    private final void savePGNToFile(String pgn, String filename, boolean silent) {
        String sep = File.separator;
        String pathName = Environment.getExternalStorageDirectory() + sep + pgnDir + sep + filename;
        Intent i = new Intent(DroidFish.this, EditPGNSave.class);
        i.setAction("org.petero.droidfish.saveFile");
        i.putExtra("org.petero.droidfish.pathname", pathName);
        i.putExtra("org.petero.droidfish.pgn", pgn);
        i.putExtra("org.petero.droidfish.silent", silent);
        startActivity(i);
    }

    @Override
    public void requestPromotePiece() {
        showDialog(PROMOTE_DIALOG);
    }

    @Override
    public void reportInvalidMove(Move m) {
        String msg = String.format("%s %s-%s",
                getString(R.string.invalid_move),
                TextIO.squareToString(m.from), TextIO.squareToString(m.to));
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void reportEngineName(String engine) {
        String msg = String.format("%s: %s",
                getString(R.string.engine), engine);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void reportEngineError(String errMsg) {
        String msg = String.format("%s: %s",
                getString(R.string.engine_error), errMsg);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void computerMoveMade() {
        if (soundEnabled) {
            if (moveSound != null)
                moveSound.release();
            moveSound = MediaPlayer.create(this, R.raw.movesound);
            moveSound.start();
        }
    }

    @Override
    public void runOnUIThread(Runnable runnable) {
        runOnUiThread(runnable);
    }

    /** Decide if user should be warned about heavy CPU usage. */
    private final void updateNotification() {
        boolean warn = false;
        if (lastVisibleMillis != 0) { // GUI not visible
            warn = lastComputationMillis >= lastVisibleMillis + 90000;
        }
        setNotification(warn);
    }

    private boolean notificationActive = false;

    /** Set/clear the "heavy CPU usage" notification. */
    private final void setNotification(boolean show) {
        if (notificationActive == show)
            return;
        notificationActive = show;
        final int cpuUsage = 1;
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(ns);
        if (show) {
            int icon = R.drawable.icon;
            CharSequence tickerText = getString(R.string.heavy_cpu_usage);
            long when = System.currentTimeMillis();
            Notification notification = new Notification(icon, tickerText, when);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;

            Context context = getApplicationContext();
            CharSequence contentTitle = getString(R.string.background_processing);
            CharSequence contentText = getString(R.string.lot_cpu_power);
            Intent notificationIntent = new Intent(this, CPUWarning.class);

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

            mNotificationManager.notify(cpuUsage, notification);
        } else {
            mNotificationManager.cancel(cpuUsage);
        }
    }

    private final String timeToString(long time) {
        int secs = (int)Math.floor((time + 999) / 1000.0);
        boolean neg = false;
        if (secs < 0) {
            neg = true;
            secs = -secs;
        }
        int mins = secs / 60;
        secs -= mins * 60;
        StringBuilder ret = new StringBuilder();
        if (neg) ret.append('-');
        ret.append(mins);
        ret.append(':');
        if (secs < 10) ret.append('0');
        ret.append(secs);
        return ret.toString();
    }

    private Handler handlerTimer = new Handler();
    private Runnable r = new Runnable() {
        public void run() {
            ctrl.updateRemainingTime();
        }
    };

    @Override
    public void setRemainingTime(long wTime, long bTime, long nextUpdate) {
        whiteClock.setText(getString(R.string.header_white) + " " + timeToString(wTime));
        blackClock.setText(getString(R.string.header_black) + " " + timeToString(bTime));
        handlerTimer.removeCallbacks(r);
        if (nextUpdate > 0) {
            handlerTimer.postDelayed(r, nextUpdate);
        }
    }

    /** PngTokenReceiver implementation that renders PGN data for screen display. */
    static class PgnScreenText implements PgnToken.PgnTokenReceiver {
        private SpannableStringBuilder sb = new SpannableStringBuilder();
        private int prevType = PgnToken.EOF;
        int nestLevel = 0;
        boolean col0 = true;
        Node currNode = null;
        final static int indentStep = 15;
        int currPos = 0, endPos = 0;
        boolean upToDate = false;
        PGNOptions options;

        private static class NodeInfo {
            int l0, l1;
            NodeInfo(int ls, int le) {
                l0 = ls;
                l1 = le;
            }
        }
        HashMap<Node, NodeInfo> nodeToCharPos;

        PgnScreenText(PGNOptions options) {
            nodeToCharPos = new HashMap<Node, NodeInfo>();
            this.options = options;
        }

        public final SpannableStringBuilder getSpannableData() {
            return sb;
        }
        public final int getCurrPos() {
            return currPos;
        }

        public boolean isUpToDate() {
            return upToDate;
        }

        int paraStart = 0;
        int paraIndent = 0;
        boolean paraBold = false;
        private final void newLine() { newLine(false); }
        private final void newLine(boolean eof) {
            if (!col0) {
                if (paraIndent > 0) {
                    int paraEnd = sb.length();
                    int indent = paraIndent * indentStep;
                    sb.setSpan(new LeadingMarginSpan.Standard(indent), paraStart, paraEnd,
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (paraBold) {
                    int paraEnd = sb.length();
                    sb.setSpan(new StyleSpan(Typeface.BOLD), paraStart, paraEnd,
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (!eof)
                    sb.append('\n');
                paraStart = sb.length();
                paraIndent = nestLevel;
                paraBold = false;
            }
            col0 = true;
        }

        boolean pendingNewLine = false;

        /** Makes moves in the move list clickable. */
        private final static class MoveLink extends ClickableSpan {
            private Node node;
            MoveLink(Node n) {
                node = n;
            }
            @Override
            public void onClick(View widget) {
                if (ctrl != null)
                    ctrl.goNode(node);
            }
            @Override
            public void updateDrawState(TextPaint ds) {
            }
        }

        public void processToken(Node node, int type, String token) {
            if (    (prevType == PgnToken.RIGHT_BRACKET) &&
                    (type != PgnToken.LEFT_BRACKET))  {
                if (options.view.headers) {
                    col0 = false;
                    newLine();
                } else {
                    sb.clear();
                    paraBold = false;
                }
            }
            if (pendingNewLine) {
                if (type != PgnToken.RIGHT_PAREN) {
                    newLine();
                    pendingNewLine = false;
                }
            }
            switch (type) {
            case PgnToken.STRING:
                sb.append(" \"");
                sb.append(token);
                sb.append('"');
                break;
            case PgnToken.INTEGER:
                if (    (prevType != PgnToken.LEFT_PAREN) &&
                        (prevType != PgnToken.RIGHT_BRACKET) && !col0)
                    sb.append(' ');
                sb.append(token);
                col0 = false;
                break;
            case PgnToken.PERIOD:
                sb.append('.');
                col0 = false;
                break;
            case PgnToken.ASTERISK:      sb.append(" *");  col0 = false; break;
            case PgnToken.LEFT_BRACKET:  sb.append('[');   col0 = false; break;
            case PgnToken.RIGHT_BRACKET: sb.append("]\n"); col0 = false; break;
            case PgnToken.LEFT_PAREN:
                nestLevel++;
                if (col0)
                    paraIndent++;
                newLine();
                sb.append('(');
                col0 = false;
                break;
            case PgnToken.RIGHT_PAREN:
                sb.append(')');
                nestLevel--;
                pendingNewLine = true;
                break;
            case PgnToken.NAG:
                sb.append(Node.nagStr(Integer.parseInt(token)));
                col0 = false;
                break;
            case PgnToken.SYMBOL: {
                if ((prevType != PgnToken.RIGHT_BRACKET) && (prevType != PgnToken.LEFT_BRACKET) && !col0)
                    sb.append(' ');
                int l0 = sb.length();
                sb.append(token);
                int l1 = sb.length();
                nodeToCharPos.put(node, new NodeInfo(l0, l1));
                sb.setSpan(new MoveLink(node), l0, l1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (endPos < l0) endPos = l0;
                col0 = false;
                if (nestLevel == 0) paraBold = true;
                break;
            }
            case PgnToken.COMMENT:
                if (prevType == PgnToken.RIGHT_BRACKET) {
                } else if (nestLevel == 0) {
                    nestLevel++;
                    newLine();
                    nestLevel--;
                } else {
                    if ((prevType != PgnToken.LEFT_PAREN) && !col0) {
                        sb.append(' ');
                    }
                }
                sb.append(token.replaceAll("[ \t\r\n]+", " ").trim());
                col0 = false;
                if (nestLevel == 0)
                    newLine();
                break;
            case PgnToken.EOF:
                newLine(true);
                upToDate = true;
                break;
            }
            prevType = type;
        }

        @Override
        public void clear() {
            sb.clear();
            prevType = PgnToken.EOF;
            nestLevel = 0;
            col0 = true;
            currNode = null;
            currPos = 0;
            endPos = 0;
            nodeToCharPos.clear();
            paraStart = 0;
            paraIndent = 0;
            paraBold = false;
            pendingNewLine = false;

            upToDate = false;
        }

        BackgroundColorSpan bgSpan = new BackgroundColorSpan(0xff888888);

        @Override
        public void setCurrent(Node node) {
            sb.removeSpan(bgSpan);
            NodeInfo ni = nodeToCharPos.get(node);
            if ((ni == null) && (node != null) && (node.getParent() != null))
                ni = nodeToCharPos.get(node.getParent());
            if (ni != null) {
                int color = ColorTheme.instance().getColor(ColorTheme.CURRENT_MOVE);
                bgSpan = new BackgroundColorSpan(color);
                sb.setSpan(bgSpan, ni.l0, ni.l1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                currPos = ni.l0;
            } else {
                currPos = 0;
            }
            currNode = node;
        }
    }

    private final boolean hasScidProvider() {
        List<ProviderInfo> providers = getPackageManager().queryContentProviders(null, 0, 0);
        for (ProviderInfo info : providers)
            if (info.authority.equals("org.scid.database.scidprovider"))
                return true;
        return false;
    }
}
