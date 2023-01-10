package com.template;

import com.alibaba.fastjson.JSONObject;
import com.template.redis.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class TemplateApplicationTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * redisTemplate
     */
    @Test
    void contextLoads() {
        redisTemplate.opsForValue().set("name", "虎哥");
        Object name = redisTemplate.opsForValue().get("name");
        System.out.println(name);
    }

    /**
     * redisTemplate
     */
    @Test
    void userTest(){
        User user = new User("张三", 22);
        redisTemplate.opsForValue().set("object", user);
        Object object = redisTemplate.opsForValue().get("object");
        System.out.println(object);
    }

    /**
     * StringRedisTemplate
     */
    @Test
    void stringRedisTemplate(){

        String userJson = JSONObject.toJSONString(new User("你好", 22));

        stringRedisTemplate.opsForValue().set("stringRedisTemplate", userJson);

        stringRedisTemplate.opsForValue().get("stringRedisTemplate");
        System.out.println("userJson--->:" + userJson);

        String userJson1 = JSONObject.toJSONString(new User("你好1", 221));

        stringRedisTemplate.opsForValue().set("StringRedisTemplate", userJson1);
        System.out.println("userJson1--->:" + userJson1);

    }

}
