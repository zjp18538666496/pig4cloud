package com.pig4cloud.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 审批申请单（轻量审批）：状态机 0待审批→1通过/2驳回。
 * apply_type决定通过后的动作：role_apply=给申请人绑定biz_data里的角色；其余类型仅记录结果人工执行
 */
@Data
@TableName("sys_approval")
public class SysApprovalEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 申请标题（如"角色申请：研发工程师"）
     */
    private String title;

    /**
     * 类型(role_apply角色申请/tenant_open租户开通/handover离职交接)
     */
    private String apply_type;

    /**
     * 业务数据(JSON，如{"roleId":151,"roleName":"研发工程师"})
     */
    private String biz_data;

    /**
     * 申请理由
     */
    private String reason;

    /**
     * 申请人账号
     */
    private String applicant;

    private Integer tenant_id;

    /**
     * 状态(0待审批1通过2驳回)
     */
    private String status;

    private String approver;

    /**
     * 审批意见
     */
    private String approve_comment;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date approve_time;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date create_time;
}
