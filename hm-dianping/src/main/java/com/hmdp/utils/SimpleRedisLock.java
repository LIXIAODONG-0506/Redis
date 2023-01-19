package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author dong
 * @since 2023/1/19 11:29
 */
public class SimpleRedisLock implements ILock{

    private String name;

    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String s, StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String UUID_PREFIX = UUID.randomUUID().toString() + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String id = UUID_PREFIX + Thread.currentThread().getId();

        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, id, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(aBoolean);
    }
    @Override
    public void unlcok() {
        // 调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                UUID_PREFIX + Thread.currentThread().getId()
        );
    }

//    @Override
//    public void unlcok() {
//        String id = UUID_PREFIX + Thread.currentThread().getId();
//        String s = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//
//        if(id.equals(s)){
//            //释放锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }
}
