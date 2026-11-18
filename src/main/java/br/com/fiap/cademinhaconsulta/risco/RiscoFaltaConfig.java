package br.com.fiap.cademinhaconsulta.risco;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Escolhe o cliente do modelo de risco de falta pela propriedade risco-falta.url (RISCO_FALTA_URL):
// vazia = StubRiscoFaltaClient; preenchida = HttpRiscoFaltaClient
@Slf4j
@Configuration
public class RiscoFaltaConfig {

    @Bean
    public RiscoFaltaClient riscoFaltaClient(@Value("${risco-falta.url:}") String url,
                                             @Value("${risco-falta.token:}") String token,
                                             @Value("${risco-falta.connect-timeout-ms:3000}") int connectTimeoutMs,
                                             @Value("${risco-falta.read-timeout-ms:15000}") int readTimeoutMs) {
        if (url == null || url.isBlank()) {
            log.info("risco-falta.url vazia: usando StubRiscoFaltaClient (risco e nivel null).");
            return new StubRiscoFaltaClient();
        }
        boolean comToken = token != null && !token.isBlank();
        log.info("Risco de falta: HttpRiscoFaltaClient em {} (token {}, connect {} ms, read {} ms).",
                HttpRiscoFaltaClient.normalizarUrl(url), comToken ? "configurado" : "ausente",
                connectTimeoutMs, readTimeoutMs);
        return HttpRiscoFaltaClient.criar(url, token, connectTimeoutMs, readTimeoutMs);
    }
}
