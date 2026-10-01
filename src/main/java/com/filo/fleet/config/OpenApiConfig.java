package com.filo.fleet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fleetOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Filo Yönetim Sistemi API")
                .version("1.0.0")
                .description("""
                        Lojistik şirketi için araç filosu, sürücü atamaları ve periyodik bakım (KM / gün bazlı) takibi.

                        **Temel iş kuralları**
                        - Ehliyet sınıfı yetersiz, ehliyeti geçersiz veya zaten aktif görevi olan sürücüye araç atanamaz.
                        - Son bakımdan bu yana 10.000 km (veya bakım gün aralığı) dolan araç göreve atanamaz.
                        - Araç KM değeri geriye doğru güncellenemez.

                        Tüm hatalar standart `ErrorResponse` formatında döner.""")
                .contact(new Contact().name("Filo Yönetim Ekibi").email("filo@example.com")));
    }
}
