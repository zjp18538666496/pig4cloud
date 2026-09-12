package com.pig4cloud.job.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.pig4cloud.message.entity.SysMessageEntity;
import com.pig4cloud.message.mapper.SysMessageMapper;
import com.pig4cloud.notify.service.NotifyService;
import com.pig4cloud.tenant.entity.TenantEntity;
import com.pig4cloud.config.service.ConfigService;
import com.pig4cloud.tenant.mapper.TenantMapper;
import com.pig4cloud.user.entity.UserEntity;
import com.pig4cloud.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 租户过期自动禁用 + 预警：每天检查
 * 1. expire_time已过期的启用租户置为禁用（登录侧同时有过期拦截兜底）
 * 2. 7天内即将到期的租户：给租户管理员发站内信+通知渠道预警（tenant-expire-warning）
 * 3. 用户数达到配额90%的租户：给租户管理员发站内信+通知渠道预警（user-quota-warning）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantExpireCheckJob implements JobHandler {

    private static final long WARN_BEFORE_MILLIS = 7L * 24 * 3600 * 1000;

    private final TenantMapper tenantMapper;
    private final UserMapper userMapper;
    private final SysMessageMapper messageMapper;
    private final NotifyService notifyService;
    private final ConfigService configService;
    private final com.pig4cloud.common.store.StateStore stateStore;

    @Override
    public String name() {
        return "tenantExpireCheckJob";
    }

    @Override
    public void execute() {
        int rows = tenantMapper.update(null, new UpdateWrapper<TenantEntity>()
                .eq("status", "1")
                .lt("expire_time", new Date())
                .set("status", "0")
                .set("update_time", new Date()));
        log.info("租户过期检查完成，自动禁用{}个租户", rows);
        warnExpiringTenants();
        warnQuotaTenants();
        warnPasswordExpiry();
    }

    /**
     * 密码到期提前提醒：开启密码有效期(pwd.expire-days>0)时，到期前7天给用户发站内信。
     * StateStore去重（7天内同一用户只提醒一次）
     */
    private void warnPasswordExpiry() {
        int expireDays = configService.getInt("pwd.expire-days", 0);
        if (expireDays <= 0) {
            return;
        }
        Date now = new Date();
        Date warnFrom = new Date(now.getTime() + 7L * 24 * 3600 * 1000);
        List<UserEntity> users = userMapper.selectList(new QueryWrapper<UserEntity>()
                .eq("deleted", 0)
                .isNotNull("pwd_update_time")
                .lt("pwd_update_time", warnFrom));
        int warned = 0;
        for (UserEntity user : users) {
            long ageDays = (now.getTime() - user.getPwd_update_time().getTime()) / (24L * 3600 * 1000);
            long remainDays = expireDays - ageDays;
            if (remainDays > 7 || remainDays < 0) {
                continue;
            }
            // 7天内同一用户只提醒一次（StateStore去重，随提醒窗口自然过期）
            if (!stateStore.putIfAbsent("pwd:warned:" + user.getId(), "1", 7L * 24 * 3600 * 1000)) {
                continue;
            }
            sendSiteMessage(user,
                    "密码即将到期提醒",
                    "您的账号密码已使用" + ageDays + "天，将于约" + Math.max(remainDays, 0) + "天后到期（策略"
                            + expireDays + "天），请尽快在【个人中心-安全信息】修改密码。");
            warned++;
        }
        if (warned > 0) {
            log.info("密码到期提醒发送{}个用户", warned);
        }
    }

    /**
     * 7天内到期的启用租户预警
     */
    private void warnExpiringTenants() {
        Date now = new Date();
        List<TenantEntity> expiring = tenantMapper.selectList(new QueryWrapper<TenantEntity>()
                .eq("status", "1")
                .gt("expire_time", now)
                .lt("expire_time", new Date(now.getTime() + WARN_BEFORE_MILLIS)));
        for (TenantEntity tenant : expiring) {
            String expireTime = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(tenant.getExpire_time());
            Map<String, String> params = Map.of(
                    "tenantName", tenant.getTenant_name() == null ? "" : tenant.getTenant_name(),
                    "expireTime", expireTime);
            sendToTenantAdmin(tenant, "租户即将到期：" + params.get("tenantName"),
                    "租户【" + params.get("tenantName") + "】将于 " + expireTime + " 到期，请及时处理。");
            notifyService.sendByEvent("tenant-expire-warning", params);
        }
        if (!expiring.isEmpty()) {
            log.info("租户到期预警发送{}个", expiring.size());
        }
    }

    /**
     * 用户数达到配额90%的启用租户预警
     */
    private void warnQuotaTenants() {
        List<TenantEntity> tenants = tenantMapper.selectList(new QueryWrapper<TenantEntity>()
                .eq("status", "1")
                .isNotNull("user_limit")
                .gt("user_limit", 0));
        for (TenantEntity tenant : tenants) {
            Long used = userMapper.selectCount(new QueryWrapper<UserEntity>().eq("tenant_id", tenant.getId()));
            if (used != null && used * 10 >= tenant.getUser_limit() * 9) {
                Map<String, String> params = Map.of(
                        "tenantName", tenant.getTenant_name() == null ? "" : tenant.getTenant_name(),
                        "used", String.valueOf(used),
                        "limit", String.valueOf(tenant.getUser_limit()));
                sendToTenantAdmin(tenant, "租户用户数即将达到上限：" + params.get("tenantName"),
                        "租户【" + params.get("tenantName") + "】用户数 " + used + "/" + tenant.getUser_limit()
                                + "，已达90%，新增用户将受限。");
                notifyService.sendByEvent("user-quota-warning", params);
            }
        }
    }

    /**
     * 发站内信给租户管理员（开通租户时自动创建的{租户ID}admin账号）；直接落库，不依赖登录上下文
     */
    private void sendSiteMessage(UserEntity target, String title, String content) {
        try {
            SysMessageEntity message = new SysMessageEntity();
            message.setTitle(title);
            message.setContent(content);
            message.setMsg_type("1");
            message.setTenant_id(target.getTenant_id());
            message.setTarget_user_id(target.getId());
            message.setRead_flag("0");
            message.setCreate_by("系统");
            message.setCreate_time(new Date());
            messageMapper.insert(message);
        } catch (Exception ex) {
            log.warn("站内信发送失败: {}", ex.getMessage());
        }
    }

    private void sendToTenantAdmin(TenantEntity tenant, String title, String content) {
        try {
            UserEntity admin = userMapper.selectOne(new QueryWrapper<UserEntity>()
                    .eq("tenant_id", tenant.getId())
                    .likeRight("username", String.valueOf(tenant.getId()))
                    .last("LIMIT 1"));
            if (admin == null) {
                return;
            }
            SysMessageEntity message = new SysMessageEntity();
            message.setTitle(title);
            message.setContent(content);
            message.setMsg_type("1");
            message.setTenant_id(tenant.getId());
            message.setTarget_user_id(admin.getId());
            message.setRead_flag("0");
            message.setCreate_by("系统");
            message.setCreate_time(new Date());
            messageMapper.insert(message);
        } catch (Exception ex) {
            log.warn("租户[{}]预警站内信发送失败: {}", tenant.getId(), ex.getMessage());
        }
    }
}
