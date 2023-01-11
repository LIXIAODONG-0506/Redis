package com.hmdp.utils;

import com.hmdp.entity.User;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author dong
 * @since 2023/1/11 17:27
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        /*
            1、获取session信息
            2、判断用户是否存在
            3、不存在进行拦截
            4、存在则保存到ThreadLocal
            5、放行
         */
        Object user = request.getSession().getAttribute("user");

        if (user == null) {
            response.setStatus(401);
            return false;
        }

        UserHolder.saveUser((User) user);

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
