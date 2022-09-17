#include "stdafx.h"
#include "Stream.h"
#include "NALUnit.h"
#include "SeqParamSet.h"
#include "PicParamSet.h"
#include "SliceStruct.h"

#include <iostream>

using namespace std;

// 构造函数
CStreamFile::CStreamFile( unsigned char *pData,int dataSize)
{
	m_sps = NULL;
	m_pps = NULL;
	m_IDRSlice = NULL;
	m_nalVec.clear();
	int i = 0;
	/* 前四个字节表示当前NALU的大小 */

	int skipNalSize = 0;
	int currentNalSize = 0;
	while (skipNalSize + currentNalSize + 4 < dataSize){
		currentNalSize = 0;
		for(i = 0; i < 4; i++){
			currentNalSize <<= 8;
			currentNalSize |= pData[i+skipNalSize];
		}
		if (skipNalSize + currentNalSize + 4 < dataSize){
			skipNalSize += (currentNalSize + 4);
		}
		//LOGE("skipNalSize:%d  ,%d",skipNalSize,currentNalSize);

	}

	for(i = skipNalSize; i < dataSize; i++){
		/*if (i < 10+skipNalSize){
			LOGE("CStreamFile: %x",pData[i]);
		}*/
		if (i >=4 +skipNalSize){
			m_nalVec.push_back(pData[i]);
		}
	}

}

//析构函数
CStreamFile::~CStreamFile()
{

	if (m_sps)
	{
		delete m_sps;
		m_sps = NULL;
	}

	if (m_pps)
	{
		delete m_pps;
		m_pps = NULL;
	}

	if (m_IDRSlice)
	{
		delete m_IDRSlice;
		m_IDRSlice = NULL;
	}

#if TRACE_CONFIG_LOGOUT
	g_traceFile.close();
#endif

#if TRACE_CONFIG_TEMP_LOG
	g_tempFile.close();
#endif
}

void CStreamFile::file_info()
{

}

void CStreamFile::file_error(int idx)
{
	switch (idx)
	{
	case 0:
		wcout << L"Error: opening input file failed." << endl;
		break;
	case 1:
		wcout << L"Error: opening output trace file failed." << endl;
		break;
	default:
		break;
	}
}

int CStreamFile::Parse_h264_bitstream()
{
	int ret = 0;
	UINT8 nalType = 0;
	int sliceType = -1;
	UINT32 sliceIdx = 0;
	if (m_nalVec.size())
	{
		UINT8 nalType = m_nalVec[0] & 0x1F;
		Dump_NAL_type(nalType);
		ebsp_to_sodb();
		CNALUnit nalUnit(&m_nalVec[1], m_nalVec.size() - 1, nalType);
		LOGE("Parse_h264_bitstream  nalType:  %d , :  %x %x %x %x %x \n",nalType,m_nalVec[0],m_nalVec[1],m_nalVec[2],m_nalVec[3],m_nalVec[4]);
		switch (nalType)
		{
			case 1:
				if (m_un_IdrSlice){
					delete  m_un_IdrSlice;
					m_un_IdrSlice = NULL;
				}
				m_un_IdrSlice = new CSliceStruct(nalUnit.Get_SODB(), m_sps, m_pps, nalType, sliceIdx++);
				sliceType = m_un_IdrSlice->Parse();
				break;
			case 5:
				// Parse IDR NAL...
				if (m_IDRSlice)
				{
					delete m_IDRSlice;
					m_IDRSlice = NULL;
				}
				m_IDRSlice = new CSliceStruct(nalUnit.Get_SODB(), m_sps, m_pps, nalType, sliceIdx++);
				sliceType = m_IDRSlice->Parse();
				break;
			case 7:
				// Parse SPS NAL...
				if (m_sps)
				{
					delete m_sps;// new SPS detected, delete old one..
					m_sps = NULL;
				}
				m_sps = new CSeqParamSet;
				nalUnit.Parse_as_seq_param_set(m_sps);
				m_sps->Dump_sps_info();
//				Extract_single_nal_unit("SPS_NAL.bin", &m_nalVec[0], m_nalVec.size());
				break;
			case 8:
				// Parse PPS NAL...
				if (m_pps)
				{
					delete m_pps;
					m_pps = NULL;
				}
				m_pps = new CPicParamSet;
				nalUnit.Parse_as_pic_param_set(m_pps);
				m_pps->Dump_pps_info();
				break;
			default:
				break;
		}
	}
	return sliceType;
}

void CStreamFile::Dump_NAL_type(UINT8 nalType)
{
#if TRACE_CONFIG_CONSOLE
	wcout << L"NAL Unit Type: " << nalType << endl;
#endif

#if TRACE_CONFIG_LOGOUT
	g_traceFile << "NAL Unit Type: " << to_string(nalType) << endl;
#endif
}

int CStreamFile::find_nal_prefix()
{
	UINT8 prefix[3] = { 0 };
	UINT8 fileByte;


	m_nalVec.clear();

	int pos = 0, getPrefix = 0;
/*	for (int idx = 0; idx < 3; idx++)
	{
		prefix[idx] = getc(m_inputFile);
		m_nalVec.push_back(prefix[idx]);
	}

	while (!feof(m_inputFile))
	{
		if ((prefix[pos % 3] == 0) && (prefix[(pos + 1) % 3] == 0) && (prefix[(pos + 2) % 3] == 1))
		{
			//0x 00 00 01 found
			getPrefix = 1;
			m_nalVec.pop_back();
			m_nalVec.pop_back();
			m_nalVec.pop_back();
			break;
		}
		else if ((prefix[pos % 3] == 0) && (prefix[(pos + 1) % 3] == 0) && (prefix[(pos + 2) % 3] == 0))
		{
			if (1 == getc(m_inputFile))
			{
				//0x 00 00 00 01 found
				getPrefix = 2;
				m_nalVec.pop_back();
				m_nalVec.pop_back();
				m_nalVec.pop_back();
				break;
			}
		}
		else
		{
			fileByte = getc(m_inputFile);
			prefix[(pos++) % 3] = fileByte;
			m_nalVec.push_back(fileByte);
		}
	}*/

	return getPrefix;
}

void CStreamFile::ebsp_to_sodb()
{
	if (m_nalVec.size() < 3)
	{
		return;
	}

	for (vector<UINT8>::iterator itor = m_nalVec.begin() + 2; itor != m_nalVec.end(); )
	{
		if ((3 == *itor) && (0 == *(itor-1)) && (0 == *(itor-2)))
		{
			vector<UINT8>::iterator temp = m_nalVec.erase(itor);
			itor = temp;
		}
		else
		{
			itor++;
		}
	}
}


