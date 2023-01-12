package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 验证码存到Redis
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.检验手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号不合法");
        }
        //2.生成验证码
        String code = RandomUtil.randomNumbers(6);
        //3.存到session
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        return Result.ok(code);
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
        if(!code.equals(stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone))){
            return Result.fail("验证码不正确");
        }

        User user = query().eq("phone", phone).one();

        if (user == null) {
            user = createUserWithPhone(phone);
        }

        String token = UUID.randomUUID().toString(true);

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        Map<String, Object> map = BeanUtil.beanToMap(userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        String loginToken = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(loginToken, map);
        stringRedisTemplate.expire(loginToken, LOGIN_USER_TTL, TimeUnit.MINUTES);

        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {

        User user = new User();

        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));

        save(user);

        return user;
    }
}
