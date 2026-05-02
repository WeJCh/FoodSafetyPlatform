package com.mortal.regulation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mortal.regulation.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
