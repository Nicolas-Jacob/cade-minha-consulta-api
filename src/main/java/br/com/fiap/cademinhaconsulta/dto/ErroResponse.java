package br.com.fiap.cademinhaconsulta.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Formato padrao de erro (secao 5 do contrato). "campos" so aparece em erros de validacao.
public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<CampoErro> campos,
        String caminho,
        LocalDateTime timestamp
) {

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho) {
        return de(status, mensagem, caminho, null);
    }

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho, List<CampoErro> campos) {
        return new ErroResponse(
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                campos,
                caminho,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }
}
