
#include "SeqParamSet.h"

using namespace std;

CSeqParamSet::CSeqParamSet()
{
	memset(this, 0, sizeof(CSeqParamSet));
	m_chroma_format_idc = 1;
}


CSeqParamSet::~CSeqParamSet()
{
}

void CSeqParamSet::Dump_sps_info()
{
#if TRACE_CONFIG_SEQ_PARAM_SET

#if TRACE_CONFIG_LOGOUT
	g_traceFile << "==========Sequence Paramater Set==========" << endl;
	g_traceFile << "Profile: " << to_string(m_profile_idc) << endl;
	g_traceFile << "Level: " << to_string(m_level_idc) << endl;
	g_traceFile << "SPS ID: " << to_string(m_sps_id) << endl;
	if (m_profile_idc == 100 || m_profile_idc == 110 || m_profile_idc == 122 || m_profile_idc == 244 || m_profile_idc == 44 ||
		m_profile_idc == 83 || m_profile_idc == 86 || m_profile_idc == 118 || m_profile_idc == 128)
	{
		g_traceFile << "chroma_format_idc: " << to_string(m_chroma_format_idc) << endl;
		if (m_chroma_format_idc == 3)
		{
			g_traceFile << "separate_colour_plane_flag: " << to_string(m_separate_colour_plane_flag) << endl;
		}
		g_traceFile << "bit_depth_luma: " << to_string(m_bit_depth_luma) << endl;
		g_traceFile << "bit_depth_chroma: " << to_string(m_bit_depth_chroma) << endl;
		g_traceFile << "qpprime_y_zero_transform_bypass_flag: " << to_string(m_qpprime_y_zero_transform_bypass_flag) << endl;
		g_traceFile << "seq_scaling_matrix_present_flag: " << to_string(m_seq_scaling_matrix_present_flag) << endl;
	}
	g_traceFile << "log2_max_frame_num: " << to_string(m_log2_max_frame_num) << endl;
	g_traceFile << "pic_order_cnt_type: " << to_string(m_poc_type) << endl;
	if (m_poc_type == 0)
	{
		g_traceFile << "log2_max_poc_cnt: " << to_string(m_log2_max_poc_cnt) << endl;
	}
	g_traceFile << "log2_max_num_ref_frames: " << to_string(m_log2_max_num_ref_frames) << endl;
	g_traceFile << "gaps_in_frame_num_value_allowed_flag: " << to_string(m_gaps_in_frame_num_value_allowed_flag) << endl;
	g_traceFile << "pic_width_in_mbs_minus1: " << to_string(m_pic_width_in_mbs) << endl;
	g_traceFile << "pic_height_in_map_units_minus1: " << to_string(m_pic_height_in_map_units) << endl;
	g_traceFile << "(picture resolution: " << to_string(m_pic_width_in_mbs * 16) << " x " << to_string(m_pic_height_in_mbs * 16) << ")" << endl;
	g_traceFile << "frame_mbs_only_flag: " << to_string(m_frame_mbs_only_flag) << endl;
	if (!m_frame_mbs_only_flag)
	{
		g_traceFile << "mb_adaptive_frame_field_flag: " << to_string(m_mb_adaptive_frame_field_flag) << endl;
	}
	g_traceFile << "direct_8x8_inference_flag: " << to_string(m_direct_8x8_inference_flag) << endl;
	g_traceFile << "frame_cropping_flag: " << to_string(m_frame_cropping_flag) << endl;
	if (m_frame_cropping_flag)
	{
		g_traceFile << "frame_crop_left_offset: " << to_string(m_frame_crop_offset[0]) << endl;
		g_traceFile << "frame_crop_right_offset: " << to_string(m_frame_crop_offset[1]) << endl;
		g_traceFile << "frame_crop_top_offset: " << to_string(m_frame_crop_offset[2]) << endl;
		g_traceFile << "frame_crop_bottum_offset: " << to_string(m_frame_crop_offset[3]) << endl;
	}
	g_traceFile << "vui_parameters_present_flag: " << to_string(m_vui_parameters_present_flag) << endl;
	g_traceFile << "==========================================" << endl;
	g_traceFile.flush();
#endif

#endif
}

void CSeqParamSet::Set_profile_level_idc(UINT8 profile, UINT8 level)
{
	m_profile_idc = profile;
	m_level_idc = level;
}

void CSeqParamSet::Set_sps_id(UINT8 sps_id)
{
	m_sps_id = sps_id;
}

void CSeqParamSet::Set_chroma_format_idc(UINT8 chromaFormatIdc)
{
	m_chroma_format_idc = chromaFormatIdc;
}

void CSeqParamSet::Set_bit_depth(UINT8 bit_depth_luma, UINT8 bit_depth_chroma)
{
	m_bit_depth_luma = bit_depth_luma;
	m_bit_depth_chroma = bit_depth_chroma;
}

void CSeqParamSet::Set_log2_max_frame_num(UINT32 log2maxFrameNum)
{
	m_log2_max_frame_num = log2maxFrameNum;
}

void CSeqParamSet::Set_poc_type(UINT8 pocType)
{
	m_poc_type = pocType;
}

void CSeqParamSet::Set_log2_max_poc_cnt(UINT32 log2maxPocCnt)
{
	m_log2_max_poc_cnt = log2maxPocCnt;
}

void CSeqParamSet::Set_max_num_ref_frames(UINT32 maxRefFrames)
{
	m_log2_max_num_ref_frames = maxRefFrames;
}

void CSeqParamSet::Set_sps_multiple_flags(UINT32 flags)
{
	m_separate_colour_plane_flag = flags & (1 << 21);
	m_qpprime_y_zero_transform_bypass_flag = flags & (1 << 20);
	m_seq_scaling_matrix_present_flag = flags & (1 << 19);

	m_gaps_in_frame_num_value_allowed_flag = flags & (1 << 5);
	m_frame_mbs_only_flag = flags & (1 << 4);
	m_mb_adaptive_frame_field_flag = flags & (1 << 3);
	m_direct_8x8_inference_flag = flags & (1 << 2);
	m_frame_cropping_flag = flags & (1 << 1);
	m_vui_parameters_present_flag = flags & 1;
}

void CSeqParamSet::Set_pic_reslution_in_mbs(UINT16 widthInMBs, UINT16 heightInMapUnits)
{
	m_pic_width_in_mbs = widthInMBs;
	m_pic_height_in_map_units = heightInMapUnits;
	m_pic_height_in_mbs = m_frame_mbs_only_flag ? m_pic_height_in_map_units : 2 * m_pic_height_in_map_units;
}

void CSeqParamSet::Set_frame_crop_offset(UINT32 offsets[4])
{
	for (int idx = 0; idx < 4; idx++)
	{
		m_frame_crop_offset[idx] = offsets[idx];
	}
}

bool CSeqParamSet::Get_separate_colour_plane_flag()
{
	return m_separate_colour_plane_flag;
}

UINT32 CSeqParamSet::Get_log2_max_frame_num()
{
	return m_log2_max_frame_num;
}

UINT32 CSeqParamSet::Get_log2_max_poc_cnt()
{
	return m_log2_max_poc_cnt;
}

bool CSeqParamSet::Get_frame_mbs_only_flag()
{
	return m_frame_mbs_only_flag;
}

UINT8 CSeqParamSet::Get_poc_type()
{
	return m_poc_type;
}

UINT16 CSeqParamSet::Get_pic_width_in_mbs()
{
	return m_pic_width_in_mbs;
}

UINT16 CSeqParamSet::Get_pic_height_in_mbs()
{
	return m_pic_height_in_mbs;
}
