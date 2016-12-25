/*
    DroidFish - An Android chess program.
    Copyright (C) 2016  Peter Österlund, peterosterlund2@gmail.com

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

import java.util.Locale;

import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.widget.Toast;

/** Handles text to speech translation. */
public class Speech {
    private TextToSpeech tts;
    boolean initialized = false;
    boolean supported = false;
    String toSpeak = null;

    public void initialize(final Context context) {
        if (initialized)
            return;
        tts = new TextToSpeech(context, new OnInitListener() {
            @Override
            public void onInit(int status) {
                initialized = true;
                int toast = -1;
                if (status == TextToSpeech.SUCCESS) {
                    int code = tts.setLanguage(Locale.US);
                    switch (code) {
                    case TextToSpeech.LANG_AVAILABLE:
                    case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                    case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                        supported = true;
                        say(toSpeak);
                        break;
                    case TextToSpeech.LANG_MISSING_DATA:
                        toast = R.string.tts_data_missing;
                        break;
                    case TextToSpeech.LANG_NOT_SUPPORTED:
                        toast = R.string.tts_not_supported_for_lang;
                        break;
                    default:
                        break;
                    }
                } else {
                    toast = R.string.tts_failed_to_init;
                }
                if (toast != -1)
                    Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressWarnings("deprecation")
    public void say(String text) {
        if (initialized) {
            if (supported && text != null)
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            toSpeak = null;
        } else {
            toSpeak = text;
        }
    }

    /** Immediately cancel all speech output. */
    public void flushQueue() {
        toSpeak = null;
        if (tts != null)
            tts.stop();
    }

    /** Shut down the speech engine. */
    public void shutdown() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
            initialized = false;
            supported = false;
        }
    }

    /** Convert move "move" in position "pos" to a sentence and speak it. */
    public void say(Position pos, Move move, String langStr) {
        String s = moveToText(pos, move, langStr);
//        System.out.printf("%.3f Speech.say(): %s\n", System.currentTimeMillis() * 1e-3, s);
        if (!s.isEmpty())
            say(s);
    }

    /** Convert move "move" in position "pos" to a sentence that can be spoken. */
    public static String moveToText(Position pos, Move move, String langStr) {
        if (move == null || !langStr.equals("en"))
            return "";

        String moveStr = TextIO.moveToString(pos, move, false, false);
        int piece = Piece.makeWhite(pos.getPiece(move.from));
        boolean capture = pos.getPiece(move.to) != Piece.EMPTY;
        boolean promotion = move.promoteTo != Piece.EMPTY;
        boolean check = moveStr.endsWith("+");
        boolean checkMate = moveStr.endsWith("#");
        boolean castle = false;

        if (piece == Piece.WPAWN && !capture) {
            int fx = Position.getX(move.from);
            int tx = Position.getX(move.to);
            if (fx != tx)
                capture = true; // En passant
        }

        StringBuilder sentence = new StringBuilder();

        if (piece == Piece.WKING) {
            int fx = Position.getX(move.from);
            int tx = Position.getX(move.to);
            if (fx == 4 && tx == 6) {
                sentence.append("Short castle");
                castle = true;
            } else if (fx == 4 && (tx == 2)) {
                sentence.append("Long castle");
                castle = true;
            }
        }

        if (!castle) {
            boolean pawnMove = piece == Piece.WPAWN;
            if (!pawnMove)
                sentence.append(pieceName(piece)).append(' ');

            if (capture) {
                int i = moveStr.indexOf("x");
                String from = moveStr.substring(pawnMove ? 0 : 1, i);
                if (!from.isEmpty())
                    sentence.append(getFromWord(from)).append(' ');
                String to = moveStr.substring(i + 1, i + 3);
                sentence.append(to.startsWith("e") ? "take " : "takes ");
                sentence.append(to).append(' ');
            } else {
                int nSkip = (promotion ? 1 : 0) + ((check | checkMate) ? 1 : 0);
                int i = moveStr.length() - nSkip;
                String from = moveStr.substring(pawnMove ? 0 : 1, i - 2);
                if (!from.isEmpty())
                    sentence.append(from).append(' ');
                String to = moveStr.substring(i - 2, i);
                sentence.append(to).append(' ');
            }

            if (promotion)
                sentence.append(pieceName(move.promoteTo)).append(' ');
        }

        if (checkMate) {
            removeLastSpace(sentence);
            sentence.append(". Check mate!");
        } else if (check) {
            removeLastSpace(sentence);
            sentence.append(". Check!");
        }

        return sentence.toString().trim();
    }

    /** Get the name of a non-pawn piece. Return empty string if no such piece. */
    private static String pieceName(int piece) {
        piece = Piece.makeWhite(piece);
        switch (piece) {
        case Piece.WKING:   return "King";
        case Piece.WQUEEN:  return "Queen";
        case Piece.WROOK:   return "Rook";
        case Piece.WBISHOP: return "Bishop";
        case Piece.WKNIGHT: return "Knight";
        default:            return "";
        }            
    }

    /** Transform a "from" file or file+rank to a word. */
    private static String getFromWord(String from) {
        if ("a".equals(from))
            return "ae";
        return from;
    }

    /** If the last character in the StringBuilder is a space, remove it. */
    private static void removeLastSpace(StringBuilder sb) {
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == ' ')
            sb.setLength(len - 1);
    }
}
