package com.quant.platform.business.kline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.kline.entity.KlineBarEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KlineBarMapper extends BaseMapper<KlineBarEntity> {

    /**
     * 单次 SQL 多行插入（需在业务侧控制单批条数，避免超过服务端 max_allowed_packet）。
     */
    void insertBatch(@Param("list") List<KlineBarEntity> list);

}
