package com.aml.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * JacksonConfig — ObjectMapper configuration.
 *
 * <p>Registers the JavaTimeModule to handle Java 8+ date/time types
 * (LocalDateTime, Instant, etc.) in JSON serialization and deserialization.</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());
        // Write dates as ISO-8601 strings, not epoch timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
