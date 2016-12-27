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
    private boolean initialized = false;
    private String toSpeak = null;

    public enum Language {
        EN,   // English
        DE,   // German
        NONE; // Not supported

        public static Language fromString(String langStr) {
            if ("en".equals(langStr))
                return EN;
            if ("de".equals(langStr))
                return DE;
            return NONE;
        }
    }
    private Language lang;

    
    /** Initialize the text to speech engine for a given language. */
    public void initialize(final Context context, final String langStr) {
        Language newLang = Language.fromString(langStr);
        if (newLang != lang)
            shutdown();
        final Locale loc = getLocale(newLang);
        if (loc == null)
            initialized = true;
        if (initialized)
            return;
        tts = new TextToSpeech(context, new OnInitListener() {
            @Override
            public void onInit(int status) {
                initialized = true;
                int toast = -1;
                if (status == TextToSpeech.SUCCESS) {
                    int code = tts.setLanguage(loc);
                    switch (code) {
                    case TextToSpeech.LANG_AVAILABLE:
                    case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                    case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                        lang = Language.fromString(langStr);
                        tts.addEarcon("[move]", "org.petero.droidfish", R.raw.movesound);
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
            if (lang != Language.NONE && text != null) {
                tts.playEarcon("[move]", TextToSpeech.QUEUE_ADD, null);
                tts.speak(text, TextToSpeech.QUEUE_ADD, null);
            }
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
            lang = Language.NONE;
            initialized = false;
        }
    }

    /** Convert move "move" in position "pos" to a sentence and speak it. */
    public void say(Position pos, Move move) {
        String s = moveToText(pos, move, lang);
//        System.out.printf("%.3f Speech.say(): %s\n", System.currentTimeMillis() * 1e-3, s);
        if (!s.isEmpty())
            say(s);
    }

    /** Convert move "move" in position "pos" to a sentence that can be spoken. */
    public static String moveToText(Position pos, Move move, Language lang) {
        if (move == null)
            return "";

        String moveStr = TextIO.moveToString(pos, move, false, false);
        int piece = Piece.makeWhite(pos.getPiece(move.from));
        boolean capture = pos.getPiece(move.to) != Piece.EMPTY;
        boolean promotion = move.promoteTo != Piece.EMPTY;
        boolean check = moveStr.endsWith("+");
        boolean checkMate = moveStr.endsWith("#");
        boolean castle = false;
        boolean enPassant = false;

        if (piece == Piece.WPAWN && !capture) {
            int fx = Position.getX(move.from);
            int tx = Position.getX(move.to);
            if (fx != tx) {
                capture = true; // En passant
                enPassant = true;
            }
        }

        StringBuilder sentence = new StringBuilder();

        if (piece == Piece.WKING) {
            int fx = Position.getX(move.from);
            int tx = Position.getX(move.to);
            if (fx == 4 && tx == 6) {
                addWord(sentence, castleToString(true, lang));
                castle = true;
            } else if (fx == 4 && (tx == 2)) {
                addWord(sentence, castleToString(false, lang));
                castle = true;
            }
        }

        if (!castle) {
            boolean pawnMove = piece == Piece.WPAWN;
            if (!pawnMove)
                addWord(sentence, pieceName(piece, lang));

            if (capture) {
                int i = moveStr.indexOf("x");
                String from = moveStr.substring(pawnMove ? 0 : 1, i);
                if (!from.isEmpty())
                    addWord(sentence, fromToString(from, lang));
                String to = moveStr.substring(i + 1, i + 3);
                addWord(sentence, captureToString(to, lang));
                addWord(sentence, toToString(to, lang));
                if (enPassant)
                    addWord(sentence, epToString(lang));
            } else {
                int nSkip = (promotion ? 1 : 0) + ((check | checkMate) ? 1 : 0);
                int i = moveStr.length() - nSkip;
                String from = moveStr.substring(pawnMove ? 0 : 1, i - 2);
                if (!from.isEmpty())
                    addWord(sentence, fromToString(from, lang));
                String to = moveStr.substring(i - 2, i);
                addWord(sentence, toToString(to, lang));
            }

            if (promotion)
                addWord(sentence, promToString(move.promoteTo, lang));
        }

        if (checkMate) {
            addWord(sentence, checkMateToString(lang));
        } else if (check) {
            addWord(sentence, checkToString(lang));
        }

        return sentence.toString().trim();
    }

    /** Return the locale corresponding to a language string,
     *  or null if language not supported. */
    private static Locale getLocale(Language lang) {
        switch (lang) {
        case EN:
            return Locale.US;
        case DE:
            return Locale.GERMAN;
        case NONE:
            return null;
        }
        throw new IllegalArgumentException();
    }

    /** Add zero or more words to the string builder.
     *  If anything was added, an extra space is also added at the end. */
    private static void addWord(StringBuilder sb, String words) {
        if (!words.isEmpty())
            sb.append(words).append(' ');
    }

    /** Get the name of a non-pawn piece. Return empty string if no such piece. */
    private static String pieceName(int piece, Language lang) {
        piece = Piece.makeWhite(piece);
        switch (lang) {
        case EN:
            switch (piece) {
            case Piece.WKING:   return "King";
            case Piece.WQUEEN:  return "Queen";
            case Piece.WROOK:   return "Rook";
            case Piece.WBISHOP: return "Bishop";
            case Piece.WKNIGHT: return "Knight";
            default:            return "";
            }            
        case DE:
            switch (piece) {
            case Piece.WKING:   return "König";
            case Piece.WQUEEN:  return "Dame";
            case Piece.WROOK:   return "Turm";
            case Piece.WBISHOP: return "Läufer";
            case Piece.WKNIGHT: return "Springer";
            default:            return "";
            }            
        case NONE:
            return "";
        }
        throw new IllegalArgumentException();
    }

    private static String fromToString(String from, Language lang) {
        switch (lang) {
        case EN:
            if ("a".equals(from))
                return "ae";
            return from;
        case DE:
            return from;
        case NONE:
            return "";
        }
        throw new IllegalArgumentException();
    }

    private static String toToString(String to, Language lang) {
        return to;
    }

    private static String captureToString(String to, Language lang) {
        switch (lang) {
        case EN:
            return to.startsWith("e") ? "take" : "takes";
        case DE:
            return "schlägt";
        case NONE:
            return "";
        }
        throw new IllegalArgumentException();
    }

    private static String castleToString(boolean kingSide, Language lang) {
        switch (lang) {
        case EN:
            return kingSide ? "Short castle" : "Long castle";
        case DE:
            return kingSide ? "Kleine Rochade" : "Große Rochade";
        case NONE:
            return "";
        }
        throw new IllegalArgumentException();
    }

    private static String epToString(Language lang) {
        switch (lang) {
        case EN:
            return "";
        case DE:
            return "en passant";
        case NONE:
            return "";
        }
        throw new IllegalArgumentException();
    }

    private static String promToString(int piece, Language lang) {
        String pn = pieceName(piece, lang);
        switch (lang) {
        case EN:
            return pn;
        case DE:
            return "Umwandlung zu " + pn;
        case NONE:
            return "";
        }
        throw new IllegalArgumentException();
    }

    private static String checkToString(Language lang) {
        switch (lang) {
        case EN:
            return "check!";
        case DE:
            return "Schach!";
        case NONE:
            return "";
        }
        throw new IllegalArgumentException();
    }

    private static String checkMateToString(Language lang) {
        switch (lang) {
        case EN:
            return "check mate!";
        case DE:
            return "Schach matt!";
        case NONE:
            return "";
        }
        throw new IllegalArgumentException();
    }
}
