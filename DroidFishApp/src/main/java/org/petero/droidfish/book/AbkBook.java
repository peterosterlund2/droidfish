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
package org.petero.droidfish.book;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.petero.droidfish.book.DroidBook.BookEntry;
import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;

/** Handle Arena Chess GUI opening books. */
class AbkBook implements IOpeningBook {
    private File abkFile; // The ".abk" file
    private Position startPos;

    /** Constructor. */
    public AbkBook() {
        try {
            startPos = TextIO.readFEN(TextIO.startPosFEN);
        } catch (ChessParseError ex) {
            throw new RuntimeException(ex);
        }
    }

    static boolean canHandle(BookOptions options) {
        String filename = options.filename;
        return filename.endsWith(".abk");
    }

    @Override
    public boolean enabled() {
        return abkFile.canRead();
    }

    @Override
    public void setOptions(BookOptions options) {
        abkFile = new File(options.filename);
    }

    private static class MoveData {
        Move move;
        double weightPrio;
        double weightNGames;
        double weightScore;
    }

    @Override
    public ArrayList<BookEntry> getBookEntries(BookPosInput posInput) {
        if (!startPos.equals(posInput.getPrevPos()))
            return null;

        try (RandomAccessFile abkF = new RandomAccessFile(abkFile, "r")) {
            ArrayList<Move> gameMoves = posInput.getMoves();

            BookSettings bs = new BookSettings(abkF);
            if (gameMoves.size() >= bs.maxPly)
                return null;

            AbkBookEntry ent = new AbkBookEntry();
            int entNo = 900;
            for (Move m : gameMoves) {
                int iter = 0;
                while (true) {
                    if (entNo < 0)
                        return null;
                    ent.read(abkF, entNo);
                    if (ent.getMove().equals(m) && ent.isValid()) {
                        entNo = ent.nextMove;
                        break;
                    }
                    entNo = ent.nextSibling;
                    iter++;
                    if (iter > 255)
                        return null; // Corrupt book
                }
            }
            if (entNo < 0)
                return null;

            boolean wtm = (gameMoves.size() % 2) == 0;
            ArrayList<MoveData> moves = new ArrayList<>();
            while (entNo >= 0) {
                ent.read(abkF, entNo);
                MoveData md = new MoveData();
                md.move = ent.getMove();

                int nWon  = wtm ? ent.nWon  : ent.nLost;
                int nLost = wtm ? ent.nLost : ent.nWon;
                int nDraw = ent.nGames - nWon - nLost;
                md.weightPrio = scaleWeight(ent.priority, bs.prioImportance);
                md.weightNGames = scaleWeight(ent.nGames, bs.nGamesImportance);
                double score = (nWon + nDraw * 0.5) / ent.nGames;
                md.weightScore = scaleWeight(score, bs.scoreImportance);

                if (ent.isValid() &&
                    (!bs.skipPrio0Moves || ent.priority > 0) &&
                    (ent.nGames >= bs.minGames) &&
                    (nWon >= bs.minWonGames) &&
                    (score * 100 >= (wtm ? bs.minWinPercentWhite : bs.minWinPercentBlack))) {
                    moves.add(md);
                }

                if (moves.size() > 255)
                    return null; // Corrupt book
                entNo = ent.nextSibling;
            }

            double sumWeightPrio = 0;
            double sumWeightNGames = 0;
            double sumWeightScore = 0;
            for (MoveData md : moves) {
                sumWeightPrio += md.weightPrio;
                sumWeightNGames += md.weightNGames;
                sumWeightScore += md.weightScore;
            }

            ArrayList<BookEntry> ret = new ArrayList<>();
            boolean hasNonZeroWeight = false;
            for (MoveData md : moves) {
                BookEntry be = new BookEntry(md.move);
                double wP = sumWeightPrio   > 0 ? md.weightPrio   / sumWeightPrio   : 0.0;
                double wN = sumWeightNGames > 0 ? md.weightNGames / sumWeightNGames : 0.0;
                double wS = sumWeightScore  > 0 ? md.weightScore  / sumWeightScore  : 0.0;
                double a = 0.624;
                double w = wP * Math.exp(a * bs.prioImportance) +
                           wN * Math.exp(a * bs.nGamesImportance) +
                           wS * Math.exp(a * bs.scoreImportance) * 1.4;
                hasNonZeroWeight |= w > 0;
                be.weight = (float)w;
                ret.add(be);
            }
            if (!hasNonZeroWeight)
                for (BookEntry be : ret)
                    be.weight = 1;
            return ret;
        } catch (IOException e) {
            return null;
        }
    }

