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

public class BitBoard {

    /** Squares attacked by a king on a given square. */
    public static final long[] kingAttacks;
    public static final long[] knightAttacks;
    public static final long[] wPawnAttacks, bPawnAttacks;

    // Squares preventing a pawn from being a passed pawn, if occupied by enemy pawn
    static final long[] wPawnBlockerMask, bPawnBlockerMask;

    public static final long maskAToGFiles = 0x7F7F7F7F7F7F7F7FL;
    public static final long maskBToHFiles = 0xFEFEFEFEFEFEFEFEL;
    public static final long maskAToFFiles = 0x3F3F3F3F3F3F3F3FL;
    public static final long maskCToHFiles = 0xFCFCFCFCFCFCFCFCL;

    public static final long[] maskFile = {
        0x0101010101010101L,
        0x0202020202020202L,
        0x0404040404040404L,
        0x0808080808080808L,
        0x1010101010101010L,
        0x2020202020202020L,
        0x4040404040404040L,
        0x8080808080808080L
    };

    public static final long maskRow1      = 0x00000000000000FFL;
    public static final long maskRow2      = 0x000000000000FF00L;
    public static final long maskRow3      = 0x0000000000FF0000L;
    public static final long maskRow4      = 0x00000000FF000000L;
    public static final long maskRow5      = 0x000000FF00000000L;
    public static final long maskRow6      = 0x0000FF0000000000L;
    public static final long maskRow7      = 0x00FF000000000000L;
    public static final long maskRow8      = 0xFF00000000000000L;
    public static final long maskRow1Row8  = 0xFF000000000000FFL;

    public static final long maskDarkSq    = 0xAA55AA55AA55AA55L;
    public static final long maskLightSq   = 0x55AA55AA55AA55AAL;

    public static final long maskCorners   = 0x8100000000000081L;

    static {
        // Compute king attacks
        kingAttacks = new long[64];

        for (int sq = 0; sq < 64; sq++) {
            long m = 1L << sq;
            long mask = (((m >>> 1) | (m << 7) | (m >>> 9)) & maskAToGFiles) |
                        (((m <<  1) | (m << 9) | (m >>> 7)) & maskBToHFiles) |
                        (m << 8) | (m >>> 8);
            kingAttacks[sq] = mask;
        }

        // Compute knight attacks
        knightAttacks = new long[64];
        for (int sq = 0; sq < 64; sq++) {
            long m = 1L << sq;
            long mask = (((m <<  6) | (m >>> 10)) & maskAToFFiles) |
                        (((m << 15) | (m >>> 17)) & maskAToGFiles) |
                        (((m << 17) | (m >>> 15)) & maskBToHFiles) |
                        (((m << 10) | (m >>>  6)) & maskCToHFiles);
            knightAttacks[sq] = mask;
        }

        // Compute pawn attacks
        wPawnAttacks = new long[64];
        bPawnAttacks = new long[64];
        wPawnBlockerMask = new long[64];
        bPawnBlockerMask = new long[64];
        for (int sq = 0; sq < 64; sq++) {
            long m = 1L << sq;
            long mask = ((m << 7) & maskAToGFiles) | ((m << 9) & maskBToHFiles);
            wPawnAttacks[sq] = mask;
            mask = ((m >>> 9) & maskAToGFiles) | ((m >>> 7) & maskBToHFiles);
            bPawnAttacks[sq] = mask;
            
            int x = Position.getX(sq);
            int y = Position.getY(sq);
            m = 0;
            for (int y2 = y+1; y2 < 8; y2++) {
                if (x > 0) m |= 1L << Position.getSquare(x-1, y2);
                           m |= 1L << Position.getSquare(x  , y2);
                if (x < 7) m |= 1L << Position.getSquare(x+1, y2);
            }
            wPawnBlockerMask[sq] = m;
            m = 0;
            for (int y2 = y-1; y2 >= 0; y2--) {
                if (x > 0) m |= 1L << Position.getSquare(x-1, y2);
                           m |= 1L << Position.getSquare(x  , y2);
                if (x < 7) m |= 1L << Position.getSquare(x+1, y2);
            }
            bPawnBlockerMask[sq] = m;
        }
    }

