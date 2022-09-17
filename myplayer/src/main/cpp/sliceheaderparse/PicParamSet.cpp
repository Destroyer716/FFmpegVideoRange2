
#include "PicParamSet.h"

using namespace std;

CPicParamSet::CPicParamSet()
{
	memset(this, 0, sizeof(CPicParamSet));
}


CPicParamSet::~CPicParamSet()
{
}

void CPicParamSet::Set_pps_id(UINT8 ppsID)
{
	m_pps_id = ppsID;
}

void CPicParamSet::Set_sps_id(UINT8 spsID)
{
	m_sps_id = spsID;
}

void CPicParamSet::Set_num_slice_groups(UINT8 num_slice_grops)
{
	m_num_slice_groups = num_slice_grops;
}

void CPicParamSet::Set_num_ref_idx(UINT8 l0, UINT8 l1)
{
	m_num_ref_idx_l0_default_active = l0;
	m_num_ref_idx_l1_default_active = l1;
}

void CPicParamSet::Set_weighted_bipred_idc(UINT8 weighted_bipred_idc)
{
	m_weighted_bipred_idc = weighted_bipred_idc;
}

void CPicParamSet::Set_pic_init_qp(int pic_init_qp)
{
	m_pic_init_qp = pic_init_qp;
}

void CPicParamSet::Set_pic_init_qs(int  pic_init_qs)
{
	m_pic_init_qs = pic_init_qs;
}

void CPicParamSet::Set_chroma_qp_index_offset(int chroma_qp_index_offset)
{
	m_chroma_qp_index_offset = chroma_qp_index_offset;
}

void CPicParamSet::Set_multiple_flags(UINT16 flags)
{
	m_entropy_coding_flag = flags & 1;
	m_bottom_field_pic_order_in_frame_present_flag = flags & (1 << 1);
	m_weighted_pred_flag = flags & (1 << 2);
	m_deblocking_filter_control_present_flag = flags & (1 << 3);
	m_constrained_intra_pred_flag = flags & (1 << 4);
	m_redundant_pic_cnt_present_flag = flags & (1 << 5);
}

void CPicParamSet::Dump_pps_info()
{
#if TRACE_CONFIG_PIC_PARAM_SET

#if TRACE_CONFIG_LOGOUT
	g_traceFile << "==========Picture Paramater Set==========" << endl;
	g_traceFile << "pic_parameter_set_id: " << to_string(m_pps_id) << endl;
	g_traceFile << "seq_parameter_set_id: " << to_string(m_sps_id) << endl;
	g_traceFile << "entropy_coding_mode_flag: " << to_string(m_entropy_coding_flag) << endl;
	g_traceFile << "bottom_field_pic_order_in_frame_present_flag: " << to_string(m_bottom_field_pic_order_in_frame_present_flag) << endl;
	g_traceFile << "num_slice_groups: " << to_string(m_num_slice_groups) << endl;
	if (m_num_slice_groups == 1)
	{
		g_traceFile << "num_ref_idx_l0_default_active: " << to_string(m_num_ref_idx_l0_default_active) << endl;
		g_traceFile << "num_ref_idx_l1_default_active: " << to_string(m_num_ref_idx_l1_default_active) << endl;
		g_traceFile << "weighted_pred_flag: " << to_string(m_weighted_pred_flag) << endl;
		g_traceFile << "weighted_bipred_idc: " << to_string(m_weighted_bipred_idc) <<endl;
		g_traceFile << "pic_init_qp: " << to_string(m_pic_init_qp) << endl;
		g_traceFile << "pic_init_qs: " << to_string(m_pic_init_qs) << endl;
		g_traceFile << "chroma_qp_index_offset :" << to_string(m_chroma_qp_index_offset) << endl;
		g_traceFile << "deblocking_filter_control_present_flag: " << to_string(m_deblocking_filter_control_present_flag) << endl;
		g_traceFile << "constrained_intra_pred_flag: " << to_string(m_constrained_intra_pred_flag) << endl;
		g_traceFile << "redundant_pic_cnt_present_flag: " << to_string(m_redundant_pic_cnt_present_flag) << endl;
	}
	g_traceFile << "==========================================" << endl;
	g_traceFile.flush();
#endif

#endif
}

bool CPicParamSet::Get_bottom_field_pic_order_in_frame_present_flag()
{
	return m_bottom_field_pic_order_in_frame_present_flag;
}

bool CPicParamSet::Get_transform_8x8_mode_flag()
{
	return m_transform_8x8_mode_flag;
}

bool CPicParamSet::Get_entropy_coding_flag()
{
	return m_entropy_coding_flag;
}

int CPicParamSet::Get_pic_init_qp()
{
	return m_pic_init_qp;
}

bool CPicParamSet::Get_deblocking_filter_control_present_flag()
{
	return m_deblocking_filter_control_present_flag;
}