    private static class AbkBookEntry {
        private byte[] data = new byte[28];

        private byte from;       // From square, 0 = a1, 7 = h1, 8 = a2, 63 = h8
        private byte to;         // To square
        private byte promotion;  // 0 = none, +-1 = rook, +-2 = knight, +-3 = bishop, +-4 = queen
        byte priority;           // 0 = bad, >0 better, 9 best
        int nGames;              // Number of times games in which move was played
        int nWon;                // Number of won games for white
        int nLost;               // Number of lost games for white
        int flags;               // Value is 0x01000000 if move has been deleted
        int nextMove;            // First following move (by opposite color)
        int nextSibling;         // Next alternative move (by same color)

        AbkBookEntry() {
        }

        void read(RandomAccessFile f, long entNo) throws IOException {
            f.seek(entNo * 28);
            f.readFully(data);

            from = data[0];
            to = data[1];
            promotion = data[2];
            priority = data[3];
            nGames = extractInt(4);
            nWon = extractInt(8);
            nLost = extractInt(12);
            flags = extractInt(16);
            nextMove = extractInt(20);
            nextSibling = extractInt(24);
        }

        Move getMove() {
            int prom;
            switch (promotion) {
            case 0:  prom = Piece.EMPTY;   break;
            case -1: prom = Piece.WROOK;   break;
            case -2: prom = Piece.WKNIGHT; break;
            case -3: prom = Piece.WBISHOP; break;
            case -4: prom = Piece.WQUEEN;  break;
            case 1:  prom = Piece.BROOK;   break;
            case 2:  prom = Piece.BKNIGHT; break;
            case 3:  prom = Piece.BBISHOP; break;
            case 4:  prom = Piece.BQUEEN;  break;
            default: prom = -1; break;
            }
            return new Move(from, to, prom);
        }

        boolean isValid() {
            return flags != 0x01000000;
        }

        private int extractInt(int offs) {
            return AbkBook.extractInt(data, offs);
        }
    }

    /** Convert 4 bytes starting at "offs" in buf[] to an integer. */
    private static int extractInt(byte[] buf, int offs) {
        int ret = 0;
        for (int i = 3; i >= 0; i--) {
            int b = buf[offs + i];
            if (b < 0) b += 256;
            ret = (ret << 8) + b;
        }
        return ret;
    }

    private static class BookSettings {
        private byte[] buf = new byte[256];

        int minGames;
        int minWonGames;
        int minWinPercentWhite;     // 0 - 100
        int minWinPercentBlack;     // 0 - 100

        int prioImportance;         // 0 - 15
        int nGamesImportance;       // 0 - 15
        int scoreImportance;        // 0 - 15

        int maxPly;

        boolean skipPrio0Moves = false; // Not stored in abk file

        public BookSettings(RandomAccessFile abkF) throws IOException {
            abkF.seek(0);
            abkF.readFully(buf);

            minGames = getInt(0xde, Integer.MAX_VALUE);
            minWonGames = getInt(0xe2, Integer.MAX_VALUE);
            minWinPercentWhite = getInt(0xe6, 100);
            minWinPercentBlack = getInt(0xea, 100);

            prioImportance   = getInt(0xee, 15);
            nGamesImportance = getInt(0xf2, 15);
            scoreImportance  = getInt(0xf6, 15);

            maxPly = getInt(0xfa, 9999);

            if (prioImportance == 0 && nGamesImportance == 0 && scoreImportance == 0) {
                minGames = 0;
                minWonGames = 0;
                minWinPercentWhite = 0;
                minWinPercentBlack = 0;
            }
        }

        private int getInt(int offs, int maxVal) {
            int val = extractInt(buf, offs);
            return Math.min(Math.max(val, 0), maxVal);
        }
    }

    private static double scaleWeight(double w, int importance) {
        double e;
        switch (importance) {
        case 0:
            return 0;
        case 1:
            e = 0.66;
            break;
        case 2:
            e = 0.86;
            break;
        default:
            e = 1 + ((double)importance - 3) / 6;
            break;
        }
        return Math.pow(w, e);
    }
}
