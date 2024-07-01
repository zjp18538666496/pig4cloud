package com.pig4cloud.util.auth;

import org.springframework.security.core.AuthenticationException;

public class TokenExpiredException extends AuthenticationException {
    public TokenExpiredException(String msg) {
        super(msg);
    }
}
