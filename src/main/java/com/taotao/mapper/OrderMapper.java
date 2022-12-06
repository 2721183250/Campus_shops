package com.taotao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taotao.entity.Order;
import com.taotao.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author YuLong
* @description 针对表【order(订单表)】的数据库操作Mapper
* @createDate 2022-11-21 14:07:49
* @Entity taotao.entity.ShopOrder
*/
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 创建订单
     * @param orderId 订单序列号
     * @param userId  用户账号
     * @param commodityId 商品序列号
     */
    void createOrder(@Param("orderId") String orderId, @Param("userId") Long userId, @Param("commodityId") Long commodityId);

    /**
     * 查看订单
     * @param userId 用户账号
     * @return 订单 list
     */
    List<OrderVO> selectAllOrder(Long userId);
}