    private final static long[][] rTables;
    private final static long[] rMasks;
    private final static int[] rBits = { 12, 11, 11, 11, 11, 11, 11, 12,
                                         11, 10, 10, 10, 10, 10, 10, 11,
                                         11, 10, 10, 10, 10, 10, 10, 11,
                                         11, 10, 10, 10, 10, 10, 10, 11,
                                         11, 10, 10, 10, 10, 10, 10, 11,
                                         11, 10, 10, 10, 10, 10, 10, 11,
                                         10,  9,  9,  9,  9,  9, 10, 10,
                                         11, 10, 10, 10, 10, 11, 11, 11 };
    private final static long[] rMagics = {
        0x0080011084624000L, 0x1440031000200141L, 0x2080082004801000L, 0x0100040900100020L,
        0x0200020010200408L, 0x0300010008040002L, 0x040024081000a102L, 0x0080003100054680L,
        0x1100800040008024L, 0x8440401000200040L, 0x0432001022008044L, 0x0402002200100840L,
        0x4024808008000400L, 0x100a000410820008L, 0x8042001144020028L, 0x2451000041002082L,
        0x1080004000200056L, 0xd41010c020004000L, 0x0004410020001104L, 0x0000818050000800L,
        0x0000050008010010L, 0x0230808002000400L, 0x2000440090022108L, 0x0488020000811044L,
        0x8000410100208006L, 0x2000a00240100140L, 0x2088802200401600L, 0x0a10100180080082L,
        0x0000080100110004L, 0x0021002300080400L, 0x8400880400010230L, 0x2001008200004401L,
        0x0000400022800480L, 0x00200040e2401000L, 0x4004100084802000L, 0x0218800800801002L,
        0x0420800800800400L, 0x002a000402001008L, 0x0e0b000401008200L, 0x0815908072000401L,
        0x1840008002498021L, 0x1070122002424000L, 0x1040200100410010L, 0x0600080010008080L,
        0x0215001008010004L, 0x0000020004008080L, 0x1300021051040018L, 0x0004040040820001L,
        0x48fffe99fecfaa00L, 0x48fffe99fecfaa00L, 0x497fffadff9c2e00L, 0x613fffddffce9200L,
        0xffffffe9ffe7ce00L, 0xfffffff5fff3e600L, 0x2000080281100400L, 0x510ffff5f63c96a0L,
        0xebffffb9ff9fc526L, 0x61fffeddfeedaeaeL, 0x53bfffedffdeb1a2L, 0x127fffb9ffdfb5f6L,
        0x411fffddffdbf4d6L, 0x0005000208040001L, 0x264038060100d004L, 0x7645fffecbfea79eL,
    };
    private final static long[][] bTables;
    private final static long[] bMasks;
    private final static int[] bBits = { 5, 4, 5, 5, 5, 5, 4, 5,
                                         4, 4, 5, 5, 5, 5, 4, 4,
                                         4, 4, 7, 7, 7, 7, 4, 4,
                                         5, 5, 7, 9, 9, 7, 5, 5,
                                         5, 5, 7, 9, 9, 7, 5, 5,
                                         4, 4, 7, 7, 7, 7, 4, 4,
                                         4, 4, 5, 5, 5, 5, 4, 4,
                                         5, 4, 5, 5, 5, 5, 4, 5 };
    private final static long[] bMagics = {
        0xffedf9fd7cfcffffL, 0xfc0962854a77f576L, 0x9010210041047000L, 0x52242420800c0000L,
        0x884404220480004aL, 0x0002080248000802L, 0xfc0a66c64a7ef576L, 0x7ffdfdfcbd79ffffL,
        0xfc0846a64a34fff6L, 0xfc087a874a3cf7f6L, 0x02000888010a2211L, 0x0040044040801808L,
        0x0880040420000000L, 0x0000084110109000L, 0xfc0864ae59b4ff76L, 0x3c0860af4b35ff76L,
        0x73c01af56cf4cffbL, 0x41a01cfad64aaffcL, 0x1010000200841104L, 0x802802142a006000L,
        0x0a02000412020020L, 0x0000800040504030L, 0x7c0c028f5b34ff76L, 0xfc0a028e5ab4df76L,
        0x0020082044905488L, 0xa572211102080220L, 0x0014020001280300L, 0x0220208058008042L,
        0x0001010000104016L, 0x0005114028080800L, 0x0202640000848800L, 0x040040900a008421L,
        0x400e094000600208L, 0x800a100400120890L, 0x0041229001480020L, 0x0000020080880082L,
        0x0040002020060080L, 0x1819100100c02400L, 0x04112a4082c40400L, 0x0001240130210500L,
        0xdcefd9b54bfcc09fL, 0xf95ffa765afd602bL, 0x008200222800a410L, 0x0100020102406400L,
        0x80a8040094000200L, 0x002002006200a041L, 0x43ff9a5cf4ca0c01L, 0x4bffcd8e7c587601L,
        0xfc0ff2865334f576L, 0xfc0bf6ce5924f576L, 0x0900420442088104L, 0x0062042084040010L,
        0x01380810220a0240L, 0x0000101002082800L, 0xc3ffb7dc36ca8c89L, 0xc3ff8a54f4ca2c89L,
        0xfffffcfcfd79edffL, 0xfc0863fccb147576L, 0x0050009040441000L, 0x00139a0000840400L,
        0x9080000412220a00L, 0x0000002020010a42L, 0xfc087e8e4bb2f736L, 0x43ff9e4ef4ca2c89L,
    };

