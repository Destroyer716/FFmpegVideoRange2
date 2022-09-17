#ifndef _SLICE_HEADER_H_
#define _SLICE_HEADER_H_
#include "stdafx.h"
#include "../log.h"
class CSeqParamSet;
class CPicParamSet;

typedef enum 
{
	SLICE_TYPE_P = 0,
	SLICE_TYPE_B,
	SLICE_TYPE_I,
	SLICE_TYPE_SP,
	SLICE_TYPE_SI
} SliceType;

typedef struct DecRefPicMarking
{
	bool no_output_of_prior_pics_flag;
	bool long_term_reference_flag;
} DecRefPicMarking;

class CSliceHeader
{
public:
	CSliceHeader(UINT8	*pSODB, CSeqParamSet *sps, CPicParamSet *pps, UINT8	nalType);
	~CSliceHeader();

	UINT32 Parse_slice_header();
	void  Dump_slice_header_info();

	UINT8 Get_slice_type();
	int  Get_slice_qp_delta();

	int	   m_disable_deblocking_filter_idc;
	int    m_slice_alpha_c0_offset;
	int    m_slice_beta_offset;
private:
	CSeqParamSet *m_sps_active;
	CPicParamSet *m_pps_active;
	UINT8	*m_pSODB;
	UINT8   m_nalType;

	UINT16 m_first_mb_in_slice;
	UINT8  m_slice_type;
	UINT8  m_pps_id;
	UINT8  m_colour_plane_id;
	UINT32 m_frame_num;
	bool   m_field_pic_flag;
	bool   m_bottom_field_flag;
	UINT16 m_idr_pic_id;
	UINT32 m_poc;
	int	   m_delta_poc_bottom;
	DecRefPicMarking m_dec_ref_pic_marking;
	int	   m_slice_qp_delta;
};

#endif