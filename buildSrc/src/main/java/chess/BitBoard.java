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

    public static final long maskAToGFiles = 0x7F7F7F7F7F7F7F7FL;
    public static final long maskBToHFiles = 0xFEFEFEFEFEFEFEFEL;
    public static final long maskAToFFiles = 0x3F3F3F3F3F3F3F3FL;
    public static final long maskCToHFiles = 0xFCFCFCFCFCFCFCFCL;

    public static final long maskCorners   = 0x8100000000000081L;

    private final static long[][] rTables;
    private final static long[] rMasks;
    private final static int[] rBits = { 12, 11, 11, 11, 11, 11, 11, 12,
                                         11, 10, 10, 11, 10, 10, 10, 11,
                                         11, 10, 10, 10, 10, 10, 10, 11,
                                         11, 10, 10, 10, 10, 10, 10, 11,
                                         11, 10, 10, 10, 10, 10, 10, 11,
                                         11, 10, 10, 11, 10, 10, 10, 11,
                                         10,  9,  9,  9,  9,  9, 10, 10,
                                         11, 10, 10, 10, 10, 11, 10, 11 };
    private final static long[] rMagics = {
        0x19a80065ff2bffffL, 0x3fd80075ffebffffL, 0x4010000df6f6fffeL, 0x0050001faffaffffL,
        0x0050028004ffffb0L, 0x7f600280089ffff1L, 0x7f5000b0029ffffcL, 0x5b58004848a7fffaL,
        0x002a90005547ffffL, 0x000050007f13ffffL, 0x007fa0006013ffffL, 0x006a9005656fffffL,
        0x007f600f600affffL, 0x007ec007e6bfffe2L, 0x007ec003eebffffbL, 0x0071d002382fffdaL,
        0x009f803000e7fffaL, 0x00680030008bffffL, 0x00606060004f3ffcL, 0x001a00600bff9ffdL,
        0x000d006005ff9fffL, 0x0001806003005fffL, 0x00000300040bfffaL, 0x000192500065ffeaL,
        0x00fff112d0006800L, 0x007ff037d000c004L, 0x003fd062001a3ff8L, 0x00087000600e1ffcL,
        0x000fff0100100804L, 0x0007ff0100080402L, 0x0003ffe0c0060003L, 0x0001ffd53000d300L,
        0x00fffd3000600061L, 0x007fff7f95900040L, 0x003fff8c00600060L, 0x001ffe2587a01860L,
        0x000fff3fbf40180cL, 0x0007ffc73f400c06L, 0x0003ff86d2c01405L, 0x0001fffeaa700100L,
        0x00fffdfdd8005000L, 0x007fff80ebffb000L, 0x003fffdf603f6000L, 0x001fffe050405000L,
        0x000fff400700c00cL, 0x0007ff6007bf600aL, 0x0003ffeebffec005L, 0x0001fffdf3feb001L,
        0x00ffff39ff484a00L, 0x007fff3fff486300L, 0x003fff99ffac2e00L, 0x001fff31ff2a6a00L,
        0x000fff19ff15b600L, 0x0007fff5fff28600L, 0x0003fffddffbfee0L, 0x0001fff5f63c96a0L,
        0x00ffff5dff65cfb6L, 0x007fffbaffd1c5aeL, 0x003fff71ff6cbceaL, 0x001fffd9ffd4756eL,
        0x000ffff5fff338e6L, 0x0007fffdfffe24f6L, 0x0003ffef27eebe74L, 0x0001ffff23ff605eL
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
        0x0006eff5367ff600L, 0x00345835ba77ff2bL, 0x00145f68a3f5dab6L, 0x003a1863fb56f21dL,
        0x0012eb6bfe9d93cdL, 0x000d82827f3420d6L, 0x00074bcd9c7fec97L, 0x000034fe99f9ffffL,
        0x0000746f8d6717f6L, 0x00003acb32e1a3f7L, 0x0000185daf1ffb8aL, 0x00003a1867f17067L,
        0x0000038ee0ccf92eL, 0x000002a2b7ff926eL, 0x000006c9aa93ff14L, 0x00000399b5e5bf87L,
        0x00400f342c951ffcL, 0x0020230579ed8ff0L, 0x007b008a0077dbfdL, 0x001d00010c13fd46L,
        0x00040022031c1ffbL, 0x000fa00fd1cbff79L, 0x000400a4bc9affdfL, 0x000200085e9cffdaL,
        0x002a14560a3dbfbdL, 0x000a0a157b9eafd1L, 0x00060600fd002ffaL, 0x004006000c009010L,
        0x001a002042008040L, 0x001a00600fd1ffc0L, 0x000d0ace50bf3f8dL, 0x000183a48434efd1L,
        0x001fbd7670982a0dL, 0x000fe24301d81a0fL, 0x0007fbf82f040041L, 0x000040c800008200L,
        0x007fe17018086006L, 0x003b7ddf0ffe1effL, 0x001f92f861df4a0aL, 0x000fd713ad98a289L,
        0x000fd6aa751e400cL, 0x0007f2a63ae9600cL, 0x0003ff7dfe0e3f00L, 0x000003fd2704ce04L,
        0x00007fc421601d40L, 0x007fff5f70900120L, 0x003fa66283556403L, 0x001fe31969aec201L,
        0x0007fdfc18ac14bbL, 0x0003fb96fb568a47L, 0x000003f72ea4954dL, 0x00000003f8dc0383L,
        0x0000007f3a814490L, 0x00007dc5c9cf62a6L, 0x007f23d3342897acL, 0x003fee36eee1565cL,
        0x0003ff3e99fcccc7L, 0x000003ecfcfac5feL, 0x00000003f97f7453L, 0x0000000003f8dc03L,
        0x000000007efa8146L, 0x0000007ed3e2ef60L, 0x00007f47243adcd6L, 0x007fb65afabfb3b5L
    };

    private static long createPattern(int i, long mask) {
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
    
    private static long addRookRays(int x, int y, long occupied, boolean inner) {
        long mask = 0;
        mask = addRay(mask, x, y,  1,  0, occupied, inner);
        mask = addRay(mask, x, y, -1,  0, occupied, inner);
        mask = addRay(mask, x, y,  0,  1, occupied, inner);
        mask = addRay(mask, x, y,  0, -1, occupied, inner);
        return mask;
    }
    private static long addBishopRays(int x, int y, long occupied, boolean inner) {
        long mask = 0;
        mask = addRay(mask, x, y,  1,  1, occupied, inner);
        mask = addRay(mask, x, y, -1, -1, occupied, inner);
        mask = addRay(mask, x, y,  1, -1, occupied, inner);
        mask = addRay(mask, x, y, -1,  1, occupied, inner);
        return mask;
    }

    private static long addRay(long mask, int x, int y, int dx, int dy,
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

    public static long bishopAttacks(int sq, long occupied) {
        return bTables[sq][(int)(((occupied & bMasks[sq]) * bMagics[sq]) >>> (64 - bBits[sq]))];
    }

    public static long rookAttacks(int sq, long occupied) {
        return rTables[sq][(int)(((occupied & rMasks[sq]) * rMagics[sq]) >>> (64 - rBits[sq]))];
    }
}