    private static final long createPattern(int i, long mask) {
        long ret = 0L;
        for (int j = 0; ; j++) {
            long nextMask = mask & (mask - 1);
            long bit = mask ^ nextMask;
            if ((i & (1L << j)) != 0)
                ret |= bit;
            mask = nextMask;
            if (mask == 0)
                break;
        }
        return ret;
    }
    
    private static final long addRookRays(int x, int y, long occupied, boolean inner) {
        long mask = 0;
        mask = addRay(mask, x, y,  1,  0, occupied, inner);
        mask = addRay(mask, x, y, -1,  0, occupied, inner);
        mask = addRay(mask, x, y,  0,  1, occupied, inner);
        mask = addRay(mask, x, y,  0, -1, occupied, inner);
        return mask;
    }
    private static final long addBishopRays(int x, int y, long occupied, boolean inner) {
        long mask = 0;
        mask = addRay(mask, x, y,  1,  1, occupied, inner);
        mask = addRay(mask, x, y, -1, -1, occupied, inner);
        mask = addRay(mask, x, y,  1, -1, occupied, inner);
        mask = addRay(mask, x, y, -1,  1, occupied, inner);
        return mask;
    }

    private static final long addRay(long mask, int x, int y, int dx, int dy, 
                                     long occupied, boolean inner) {
        int lo = inner ? 1 : 0;
        int hi = inner ? 6 : 7;
        while (true) {
            if (dx != 0) {
                x += dx; if ((x < lo) || (x > hi)) break;
            }
            if (dy != 0) {
                y += dy; if ((y < lo) || (y > hi)) break;
            }
            int sq = Position.getSquare(x, y);
            mask |= 1L << sq;
            if ((occupied & (1L << sq)) != 0)
                break;
        }
        return mask;
    }

    static { // Rook magics
        rTables = new long[64][];
        rMasks = new long[64];
        for (int sq = 0; sq < 64; sq++) {
            int x = Position.getX(sq);
            int y = Position.getY(sq);
            rMasks[sq] = addRookRays(x, y, 0L, true);
            int tableSize = 1 << rBits[sq];
            long[] table = new long[tableSize];
            for (int i = 0; i < tableSize; i++) table[i] = -1;
            int nPatterns = 1 << Long.bitCount(rMasks[sq]);
            for (int i = 0; i < nPatterns; i++) {
                long p = createPattern(i, rMasks[sq]);
                int entry = (int)((p * rMagics[sq]) >>> (64 - rBits[sq]));
                long atks = addRookRays(x, y, p, false);
                if (table[entry] == -1) {
                    table[entry] = atks;
                } else if (table[entry] != atks) {
                    throw new RuntimeException();
                }
            }
            rTables[sq] = table;
        }
    }

    static { // Bishop magics
        bTables = new long[64][];
        bMasks = new long[64];
        for (int sq = 0; sq < 64; sq++) {
            int x = Position.getX(sq);
            int y = Position.getY(sq);
            bMasks[sq] = addBishopRays(x, y, 0L, true);
            int tableSize = 1 << bBits[sq];
            long[] table = new long[tableSize];
            for (int i = 0; i < tableSize; i++) table[i] = -1;
            int nPatterns = 1 << Long.bitCount(bMasks[sq]);
            for (int i = 0; i < nPatterns; i++) {
                long p = createPattern(i, bMasks[sq]);
                int entry = (int)((p * bMagics[sq]) >>> (64 - bBits[sq]));
                long atks = addBishopRays(x, y, p, false);
                if (table[entry] == -1) {
                    table[entry] = atks;
                } else if (table[entry] != atks) {
                    throw new RuntimeException();
                }
            }
            bTables[sq] = table;
        }
    }

