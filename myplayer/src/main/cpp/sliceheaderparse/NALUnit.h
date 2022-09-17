#ifndef _NAL_UNIT_H_
#define _NAL_UNIT_H_
#include "stdafx.h"
class CSeqParamSet;
class CPicParamSet;
class CNALUnit
{
public:
	CNALUnit(UINT8	*pSODB, UINT32	SODBLength, UINT8 nalType);
	~CNALUnit();

	int Parse_as_seq_param_set(CSeqParamSet *sps);
	int Parse_as_pic_param_set(CPicParamSet *pps);

	UINT8* Get_SODB();

private:
	UINT8	*m_pSODB;
	UINT32	m_SODBLength;

	UINT8	m_nalType;
};

#endif
