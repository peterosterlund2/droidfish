/*
  Copyright (c) 2013 Ronald de Man
  This file may be redistributed and/or modified without restrictions.

  tbprobe.cpp contains the Stockfish-specific routines of the
  tablebase probing code. It should be relatively easy to adapt
  this code to other chess engines.
*/

#include "piece.hpp"
#include "position.hpp"
#include "moveGen.hpp"

#include <type_traits>

#include "rtb-probe.hpp"
#include "rtb-core.hpp"

#include "rtb-core-impl.hpp"

int Syzygy::TBLargest = 0;

// Given a position with 6 or fewer pieces, produce a text string
// of the form KQPvKRP, where "KQP" represents the white pieces if
// mirror == false and the black pieces if mirror == true.
static void prt_str(Position& pos, char *str, bool mirror)
{
    static_assert(Piece::WQUEEN  == Piece::WKING   + 1, "");
    static_assert(Piece::WROOK   == Piece::WQUEEN  + 1, "");
    static_assert(Piece::WBISHOP == Piece::WROOK   + 1, "");
    static_assert(Piece::WKNIGHT == Piece::WBISHOP + 1, "");
    static_assert(Piece::WPAWN   == Piece::WKNIGHT + 1, "");
    static_assert(Piece::BQUEEN  == Piece::BKING   + 1, "");
    static_assert(Piece::BROOK   == Piece::BQUEEN  + 1, "");
    static_assert(Piece::BBISHOP == Piece::BROOK   + 1, "");
    static_assert(Piece::BKNIGHT == Piece::BBISHOP + 1, "");
    static_assert(Piece::BPAWN   == Piece::BKNIGHT + 1, "");

    static char pchr[Piece::nPieceTypes+1] = " KQRBNPKQRBNP";

    int p1Beg = mirror ? Piece::BKING : Piece::WKING;
    int p1End = mirror ? Piece::BPAWN : Piece::WPAWN;
    int p2Beg = mirror ? Piece::WKING : Piece::BKING;
    int p2End = mirror ? Piece::WPAWN : Piece::BPAWN;

    for (int p = p1Beg; p <= p1End; p++) {
        int cnt = BitBoard::bitCount(pos.pieceTypeBB((Piece::Type)p));
        for (int i = 0; i < cnt; i++)
            *str++ = pchr[p];
    }
    *str++ = 'v';
    for (int p = p2Beg; p <= p2End; p++) {
        int cnt = BitBoard::bitCount(pos.pieceTypeBB((Piece::Type)p));
        for (int i = 0; i < cnt; i++)
            *str++ = pchr[p];
    }
    *str++ = 0;
}

// Given a position, produce a 64-bit material signature key.
// If the engine supports such a key, it should equal the engine's key.
static uint64_t calc_key(const Position& pos, bool mirror)
{
    uint64_t h = mirror ? MatId::mirror(pos.materialId()) : pos.materialId();
    h *= 0x842c2f50a7ac0ae1ULL;
    h = ((h >> 32) ^ h) * 0xace7b66dbad28265ULL;
    return h;
}

// Produce a 64-bit material key corresponding to the material combination
// defined by pcs[16], where pcs[1], ..., pcs[6] is the number of white
// pawns, ..., kings and pcs[9], ..., pcs[14] is the number of black
// pawns, ..., kings.
static uint64_t calc_key_from_pcs(const int *pcs, bool mirror)
{
    MatId key;
    key.addPieceCnt(Piece::WPAWN,   pcs[1]);
    key.addPieceCnt(Piece::WKNIGHT, pcs[2]);
    key.addPieceCnt(Piece::WBISHOP, pcs[3]);
    key.addPieceCnt(Piece::WROOK,   pcs[4]);
    key.addPieceCnt(Piece::WQUEEN,  pcs[5]);
    key.addPieceCnt(Piece::BPAWN,   pcs[8+1]);
    key.addPieceCnt(Piece::BKNIGHT, pcs[8+2]);
    key.addPieceCnt(Piece::BBISHOP, pcs[8+3]);
    key.addPieceCnt(Piece::BROOK,   pcs[8+4]);
    key.addPieceCnt(Piece::BQUEEN,  pcs[8+5]);

    uint64_t h = mirror ? MatId::mirror(key()) : key();
    h *= 0x842c2f50a7ac0ae1ULL;
    h = ((h >> 32) ^ h) * 0xace7b66dbad28265ULL;
    return h;
}

