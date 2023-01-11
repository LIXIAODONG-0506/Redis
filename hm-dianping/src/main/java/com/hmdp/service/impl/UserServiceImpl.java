package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.检验手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号不合法");
        }
        //2.生成验证码
        String code = RandomUtil.randomNumbers(6);
        //3.存到session
        session.setAttribute("phone", phone);
        session.setAttribute("code", code);
        log.debug("验证码：" + code);
        return null;
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        /*
            1、判断手机号和验证码是否正确
            2、判断手机号对应的用户是否存在
            3、存在则登录，不存在在创建
            4、用户信息保存到session
         */

        String phone = loginForm.getPhone();
        String code = loginForm.getCode();
        if(!phone.equals(session.getAttribute("phone").toString())
                || !code.equals(session.getAttribute("code").toString())){
            return Result.fail("验证码不正确");
        }

        User user = query().eq("phone", phone).one();

        if (user == null) {
            user = createUserWithPhone(phone);
        }

        session.setAttribute("user", user);

        return Result.ok();
    }

    private User createUserWithPhone(String phone) {

        User user = new User();

        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));

        save(user);

        return user;
    }
}
