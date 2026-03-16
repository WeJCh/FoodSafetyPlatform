package com.mortal.complaint.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mortal.complaint.domain.entity.Complaint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投诉Mapper
 */
@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {
}
