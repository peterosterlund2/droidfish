/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2013  Peter Österlund, peterosterlund2@gmail.com

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

import java.util.ArrayList;
import java.util.Locale;

import org.petero.droidfish.ChessBoard;
import org.petero.droidfish.DroidFish;
import org.petero.droidfish.R;
import org.petero.droidfish.ChessBoard.SquareDecoration;
import org.petero.droidfish.Util;
import org.petero.droidfish.Util.MaterialDiff;
import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.tb.Probe;
import org.petero.droidfish.tb.ProbeResult;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ClickableViewAccessibility")
public class EditBoard extends Activity {
    private ChessBoardEdit cb;
    private TextView status;
    private Button okButton;
    private Button cancelButton;

    private boolean egtbHints;
    private boolean autoScrollTitle;
    private TextView whiteFigText;
    private TextView blackFigText;
    private Typeface figNotation;

    private DrawerLayout drawerLayout;
    private ListView leftDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        figNotation = Typeface.createFromAsset(getAssets(), "fonts/DroidFishChessNotationDark.otf");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        egtbHints = settings.getBoolean("tbHintsEdit", false);
        autoScrollTitle = settings.getBoolean("autoScrollTitle", true);

        initUI();

        Util.setFullScreenMode(this, settings);

        Intent i = getIntent();
        Position pos = null;
        try {
            pos = TextIO.readFEN(i.getAction());
        } catch (ChessParseError e) {
            pos = e.pos;
        }
        if (pos != null)
            cb.setPosition(pos);
        checkValidAndUpdateMaterialDiff();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ChessBoardEdit oldCB = cb;
        String statusStr = status.getText().toString();
        initUI();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        cb.setPosition(oldCB.pos);
        setSelection(oldCB.selectedSquare);
        cb.userSelectedSquare = oldCB.userSelectedSquare;
        status.setText(statusStr);
        checkValidAndUpdateMaterialDiff();
    }

    private final void initUI() {
        setContentView(R.layout.editboard);
        Util.overrideViewAttribs(findViewById(R.id.main));

        View firstTitleLine = findViewById(R.id.first_title_line);
        View secondTitleLine = findViewById(R.id.second_title_line);
        cb = (ChessBoardEdit)findViewById(R.id.eb_chessboard);
        status = (TextView)findViewById(R.id.eb_status);
        okButton = (Button)findViewById(R.id.eb_ok);
        cancelButton = (Button)findViewById(R.id.eb_cancel);

        TextView whiteTitleText = (TextView)findViewById(R.id.white_clock);
        whiteTitleText.setVisibility(View.GONE);
        TextView blackTitleText = (TextView)findViewById(R.id.black_clock);
        blackTitleText.setVisibility(View.GONE);
        TextView engineTitleText = (TextView)findViewById(R.id.title_text);
        engineTitleText.setVisibility(View.GONE);
        whiteFigText = (TextView) findViewById(R.id.white_pieces);
        whiteFigText.setTypeface(figNotation);
        whiteFigText.setSelected(true);
        whiteFigText.setTextColor(whiteTitleText.getTextColors());
        blackFigText = (TextView) findViewById(R.id.black_pieces);
        blackFigText.setTypeface(figNotation);
        blackFigText.setSelected(true);
        blackFigText.setTextColor(blackTitleText.getTextColors());
        TextView summaryTitleText = (TextView) findViewById(R.id.title_text_summary);
        summaryTitleText.setText(R.string.edit_board);

        TextUtils.TruncateAt where = autoScrollTitle ? TextUtils.TruncateAt.MARQUEE
                                                     : TextUtils.TruncateAt.END;
        engineTitleText.setEllipsize(where);
        whiteFigText.setEllipsize(where);
        blackFigText.setEllipsize(where);

        initDrawers();

        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        };
        firstTitleLine.setOnClickListener(listener);
        secondTitleLine.setOnClickListener(listener);

        okButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendBackResult();
            }
        });
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        status.setFocusable(false);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        cb.setOnTouchListener(new OnTouchListener() {
            private boolean pending = false;
            private int sq0 = -1;
            private Handler handler = new Handler();
            private Runnable runnable = new Runnable() {
                public void run() {
                    pending = false;
                    handler.removeCallbacks(runnable);
                    ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(20);
                    drawerLayout.openDrawer(Gravity.LEFT);
                }
            };
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                case MotionEvent.ACTION_DOWN:
                    handler.postDelayed(runnable, ViewConfiguration.getLongPressTimeout());
                    sq0 = cb.eventToSquare(event);
                    pending = true;
                    break;
                case MotionEvent.ACTION_UP:
                    if (pending) {
                        pending = false;
                        handler.removeCallbacks(runnable);
                        int sq = cb.eventToSquare(event);
                        if (sq == sq0) {
                            Move m = cb.mousePressed(sq);
                            if (m != null)
                                doMove(m);
                            setEgtbHints(cb.getSelectedSquare());
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
        });
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
            public void onTrackballEvent(MotionEvent event) {
                Move m = cb.handleTrackballEvent(event);
                if (m != null)
                    doMove(m);
                setEgtbHints(cb.getSelectedSquare());
            }
        });
    }

    /** Initialize the drawer part of the user interface. */
    private void initDrawers() {
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        leftDrawer = (ListView)findViewById(R.id.left_drawer);

        class DrawerItem {
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

        final int SIDE_TO_MOVE    = 0;
        final int CLEAR_BOARD     = 1;
        final int INITIAL_POS     = 2;
        final int CASTLING_FLAGS  = 3;
        final int EN_PASSANT_FILE = 4;
        final int MOVE_COUNTERS   = 5;
        final int COPY_POSITION   = 6;
        final int PASTE_POSITION  = 7;
        final int GET_FEN         = 8;

        final ArrayList<DrawerItem> leftItems = new ArrayList<DrawerItem>();
        leftItems.add(new DrawerItem(SIDE_TO_MOVE, R.string.side_to_move));
        leftItems.add(new DrawerItem(CLEAR_BOARD, R.string.clear_board));
        leftItems.add(new DrawerItem(INITIAL_POS, R.string.initial_position));
        leftItems.add(new DrawerItem(CASTLING_FLAGS, R.string.castling_flags));
        leftItems.add(new DrawerItem(EN_PASSANT_FILE, R.string.en_passant_file));
        leftItems.add(new DrawerItem(MOVE_COUNTERS, R.string.move_counters));
        leftItems.add(new DrawerItem(COPY_POSITION, R.string.copy_position));
        leftItems.add(new DrawerItem(PASTE_POSITION, R.string.paste_position));
        if (DroidFish.hasFenProvider(getPackageManager()))
            leftItems.add(new DrawerItem(GET_FEN, R.string.get_fen));

        leftDrawer.setAdapter(new ArrayAdapter<DrawerItem>(this,
                                                           R.layout.drawer_list_item,
                                                           leftItems.toArray(new DrawerItem[0])));
        leftDrawer.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                leftDrawer.clearChoices();
                DrawerItem di = leftItems.get(position);
                switch (di.id) {
                case SIDE_TO_MOVE:
                    showDialog(SIDE_DIALOG);
                    setSelection(-1);
                    checkValidAndUpdateMaterialDiff();
                    break;
                case CLEAR_BOARD: {
                    Position pos = new Position();
                    cb.setPosition(pos);
                    setSelection(-1);
                    checkValidAndUpdateMaterialDiff();
                    break;
                }
                case INITIAL_POS: {
                    try {
                        Position pos = TextIO.readFEN(TextIO.startPosFEN);
                        cb.setPosition(pos);
                        setSelection(-1);
                        checkValidAndUpdateMaterialDiff();
                    } catch (ChessParseError e) {
                    }
                    break;
                }
                case CASTLING_FLAGS:
                    removeDialog(CASTLE_DIALOG);
                    showDialog(CASTLE_DIALOG);
                    setSelection(-1);
                    checkValidAndUpdateMaterialDiff();
                    break;
                case EN_PASSANT_FILE:
                    removeDialog(EP_DIALOG);
                    showDialog(EP_DIALOG);
                    setSelection(-1);
                    checkValidAndUpdateMaterialDiff();
                    break;
                case MOVE_COUNTERS:
                    removeDialog(MOVCNT_DIALOG);
                    showDialog(MOVCNT_DIALOG);
                    setSelection(-1);
                    checkValidAndUpdateMaterialDiff();
                    break;
                case COPY_POSITION: {
                    setPosFields();
                    String fen = TextIO.toFEN(cb.pos) + "\n";
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(new ClipData(fen,
                            new String[]{ "application/x-chess-fen", ClipDescription.MIMETYPE_TEXT_PLAIN },
                            new ClipData.Item(fen)));
                    setSelection(-1);
                    break;
                }
                case PASTE_POSITION: {
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()) {
                        ClipData clip = clipboard.getPrimaryClip();
                        if (clip.getItemCount() > 0) {
                            String fen = clip.getItemAt(0).coerceToText(getApplicationContext()).toString();
                            setFEN(fen);
                        }
                    }
                    break;
                }
                case GET_FEN:
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT); 
                    i.setType("application/x-chess-fen");
                    try {
                        startActivityForResult(i, RESULT_GET_FEN);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        drawerLayout.openDrawer(Gravity.LEFT);
        return false;
    }

    private final void setSelection(int sq) {
        cb.setSelection(sq);
        setEgtbHints(sq);
    }

    private final void setEgtbHints(int sq) {
        if (!egtbHints || (sq < 0)) {
            cb.setSquareDecorations(null);
            return;
        }

        Probe gtbProbe = Probe.getInstance();
        ArrayList<Pair<Integer,ProbeResult>> x = gtbProbe.relocatePieceProbe(cb.pos, sq);
        if (x == null) {
            cb.setSquareDecorations(null);
            return;
        }

        ArrayList<SquareDecoration> sd = new ArrayList<SquareDecoration>();
        for (Pair<Integer,ProbeResult> p : x)
            sd.add(new SquareDecoration(p.first, p.second));
        cb.setSquareDecorations(sd);
    }

    private void doMove(Move m) {
        if (m.to < 0) {
            if ((m.from < 0) || (cb.pos.getPiece(m.from) == Piece.EMPTY)) {
                setSelection(m.to);
                return;
            }
        }
        Position pos = new Position(cb.pos);
        int piece = Piece.EMPTY;
        if (m.from >= 0) {
            piece = pos.getPiece(m.from);
        } else {
            piece = -(m.from + 2);
        }
        if (m.to >= 0) {
            int oPiece = Piece.swapColor(piece);
            if ((m.from < 0) && (pos.getPiece(m.to) == oPiece))
                pos.setPiece(m.to, Piece.EMPTY);
            else if ((m.from < 0) && (pos.getPiece(m.to) == piece))
                pos.setPiece(m.to, oPiece);
            else
                pos.setPiece(m.to, piece);
        }
        if (m.from >= 0)
            pos.setPiece(m.from, Piece.EMPTY);
        cb.setPosition(pos);
        if (m.from >= 0)
            setSelection(-1);
        else
            setSelection(m.from);
        checkValidAndUpdateMaterialDiff();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            sendBackResult();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private final void sendBackResult() {
        if (checkValidAndUpdateMaterialDiff()) {
            setPosFields();
            String fen = TextIO.toFEN(cb.pos);
            setResult(RESULT_OK, (new Intent()).setAction(fen));
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private final void setPosFields() {
        setEPFile(getEPFile()); // To handle sideToMove change
        TextIO.fixupEPSquare(cb.pos);
        TextIO.removeBogusCastleFlags(cb.pos);
    }

    private final int getEPFile() {
        int epSquare = cb.pos.getEpSquare();
        if (epSquare < 0) return 8;
        return Position.getX(epSquare);
    }

    private final void setEPFile(int epFile) {
        int epSquare = -1;
        if ((epFile >= 0) && (epFile < 8)) {
            int epRank = cb.pos.whiteMove ? 5 : 2;
            epSquare = Position.getSquare(epFile, epRank);
        }
        cb.pos.setEpSquare(epSquare);
    }

    /** Test if a position is valid and update material diff display. */
    private final boolean checkValidAndUpdateMaterialDiff() {
        try {
            MaterialDiff md = Util.getMaterialDiff(cb.pos);
            whiteFigText.setText(md.white);
            blackFigText.setText(md.black);

            String fen = TextIO.toFEN(cb.pos);
            TextIO.readFEN(fen);
            status.setText("");
            return true;
        } catch (ChessParseError e) {
            status.setText(getParseErrString(e));
        }
        return false;
    }

    private final String getParseErrString(ChessParseError e) {
        if (e.resourceId == -1)
            return e.getMessage();
        else
            return getString(e.resourceId);
    }

    static final int SIDE_DIALOG = 1;
    static final int CASTLE_DIALOG = 2;
    static final int EP_DIALOG = 3;
    static final int MOVCNT_DIALOG = 4;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case SIDE_DIALOG: {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_side_to_move_first);
            final int selectedItem = (cb.pos.whiteMove) ? 0 : 1;
            builder.setSingleChoiceItems(new String[]{getString(R.string.white), getString(R.string.black)}, selectedItem, new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (id == 0) { // white to move
                        cb.pos.setWhiteMove(true);
                        checkValidAndUpdateMaterialDiff();
                        dialog.cancel();
                    } else {
                        cb.pos.setWhiteMove(false);
                        checkValidAndUpdateMaterialDiff();
                        dialog.cancel();
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case CASTLE_DIALOG: {
            final CharSequence[] items = {
                getString(R.string.white_king_castle), getString(R.string.white_queen_castle),
                getString(R.string.black_king_castle), getString(R.string.black_queen_castle)
            };
            boolean[] checkedItems = {
                    cb.pos.h1Castle(), cb.pos.a1Castle(),
                    cb.pos.h8Castle(), cb.pos.a8Castle()
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.castling_flags);
            builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    Position pos = new Position(cb.pos);
                    boolean a1Castle = pos.a1Castle();
                    boolean h1Castle = pos.h1Castle();
                    boolean a8Castle = pos.a8Castle();
                    boolean h8Castle = pos.h8Castle();
                    switch (which) {
                    case 0: h1Castle = isChecked; break;
                    case 1: a1Castle = isChecked; break;
                    case 2: h8Castle = isChecked; break;
                    case 3: a8Castle = isChecked; break;
                    }
                    int castleMask = 0;
                    if (a1Castle) castleMask |= 1 << Position.A1_CASTLE;
                    if (h1Castle) castleMask |= 1 << Position.H1_CASTLE;
                    if (a8Castle) castleMask |= 1 << Position.A8_CASTLE;
                    if (h8Castle) castleMask |= 1 << Position.H8_CASTLE;
                    pos.setCastleMask(castleMask);
                    cb.setPosition(pos);
                    checkValidAndUpdateMaterialDiff();
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case EP_DIALOG: {
            final CharSequence[] items = {
                    "A", "B", "C", "D", "E", "F", "G", "H", getString(R.string.none)
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_en_passant_file);
            builder.setSingleChoiceItems(items, getEPFile(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    setEPFile(item);
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        case MOVCNT_DIALOG: {
            View content = View.inflate(this, R.layout.edit_move_counters, null);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setView(content);
            builder.setTitle(R.string.edit_move_counters);
            final EditText halfMoveClock = (EditText)content.findViewById(R.id.ed_cnt_halfmove);
            final EditText fullMoveCounter = (EditText)content.findViewById(R.id.ed_cnt_fullmove);
            halfMoveClock.setText(String.format(Locale.US, "%d", cb.pos.halfMoveClock));
            fullMoveCounter.setText(String.format(Locale.US, "%d", cb.pos.fullMoveCounter));
            final Runnable setCounters = new Runnable() {
                public void run() {
                    try {
                        int halfClock = Integer.parseInt(halfMoveClock.getText().toString());
                        int fullCount = Integer.parseInt(fullMoveCounter.getText().toString());
                        cb.pos.halfMoveClock = halfClock;
                        cb.pos.fullMoveCounter = fullCount;
                    } catch (NumberFormatException nfe) {
                        Toast.makeText(getApplicationContext(), R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
                    }
                }
            };
            builder.setPositiveButton("Ok", new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    setCounters.run();
                }
            });
            builder.setNegativeButton("Cancel", null);

            final Dialog dialog = builder.create();

            fullMoveCounter.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        setCounters.run();
                        dialog.cancel();
                        return true;
                    }
                    return false;
                }
            });
            return dialog;
        }
        }
        return null;
    }

    private final void setFEN(String fen) {
        if (fen == null)
            return;
        try {
            Position pos = TextIO.readFEN(fen);
            cb.setPosition(pos);
        } catch (ChessParseError e) {
            if (e.pos != null)
                cb.setPosition(e.pos);
            Toast.makeText(getApplicationContext(), getParseErrString(e), Toast.LENGTH_SHORT).show();
        }
        setSelection(-1);
        checkValidAndUpdateMaterialDiff();
    }

    static private final int RESULT_GET_FEN  = 0;
    static private final int RESULT_LOAD_FEN = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case RESULT_GET_FEN:
            if (resultCode == RESULT_OK) {
                String fen = data.getStringExtra(Intent.EXTRA_TEXT);
                if (fen == null) {
                    String pathName = DroidFish.getFilePathFromUri(data.getData());
                    Intent i = new Intent(EditBoard.this, LoadFEN.class);
                    i.setAction("org.petero.droidfish.loadFen");
                    i.putExtra("org.petero.droidfish.pathname", pathName);
                    startActivityForResult(i, RESULT_LOAD_FEN);
                }
                setFEN(fen);
            }
            break;
        case RESULT_LOAD_FEN:
            if (resultCode == RESULT_OK) {
                String fen = data.getAction();
                setFEN(fen);
            }
            break;
        }
    }
}
