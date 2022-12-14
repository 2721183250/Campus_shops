package com.taotao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单表
 * @author YuLong
 * @createDate 2022-11-21 14:06:42
 * @TableName shop_order
 */
@TableName(value ="order")
@Data
public class Order implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "order_id")
    private String orderId;

    /**
     * 用户账号
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 商品序列号
     */
    @TableField(value = "commodity_id")
    private Long commodityId;

    /**
     * 购买商品数量
     */
    @TableField(value = "buy_quantity")
    private Long buyQuantity;

    /**
     * 订单花费
     */
    @TableField(value = "order_cost")
    private Long orderCost;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;

}