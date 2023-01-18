package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
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
        //缓存穿透
        //Shop shop = queryWithPassThrough(id);

        //缓存击穿：互斥锁
        Shop shop = queryWithMutex(id);
        return Result.ok(shop);
    }

    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        // 1.根据id去redis查询是否存在对应的数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.如果存在则返回
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if(shopJson != null){
            return null;
        }
        // 3.实现缓存重建
        // 3.1获取互斥锁
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 3.2判断是否获取成功
            if(!isLock) {
                // 3.3失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 3.4成功，根据id查询数据库
            shop = getById(id);
            //模拟重建延时
            Thread.sleep(200);
            // 4.如果不存在，则返回404
            if(shop == null){
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 5.如果存在，则放进redis，返回数据
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }finally {
            unlock(lockKey);
        }
        return shop;
    }

    public Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        // 1.根据id去redis查询是否存在对应的数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.如果存在则返回
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if(shopJson != null){
            return null;
        }
        // 3.如果不存在则去数据库查询对应的数据
        Shop shop = getById(id);
        // 4.如果不存在，则返回404
        if(shop == null){
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 5.如果存在，则放进redis，返回数据
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
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
