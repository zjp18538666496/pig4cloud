package com.pig4cloud.config;

import com.pig4cloud.util.auth.SpringContextUtils;
import com.pig4cloud.util.auth.AuthAccessDeniedHandler;
import com.pig4cloud.util.auth.AuthEntryPointHandler;
import com.pig4cloud.util.auth.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
class SpringSecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthEntryPointHandler authEntryPointHandler;

    @Autowired
    private AuthAccessDeniedHandler authAccessDeniedHandler;

    /**
     * 加载用户信息
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return userDetailsService;
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 身份验证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 处理身份验证
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 配置SecurityFilterChain过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                (authz) -> authz
                        // 允许所有OPTIONS请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/user/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/createUser").permitAll()
                        // 允许任意请求被已登录用户访问，不检查Authority
                        .anyRequest().authenticated()
        );
        // 允许跨域
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        // 添加自定义身份验证
        http.authenticationProvider(authenticationProvider());
        // 禁用登录页面
        http.formLogin(AbstractHttpConfigurer::disable);
        // 禁用登出页面
        http.logout(AbstractHttpConfigurer::disable);
        // 禁用CSRF保护
        http.csrf(AbstractHttpConfigurer::disable);
        // 禁用session
        http.sessionManagement(AbstractHttpConfigurer::disable);
        // 禁用httpBasic
        http.httpBasic(AbstractHttpConfigurer::disable);
        // 通过上下文获取AuthenticationManager
        AuthenticationManager authenticationManager = SpringContextUtils.getBean("authenticationManager");
        // 添加自定义token验证过滤器
        http.addFilterBefore(new JwtAuthenticationFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class);
        //自定义处理器
        http.exceptionHandling(exceptionHandling -> exceptionHandling
                .accessDeniedHandler(authAccessDeniedHandler) //处理用户权限不足
                .authenticationEntryPoint(authEntryPointHandler) //处理用户未登录（未携带token）
        );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOriginPattern("*"); // 允许任何源
        corsConfig.addAllowedMethod("*"); // 允许任何HTTP方法
        corsConfig.addAllowedHeader("*"); // 允许任何HTTP头
        corsConfig.setAllowCredentials(true); // 允许证书（cookies）
        corsConfig.setMaxAge(3600L); // 预检请求的缓存时间（秒）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig); // 对所有路径应用这个配置
        return source;
    }

    /**
     * 配置一个WebSecurityCustomizer Bean，用于指定不需要Spring Security认证和授权机制的接口地址
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/error"
        );
    }
}
