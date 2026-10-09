package com.campusknowledgeqa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusknowledgeqa.entity.QaMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaMessageMapper extends BaseMapper<QaMessage> {
}
