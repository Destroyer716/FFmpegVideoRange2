#ifndef _SEQ_PARAM_SET_H_
#define _SEQ_PARAM_SET_H_
#include "stdafx.h"
class CSeqParamSet
{
public:
	CSeqParamSet();
	~CSeqParamSet();

	//Open API..
	void  Dump_sps_info();

	void  Set_profile_level_idc(UINT8 profile, UINT8 level);
	void  Set_sps_id(UINT8 spsID);
	void  Set_chroma_format_idc(UINT8 chromaFormatIdc);
	void  Set_bit_depth(UINT8 bit_depth_luma, UINT8 bit_depth_chroma);

	void  Set_log2_max_frame_num(UINT32 log2maxFrameNum);
	void  Set_poc_type(UINT8 pocType);
	void  Set_log2_max_poc_cnt(UINT32 log2maxPocCnt);
	void  Set_max_num_ref_frames(UINT32 maxRefFrames);
	void  Set_sps_multiple_flags(UINT32 flags);
	void  Set_pic_reslution_in_mbs(UINT16 widthInMBs, UINT16 heightInMapUnits);
	void  Set_frame_crop_offset(UINT32 offsets[4]);

	bool  Get_separate_colour_plane_flag();
	UINT32 Get_log2_max_frame_num();
	UINT32 Get_log2_max_poc_cnt();
	bool Get_frame_mbs_only_flag();
	UINT8 Get_poc_type();
	UINT16 Get_pic_width_in_mbs();
	UINT16 Get_pic_height_in_mbs();

private:
	UINT8  m_profile_idc;
	UINT8  m_level_idc;
	UINT8  m_sps_id;

	// for uncommon profile...
	UINT8  m_chroma_format_idc;
	bool   m_separate_colour_plane_flag;
	UINT8  m_bit_depth_luma;
	UINT8  m_bit_depth_chroma;
	bool   m_qpprime_y_zero_transform_bypass_flag;
	bool   m_seq_scaling_matrix_present_flag;
	// ...for uncommon profile

	UINT32 m_log2_max_frame_num;
	UINT8  m_poc_type;
	UINT32 m_log2_max_poc_cnt;
	UINT32 m_log2_max_num_ref_frames;
	bool   m_gaps_in_frame_num_value_allowed_flag;
	UINT16 m_pic_width_in_mbs;
	UINT16 m_pic_height_in_map_units;
	UINT16 m_pic_height_in_mbs;	// not defined in spec, derived...
	bool   m_frame_mbs_only_flag;
	bool   m_mb_adaptive_frame_field_flag;
	bool   m_direct_8x8_inference_flag;
	bool   m_frame_cropping_flag;
	UINT32 m_frame_crop_offset[4];
	bool   m_vui_parameters_present_flag;

	UINT32 m_reserved;
};

#endif

