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

package org.petero.droidfish.engine;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.petero.droidfish.BookOptions;
import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.MoveGen;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.Pair;
import org.petero.droidfish.gamelogic.UndoInfo;

import chess.Piece;

/**
 * Implements an opening book.
 * @author petero
 */
public final class DroidBook {
    static class BookEntry {
        Move move;
        double weight;
        BookEntry(Move move) {
            this.move = move;
            weight = 1;
        }
        @Override
        public String toString() {
            return TextIO.moveToUCIString(move) + " (" + weight + ")";
        }
    }
    private Random rndGen = new SecureRandom();

    private IOpeningBook externalBook = new NullBook();
    private IOpeningBook internalBook = new InternalBook();
    private BookOptions options = null;

    private static final DroidBook INSTANCE = new DroidBook();

    /** Get singleton instance. */
    public static DroidBook getInstance() {
        return INSTANCE;
    }

    private DroidBook() {
        rndGen.setSeed(System.currentTimeMillis());
    }

    /** Set opening book options. */
    public final synchronized void setOptions(BookOptions options) {
        this.options = options;
        if (CtgBook.canHandle(options))
            externalBook = new CtgBook();
        else if (PolyglotBook.canHandle(options))
            externalBook = new PolyglotBook();
        else
            externalBook = new NullBook();
        externalBook.setOptions(options);
        internalBook.setOptions(options);
    }

    /** Return a random book move for a position, or null if out of book. */
    public final synchronized Move getBookMove(Position pos) {
        if ((options != null) && (pos.fullMoveCounter > options.maxLength))
            return null;
        List<BookEntry> bookMoves = getBook().getBookEntries(pos);
        if (bookMoves == null)
            return null;

        ArrayList<Move> legalMoves = new MoveGen().pseudoLegalMoves(pos);
        legalMoves = MoveGen.removeIllegal(pos, legalMoves);
        double sum = 0;
        final int nMoves = bookMoves.size();
        for (int i = 0; i < nMoves; i++) {
            BookEntry be = bookMoves.get(i);
            if (!legalMoves.contains(be.move)) {
                // If an illegal move was found, it means there was a hash collision,
                // or a corrupt external book file.
                return null;
            }
            sum += scaleWeight(bookMoves.get(i).weight);
        }
        if (sum <= 0) {
            return null;
        }
        double rnd = rndGen.nextDouble() * sum;
        sum = 0;
        for (int i = 0; i < nMoves; i++) {
            sum += scaleWeight(bookMoves.get(i).weight);
            if (rnd < sum)
                return bookMoves.get(i).move;
        }
        return bookMoves.get(nMoves-1).move;
    }

    /** Return all book moves, both as a formatted string and as a list of moves. */
    public final synchronized Pair<String,ArrayList<Move>> getAllBookMoves(Position pos) {
        StringBuilder ret = new StringBuilder();
        ArrayList<Move> bookMoveList = new ArrayList<Move>();
        List<BookEntry> bookMoves = getBook().getBookEntries(pos);
    
        // Check legality
        if (bookMoves != null) {
            ArrayList<Move> legalMoves = new MoveGen().pseudoLegalMoves(pos);
            legalMoves = MoveGen.removeIllegal(pos, legalMoves);
            for (int i = 0; i < bookMoves.size(); i++) {
                BookEntry be = bookMoves.get(i);
                if (!legalMoves.contains(be.move)) {
                    bookMoves = null;
                    break;
                }
            }
        }
    
        if (bookMoves != null) {
            Collections.sort(bookMoves, new Comparator<BookEntry>() {
                public int compare(BookEntry arg0, BookEntry arg1) {
                    double wd = arg1.weight - arg0.weight;
                    if (wd != 0)
                        return (wd > 0) ? 1 : -1;
                    String str0 = TextIO.moveToUCIString(arg0.move);
                    String str1 = TextIO.moveToUCIString(arg1.move);
                    return str0.compareTo(str1);
                }});
            double totalWeight = 0;
            for (BookEntry be : bookMoves)
                totalWeight += scaleWeight(be.weight);
            if (totalWeight <= 0) totalWeight = 1;
            for (BookEntry be : bookMoves) {
                Move m = be.move;
                bookMoveList.add(m);
                String moveStr = TextIO.moveToString(pos, m, false);
                ret.append(moveStr);
                ret.append(':');
                int percent = (int)Math.round(scaleWeight(be.weight) * 100 / totalWeight);
                ret.append(percent);
                ret.append(' ');
            }
        }
        return new Pair<String, ArrayList<Move>>(ret.toString(), bookMoveList);
    }

