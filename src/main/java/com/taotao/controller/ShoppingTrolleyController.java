package com.taotao.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.taotao.dto.Result;
import com.taotao.entity.ShoppingTrolley;
import com.taotao.service.ShoppingTrolleyService;
import com.taotao.vo.ShoppingTrolleyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author YuLong
 * Date: 2022/11/21 14:15
 */
@Slf4j
@RestController
@RequestMapping("/trolley")
public class ShoppingTrolleyController {

    @Resource
    private ShoppingTrolleyService trolleyService;

    @GetMapping("/show")
    public Result searchTrolley(Long userId) {
        log.info("查询用户购物车。。。");
        ShoppingTrolley trolley = trolleyService.queryTrolley(userId);
        // 取出trolley中的所有商品（String型）
        String goodsJson = trolley.getGoods();
        // 新建购物车视图对象
        ShoppingTrolleyVO trolleyVO = new ShoppingTrolleyVO();
        // 将商品 String型转为 JSON型
        JSON parse = JSONUtil.parse(goodsJson);
        // 购物车视图对象 set 商品JSON
        trolleyVO.setGoods(parse);
        // 购物车视图对象 copy 购物车对象，并忽略 "trolleyId","userId","goods" 字段
        BeanUtil.copyProperties(trolley, trolleyVO, "trolleyId","userId","goods");
        // 返回购物车视图对象
        return Result.success(trolleyVO);
    }
}
