package br.com.fiap.cademinhaconsulta.risco;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Escolhe o cliente do modelo de risco de falta pela propriedade risco-falta.url (RISCO_FALTA_URL)
@Slf4j
@Configuration
public class RiscoFaltaConfig {

    @Bean
    public RiscoFaltaClient riscoFaltaClient(@Value("${risco-falta.url:}") String url) {
        if (url == null || url.isBlank()) {
            log.info("risco-falta.url vazia: usando StubRiscoFaltaClient (risco e nivel null).");
            return new StubRiscoFaltaClient();
        }
        // Ponto de integracao: quando o servico do modelo existir, devolver aqui o cliente HTTP real, por exemplo
        // new HttpRiscoFaltaClient(RestClient.builder().baseUrl(url).build()), que envia os atributos e le as
        // previsoes. Enquanto ele nao existe, a API continua funcionando com o stub.
        log.warn("risco-falta.url configurada ({}), mas o cliente HTTP ainda nao foi implementado. Usando o stub.",
                url);
        return new StubRiscoFaltaClient();
    }
}
