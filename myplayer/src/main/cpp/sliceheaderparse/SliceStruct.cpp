#include "stdafx.h"
#include "SeqParamSet.h"
#include "PicParamSet.h"
#include "SliceStruct.h"
#include "SliceHeader.h"

#include <iostream>
using namespace std;

CSliceStruct::CSliceStruct(UINT8	*pSODB, CSeqParamSet *sps, CPicParamSet *pps, UINT8	nalType, UINT32 sliceIdx)
{	

	m_pSODB = pSODB;
	m_sps_active = sps;
	m_pps_active = pps;
	m_nalType = nalType;

	m_sliceHeader = NULL;
	m_sliceIdx = sliceIdx;

	/*m_max_mb_number = m_sps_active->Get_pic_width_in_mbs() * m_sps_active->Get_pic_height_in_mbs();
	m_macroblocks = new CMacroblock *[m_max_mb_number];
	memset(m_macroblocks, NULL, m_max_mb_number * sizeof(CMacroblock *));*/
}

CSliceStruct::~CSliceStruct()
{
	if (m_sliceHeader)
	{
		delete m_sliceHeader;
		m_sliceHeader = NULL;
	}

	/*if (m_macroblocks)
	{
		for (int idx = 0; idx < m_max_mb_number; idx++)
		{
			if (m_macroblocks[idx])
			{
				delete m_macroblocks[idx];
				m_macroblocks[idx] = NULL;
			}
		}

		delete m_macroblocks;
		m_macroblocks = NULL;
	}*/
}

int CSliceStruct::Parse()
{
	m_sliceHeader = new CSliceHeader(m_pSODB, m_sps_active, m_pps_active, m_nalType);
	return m_sliceHeader->Parse_slice_header();
}

CMacroblock * CSliceStruct::Get_macroblock_at_index(int mbIdx)
{
	return m_macroblocks[mbIdx];
}
