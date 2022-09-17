#ifndef _CSliceStruct_H_
#define _CSliceStruct_H_

#include "stdafx.h"


class CSeqParamSet;
class CPicParamSet;
class CSliceHeader;
class CMacroblock;

class CSliceStruct
{
public:
	CSliceStruct(UINT8	*pSODB, CSeqParamSet *sps, CPicParamSet *pps, UINT8	nalType, UINT32 sliceIdx);
	~CSliceStruct();

	CSliceHeader *m_sliceHeader;

	int Parse();
	CMacroblock *Get_macroblock_at_index(int mbIdx);

	CSeqParamSet *m_sps_active;
	CPicParamSet *m_pps_active;

private:
	UINT8	*m_pSODB;
	UINT8   m_nalType;
	UINT32  m_sliceIdx;

	UINT16 m_max_mb_number;
	CMacroblock **m_macroblocks;
};


#endif