static uint64_t get_pieces(const Position& pos, int color, int piece) {
    int p = 7 - piece;
    if (color)
        p += Piece::BKING - Piece::WKING;
    return pos.pieceTypeBB((Piece::Type)p);
}

static inline int pop_lsb(uint64_t& bb) {
    int ret = BitBoard::numberOfTrailingZeros(bb);
    bb &= bb - 1;
    return ret;
}

// probe_wdl_table and probe_dtz_table require similar adaptations.
static int probe_wdl_table(Position& pos, int *success)
{
    struct TBEntry *ptr;
    struct TBHashEntry *ptr2;
    uint64_t idx;
    uint64_t key;
    int i;
    ubyte res;
    int p[TBPIECES];

    // Obtain the position's material signature key.
    key = calc_key(pos, false);

    // Test for KvK.
    if (!key) return 0;

    ptr2 = WDL_hash[key >> (64 - TBHASHBITS)];
    for (i = 0; i < HSHMAX; i++)
        if (ptr2[i].key == key) break;
    if (i == HSHMAX) {
        *success = 0;
        return 0;
    }

    ptr = ptr2[i].ptr;
    ubyte ready = ptr->ready.load(std::memory_order_relaxed);
    std::atomic_thread_fence(std::memory_order_acquire);
    if (!ready) {
        std::lock_guard<std::mutex> L(TB_mutex);
        ready = ptr->ready.load(std::memory_order_relaxed);
        if (!ready) {
            char str[16];
            prt_str(pos, str, ptr->key != key);
            if (!init_table_wdl(ptr, str)) {
                ptr2[i].key = 0ULL;
                *success = 0;
                return 0;
            }
            std::atomic_thread_fence(std::memory_order_release);
            ptr->ready.store(1, std::memory_order_relaxed);
        }
    }

    int bside, mirror, cmirror;
    if (!ptr->symmetric) {
        if (key != ptr->key) {
            cmirror = 8;
            mirror = 0x38;
            bside = pos.isWhiteMove();
        } else {
            cmirror = mirror = 0;
            bside = !pos.isWhiteMove();
        }
    } else {
        cmirror = pos.isWhiteMove() ? 0 : 8;
        mirror = pos.isWhiteMove() ? 0 : 0x38;
        bside = 0;
    }

    // p[i] is to contain the square 0-63 (A1-H8) for a piece of type
    // pc[i] ^ cmirror, where 1 = white pawn, ..., 14 = black king.
    // Pieces of the same type are guaranteed to be consecutive.
    if (!ptr->has_pawns) {
        struct TBEntry_piece *entry = (struct TBEntry_piece *)ptr;
        ubyte *pc = entry->pieces[bside];
        for (i = 0; i < entry->num;) {
            uint64_t bb = get_pieces(pos, (pc[i] ^ cmirror) >> 3, pc[i] & 0x07);
            do {
                p[i++] = pop_lsb(bb);
            } while (bb);
        }
        idx = encode_piece(entry, entry->norm[bside], p, entry->factor[bside]);
        res = decompress_pairs(entry->precomp[bside], idx);
    } else {
        struct TBEntry_pawn *entry = (struct TBEntry_pawn *)ptr;
        int k = entry->file[0].pieces[0][0] ^ cmirror;
        uint64_t bb = get_pieces(pos, k >> 3, k & 0x07);
        i = 0;
        do {
            p[i++] = pop_lsb(bb) ^ mirror;
        } while (bb);
        int f = pawn_file(entry, p);
        ubyte *pc = entry->file[f].pieces[bside];
        for (; i < entry->num;) {
            bb = get_pieces(pos, (pc[i] ^ cmirror) >> 3, pc[i] & 0x07);
            do {
                p[i++] = pop_lsb(bb) ^ mirror;
            } while (bb);
        }
        idx = encode_pawn(entry, entry->file[f].norm[bside], p, entry->file[f].factor[bside]);
        res = decompress_pairs(entry->file[f].precomp[bside], idx);
    }

    return ((int)res) - 2;
}