    public static final long bishopAttacks(int sq, long occupied) {
        return bTables[sq][(int)(((occupied & bMasks[sq]) * bMagics[sq]) >>> (64 - bBits[sq]))];
    }

    public static final long rookAttacks(int sq, long occupied) {
        return rTables[sq][(int)(((occupied & rMasks[sq]) * rMagics[sq]) >>> (64 - rBits[sq]))];
    }
    
    static public final long[][] squaresBetween;
    static {
        squaresBetween = new long[64][];
        for (int sq1 = 0; sq1 < 64; sq1++) {
            squaresBetween[sq1] = new long[64];
            for (int j = 0; j < 64; j++)
                squaresBetween[sq1][j] = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if ((dx == 0) && (dy == 0))
                        continue;
                    long m = 0;
                    int x = Position.getX(sq1);
                    int y = Position.getY(sq1);
                    while (true) {
                        x += dx; y += dy;
                        if ((x < 0) || (x > 7) || (y < 0) || (y > 7))
                            break;
                        int sq2 = Position.getSquare(x, y);
                        squaresBetween[sq1][sq2] = m;
                        m |= 1L << sq2;
                    }
                }
            }
        }
    }

    private static final byte dirTable[] = {
            -9,   0,   0,   0,   0,   0,   0,  -8,   0,   0,   0,   0,   0,   0,  -7,
        0,   0,  -9,   0,   0,   0,   0,   0,  -8,   0,   0,   0,   0,   0,  -7,   0,
        0,   0,   0,  -9,   0,   0,   0,   0,  -8,   0,   0,   0,   0,  -7,   0,   0,
        0,   0,   0,   0,  -9,   0,   0,   0,  -8,   0,   0,   0,  -7,   0,   0,   0,
        0,   0,   0,   0,   0,  -9,   0,   0,  -8,   0,   0,  -7,   0,   0,   0,   0,
        0,   0,   0,   0,   0,   0,  -9, -17,  -8, -15,  -7,   0,   0,   0,   0,   0,
        0,   0,   0,   0,   0,   0, -10,  -9,  -8,  -7,  -6,   0,   0,   0,   0,   0,
        0,  -1,  -1,  -1,  -1,  -1,  -1,  -1,   0,   1,   1,   1,   1,   1,   1,   1,
        0,   0,   0,   0,   0,   0,   6,   7,   8,   9,  10,   0,   0,   0,   0,   0,
        0,   0,   0,   0,   0,   0,   7,  15,   8,  17,   9,   0,   0,   0,   0,   0,
        0,   0,   0,   0,   0,   7,   0,   0,   8,   0,   0,   9,   0,   0,   0,   0,
        0,   0,   0,   0,   7,   0,   0,   0,   8,   0,   0,   0,   9,   0,   0,   0,
        0,   0,   0,   7,   0,   0,   0,   0,   8,   0,   0,   0,   0,   9,   0,   0,
        0,   0,   7,   0,   0,   0,   0,   0,   8,   0,   0,   0,   0,   0,   9,   0,
        0,   7,   0,   0,   0,   0,   0,   0,   8,   0,   0,   0,   0,   0,   0,   9
    };

    static public final int getDirection(int from, int to) {
        int offs = to + (to|7) - from - (from|7) + 0x77;
        return dirTable[offs];
    }

    public static final long southFill(long mask) {
        mask |= (mask >>> 8);
        mask |= (mask >>> 16);
        mask |= (mask >>> 32);
        return mask;
    }
    
    public static final long northFill(long mask) {
        mask |= (mask << 8);
        mask |= (mask << 16);
        mask |= (mask << 32);
        return mask;
    }

    private static final int trailingZ[] = {
        63,  0, 58,  1, 59, 47, 53,  2,
        60, 39, 48, 27, 54, 33, 42,  3,
        61, 51, 37, 40, 49, 18, 28, 20,
        55, 30, 34, 11, 43, 14, 22,  4,
        62, 57, 46, 52, 38, 26, 32, 41,
        50, 36, 17, 19, 29, 10, 13, 21,
        56, 45, 25, 31, 35, 16,  9, 12,
        44, 24, 15,  8, 23,  7,  6,  5
    };

    static public final int numberOfTrailingZeros(long mask) {
        return trailingZ[(int)(((mask & -mask) * 0x07EDD5E59A4E28C2L) >>> 58)];
    }
}
