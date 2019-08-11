/*
    CuckooChess - A java chess program.
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

package chess;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Implements an opening book. */
public class Book {
    /** Creates the book.bin file. */
    public static void main(String[] args) throws IOException {
        String inFile = args[0];
        String outFile = args[1];
        main2(inFile, outFile);
    }
    public static void main2(String inFile, String outFile) throws IOException {
        List<Byte> binBook = createBinBook(inFile);
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            int bookLen = binBook.size();
            byte[] binBookA = new byte[bookLen];
            for (int i = 0; i < bookLen; i++)
                binBookA[i] = binBook.get(i);
            out.write(binBookA);
        }
    }

    public static List<Byte> createBinBook(String inFileName) {
        List<Byte> binBook = new ArrayList<>(0);
        try (InputStream inStream = new FileInputStream(inFileName);
            InputStreamReader inFile = new InputStreamReader(inStream);
            BufferedReader inBuf = new BufferedReader(inFile);
            LineNumberReader lnr = new LineNumberReader(inBuf)) {
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
        } catch (ChessParseError ex) {
            throw new RuntimeException();
        } catch (IOException ex) {
            System.out.println("Can't read opening book resource");
            throw new RuntimeException();
        }
        return binBook;
    }

    /** Add a sequence of moves, starting from the initial position, to the binary opening book. */
    private static boolean addBookLine(String line, List<Byte> binBook) throws ChessParseError {
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

    private static int pieceToProm(int p) {
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
