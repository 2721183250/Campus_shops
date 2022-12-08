package com.taotao.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.taotao.dto.Result;
import com.taotao.dto.UserLoginFormDTO;
import com.taotao.entity.User;

import java.util.List;

/**
* @author YuLong
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2022-11-21 14:08:09
*/
public interface UserService extends IService<User> {

    /**
     * 发送短信验证码
     * @param phone 电话号码
     * @return  Result
     */
    Result sendCode(String phone);

    /**
     * 用户登录功能
     * @param userLoginFormDTO 用户登录信息DTO
     * @return  Result
     */
    Result login(UserLoginFormDTO userLoginFormDTO);

    /**
     * 用户重置手机号码
     * @param oldPhone 原手机号
     * @param phone 新手机号
     * @param code 验证码
     * @return  Result
     */
    Result resetPhone(String oldPhone, String phone, String code);

    /**
     * 查询商家热力榜的单个商家所属个人信息
     * @param merchantId 商家id
     * @return  Resul
     */
    User querySingleHotRankOfUserInfo(Long merchantId);

    /**
     * 查询商家热力榜的所有商家所属个人信息
     * @param merchantIds id列表
     * @return  Result
     */
    List<User> queryListHotRankOfUserInfo(List<Long> merchantIds);

    /**
     * 更新用户个人信息
     * @param user 用户信息
     */
    void modifyUserInfo(User user);

    /**
     * 用户实名认证
     * @param user 用户实名信息
     */
    void modifyUserRealName(User user);

    /**
     * 根据 userId查询数据库中密码
     * @param userId 用户账号
     * @return 数据库中的密码
     */
    String queryPasswordOfDatabase(Long userId);
}
