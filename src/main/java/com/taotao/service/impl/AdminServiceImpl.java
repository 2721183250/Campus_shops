package com.taotao.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taotao.dto.AdminDTO;
import com.taotao.dto.AdminLoginFormDTO;
import com.taotao.dto.PageData;
import com.taotao.dto.Result;
import com.taotao.entity.Admin;
import com.taotao.entity.Apply;
import com.taotao.entity.Merchant;
import com.taotao.entity.User;
import com.taotao.mapper.AdminMapper;
import com.taotao.service.AdminService;
import com.taotao.service.ApplyService;
import com.taotao.service.MerchantService;
import com.taotao.service.UserService;
import com.taotao.util.PasswordEncoder;
import com.taotao.util.RegexUtils;
import com.taotao.vo.MerchantVO;
import com.taotao.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.taotao.util.RedisConstants.*;
import static com.taotao.util.SystemConstants.*;

/**
* @author YuLong
* @description 针对表【admin(管理员表)】的数据库操作Service实现
* @createDate 2022-11-21 14:06:42
*/
@Slf4j
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private MerchantService merchantService;

    @Resource
    private UserService userService;

    @Resource
    private ApplyService applyService;

    /**
     * 发送短信验证码
     * @param phone 手机号码
     * @return 通用返回结果
     */
    @Override
    public Result<String> sendCodeOfTel(String phone) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号码格式不正确");
        }
        // 3.符合，生成验证码
        String authCode = RandomUtil.randomNumbers(AUTH_CODE_LENGTH);
        // 4.保存验证码到 redis
        long authCodeTime = ADMIN_CODE_TTL + RandomUtil.randomLong(ADMIN_CODE_TTL);
        String codeKey = ADMIN_CODE_KEY + phone;
        stringRedisTemplate.opsForValue().set(codeKey, authCode, authCodeTime, TimeUnit.MINUTES);
        log.info("管理员点击发送验证码时获取到的手机号码： {}", phone);
        // 5.发送验证码
        log.debug("发送短信验证码成功，验证码：{}", authCode);
        // 返回success
        return Result.success();
    }

    /**
     * 管理员登录处理流程
     * @param adminLoginFormDTO 管理员登录DTO
     * @return 通用返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> loginProcessingFlow(AdminLoginFormDTO adminLoginFormDTO) {
        // 1.检验手机号
        String phone = adminLoginFormDTO.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 2.判断密码是否为空，若为空则为验证码登录，反之则为密码登录
        String rawPassword = adminLoginFormDTO.getPassword();
        // flag用来判断是否在密码登录时查询了数据库 true代表查询了数据库
        boolean flag = false;
        Admin admin = null;
        if (rawPassword == null || rawPassword.isEmpty() ) {
            // 2.1.验证码登录
            // 2.1.1.在 redis中获取管理员验证码并校验
            String adminLoginKey = ADMIN_CODE_KEY + phone;
            String cacheCode = stringRedisTemplate.opsForValue().get(adminLoginKey);
            String code = adminLoginFormDTO.getCode();
            if (cacheCode == null || !cacheCode.equals(code)) {
                // 2.1.2不一致，报错
                return Result.fail("验证码错误");
            }
        } else {
            // 2.2.密码登录
            // 2.2.1.先从redis中查询密码
            String adminLoginKey = ADMIN_TOKEN_KEY + adminLoginFormDTO.getId();
            String adminJson = stringRedisTemplate.opsForValue().get(adminLoginKey);
            AdminLoginFormDTO adLogin = JSONUtil.toBean(adminJson, AdminLoginFormDTO.class);
            String encodedPassword = adLogin.getPassword();
            // 2.2.2.判断该密码在redis中是否存在
            if (encodedPassword == null || encodedPassword.isEmpty()) {
                // 不存在，在数据库中根据手机继续查找密码
                admin = query().eq(AUTH_PHONE, phone).one();
                // 查不到，直接返回
                if (admin == null) {
                    return Result.fail("是否要新建管理员?");
                }
                encodedPassword = admin.getPassword();
                flag = true;
            }
            // 2.2.3.将原始密码进行 MD5加密后与从 redis或数据库中 取出的密码进行匹配 （在 matches方法中完成 MD5加密且匹配）
            if (!PasswordEncoder.matches(encodedPassword, rawPassword)) {
                // 密码不匹配，直接返回登录失败
                log.info("管理员登录失败 encodedPassword = {}, rawPassword = {}", encodedPassword, rawPassword);
                return Result.fail("登录失败，管理员账号或密码错误");
            }
        }
        // 3.一致，根据手机号查询管理员 select * from admin where phone = ?
        // 先判断是否 admin已在数据库中查询过 !flag 则没查过
        if (!flag) {
            admin = query().eq(AUTH_PHONE, phone).one();
        }
        // 4.判断 admin是否为空
        if (admin == null) {
            // 数据库中没有该管理员，需创建，此时发送消息到前端询问超管是否要新建用户，可避免误输入而创建不必要的新管理员的情况
            return Result.fail("是否要新建管理员?");
        }

        // 5.一致，保存管理员信息到 redis
        // 5.1.随机生成 token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 5.2.将 AdminDTO对象转为 Json存储
        AdminDTO adminDTO = BeanUtil.copyProperties(admin, AdminDTO.class);
        String adminJson = JSONUtil.toJsonStr(adminDTO);
        // 5.3.存储
        String tokenKey = ADMIN_TOKEN_KEY + token;
        stringRedisTemplate.opsForValue().set(tokenKey, adminJson);
        log.info("adminJson = {}", stringRedisTemplate.opsForValue().get(tokenKey));
        // 5.4.设置 token有效期
        long adminLoginTime = ADMIN_TOKEN_TTL + RandomUtil.randomLong(ADMIN_TOKEN_TTL) / 2;
        stringRedisTemplate.expire(tokenKey, adminLoginTime, TimeUnit.SECONDS);
        log.info("tempToken = {}", tokenKey);
        log.info("管理员登录成功");
        return Result.success(token);
    }
    /**
     * 管理员注册流程
     * @param adminLoginFormDTO 管理员登录DTO
     * @return 通用返回结果
     */
    @Override
    public Result<String> registerProcess(AdminLoginFormDTO adminLoginFormDTO) {
        log.info("adminLoginFormDTO = {}", adminLoginFormDTO);
        Integer isRoot = adminLoginFormDTO.getIsRoot();
        if (!IS_ROOT.equals(isRoot)) {
            return Result.fail("你不是尊贵的超级管理员，无法使用该权限创建普通管理员");
        }
        Long adminId = RandomUtil.randomLong(LONG_RANDOM_START, Long.MAX_VALUE);
        String rawPassword = adminLoginFormDTO.getPassword();
        String phone = adminLoginFormDTO.getPhone();
        // 原本需要判断电话号码是否已经被注册，若被注册则直接返回注册错误信息
        // 但需判断出该手机号未被注册时，才会出现注册弹窗页面，从而发送请求调用该方法
        // 这样做的好处是无需过多的查询数据库信息。
        // 1.若密码为空，则随机生成强密码，反之则为按意愿输入密码创建管理员
        if (rawPassword == null || rawPassword.isEmpty()) {
            rawPassword = initializePassword();
        }
        // 2.新建管理员
        Admin admin = new Admin();
        String encodedPassword = PasswordEncoder.encode(rawPassword);
        admin.setAdminId(adminId);
        admin.setPhone(phone);
        admin.setPassword(encodedPassword);
        // 3.将数据存入 数据库
        save(admin);
        log.info("保存新建管理员数据到数据库成功");
        // 4.将管理员id和密码放入 DTO中，并将DTO数据存入 Redis
        adminLoginFormDTO.setId(adminId);
        adminLoginFormDTO.setPassword(encodedPassword);
        long adminPasswordTime = ADMIN_CODE_TTL + RandomUtil.randomLong(ADMIN_CODE_TTL);
        String key = ADMIN_CODE_KEY + adminId;
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(adminLoginFormDTO), adminPasswordTime, TimeUnit.MINUTES);
        log.info("保存新建管理员数据到Redis成功");
        return Result.success("管理员注册成功");
    }

    /**
     * 管理员查询商家分页中间传输方法（数据传输 adminService -> merchantService）
     * @param pageData 分页信息
     * @return 商家分页
     */
    @Override
    public Page<Merchant> viewMerchantOfAdminWithTransmitData(PageData pageData) {
        return merchantService.queryMerchantOfAdmin(pageData);
    }

    /**
     * 管理员查询用户分页中间传输方法（数据传输 adminService -> userService）
     * @param pageData 分页信息
     * @return 用户分页
     */
    @Override
    public Page<User> viewUserOfAdminWithTransmitData(PageData pageData) {
        return userService.queryUserOfAdmin(pageData);
    }

    /**
     * 超级管理员查询普通管理员
     * @param pageData 分页信息
     * @return 管理员分页
     */
    @Override
    public Page<Admin> viewAdmin(PageData pageData) {
        // 获取到前端发送过来的分页数据
        Integer page = pageData.getPage();
        Integer pageSize = pageData.getPageSize();
        String key = pageData.getKey();
        Map<String, List<Boolean>> sortMap = pageData.getSortMap();
        log.info("page = {}, pageSize = {}, key = {}, sortMap = {}", page, pageSize, key, sortMap);
        // 从sortMap中拿取到排序条件
        List<Boolean> createTimeSort = sortMap.get(CREATE_TIME_SORT);
        Boolean createTimeSortStatus = createTimeSort.get(LIST_FIRST_INDEX);
        Boolean createTimeSortOrder = createTimeSort.get(LIST_SECOND_INDEX);
        // 构造分页器
        Page<Admin> pageInfo = new Page<>(page, pageSize);
        // 用 LambdaQueryWrapper 查询并按条件进行排序
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Admin::getCreateTime, key)
                .orderBy(createTimeSortStatus,
                        createTimeSortOrder, Admin::getCreateTime);
        // 将 LambdaQueryWrapper中数据赋给 pageInfo
        page(pageInfo, queryWrapper);
        // 返回分页数据
        return pageInfo;
    }

    /**
     * 管理员修改商家状态中间传输方法（数据传输 adminService -> merchantService）
     * @param merchantVO 商家状态信息
     */
    @Override
    public void modifyMerchantStatusOfAdminWithTransmitData(MerchantVO merchantVO) {
        merchantService.modifyMerchantStatusOfAdmin(merchantVO);
    }

    /**
     * 管理员修改用户状态中间传输方法（数据传输 adminService -> userService）
     * @param userVO 用户状态信息
     */
    @Override
    public void modifyUserStatusOfAdminWithTransmitData(UserVO userVO) {
        userService.modifyUserStatusOfAdmin(userVO);
    }

    /**
     * 超级管理员修改管理员状态
     * @param adminDTO 管理员状态信息
     */
    @Override
    public void modifyAdminStatus(AdminDTO adminDTO) {
        adminMapper.updateAdminStatus(adminDTO);
    }

    /**
     * 管理员查询申请分页中间传输方法（数据传输 adminService -> applyService）
     * @param pageData 分页信息
     * @return 申请分页
     */
    @Override
    public Page<Apply> viewApplyOfAdminWithTransmitData(PageData pageData) {
        return applyService.queryApplyPage(pageData);
    }

    /**
     * 随机生成初始化强密码
     * @return 生成密码
     */
    private String initializePassword() {
        // 随机生成 Integer型数字后再用 MD5算法进行加盐并加密，最后将返回的加密字符串随机截断为16位密码保证生成的管理员密码足够随机且安全
        Integer initialDigit = RandomUtil.randomInt(INTEGER_RANDOM_START / 2, Integer.MAX_VALUE);
        String password =  PasswordEncoder.encode(String.valueOf(initialDigit));
        password = subPassword(password);
        return password;
    }

    /**
     * 截断密码为 16位
     * @param password 54位加盐字符串
     * @return 16位密码
     */
    private String subPassword(String password) {
        int strLen = password.length();
        int left = strLen, right = strLen, step, linkLen;
        StringBuilder res = new StringBuilder();
        while (res.length() < PASSWORD_LENGTH) {
            step = RandomUtil.randomInt(5);
            linkLen = RandomUtil.randomInt(7);
            right -= step;
            left -= (step + linkLen);
            res.append(password, left, right);
            if (res.length() > PASSWORD_LENGTH) {
                res.setLength(PASSWORD_LENGTH);
            }
        }
        return res.toString();
    }

}




