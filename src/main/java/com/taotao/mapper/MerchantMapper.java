package com.taotao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taotao.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
* @author YuLong
* @description 针对表【merchant(商家表)】的数据库操作Mapper
* @createDate 2022-11-21 14:07:39
* @Entity taotao.entity.Merchant
*/
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

}



