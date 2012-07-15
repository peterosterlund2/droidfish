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

import java.io.IOException;
import java.io.InputStream;

/**
 * Position evaluation routines.
 * 
 * @author petero
 */
public class Evaluate {
    static final int pV =   92 + Parameters.instance().getIntPar("pV");
    static final int nV =  385 + Parameters.instance().getIntPar("nV");
    static final int bV =  385 + Parameters.instance().getIntPar("bV");
    static final int rV =  593 + Parameters.instance().getIntPar("rV");
    static final int qV = 1244 + Parameters.instance().getIntPar("qV");
    static final int kV = 9900; // Used by SEE algorithm, but not included in board material sums

    static final int[] pieceValue;
    static {
        // Initialize material table
        pieceValue = new int[Piece.nPieceTypes];
        pieceValue[Piece.WKING  ] = kV;
        pieceValue[Piece.WQUEEN ] = qV;
        pieceValue[Piece.WROOK  ] = rV;
        pieceValue[Piece.WBISHOP] = bV;
        pieceValue[Piece.WKNIGHT] = nV;
        pieceValue[Piece.WPAWN  ] = pV;
        pieceValue[Piece.BKING  ] = kV;
        pieceValue[Piece.BQUEEN ] = qV;
        pieceValue[Piece.BROOK  ] = rV;
        pieceValue[Piece.BBISHOP] = bV;
        pieceValue[Piece.BKNIGHT] = nV;
        pieceValue[Piece.BPAWN  ] = pV;
        pieceValue[Piece.EMPTY  ] =  0;
    }

    /** Piece/square table for king during middle game. */
    static final int[] kt1b = { -22,-35,-40,-40,-40,-40,-35,-22,
                                -22,-35,-40,-40,-40,-40,-35,-22,
                                -25,-35,-40,-45,-45,-40,-35,-25,
                                -15,-30,-35,-40,-40,-35,-30,-15,
                                -10,-15,-20,-25,-25,-20,-15,-10,
                                  4, -2, -5,-15,-15, -5, -2,  4,
                                 16, 14,  7, -3, -3,  7, 14, 16,
                                 24, 24,  9,  0,  0,  9, 24, 24 };

    /** Piece/square table for king during end game. */
    static final int[] kt2b = {  0,  8, 16, 24, 24, 16,  8,  0,
                                 8, 16, 24, 32, 32, 24, 16,  8,
                                16, 24, 32, 40, 40, 32, 24, 16,
                                24, 32, 40, 48, 48, 40, 32, 24,
                                24, 32, 40, 48, 48, 40, 32, 24,
                                16, 24, 32, 40, 40, 32, 24, 16,
                                 8, 16, 24, 32, 32, 24, 16,  8,
                                 0,  8, 16, 24, 24, 16,  8,  0 };