    private final double scaleWeight(double w) {
        if (w <= 0)
            return 0;
        if (options == null)
            return w;
        return Math.pow(w, Math.exp(-options.random));
    }

    private final IOpeningBook getBook() {
        if (externalBook.enabled()) {
            return externalBook;
        } else {
            return internalBook;
        }
    }

    /** Creates the book.bin file. */
    public static void main(String[] args) throws IOException {
        List<Byte> binBook = createBinBook();
        FileOutputStream out = new FileOutputStream("../src/book.bin");
        int bookLen = binBook.size();
        byte[] binBookA = new byte[bookLen];
        for (int i = 0; i < bookLen; i++)
            binBookA[i] = binBook.get(i);
        out.write(binBookA);
        out.close();
    }

    private static final List<Byte> createBinBook() {
        List<Byte> binBook = new ArrayList<Byte>(0);
        try {
            InputStream inStream = new Object().getClass().getResourceAsStream("/book.txt");
            InputStreamReader inFile = new InputStreamReader(inStream);
            BufferedReader inBuf = new BufferedReader(inFile, 8192);
            LineNumberReader lnr = new LineNumberReader(inBuf);
            String line;
            while ((line = lnr.readLine()) != null) {
                if (line.startsWith("#") || (line.length() == 0)) {
                    continue;
                }
                if (!addBookLine(line, binBook)) {
                    System.out.printf("Book parse error, line:%d\n", lnr.getLineNumber());
                    throw new RuntimeException();
                }
//              System.out.printf("no:%d line:%s%n", lnr.getLineNumber(), line);
            }
            lnr.close();
        } catch (ChessParseError ex) {
            throw new RuntimeException();
        } catch (IOException ex) {
            System.out.println("Can't read opening book resource");
            throw new RuntimeException();
        }
        return binBook;
    }

    /** Add a sequence of moves, starting from the initial position, to the binary opening book. */
    private static final boolean addBookLine(String line, List<Byte> binBook) throws ChessParseError {
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        UndoInfo ui = new UndoInfo();
        String[] strMoves = line.split(" ");
        for (String strMove : strMoves) {
//            System.out.printf("Adding move:%s\n", strMove);
            int bad = 0;
            if (strMove.endsWith("?")) {
                strMove = strMove.substring(0, strMove.length() - 1);
                bad = 1;
            }
            Move m = TextIO.stringToMove(pos, strMove);
            if (m == null) {
                return false;
            }
            int prom = pieceToProm(m.promoteTo);
            int val = m.from + (m.to << 6) + (prom << 12) + (bad << 15);
            binBook.add((byte)(val >> 8));
            binBook.add((byte)(val & 255));
            pos.makeMove(m, ui);
        }
        binBook.add((byte)0);
        binBook.add((byte)0);
        return true;
    }

    private static final int pieceToProm(int p) {
        switch (p) {
        case Piece.WQUEEN: case Piece.BQUEEN:
            return 1;
        case Piece.WROOK: case Piece.BROOK:
            return 2;
        case Piece.WBISHOP: case Piece.BBISHOP:
            return 3;
        case Piece.WKNIGHT: case Piece.BKNIGHT:
            return 4;
        default:
            return 0;
        }
    }
}
