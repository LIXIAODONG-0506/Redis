package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1.根据id去redis查询是否存在对应的数据
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        // 2.如果存在则返回
        if(CollectionUtil.isNotEmpty(entries)){
            Shop shop = new Shop();
            BeanUtil.fillBeanWithMap(entries, shop, false);

            return Result.ok(shop);
        }
        if(entries.size() != 0){
            return Result.fail("404");
        }
        // 3.如果不存在则去数据库查询对应的数据
        Shop shop = getById(id);
        // 4.如果不存在，则返回404
        if(shop == null){
            Map<String, Object> shopMap1 = new HashMap<>();
            shopMap1.put("","");
            stringRedisTemplate.opsForHash().putAll(key, shopMap1);
            stringRedisTemplate.expire(key, CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("404");
        }
        // 5.如果存在，则放进redis，返回数据
        Map<String, Object> shopMap = BeanUtil.beanToMap(shop,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString()));

        stringRedisTemplate.opsForHash().putAll(key, shopMap);
        stringRedisTemplate.expire(key, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result updById(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不存在");
        }
        updateById(shop);

        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
