package com.gff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Cross-Origin Resource Sharing (CORS) Configuration.
 * Enables seamless communication between Next.js frontend (localhost and remote 18.60.179.46:3000) and Spring Boot backend.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:${app.cors.allowed-origins:}}")
    private String allowedOriginsConfig;

    private List<String> getAllowedOriginPatterns() {
        List<String> patterns = new ArrayList<>(Arrays.asList(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]",
                "http://18.60.179.46:3000",
                "http://18.60.179.46:[*]",
                "https://18.60.179.46:[*]"
        ));

        if (StringUtils.hasText(allowedOriginsConfig)) {
            String[] origins = allowedOriginsConfig.split(",");
            for (String origin : origins) {
                String trimmed = origin.trim();
                if (StringUtils.hasText(trimmed) && !patterns.contains(trimmed)) {
                    patterns.add(trimmed);
                }
            }
        }
        return patterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(getAllowedOriginPatterns().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> customCorsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(getAllowedOriginPatterns());
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
