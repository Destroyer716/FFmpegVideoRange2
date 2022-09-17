
#include "NALUnit.h"
#include "SeqParamSet.h"
#include "PicParamSet.h"

CNALUnit::CNALUnit(UINT8	*pSODB, UINT32	SODBLength, UINT8 nalType)
{
	m_pSODB = pSODB;
	m_SODBLength = SODBLength;

	m_nalType = nalType;
}


CNALUnit::~CNALUnit()
{
}

int CNALUnit::Parse_as_seq_param_set(CSeqParamSet *sps)
{
	UINT8  profile_idc = 0;
	UINT8  level_idc = 0;
	UINT8  sps_id = 0;

	UINT8  chroma_format_idc = 0;
	bool   separate_colour_plane_flag = 0;
	UINT8  bit_depth_luma = 0;
	UINT8  bit_depth_chroma = 0;
	bool   qpprime_y_zero_transform_bypass_flag = 0;
	bool   seq_scaling_matrix_present_flag = 0;

	UINT32 log2_max_frame_num = 0;
	UINT8  poc_type = 0;
	UINT32 log2_max_poc_cnt = 0;
	UINT32 max_num_ref_frames = 0;
	bool   gaps_in_frame_num_value_allowed_flag = 0;
	UINT16 pic_width_in_mbs = 0;
	UINT16 pic_height_in_map_units = 0;
	UINT16 pic_height_in_mbs = 0;	// not defined in spec, derived...
	bool   frame_mbs_only_flag = 0;
	bool   mb_adaptive_frame_field_flag = 0;
	bool   direct_8x8_inference_flag = 0;
	bool   frame_cropping_flag = 0;
	UINT32 frame_crop_offset[4] = {0};
	bool   vui_parameters_present_flag = 0;

	UINT8  bitPosition = 0; 
	UINT32 bypePosition = 3;
	UINT32 flags = 0;

	profile_idc = m_pSODB[0];
	level_idc = m_pSODB[2];
	sps_id = Get_uev_code_num(m_pSODB, bypePosition, bitPosition);

	if ( profile_idc == 100 || profile_idc == 110 || profile_idc == 122 || profile_idc == 244 || profile_idc == 44 ||
		 profile_idc == 83 || profile_idc == 86 || profile_idc == 118 || profile_idc == 128) 
	{
		chroma_format_idc = Get_uev_code_num(m_pSODB, bypePosition, bitPosition);
		if (chroma_format_idc == 3)
		{
			separate_colour_plane_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
			flags |= (separate_colour_plane_flag << 21);
		}
		bit_depth_luma = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 8;
		bit_depth_chroma = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 8;
		qpprime_y_zero_transform_bypass_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
		flags |= (qpprime_y_zero_transform_bypass_flag << 20);

		seq_scaling_matrix_present_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
		flags |= (seq_scaling_matrix_present_flag << 19);
		if (seq_scaling_matrix_present_flag)
		{
			return kPARSING_SPS_ERROR_SCALING_MATRIX;
		}
	}
	log2_max_frame_num = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 4;
	poc_type = Get_uev_code_num(m_pSODB, bypePosition, bitPosition);
	if (0 == poc_type)
	{
		log2_max_poc_cnt = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 4;
	} 
	else
	{
		return kPARSING_SPS_ERROR_UNSUPPORTED_POC_TYPE;
	}
	max_num_ref_frames = Get_uev_code_num(m_pSODB, bypePosition, bitPosition);
	gaps_in_frame_num_value_allowed_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= (gaps_in_frame_num_value_allowed_flag << 5);

	pic_width_in_mbs = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 1;
	pic_height_in_map_units = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 1;
	frame_mbs_only_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= (frame_mbs_only_flag << 4);
	if (!frame_mbs_only_flag)
	{
		mb_adaptive_frame_field_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
		flags |= (mb_adaptive_frame_field_flag << 3);
	}

	direct_8x8_inference_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= (direct_8x8_inference_flag << 2);
	frame_cropping_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= (direct_8x8_inference_flag << 1);
	if (frame_cropping_flag)
	{
		for (int idx = 0; idx < 4; idx++)
		{
			frame_crop_offset[idx] = Get_uev_code_num(m_pSODB, bypePosition, bitPosition);
		}

	}
	vui_parameters_present_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= vui_parameters_present_flag;

	sps->Set_profile_level_idc(profile_idc, level_idc);
	sps->Set_sps_id(sps_id);
	sps->Set_chroma_format_idc(chroma_format_idc);
	sps->Set_bit_depth(bit_depth_luma, bit_depth_chroma);
	sps->Set_log2_max_frame_num(log2_max_frame_num);
	sps->Set_poc_type(poc_type);
	sps->Set_log2_max_poc_cnt(log2_max_poc_cnt);
	sps->Set_max_num_ref_frames(max_num_ref_frames);
	sps->Set_sps_multiple_flags(flags);
	sps->Set_pic_reslution_in_mbs(pic_width_in_mbs, pic_height_in_map_units);
	if (frame_cropping_flag)
	{
		sps->Set_frame_crop_offset(frame_crop_offset);
	}

	return kPARSING_ERROR_NO_ERROR;
}

