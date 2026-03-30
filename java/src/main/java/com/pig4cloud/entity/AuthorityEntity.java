package com.pig4cloud.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 【关键】MyBatis 反射创建对象必须要有无参构造
@AllArgsConstructor // 替代你手写的全参构造函数
public class AuthorityEntity {
    private Integer id;
    private String name;
    private String description;
}