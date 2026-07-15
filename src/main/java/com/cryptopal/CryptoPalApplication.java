package com.cryptopal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CryptoPalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoPalApplication.class, args);
    }

    // Spring Boot'un bulamadığı ObjectMapper'ı manuel olarak tanımlıyoruz
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // JavaTimeModule (Instant) gibi tarih modüllerini otomatik tanıması için:
        mapper.findAndRegisterModules();
        return mapper;
    }
}