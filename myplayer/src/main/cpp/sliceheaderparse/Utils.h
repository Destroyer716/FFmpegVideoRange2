#ifndef _UTILS_H_
#define _UTILS_H_
#include "Global.h"

//********************Parsing Bitstream**********************
// Get bool value from bit position..
int Get_bit_at_position(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition);

// Parse bit stream using Expo-Columb coding
int Get_uev_code_num(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition);

// Parse bit stream using signed-Expo-Columb coding
int Get_sev_code_num(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition);

// Parse bit stream as unsigned int bits
int Get_uint_code_num(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition, UINT8 length);
int Peek_uint_code_num(UINT8 *buf, UINT32 bytePosition, UINT8 bitPosition, UINT8 length);

// Parse bit stream as me(coded_block_pattern)
int Get_me_code_num(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition, UINT8 mode);
//***********************************************************


int block_index_to_position(UINT8 blkIdx, UINT8 &block_pos_row, UINT8 &block_pos_column);
UINT8 position_to_block_index(UINT8 block_pos_row, UINT8 block_pos_column);
#endif