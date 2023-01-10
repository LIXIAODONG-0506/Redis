package com.template.redis.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author dong
 * @since 2023/1/10 15:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    public String name;
    public Integer age;
}
