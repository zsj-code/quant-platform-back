package com.quant.platform.business.kline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.kline.entity.KlineSyncLogEntity;
import org.apache.ibatis.annotations.Param;

public interface KlineSyncLogMapper extends BaseMapper<KlineSyncLogEntity> {

    /**
     * 唯一键冲突时忽略，避免并发任务重复写导致任务失败。
     */
    void insertIgnore(@Param("e") KlineSyncLogEntity e);
}
