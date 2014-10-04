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

package org.petero.droidfish.book;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.petero.droidfish.book.DroidBook.BookEntry;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;
import org.petero.droidfish.gamelogic.UndoInfo;

class CtgBook implements IOpeningBook {
    private BookOptions options = new BookOptions();
    private File ctgFile;
    private File ctbFile;
    private File ctoFile;

    static boolean canHandle(BookOptions options) {
        String filename = options.filename;
        return (filename.endsWith(".ctg") ||
                filename.endsWith(".ctb") ||
                filename.endsWith(".cto"));
    }

    @Override
    public boolean enabled() {
        return ctgFile.canRead() &&
               ctbFile.canRead() &&
               ctoFile.canRead();
    }

    @Override
    public void setOptions(BookOptions options) {
        this.options = new BookOptions(options);
        String fileName = options.filename;
        int len = fileName.length();
        ctgFile = new File(fileName.substring(0, len-1) + "g");
        ctbFile = new File(fileName.substring(0, len-1) + "b");
        ctoFile = new File(fileName.substring(0, len-1) + "o");
    }

    @Override
    public ArrayList<BookEntry> getBookEntries(Position pos) {
        RandomAccessFile ctgF = null;
        RandomAccessFile ctbF = null;
        RandomAccessFile ctoF = null;
        try {
            ctgF = new RandomAccessFile(ctgFile, "r");
            ctbF = new RandomAccessFile(ctbFile, "r");
            ctoF = new RandomAccessFile(ctoFile, "r");

            CtbFile ctb = new CtbFile(ctbF);
            CtoFile cto = new CtoFile(ctoF);
            CtgFile ctg = new CtgFile(ctgF, ctb, cto);

            ArrayList<BookEntry> ret = null;
            PositionData pd = ctg.getPositionData(pos);
            if (pd != null) {
                boolean mirrorColor = pd.mirrorColor;
                boolean mirrorLeftRight = pd.mirrorLeftRight;
                ret = pd.getBookMoves();
                UndoInfo ui = new UndoInfo();
                for (BookEntry be : ret) {
                    pd.pos.makeMove(be.move, ui);
                    PositionData movePd = ctg.getPositionData(pd.pos);
                    pd.pos.unMakeMove(be.move, ui);
                    float weight = be.weight;
                    if (movePd == null) {
//                        System.out.printf("%s : no pos\n", TextIO.moveToUCIString(be.move));
                        weight = 0;
                    } else {
                        int recom = movePd.getRecommendation();
                        if ((recom >= 64) && (recom < 128)) {
                            if (options.tournamentMode)
                                weight = 0;
                        } else if (recom >= 128) {
                            if (options.preferMainLines)
                                weight *= 10;
                        }
                        float score = movePd.getOpponentScore() + 1e-4f;
//                      double w0 = weight;
                        weight = weight * score;
//                      System.out.printf("%s : w0:%.3f rec:%d score:%d %.3f\n", TextIO.moveToUCIString(be.move),
//                                        w0, recom, score, weight);
                    }
                    be.weight = weight;
                }
                if (mirrorLeftRight) {
                    for (int i = 0; i < ret.size(); i++)
                        ret.get(i).move = mirrorMoveLeftRight(ret.get(i).move);
                }
                if (mirrorColor) {
                    for (int i = 0; i < ret.size(); i++)
                        ret.get(i).move = mirrorMoveColor(ret.get(i).move);
                }
            }
            return ret;
        } catch (IOException e) {
            return null;
        } finally {
            if (ctgF != null) try { ctgF.close(); } catch (IOException e) { }
            if (ctbF != null) try { ctbF.close(); } catch (IOException e) { }
            if (ctoF != null) try { ctoF.close(); } catch (IOException e) { }
        }
    }

    /** Read len bytes from offs in file f. */
    private final static byte[] readBytes(RandomAccessFile f, long offs, int len) throws IOException {
        byte[] ret = new byte[len];
        f.seek(offs);
        f.readFully(ret);
        return ret;
    }

    /** Convert len bytes starting at offs in buf to an integer. */
    private final static int extractInt(byte[] buf, int offs, int len) {
        int ret = 0;
        for (int i = 0; i < len; i++) {
            int b = buf[offs + i];
            if (b < 0) b += 256;
            ret = (ret << 8) + b;
        }
        return ret;
    }

