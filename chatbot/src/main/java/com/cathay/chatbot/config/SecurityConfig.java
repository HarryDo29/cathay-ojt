//package com.cathay.chatbot.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    private final InternalApiKeyFilter internalApiKeyFilter;
//
//    public SecurityConfig(InternalApiKeyFilter internalApiKeyFilter) {
//        this.internalApiKeyFilter = internalApiKeyFilter;
//        System.out.println("✅ SecurityConfig đã được khởi tạo!");
//    }
//
//    //public end points
//    private final String[] PUBLIC_POST_ENDPOINTS = {
//            "/auth/login",
//            "/auth/register"
//    };
//
//    private final String[] PUBLIC_GET_ENDPOINTS = {
//            "/login/oauth2/code/google",  // Spring Security OAuth2 callback
//            "/oauth2/authorization/google"
//    };
//
////    @Bean
////    public AuthenticationManager authenticationManager (AuthenticationConfiguration config){
////        return config.getAuthenticationManager();
////    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
//        httpSecurity
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
//
//                .authorizeHttpRequests(req -> req
//                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
//                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
//                        .anyRequest().authenticated()
//                )
//
//                // InternalApiKeyFilter checked all requests
//                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return httpSecurity.build();
//    }
//}
