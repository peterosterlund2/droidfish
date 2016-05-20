/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2014  Peter Österlund, peterosterlund2@gmail.com
    Copyright (C) 2012 Leo Mayer

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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.petero.droidfish.ChessBoard.SquareDecoration;
import org.petero.droidfish.activities.CPUWarning;
import org.petero.droidfish.activities.EditBoard;
import org.petero.droidfish.activities.EditOptions;
import org.petero.droidfish.activities.EditPGNLoad;
import org.petero.droidfish.activities.EditPGNSave;
import org.petero.droidfish.activities.LoadFEN;
import org.petero.droidfish.activities.LoadScid;
import org.petero.droidfish.activities.Preferences;
import org.petero.droidfish.book.BookOptions;
import org.petero.droidfish.engine.EngineUtil;
import org.petero.droidfish.engine.UCIOptions;
import org.petero.droidfish.gamelogic.DroidChessController;
import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.PgnToken;
import org.petero.droidfish.gamelogic.GameTree.Node;
import org.petero.droidfish.gamelogic.TimeControlData;
import org.petero.droidfish.tb.Probe;
import org.petero.droidfish.tb.ProbeResult;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.Sequence;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewConfiguration;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ClickableViewAccessibility")
public class DroidFish extends Activity
                       implements GUIInterface,
                                  ActivityCompat.OnRequestPermissionsResultCallback {
    // FIXME!!! PGN view option: game continuation (for training)
    // FIXME!!! Remove invalid playerActions in PGN import (should be done in verifyChildren)
    // FIXME!!! Implement bookmark mechanism for positions in pgn files
    // FIXME!!! Add support for "Chess Leipzig" font

    // FIXME!!! Computer clock should stop if phone turned off (computer stops thinking if unplugged)
    // FIXME!!! Add support for "no time control" and "hour-glass time control" as defined by the PGN standard

    // FIXME!!! Add chess960 support
    // FIXME!!! Implement "hint" feature

    // FIXME!!! Show extended book info. (Win percent, number of games, performance rating, etc.)
    // FIXME!!! Green color for "main move". Red color for "don't play in tournaments" moves.
    // FIXME!!! ECO opening codes

    // FIXME!!! Option to display coordinates in border outside chess board.

    // FIXME!!! Better behavior if engine is terminated. How exactly?
    // FIXME!!! Handle PGN non-file intents with more than one game.
    // FIXME!!! Save position to fen/epd file

    // FIXME!!! Selection dialog for going into variation
    // FIXME!!! Use two engines in engine/engine games

    private ChessBoardPlay cb;
    private static DroidChessController ctrl = null;
    private boolean mShowThinking;
    private boolean mShowStats;
    private int numPV;
    private boolean mWhiteBasedScores;
    private boolean mShowBookHints;
    private int maxNumArrows;
    private GameMode gameMode;
    private boolean mPonderMode;
    private int timeControl;
    private int movesPerSession;
    private int timeIncrement;
    private String playerName;
    private boolean boardFlipped;
    private boolean autoSwapSides;
    private boolean playerNameFlip;
    private boolean discardVariations;

    private TextView status;
    private ScrollView moveListScroll;
    private MoveListView moveList;
    private View thinkingScroll;
    private TextView thinking;
    private View buttons;
    private ImageButton custom1Button, custom2Button, custom3Button;
    private ImageButton modeButton, undoButton, redoButton;
    private ButtonActions custom1ButtonActions, custom2ButtonActions, custom3ButtonActions;
    private TextView whiteTitleText, blackTitleText, engineTitleText;
    private View firstTitleLine, secondTitleLine;
    private TextView whiteFigText, blackFigText, summaryTitleText;
    private static Dialog moveListMenuDlg;

    private DrawerLayout drawerLayout;
    private ListView leftDrawer;
    private ListView rightDrawer;

    private SharedPreferences settings;

    private float scrollSensitivity;
    private boolean invertScrollDirection;

    private boolean leftHanded;
    private boolean soundEnabled;
    private MediaPlayer moveSound;
    private boolean vibrateEnabled;
    private boolean animateMoves;
    private boolean autoScrollTitle;
    private boolean showMaterialDiff;
    private boolean showVariationLine;

    private int autoMoveDelay; // Delay in auto forward/backward mode
    private static enum AutoMode {
        OFF, FORWARD, BACKWARD;
    }
    private AutoMode autoMode = AutoMode.OFF;

    /** State of requested permissions. */
    private static enum PermissionState {
        UNKNOWN,
        REQUESTED,
        GRANTED,
        DENIED
    }
    /** State of WRITE_EXTERNAL_STORAGE permission. */
    private PermissionState storagePermission = PermissionState.UNKNOWN;

    private final static String bookDir = "DroidFish/book";
    private final static String pgnDir = "DroidFish/pgn";
    private final static String fenDir = "DroidFish/epd";
    private final static String engineDir = "DroidFish/uci";
    private final static String gtbDefaultDir = "DroidFish/gtb";
    private final static String rtbDefaultDir = "DroidFish/rtb";
    private BookOptions bookOptions = new BookOptions();
    private PGNOptions pgnOptions = new PGNOptions();
    private EngineOptions engineOptions = new EngineOptions();

    private long lastVisibleMillis; // Time when GUI became invisible. 0 if currently visible.
    private long lastComputationMillis; // Time when engine last showed that it was computing.

    private PgnScreenText gameTextListener;

    private boolean useWakeLock = false;

    private Typeface figNotation;
    private Typeface defaultThinkingListTypeFace;

    private boolean guideShowOnStart;
    private TourGuide tourGuide;


    /** Defines all configurable button actions. */
    private ActionFactory actionFactory = new ActionFactory() {
        private HashMap<String, UIAction> actions;

        private void addAction(UIAction a) {
            actions.put(a.getId(), a);
        }

        {
            actions = new HashMap<String, UIAction>();
            addAction(new UIAction() {
                public String getId() { return "flipboard"; }
                public int getName() { return R.string.flip_board; }
                public int getIcon() { return R.raw.flip; }
                public boolean enabled() { return true; }
                public void run() {
                    boardFlipped = !cb.flipped;
                    setBooleanPref("boardFlipped", boardFlipped);
                    cb.setFlipped(boardFlipped);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "showThinking"; }
                public int getName() { return R.string.toggle_show_thinking; }
                public int getIcon() { return R.raw.thinking; }
                public boolean enabled() { return true; }
                public void run() {
                    mShowThinking = toggleBooleanPref("showThinking");
                    updateThinkingInfo();
                }
            });
            addAction(new UIAction() {
                public String getId() { return "bookHints"; }
                public int getName() { return R.string.toggle_book_hints; }
                public int getIcon() { return R.raw.book; }
                public boolean enabled() { return true; }
                public void run() {
                    mShowBookHints = toggleBooleanPref("bookHints");
                    updateThinkingInfo();
                }
            });
            addAction(new UIAction() {
                public String getId() { return "viewVariations"; }
                public int getName() { return R.string.toggle_pgn_variations; }
                public int getIcon() { return R.raw.variation; }
                public boolean enabled() { return true; }
                public void run() {
                    pgnOptions.view.variations = toggleBooleanPref("viewVariations");
                    gameTextListener.clear();
                    ctrl.prefsChanged(false);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "viewComments"; }
                public int getName() { return R.string.toggle_pgn_comments; }
                public int getIcon() { return R.raw.comment; }
                public boolean enabled() { return true; }
                public void run() {
                    pgnOptions.view.comments = toggleBooleanPref("viewComments");
                    gameTextListener.clear();
                    ctrl.prefsChanged(false);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "viewHeaders"; }
                public int getName() { return R.string.toggle_pgn_headers; }
                public int getIcon() { return R.raw.header; }
                public boolean enabled() { return true; }
                public void run() {
                    pgnOptions.view.headers = toggleBooleanPref("viewHeaders");
                    gameTextListener.clear();
                    ctrl.prefsChanged(false);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "toggleAnalysis"; }
                public int getName() { return R.string.toggle_analysis; }
                public int getIcon() { return R.raw.analyze; }
                public boolean enabled() { return true; }
                private int oldGameModeType = GameMode.EDIT_GAME;
                public void run() {
                    int gameModeType;
                    if (ctrl.analysisMode()) {
                        gameModeType = oldGameModeType;
                    } else {
                        oldGameModeType = ctrl.getGameMode().getModeNr();
                        gameModeType = GameMode.ANALYSIS;
                    }
                    newGameMode(gameModeType);
                    setBoardFlip(true);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "largeButtons"; }
                public int getName() { return R.string.toggle_large_buttons; }
                public int getIcon() { return R.raw.magnify; }
                public boolean enabled() { return true; }
                public void run() {
                    pgnOptions.view.headers = toggleBooleanPref("largeButtons");
                    updateButtons();
                }
            });
            addAction(new UIAction() {
                public String getId() { return "blindMode"; }
                public int getName() { return R.string.blind_mode; }
                public int getIcon() { return R.raw.blind; }
                public boolean enabled() { return true; }
                public void run() {
                    boolean blindMode = !cb.blindMode;
                    setBooleanPref("blindMode", blindMode);
                    cb.setBlindMode(blindMode);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "loadLastFile"; }
                public int getName() { return R.string.load_last_file; }
                public int getIcon() { return R.raw.open_last_file; }
                public boolean enabled() { return currFileType() != FT_NONE && storageAvailable(); }
                public void run() {
                    loadLastFile();
                }
            });
            addAction(new UIAction() {
                public String getId() { return "selectEngine"; }
                public int getName() { return R.string.select_engine; }
                public int getIcon() { return R.raw.engine; }
                public boolean enabled() { return true; }
                public void run() {
                    removeDialog(SELECT_ENGINE_DIALOG_NOMANAGE);
                    showDialog(SELECT_ENGINE_DIALOG_NOMANAGE);
                }
            });
            addAction(new UIAction() {
                public String getId() { return "engineOptions"; }
                public int getName() { return R.string.engine_options; }
                public int getIcon() { return R.raw.custom; }
                public boolean enabled() { return canSetEngineOptions(); }
                public void run() {
                    setEngineOptions();
                }
            });
        }

        @Override
        public UIAction getAction(String actionId) {
            return actions.get(actionId);
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Pair<String,String> pair = getPgnOrFenIntent();
        String intentPgnOrFen = pair.first;
        String intentFilename = pair.second;

        createDirectories();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        setWakeLock(false);

        custom1ButtonActions = new ButtonActions("custom1", CUSTOM1_BUTTON_DIALOG,
                                                 R.string.select_action);
        custom2ButtonActions = new ButtonActions("custom2", CUSTOM2_BUTTON_DIALOG,
                                                 R.string.select_action);
        custom3ButtonActions = new ButtonActions("custom3", CUSTOM3_BUTTON_DIALOG,
                                                 R.string.select_action);

        figNotation = Typeface.createFromAsset(getAssets(), "fonts/DroidFishChessNotationDark.otf");
        setPieceNames(PGNOptions.PT_LOCAL);
        initUI();

        gameTextListener = new PgnScreenText(this, pgnOptions);
        moveList.setOnLinkClickListener(gameTextListener);
        if (ctrl != null)
            ctrl.shutdownEngine();
        ctrl = new DroidChessController(this, gameTextListener, pgnOptions);
        egtbForceReload = true;
        readPrefs();
        TimeControlData tcData = new TimeControlData();
        tcData.setTimeControl(timeControl, movesPerSession, timeIncrement);
        ctrl.newGame(gameMode, tcData);
        setAutoMode(AutoMode.OFF);
        {
            byte[] data = null;
            int version = 1;
            if (savedInstanceState != null) {
                data = savedInstanceState.getByteArray("gameState");
                version = savedInstanceState.getInt("gameStateVersion", version);
            } else {
                String dataStr = settings.getString("gameState", null);
                version = settings.getInt("gameStateVersion", version);
                if (dataStr != null)
                    data = strToByteArr(dataStr);
            }
            if (data != null)
                ctrl.fromByteArray(data, version);
        }
        ctrl.setGuiPaused(true);
        ctrl.setGuiPaused(false);
        ctrl.startGame();
        if (intentPgnOrFen != null) {
            try {
                ctrl.setFENOrPGN(intentPgnOrFen);
                setBoardFlip(true);
            } catch (ChessParseError e) {
                // If FEN corresponds to illegal chess position, go into edit board mode.
                try {
                    TextIO.readFEN(intentPgnOrFen);
                } catch (ChessParseError e2) {
                    if (e2.pos != null)
                        startEditBoard(intentPgnOrFen);
                }
            }
        } else if (intentFilename != null) {
            if (intentFilename.toLowerCase(Locale.US).endsWith(".fen") ||
                intentFilename.toLowerCase(Locale.US).endsWith(".epd"))
                loadFENFromFile(intentFilename);
            else
                loadPGNFromFile(intentFilename);
        }

        startTourGuide();
    }

    private void startTourGuide(){
        if (!guideShowOnStart)
            return;

        tourGuide = TourGuide.init(this);
        ArrayList<TourGuide> guides = new ArrayList<TourGuide>();

        TourGuide tg = TourGuide.init(this);
        tg.setToolTip(new ToolTip()
                      .setTitle(getString(R.string.tour_leftMenu_title))
                      .setDescription(getString(R.string.tour_leftMenu_desc))
                      .setGravity(Gravity.BOTTOM | Gravity.RIGHT));
        tg.playLater(whiteTitleText);
        guides.add(tg);

        tg = TourGuide.init(this);
        tg.setToolTip(new ToolTip()
                      .setTitle(getString(R.string.tour_rightMenu_title))
                      .setDescription(getString(R.string.tour_rightMenu_desc))
                      .setGravity(Gravity.BOTTOM | Gravity.LEFT));
        tg.playLater(blackTitleText);
        guides.add(tg);

        tg = TourGuide.init(this);
        int gravity = !landScapeView() ? Gravity.BOTTOM : leftHandedView() ? Gravity.LEFT : Gravity.RIGHT;
        tg.setToolTip(new ToolTip()
                      .setTitle(getString(R.string.tour_chessBoard_title))
                      .setDescription(getString(R.string.tour_chessBoard_desc))
                      .setGravity(gravity));
        tg.playLater(cb);
        guides.add(tg);

        tg = TourGuide.init(this);
        gravity = !landScapeView() ? Gravity.TOP : Gravity.BOTTOM;
        tg.setToolTip(new ToolTip()
                      .setTitle(getString(R.string.tour_buttons_title))
                      .setDescription(getString(R.string.tour_buttons_desc))
                      .setGravity(gravity));
        tg.playLater(buttons);
        guides.add(tg);

        tg = TourGuide.init(this);
        gravity = !landScapeView() ? Gravity.TOP : leftHandedView() ? Gravity.RIGHT : Gravity.LEFT;
        tg.setToolTip(new ToolTip()
                      .setTitle(getString(R.string.tour_moveList_title))
                      .setDescription(getString(R.string.tour_moveList_desc))
                      .setGravity(gravity));
        tg.playLater(moveListScroll);
        guides.add(tg);

        tg = TourGuide.init(this);
        tg.setToolTip(new ToolTip()
                      .setTitle(getString(R.string.tour_analysis_title))
                      .setDescription(getString(R.string.tour_analysis_desc))
                      .setGravity(Gravity.TOP));
        tg.playLater(thinkingScroll);
        guides.add(tg);

        tg.setOverlay(new Overlay()
                      .setOnClickListener(new View.OnClickListener() {
                          @Override
                          public void onClick(View v) {
                              guideShowOnStart = false;
                              Editor editor = settings.edit();
                              editor.putBoolean("guideShowOnStart", false);
                              editor.commit();
                              tourGuide.next();
                              tourGuide = null;
                          }
                      }));

        Sequence sequence = new Sequence.SequenceBuilder()
                .add(guides.toArray(new TourGuide[guides.size()]))
                .setDefaultOverlay(new Overlay()
                                   .setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           tourGuide.next();
                                       }
                                   }))
                .setDefaultPointer(new Pointer())
                .setContinueMethod(Sequence.ContinueMethod.OverlayListener)
                .build();
        tourGuide.playInSequence(sequence);
    }

    // Unicode code points for chess pieces
    private static final String figurinePieceNames = Piece.NOTATION_PAWN   + " " +
                                                     Piece.NOTATION_KNIGHT + " " +
                                                     Piece.NOTATION_BISHOP + " " +
                                                     Piece.NOTATION_ROOK   + " " +
                                                     Piece.NOTATION_QUEEN  + " " +
                                                     Piece.NOTATION_KING;

    private final void setPieceNames(int pieceType) {
        if (pieceType == PGNOptions.PT_FIGURINE) {
            TextIO.setPieceNames(figurinePieceNames);
        } else {
            TextIO.setPieceNames(getString(R.string.piece_names));
        }
    }

    /** Create directory structure on SD card. */
    private final void createDirectories() {
        if (storagePermission == PermissionState.UNKNOWN) {
            String extStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, extStorage) == 
                    PackageManager.PERMISSION_GRANTED) {
                storagePermission = PermissionState.GRANTED;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{extStorage}, 0);
                storagePermission = PermissionState.REQUESTED;
            }
        }
        if (storagePermission != PermissionState.GRANTED)
            return;

        File extDir = Environment.getExternalStorageDirectory();
        String sep = File.separator;
        new File(extDir + sep + bookDir).mkdirs();
        new File(extDir + sep + pgnDir).mkdirs();
        new File(extDir + sep + fenDir).mkdirs();
        new File(extDir + sep + engineDir).mkdirs();
        new File(extDir + sep + engineDir + sep + EngineUtil.openExchangeDir).mkdirs();
        new File(extDir + sep + gtbDefaultDir).mkdirs();
        new File(extDir + sep + rtbDefaultDir).mkdirs();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        if (storagePermission == PermissionState.REQUESTED) {
            if ((results.length > 0) && (results[0] == PackageManager.PERMISSION_GRANTED))
                storagePermission = PermissionState.GRANTED;
            else
                storagePermission = PermissionState.DENIED;
        }
        createDirectories();
    }

    /** Return true if the WRITE_EXTERNAL_STORAGE permission has been granted. */
    private boolean storageAvailable() {
        return storagePermission == PermissionState.GRANTED;
    }

    /**
     * Return PGN/FEN data or filename from the Intent. Both can not be non-null.
     * @return Pair of PGN/FEN data and filename.
     */
    private final Pair<String,String> getPgnOrFenIntent() {
        String pgnOrFen = null;
        String filename = null;
        try {
            Intent intent = getIntent();
            Uri data = intent.getData();
            if (data == null) {
                Bundle b = intent.getExtras();
                if (b != null) {
                    Object strm = b.get(Intent.EXTRA_STREAM);
                    if (strm instanceof Uri) {
                        data = (Uri)strm;
                        if ("file".equals(data.getScheme())) {
                            filename = data.getEncodedPath();
                            if (filename != null)
                                filename = Uri.decode(filename);
                        }
                    }
                }
            }
            if (data == null) {
                if ((Intent.ACTION_SEND.equals(intent.getAction()) ||
                     Intent.ACTION_VIEW.equals(intent.getAction())) &&
                    ("application/x-chess-pgn".equals(intent.getType()) ||
                     "application/x-chess-fen".equals(intent.getType())))
                    pgnOrFen = intent.getStringExtra(Intent.EXTRA_TEXT);
            } else {
                String scheme = intent.getScheme();
                if ("file".equals(scheme)) {
                    filename = data.getEncodedPath();
                    if (filename != null)
                        filename = Uri.decode(filename);
                }
                if ((filename == null) &&
                    ("content".equals(scheme) ||
                     "file".equals(scheme))) {
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
                    pgnOrFen = sb.toString();
                }
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), R.string.failed_to_read_pgn_data,
                           Toast.LENGTH_SHORT).show();
        }
        return new Pair<String,String>(pgnOrFen,filename);
    }

    private final byte[] strToByteArr(String str) {
        if (str == null)
            return null;
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
        if (data == null)
            return null;
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
        reInitUI();
    }

    /** Re-initialize UI when layout should change because of rotation or handedness change. */
    private final void reInitUI() {
        ChessBoardPlay oldCB = cb;
        String statusStr = status.getText().toString();
        initUI();
        readPrefs();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        cb.setPosition(oldCB.pos);
        cb.setFlipped(oldCB.flipped);
        cb.setDrawSquareLabels(oldCB.drawSquareLabels);
        cb.oneTouchMoves = oldCB.oneTouchMoves;
        cb.toggleSelection = oldCB.toggleSelection;
        cb.highlightLastMove = oldCB.highlightLastMove;
        cb.setBlindMode(oldCB.blindMode);
        setSelection(oldCB.selectedSquare);
        cb.userSelectedSquare = oldCB.userSelectedSquare;
        setStatusString(statusStr);
        moveList.setOnLinkClickListener(gameTextListener);
        moveListUpdated();
        updateThinkingInfo();
        ctrl.updateRemainingTime();
        ctrl.updateMaterialDiffList();
        if (tourGuide != null) {
            tourGuide.cleanUp();
            tourGuide = null;
        }
    }

    /** Return true if the current orientation is landscape. */
    private final boolean landScapeView() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
    
    /** Return true if left-handed layout should be used. */
    private final boolean leftHandedView() {
        return settings.getBoolean("leftHanded", false) && landScapeView();
    }

    /** Re-read preferences settings. */
    private final void handlePrefsChange() {
        if (leftHanded != leftHandedView())
            reInitUI();
        else
            readPrefs();
        maybeAutoModeOff(gameMode);
        ctrl.setGameMode(gameMode);
    }

    private final void initUI() {
        leftHanded = leftHandedView();
        setContentView(leftHanded ? R.layout.main_left_handed : R.layout.main);
        overrideViewAttribs();

        // title lines need to be regenerated every time due to layout changes (rotations)
        firstTitleLine = findViewById(R.id.first_title_line);
        secondTitleLine = findViewById(R.id.second_title_line);
        whiteTitleText = (TextView)findViewById(R.id.white_clock);
        whiteTitleText.setSelected(true);
        blackTitleText = (TextView)findViewById(R.id.black_clock);
        blackTitleText.setSelected(true);
        engineTitleText = (TextView)findViewById(R.id.title_text);
        whiteFigText = (TextView)findViewById(R.id.white_pieces);
        whiteFigText.setTypeface(figNotation);
        whiteFigText.setSelected(true);
        whiteFigText.setTextColor(whiteTitleText.getTextColors());
        blackFigText = (TextView)findViewById(R.id.black_pieces);
        blackFigText.setTypeface(figNotation);
        blackFigText.setSelected(true);
        blackFigText.setTextColor(blackTitleText.getTextColors());
        summaryTitleText = (TextView)findViewById(R.id.title_text_summary);

        status = (TextView)findViewById(R.id.status);
        moveListScroll = (ScrollView)findViewById(R.id.scrollView);
        moveList = (MoveListView)findViewById(R.id.moveList);
        thinkingScroll = (View)findViewById(R.id.scrollViewBot);
        thinking = (TextView)findViewById(R.id.thinking);
        defaultThinkingListTypeFace = thinking.getTypeface();
        status.setFocusable(false);
        moveListScroll.setFocusable(false);
        moveList.setFocusable(false);
        thinking.setFocusable(false);

        initDrawers();

        class ClickListener implements OnClickListener, OnTouchListener {
            private float touchX = -1;
            @Override
            public void onClick(View v) {
                boolean left = touchX <= v.getWidth() / 2.0;
                drawerLayout.openDrawer(left ? Gravity.LEFT : Gravity.RIGHT);
                touchX = -1;
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touchX = event.getX();
                return false;
            }
        }
        ClickListener listener = new ClickListener();
        firstTitleLine.setOnClickListener(listener);
        firstTitleLine.setOnTouchListener(listener);
        secondTitleLine.setOnClickListener(listener);
        secondTitleLine.setOnTouchListener(listener);

        cb = (ChessBoardPlay)findViewById(R.id.chessboard);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        cb.setPgnOptions(pgnOptions);

        cb.setOnTouchListener(new OnTouchListener() {
            private boolean pending = false;
            private boolean pendingClick = false;
            private int sq0 = -1;
            private float scrollX = 0;
            private float scrollY = 0;
            private float prevX = 0;
            private float prevY = 0;
            private Handler handler = new Handler();
            private Runnable runnable = new Runnable() {
                public void run() {
                    pending = false;
                    handler.removeCallbacks(runnable);
                    ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(20);
                    removeDialog(BOARD_MENU_DIALOG);
                    showDialog(BOARD_MENU_DIALOG);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    handler.postDelayed(runnable, ViewConfiguration.getLongPressTimeout());
                    pending = true;
                    pendingClick = true;
                    sq0 = cb.eventToSquare(event);
                    scrollX = 0;
                    scrollY = 0;
                    prevX = event.getX();
                    prevY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (pending) {
                        int sq = cb.eventToSquare(event);
                        if (sq != sq0) {
                            handler.removeCallbacks(runnable);
                            pendingClick = false;
                        }
                        float currX = event.getX();
                        float currY = event.getY();
                        if (onScroll(currX - prevX, currY - prevY)) {
                            handler.removeCallbacks(runnable);
                            pendingClick = false;
                        }
                        prevX = currX;
                        prevY = currY;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (pending) {
                        pending = false;
                        handler.removeCallbacks(runnable);
                        if (!pendingClick)
                            break;
                        int sq = cb.eventToSquare(event);
                        if (sq == sq0) {
                            if (ctrl.humansTurn()) {
                                Move m = cb.mousePressed(sq);
                                if (m != null) {
                                    setAutoMode(AutoMode.OFF);
                                    ctrl.makeHumanMove(m);
                                }
                                setEgtbHints(cb.getSelectedSquare());
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    pending = false;
                    handler.removeCallbacks(runnable);
                    break;
                }
                return true;
            }

            private boolean onScroll(float distanceX, float distanceY) {
                if (invertScrollDirection) {
                    distanceX = -distanceX;
                    distanceY = -distanceY;
                }
                if ((scrollSensitivity > 0) && (cb.sqSize > 0)) {
                    scrollX += distanceX;
                    scrollY += distanceY;
                    final float scrollUnit = cb.sqSize * scrollSensitivity;
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
                        if (nUndo + nRedo > 0) {
                            scrollY = 0;
                            setAutoMode(AutoMode.OFF);
                        }
                        if (nRedo + nUndo > 1) {
                            boolean analysis = gameMode.analysisMode();
                            boolean human = gameMode.playerWhite() || gameMode.playerBlack();
                            if (analysis || !human)
                                ctrl.setGameMode(new GameMode(GameMode.TWO_PLAYERS));
                        }
                        for (int i = 0; i < nRedo; i++) ctrl.redoMove();
                        for (int i = 0; i < nUndo; i++) ctrl.undoMove();
                        ctrl.setGameMode(gameMode);
                        return nRedo + nUndo > 0;
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
                        if (varDelta != 0) {
                            scrollX = 0;
                            setAutoMode(AutoMode.OFF);
                            ctrl.changeVariation(varDelta);
                        }
                        return varDelta != 0;
                    }
                }
                return false;
            }
        });
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
            public void onTrackballEvent(MotionEvent event) {
                if (ctrl.humansTurn()) {
                    Move m = cb.handleTrackballEvent(event);
                    if (m != null) {
                        setAutoMode(AutoMode.OFF);
                        ctrl.makeHumanMove(m);
                    }
                    setEgtbHints(cb.getSelectedSquare());
                }
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

        buttons = (View)findViewById(R.id.buttons);
        custom1Button = (ImageButton)findViewById(R.id.custom1Button);
        custom1ButtonActions.setImageButton(custom1Button, this);
        custom2Button = (ImageButton)findViewById(R.id.custom2Button);
        custom2ButtonActions.setImageButton(custom2Button, this);
        custom3Button = (ImageButton)findViewById(R.id.custom3Button);
        custom3ButtonActions.setImageButton(custom3Button, this);

        modeButton = (ImageButton)findViewById(R.id.modeButton);
        modeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(GAME_MODE_DIALOG);
            }
        });
        modeButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                drawerLayout.openDrawer(Gravity.LEFT);
                return true;
            }
        });
        undoButton = (ImageButton)findViewById(R.id.undoButton);
        undoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setAutoMode(AutoMode.OFF);
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
                setAutoMode(AutoMode.OFF);
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
            outState.putInt("gameStateVersion", 3);
        }
    }

    @Override
    protected void onResume() {
        lastVisibleMillis = 0;
        if (ctrl != null)
            ctrl.setGuiPaused(false);
        notificationActive = true;
        updateNotification();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (ctrl != null) {
            setAutoMode(AutoMode.OFF);
            ctrl.setGuiPaused(true);
            byte[] data = ctrl.toByteArray();
            Editor editor = settings.edit();
            String dataStr = byteArrToString(data);
            editor.putString("gameState", dataStr);
            editor.putInt("gameStateVersion", 3);
            editor.commit();
        }
        lastVisibleMillis = System.currentTimeMillis();
        updateNotification();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        setAutoMode(AutoMode.OFF);
        if (ctrl != null)
            ctrl.shutdownEngine();
        setNotification(false);
        super.onDestroy();
    }

    private final int getIntSetting(String settingName, int defaultValue) {
        String tmp = settings.getString(settingName, String.format(Locale.US, "%d", defaultValue));
        int value = Integer.parseInt(tmp);
        return value;
    }

    private final void readPrefs() {
        int modeNr = getIntSetting("gameMode", 1);
        gameMode = new GameMode(modeNr);
        String oldPlayerName = playerName;
        playerName = settings.getString("playerName", "Player");
        boardFlipped = settings.getBoolean("boardFlipped", false);
        autoSwapSides = settings.getBoolean("autoSwapSides", false);
        playerNameFlip = settings.getBoolean("playerNameFlip", true);
        setBoardFlip(!playerName.equals(oldPlayerName));
        boolean drawSquareLabels = settings.getBoolean("drawSquareLabels", false);
        cb.setDrawSquareLabels(drawSquareLabels);
        cb.oneTouchMoves = settings.getBoolean("oneTouchMoves", false);
        cb.toggleSelection = getIntSetting("squareSelectType", 0) == 1;
        cb.highlightLastMove = settings.getBoolean("highlightLastMove", true);
        cb.setBlindMode(settings.getBoolean("blindMode", false));

        mShowThinking = settings.getBoolean("showThinking", false);
        mShowStats = settings.getBoolean("showStats", true);
        numPV = settings.getInt("numPV", 1);
        ctrl.setMultiPVMode(numPV);
        mWhiteBasedScores = settings.getBoolean("whiteBasedScores", false);
        maxNumArrows = getIntSetting("thinkingArrows", 2);
        mShowBookHints = settings.getBoolean("bookHints", false);

        String engine = settings.getString("engine", "stockfish");
        int strength = settings.getInt("strength", 1000);
        setEngineStrength(engine, strength);

        mPonderMode = settings.getBoolean("ponderMode", false);
        if (!mPonderMode)
            ctrl.stopPonder();

        timeControl = getIntSetting("timeControl", 120000);
        movesPerSession = getIntSetting("movesPerSession", 60);
        timeIncrement = getIntSetting("timeIncrement", 0);

        autoMoveDelay = getIntSetting("autoDelay", 5000);

        scrollSensitivity = Float.parseFloat(settings.getString("scrollSensitivity", "2"));
        invertScrollDirection = settings.getBoolean("invertScrollDirection", false);
        discardVariations = settings.getBoolean("discardVariations", false);
        Util.setFullScreenMode(this, settings);
        useWakeLock = settings.getBoolean("wakeLock", false);
        setWakeLock(useWakeLock);

        int fontSize = getIntSetting("fontSize", 12);
        int statusFontSize = fontSize;
        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_PORTRAIT)
            statusFontSize = Math.min(statusFontSize, 16);
        status.setTextSize(statusFontSize);
        soundEnabled = settings.getBoolean("soundEnabled", false);
        vibrateEnabled = settings.getBoolean("vibrateEnabled", false);
        animateMoves = settings.getBoolean("animateMoves", true);
        autoScrollTitle = settings.getBoolean("autoScrollTitle", true);
        setTitleScrolling();

        custom1ButtonActions.readPrefs(settings, actionFactory);
        custom2ButtonActions.readPrefs(settings, actionFactory);
        custom3ButtonActions.readPrefs(settings, actionFactory);
        updateButtons();

        guideShowOnStart = settings.getBoolean("guideShowOnStart", true);

        bookOptions.filename = settings.getString("bookFile", "");
        bookOptions.maxLength = getIntSetting("bookMaxLength", 1000000);
        bookOptions.preferMainLines = settings.getBoolean("bookPreferMainLines", false);
        bookOptions.tournamentMode = settings.getBoolean("bookTournamentMode", false);
        bookOptions.random = (settings.getInt("bookRandom", 500) - 500) * (3.0 / 500);
        setBookOptions();

        File extDir = Environment.getExternalStorageDirectory();
        String sep = File.separator;
        engineOptions.hashMB = getIntSetting("hashMB", 16);
        engineOptions.unSafeHash = new File(extDir + sep + engineDir + sep + ".unsafehash").exists();
        engineOptions.hints = settings.getBoolean("tbHints", false);
        engineOptions.hintsEdit = settings.getBoolean("tbHintsEdit", false);
        engineOptions.rootProbe = settings.getBoolean("tbRootProbe", true);
        engineOptions.engineProbe = settings.getBoolean("tbEngineProbe", true);

        String gtbPath = settings.getString("gtbPath", "").trim();
        if (gtbPath.length() == 0)
            gtbPath = extDir.getAbsolutePath() + sep + gtbDefaultDir;
        engineOptions.gtbPath = gtbPath;
        String gtbPathNet = settings.getString("gtbPathNet", "").trim();
        engineOptions.gtbPathNet = gtbPathNet;
        String rtbPath = settings.getString("rtbPath", "").trim();
        if (rtbPath.length() == 0)
            rtbPath = extDir.getAbsolutePath() + sep + rtbDefaultDir;
        engineOptions.rtbPath = rtbPath;
        String rtbPathNet = settings.getString("rtbPathNet", "").trim();
        engineOptions.rtbPathNet = rtbPathNet;

        setEngineOptions(false);
        setEgtbHints(cb.getSelectedSquare());

        updateThinkingInfo();

        pgnOptions.view.variations  = settings.getBoolean("viewVariations",     true);
        pgnOptions.view.comments    = settings.getBoolean("viewComments",       true);
        pgnOptions.view.nag         = settings.getBoolean("viewNAG",            true);
        pgnOptions.view.headers     = settings.getBoolean("viewHeaders",        false);
        final int oldViewPieceType = pgnOptions.view.pieceType;
        pgnOptions.view.pieceType   = getIntSetting("viewPieceType", PGNOptions.PT_LOCAL);
        showVariationLine           = settings.getBoolean("showVariationLine",  false);
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
        overrideViewAttribs();

        gameTextListener.clear();
        setPieceNames(pgnOptions.view.pieceType);
        ctrl.prefsChanged(oldViewPieceType != pgnOptions.view.pieceType);
        // update the typeset in case of a change anyway, cause it could occur
        // as well in rotation
        setFigurineNotation(pgnOptions.view.pieceType == PGNOptions.PT_FIGURINE, fontSize);

        showMaterialDiff = settings.getBoolean("materialDiff", false);
        secondTitleLine.setVisibility(showMaterialDiff ? View.VISIBLE : View.GONE);
    }

    private void overrideViewAttribs() {
        Util.overrideViewAttribs(findViewById(R.id.main));
    }

    /**
     * Change the Pieces into figurine or regular (i.e. letters) display
     */
    private final void setFigurineNotation(boolean displayAsFigures, int fontSize) {
        if (displayAsFigures) {
            // increase the font cause it has different kerning and looks small
            float increaseFontSize = fontSize * 1.1f;
            moveList.setTypeface(figNotation, increaseFontSize);
            thinking.setTypeface(figNotation);
            thinking.setTextSize(increaseFontSize);
        } else {
            moveList.setTypeface(null, fontSize);
            thinking.setTypeface(defaultThinkingListTypeFace);
            thinking.setTextSize(fontSize);
        }
    }

    /** Enable/disable title bar scrolling. */
    private final void setTitleScrolling() {
        TextUtils.TruncateAt where = autoScrollTitle ? TextUtils.TruncateAt.MARQUEE
                                                     : TextUtils.TruncateAt.END;
        whiteTitleText.setEllipsize(where);
        blackTitleText.setEllipsize(where);
        whiteFigText.setEllipsize(where);
        blackFigText.setEllipsize(where);
    }

    private final void updateButtons() {
        boolean largeButtons = settings.getBoolean("largeButtons", false);
        Resources r = getResources();
        int bWidth  = (int)Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, r.getDisplayMetrics()));
        int bHeight = (int)Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics()));
        if (largeButtons) {
            if (custom1ButtonActions.isEnabled() &&
                custom2ButtonActions.isEnabled() &&
                custom3ButtonActions.isEnabled()) {
                Configuration config = getResources().getConfiguration();
                if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    bWidth  = bWidth  * 6 / 5;
                    bHeight = bHeight * 6 / 5;
                } else {
                    bWidth  = bWidth  * 5 / 4;
                    bHeight = bHeight * 5 / 4;
                }
            } else {
                bWidth  = bWidth  * 3 / 2;
                bHeight = bHeight * 3 / 2;
            }
        }
        SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.touch);
        setButtonData(custom1Button, bWidth, bHeight, custom1ButtonActions.getIcon(), svg);
        setButtonData(custom2Button, bWidth, bHeight, custom2ButtonActions.getIcon(), svg);
        setButtonData(custom3Button, bWidth, bHeight, custom3ButtonActions.getIcon(), svg);
        setButtonData(modeButton, bWidth, bHeight, R.raw.mode, svg);
        setButtonData(undoButton, bWidth, bHeight, R.raw.left, svg);
        setButtonData(redoButton, bWidth, bHeight, R.raw.right, svg);
    }

    @SuppressWarnings("deprecation")
    private final void setButtonData(ImageButton button, int bWidth, int bHeight,
                                     int svgResId, SVG touched) {
        SVG svg = SVGParser.getSVGFromResource(getResources(), svgResId);
        button.setBackgroundDrawable(new SVGPictureDrawable(svg));

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, new SVGPictureDrawable(touched));
        button.setImageDrawable(sld);

        LayoutParams lp = button.getLayoutParams();
        lp.height = bHeight;
        lp.width = bWidth;
        button.setLayoutParams(lp);
        button.setPadding(0,0,0,0);
        button.setScaleType(ScaleType.FIT_XY);
    }

    @SuppressLint("Wakelock")
    private synchronized final void setWakeLock(boolean enableLock) {
        if (enableLock)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private final void setEngineStrength(String engine, int strength) {
        if (!storageAvailable()) {
            if (!"stockfish".equals(engine) && !"cuckoochess".equals(engine))
                engine = "stockfish";
        }
        ctrl.setEngineStrength(engine, strength);
        setEngineTitle(engine, strength);
    }

    private final void setEngineTitle(String engine, int strength) {
        String eName = "";
        if (EngineUtil.isOpenExchangeEngine(engine)) {
            String engineFileName = new File(engine).getName();
            ChessEngineResolver resolver = new ChessEngineResolver(this);
            List<ChessEngine> engines = resolver.resolveEngines();
            for (ChessEngine ce : engines) {
                if (EngineUtil.openExchangeFileName(ce).equals(engineFileName)) {
                    eName = ce.getName();
                    break;
                }
            }
        } else if (engine.contains("/")) {
            int idx = engine.lastIndexOf('/');
            eName = engine.substring(idx + 1);
        } else {
            eName = getString(engine.equals("cuckoochess") ?
                              R.string.cuckoochess_engine :
                              R.string.stockfish_engine);
            boolean analysis = (ctrl != null) && ctrl.analysisMode();
            if ((strength < 1000) && !analysis)
                eName = String.format(Locale.US, "%s: %d%%", eName, strength / 10);
        }
        engineTitleText.setText(eName);
    }

    /** Update center field in second header line. */
    public final void updateTimeControlTitle() {
        int[] tmpInfo = ctrl.getTimeLimit();
        StringBuilder sb = new StringBuilder();
        int tc = tmpInfo[0];
        int mps = tmpInfo[1];
        int inc = tmpInfo[2];
        if (mps > 0) {
            sb.append(mps);
            sb.append("/");
        }
        sb.append(timeToString(tc));
        if ((inc > 0) || (mps <= 0)) {
            sb.append("+");
            sb.append(tmpInfo[2] / 1000);
        }
        summaryTitleText.setText(sb.toString());
    }

    @Override
    public void updateEngineTitle() {
        String engine = settings.getString("engine", "stockfish");
        int strength = settings.getInt("strength", 1000);
        setEngineTitle(engine, strength);
    }

    @Override
    public void updateMaterialDifferenceTitle(Util.MaterialDiff diff) {
        whiteFigText.setText(diff.white);
        blackFigText.setText(diff.black);
    }

    private final void setBookOptions() {
        BookOptions options = new BookOptions(bookOptions);
        if (options.filename.length() > 0) {
            String sep = File.separator;
            if (!options.filename.startsWith(sep)) {
                File extDir = Environment.getExternalStorageDirectory();
                options.filename = extDir.getAbsolutePath() + sep + bookDir + sep + options.filename;
            }
        }
        ctrl.setBookOptions(options);
    }

    private boolean egtbForceReload = false;

    private final void setEngineOptions(boolean restart) {
        computeNetEngineID();
        ctrl.setEngineOptions(new EngineOptions(engineOptions), restart);
        Probe.getInstance().setPath(engineOptions.gtbPath, engineOptions.rtbPath,
                                    egtbForceReload);
        egtbForceReload = false;
    }

    private final void computeNetEngineID() {
        String id = "";
        try {
            String engine = settings.getString("engine", "stockfish");
            if (EngineUtil.isNetEngine(engine)) {
                String[] lines = Util.readFile(engine);
                if (lines.length >= 3)
                    id = lines[1] + ":" + lines[2];
            }
        } catch (IOException e) {
        }
        engineOptions.networkID = id;
    }

    private final void setEgtbHints(int sq) {
        if (!engineOptions.hints || (sq < 0)) {
            cb.setSquareDecorations(null);
            return;
        }

        Probe gtbProbe = Probe.getInstance();
        ArrayList<Pair<Integer,ProbeResult>> x = gtbProbe.movePieceProbe(cb.pos, sq);
        if (x == null) {
            cb.setSquareDecorations(null);
            return;
        }

        ArrayList<SquareDecoration> sd = new ArrayList<SquareDecoration>();
        for (Pair<Integer,ProbeResult> p : x)
            sd.add(new SquareDecoration(p.first, p.second));
        cb.setSquareDecorations(sd);
    }

    private class DrawerItem {
        int id;
        int itemId; // Item string resource id

        DrawerItem(int id, int itemId) {
            this.id = id;
            this.itemId = itemId;
        }

        @Override
        public String toString() {
            return getString(itemId);
        }
    }

    static private final int ITEM_NEW_GAME = 0;
    static private final int ITEM_EDIT_BOARD = 1;
    static private final int ITEM_SETTINGS = 2;
    static private final int ITEM_FILE_MENU = 3;
    static private final int ITEM_RESIGN = 4;
    static private final int ITEM_FORCE_MOVE = 5;
    static private final int ITEM_DRAW = 6;
    static private final int ITEM_SELECT_BOOK = 7;
    static private final int ITEM_MANAGE_ENGINES = 8;
    static private final int ITEM_SET_COLOR_THEME = 9;
    static private final int ITEM_ABOUT = 10;

    /** Initialize the drawer part of the user interface. */
    private void initDrawers() {
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        leftDrawer = (ListView)findViewById(R.id.left_drawer);
        rightDrawer = (ListView)findViewById(R.id.right_drawer);

        final DrawerItem[] leftItems = new DrawerItem[] {
            new DrawerItem(ITEM_EDIT_BOARD, R.string.option_edit_board),
            new DrawerItem(ITEM_FILE_MENU, R.string.option_file),
            new DrawerItem(ITEM_SELECT_BOOK, R.string.option_select_book),
            new DrawerItem(ITEM_MANAGE_ENGINES, R.string.option_manage_engines),
            new DrawerItem(ITEM_SET_COLOR_THEME, R.string.option_color_theme),
            new DrawerItem(ITEM_SETTINGS, R.string.option_settings),
            new DrawerItem(ITEM_ABOUT, R.string.option_about)
        };
        leftDrawer.setAdapter(new ArrayAdapter<DrawerItem>(this,
                                                           R.layout.drawer_list_item,
                                                           leftItems));
        leftDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                DrawerItem di = leftItems[position];
                handleDrawerSelection(di.id);
            }
        });

        final DrawerItem[] rightItems = new DrawerItem[] {
            new DrawerItem(ITEM_NEW_GAME, R.string.option_new_game),
            new DrawerItem(ITEM_RESIGN, R.string.option_resign_game),
            new DrawerItem(ITEM_FORCE_MOVE, R.string.option_force_computer_move),
            new DrawerItem(ITEM_DRAW, R.string.option_draw)
        };
        rightDrawer.setAdapter(new ArrayAdapter<DrawerItem>(this,
                                                            R.layout.drawer_list_item,
                                                            rightItems));
        rightDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                DrawerItem di = rightItems[position];
                handleDrawerSelection(di.id);
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        drawerLayout.openDrawer(Gravity.LEFT);
        return false;
    }

    /** React to a selection in the left/right drawers. */
    private void handleDrawerSelection(int itemId) {
        drawerLayout.closeDrawer(Gravity.LEFT);
        drawerLayout.closeDrawer(Gravity.RIGHT);
        leftDrawer.clearChoices();
        rightDrawer.clearChoices();

        setAutoMode(AutoMode.OFF);

        switch (itemId) {
        case ITEM_NEW_GAME:
            showDialog(NEW_GAME_DIALOG);
            break;
        case ITEM_EDIT_BOARD:
            startEditBoard(ctrl.getFEN());
            break;
        case ITEM_SETTINGS: {
            Intent i = new Intent(DroidFish.this, Preferences.class);
            startActivityForResult(i, RESULT_SETTINGS);
            break;
        }
        case ITEM_FILE_MENU:
            if (storageAvailable()) {
                removeDialog(FILE_MENU_DIALOG);
                showDialog(FILE_MENU_DIALOG);
            }
            break;
        case ITEM_RESIGN:
            if (ctrl.humansTurn())
                ctrl.resignGame();
            break;
        case ITEM_FORCE_MOVE:
            ctrl.stopSearch();
            break;
        case ITEM_DRAW:
            if (ctrl.humansTurn()) {
                if (ctrl.claimDrawIfPossible())
                    ctrl.stopPonder();
                else
                    Toast.makeText(getApplicationContext(), R.string.offer_draw, Toast.LENGTH_SHORT).show();
            }
            break;
        case ITEM_SELECT_BOOK:
            if (storageAvailable()) {
                removeDialog(SELECT_BOOK_DIALOG);
                showDialog(SELECT_BOOK_DIALOG);
            }
            break;
        case ITEM_MANAGE_ENGINES:
            if (storageAvailable()) {
                removeDialog(MANAGE_ENGINES_DIALOG);
                showDialog(MANAGE_ENGINES_DIALOG);
            } else {
                removeDialog(SELECT_ENGINE_DIALOG_NOMANAGE);
                showDialog(SELECT_ENGINE_DIALOG_NOMANAGE);
            }
            break;
        case ITEM_SET_COLOR_THEME:
            showDialog(SET_COLOR_THEME_DIALOG);
            break;
        case ITEM_ABOUT:
            showDialog(ABOUT_DIALOG);
            break;
        }
    }

    static private final int RESULT_EDITBOARD = 0;
    static private final int RESULT_SETTINGS = 1;
    static private final int RESULT_LOAD_PGN = 2;
    static private final int RESULT_LOAD_FEN = 3;
    static private final int RESULT_SELECT_SCID = 4;
    static private final int RESULT_OI_PGN_SAVE = 5;
    static private final int RESULT_OI_PGN_LOAD = 6;
    static private final int RESULT_OI_FEN_LOAD = 7;
    static private final int RESULT_GET_FEN = 8;
    static private final int RESULT_EDITOPTIONS = 9;

    private void startEditBoard(String fen) {
        Intent i = new Intent(DroidFish.this, EditBoard.class);
        i.setAction(fen);
        startActivityForResult(i, RESULT_EDITBOARD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case RESULT_SETTINGS:
            handlePrefsChange();
            break;
        case RESULT_EDITBOARD:
            if (resultCode == RESULT_OK) {
                try {
                    String fen = data.getAction();
                    ctrl.setFENOrPGN(fen);
                    setBoardFlip(false);
                } catch (ChessParseError e) {
                }
            }
            break;
        case RESULT_LOAD_PGN:
            if (resultCode == RESULT_OK) {
                try {
                    String pgn = data.getAction();
                    int modeNr = ctrl.getGameMode().getModeNr();
                    if ((modeNr != GameMode.ANALYSIS) && (modeNr != GameMode.EDIT_GAME))
                        newGameMode(GameMode.EDIT_GAME);
                    ctrl.setFENOrPGN(pgn);
                    setBoardFlip(true);
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
        case RESULT_OI_PGN_LOAD:
            if (resultCode == RESULT_OK) {
                String pathName = getFilePathFromUri(data.getData());
                if (pathName != null)
                    loadPGNFromFile(pathName);
            }
            break;
        case RESULT_OI_PGN_SAVE:
            if (resultCode == RESULT_OK) {
                String pathName = getFilePathFromUri(data.getData());
                if (pathName != null) {
                    if ((pathName.length() > 0) && !pathName.contains("."))
                        pathName += ".pgn";
                    savePGNToFile(pathName, false);
                }
            }
            break;
        case RESULT_OI_FEN_LOAD:
            if (resultCode == RESULT_OK) {
                String pathName = getFilePathFromUri(data.getData());
                if (pathName != null)
                    loadFENFromFile(pathName);
            }
            break;
        case RESULT_GET_FEN:
            if (resultCode == RESULT_OK) {
                String fen = data.getStringExtra(Intent.EXTRA_TEXT);
                if (fen == null) {
                    String pathName = getFilePathFromUri(data.getData());
                    loadFENFromFile(pathName);
                }
                setFenHelper(fen);
            }
            break;
        case RESULT_LOAD_FEN:
            if (resultCode == RESULT_OK) {
                String fen = data.getAction();
                setFenHelper(fen);
            }
            break;
        case RESULT_EDITOPTIONS:
            if (resultCode == RESULT_OK) {
                @SuppressWarnings("unchecked")
                Map<String,String> uciOpts =
                    (Map<String,String>)data.getSerializableExtra("org.petero.droidfish.ucioptions");
                ctrl.setEngineUCIOptions(uciOpts);
            }
            break;
        }
    }

    /** Set new game mode. */
    private final void newGameMode(int gameModeType) {
        Editor editor = settings.edit();
        String gameModeStr = String.format(Locale.US, "%d", gameModeType);
        editor.putString("gameMode", gameModeStr);
        editor.commit();
        gameMode = new GameMode(gameModeType);
        maybeAutoModeOff(gameMode);
        ctrl.setGameMode(gameMode);
    }

    public static String getFilePathFromUri(Uri uri) {
        if (uri == null)
            return null;
        return uri.getPath();
    }

    private final String getParseErrString(ChessParseError e) {
        if (e.resourceId == -1)
            return e.getMessage();
        else
            return getString(e.resourceId);
    }

    private final int nameMatchScore(String name, String match) {
        if (name == null)
            return 0;
        String lName = name.toLowerCase(Locale.US);
        String lMatch = match.toLowerCase(Locale.US);
        if (name.equals(match))
            return 6;
        if (lName.equals(lMatch))
            return 5;
        if (name.startsWith(match))
            return 4;
        if (lName.startsWith(lMatch))
            return 3;
        if (name.contains(match))
            return 2;
        if (lName.contains(lMatch))
            return 1;
        return 0;
    }

    private final void setBoardFlip() {
        setBoardFlip(false);
    }

    /** Set a boolean preference setting. */
    private final void setBooleanPref(String name, boolean value) {
        Editor editor = settings.edit();
        editor.putBoolean(name, value);
        editor.commit();
    }

    /** Toggle a boolean preference setting. Return new value. */
    private final boolean toggleBooleanPref(String name) {
        boolean value = !settings.getBoolean(name, false);
        setBooleanPref(name, value);
        return value;
    }

    private final void setBoardFlip(boolean matchPlayerNames) {
        boolean flipped = boardFlipped;
        if (playerNameFlip && matchPlayerNames && (ctrl != null)) {
            final TreeMap<String,String> headers = new TreeMap<String,String>();
            ctrl.getHeaders(headers);
            int whiteMatch = nameMatchScore(headers.get("White"), playerName);
            int blackMatch = nameMatchScore(headers.get("Black"), playerName);
            if (( flipped && (whiteMatch > blackMatch)) ||
                (!flipped && (whiteMatch < blackMatch))) {
                flipped = !flipped;
                boardFlipped = flipped;
                setBooleanPref("boardFlipped", flipped);
            }
        }
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
        cb.setSelection(cb.highlightLastMove ? sq : -1);
        cb.userSelectedSquare = false;
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
        moveList.setText(gameTextListener.getText());
        int currPos = gameTextListener.getCurrPos();
        int line = moveList.getLineForOffset(currPos);
        if (line >= 0) {
            int y = (line - 1) * moveList.getLineHeight();
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
    public Context getContext() {
        return this;
    }

    @Override
    public String playerName() {
        return playerName;
    }

    @Override
    public boolean discardVariations() {
        return discardVariations;
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
    public void setThinkingInfo(ThinkingInfo ti) {
        thinkingStr1 = ti.pvStr;
        thinkingStr2 = ti.statStr;
        bookInfoStr = ti.bookInfo;
        this.pvMoves = ti.pvMoves;
        this.bookMoves = ti.bookMoves;
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
            s += Util.boldStart + getString(R.string.book) + Util.boldStop + bookInfoStr;
            thinking.append(Html.fromHtml(s));
            thinkingEmpty = false;
        }
        if (showVariationLine && (variantStr.indexOf(' ') >= 0)) {
            String s = "";
            if (!thinkingEmpty)
                s += "<br>";
            s += Util.boldStart + getString(R.string.variation) + Util.boldStop + variantStr;
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
        if (((hints == null) || hints.isEmpty()) &&
            (variantMoves != null) && variantMoves.size() > 1) {
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
    static private final int SELECT_BOOK_DIALOG = 4;
    static private final int SELECT_ENGINE_DIALOG = 5;
    static private final int SELECT_ENGINE_DIALOG_NOMANAGE = 6;
    static private final int SELECT_PGN_FILE_DIALOG = 7;
    static private final int SELECT_PGN_FILE_SAVE_DIALOG = 8;
    static private final int SET_COLOR_THEME_DIALOG = 9;
    static private final int GAME_MODE_DIALOG = 10;
    static private final int SELECT_PGN_SAVE_NEWFILE_DIALOG = 11;
    static private final int MOVELIST_MENU_DIALOG = 12;
    static private final int THINKING_MENU_DIALOG = 13;
    static private final int GO_BACK_MENU_DIALOG = 14;
    static private final int GO_FORWARD_MENU_DIALOG = 15;
    static private final int FILE_MENU_DIALOG = 16;
    static private final int NEW_GAME_DIALOG = 17;
    static private final int CUSTOM1_BUTTON_DIALOG = 18;
    static private final int CUSTOM2_BUTTON_DIALOG = 19;
    static private final int CUSTOM3_BUTTON_DIALOG = 20;
    static private final int MANAGE_ENGINES_DIALOG = 21;
    static private final int NETWORK_ENGINE_DIALOG = 22;
    static private final int NEW_NETWORK_ENGINE_DIALOG = 23;
    static private final int NETWORK_ENGINE_CONFIG_DIALOG = 24;
    static private final int DELETE_NETWORK_ENGINE_DIALOG = 25;
    static private final int CLIPBOARD_DIALOG = 26;
    static private final int SELECT_FEN_FILE_DIALOG = 27;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case NEW_GAME_DIALOG:                return newGameDialog();
        case PROMOTE_DIALOG:                 return promoteDialog();
        case BOARD_MENU_DIALOG:              return boardMenuDialog();
        case FILE_MENU_DIALOG:               return fileMenuDialog();
        case ABOUT_DIALOG:                   return aboutDialog();
        case SELECT_BOOK_DIALOG:             return selectBookDialog();
        case SELECT_ENGINE_DIALOG:           return selectEngineDialog(false);
        case SELECT_ENGINE_DIALOG_NOMANAGE:  return selectEngineDialog(true);
        case SELECT_PGN_FILE_DIALOG:         return selectPgnFileDialog();
        case SELECT_PGN_FILE_SAVE_DIALOG:    return selectPgnFileSaveDialog();
        case SELECT_PGN_SAVE_NEWFILE_DIALOG: return selectPgnSaveNewFileDialog();
        case SET_COLOR_THEME_DIALOG:         return setColorThemeDialog();
        case GAME_MODE_DIALOG:               return gameModeDialog();
        case MOVELIST_MENU_DIALOG:           return moveListMenuDialog();
        case THINKING_MENU_DIALOG:           return thinkingMenuDialog();
        case GO_BACK_MENU_DIALOG:            return goBackMenuDialog();
        case GO_FORWARD_MENU_DIALOG:         return goForwardMenuDialog();
        case CUSTOM1_BUTTON_DIALOG:          return makeButtonDialog(custom1ButtonActions);
        case CUSTOM2_BUTTON_DIALOG:          return makeButtonDialog(custom2ButtonActions);
        case CUSTOM3_BUTTON_DIALOG:          return makeButtonDialog(custom3ButtonActions);
        case MANAGE_ENGINES_DIALOG:          return manageEnginesDialog();
        case NETWORK_ENGINE_DIALOG:          return networkEngineDialog();
        case NEW_NETWORK_ENGINE_DIALOG:      return newNetworkEngineDialog();
        case NETWORK_ENGINE_CONFIG_DIALOG:   return networkEngineConfigDialog();
        case DELETE_NETWORK_ENGINE_DIALOG:   return deleteNetworkEngineDialog();
        case CLIPBOARD_DIALOG:               return clipBoardDialog();
        case SELECT_FEN_FILE_DIALOG:         return selectFenFileDialog();
        }
        return null;
    }

    private final Dialog newGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.option_new_game);
        builder.setMessage(R.string.start_new_game);
        builder.setNeutralButton(R.string.yes, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startNewGame(2);
            }
        });
        builder.setNegativeButton(R.string.white, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startNewGame(0);
            }
        });
        builder.setPositiveButton(R.string.black, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startNewGame(1);
            }
        });
        return builder.create();
    }

    private final void startNewGame(int type) {
        if (type != 2) {
            int gameModeType = (type == 0) ? GameMode.PLAYER_WHITE : GameMode.PLAYER_BLACK;
            Editor editor = settings.edit();
            String gameModeStr = String.format(Locale.US, "%d", gameModeType);
            editor.putString("gameMode", gameModeStr);
            editor.commit();
            gameMode = new GameMode(gameModeType);
        }
//        savePGNToFile(".autosave.pgn", true);
        TimeControlData tcData = new TimeControlData();
        tcData.setTimeControl(timeControl, movesPerSession, timeIncrement);
        ctrl.newGame(gameMode, tcData);
        ctrl.startGame();
        setBoardFlip(true);
        updateEngineTitle();
    }

    private final Dialog promoteDialog() {
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

    private final Dialog clipBoardDialog() {
        final int COPY_GAME      = 0;
        final int COPY_POSITION  = 1;
        final int PASTE          = 2;

        setAutoMode(AutoMode.OFF);
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.copy_game));     actions.add(COPY_GAME);
        lst.add(getString(R.string.copy_position)); actions.add(COPY_POSITION);
        lst.add(getString(R.string.paste));         actions.add(PASTE);
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tools_menu);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case COPY_GAME: {
                    String pgn = ctrl.getPGN();
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(new ClipData("DroidFish game",
                            new String[]{ "application/x-chess-pgn", ClipDescription.MIMETYPE_TEXT_PLAIN },
                            new ClipData.Item(pgn)));
                    break;
                }
                case COPY_POSITION: {
                    String fen = ctrl.getFEN() + "\n";
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(new ClipData(fen,
                            new String[]{ "application/x-chess-fen", ClipDescription.MIMETYPE_TEXT_PLAIN },
                            new ClipData.Item(fen)));
                    break;
                }
                case PASTE: {
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()) {
                        ClipData clip = clipboard.getPrimaryClip();
                        StringBuilder fenPgn = new StringBuilder();
                        for (int i = 0; i < clip.getItemCount(); i++)
                            fenPgn.append(clip.getItemAt(i).coerceToText(getApplicationContext()));
                        try {
                            ctrl.setFENOrPGN(fenPgn.toString());
                            setBoardFlip(true);
                        } catch (ChessParseError e) {
                            Toast.makeText(getApplicationContext(), getParseErrString(e), Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                }
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog boardMenuDialog() {
        final int CLIPBOARD = 0;
        final int FILEMENU  = 1;
        final int SHARE     = 2;
        final int GET_FEN   = 3;

        setAutoMode(AutoMode.OFF);
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.clipboard));     actions.add(CLIPBOARD);
        if (storageAvailable()) {
            lst.add(getString(R.string.option_file));   actions.add(FILEMENU);
        }
        lst.add(getString(R.string.share));         actions.add(SHARE);
        if (hasFenProvider(getPackageManager())) {
            lst.add(getString(R.string.get_fen)); actions.add(GET_FEN);
        }
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tools_menu);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case CLIPBOARD: {
                    showDialog(CLIPBOARD_DIALOG);
                    break;
                }
                case FILEMENU: {
                    removeDialog(FILE_MENU_DIALOG);
                    showDialog(FILE_MENU_DIALOG);
                    break;
                }
                case SHARE: {
                    shareGame();
                    break;
                }
                case GET_FEN:
                    getFen();
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final void shareGame() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, ctrl.getPGN());
        try {
            startActivity(Intent.createChooser(i, getString(R.string.share_pgn_game)));
        } catch (ActivityNotFoundException ex) {
            // Ignore
        }
    }

    private final Dialog fileMenuDialog() {
        final int LOAD_LAST_FILE = 0;
        final int LOAD_GAME      = 1;
        final int LOAD_POS       = 2;
        final int LOAD_SCID_GAME = 3;
        final int SAVE_GAME      = 4;

        setAutoMode(AutoMode.OFF);
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        if (currFileType() != FT_NONE) {
            lst.add(getString(R.string.load_last_file)); actions.add(LOAD_LAST_FILE);
        }
        lst.add(getString(R.string.load_game));     actions.add(LOAD_GAME);
        lst.add(getString(R.string.load_position)); actions.add(LOAD_POS);
        if (hasScidProvider()) {
            lst.add(getString(R.string.load_scid_game)); actions.add(LOAD_SCID_GAME);
        }
        lst.add(getString(R.string.save_game));     actions.add(SAVE_GAME);
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.load_save_menu);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case LOAD_LAST_FILE:
                    loadLastFile();
                    break;
                case LOAD_GAME:
                    selectFile(R.string.select_pgn_file, R.string.pgn_load, "currentPGNFile", pgnDir,
                                  SELECT_PGN_FILE_DIALOG, RESULT_OI_PGN_LOAD);
                    break;
                case SAVE_GAME:
                    selectFile(R.string.select_pgn_file_save, R.string.pgn_save, "currentPGNFile", pgnDir,
                                  SELECT_PGN_FILE_SAVE_DIALOG, RESULT_OI_PGN_SAVE);
                    break;
                case LOAD_POS:
                    selectFile(R.string.select_fen_file, R.string.pgn_load, "currentFENFile", fenDir,
                                  SELECT_FEN_FILE_DIALOG, RESULT_OI_FEN_LOAD);
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

    /** Open dialog to select a game/position from the last used file. */
    final private void loadLastFile() {
        String path = currPathName();
        if (path.length() == 0)
            return;
        setAutoMode(AutoMode.OFF);
        switch (currFileType()) {
        case FT_PGN:
            loadPGNFromFile(path);
            break;
        case FT_SCID: {
            Intent data = new Intent(path);
            onActivityResult(RESULT_SELECT_SCID, RESULT_OK, data);
            break;
        }
        case FT_FEN:
            loadFENFromFile(path);
            break;
        }
    }

    private final Dialog aboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = getString(R.string.app_name);
        WebView wv = new WebView(this);
        builder.setView(wv);
        InputStream is = getResources().openRawResource(R.raw.about);
        String data = Util.readFromStream(is);
        if (data == null)
            data = "";
        try { is.close(); } catch (IOException e1) {}
        wv.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo("org.petero.droidfish", 0);
            title += " " + pi.versionName;
        } catch (NameNotFoundException e) {
        }
        builder.setTitle(title);
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog selectBookDialog() {
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

    private final static boolean reservedEngineName(String name) {
        return "cuckoochess".equals(name) ||
               "stockfish".equals(name) ||
               name.endsWith(".ini");
    }

    private final Dialog selectEngineDialog(final boolean abortOnCancel) {
        final ArrayList<String> items = new ArrayList<String>();
        final ArrayList<String> ids = new ArrayList<String>();
        ids.add("stockfish"); items.add(getString(R.string.stockfish_engine));
        ids.add("cuckoochess"); items.add(getString(R.string.cuckoochess_engine));

        if (storageAvailable()) {
            final String sep = File.separator;
            final String base = Environment.getExternalStorageDirectory() + sep + engineDir + sep;
            {
                ChessEngineResolver resolver = new ChessEngineResolver(this);
                List<ChessEngine> engines = resolver.resolveEngines();
                ArrayList<Pair<String,String>> oexEngines = new ArrayList<Pair<String,String>>();
                for (ChessEngine engine : engines) {
                    if ((engine.getName() != null) && (engine.getFileName() != null) &&
                            (engine.getPackageName() != null)) {
                        oexEngines.add(new Pair<String,String>(EngineUtil.openExchangeFileName(engine),
                                engine.getName()));
                    }
                }
                Collections.sort(oexEngines, new Comparator<Pair<String,String>>() {
                    @Override
                    public int compare(Pair<String, String> lhs, Pair<String, String> rhs) {
                        return lhs.second.compareTo(rhs.second);
                    }
                });
                for (Pair<String,String> eng : oexEngines) {
                    ids.add(base + EngineUtil.openExchangeDir + sep + eng.first);
                    items.add(eng.second);
                }
            }

            String[] fileNames = findFilesInDirectory(engineDir, new FileNameFilter() {
                @Override
                public boolean accept(String filename) {
                    return !reservedEngineName(filename);
                }
            });
            for (String file : fileNames) {
                ids.add(base + file);
                items.add(file);
            }
        }

        String currEngine = ctrl.getEngine();
        int defaultItem = 0;
        final int nEngines = items.size();
        for (int i = 0; i < nEngines; i++) {
            if (ids.get(i).equals(currEngine)) {
                defaultItem = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_chess_engine);
        builder.setSingleChoiceItems(items.toArray(new String[0]), defaultItem,
                                     new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if ((item < 0) || (item >= nEngines))
                    return;
                Editor editor = settings.edit();
                String engine = ids.get(item);
                editor.putString("engine", engine);
                editor.commit();
                dialog.dismiss();
                int strength = settings.getInt("strength", 1000);
                setEngineOptions(false);
                setEngineStrength(engine, strength);
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!abortOnCancel) {
                    removeDialog(MANAGE_ENGINES_DIALOG);
                    showDialog(MANAGE_ENGINES_DIALOG);
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private static interface Loader {
        void load(String pathName);
    }

    private final Dialog selectPgnFileDialog() {
        return selectFileDialog(pgnDir, R.string.select_pgn_file, R.string.no_pgn_files,
                                "currentPGNFile", new Loader() {
            @Override
            public void load(String pathName) {
                loadPGNFromFile(pathName);
            }
        });
    }

    private final Dialog selectFenFileDialog() {
        return selectFileDialog(fenDir, R.string.select_fen_file, R.string.no_fen_files,
                                "currentFENFile", new Loader() {
            @Override
            public void load(String pathName) {
                loadFENFromFile(pathName);
            }
        });
    }

    private final Dialog selectFileDialog(final String defaultDir, int selectFileMsg, int noFilesMsg,
                                          String settingsName, final Loader loader) {
        setAutoMode(AutoMode.OFF);
        final String[] fileNames = findFilesInDirectory(defaultDir, null);
        final int numFiles = fileNames.length;
        if (numFiles == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name).setMessage(noFilesMsg);
            AlertDialog alert = builder.create();
            return alert;
        }
        int defaultItem = 0;
        String currentFile = settings.getString(settingsName, "");
        currentFile = new File(currentFile).getName();
        for (int i = 0; i < numFiles; i++) {
            if (currentFile.equals(fileNames[i])) {
                defaultItem = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectFileMsg);
        builder.setSingleChoiceItems(fileNames, defaultItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                String sep = File.separator;
                String fn = fileNames[item].toString();
                String pathName = Environment.getExternalStorageDirectory() + sep + defaultDir + sep + fn;
                loader.load(pathName);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog selectPgnFileSaveDialog() {
        setAutoMode(AutoMode.OFF);
        final String[] fileNames = findFilesInDirectory(pgnDir, null);
        final int numFiles = fileNames.length;
        int defaultItem = 0;
        String currentPGNFile = settings.getString("currentPGNFile", "");
        currentPGNFile = new File(currentPGNFile).getName();
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
                    dialog.dismiss();
                    pgnFile = fileNames[item].toString();
                    String sep = File.separator;
                    String pathName = Environment.getExternalStorageDirectory() + sep + pgnDir + sep + pgnFile;
                    savePGNToFile(pathName, false);
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog selectPgnSaveNewFileDialog() {
        setAutoMode(AutoMode.OFF);
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
                String sep = File.separator;
                String pathName = Environment.getExternalStorageDirectory() + sep + pgnDir + sep + pgnFile;
                savePGNToFile(pathName, false);
            }
        };
        builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
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

    private final Dialog setColorThemeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_color_theme);
        String[] themeNames = new String[ColorTheme.themeNames.length];
        for (int i = 0; i < themeNames.length; i++)
            themeNames[i] = getString(ColorTheme.themeNames[i]);
        builder.setSingleChoiceItems(themeNames, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                ColorTheme.instance().setTheme(settings, item);
                cb.setColors();
                gameTextListener.clear();
                ctrl.prefsChanged(false);
                dialog.dismiss();
                overrideViewAttribs();
            }
        });
        return builder.create();
    }

    private final Dialog gameModeDialog() {
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
                /* only flip site in case the player was specified resp. changed */
                boolean flipSite = false;
                switch (item) {
                case 0: gameModeType = GameMode.ANALYSIS;      break;
                case 1: gameModeType = GameMode.EDIT_GAME;     break;
                case 2: gameModeType = GameMode.PLAYER_WHITE; flipSite = true; break;
                case 3: gameModeType = GameMode.PLAYER_BLACK; flipSite = true; break;
                case 4: gameModeType = GameMode.TWO_PLAYERS;   break;
                case 5: gameModeType = GameMode.TWO_COMPUTERS; break;
                default: break;
                }
                dialog.dismiss();
                if (gameModeType >= 0) {
                    Editor editor = settings.edit();
                    String gameModeStr = String.format(Locale.US, "%d", gameModeType);
                    editor.putString("gameMode", gameModeStr);
                    editor.commit();
                    gameMode = new GameMode(gameModeType);
                    maybeAutoModeOff(gameMode);
                    ctrl.setGameMode(gameMode);
                    setBoardFlip(flipSite);
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog moveListMenuDialog() {
        final int EDIT_HEADERS   = 0;
        final int EDIT_COMMENTS  = 1;
        final int REMOVE_SUBTREE = 2;
        final int MOVE_VAR_UP    = 3;
        final int MOVE_VAR_DOWN  = 4;
        final int ADD_NULL_MOVE  = 5;

        setAutoMode(AutoMode.OFF);
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.edit_headers));      actions.add(EDIT_HEADERS);
        if (ctrl.humansTurn()) {
            lst.add(getString(R.string.edit_comments)); actions.add(EDIT_COMMENTS);
        }
        lst.add(getString(R.string.truncate_gametree)); actions.add(REMOVE_SUBTREE);
        if (ctrl.canMoveVariationUp()) {
            lst.add(getString(R.string.move_var_up));   actions.add(MOVE_VAR_UP);
        }
        if (ctrl.canMoveVariationDown()) {
            lst.add(getString(R.string.move_var_down)); actions.add(MOVE_VAR_DOWN);
        }

        boolean allowNullMove =
            (gameMode.analysisMode() ||
             (gameMode.playerWhite() && gameMode.playerBlack() && !gameMode.clocksActive())) &&
             !ctrl.inCheck();
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
                    builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            headers.put("Event", event.getText().toString().trim());
                            headers.put("Site",  site .getText().toString().trim());
                            headers.put("Date",  date .getText().toString().trim());
                            headers.put("Round", round.getText().toString().trim());
                            headers.put("White", white.getText().toString().trim());
                            headers.put("Black", black.getText().toString().trim());
                            ctrl.setHeaders(headers);
                            setBoardFlip(true);
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
                        nagStr = String.format(Locale.US, "%d", commInfo.nag);
                    nag.setText(nagStr);

                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
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
                moveListMenuDlg = null;
            }
        });
        AlertDialog alert = builder.create();
        moveListMenuDlg = alert;
        return alert;
    }

    private final Dialog thinkingMenuDialog() {
        final int ADD_ANALYSIS = 0;
        final int MULTIPV_DEC = 1;
        final int MULTIPV_INC = 2;
        final int HIDE_STATISTICS = 3;
        final int SHOW_STATISTICS = 4;
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.add_analysis)); actions.add(ADD_ANALYSIS);
        int numPV = this.numPV;
        if (gameMode.analysisMode()) {
            int maxPV = ctrl.maxPV();
            numPV = Math.min(numPV, maxPV);
            numPV = Math.max(numPV, 1);
            if (numPV > 1) {
                lst.add(getString(R.string.fewer_variations)); actions.add(MULTIPV_DEC);
            }
            if (numPV < maxPV) {
                lst.add(getString(R.string.more_variations)); actions.add(MULTIPV_INC);
            }
        }
        final int numPVF = numPV;
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
                    setMultiPVMode(numPVF - 1);
                    break;
                case MULTIPV_INC:
                    setMultiPVMode(numPVF + 1);
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

    private void setMultiPVMode(int nPV) {
        numPV = nPV;
        Editor editor = settings.edit();
        editor.putInt("numPV", numPV);
        editor.commit();
        ctrl.setMultiPVMode(numPV);
    }

    private final Dialog goBackMenuDialog() {
        final int GOTO_START_GAME = 0;
        final int GOTO_START_VAR  = 1;
        final int GOTO_PREV_VAR   = 2;
        final int LOAD_PREV_GAME  = 3;
        final int AUTO_BACKWARD   = 4;

        setAutoMode(AutoMode.OFF);
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.goto_start_game));      actions.add(GOTO_START_GAME);
        lst.add(getString(R.string.goto_start_variation)); actions.add(GOTO_START_VAR);
        if (ctrl.currVariation() > 0) {
            lst.add(getString(R.string.goto_prev_variation)); actions.add(GOTO_PREV_VAR);
        }
        final int currFT = currFileType();
        final String currPathName = currPathName();
        if ((currFT != FT_NONE) && !gameMode.clocksActive()) {
            lst.add(getString(R.string.load_prev_game)); actions.add(LOAD_PREV_GAME);
        }
        if (!gameMode.clocksActive()) {
            lst.add(getString(R.string.auto_backward)); actions.add(AUTO_BACKWARD);
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
                    Intent i;
                    if (currFT == FT_PGN) {
                        i = new Intent(DroidFish.this, EditPGNLoad.class);
                        i.setAction("org.petero.droidfish.loadFilePrevGame");
                        i.putExtra("org.petero.droidfish.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_PGN);
                    } else if (currFT == FT_SCID) {
                        i = new Intent(DroidFish.this, LoadScid.class);
                        i.setAction("org.petero.droidfish.loadScidPrevGame");
                        i.putExtra("org.petero.droidfish.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_PGN);
                    } else if (currFT == FT_FEN) {
                        i = new Intent(DroidFish.this, LoadFEN.class);
                        i.setAction("org.petero.droidfish.loadPrevFen");
                        i.putExtra("org.petero.droidfish.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_FEN);
                    }
                    break;
                case AUTO_BACKWARD:
                    setAutoMode(AutoMode.BACKWARD);
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private final Dialog goForwardMenuDialog() {
        final int GOTO_END_VAR   = 0;
        final int GOTO_NEXT_VAR  = 1;
        final int LOAD_NEXT_GAME = 2;
        final int AUTO_FORWARD   = 3;

        setAutoMode(AutoMode.OFF);
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.goto_end_variation)); actions.add(GOTO_END_VAR);
        if (ctrl.currVariation() < ctrl.numVariations() - 1) {
            lst.add(getString(R.string.goto_next_variation)); actions.add(GOTO_NEXT_VAR);
        }
        final int currFT = currFileType();
        final String currPathName = currPathName();
        if ((currFT != FT_NONE) && !gameMode.clocksActive()) {
            lst.add(getString(R.string.load_next_game)); actions.add(LOAD_NEXT_GAME);
        }
        if (!gameMode.clocksActive()) {
            lst.add(getString(R.string.auto_forward)); actions.add(AUTO_FORWARD);
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
                    Intent i;
                    if (currFT == FT_PGN) {
                        i = new Intent(DroidFish.this, EditPGNLoad.class);
                        i.setAction("org.petero.droidfish.loadFileNextGame");
                        i.putExtra("org.petero.droidfish.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_PGN);
                    } else if (currFT == FT_SCID) {
                        i = new Intent(DroidFish.this, LoadScid.class);
                        i.setAction("org.petero.droidfish.loadScidNextGame");
                        i.putExtra("org.petero.droidfish.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_PGN);
                    } else if (currFT == FT_FEN) {
                        i = new Intent(DroidFish.this, LoadFEN.class);
                        i.setAction("org.petero.droidfish.loadNextFen");
                        i.putExtra("org.petero.droidfish.pathname", currPathName);
                        startActivityForResult(i, RESULT_LOAD_FEN);
                    }
                    break;
                case AUTO_FORWARD:
                    setAutoMode(AutoMode.FORWARD);
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private Dialog makeButtonDialog(ButtonActions buttonActions) {
        List<CharSequence> names = new ArrayList<CharSequence>();
        final List<UIAction> actions = new ArrayList<UIAction>();

        HashSet<String> used = new HashSet<String>();
        for (UIAction a : buttonActions.getMenuActions()) {
            if ((a != null) && a.enabled() && !used.contains(a.getId())) {
                names.add(getString(a.getName()));
                actions.add(a);
                used.add(a.getId());
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(buttonActions.getMenuTitle());
        builder.setItems(names.toArray(new CharSequence[names.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                UIAction a = actions.get(item);
                a.run();
            }
        });
        return builder.create();
    }

    private final Dialog manageEnginesDialog() {
        final int SELECT_ENGINE = 0;
        final int SET_ENGINE_OPTIONS = 1;
        final int CONFIG_NET_ENGINE = 2;
        List<CharSequence> lst = new ArrayList<CharSequence>();
        List<Integer> actions = new ArrayList<Integer>();
        lst.add(getString(R.string.select_engine)); actions.add(SELECT_ENGINE);
        if (canSetEngineOptions()) {
            lst.add(getString(R.string.set_engine_options));
            actions.add(SET_ENGINE_OPTIONS);
        }
        lst.add(getString(R.string.configure_network_engine)); actions.add(CONFIG_NET_ENGINE);
        final List<Integer> finalActions = actions;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.option_manage_engines);
        builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (finalActions.get(item)) {
                case SELECT_ENGINE:
                    removeDialog(SELECT_ENGINE_DIALOG);
                    showDialog(SELECT_ENGINE_DIALOG);
                    break;
                case SET_ENGINE_OPTIONS:
                    setEngineOptions();
                    break;
                case CONFIG_NET_ENGINE:
                    removeDialog(NETWORK_ENGINE_DIALOG);
                    showDialog(NETWORK_ENGINE_DIALOG);
                    break;
                }
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    /** Return true if engine UCI options can be set now. */
    private final boolean canSetEngineOptions() {
        if (!storageAvailable())
            return false;
        UCIOptions uciOpts = ctrl.getUCIOptions();
        if (uciOpts == null)
            return false;
        for (String name : uciOpts.getOptionNames())
            if (uciOpts.getOption(name).visible)
                return true;
        return false;
    }

    /** Start activity to set engine options. */
    private final void setEngineOptions() {
        Intent i = new Intent(DroidFish.this, EditOptions.class);
        UCIOptions uciOpts = ctrl.getUCIOptions();
        if (uciOpts != null) {
            i.putExtra("org.petero.droidfish.ucioptions", uciOpts);
            i.putExtra("org.petero.droidfish.enginename", engineTitleText.getText());
            startActivityForResult(i, RESULT_EDITOPTIONS);
        }
    }

    private final Dialog networkEngineDialog() {
        String[] fileNames = findFilesInDirectory(engineDir, new FileNameFilter() {
            @Override
            public boolean accept(String filename) {
                if (reservedEngineName(filename))
                    return false;
                return EngineUtil.isNetEngine(filename);
            }
        });
        final int numFiles = fileNames.length;
        final int numItems = numFiles + 1;
        final String[] items = new String[numItems];
        final String[] ids = new String[numItems];
        int idx = 0;
        String sep = File.separator;
        String base = Environment.getExternalStorageDirectory() + sep + engineDir + sep;
        for (int i = 0; i < numFiles; i++) {
            ids[idx] = base + fileNames[i];
            items[idx] = fileNames[i];
            idx++;
        }
        ids[idx] = ""; items[idx] = getString(R.string.new_engine); idx++;
        String currEngine = ctrl.getEngine();
        int defaultItem = 0;
        for (int i = 0; i < numItems; i++)
            if (ids[i].equals(currEngine)) {
                defaultItem = i;
                break;
            }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.configure_network_engine);
        builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if ((item < 0) || (item >= numItems))
                    return;
                dialog.dismiss();
                if (item == numItems - 1) {
                    showDialog(NEW_NETWORK_ENGINE_DIALOG);
                } else {
                    networkEngineToConfig = ids[item];
                    removeDialog(NETWORK_ENGINE_CONFIG_DIALOG);
                    showDialog(NETWORK_ENGINE_CONFIG_DIALOG);
                }
            }
        });
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                removeDialog(MANAGE_ENGINES_DIALOG);
                showDialog(MANAGE_ENGINES_DIALOG);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    // Filename of network engine to configure
    private String networkEngineToConfig = "";

    // Ask for name of new network engine
    private final Dialog newNetworkEngineDialog() {
        View content = View.inflate(this, R.layout.create_network_engine, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(content);
        builder.setTitle(R.string.create_network_engine);
        final EditText engineNameView = (EditText)content.findViewById(R.id.create_network_engine);
        engineNameView.setText("");
        final Runnable createEngine = new Runnable() {
            public void run() {
                String engineName = engineNameView.getText().toString();
                String sep = File.separator;
                String pathName = Environment.getExternalStorageDirectory() + sep + engineDir + sep + engineName;
                File file = new File(pathName);
                boolean nameOk = true;
                int errMsg = -1;
                if (engineName.contains("/")) {
                    nameOk = false;
                    errMsg = R.string.slash_not_allowed;
                } else if (reservedEngineName(engineName) || file.exists()) {
                    nameOk = false;
                    errMsg = R.string.engine_name_in_use;
                }
                if (!nameOk) {
                    Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
                    removeDialog(NETWORK_ENGINE_DIALOG);
                    showDialog(NETWORK_ENGINE_DIALOG);
                    return;
                }
                networkEngineToConfig = pathName;
                removeDialog(NETWORK_ENGINE_CONFIG_DIALOG);
                showDialog(NETWORK_ENGINE_CONFIG_DIALOG);
            }
        };
        builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                createEngine.run();
            }
        });
        builder.setNegativeButton(R.string.cancel, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });

        final Dialog dialog = builder.create();
        engineNameView.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    createEngine.run();
                    dialog.cancel();
                    return true;
                }
                return false;
            }
        });
        return dialog;
    }

    // Configure network engine settings
    private final Dialog networkEngineConfigDialog() {
        View content = View.inflate(this, R.layout.network_engine_config, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(content);
        builder.setTitle(R.string.configure_network_engine);
        final EditText hostNameView = (EditText)content.findViewById(R.id.network_engine_host);
        final EditText portView = (EditText)content.findViewById(R.id.network_engine_port);
        String hostName = "";
        String port = "0";
        try {
            if (EngineUtil.isNetEngine(networkEngineToConfig)) {
                String[] lines = Util.readFile(networkEngineToConfig);
                if (lines.length > 1)
                    hostName = lines[1];
                if (lines.length > 2)
                    port = lines[2];
            }
        } catch (IOException e1) {
        }
        hostNameView.setText(hostName);
        portView.setText(port);
        final Runnable writeConfig = new Runnable() {
            public void run() {
                String hostName = hostNameView.getText().toString();
                String port = portView.getText().toString();
                try {
                    FileWriter fw = new FileWriter(new File(networkEngineToConfig), false);
                    fw.write("NETE\n");
                    fw.write(hostName); fw.write("\n");
                    fw.write(port); fw.write("\n");
                    fw.close();
                    setEngineOptions(true);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };
        builder.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeConfig.run();
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setNegativeButton(R.string.cancel, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setNeutralButton(R.string.delete, new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDialog(DELETE_NETWORK_ENGINE_DIALOG);
                showDialog(DELETE_NETWORK_ENGINE_DIALOG);
            }
        });

        final Dialog dialog = builder.create();
        portView.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    writeConfig.run();
                    dialog.cancel();
                    removeDialog(NETWORK_ENGINE_DIALOG);
                    showDialog(NETWORK_ENGINE_DIALOG);
                    return true;
                }
                return false;
            }
        });
        return dialog;
    }

    private Dialog deleteNetworkEngineDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_network_engine);
        String msg = networkEngineToConfig;
        if (msg.lastIndexOf('/') >= 0)
            msg = msg.substring(msg.lastIndexOf('/')+1);
        builder.setMessage(getString(R.string.network_engine) + ": " + msg);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                new File(networkEngineToConfig).delete();
                String engine = settings.getString("engine", "stockfish");
                if (engine.equals(networkEngineToConfig)) {
                    engine = "stockfish";
                    Editor editor = settings.edit();
                    editor.putString("engine", engine);
                    editor.commit();
                    dialog.dismiss();
                    int strength = settings.getInt("strength", 1000);
                    setEngineOptions(false);
                    setEngineStrength(engine, strength);
                }
                dialog.cancel();
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                removeDialog(NETWORK_ENGINE_DIALOG);
                showDialog(NETWORK_ENGINE_DIALOG);
            }
        });
        AlertDialog alert = builder.create();
        return alert;
    }

    /** Open a load/save file dialog. Uses OI file manager if available. */
    private void selectFile(int titleMsg, int buttonMsg, String settingsName, String defaultDir,
                            int dialog, int result) {
        setAutoMode(AutoMode.OFF);
        String action = "org.openintents.action.PICK_FILE";
        Intent i = new Intent(action);
        String currentFile = settings.getString(settingsName, "");
        String sep = File.separator;
        if (!currentFile.contains(sep))
            currentFile = Environment.getExternalStorageDirectory() +
                          sep + defaultDir + sep + currentFile;
        i.setData(Uri.fromFile(new File(currentFile)));
        i.putExtra("org.openintents.extra.TITLE", getString(titleMsg));
        i.putExtra("org.openintents.extra.BUTTON_TEXT", getString(buttonMsg));
        try {
            startActivityForResult(i, result);
        } catch (ActivityNotFoundException e) {
            removeDialog(dialog);
            showDialog(dialog);
        }
    }

    private final boolean hasScidProvider() {
        try {
            getPackageManager().getPackageInfo("org.scid.android", 0);
            return true;
        } catch (PackageManager.NameNotFoundException ex) {
            return false;
        }
    }

    private final void selectScidFile() {
        setAutoMode(AutoMode.OFF);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("org.scid.android",
                                              "org.scid.android.SelectFileActivity"));
        intent.setAction(".si4");
        try {
            startActivityForResult(intent, RESULT_SELECT_SCID);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public final static boolean hasFenProvider(PackageManager manager) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); 
        i.setType("application/x-chess-fen");
        List<ResolveInfo> resolvers = manager.queryIntentActivities(i, 0);
        return (resolvers != null) && (resolvers.size() > 0);
    }

    private final void getFen() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); 
        i.setType("application/x-chess-fen");
        try {
            startActivityForResult(i, RESULT_GET_FEN);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    final static int FT_NONE = 0;
    final static int FT_PGN  = 1;
    final static int FT_SCID = 2;
    final static int FT_FEN  = 3;

    private final int currFileType() {
        return settings.getInt("currFT", FT_NONE);
    }

    /** Return path name for the last used PGN or SCID file. */
    private final String currPathName() {
        int ft = settings.getInt("currFT", FT_NONE);
        switch (ft) {
        case FT_PGN: {
            String ret = settings.getString("currentPGNFile", "");
            String sep = File.separator;
            if (!ret.contains(sep))
                ret = Environment.getExternalStorageDirectory() + sep + pgnDir + sep + ret;
            return ret;
        }
        case FT_SCID:
            return settings.getString("currentScidFile", "");
        case FT_FEN:
            return settings.getString("currentFENFile", "");
        default:
            return "";
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

    /** Save current game to a PGN file. */
    private final void savePGNToFile(String pathName, boolean silent) {
        String pgn = ctrl.getPGN();
        Editor editor = settings.edit();
        editor.putString("currentPGNFile", pathName);
        editor.putInt("currFT", FT_PGN);
        editor.commit();
        Intent i = new Intent(DroidFish.this, EditPGNSave.class);
        i.setAction("org.petero.droidfish.saveFile");
        i.putExtra("org.petero.droidfish.pathname", pathName);
        i.putExtra("org.petero.droidfish.pgn", pgn);
        i.putExtra("org.petero.droidfish.silent", silent);
        startActivity(i);
    }

    /** Load a PGN game from a file. */
    private final void loadPGNFromFile(String pathName) {
        Editor editor = settings.edit();
        editor.putString("currentPGNFile", pathName);
        editor.putInt("currFT", FT_PGN);
        editor.commit();
        Intent i = new Intent(DroidFish.this, EditPGNLoad.class);
        i.setAction("org.petero.droidfish.loadFile");
        i.putExtra("org.petero.droidfish.pathname", pathName);
        startActivityForResult(i, RESULT_LOAD_PGN);
    }

    /** Load a FEN position from a file. */
    private final void loadFENFromFile(String pathName) {
        if (pathName == null)
            return;
        Editor editor = settings.edit();
        editor.putString("currentFENFile", pathName);
        editor.putInt("currFT", FT_FEN);
        editor.commit();
        Intent i = new Intent(DroidFish.this, LoadFEN.class);
        i.setAction("org.petero.droidfish.loadFen");
        i.putExtra("org.petero.droidfish.pathname", pathName);
        startActivityForResult(i, RESULT_LOAD_FEN);
    }

    private final void setFenHelper(String fen) {
        if (fen == null)
            return;
        try {
            ctrl.setFENOrPGN(fen);
        } catch (ChessParseError e) {
            // If FEN corresponds to illegal chess position, go into edit board mode.
            try {
                TextIO.readFEN(fen);
            } catch (ChessParseError e2) {
                if (e2.pos != null)
                    startEditBoard(fen);
            }
        }
    }

    @Override
    public void requestPromotePiece() {
        showDialog(PROMOTE_DIALOG);
    }

    @Override
    public void reportInvalidMove(Move m) {
        String msg = String.format(Locale.US, "%s %s-%s",
                getString(R.string.invalid_move),
                TextIO.squareToString(m.from), TextIO.squareToString(m.to));
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void reportEngineName(String engine) {
        String msg = String.format(Locale.US, "%s: %s",
                getString(R.string.engine), engine);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void reportEngineError(String errMsg) {
        String msg = String.format(Locale.US, "%s: %s",
                getString(R.string.engine_error), errMsg);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void computerMoveMade() {
        if (soundEnabled) {
            if (moveSound != null)
                moveSound.release();
            try {
                moveSound = MediaPlayer.create(this, R.raw.movesound);
                if (moveSound != null)
                    moveSound.start();
            } catch (NotFoundException ex) {
            }
        }
        if (vibrateEnabled) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
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
            warn = lastComputationMillis >= lastVisibleMillis + 60000;
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
            boolean silhouette = Build.VERSION.SDK_INT >= 21;
            int icon = silhouette ? R.drawable.silhouette : R.mipmap.icon;
            CharSequence tickerText = getString(R.string.heavy_cpu_usage);
            long when = System.currentTimeMillis();
            Context context = getApplicationContext();
            CharSequence contentTitle = getString(R.string.background_processing);
            CharSequence contentText = getString(R.string.lot_cpu_power);
            Intent notificationIntent = new Intent(this, CPUWarning.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            @SuppressWarnings("deprecation")
            Notification notification = new Notification.Builder(context)
                    .setSmallIcon(icon)
                    .setTicker(tickerText)
                    .setWhen(when)
                    .setOngoing(true)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setContentIntent(contentIntent)
                    .getNotification();
            mNotificationManager.notify(cpuUsage, notification);
        } else {
            mNotificationManager.cancel(cpuUsage);
        }
    }

    private final String timeToString(int time) {
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
    public void setRemainingTime(int wTime, int bTime, int nextUpdate) {
        if (ctrl.getGameMode().clocksActive()) {
            whiteTitleText.setText(getString(R.string.white_square_character) + " " + timeToString(wTime));
            blackTitleText.setText(getString(R.string.black_square_character) + " " + timeToString(bTime));
        } else {
            TreeMap<String,String> headers = new TreeMap<String,String>();
            ctrl.getHeaders(headers);
            whiteTitleText.setText(headers.get("White"));
            blackTitleText.setText(headers.get("Black"));
        }
        handlerTimer.removeCallbacks(r);
        if (nextUpdate > 0)
            handlerTimer.postDelayed(r, nextUpdate);
    }

    private Handler autoModeTimer = new Handler();
    private Runnable amRunnable = new Runnable() {
        @Override
        public void run() {
            switch (autoMode) {
            case BACKWARD:
                ctrl.undoMove();
                setAutoMode(autoMode);
                break;
            case FORWARD:
                ctrl.redoMove();
                setAutoMode(autoMode);
                break;
            case OFF:
                break;
            }
        }
    };

    /** Set automatic move forward/backward mode. */
    void setAutoMode(AutoMode am) {
//        System.out.printf("%.3f DroidFish.setAutoMode(): %s\n",
//                System.currentTimeMillis() * 1e-3, am.toString());
        autoMode = am;
        switch (am) {
        case BACKWARD:
        case FORWARD:
            if (autoMoveDelay > 0)
                autoModeTimer.postDelayed(amRunnable, autoMoveDelay);
            break;
        case OFF:
            autoModeTimer.removeCallbacks(amRunnable);
            break;
        }
    }

    /** Disable automatic move mode if clocks are active. */
    void maybeAutoModeOff(GameMode gm) {
        if (gm.clocksActive())
            setAutoMode(AutoMode.OFF);
    }

    /** PngTokenReceiver implementation that renders PGN data for screen display. */
    static class PgnScreenText implements PgnToken.PgnTokenReceiver,
                                          MoveListView.OnLinkClickListener {
        private SpannableStringBuilder sb = new SpannableStringBuilder();
        private TreeMap<Integer,Node> offs2Node = new TreeMap<Integer,Node>();
        private int prevType = PgnToken.EOF;
        int nestLevel = 0;
        boolean col0 = true;
        Node currNode = null;
        final static int indentStep = 15;
        int currPos = 0, endPos = 0;
        boolean upToDate = false;
        PGNOptions options;
        DroidFish df;

        private static class NodeInfo {
            int l0, l1;
            NodeInfo(int ls, int le) {
                l0 = ls;
                l1 = le;
            }
        }
        HashMap<Node, NodeInfo> nodeToCharPos;

        PgnScreenText(DroidFish df, PGNOptions options) {
            this.df = df;
            nodeToCharPos = new HashMap<Node, NodeInfo>();
            this.options = options;
        }

        public final CharSequence getText() {
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

        private void addMoveLink(Node node, int l0, int l1) {
            offs2Node.put(l0, node);
            offs2Node.put(l1, null);
        }

        @Override
        public boolean onLinkClick(int offs) {
            if (ctrl == null)
                return false;
            Map.Entry<Integer, Node> e = offs2Node.floorEntry(offs);
            if (e == null)
                return false;
            Node node = e.getValue();
            if (node == null && e.getKey() == offs) {
                e = offs2Node.lowerEntry(e.getKey());
                if (e != null)
                    node = e.getValue();
            }
            if (node == null)
                return false;

            // On android 4.1 this onClick method is called
            // even when you long click the move list. The test
            // below works around the problem.
            Dialog mlmd = moveListMenuDlg;
            if ((mlmd == null) || !mlmd.isShowing()) {
                df.setAutoMode(AutoMode.OFF);
                ctrl.goNode(node);
            }
            return true;
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
                addMoveLink(node, l0, l1);
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
                int l0 = sb.length();
                sb.append(token.replaceAll("[ \t\r\n]+", " ").trim());
                int l1 = sb.length();
                int color = ColorTheme.instance().getColor(ColorTheme.PGN_COMMENT);
                sb.setSpan(new ForegroundColorSpan(color), l0, l1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
            sb = new SpannableStringBuilder();
            offs2Node.clear();
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
}
