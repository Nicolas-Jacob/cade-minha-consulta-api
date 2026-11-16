package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.StatusPresenca;
import jakarta.validation.constraints.NotNull;

public record PresencaRequest(
        @NotNull(message = "Status e obrigatorio (PRESENTE ou FALTA)")
        StatusPresenca status
) {
}
