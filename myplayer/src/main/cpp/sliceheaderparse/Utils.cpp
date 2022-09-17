#include "stdafx.h"
#include "Utils.h"

//********************Parsing Bitstream**********************
// Get bool value from bit position..
int Get_bit_at_position(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition)
{
	UINT8 mask = 0, val = 0;

	mask = 1 << (7 - bitPosition);
	val = ((buf[bytePosition] & mask) != 0);
	if (++bitPosition > 7)
	{
		bytePosition++;
		bitPosition = 0;
	}

	return val;
}

// Parse bit stream using Expo-Columb coding
int Get_uev_code_num(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition)
{
	assert(bitPosition < 8);
	UINT8 val = 0, prefixZeroCount = 0;
	int prefix = 0, surfix = 0;

	while (true)
	{
		val = Get_bit_at_position(buf, bytePosition, bitPosition);
		if (val == 0)
		{
			prefixZeroCount++;
		}
		else
		{
			break;
		}
	}
	prefix = (1 << prefixZeroCount) - 1;
	for (size_t i = 0; i < prefixZeroCount; i++)
	{
		val = Get_bit_at_position(buf, bytePosition, bitPosition);
		surfix += val * (1 << (prefixZeroCount - i - 1));
	}

	prefix += surfix;

	return prefix;
}

// Parse bit stream using signed-Expo-Columb coding
int Get_sev_code_num(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition)
{
	int uev = Get_uev_code_num(buf, bytePosition, bitPosition);
	int sign = (uev % 2) ? 1 : -1;
	int sev = sign * ((uev + 1) >> 1);
	
	return sev;
}

// Parse bit stream as unsigned int bits
int Get_uint_code_num(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition, UINT8 length)
{
	UINT32 uVal = 0;

	for (int idx = 0; idx < length; idx++)
	{
		uVal += Get_bit_at_position(buf, bytePosition, bitPosition) << (length - idx - 1);
	}

	return uVal;
}
int Peek_uint_code_num(UINT8 *buf, UINT32 bytePosition, UINT8 bitPosition, UINT8 length)
{
	UINT32 uVal = 0;

	for (int idx = 0; idx < length; idx++)
	{
		uVal += Get_bit_at_position(buf, bytePosition, bitPosition) << (length - idx - 1);
	}

	return uVal;
}


// Parse bit stream as me(coded_block_pattern)
int Get_me_code_num(UINT8 *buf, UINT32 &bytePosition, UINT8 &bitPosition, UINT8 mode)
{
	int intra_cbp[48] = { 47, 31, 15, 0, 23, 27, 29, 30, 7, 11, 13, 14, 39, 43, 45, 46, 16, 3, 5, 10, 12, 19, 21, 26, 28, 35, 37, 42, 44, 1, 2 ,4, 8, 17,18,20,24,6,9,22,25,32,33,34,36,40,38,41};
	int uev = Get_uev_code_num(buf, bytePosition, bitPosition);
	return intra_cbp[uev];
}
//***********************************************************


/*
block position of each index:
row:  0   1   2   3
_______________
col:0	| 0 | 1 | 4 | 5 |
1	| 2 | 3 | 6 | 7 |
2	| 8 | 9 | 12| 13|
3	| 10| 11| 14| 15|
*/
int block_index_to_position(UINT8 blkIdx, UINT8 &block_pos_row, UINT8 &block_pos_column)
{
	/*
	block8_index of each index:			block4_index of each index:
	row:  0   1   2   3					row:  0   1   2   3
	_______________					 _______________
	col:0	| 0 | 0 | 1 | 1 |			col:0	| 0 | 1 | 0 | 1 |
	1	| 0 | 0 | 1 | 1 |				1	| 2 | 3 | 2 | 3 |
	2	| 2 | 2 | 3 | 3 |				2	| 0 | 1 | 0 | 1 |
	3	| 2 | 2 | 3 | 3 |				3	| 2 | 3 | 2 | 3 |
	*/
	UINT8 block8_idx = blkIdx / 4, block4_index = blkIdx % 4; /* 0 1 2 3 */

															  /*
															  (block_row, block_column) of each index:
															  row:    0       1       2       3
															  _______________ _______________
															  col:0	| (0,0) | (1,0) | (0,0) | (1,0) |
															  1	| (0,1) | (1,1) | (0,1) | (1,1) |
															  2	| (0,0) | (1,0) | (0,0) | (1,0) |
															  3	| (0,1) | (1,1) | (0,1) | (1,1) |
															  */
	UINT8 block4_row = block4_index % 2, block4_column = block4_index / 2; /* 0 1 */

																		   /*
																		   (block_row, block_column) of each index:
																		   row:    0       1       2       3
																		   _______________ _______________
																		   col:0	| (0,0) | (1,0) | (2,0) | (3,0) |
																		   1	| (0,1) | (1,1) | (2,1) | (3,1) |
																		   2	| (0,2) | (1,2) | (2,2) | (3,2) |
																		   3	| (0,3) | (1,3) | (2,3) | (3,3) |
																		   */
	UINT8 block_row = block4_row + 2 * (block8_idx % 2), block_column = block4_column + 2 * (block8_idx / 2);

	block_pos_row = block_row;
	block_pos_column = block_column;

	return kPARSING_ERROR_NO_ERROR;
}

UINT8 position_to_block_index(UINT8 block_pos_row, UINT8 block_pos_column)
{
	int block8_row = block_pos_row / 2, block8_column = block_pos_column / 2, block8_index = block8_row + block8_column * 2;
	int block4_row = block_pos_row % 2, block4_column = block_pos_column % 2, block4_index = block4_row + block4_column * 2;

	return block4_index + 4 * block8_index;
}
