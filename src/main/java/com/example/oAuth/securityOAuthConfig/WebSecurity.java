package com.example.oAuth.securityOAuthConfig;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import in.mahesh.tasks.taskSecurityConfig.JwtTokenValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.bson.json.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.security.config.http.SessionCreationPolicy.*;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.security.config.annotation.web.builders.HttpSecurity.*;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableMethodSecurity
@Slf4j
public class WebSecurity {

	@Autowired
	JWTtoUserConvertor jwtToUserConverter;

	@Autowired
	KeyUtils keyUtils;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	UserDetailsManager userDetailsManager;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(
				(authorize) -> authorize.requestMatchers("/api/auth/*").permitAll().anyRequest().authenticated())
				.csrf(csrf -> csrf.disable()).cors(cors -> cors.disable()).httpBasic(basic -> basic.disable())
				.oauth2ResourceServer(
						(oauth2) -> oauth2.jwt((jwt) -> jwt.jwtAuthenticationConverter(jwtToUserConverter)))
				.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(
						(exceptions) -> exceptions.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
								.accessDeniedHandler(new BearerTokenAccessDeniedHandler()));
		return http.build();
	}

	@Bean

	@Primary
	JwtDecoder jwtAccessTokenDecoder() {
		return NimbusJwtDecoder.withPublicKey(keyUtils.getAccessTokenPublicKey()).build();
	}

	@Bean

	@Primary
	JwtEncoder jwtAccessTokenEncoder() {
		JWK jwk = new RSAKey.Builder(keyUtils.getAccessTokenPublicKey()).privateKey(keyUtils.getAccessTokenPrivateKey())
				.build();
		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwks);
	}

	@Bean

	@Qualifier("jwtRefreshTokenDecoder")
	JwtDecoder jwtRefreshTokenDecoder() {
		return NimbusJwtDecoder.withPublicKey(keyUtils.getRefreshTokenPublicKey()).build();
	}

	@Bean

	@Qualifier("jwtRefreshTokenEncoder")
	JwtEncoder jwtRefreshTokenEncoder() {
		JWK jwk = new RSAKey.Builder(keyUtils.getRefreshTokenPublicKey())
				.privateKey(keyUtils.getRefreshTokenPrivateKey()).build();
		JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwks);
	}

	@Bean

	@Qualifier("jwtRefreshTokenAuthProvider")
	JwtAuthenticationProvider jwtRefreshTokenAuthProvider() {
		JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtRefreshTokenDecoder());
		provider.setJwtAuthenticationConverter(jwtToUserConverter);
		return provider;
	}

	@Bean
	DaoAuthenticationProvider daoAuthenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setPasswordEncoder(passwordEncoder);
		provider.setUserDetailsService(userDetailsManager);
		return provider;
	}

}