int CNALUnit::Parse_as_pic_param_set(CPicParamSet *pps)
{
	UINT8  pps_id = 0;
	UINT8  sps_id = 0;
	bool   entropy_coding_flag = 0;
	bool   bottom_field_pic_order_in_frame_present_flag = 0;
	UINT8  num_slice_groups = 0;
	UINT8  num_ref_idx_l0_default_active = 0;
	UINT8  num_ref_idx_l1_default_active = 0;
	bool   weighted_pred_flag = 0;
	UINT8  weighted_bipred_idc = 0;
	int    pic_init_qp = 0;
	int    pic_init_qs = 0;
	int    chroma_qp_index_offset = 0;
	bool   deblocking_filter_control_present_flag = 0;
	bool   constrained_intra_pred_flag = 0;
	bool   redundant_pic_cnt_present_flag = 0;

	UINT8  bitPosition = 0;
	UINT32 bypePosition = 0;
	UINT16 flags = 0;

	pps_id = Get_uev_code_num(m_pSODB, bypePosition, bitPosition);
	sps_id = Get_uev_code_num(m_pSODB, bypePosition, bitPosition);

	entropy_coding_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= entropy_coding_flag;
	bottom_field_pic_order_in_frame_present_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= bottom_field_pic_order_in_frame_present_flag << 1;

	num_slice_groups = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 1;

	if (num_slice_groups > 1)
	{
		return kPARSING_PPS_ERROR_UNSUPPORTED_NUM_SLICE_GROUP;
	}

	num_ref_idx_l0_default_active = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 1;
	num_ref_idx_l1_default_active = Get_uev_code_num(m_pSODB, bypePosition, bitPosition) + 1;

	weighted_pred_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= weighted_pred_flag << 2;
	weighted_bipred_idc = Get_uint_code_num(m_pSODB, bypePosition, bitPosition, 2);
//	weighted_bipred_idc = Get_bit_at_position(m_pSODB, bypePosition, bitPosition) << 1 + Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	
	pic_init_qp = Get_sev_code_num(m_pSODB, bypePosition, bitPosition) + 26;
	pic_init_qs = Get_sev_code_num(m_pSODB, bypePosition, bitPosition) + 26;
	chroma_qp_index_offset = Get_sev_code_num(m_pSODB, bypePosition, bitPosition);
	
	deblocking_filter_control_present_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= deblocking_filter_control_present_flag << 3;
	constrained_intra_pred_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= constrained_intra_pred_flag << 4;
	redundant_pic_cnt_present_flag = Get_bit_at_position(m_pSODB, bypePosition, bitPosition);
	flags |= redundant_pic_cnt_present_flag << 5;

	pps->Set_pps_id(pps_id);
	pps->Set_sps_id(sps_id);
	pps->Set_num_slice_groups(num_slice_groups);
	pps->Set_num_ref_idx(num_ref_idx_l0_default_active, num_ref_idx_l1_default_active);
	pps->Set_weighted_bipred_idc(weighted_bipred_idc);
	pps->Set_pic_init_qp(pic_init_qp);
	pps->Set_pic_init_qs(pic_init_qs);
	pps->Set_chroma_qp_index_offset(chroma_qp_index_offset);
	pps->Set_multiple_flags(flags);

	return kPARSING_ERROR_NO_ERROR;
}

UINT8* CNALUnit::Get_SODB()
{
	return m_pSODB;
}
