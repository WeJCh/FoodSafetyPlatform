package com.mortal.regulation.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mortal.regulation.operation.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
