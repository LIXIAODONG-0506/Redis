package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SHOP_FOOD_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryList() {

        List<String> range = stringRedisTemplate.opsForList().range(SHOP_FOOD_KEY + ":list", 0, -1);

        if(CollectionUtil.isNotEmpty(range)){
            List<ShopType> shopTypeList = new ArrayList<>();
            range.stream().forEach(e-> shopTypeList.add(JSON.parseObject(e, ShopType.class)));

            return shopTypeList;
        }

        List<ShopType> sort = query().orderByAsc("sort").list();
        if(CollectionUtil.isEmpty(sort)){
            return null;
        }

        List<String> sortStrList = new ArrayList<>();
        sort.stream().forEach(e -> sortStrList.add(JSON.toJSONString(e)));

        stringRedisTemplate.opsForList().rightPushAll(SHOP_FOOD_KEY + ":list", sortStrList);

        return sort;
    }
}