static int probe_dtz_table(Position& pos, int wdl, int *success)
{
    uint64_t idx;
    int i, res;
    int p[TBPIECES];

    // Obtain the position's material signature key.
    uint64_t key = calc_key(pos, false);

    DTZTableEntry* dtzTabEnt;
    {
        dtzTabEnt = DTZ_hash[key >> (64 - TBHASHBITS)];
        for (i = 0; i < HSHMAX; i++)
            if (dtzTabEnt[i].key1 == key) break;
        if (i == HSHMAX) {
            uint64_t key2 = calc_key(pos, true);
            dtzTabEnt = DTZ_hash[key2 >> (64 - TBHASHBITS)];
            for (i = 0; i < HSHMAX; i++)
                if (dtzTabEnt[i].key2 == key) break;
        }
        if (i == HSHMAX) {
            *success = 0;
            return 0;
        }
        dtzTabEnt += i;
    }

    TBEntry* ptr = dtzTabEnt->entry.load(std::memory_order_relaxed);
    std::atomic_thread_fence(std::memory_order_acquire);
    if (!ptr) {
        std::lock_guard<std::mutex> L(TB_mutex);
        ptr = dtzTabEnt->entry.load(std::memory_order_relaxed);
        if (!ptr) {
            struct TBHashEntry *ptr2 = WDL_hash[key >> (64 - TBHASHBITS)];
            for (i = 0; i < HSHMAX; i++)
                if (ptr2[i].key == key) break;
            if (i == HSHMAX) {
                *success = 0;
                return 0;
            }
            char str[16];
            bool mirror = (ptr2[i].ptr->key != key);
            prt_str(pos, str, mirror);
            ptr = load_dtz_table(str, calc_key(pos, mirror), calc_key(pos, !mirror));
            std::atomic_thread_fence(std::memory_order_release);
            dtzTabEnt->entry.store(ptr, std::memory_order_relaxed);
        }
    }

    if (!ptr) {
        *success = 0;
        return 0;
    }

    int bside, mirror, cmirror;
    if (!ptr->symmetric) {
        if (key != ptr->key) {
            cmirror = 8;
            mirror = 0x38;
            bside = pos.isWhiteMove();
        } else {
            cmirror = mirror = 0;
            bside = !pos.isWhiteMove();
        }
    } else {
        cmirror = pos.isWhiteMove() ? 0 : 8;
        mirror = pos.isWhiteMove() ? 0 : 0x38;
        bside = 0;
    }

    if (!ptr->has_pawns) {
        struct DTZEntry_piece *entry = (struct DTZEntry_piece *)ptr;
        if ((entry->flags & 1) != bside && !entry->symmetric) {
            *success = -1;
            return 0;
        }
        ubyte *pc = entry->pieces;
        for (i = 0; i < entry->num;) {
            uint64_t bb = get_pieces(pos, (pc[i] ^ cmirror) >> 3, pc[i] & 0x07);
            do {
                p[i++] = pop_lsb(bb);
            } while (bb);
        }
        idx = encode_piece((struct TBEntry_piece *)entry, entry->norm, p, entry->factor);
        res = decompress_pairs(entry->precomp, idx);

        if (entry->flags & 2)
            res = entry->map[entry->map_idx[wdl_to_map[wdl + 2]] + res];

        if (!(entry->flags & pa_flags[wdl + 2]) || (wdl & 1))
            res *= 2;
    } else {
        struct DTZEntry_pawn *entry = (struct DTZEntry_pawn *)ptr;
        int k = entry->file[0].pieces[0] ^ cmirror;
        uint64_t bb = get_pieces(pos, k >> 3, k & 0x07);
        i = 0;
        do {
            p[i++] = pop_lsb(bb) ^ mirror;
        } while (bb);
        int f = pawn_file((struct TBEntry_pawn *)entry, p);
        if ((entry->flags[f] & 1) != bside) {
            *success = -1;
            return 0;
        }
        ubyte *pc = entry->file[f].pieces;
        for (; i < entry->num;) {
            bb = get_pieces(pos, (pc[i] ^ cmirror) >> 3, pc[i] & 0x07);
            do {
                p[i++] = pop_lsb(bb) ^ mirror;
            } while (bb);
        }
        idx = encode_pawn((struct TBEntry_pawn *)entry, entry->file[f].norm, p, entry->file[f].factor);
        res = decompress_pairs(entry->file[f].precomp, idx);

        if (entry->flags[f] & 2)
            res = entry->map[entry->map_idx[f][wdl_to_map[wdl + 2]] + res];

        if (!(entry->flags[f] & pa_flags[wdl + 2]) || (wdl & 1))
            res *= 2;
    }

    return res;
}