    private final static class CtbFile {
        int lowerPageBound;
        int upperPageBound;
        CtbFile(RandomAccessFile f) throws IOException {
            byte[] buf = readBytes(f, 4, 8);
            lowerPageBound = extractInt(buf, 0, 4);
            upperPageBound = extractInt(buf, 4, 4);
        }
    }

    private final static class BitVector {
        private List<Byte> buf = new ArrayList<Byte>();
        private int length = 0;

        void addBit(boolean value) {
            int byteIdx = length / 8;
            int bitIdx = 7 - (length & 7);
            while (buf.size() <= byteIdx)
                buf.add(Byte.valueOf((byte)0));
            if (value)
                buf.set(byteIdx, (byte)(buf.get(byteIdx) | (1 << bitIdx)));
            length++;
        }

        void addBits(int mask, int numBits) {
            for (int i = 0; i < numBits; i++) {
                int b = numBits - 1 - i;
                addBit((mask & (1 << b)) != 0);
            }
        }

        /** Number of bits left in current byte. */
        int padBits() {
            int bitIdx = length & 7;
            return (bitIdx == 0) ? 0 : 8 - bitIdx;
        }

        final byte[] toByteArray() {
            byte[] ret = new byte[buf.size()];
            for (int i = 0; i < buf.size(); i++)
                ret[i] = buf.get(i);
            return ret;
        }
    }

