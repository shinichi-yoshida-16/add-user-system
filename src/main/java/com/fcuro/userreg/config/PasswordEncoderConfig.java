package com.fcuro.userreg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoderをSecurityConfigから切り出したもの。
 *
 * SecurityConfigはSheetAuthenticationProvider(PasswordEncoderに依存)をコンストラクタ注入しているため、
 * 同じクラスにPasswordEncoderのBean定義を置くと循環参照になる。
 *
 * スプレッドシート上のパスワードはbcrypt(bcryptjs)形式でハッシュ化されているため、
 * 互換性のあるSpring SecurityのBCryptPasswordEncoderを使用する。
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