    /** Piece/square table for pawns during middle game. */
    static final int[] pt1b = {  0,  0,  0,  0,  0,  0,  0,  0,
                                 8, 16, 24, 32, 32, 24, 16,  8,
                                 3, 12, 20, 28, 28, 20, 12,  3,
                                -5,  4, 10, 20, 20, 10,  4, -5,
                                -6,  4,  5, 16, 16,  5,  4, -6,
                                -6,  4,  2,  5,  5,  2,  4, -6,
                                -6,  4,  4,-15,-15,  4,  4, -6,
                                 0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for pawns during end game. */
    static final int[] pt2b = {   0,  0,  0,  0,  0,  0,  0,  0,
                                 25, 40, 45, 45, 45, 45, 40, 25,
                                 17, 32, 35, 35, 35, 35, 32, 17,
                                  5, 24, 24, 24, 24, 24, 24,  5,
                                 -9, 11, 11, 11, 11, 11, 11, -9,
                                -17,  3,  3,  3,  3,  3,  3,-17,
                                -20,  0,  0,  0,  0,  0,  0,-20,
                                  0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for knights during middle game. */
    static final int[] nt1b = { -53,-42,-32,-21,-21,-32,-42,-53,
                                -42,-32,-10,  0,  0,-10,-32,-42,
                                -21,  5, 10, 16, 16, 10,  5,-21,
                                -18,  0, 10, 21, 21, 10,  0,-18,
                                -18,  0,  3, 21, 21,  3,  0,-18,
                                -21,-10,  0,  0,  0,  0,-10,-21,
                                -42,-32,-10,  0,  0,-10,-32,-42,
                                -53,-42,-32,-21,-21,-32,-42,-53 };

    /** Piece/square table for knights during end game. */
    static final int[] nt2b = { -56,-44,-34,-22,-22,-34,-44,-56,
                                -44,-34,-10,  0,  0,-10,-34,-44,
                                -22,  5, 10, 17, 17, 10,  5,-22,
                                -19,  0, 10, 22, 22, 10,  0,-19,
                                -19,  0,  3, 22, 22,  3,  0,-19,
                                -22,-10,  0,  0,  0,  0,-10,-22,
                                -44,-34,-10,  0,  0,-10,-34,-44,
                                -56,-44,-34,-22,-22,-34,-44,-56 };

    /** Piece/square table for bishops during middle game. */
    static final int[] bt1b = {  0,  0,  0,  0,  0,  0,  0,  0,
                                 0,  4,  2,  2,  2,  2,  4,  0,
                                 0,  2,  4,  4,  4,  4,  2,  0,
                                 0,  2,  4,  4,  4,  4,  2,  0,
                                 0,  2,  4,  4,  4,  4,  2,  0,
                                 0,  3,  4,  4,  4,  4,  3,  0,
                                 0,  4,  2,  2,  2,  2,  4,  0,
                                -5, -5, -7, -5, -5, -7, -5, -5 };

    /** Piece/square table for bishops during middle game. */
    static final int[] bt2b = {  0,  0,  0,  0,  0,  0,  0,  0,
                                 0,  2,  2,  2,  2,  2,  2,  0,
                                 0,  2,  4,  4,  4,  4,  2,  0,
                                 0,  2,  4,  4,  4,  4,  2,  0,
                                 0,  2,  4,  4,  4,  4,  2,  0,
                                 0,  2,  4,  4,  4,  4,  2,  0,
                                 0,  2,  2,  2,  2,  2,  2,  0,
                                 0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for queens during middle game. */
    static final int[] qt1b = { -10, -5,  0,  0,  0,  0, -5,-10,
                                 -5,  0,  5,  5,  5,  5,  0, -5,
                                  0,  5,  5,  6,  6,  5,  5,  0,
                                  0,  5,  6,  6,  6,  6,  5,  0,
                                  0,  5,  6,  6,  6,  6,  5,  0,
                                  0,  5,  5,  6,  6,  5,  5,  0,
                                 -5,  0,  5,  5,  5,  5,  0, -5,
                                -10, -5,  0,  0,  0,  0, -5,-10 };

    /** Piece/square table for rooks during middle game. */
    static final int[] rt1b = {  8, 11, 13, 13, 13, 13, 11,  8,
                                22, 27, 27, 27, 27, 27, 27, 22,
                                 0,  0,  0,  0,  0,  0,  0,  0,
                                 0,  0,  0,  0,  0,  0,  0,  0,
                                -2,  0,  0,  0,  0,  0,  0, -2,
                                -2,  0,  0,  2,  2,  0,  0, -2,
                                -3,  2,  5,  5,  5,  5,  2, -3,
                                 0,  3,  5,  5,  5,  5,  3,  0 };

    static final int[] kt1w, qt1w, rt1w, bt1w, nt1w, pt1w, kt2w, bt2w, nt2w, pt2w;
    static {
        kt1w = new int[64];
        qt1w = new int[64];
        rt1w = new int[64];
        bt1w = new int[64];
        nt1w = new int[64];
        pt1w = new int[64];
        kt2w = new int[64];
        bt2w = new int[64];
        nt2w = new int[64];
        pt2w = new int[64];
        for (int i = 0; i < 64; i++) {
            kt1w[i] = kt1b[63-i];
            qt1w[i] = qt1b[63-i];
            rt1w[i] = rt1b[63-i];
            bt1w[i] = bt1b[63-i];
            nt1w[i] = nt1b[63-i];
            pt1w[i] = pt1b[63-i];
            kt2w[i] = kt2b[63-i];
            bt2w[i] = bt2b[63-i];
            nt2w[i] = nt2b[63-i];
            pt2w[i] = pt2b[63-i];
        }
    }

    private static final int[] empty = { 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
                                         0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
                                         0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
                                         0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0};
    static final int[][] psTab1 = { empty, kt1w, qt1w, rt1w, bt1w, nt1w, pt1w,
                                           kt1b, qt1b, rt1b, bt1b, nt1b, pt1b };
    static final int[][] psTab2 = { empty, kt2w, qt1w, rt1w, bt2w, nt2w, pt2w,
                                           kt2b, qt1b, rt1b, bt2b, nt2b, pt2b };

    static final int[][] distToH1A8 = { { 0, 1, 2, 3, 4, 5, 6, 7 },
                                        { 1, 2, 3, 4, 5, 6, 7, 6 },
                                        { 2, 3, 4, 5, 6, 7, 6, 5 },
                                        { 3, 4, 5, 6, 7, 6, 5, 4 },
                                        { 4, 5, 6, 7, 6, 5, 4, 3 },
                                        { 5, 6, 7, 6, 5, 4, 3, 2 },
                                        { 6, 7, 6, 5, 4, 3, 2, 1 },
                                        { 7, 6, 5, 4, 3, 2, 1, 0 } };

    static final int[] rookMobScore = {-10,-7,-4,-1,2,5,7,9,11,12,13,14,14,14,14};
    static final int[] bishMobScore = {-15,-10,-6,-2,2,6,10,13,16,18,20,22,23,24};
    static final int[] queenMobScore = {-5,-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,9,9,10,10,10,10,10,10,10,10,10,10,10,10};

    private static final class PawnHashData {
        long key;
        int score;         // Positive score means good for white
        short passedBonusW;
        short passedBonusB;
        long passedPawnsW;     // The most advanced passed pawns for each file
        long passedPawnsB;
    }
    static final PawnHashData[] pawnHash;
    static {
        final int numEntries = 1<<16;
        pawnHash = new PawnHashData[numEntries];
        for (int i = 0; i < numEntries; i++) {
            PawnHashData phd = new PawnHashData();
            phd.key = -1; // Non-zero to avoid collision for positions with no pawns
            phd.score = 0;
            pawnHash[i] = phd;
        }
    }

    static byte[] kpkTable = null;
    static byte[] krkpTable = null;

    // King safety variables
    private long wKingZone, bKingZone;       // Squares close to king that are worth attacking
    private int wKingAttacks, bKingAttacks; // Number of attacks close to white/black king
    private long wAttacksBB, bAttacksBB;
    private long wPawnAttacks, bPawnAttacks; // Squares attacked by white/black pawns

    /** Constructor. */
    public Evaluate() {
        if (kpkTable == null)
            kpkTable = readTable("/kpk.bitbase", 2*32*64*48/8);
        if (krkpTable == null)
            krkpTable = readTable("/krkp.winmasks", 2*32*48*8);
    }

    private byte[] readTable(String resource, int length) {
        byte[] table = new byte[2*32*64*48/8];
        InputStream inStream = getClass().getResourceAsStream(resource);
        try {
            int off = 0;
            while (off < table.length) {
                int len = inStream.read(table, off, table.length - off);
                if (len < 0)
                    throw new RuntimeException();
                off += len;
            }
            inStream.close();
            return table;
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Static evaluation of a position.
     * @param pos The position to evaluate.
     * @return The evaluation score, measured in centipawns.
     *         Positive values are good for the side to make the next move.
     */
    final public int evalPos(Position pos) {
        int score = pos.wMtrl - pos.bMtrl;

        wKingAttacks = bKingAttacks = 0;
        wKingZone = BitBoard.kingAttacks[pos.getKingSq(true)]; wKingZone |= wKingZone << 8;
        bKingZone = BitBoard.kingAttacks[pos.getKingSq(false)]; bKingZone |= bKingZone >>> 8;
        wAttacksBB = bAttacksBB = 0L;

        long pawns = pos.pieceTypeBB[Piece.WPAWN];
        wPawnAttacks = ((pawns & BitBoard.maskBToHFiles) << 7) |
                       ((pawns & BitBoard.maskAToGFiles) << 9);
        pawns = pos.pieceTypeBB[Piece.BPAWN];
        bPawnAttacks = ((pawns & BitBoard.maskBToHFiles) >>> 9) |
                       ((pawns & BitBoard.maskAToGFiles) >>> 7);

        score += pieceSquareEval(pos);
        score += pawnBonus(pos);
        score += tradeBonus(pos);
        score += castleBonus(pos);

        score += rookBonus(pos);
        score += bishopEval(pos, score);
        score += threatBonus(pos);
        score += kingSafety(pos);
        score = endGameEval(pos, score);

        if (!pos.whiteMove)
            score = -score;
        return score;

        // FIXME! Test penalty if side to move has >1 hanging piece
        
        // FIXME! Test "tempo value"
    }

    /** Compute white_material - black_material. */
    static final int material(Position pos) {
        return pos.wMtrl - pos.bMtrl;
    }
    
    /** Compute score based on piece square tables. Positive values are good for white. */
    private final int pieceSquareEval(Position pos) {
        int score = 0;
        final int wMtrl = pos.wMtrl;
        final int bMtrl = pos.bMtrl;
        final int wMtrlPawns = pos.wMtrlPawns;
        final int bMtrlPawns = pos.bMtrlPawns;
        
        // Kings
        {
            final int t1 = qV + 2 * rV + 2 * bV;
            final int t2 = rV;
            {
                final int k1 = pos.psScore1[Piece.WKING];
                final int k2 = pos.psScore2[Piece.WKING];
                final int t = bMtrl - bMtrlPawns;
                score += interpolate(t, t2, k2, t1, k1);
            }
            {
                final int k1 = pos.psScore1[Piece.BKING];
                final int k2 = pos.psScore2[Piece.BKING];
                final int t = wMtrl - wMtrlPawns;
                score -= interpolate(t, t2, k2, t1, k1);
            }
        }

        // Pawns
        {
            final int t1 = qV + 2 * rV + 2 * bV;
            final int t2 = rV;
            int wp1 = pos.psScore1[Piece.WPAWN];
            int wp2 = pos.psScore2[Piece.WPAWN];
            if ((wp1 != 0) || (wp2 != 0)) {
                final int tw = bMtrl - bMtrlPawns;
                score += interpolate(tw, t2, wp2, t1, wp1);
            }
            int bp1 = pos.psScore1[Piece.BPAWN];
            int bp2 = pos.psScore2[Piece.BPAWN];
            if ((bp1 != 0) || (bp2 != 0)) {
                final int tb = wMtrl - wMtrlPawns;
                score -= interpolate(tb, t2, bp2, t1, bp1);
            }
        }

        // Knights
        {
            final int t1 = qV + 2 * rV + 1 * bV + 1 * nV + 6 * pV;
            final int t2 = nV + 8 * pV;
            int n1 = pos.psScore1[Piece.WKNIGHT];
            int n2 = pos.psScore2[Piece.WKNIGHT];
            if ((n1 != 0) || (n2 != 0)) {
                score += interpolate(bMtrl, t2, n2, t1, n1);
            }
            n1 = pos.psScore1[Piece.BKNIGHT];
            n2 = pos.psScore2[Piece.BKNIGHT];
            if ((n1 != 0) || (n2 != 0)) {
                score -= interpolate(wMtrl, t2, n2, t1, n1);
            }
        }

        // Bishops
        {
            score += pos.psScore1[Piece.WBISHOP];
            score -= pos.psScore1[Piece.BBISHOP];
        }

        // Queens
        {
            final long occupied = pos.whiteBB | pos.blackBB;
            score += pos.psScore1[Piece.WQUEEN];
            long m = pos.pieceTypeBB[Piece.WQUEEN];
            while (m != 0) {
                int sq = BitBoard.numberOfTrailingZeros(m);
                long atk = BitBoard.rookAttacks(sq, occupied) | BitBoard.bishopAttacks(sq, occupied);
                wAttacksBB |= atk;
                score += queenMobScore[Long.bitCount(atk & ~(pos.whiteBB | bPawnAttacks))];
                bKingAttacks += Long.bitCount(atk & bKingZone) * 2;
                m &= m-1;
            }
            score -= pos.psScore1[Piece.BQUEEN];
            m = pos.pieceTypeBB[Piece.BQUEEN];
            while (m != 0) {
                int sq = BitBoard.numberOfTrailingZeros(m);
                long atk = BitBoard.rookAttacks(sq, occupied) | BitBoard.bishopAttacks(sq, occupied);
                bAttacksBB |= atk;
                score -= queenMobScore[Long.bitCount(atk & ~(pos.blackBB | wPawnAttacks))];
                wKingAttacks += Long.bitCount(atk & wKingZone) * 2;
                m &= m-1;
            }
        }

        // Rooks
        {
            int r1 = pos.psScore1[Piece.WROOK];
            if (r1 != 0) {
                final int nP = bMtrlPawns / pV;
                final int s = r1 * Math.min(nP, 6) / 6;
                score += s;
            }
            r1 = pos.psScore1[Piece.BROOK];
            if (r1 != 0) {
                final int nP = wMtrlPawns / pV;
                final int s = r1 * Math.min(nP, 6) / 6;
                score -= s;
            }
        }

        return score;
    }

    /** Implement the "when ahead trade pieces, when behind trade pawns" rule. */
    private final int tradeBonus(Position pos) {
        final int wM = pos.wMtrl;
        final int bM = pos.bMtrl;
        final int wPawn = pos.wMtrlPawns;
        final int bPawn = pos.bMtrlPawns;
        final int deltaScore = wM - bM;

        int pBonus = 0;
        pBonus += interpolate((deltaScore > 0) ? wPawn : bPawn, 0, -30 * deltaScore / 100, 6 * pV, 0);
        pBonus += interpolate((deltaScore > 0) ? bM : wM, 0, 30 * deltaScore / 100, qV + 2 * rV + 2 * bV + 2 * nV, 0);

        return pBonus;
    }

    private static final int[] castleFactor;
    static {
        castleFactor = new int[256];
        for (int i = 0; i < 256; i++) {
            int h1Dist = 100;
            boolean h1Castle = (i & (1<<7)) != 0;
            if (h1Castle)
                h1Dist = 2 + Long.bitCount(i & 0x0000000000000060L); // f1,g1
            int a1Dist = 100;
            boolean a1Castle = (i & 1) != 0;
            if (a1Castle)
                a1Dist = 2 + Long.bitCount(i & 0x000000000000000EL); // b1,c1,d1
            castleFactor[i] = 1024 / Math.min(a1Dist, h1Dist);
        }
    }

    /** Score castling ability. */
    private final int castleBonus(Position pos) {
        if (pos.getCastleMask() == 0) return 0;

        final int k1 = kt1b[7*8+6] - kt1b[7*8+4];
        final int k2 = kt2b[7*8+6] - kt2b[7*8+4];
        final int t1 = qV + 2 * rV + 2 * bV;
        final int t2 = rV;
        final int t = pos.bMtrl - pos.bMtrlPawns;
        final int ks = interpolate(t, t2, k2, t1, k1);

        final int castleValue = ks + rt1b[7*8+5] - rt1b[7*8+7];
        if (castleValue <= 0)
            return 0;

        long occupied = pos.whiteBB | pos.blackBB;
        int tmp = (int) (occupied & 0x6E);
        if (pos.a1Castle()) tmp |= 1;
        if (pos.h1Castle()) tmp |= (1 << 7);
        final int wBonus = (castleValue * castleFactor[tmp]) >> 10;

        tmp = (int) ((occupied >>> 56) & 0x6E);
        if (pos.a8Castle()) tmp |= 1;
        if (pos.h8Castle()) tmp |= (1 << 7);
        final int bBonus = (castleValue * castleFactor[tmp]) >> 10;

        return wBonus - bBonus;
    }

    private final int pawnBonus(Position pos) {
        long key = pos.pawnZobristHash();
        PawnHashData phd = pawnHash[(int)key & (pawnHash.length - 1)];
        if (phd.key != key)
            computePawnHashData(pos, phd);
        int score = phd.score;

        final int hiMtrl = qV + rV;
        score += interpolate(pos.bMtrl - pos.bMtrlPawns, 0, 2 * phd.passedBonusW, hiMtrl, phd.passedBonusW);
        score -= interpolate(pos.wMtrl - pos.wMtrlPawns, 0, 2 * phd.passedBonusB, hiMtrl, phd.passedBonusB);

        // Passed pawns are more dangerous if enemy king is far away
        int bestWPawnDist = 8;
        int bestWPromSq = -1;
        long m = phd.passedPawnsW;
        if (m != 0) {
            int mtrlNoPawns = pos.bMtrl - pos.bMtrlPawns;
            if (mtrlNoPawns < hiMtrl) {
                int kingPos = pos.getKingSq(false);
                while (m != 0) {
                    int sq = BitBoard.numberOfTrailingZeros(m);
                    int x = Position.getX(sq);
                    int y = Position.getY(sq);
                    int pawnDist = Math.min(5, 7 - y);
                    int kingDist = BitBoard.getDistance(kingPos, Position.getSquare(x, 7));
                    int kScore = kingDist * 4;
                    if (kingDist > pawnDist) kScore += (kingDist - pawnDist) * (kingDist - pawnDist);
                    score += interpolate(mtrlNoPawns, 0, kScore, hiMtrl, 0);
                    if (!pos.whiteMove)
                        kingDist--;
                    if ((pawnDist < kingDist) && (mtrlNoPawns == 0)) {
                        if ((BitBoard.northFill(1L<<sq) & (1L << pos.getKingSq(true))) != 0)
                            pawnDist++; // Own king blocking pawn
                        if (pawnDist < bestWPawnDist) {
                            bestWPawnDist = pawnDist;
                            bestWPromSq = Position.getSquare(x, 7);
                        }
                    }
                    m &= m-1;
                }
            }
        }
        int bestBPawnDist = 8;
        int bestBPromSq = -1;
        m = phd.passedPawnsB;
        if (m != 0) {
            int mtrlNoPawns = pos.wMtrl - pos.wMtrlPawns;
            if (mtrlNoPawns < hiMtrl) {
                int kingPos = pos.getKingSq(true);
                while (m != 0) {
                    int sq = BitBoard.numberOfTrailingZeros(m);
                    int x = Position.getX(sq);
                    int y = Position.getY(sq);
                    int pawnDist = Math.min(5, y);
                    int kingDist = BitBoard.getDistance(kingPos, Position.getSquare(x, 0));
                    int kScore = kingDist * 4;
                    if (kingDist > pawnDist) kScore += (kingDist - pawnDist) * (kingDist - pawnDist);
                    score -= interpolate(mtrlNoPawns, 0, kScore, hiMtrl, 0);
                    if (pos.whiteMove)
                        kingDist--;
                    if ((pawnDist < kingDist) && (mtrlNoPawns == 0)) {
                        if ((BitBoard.southFill(1L<<sq) & (1L << pos.getKingSq(false))) != 0)
                            pawnDist++; // Own king blocking pawn
                        if (pawnDist < bestBPawnDist) {
                            bestBPawnDist = pawnDist;
                            bestBPromSq = Position.getSquare(x, 0);
                        }
                    }
                    m &= m-1;
                }
            }
        }

        // Evaluate pawn races in pawn end games
        if (bestWPromSq >= 0) {
            if (bestBPromSq >= 0) {
                int wPly = bestWPawnDist * 2; if (pos.whiteMove) wPly--;
                int bPly = bestBPawnDist * 2; if (!pos.whiteMove) bPly--;
                if (wPly < bPly - 1) {
                    score += 500;
                } else if (wPly == bPly - 1) {
                    if (BitBoard.getDirection(bestWPromSq, pos.getKingSq(false)) != 0)
                        score += 500;
                } else if (wPly == bPly + 1) {
                    if (BitBoard.getDirection(bestBPromSq, pos.getKingSq(true)) != 0)
                        score -= 500;
                } else {
                    score -= 500;
                }
            } else
                score += 500;
        } else if (bestBPromSq >= 0)
            score -= 500;

        return score;
    }

    /** Compute pawn hash data for pos. */
    private final void computePawnHashData(Position pos, PawnHashData ph) {
        int score = 0;

        // Evaluate double pawns and pawn islands
        long wPawns = pos.pieceTypeBB[Piece.WPAWN];
        long wPawnFiles = BitBoard.southFill(wPawns) & 0xff;
        int wDouble = Long.bitCount(wPawns) - Long.bitCount(wPawnFiles);
        int wIslands = Long.bitCount(((~wPawnFiles) >>> 1) & wPawnFiles);
        int wIsolated = Long.bitCount(~(wPawnFiles<<1) & wPawnFiles & ~(wPawnFiles>>>1));

        
        long bPawns = pos.pieceTypeBB[Piece.BPAWN];
        long bPawnFiles = BitBoard.southFill(bPawns) & 0xff;
        int bDouble = Long.bitCount(bPawns) - Long.bitCount(bPawnFiles);
        int bIslands = Long.bitCount(((~bPawnFiles) >>> 1) & bPawnFiles);
        int bIsolated = Long.bitCount(~(bPawnFiles<<1) & bPawnFiles & ~(bPawnFiles>>>1));

        score -= (wDouble - bDouble) * 25;
        score -= (wIslands - bIslands) * 15;
        score -= (wIsolated - bIsolated) * 15;

        // Evaluate backward pawns, defined as a pawn that guards a friendly pawn,
        // can't be guarded by friendly pawns, can advance, but can't advance without 
        // being captured by an enemy pawn.
        long wPawnAttacks = (((wPawns & BitBoard.maskBToHFiles) << 7) |
                             ((wPawns & BitBoard.maskAToGFiles) << 9));
        long bPawnAttacks = (((bPawns & BitBoard.maskBToHFiles) >>> 9) |
                             ((bPawns & BitBoard.maskAToGFiles) >>> 7));
        long wBackward = wPawns & ~((wPawns | bPawns) >>> 8) & (bPawnAttacks >>> 8) &
                         ~BitBoard.northFill(wPawnAttacks);
        wBackward &= (((wPawns & BitBoard.maskBToHFiles) >>> 9) |
                      ((wPawns & BitBoard.maskAToGFiles) >>> 7));
        wBackward &= ~BitBoard.northFill(bPawnFiles);
        long bBackward = bPawns & ~((wPawns | bPawns) << 8) & (wPawnAttacks << 8) &
                         ~BitBoard.southFill(bPawnAttacks);
        bBackward &= (((bPawns & BitBoard.maskBToHFiles) << 7) |
                      ((bPawns & BitBoard.maskAToGFiles) << 9));
        bBackward &= ~BitBoard.northFill(wPawnFiles);
        score -= (Long.bitCount(wBackward) - Long.bitCount(bBackward)) * 15;

        // Evaluate passed pawn bonus, white
        long passedPawnsW = wPawns & ~BitBoard.southFill(bPawns | bPawnAttacks | (wPawns >>> 8));
        final int[] ppBonus = {-1,24,26,30,36,55,100,-1};
        int passedBonusW = 0;
        if (passedPawnsW != 0) {
            long guardedPassedW = passedPawnsW & (((wPawns & BitBoard.maskBToHFiles) << 7) |
                                                  ((wPawns & BitBoard.maskAToGFiles) << 9));
            passedBonusW += 15 * Long.bitCount(guardedPassedW);
            long m = passedPawnsW;
            while (m != 0) {
                int sq = Long .numberOfTrailingZeros(m);
                int y = Position.getY(sq);
                passedBonusW += ppBonus[y];
                m &= m-1;
            }
        }

        // Evaluate passed pawn bonus, black
        long passedPawnsB = bPawns & ~BitBoard.northFill(wPawns | wPawnAttacks | (bPawns << 8));
        int passedBonusB = 0;
        if (passedPawnsB != 0) {
            long guardedPassedB = passedPawnsB & (((bPawns & BitBoard.maskBToHFiles) >>> 9) |
                                                  ((bPawns & BitBoard.maskAToGFiles) >>> 7));
            passedBonusB += 15 * Long.bitCount(guardedPassedB);
            long m = passedPawnsB;
            while (m != 0) {
                int sq = Long .numberOfTrailingZeros(m);
                int y = Position.getY(sq);
                passedBonusB += ppBonus[7-y];
                m &= m-1;
            }
        }

        // Connected passed pawn bonus. Seems logical but scored -8 elo in tests
//        if (passedPawnsW != 0) {
//            long mask = passedPawnsW;
//            mask = (((mask >> 7) | (mask << 1) | (mask << 9)) & BitBoard.maskBToHFiles) |
//                    (((mask >> 9) | (mask >> 1) | (mask << 7)) & BitBoard.maskAToGFiles);
//            passedBonusW += 13 * Long.bitCount(passedPawnsW & mask);
//        }
//        if (passedPawnsB != 0) {
//            long mask = passedPawnsB;
//            mask = (((mask >> 7) | (mask << 1) | (mask << 9)) & BitBoard.maskBToHFiles) |
//                    (((mask >> 9) | (mask >> 1) | (mask << 7)) & BitBoard.maskAToGFiles);
//            passedBonusB += 13 * Long.bitCount(passedPawnsB & mask);
//        }

        ph.key = pos.pawnZobristHash();
        ph.score = score;
        ph.passedBonusW = (short)passedBonusW;
        ph.passedBonusB = (short)passedBonusB;
        ph.passedPawnsW = passedPawnsW;
        ph.passedPawnsB = passedPawnsB;
    }

    /** Compute rook bonus. Rook on open/half-open file. */
    private final int rookBonus(Position pos) {
        int score = 0;
        final long wPawns = pos.pieceTypeBB[Piece.WPAWN];
        final long bPawns = pos.pieceTypeBB[Piece.BPAWN];
        final long occupied = pos.whiteBB | pos.blackBB;
        long m = pos.pieceTypeBB[Piece.WROOK];
        while (m != 0) {
            int sq = BitBoard.numberOfTrailingZeros(m);
            final int x = Position.getX(sq);
            if ((wPawns & BitBoard.maskFile[x]) == 0) { // At least half-open file
                score += (bPawns & BitBoard.maskFile[x]) == 0 ? 25 : 12;
            }
            long atk = BitBoard.rookAttacks(sq, occupied);
            wAttacksBB |= atk;
            score += rookMobScore[Long.bitCount(atk & ~(pos.whiteBB | bPawnAttacks))];
            if ((atk & bKingZone) != 0)
                bKingAttacks += Long.bitCount(atk & bKingZone);
            m &= m-1;
        }
        long r7 = (pos.pieceTypeBB[Piece.WROOK] >>> 48) & 0x00ffL;
        if (((r7 & (r7 - 1)) != 0) &&
            ((pos.pieceTypeBB[Piece.BKING] & 0xff00000000000000L) != 0))
            score += 30; // Two rooks on 7:th row
        m = pos.pieceTypeBB[Piece.BROOK];
        while (m != 0) {
            int sq = BitBoard.numberOfTrailingZeros(m);
            final int x = Position.getX(sq);
            if ((bPawns & BitBoard.maskFile[x]) == 0) {
                score -= (wPawns & BitBoard.maskFile[x]) == 0 ? 25 : 12;
            }
            long atk = BitBoard.rookAttacks(sq, occupied);
            bAttacksBB |= atk;
            score -= rookMobScore[Long.bitCount(atk & ~(pos.blackBB | wPawnAttacks))];
            if ((atk & wKingZone) != 0)
                wKingAttacks += Long.bitCount(atk & wKingZone);
            m &= m-1;
        }
        r7 = pos.pieceTypeBB[Piece.BROOK] & 0xff00L;
        if (((r7 & (r7 - 1)) != 0) &&
            ((pos.pieceTypeBB[Piece.WKING] & 0xffL) != 0))
          score -= 30; // Two rooks on 2:nd row
        return score;
    }

    /** Compute bishop evaluation. */
    private final int bishopEval(Position pos, int oldScore) {
        int score = 0;
        final long occupied = pos.whiteBB | pos.blackBB;
        long wBishops = pos.pieceTypeBB[Piece.WBISHOP];
        long bBishops = pos.pieceTypeBB[Piece.BBISHOP];
        if ((wBishops | bBishops) == 0)
            return 0;
        long m = wBishops;
        while (m != 0) {
            int sq = BitBoard.numberOfTrailingZeros(m);
            long atk = BitBoard.bishopAttacks(sq, occupied);
            wAttacksBB |= atk;
            score += bishMobScore[Long.bitCount(atk & ~(pos.whiteBB | bPawnAttacks))];
            if ((atk & bKingZone) != 0)
                bKingAttacks += Long.bitCount(atk & bKingZone);
            m &= m-1;
        }
        m = bBishops;
        while (m != 0) {
            int sq = BitBoard.numberOfTrailingZeros(m);
            long atk = BitBoard.bishopAttacks(sq, occupied);
            bAttacksBB |= atk;
            score -= bishMobScore[Long.bitCount(atk & ~(pos.blackBB | wPawnAttacks))];
            if ((atk & wKingZone) != 0)
                wKingAttacks += Long.bitCount(atk & wKingZone);
            m &= m-1;
        }

        boolean whiteDark  = (pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskDarkSq ) != 0;
        boolean whiteLight = (pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskLightSq) != 0;
        boolean blackDark  = (pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskDarkSq ) != 0;
        boolean blackLight = (pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskLightSq) != 0;
        int numWhite = (whiteDark ? 1 : 0) + (whiteLight ? 1 : 0);
        int numBlack = (blackDark ? 1 : 0) + (blackLight ? 1 : 0);

        // Bishop pair bonus
        if (numWhite == 2) {
            final int numPawns = pos.wMtrlPawns / pV;
            score += 28 + (8 - numPawns) * 3;
        }
        if (numBlack == 2) {
            final int numPawns = pos.bMtrlPawns / pV;
            score -= 28 + (8 - numPawns) * 3;
        }
    
        // FIXME!!! Bad bishop
    
        if ((numWhite == 1) && (numBlack == 1) && (whiteDark != blackDark) &&
            (pos.wMtrl - pos.wMtrlPawns == pos.bMtrl - pos.bMtrlPawns)) {
            final int penalty = (oldScore + score) / 2;
            final int loMtrl = 2 * bV;
            final int hiMtrl = 2 * (qV + rV + bV);
            int mtrl = pos.wMtrl + pos.bMtrl - pos.wMtrlPawns - pos.bMtrlPawns;
            score -= interpolate(mtrl, loMtrl, penalty, hiMtrl, 0);
        }

        // Penalty for bishop trapped behind pawn at a2/h2/a7/h7
        if (((wBishops | bBishops) & 0x0081000000008100L) != 0) {
            if ((pos.squares[48] == Piece.WBISHOP) && // a7
                (pos.squares[41] == Piece.BPAWN) &&
                (pos.squares[50] == Piece.BPAWN))
                score -= pV * 3 / 2;
            if ((pos.squares[55] == Piece.WBISHOP) && // h7
                (pos.squares[46] == Piece.BPAWN) &&
                (pos.squares[53] == Piece.BPAWN))
                score -= (pos.pieceTypeBB[Piece.WQUEEN] != 0) ? pV : pV * 3 / 2;
            if ((pos.squares[8] == Piece.BBISHOP) &&  // a2
                (pos.squares[17] == Piece.WPAWN) &&
                (pos.squares[10] == Piece.WPAWN))
                score += pV * 3 / 2;
            if ((pos.squares[15] == Piece.BBISHOP) && // h2
                (pos.squares[22] == Piece.WPAWN) &&
                (pos.squares[13] == Piece.WPAWN))
                score += (pos.pieceTypeBB[Piece.BQUEEN] != 0) ? pV : pV * 3 / 2;
        }

        return score;
    }

    private int threatBonus(Position pos) {
        // FIXME!! Try higher weight for attacks on more valuable pieces.
        int score = 0;

        // Sum values for all black pieces under attack
        long m = pos.pieceTypeBB[Piece.WKNIGHT];
        while (m != 0) {
            int sq = BitBoard.numberOfTrailingZeros(m);
            wAttacksBB |= BitBoard.knightAttacks[sq];
            m &= m-1;
        }
        wAttacksBB &= (pos.pieceTypeBB[Piece.BKNIGHT] |
                       pos.pieceTypeBB[Piece.BBISHOP] |
                       pos.pieceTypeBB[Piece.BROOK] |
                       pos.pieceTypeBB[Piece.BQUEEN]);
        wAttacksBB |= wPawnAttacks;
        m = wAttacksBB & pos.blackBB & ~pos.pieceTypeBB[Piece.BKING];
        int tmp = 0;
        while (m != 0) {
            int sq = BitBoard.numberOfTrailingZeros(m);
            tmp += pieceValue[pos.squares[sq]];
            m &= m-1;
        }
        score += tmp + tmp * tmp / qV;

        // Sum values for all white pieces under attack
        m = pos.pieceTypeBB[Piece.BKNIGHT];
        while (m != 0) {
            int sq = BitBoard.numberOfTrailingZeros(m);
            bAttacksBB |= BitBoard.knightAttacks[sq];
            m &= m-1;
        }
        bAttacksBB &= (pos.pieceTypeBB[Piece.WKNIGHT] |
                       pos.pieceTypeBB[Piece.WBISHOP] |
                       pos.pieceTypeBB[Piece.WROOK] |
                       pos.pieceTypeBB[Piece.WQUEEN]);
        bAttacksBB |= bPawnAttacks;
        m = bAttacksBB & pos.whiteBB & ~pos.pieceTypeBB[Piece.WKING];
        tmp = 0;
        while (m != 0) {
            int sq = BitBoard.numberOfTrailingZeros(m);
            tmp += pieceValue[pos.squares[sq]];
            m &= m-1;
        }
        score -= tmp + tmp * tmp / qV;
        return score / 64;
    }

    /** Compute king safety for both kings. */
    private final int kingSafety(Position pos) {
        final int minM = rV + bV;
        final int m = (pos.wMtrl - pos.wMtrlPawns + pos.bMtrl - pos.bMtrlPawns) / 2;
        if (m <= minM)
            return 0;
        final int maxM = qV + 2 * rV + 2 * bV + 2 * nV;
        int score = kingSafetyKPPart(pos);
        if (Position.getY(pos.wKingSq) == 0) {
            if (((pos.pieceTypeBB[Piece.WKING] & 0x60L) != 0) && // King on f1 or g1
                ((pos.pieceTypeBB[Piece.WROOK] & 0xC0L) != 0) && // Rook on g1 or h1
                ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[6]) != 0) &&
                ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[7]) != 0)) {
                score -= 6 * 15;
            } else
            if (((pos.pieceTypeBB[Piece.WKING] & 0x6L) != 0) && // King on b1 or c1
                ((pos.pieceTypeBB[Piece.WROOK] & 0x3L) != 0) && // Rook on a1 or b1
                ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[0]) != 0) &&
                ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[1]) != 0)) {
                score -= 6 * 15;
            }
        }
        if (Position.getY(pos.bKingSq) == 7) {
            if (((pos.pieceTypeBB[Piece.BKING] & 0x6000000000000000L) != 0) && // King on f8 or g8
                ((pos.pieceTypeBB[Piece.BROOK] & 0xC000000000000000L) != 0) && // Rook on g8 or h8
                ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[6]) != 0) &&
                ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[7]) != 0)) {
                score += 6 * 15;
            } else
            if (((pos.pieceTypeBB[Piece.BKING] & 0x600000000000000L) != 0) && // King on b8 or c8
                ((pos.pieceTypeBB[Piece.BROOK] & 0x300000000000000L) != 0) && // Rook on a8 or b8
                ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[0]) != 0) &&
                ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[1]) != 0)) {
                score += 6 * 15;
            }
        }
        score += (bKingAttacks - wKingAttacks) * 4;
        final int kSafety = interpolate(m, minM, 0, maxM, score);
        return kSafety;
    }

    private static final class KingSafetyHashData {
        long key;
        int score;
    }
    private static final KingSafetyHashData[] kingSafetyHash;
    static {
        final int numEntries = 1 << 15;
        kingSafetyHash = new KingSafetyHashData[numEntries];
        for (int i = 0; i < numEntries; i++) {
            KingSafetyHashData ksh = new KingSafetyHashData();
            ksh.key = -1;
            ksh.score = 0;
            kingSafetyHash[i] = ksh;
        }
    }

    private final int kingSafetyKPPart(Position pos) {
        // FIXME!!! Try non-linear king safety
        final long key = pos.pawnZobristHash() ^ pos.kingZobristHash();
        KingSafetyHashData ksh = kingSafetyHash[(int)key & (kingSafetyHash.length - 1)];
        if (ksh.key != key) {
            int score = 0;
            long wPawns = pos.pieceTypeBB[Piece.WPAWN];
            long bPawns = pos.pieceTypeBB[Piece.BPAWN];
            {
                int safety = 0;
                int halfOpenFiles = 0;
                if (Position.getY(pos.wKingSq) < 2) {
                    long shelter = 1L << Position.getX(pos.wKingSq);
                    shelter |= ((shelter & BitBoard.maskBToHFiles) >>> 1) |
                               ((shelter & BitBoard.maskAToGFiles) << 1);
                    shelter <<= 8;
                    safety += 3 * Long.bitCount(wPawns & shelter);
                    safety -= 2 * Long.bitCount(bPawns & (shelter | (shelter << 8)));
                    shelter <<= 8;
                    safety += 2 * Long.bitCount(wPawns & shelter);
                    shelter <<= 8;
                    safety -= Long.bitCount(bPawns & shelter);
                    
                    long wOpen = BitBoard.southFill(shelter) & (~BitBoard.southFill(wPawns)) & 0xff;
                    if (wOpen != 0) {
                        halfOpenFiles += 25 * Long.bitCount(wOpen & 0xe7);
                        halfOpenFiles += 10 * Long.bitCount(wOpen & 0x18);
                    }
                    long bOpen = BitBoard.southFill(shelter) & (~BitBoard.southFill(bPawns)) & 0xff;
                    if (bOpen != 0) {
                        halfOpenFiles += 25 * Long.bitCount(bOpen & 0xe7);
                        halfOpenFiles += 10 * Long.bitCount(bOpen & 0x18);
                    }
                    safety = Math.min(safety, 8);
                }
                final int kSafety = (safety - 9) * 15 - halfOpenFiles;
                score += kSafety;
            }
            {
                int safety = 0;
                int halfOpenFiles = 0;
                if (Position.getY(pos.bKingSq) >= 6) {
                    long shelter = 1L << (56 + Position.getX(pos.bKingSq));
                    shelter |= ((shelter & BitBoard.maskBToHFiles) >>> 1) |
                               ((shelter & BitBoard.maskAToGFiles) << 1);
                    shelter >>>= 8;
                    safety += 3 * Long.bitCount(bPawns & shelter);
                    safety -= 2 * Long.bitCount(wPawns & (shelter | (shelter >>> 8)));
                    shelter >>>= 8;
                    safety += 2 * Long.bitCount(bPawns & shelter);
                    shelter >>>= 8;
                    safety -= Long.bitCount(wPawns & shelter);

                    long wOpen = BitBoard.southFill(shelter) & (~BitBoard.southFill(wPawns)) & 0xff;
                    if (wOpen != 0) {
                        halfOpenFiles += 25 * Long.bitCount(wOpen & 0xe7);
                        halfOpenFiles += 10 * Long.bitCount(wOpen & 0x18);
                    }
                    long bOpen = BitBoard.southFill(shelter) & (~BitBoard.southFill(bPawns)) & 0xff;
                    if (bOpen != 0) {
                        halfOpenFiles += 25 * Long.bitCount(bOpen & 0xe7);
                        halfOpenFiles += 10 * Long.bitCount(bOpen & 0x18);
                    }
                    safety = Math.min(safety, 8);
                }
                final int kSafety = (safety - 9) * 15 - halfOpenFiles;
                score -= kSafety;
            }
            ksh.key = key;
            ksh.score = score;
        }
        return ksh.score;
    }

    /** Implements special knowledge for some endgame situations. */
    private final int endGameEval(Position pos, int oldScore) {
        int score = oldScore;
        if (pos.wMtrl + pos.bMtrl > 6 * rV)
            return score;
        final int wMtrlPawns = pos.wMtrlPawns;
        final int bMtrlPawns = pos.bMtrlPawns;
        final int wMtrlNoPawns = pos.wMtrl - wMtrlPawns;
        final int bMtrlNoPawns = pos.bMtrl - bMtrlPawns;

        boolean handled = false;
        if ((wMtrlPawns + bMtrlPawns == 0) && (wMtrlNoPawns < rV) && (bMtrlNoPawns < rV)) {
            // King + minor piece vs king + minor piece is a draw
            return 0;
        }
        if (!handled && (pos.wMtrl == qV) && (pos.bMtrl == pV) && (pos.pieceTypeBB[Piece.WQUEEN] != 0)) {
            int wk = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WKING]);
            int wq = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WQUEEN]);
            int bk = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BKING]);
            int bp = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BPAWN]);
            score = evalKQKP(wk, wq, bk, bp, pos.whiteMove);
            handled = true;
        }
        if (!handled && (pos.wMtrl == rV) && (pos.pieceTypeBB[Piece.WROOK] != 0)) {
            if (pos.bMtrl == pV) {
                int bp = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BPAWN]);
                score = krkpEval(pos.getKingSq(true), pos.getKingSq(false),
                        bp, pos.whiteMove);
                handled = true;
            } else if ((pos.bMtrl == bV) && (pos.pieceTypeBB[Piece.BBISHOP] != 0)) {
                score /= 8;
                final int kSq = pos.getKingSq(false);
                final int x = Position.getX(kSq);
                final int y = Position.getY(kSq);
                if ((pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskDarkSq) != 0) {
                    score += (7 - distToH1A8[7-y][7-x]) * 7;
                } else {
                    score += (7 - distToH1A8[7-y][x]) * 7;
                }
                handled = true;
            }
        }
        if (!handled && (pos.bMtrl == qV) && (pos.wMtrl == pV) && (pos.pieceTypeBB[Piece.BQUEEN] != 0)) {
            int bk = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BKING]);
            int bq = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BQUEEN]);
            int wk = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WKING]);
            int wp = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WPAWN]);
            score = -evalKQKP(63-bk, 63-bq, 63-wk, 63-wp, !pos.whiteMove);
            handled = true;
        }
        if (!handled && (pos.bMtrl == rV) && (pos.pieceTypeBB[Piece.BROOK] != 0)) {
            if (pos.wMtrl == pV) {
                int wp = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WPAWN]);
                score = -krkpEval(63-pos.getKingSq(false), 63-pos.getKingSq(true),
                        63-wp, !pos.whiteMove);
                handled = true;
            } else if ((pos.wMtrl == bV) && (pos.pieceTypeBB[Piece.WBISHOP] != 0)) {
                score /= 8;
                final int kSq = pos.getKingSq(true);
                final int x = Position.getX(kSq);
                final int y = Position.getY(kSq);
                if ((pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskDarkSq) != 0) {
                    score -= (7 - distToH1A8[7-y][7-x]) * 7;
                } else {
                    score -= (7 - distToH1A8[7-y][x]) * 7;
                }
                handled = true;
            }
        }
        if (!handled && (score > 0)) {
            if ((wMtrlPawns == 0) && (wMtrlNoPawns <= bMtrlNoPawns + bV)) {
                if (wMtrlNoPawns < rV) {
                    return -pos.bMtrl / 50;
                } else {
                    score /= 8;         // Too little excess material, probably draw
                    handled = true;
                }
            } else if ((pos.pieceTypeBB[Piece.WROOK] | pos.pieceTypeBB[Piece.WKNIGHT] |
                        pos.pieceTypeBB[Piece.WQUEEN]) == 0) {
                // Check for rook pawn + wrong color bishop
                if (((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskBToHFiles) == 0) &&
                    ((pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskLightSq) == 0) &&
                    ((pos.pieceTypeBB[Piece.BKING] & 0x0303000000000000L) != 0)) {
                    return 0;
                } else
                if (((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskAToGFiles) == 0) &&
                    ((pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskDarkSq) == 0) &&
                    ((pos.pieceTypeBB[Piece.BKING] & 0xC0C0000000000000L) != 0)) {
                    return 0;
                }
            }
        }
        if (!handled) {
            if (bMtrlPawns == 0) {
                if (wMtrlNoPawns - bMtrlNoPawns > bV) {
                    int wKnights = Long.bitCount(pos.pieceTypeBB[Piece.WKNIGHT]);
                    int wBishops = Long.bitCount(pos.pieceTypeBB[Piece.WBISHOP]);
                    if ((wKnights == 2) && (pos.wMtrl == 2 * nV) && (bMtrlNoPawns == 0)) {
                        score /= 50;    // KNNK is a draw
                    } else if ((wKnights == 1) && (wBishops == 1) && (wMtrlNoPawns == nV + bV) && (bMtrlNoPawns == 0)) {
                        score /= 10;
                        score += nV + bV + 300;
                        final int kSq = pos.getKingSq(false);
                        final int x = Position.getX(kSq);
                        final int y = Position.getY(kSq);
                        if ((pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskDarkSq) != 0) {
                            score += (7 - distToH1A8[7-y][7-x]) * 10;
                        } else {
                            score += (7 - distToH1A8[7-y][x]) * 10;
                        }
                    } else {
                        score += 300;       // Enough excess material, should win
                    }
                    handled = true;
                } else if ((wMtrlNoPawns + bMtrlNoPawns == 0) && (wMtrlPawns == pV)) { // KPK
                    int wp = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WPAWN]);
                    score = kpkEval(pos.getKingSq(true), pos.getKingSq(false),
                                    wp, pos.whiteMove);
                    handled = true;
                }
            }
        }
        if (!handled && (score < 0)) {
            if ((bMtrlPawns == 0) && (bMtrlNoPawns <= wMtrlNoPawns + bV)) {
                if (bMtrlNoPawns < rV) {
                    return pos.wMtrl / 50;
                } else {
                    score /= 8;         // Too little excess material, probably draw
                    handled = true;
                }
            } else if ((pos.pieceTypeBB[Piece.BROOK] | pos.pieceTypeBB[Piece.BKNIGHT] |
                        pos.pieceTypeBB[Piece.BQUEEN]) == 0) {
                // Check for rook pawn + wrong color bishop
                if (((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskBToHFiles) == 0) &&
                    ((pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskDarkSq) == 0) &&
                    ((pos.pieceTypeBB[Piece.WKING] & 0x0303L) != 0)) {
                    return 0;
                } else
                if (((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskAToGFiles) == 0) &&
                    ((pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskLightSq) == 0) &&
                    ((pos.pieceTypeBB[Piece.WKING] & 0xC0C0L) != 0)) {
                    return 0;
                }
            }
        }
        if (!handled) {
            if (wMtrlPawns == 0) {
                if (bMtrlNoPawns - wMtrlNoPawns > bV) {
                    int bKnights = Long.bitCount(pos.pieceTypeBB[Piece.BKNIGHT]);
                    int bBishops = Long.bitCount(pos.pieceTypeBB[Piece.BBISHOP]);
                    if ((bKnights == 2) && (pos.bMtrl == 2 * nV) && (wMtrlNoPawns == 0)) {
                        score /= 50;    // KNNK is a draw
                    } else if ((bKnights == 1) && (bBishops == 1) && (bMtrlNoPawns == nV + bV) && (wMtrlNoPawns == 0)) {
                        score /= 10;
                        score -= nV + bV + 300;
                        final int kSq = pos.getKingSq(true);
                        final int x = Position.getX(kSq);
                        final int y = Position.getY(kSq);
                        if ((pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskDarkSq) != 0) {
                            score -= (7 - distToH1A8[7-y][7-x]) * 10;
                        } else {
                            score -= (7 - distToH1A8[7-y][x]) * 10;
                        }
                    } else {
                        score -= 300;       // Enough excess material, should win
                    }
                    handled = true;
                } else if ((wMtrlNoPawns + bMtrlNoPawns == 0) && (bMtrlPawns == pV)) { // KPK
                    int bp = BitBoard.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BPAWN]);
                    score = -kpkEval(63-pos.getKingSq(false), 63-pos.getKingSq(true),
                                     63-bp, !pos.whiteMove);
                    handled = true;
                }
            }
        }
        return score;

        // FIXME! Add evaluation of KRPKR   : eg 8/8/8/5pk1/1r6/R7/8/4K3 w - - 0 74
        // FIXME! KRBKR is very hard to draw
    }

    private static final int evalKQKP(int wKing, int wQueen, int bKing, int bPawn, boolean whiteMove) {
        boolean canWin = false;
        if (((1L << bKing) & 0xFFFF) == 0) {
            canWin = true; // King doesn't support pawn
        } else if (Math.abs(Position.getX(bPawn) - Position.getX(bKing)) > 2) {
            canWin = true; // King doesn't support pawn
        } else {
            switch (bPawn) {
            case 8:  // a2
                canWin = ((1L << wKing) & 0x0F1F1F1F1FL) != 0;
                if (canWin && (bKing == 0) && (Position.getX(wQueen) == 1) && !whiteMove)
                    canWin = false; // Stale-mate
                break;
            case 10: // c2
                canWin = ((1L << wKing) & 0x071F1F1FL) != 0;
                break;
            case 13: // f2
                canWin = ((1L << wKing) & 0xE0F8F8F8L) != 0;
                break;
            case 15: // h2
                canWin = ((1L << wKing) & 0xF0F8F8F8F8L) != 0;
                if (canWin && (bKing == 7) && (Position.getX(wQueen) == 6) && !whiteMove)
                    canWin = false; // Stale-mate
                break;
            default:
                canWin = true;
                break;
            }
        }

        final int dist = BitBoard.getDistance(wKing, bPawn);
        int score = qV - pV - 20 * dist;
        if (!canWin)
            score /= 50;
        return score;
    }

    private static final int kpkEval(int wKing, int bKing, int wPawn, boolean whiteMove) {
        if (Position.getX(wKing) >= 4) { // Mirror X
            wKing ^= 7;
            bKing ^= 7;
            wPawn ^= 7;
        }
        int index = whiteMove ? 0 : 1;
        index = index * 32 + Position.getY(wKing)*4+Position.getX(wKing);
        index = index * 64 + bKing;
        index = index * 48 + wPawn - 8;

        int bytePos = index / 8;
        int bitPos = index % 8;
        boolean draw = (((int)kpkTable[bytePos]) & (1 << bitPos)) == 0;
        if (draw)
            return 0;
        return qV - pV / 4 * (7-Position.getY(wPawn));
    }

    private static final int krkpEval(int wKing, int bKing, int bPawn, boolean whiteMove) {
        if (Position.getX(bKing) >= 4) { // Mirror X
            wKing ^= 7;
            bKing ^= 7;
            bPawn ^= 7;
        }
        int index = whiteMove ? 0 : 1;
        index = index * 32 + Position.getY(bKing)*4+Position.getX(bKing);
        index = index * 48 + bPawn - 8;
        index = index * 8 + Position.getY(wKing);
        byte mask = krkpTable[index];
        boolean canWin = (mask & (1 << Position.getX(wKing))) != 0;

        int score = rV - pV + Position.getY(bPawn) * pV / 4;
        if (canWin)
            score += 150;
        else
            score /= 50;
        return score;
    }

    /**
     * Interpolate between (x1,y1) and (x2,y2).
     * If x < x1, return y1, if x > x2 return y2. Otherwise, use linear interpolation.
     */
    static final int interpolate(int x, int x1, int y1, int x2, int y2) {
        if (x > x2) {
            return y2;
        } else if (x < x1) {
            return y1;
        } else {
            return (x - x1) * (y2 - y1) / (x2 - x1) + y1;
        }
    }
}
