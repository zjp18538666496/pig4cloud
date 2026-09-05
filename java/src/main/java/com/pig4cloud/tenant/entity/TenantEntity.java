package com.pig4cloud.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_tenant")
public class TenantEntity {

    /**
     * 租户id（0为平台层）
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 租户编码
     */
    private String tenant_code;

    /**
     * 租户名称
     */
    private String tenant_name;

    /**
     * 状态(0禁用1启用)
     */
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date update_time;

    /**
     * 绑定套餐id（决定租户管理员可用菜单）
     */
    private Integer package_id;

    /**
     * 过期时间（空为永不过期）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expire_time;

    /**
     * 用户数上限（空为不限制）
     */
    private Integer user_limit;

    /**
     * 品牌名称（登录页/侧边栏展示，空则用tenant_name）
     */
    private String brand_name;

    /**
     * 品牌logo地址（空则不展示图片）
     */
    private String brand_logo;

    /**
     * 品牌主题色（如#409EFF，登录页/侧边栏点缀）
     */
    private String brand_color;
}
