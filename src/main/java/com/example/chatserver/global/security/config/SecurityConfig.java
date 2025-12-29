package com.example.chatserver.global.security.config;


import java.util.List;

import com.example.chatserver.global.security.handler.CustomAccessDeniedHandler;
import com.example.chatserver.global.security.handler.CustomAuthenticationEntryPoint;
import com.example.chatserver.global.security.jwt.JwtAuthenticationFilter;
import com.example.chatserver.global.security.jwt.JwtAuthenticationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final ObjectMapper objectMapper;
	private final JwtAuthenticationProvider jwtAuthenticationProvider;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 연결
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/assets/**", "/favicon.ico", "/swagger-resources/**", "/swagger-ui.html",
					"/swagger-ui/**",
					"/webjars/**", "/swagger/**", "/", "/index.html", "/api-docs/**", "/images/logo.png")
				.permitAll()
				.requestMatchers(
					"/api/error",
					"/api/auth/**",
					"/test/anonymous",
					"/api/actuator/health",
					"/ws-test/**",
					"/api/connect/**"
					// "/publish/**",
					// "/topic/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterAfter(jwtAuthenticationFilter(), LogoutFilter.class)
			.exceptionHandling(configurer -> configurer
				.authenticationEntryPoint(new CustomAuthenticationEntryPoint(objectMapper))
				.accessDeniedHandler(new CustomAccessDeniedHandler(objectMapper))
			)
			.build();
	}

	@Bean
	public AuthenticationManager authenticationManager() {
		return new ProviderManager(jwtAuthenticationProvider);
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(authenticationManager());
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of("http://localhost:5173","http://172.29.67.245:5173"));

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true); // 쿠키 허용

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
