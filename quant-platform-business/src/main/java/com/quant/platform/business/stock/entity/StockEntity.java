package com.quant.platform.business.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.quant.platform.business.stock.enums.StockDelistStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stocks")
public class StockEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("is_delisted")
    private StockDelistStatus isDelisted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
