package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author dong
 * @since 2023/1/11 17:27
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        /*
            1、获取token信息
            2、判断用户是否存在
            3、不存在进行拦截
            4、存在则保存到ThreadLocal
            5、放行
         */
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            return true;
        }

        String key = RedisConstants.LOGIN_USER_KEY + token;

        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if(entries.isEmpty()){
            return true;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(entries, new UserDTO(), false);

        UserHolder.saveUser(userDTO);

        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