    /** Converts a position to a byte array. */
    private final static byte[] positionToByteArray(Position pos) {
        BitVector bits = new BitVector();
        bits.addBits(0, 8); // Header byte
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int p = pos.getPiece(Position.getSquare(x, y));
                switch (p) {
                case Piece.EMPTY:   bits.addBits(0x00, 1); break;
                case Piece.WKING:   bits.addBits(0x20, 6); break;
                case Piece.WQUEEN:  bits.addBits(0x22, 6); break;
                case Piece.WROOK:   bits.addBits(0x16, 5); break;
                case Piece.WBISHOP: bits.addBits(0x14, 5); break;
                case Piece.WKNIGHT: bits.addBits(0x12, 5); break;
                case Piece.WPAWN:   bits.addBits(0x06, 3); break;
                case Piece.BKING:   bits.addBits(0x21, 6); break;
                case Piece.BQUEEN:  bits.addBits(0x23, 6); break;
                case Piece.BROOK:   bits.addBits(0x17, 5); break;
                case Piece.BBISHOP: bits.addBits(0x15, 5); break;
                case Piece.BKNIGHT: bits.addBits(0x13, 5); break;
                case Piece.BPAWN:   bits.addBits(0x07, 3); break;
                }
            }
        }

        TextIO.fixupEPSquare(pos);
        boolean ep = pos.getEpSquare() != -1;
        boolean cs = pos.getCastleMask() != 0;
        if (!ep && !cs)
            bits.addBit(false); // At least one pad bit

        int specialBits = (ep ? 3 : 0) + (cs ? 4 : 0);
        while (bits.padBits() != specialBits)
            bits.addBit(false);

        if (ep)
            bits.addBits(Position.getX(pos.getEpSquare()), 3);
        if (cs) {
            bits.addBit(pos.h8Castle());
            bits.addBit(pos.a8Castle());
            bits.addBit(pos.h1Castle());
            bits.addBit(pos.a1Castle());
        }

        if ((bits.length & 7) != 0) throw new RuntimeException();
        int header = bits.length / 8;
        if (ep) header |= 0x20;
        if (cs) header |= 0x40;

        byte[] buf = bits.toByteArray();
        buf[0] = (byte)header;
        return buf;
    }

    private final static class CtoFile {
        RandomAccessFile f;
        CtoFile(RandomAccessFile f) {
            this.f = f;
        }

        final static ArrayList<Integer> getHashIndices(byte[] encodedPos, CtbFile ctb) throws IOException {
            ArrayList<Integer> ret = new ArrayList<Integer>();
            int hash = getHashValue(encodedPos);
            for (int n = 0; n < 0x7fffffff; n = 2*n + 1) {
                int c = (hash & n) + n;
                if (c < ctb.lowerPageBound)
                    continue;
                ret.add(c);
                if (c >= ctb.upperPageBound)
                    break;
            }
            return ret;
        }

        final int getPage(int hashIndex) throws IOException {
            byte[] buf = readBytes(f, 16 + 4 * hashIndex, 4);
            int page = extractInt(buf, 0, 4);
            return page;
        }

        private final static int tbl[] = {
            0x3100d2bf, 0x3118e3de, 0x34ab1372, 0x2807a847,
            0x1633f566, 0x2143b359, 0x26d56488, 0x3b9e6f59,
            0x37755656, 0x3089ca7b, 0x18e92d85, 0x0cd0e9d8,
            0x1a9e3b54, 0x3eaa902f, 0x0d9bfaae, 0x2f32b45b,
            0x31ed6102, 0x3d3c8398, 0x146660e3, 0x0f8d4b76,
            0x02c77a5f, 0x146c8799, 0x1c47f51f, 0x249f8f36,
            0x24772043, 0x1fbc1e4d, 0x1e86b3fa, 0x37df36a6,
            0x16ed30e4, 0x02c3148e, 0x216e5929, 0x0636b34e,
            0x317f9f56, 0x15f09d70, 0x131026fb, 0x38c784b1,
            0x29ac3305, 0x2b485dc5, 0x3c049ddc, 0x35a9fbcd,
            0x31d5373b, 0x2b246799, 0x0a2923d3, 0x08a96e9d,
            0x30031a9f, 0x08f525b5, 0x33611c06, 0x2409db98,
            0x0ca4feb2, 0x1000b71e, 0x30566e32, 0x39447d31,
            0x194e3752, 0x08233a95, 0x0f38fe36, 0x29c7cd57,
            0x0f7b3a39, 0x328e8a16, 0x1e7d1388, 0x0fba78f5,
            0x274c7e7c, 0x1e8be65c, 0x2fa0b0bb, 0x1eb6c371
        };

        private final static int getHashValue(byte[] encodedPos) {
            int hash = 0;
            int tmp = 0;
            for (int i = 0; i < encodedPos.length; i++) {
                int ch = encodedPos[i];
                tmp += ((0x0f - (ch & 0x0f)) << 2) + 1;
                hash += tbl[tmp & 0x3f];
                tmp += ((0xf0 - (ch & 0xf0)) >> 2) + 1;
                hash += tbl[tmp & 0x3f];
            }
            return hash;
        }
    }

    private final static class CtgFile {
        private RandomAccessFile f;
        private CtbFile ctb;
        private CtoFile cto;
        CtgFile(RandomAccessFile f, CtbFile ctb, CtoFile cto) {
            this.f = f;
            this.ctb = ctb;
            this.cto = cto;
        }

        final PositionData getPositionData(Position pos) throws IOException {
            boolean mirrorColor = !pos.whiteMove;
            boolean needCopy = true;
            if (mirrorColor) {
                pos = mirrorPosColor(pos);
                needCopy = false;
            }

            boolean mirrorLeftRight = false;
            if ((pos.getCastleMask() == 0) && (Position.getX(pos.getKingSq(true)) < 4)) {
                pos = mirrorPosLeftRight(pos);
                mirrorLeftRight = true;
                needCopy = false;
            }
            if (needCopy)
                pos = new Position(pos);

            byte[] encodedPos = positionToByteArray(pos);
            ArrayList<Integer> hashIdxList = CtoFile.getHashIndices(encodedPos, ctb);

            PositionData pd = null;
            for (int i = 0; i < hashIdxList.size(); i++) {
                int page = cto.getPage(hashIdxList.get(i));
                if (page < 0)
                    continue;
                pd = findInPage(page, encodedPos);
                if (pd != null) {
                    pd.pos = pos;
                    pd.mirrorColor = mirrorColor;
                    pd.mirrorLeftRight = mirrorLeftRight;
                    break;
                }
            }
            return pd;
        }

        private final PositionData findInPage(int page, byte[] encodedPos) throws IOException {
            byte[] pageBuf = readBytes(f, (page+1)*4096L, 4096);
            try {
                int nPos = extractInt(pageBuf, 0, 2);
                int nBytes = extractInt(pageBuf, 2, 2);
                for (int i = nBytes; i < 4096; i++)
                    pageBuf[i] = 0; // Don't depend on trailing garbage
                int offs = 4;
                for (int p = 0; p < nPos; p++) {
                    boolean match = true;
                    for (int i = 0; i < encodedPos.length; i++)
                        if (encodedPos[i] != pageBuf[offs+i]) {
                            match = false;
                            break;
                        }
                    if (match)
                        return new PositionData(pageBuf, offs);

                    int posLen = pageBuf[offs] & 0x1f;
                    offs += posLen;
                    int moveBytes = extractInt(pageBuf, offs, 1);
                    offs += moveBytes;
                    offs += PositionData.posInfoBytes;
                }
                return null;
            } catch (ArrayIndexOutOfBoundsException ex) {
                return null; // Ignore corrupt book file entries
            }
        }
    }

    private final static class PositionData {
        private byte[] buf;
        private int posLen;
        private int moveBytes;
        final static int posInfoBytes = 3*4 + 4 + (3+4)*2 + 1 + 1 + 1;

        Position pos;
        boolean mirrorColor = false;
        boolean mirrorLeftRight = false;

        PositionData(byte[] pageBuf, int offs) {
            posLen = pageBuf[offs] & 0x1f;
            moveBytes = extractInt(pageBuf, offs + posLen, 1);
            int bufLen = posLen + moveBytes + posInfoBytes;
            buf = new byte[bufLen];
            for (int i = 0; i < bufLen; i++)
                buf[i] = pageBuf[offs + i];
        }

        final ArrayList<BookEntry> getBookMoves() {
            ArrayList<BookEntry> entries = new ArrayList<BookEntry>();
            int nMoves = (moveBytes - 1) / 2;
            for (int mi = 0; mi < nMoves; mi++) {
                int move  = extractInt(buf, posLen + 1 + mi * 2, 1);
                int flags = extractInt(buf, posLen + 1 + mi * 2 + 1, 1);
                Move m = decodeMove(pos, move);
                if (m == null)
                    continue;
//                System.out.printf("mi:%d m:%s flags:%d\n", mi, TextIO.moveToUCIString(m), flags);
                BookEntry ent = new BookEntry(m);
                switch (flags) {
                default:
                case 0x00: ent.weight = 1;       break; // No annotation
                case 0x01: ent.weight = 8;       break; // !
                case 0x02: ent.weight = 0;       break; // ?
                case 0x03: ent.weight = 32;      break; // !!
                case 0x04: ent.weight = 0;       break; // ??
                case 0x05: ent.weight = 0.5f;    break; // !?
                case 0x06: ent.weight = 0.125f;  break; // ?!
                case 0x08: ent.weight = 1000000; break; // Only move
                }
                entries.add(ent);
            }
            return entries;
        }

        /** Return (loss * 2 + draws). */
        final int getOpponentScore() {
            int statStart = posLen + moveBytes;
//            int wins  = extractInt(buf, statStart + 3, 3);
            int loss  = extractInt(buf, statStart + 6, 3);
            int draws = extractInt(buf, statStart + 9, 3);
            return loss * 2 + draws;
        }

        final int getRecommendation() {
            int statStart = posLen + moveBytes;
            int recom = extractInt(buf, statStart + 30, 1);
            return recom;
        }

        private static final class MoveInfo {
            int piece;
            int pieceNo;
            int dx;
            int dy;
        }

        private final static MoveInfo MI(int piece, int pieceNo, int dx, int dy) {
            MoveInfo mi = new MoveInfo();
            mi.piece = piece;
            mi.pieceNo = pieceNo;
            mi.dx = dx;
            mi.dy = dy;
            return mi;
        }

        private final static MoveInfo[] moveInfo = new MoveInfo[256];
        static {
            moveInfo[0x00] = MI(Piece.WPAWN  , 4, +1, +1);
            moveInfo[0x01] = MI(Piece.WKNIGHT, 1, -2, -1);
            moveInfo[0x03] = MI(Piece.WQUEEN , 1, +2, +0);
            moveInfo[0x04] = MI(Piece.WPAWN  , 1, +0, +1);
            moveInfo[0x05] = MI(Piece.WQUEEN , 0, +0, +1);
            moveInfo[0x06] = MI(Piece.WPAWN  , 3, -1, +1);
            moveInfo[0x08] = MI(Piece.WQUEEN , 1, +4, +0);
            moveInfo[0x09] = MI(Piece.WBISHOP, 1, +6, +6);
            moveInfo[0x0a] = MI(Piece.WKING  , 0, +0, -1);
            moveInfo[0x0c] = MI(Piece.WPAWN  , 0, -1, +1);
            moveInfo[0x0d] = MI(Piece.WBISHOP, 0, +3, +3);
            moveInfo[0x0e] = MI(Piece.WROOK  , 1, +3, +0);
            moveInfo[0x0f] = MI(Piece.WKNIGHT, 0, -2, -1);
            moveInfo[0x12] = MI(Piece.WBISHOP, 0, +7, +7);
            moveInfo[0x13] = MI(Piece.WKING  , 0, +0, +1);
            moveInfo[0x14] = MI(Piece.WPAWN  , 7, +1, +1);
            moveInfo[0x15] = MI(Piece.WBISHOP, 0, +5, +5);
            moveInfo[0x18] = MI(Piece.WPAWN  , 6, +0, +1);
            moveInfo[0x1a] = MI(Piece.WQUEEN , 1, +0, +6);
            moveInfo[0x1b] = MI(Piece.WBISHOP, 0, -1, +1);
            moveInfo[0x1d] = MI(Piece.WBISHOP, 1, +7, +7);
            moveInfo[0x21] = MI(Piece.WROOK  , 1, +7, +0);
            moveInfo[0x22] = MI(Piece.WBISHOP, 1, -2, +2);
            moveInfo[0x23] = MI(Piece.WQUEEN , 1, +6, +6);
            moveInfo[0x24] = MI(Piece.WPAWN  , 7, -1, +1);
            moveInfo[0x26] = MI(Piece.WBISHOP, 0, -7, +7);
            moveInfo[0x27] = MI(Piece.WPAWN  , 2, -1, +1);
            moveInfo[0x28] = MI(Piece.WQUEEN , 0, +5, +5);
            moveInfo[0x29] = MI(Piece.WQUEEN , 0, +6, +0);
            moveInfo[0x2a] = MI(Piece.WKNIGHT, 1, +1, -2);
            moveInfo[0x2d] = MI(Piece.WPAWN  , 5, +1, +1);
            moveInfo[0x2e] = MI(Piece.WBISHOP, 0, +1, +1);
            moveInfo[0x2f] = MI(Piece.WQUEEN , 0, +1, +0);
            moveInfo[0x30] = MI(Piece.WKNIGHT, 1, -1, -2);
            moveInfo[0x31] = MI(Piece.WQUEEN , 0, +3, +0);
            moveInfo[0x32] = MI(Piece.WBISHOP, 1, +5, +5);
            moveInfo[0x34] = MI(Piece.WKNIGHT, 0, +1, +2);
            moveInfo[0x36] = MI(Piece.WKNIGHT, 0, +2, +1);
            moveInfo[0x37] = MI(Piece.WQUEEN , 0, +0, +4);
            moveInfo[0x38] = MI(Piece.WQUEEN , 1, -4, +4);
            moveInfo[0x39] = MI(Piece.WQUEEN , 0, +5, +0);
            moveInfo[0x3a] = MI(Piece.WBISHOP, 0, +6, +6);
            moveInfo[0x3b] = MI(Piece.WQUEEN , 1, -5, +5);
            moveInfo[0x3c] = MI(Piece.WBISHOP, 0, -5, +5);
            moveInfo[0x41] = MI(Piece.WQUEEN , 1, +5, +5);
            moveInfo[0x42] = MI(Piece.WQUEEN , 0, -7, +7);
            moveInfo[0x44] = MI(Piece.WKING  , 0, +1, -1);
            moveInfo[0x45] = MI(Piece.WQUEEN , 0, +3, +3);
            moveInfo[0x4a] = MI(Piece.WPAWN  , 7, +0, +2);
            moveInfo[0x4b] = MI(Piece.WQUEEN , 0, -5, +5);
            moveInfo[0x4c] = MI(Piece.WKNIGHT, 1, +1, +2);
            moveInfo[0x4d] = MI(Piece.WQUEEN , 1, +0, +1);
            moveInfo[0x50] = MI(Piece.WROOK  , 0, +0, +6);
            moveInfo[0x52] = MI(Piece.WROOK  , 0, +6, +0);
            moveInfo[0x54] = MI(Piece.WBISHOP, 1, -1, +1);
            moveInfo[0x55] = MI(Piece.WPAWN  , 2, +0, +1);
            moveInfo[0x5c] = MI(Piece.WPAWN  , 6, +1, +1);
            moveInfo[0x5f] = MI(Piece.WPAWN  , 4, +0, +2);
            moveInfo[0x61] = MI(Piece.WQUEEN , 0, +6, +6);
            moveInfo[0x62] = MI(Piece.WPAWN  , 1, +0, +2);
            moveInfo[0x63] = MI(Piece.WQUEEN , 1, -7, +7);
            moveInfo[0x66] = MI(Piece.WBISHOP, 0, -3, +3);
            moveInfo[0x67] = MI(Piece.WKING  , 0, +1, +1);
            moveInfo[0x69] = MI(Piece.WROOK  , 1, +0, +7);
            moveInfo[0x6a] = MI(Piece.WBISHOP, 0, +4, +4);
            moveInfo[0x6b] = MI(Piece.WKING  , 0, +2, +0);
            moveInfo[0x6e] = MI(Piece.WROOK  , 0, +5, +0);
            moveInfo[0x6f] = MI(Piece.WQUEEN , 1, +7, +7);
            moveInfo[0x72] = MI(Piece.WBISHOP, 1, -7, +7);
            moveInfo[0x74] = MI(Piece.WQUEEN , 0, +2, +0);
            moveInfo[0x79] = MI(Piece.WBISHOP, 1, -6, +6);
            moveInfo[0x7a] = MI(Piece.WROOK  , 0, +0, +3);
            moveInfo[0x7b] = MI(Piece.WROOK  , 1, +0, +6);
            moveInfo[0x7c] = MI(Piece.WPAWN  , 2, +1, +1);
            moveInfo[0x7d] = MI(Piece.WROOK  , 1, +0, +1);
            moveInfo[0x7e] = MI(Piece.WQUEEN , 0, -3, +3);
            moveInfo[0x7f] = MI(Piece.WROOK  , 0, +1, +0);
            moveInfo[0x80] = MI(Piece.WQUEEN , 0, -6, +6);
            moveInfo[0x81] = MI(Piece.WROOK  , 0, +0, +1);
            moveInfo[0x82] = MI(Piece.WPAWN  , 5, -1, +1);
            moveInfo[0x85] = MI(Piece.WKNIGHT, 0, -1, +2);
            moveInfo[0x86] = MI(Piece.WROOK  , 0, +7, +0);
            moveInfo[0x87] = MI(Piece.WROOK  , 0, +0, +5);
            moveInfo[0x8a] = MI(Piece.WKNIGHT, 0, +1, -2);
            moveInfo[0x8b] = MI(Piece.WPAWN  , 0, +1, +1);
            moveInfo[0x8c] = MI(Piece.WKING  , 0, -1, -1);
            moveInfo[0x8e] = MI(Piece.WQUEEN , 1, -2, +2);
            moveInfo[0x8f] = MI(Piece.WQUEEN , 0, +7, +0);
            moveInfo[0x92] = MI(Piece.WQUEEN , 1, +1, +1);
            moveInfo[0x94] = MI(Piece.WQUEEN , 0, +0, +3);
            moveInfo[0x96] = MI(Piece.WPAWN  , 1, +1, +1);
            moveInfo[0x97] = MI(Piece.WKING  , 0, -1, +0);
            moveInfo[0x98] = MI(Piece.WROOK  , 0, +3, +0);
            moveInfo[0x99] = MI(Piece.WROOK  , 0, +0, +4);
            moveInfo[0x9a] = MI(Piece.WQUEEN , 0, +0, +6);
            moveInfo[0x9b] = MI(Piece.WPAWN  , 2, +0, +2);
            moveInfo[0x9d] = MI(Piece.WQUEEN , 0, +0, +2);
            moveInfo[0x9f] = MI(Piece.WBISHOP, 1, -4, +4);
            moveInfo[0xa0] = MI(Piece.WQUEEN , 1, +0, +3);
            moveInfo[0xa2] = MI(Piece.WQUEEN , 0, +2, +2);
            moveInfo[0xa3] = MI(Piece.WPAWN  , 7, +0, +1);
            moveInfo[0xa5] = MI(Piece.WROOK  , 1, +0, +5);
            moveInfo[0xa9] = MI(Piece.WROOK  , 1, +2, +0);
            moveInfo[0xab] = MI(Piece.WQUEEN , 1, -6, +6);
            moveInfo[0xad] = MI(Piece.WROOK  , 1, +4, +0);
            moveInfo[0xae] = MI(Piece.WQUEEN , 1, +3, +3);
            moveInfo[0xb0] = MI(Piece.WQUEEN , 1, +0, +4);
            moveInfo[0xb1] = MI(Piece.WPAWN  , 5, +0, +2);
            moveInfo[0xb2] = MI(Piece.WBISHOP, 0, -6, +6);
            moveInfo[0xb5] = MI(Piece.WROOK  , 1, +5, +0);
            moveInfo[0xb7] = MI(Piece.WQUEEN , 0, +0, +5);
            moveInfo[0xb9] = MI(Piece.WBISHOP, 1, +3, +3);
            moveInfo[0xbb] = MI(Piece.WPAWN  , 4, +0, +1);
            moveInfo[0xbc] = MI(Piece.WQUEEN , 1, +5, +0);
            moveInfo[0xbd] = MI(Piece.WQUEEN , 1, +0, +2);
            moveInfo[0xbe] = MI(Piece.WKING  , 0, +1, +0);
            moveInfo[0xc1] = MI(Piece.WBISHOP, 0, +2, +2);
            moveInfo[0xc2] = MI(Piece.WBISHOP, 1, +2, +2);
            moveInfo[0xc3] = MI(Piece.WBISHOP, 0, -2, +2);
            moveInfo[0xc4] = MI(Piece.WROOK  , 1, +1, +0);
            moveInfo[0xc5] = MI(Piece.WROOK  , 1, +0, +4);
            moveInfo[0xc6] = MI(Piece.WQUEEN , 1, +0, +5);
            moveInfo[0xc7] = MI(Piece.WPAWN  , 6, -1, +1);
            moveInfo[0xc8] = MI(Piece.WPAWN  , 6, +0, +2);
            moveInfo[0xc9] = MI(Piece.WQUEEN , 1, +0, +7);
            moveInfo[0xca] = MI(Piece.WBISHOP, 1, -3, +3);
            moveInfo[0xcb] = MI(Piece.WPAWN  , 5, +0, +1);
            moveInfo[0xcc] = MI(Piece.WBISHOP, 1, -5, +5);
            moveInfo[0xcd] = MI(Piece.WROOK  , 0, +2, +0);
            moveInfo[0xcf] = MI(Piece.WPAWN  , 3, +0, +1);
            moveInfo[0xd1] = MI(Piece.WPAWN  , 1, -1, +1);
            moveInfo[0xd2] = MI(Piece.WKNIGHT, 1, +2, +1);
            moveInfo[0xd3] = MI(Piece.WKNIGHT, 1, -2, +1);
            moveInfo[0xd7] = MI(Piece.WQUEEN , 0, -1, +1);
            moveInfo[0xd8] = MI(Piece.WROOK  , 1, +6, +0);
            moveInfo[0xd9] = MI(Piece.WQUEEN , 0, -2, +2);
            moveInfo[0xda] = MI(Piece.WKNIGHT, 0, -1, -2);
            moveInfo[0xdb] = MI(Piece.WPAWN  , 0, +0, +2);
            moveInfo[0xde] = MI(Piece.WPAWN  , 4, -1, +1);
            moveInfo[0xdf] = MI(Piece.WKING  , 0, -1, +1);
            moveInfo[0xe0] = MI(Piece.WKNIGHT, 1, +2, -1);
            moveInfo[0xe1] = MI(Piece.WROOK  , 0, +0, +7);
            moveInfo[0xe3] = MI(Piece.WROOK  , 1, +0, +3);
            moveInfo[0xe5] = MI(Piece.WQUEEN , 0, +4, +0);
            moveInfo[0xe6] = MI(Piece.WPAWN  , 3, +0, +2);
            moveInfo[0xe7] = MI(Piece.WQUEEN , 0, +4, +4);
            moveInfo[0xe8] = MI(Piece.WROOK  , 0, +0, +2);
            moveInfo[0xe9] = MI(Piece.WKNIGHT, 0, +2, -1);
            moveInfo[0xeb] = MI(Piece.WPAWN  , 3, +1, +1);
            moveInfo[0xec] = MI(Piece.WPAWN  , 0, +0, +1);
            moveInfo[0xed] = MI(Piece.WQUEEN , 0, +7, +7);
            moveInfo[0xee] = MI(Piece.WQUEEN , 1, -1, +1);
            moveInfo[0xef] = MI(Piece.WROOK  , 0, +4, +0);
            moveInfo[0xf0] = MI(Piece.WQUEEN , 1, +7, +0);
            moveInfo[0xf1] = MI(Piece.WQUEEN , 0, +1, +1);
            moveInfo[0xf3] = MI(Piece.WKNIGHT, 1, -1, +2);
            moveInfo[0xf4] = MI(Piece.WROOK  , 1, +0, +2);
            moveInfo[0xf5] = MI(Piece.WBISHOP, 1, +1, +1);
            moveInfo[0xf6] = MI(Piece.WKING  , 0, -2, +0);
            moveInfo[0xf7] = MI(Piece.WKNIGHT, 0, -2, +1);
            moveInfo[0xf8] = MI(Piece.WQUEEN , 1, +1, +0);
            moveInfo[0xf9] = MI(Piece.WQUEEN , 1, +0, +6);
            moveInfo[0xfa] = MI(Piece.WQUEEN , 1, +3, +0);
            moveInfo[0xfb] = MI(Piece.WQUEEN , 1, +2, +2);
            moveInfo[0xfd] = MI(Piece.WQUEEN , 0, +0, +7);
            moveInfo[0xfe] = MI(Piece.WQUEEN , 1, -3, +3);
        }

        private final static int findPiece(Position pos, int piece, int pieceNo) {
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++) {
                    int sq = Position.getSquare(x, y);
                    if (pos.getPiece(sq) == piece)
                        if (pieceNo-- == 0)
                            return sq;
                }
            return -1;
        }

        private final Move decodeMove(Position pos, int moveCode) {
            MoveInfo mi = moveInfo[moveCode];
            if (mi == null)
                return null;
            int from = findPiece(pos, mi.piece, mi.pieceNo);
            if (from < 0)
                return null;
            int toX = (Position.getX(from) + mi.dx) & 7;
            int toY = (Position.getY(from) + mi.dy) & 7;
            int to = Position.getSquare(toX, toY);
            int promoteTo = Piece.EMPTY;
            if ((pos.getPiece(from) == Piece.WPAWN) && (toY == 7))
                promoteTo = Piece.WQUEEN;
            Move m = new Move(from, to, promoteTo);
            return m;
        }
    }

    private final static int mirrorSquareColor(int sq) {
        int x = Position.getX(sq);
        int y = 7 - Position.getY(sq);
        return Position.getSquare(x, y);
    }

    private final static int mirrorPieceColor(int piece) {
        if (Piece.isWhite(piece)) {
            piece = Piece.makeBlack(piece);
        } else {
            piece = Piece.makeWhite(piece);
        }
        return piece;
    }

    private final static Position mirrorPosColor(Position pos) {
        Position ret = new Position(pos);
        for (int sq = 0; sq < 64; sq++) {
            int mSq = mirrorSquareColor(sq);
            int piece = pos.getPiece(sq);
            int mPiece = mirrorPieceColor(piece);
            ret.setPiece(mSq, mPiece);
        }
        ret.setWhiteMove(!pos.whiteMove);
        int castleMask = 0;
        if (pos.a1Castle()) castleMask |= (1 << Position.A8_CASTLE);
        if (pos.h1Castle()) castleMask |= (1 << Position.H8_CASTLE);
        if (pos.a8Castle()) castleMask |= (1 << Position.A1_CASTLE);
        if (pos.h8Castle()) castleMask |= (1 << Position.H1_CASTLE);
        ret.setCastleMask(castleMask);
        int epSquare = pos.getEpSquare();
        if (epSquare >= 0) {
            int mEpSquare = mirrorSquareColor(epSquare);
            ret.setEpSquare(mEpSquare);
        }
        ret.halfMoveClock = pos.halfMoveClock;
        ret.fullMoveCounter = pos.fullMoveCounter;
        return ret;
    }

    private final static Move mirrorMoveColor(Move m) {
        if (m == null) return null;
        Move ret = new Move(m);
        ret.from = mirrorSquareColor(m.from);
        ret.to = mirrorSquareColor(m.to);
        ret.promoteTo = mirrorPieceColor(m.promoteTo);
        return ret;
    }

    private final static int mirrorSquareLeftRight(int sq) {
        int x = 7 - Position.getX(sq);
        int y = Position.getY(sq);
        return Position.getSquare(x, y);
    }

    private final static Position mirrorPosLeftRight(Position pos) {
        Position ret = new Position(pos);
        for (int sq = 0; sq < 64; sq++) {
            int mSq = mirrorSquareLeftRight(sq);
            int piece = pos.getPiece(sq);
            ret.setPiece(mSq, piece);
        }
        int epSquare = pos.getEpSquare();
        if (epSquare >= 0) {
            int mEpSquare = mirrorSquareLeftRight(epSquare);
            ret.setEpSquare(mEpSquare);
        }
        ret.halfMoveClock = pos.halfMoveClock;
        ret.fullMoveCounter = pos.fullMoveCounter;
        return ret;
    }

    private final static Move mirrorMoveLeftRight(Move m) {
        if (m == null) return null;
        Move ret = new Move(m);
        ret.from = mirrorSquareLeftRight(m.from);
        ret.to = mirrorSquareLeftRight(m.to);
        ret.promoteTo = m.promoteTo;
        return ret;
    }
}
