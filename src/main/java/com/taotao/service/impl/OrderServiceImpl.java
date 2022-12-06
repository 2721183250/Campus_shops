package com.taotao.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taotao.dto.OrderDTO;
import com.taotao.entity.Order;
import com.taotao.mapper.OrderMapper;
import com.taotao.service.OrderService;
import com.taotao.vo.OrderVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
* @author YuLong
* @description 针对表【shop_order(订单表)】的数据库操作Service实现
* @createDate 2022-11-21 14:07:49
*/
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
    implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    /**
     * 保存订单
     * @param orderDTO 订单DTO
     */
    @Override
    public void saveOrder(OrderDTO orderDTO) {
        UUID uuid = UUID.randomUUID();
        String orderId = String.valueOf(uuid);
        Long userId = orderDTO.getUserId();
        Long commodityId = orderDTO.getCommodityId();
        orderMapper.createOrder(orderId, userId, commodityId);
    }

    /**
     * 查看订单
     * @param userId 用户账号
     * @return 订单 list
     */
    @Override
    public List<OrderVO> observeOrder(Long userId) {
        return orderMapper.selectAllOrder(userId);
    }
}




