/*
  Copyright (c) 2011-2013 Ronald de Man
*/

#ifndef RTB_CORE_HPP_
#define RTB_CORE_HPP_

#ifndef _WIN32
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
#define TBPIECES 7

#define WDL_MAGIC 0x5d23e871
#define DTZ_MAGIC 0xa50c66d7

#define TBHASHBITS 11

struct TBHashEntry;

using base_t = uint64_t;

struct PairsData {
    char *indextable;
    uint16_t *sizetable;
    uint8_t *data;
    uint16_t *offset;
    uint8_t *symlen;
    uint8_t *sympat;
    int blocksize;
    int idxbits;
    int min_len;
    base_t base[1]; // C++ complains about base[]...
};

struct TBEntry {
    uint8_t *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<uint8_t> ready;
    uint8_t num;
    uint8_t symmetric;
    uint8_t has_pawns;
} __attribute__((__may_alias__));

struct TBEntry_piece {
    uint8_t *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<uint8_t> ready;
    uint8_t num;
    uint8_t symmetric;
    uint8_t has_pawns;
    uint8_t enc_type;
    struct PairsData *precomp[2];
    uint64_t factor[2][TBPIECES];
    uint8_t pieces[2][TBPIECES];
    uint8_t norm[2][TBPIECES];
};

struct TBEntry_pawn {
    uint8_t *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<uint8_t> ready;
    uint8_t num;
    uint8_t symmetric;
    uint8_t has_pawns;
    uint8_t pawns[2];
    struct {
        struct PairsData *precomp[2];
        uint64_t factor[2][TBPIECES];
        uint8_t pieces[2][TBPIECES];
        uint8_t norm[2][TBPIECES];
    } file[4];
};

struct DTZEntry_piece {
    char *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<uint8_t> ready;
    uint8_t num;
    uint8_t symmetric;
    uint8_t has_pawns;
    uint8_t enc_type;
    struct PairsData *precomp;
    uint64_t factor[TBPIECES];
    uint8_t pieces[TBPIECES];
    uint8_t norm[TBPIECES];
    uint8_t flags; // accurate, mapped, side
    uint16_t map_idx[4];
    uint8_t *map;
};

struct DTZEntry_pawn {
    char *data;
    uint64_t key;
    uint64_t mapping;
    std::atomic<uint8_t> ready;
    uint8_t num;
    uint8_t symmetric;
    uint8_t has_pawns;
    uint8_t pawns[2];
    struct {
        struct PairsData *precomp;
        uint64_t factor[TBPIECES];
        uint8_t pieces[TBPIECES];
        uint8_t norm[TBPIECES];
    } file[4];
    uint8_t flags[4];
    uint16_t map_idx[4][4];
    uint8_t *map;
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