// Add bishop and rook underpromotion captures to move list.
static void add_underprom_caps(Position& pos, MoveGen::MoveList& moveList)
{
    const int nMoves = moveList.size;
    const bool wtm = pos.isWhiteMove();
    const int queen = wtm ? Piece::WQUEEN : Piece::BQUEEN;
    for (int i = 0; i < nMoves; i++) {
        const Move& m = moveList[i];
        if ((m.promoteTo() == queen) && (pos.getPiece(m.to()) != Piece::EMPTY)) {
            moveList.addMove(m.from(), m.to(), wtm ? Piece::WROOK : Piece::BROOK);
            moveList.addMove(m.from(), m.to(), wtm ? Piece::WBISHOP : Piece::BBISHOP);
        }
    }
}

static int probe_ab(Position& pos, int alpha, int beta, int *success)
{
    // Generate (at least) all legal non-ep captures including (under)promotions.
    // It is OK to generate more, as long as they are filtered out below.
    MoveGen::MoveList moveList;
    const bool inCheck = MoveGen::inCheck(pos);
    if (inCheck) {
        MoveGen::checkEvasions(pos, moveList);
    } else {
        MoveGen::pseudoLegalCaptures(pos, moveList);
        // Since bishop and rook promotions are not included, we need to add them.
        add_underprom_caps(pos, moveList);
    }

    UndoInfo ui;
    for (int m = 0; m < moveList.size; m++) {
        const Move& capture = moveList[m];
        if ((pos.getPiece(capture.to()) == Piece::EMPTY) ||
            !MoveGen::isLegal(pos, capture, inCheck))
            continue;
        pos.makeMove(capture, ui);
        int v = -probe_ab(pos, -beta, -alpha, success);
        pos.unMakeMove(capture, ui);
        if (*success == 0) return 0;
        if (v > alpha) {
            if (v >= beta) {
                *success = 2;
                return v;
            }
            alpha = v;
        }
    }

    int v = probe_wdl_table(pos, success);
    if (*success == 0) return 0;
    if (alpha >= v) {
        *success = 1 + (alpha > 0);
        return alpha;
    } else {
        *success = 1;
        return v;
    }
}

int Syzygy::probe_wdl(Position& pos, int *success)
{
    *success = 1;
    int v = probe_ab(pos, -2, 2, success);

    // If en passant is not possible, we are done.
    if (pos.getEpSquare() == -1)
        return v;
    if (!(*success)) return 0;

    // Now handle en passant.
    int v1 = -3;
    // Generate (at least) all legal en passant captures.
    MoveGen::MoveList moveList;

    const bool inCheck = MoveGen::inCheck(pos);
    if (inCheck) {
        MoveGen::checkEvasions(pos, moveList);
    } else {
        MoveGen::pseudoLegalMoves(pos, moveList);
    }

    const int pawn = pos.isWhiteMove() ? Piece::WPAWN : Piece::BPAWN;
    UndoInfo ui;
    for (int m = 0; m < moveList.size; m++) {
        const Move& capture = moveList[m];
        if ((capture.to() != pos.getEpSquare()) || (pos.getPiece(capture.from()) != pawn) ||
            !MoveGen::isLegal(pos, capture, inCheck))
            continue;
        pos.makeMove(capture, ui);
        int v0 = -probe_ab(pos, -2, 2, success);
        pos.unMakeMove(capture, ui);
        if (*success == 0) return 0;
        if (v0 > v1) v1 = v0;
    }
    if (v1 > -3) {
        if (v1 >= v) v = v1;
        else if (v == 0) {
            // Check whether there is at least one legal non-ep move.
            for (int m = 0; m < moveList.size; m++) {
                const Move& capture = moveList[m];
                if ((capture.to() == pos.getEpSquare()) &&
                    (pos.getPiece(capture.from()) == pawn))
                    continue;
                if (MoveGen::isLegal(pos, capture, inCheck))
                    return v;
            }
            // If not, then we are forced to play the losing ep capture.
            v = v1;
        }
    }

    return v;
}

