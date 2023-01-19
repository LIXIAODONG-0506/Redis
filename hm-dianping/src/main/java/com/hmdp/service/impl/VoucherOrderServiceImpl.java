package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀未开始");
        }
        // 3.判断秒杀是否已经结束
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已结束");
        }
        // 4.判断库存是否充足
        if(voucher.getStock() < 1){
            return Result.fail("库存不足!");
        }

        Long userId = UserHolder.getUser().getId();
        //创建锁对象
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order" + userId, stringRedisTemplate);
        //获取锁
        boolean b = simpleRedisLock.tryLock(1200);
        //判断是否获取锁成功
        if(!b){
            //获取锁失败
            return Result.fail("不允许重复下单");
        }
        try {
            // 获取代理对象（事务）
            IVoucherOrderService orderService = (IVoucherOrderService) AopContext.currentProxy();
            return orderService.createVoucherOrder(voucherId);
        }finally {
            // 释放锁
            simpleRedisLock.unlcok();
        }
    }

    @Transactional
    @Override
    public Result createVoucherOrder(Long voucherId) {
        // 5.一人一单
        Long userId = UserHolder.getUser().getId();
        // 5.1查询订单
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 5.2判断是否存在
        if(count > 0){
            //用户已经购买过了
            return Result.fail("用户已经购买过一次");
        }

        // 6.扣减库存
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();
        // 7.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long order = redisIdWorker.nextId("order");
        voucherOrder.setId(order);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);

        // 8.返回订单
        return Result.ok(order);
    }
}
