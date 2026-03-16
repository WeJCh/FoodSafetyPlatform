package com.mortal.complaint.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mortal.complaint.domain.entity.ComplaintHandle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 投诉处理Mapper
 */
@Mapper
public interface ComplaintHandleMapper extends BaseMapper<ComplaintHandle> {
}
