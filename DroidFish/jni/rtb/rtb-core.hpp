/*
  Copyright (c) 2011-2013 Ronald de Man
*/

#ifndef RTB_CORE_HPP_
#define RTB_CORE_HPP_

#ifndef __WIN32__
#define SEP_CHAR ':'
#define FD int
#define FD_ERR -1
#else
#include <windows.h>
#define SEP_CHAR ';'
#define FD HANDLE
#define FD_ERR INVALID_HANDLE_VALUE
#endif

#include <stdint.h>
#include <atomic>

#define WDLSUFFIX ".rtbw"
#define DTZSUFFIX ".rtbz"
#define TBPIECES 6

#define WDL_MAGIC 0x5d23e871
#define DTZ_MAGIC 0xa50c66d7

#define TBHASHBITS 11

typedef unsigned char ubyte;
typedef unsigned short ushort;

struct TBHashEntry;

typedef uint64_t base_t;

struct PairsData {
    char *indextable;
    ushort *sizetable;
    ubyte *data;
    ushort *offset;
    ubyte *symlen;
    ubyte *sympat;
    int blocksize;
    int idxbits;
    int min_len;
    base_t base[1]; // C++ complains about base[]...
};

struct TBEntry {
    char *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<ubyte> ready;
    ubyte num;
    ubyte symmetric;
    ubyte has_pawns;
} __attribute__((__may_alias__));

struct TBEntry_piece {
    char *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<ubyte> ready;
    ubyte num;
    ubyte symmetric;
    ubyte has_pawns;
    ubyte enc_type;
    struct PairsData *precomp[2];
    int factor[2][TBPIECES];
    ubyte pieces[2][TBPIECES];
    ubyte norm[2][TBPIECES];
};

struct TBEntry_pawn {
    char *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<ubyte> ready;
    ubyte num;
    ubyte symmetric;
    ubyte has_pawns;
    ubyte pawns[2];
    struct {
        struct PairsData *precomp[2];
        int factor[2][TBPIECES];
        ubyte pieces[2][TBPIECES];
        ubyte norm[2][TBPIECES];
    } file[4];
};

struct DTZEntry_piece {
    char *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<ubyte> ready;
    ubyte num;
    ubyte symmetric;
    ubyte has_pawns;
    ubyte enc_type;
    struct PairsData *precomp;
    int factor[TBPIECES];
    ubyte pieces[TBPIECES];
    ubyte norm[TBPIECES];
    ubyte flags; // accurate, mapped, side
    ushort map_idx[4];
    ubyte *map;
};

struct DTZEntry_pawn {
    char *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<ubyte> ready;
    ubyte num;
    ubyte symmetric;
    ubyte has_pawns;
    ubyte pawns[2];
    struct {
        struct PairsData *precomp;
        int factor[TBPIECES];
        ubyte pieces[TBPIECES];
        ubyte norm[TBPIECES];
    } file[4];
    ubyte flags[4];
    ushort map_idx[4][4];
    ubyte *map;
};

struct TBHashEntry {
    uint64_t key;
    struct TBEntry *ptr;
};

struct DTZTableEntry {
    uint64_t key1;
    uint64_t key2;
    std::atomic<TBEntry*> entry;
};

#endif