// This routine treats a position with en passant captures as one without.
static int probe_dtz_no_ep(Position& pos, int *success)
{
    const int wdl = probe_ab(pos, -2, 2, success);
    if (*success == 0) return 0;

    if (wdl == 0) return 0;

    if (*success == 2)
        return wdl == 2 ? 1 : 101;

    MoveGen::MoveList moveList;
    const bool inCheck = MoveGen::inCheck(pos);
    const int pawn = pos.isWhiteMove() ? Piece::WPAWN : Piece::BPAWN;
    UndoInfo ui;

    if (wdl > 0) {
        // Generate at least all legal non-capturing pawn moves
        // including non-capturing promotions.
        if (inCheck) {
            MoveGen::checkEvasions(pos, moveList);
        } else {
            MoveGen::pseudoLegalMoves(pos, moveList);
        }

        for (int m = 0; m < moveList.size; m++) {
            const Move& move = moveList[m];
            if ((pos.getPiece(move.from()) != pawn) ||
                (Position::getX(move.from()) != Position::getX(move.to())) ||
                !MoveGen::isLegal(pos, move, inCheck))
                continue;
            pos.makeMove(move, ui);
            int v = -probe_ab(pos, -2, -wdl + 1, success);
            pos.unMakeMove(move, ui);
            if (*success == 0) return 0;
            if (v == wdl)
                return v == 2 ? 1 : 101;
        }
    }

    int dtz = 1 + probe_dtz_table(pos, wdl, success);

    if (*success >= 0) {
        if (wdl & 1) dtz += 100;
        return wdl >= 0 ? dtz : -dtz;
    }

    if (wdl > 0) {
        int best = 0xffff;
        for (int m = 0; m < moveList.size; m++) {
            const Move& move = moveList[m];
            if ((pos.getPiece(move.to()) != Piece::EMPTY) ||
                (pos.getPiece(move.from()) == pawn) ||
                !MoveGen::isLegal(pos, move, inCheck))
                continue;
            pos.makeMove(move, ui);
            int v = -Syzygy::probe_dtz(pos, success);
            pos.unMakeMove(move, ui);
            if (*success == 0) return 0;
            if (v > 0 && v + 1 < best)
                best = v + 1;
        }
        return best;
    } else {
        int best = -1;
        if (inCheck) {
            MoveGen::checkEvasions(pos, moveList);
        } else {
            MoveGen::pseudoLegalMoves(pos, moveList);
        }
        for (int m = 0; m < moveList.size; m++) {
            const Move& move = moveList[m];
            if (!MoveGen::isLegal(pos, move, inCheck))
                continue;
            pos.makeMove(move, ui);
            int v;
            if (pos.getHalfMoveClock() == 0) {
                if (wdl == -2) v = -1;
                else {
                    v = probe_ab(pos, 1, 2, success);
                    v = (v == 2) ? 0 : -101;
                }
            } else {
                v = -Syzygy::probe_dtz(pos, success) - 1;
            }
            pos.unMakeMove(move, ui);
            if (*success == 0) return 0;
            if (v < best)
                best = v;
        }
        return best;
    }
}

static int wdl_to_dtz[] = {
    -1, -101, 0, 101, 1
};

int Syzygy::probe_dtz(Position& pos, int *success)
{
    *success = 1;
    int v = probe_dtz_no_ep(pos, success);

    if (pos.getEpSquare() == -1)
        return v;
    if (*success == 0) return 0;

    // Now handle en passant.
    int v1 = -3;

    MoveGen::MoveList moveList;
    const bool inCheck = MoveGen::inCheck(pos);
    const int pawn = pos.isWhiteMove() ? Piece::WPAWN : Piece::BPAWN;
    UndoInfo ui;

    if (!inCheck) {
        MoveGen::pseudoLegalMoves(pos, moveList);
    } else {
        MoveGen::checkEvasions(pos, moveList);
    }

    for (int m = 0; m < moveList.size; m++) {
        const Move& capture = moveList[m];
        if ((capture.to() != pos.getEpSquare()) ||
            (pos.getPiece(capture.from()) != pawn) ||
            !MoveGen::isLegal(pos, capture, inCheck))
            continue;
        pos.makeMove(capture, ui);
        int v0 = -probe_ab(pos, -2, 2, success);
        pos.unMakeMove(capture, ui);
        if (*success == 0) return 0;
        if (v0 > v1) v1 = v0;
    }
    if (v1 > -3) {
        v1 = wdl_to_dtz[v1 + 2];
        if (v < -100) {
            if (v1 >= 0)
                v = v1;
        } else if (v < 0) {
            if (v1 >= 0 || v1 < -100)
                v = v1;
        } else if (v > 100) {
            if (v1 > 0)
                v = v1;
        } else if (v > 0) {
            if (v1 == 1)
                v = v1;
        } else if (v1 >= 0) {
            v = v1;
        } else {
            for (int m = 0; m < moveList.size; m++) {
                const Move& move = moveList[m];
                if ((move.to() == pos.getEpSquare()) && (pos.getPiece(move.from()) == pawn))
                    continue;
                if (MoveGen::isLegal(pos, move, inCheck))
                    return v;
            }
            v = v1;
        }
    }

    return v;
}
