package ru.mirea.hrsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Bootstrap bootstrap,
        Cors cors,
        boolean cookieSecure,
        boolean permanentDeleteEnabled
) {
    public record Jwt(String secretBase64, String issuer, long accessMinutes, long refreshDays) {}
    public record Bootstrap(String email, String name, String password) {}
    public record Cors(String allowedOrigins) {
        public List<String> asList() {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }
}
