package com.mortal.regulation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mortal.regulation.entity.Complaint;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {
}
