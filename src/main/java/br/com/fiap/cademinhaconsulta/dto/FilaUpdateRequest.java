package br.com.fiap.cademinhaconsulta.dto;

import jakarta.validation.constraints.NotNull;

public record FilaUpdateRequest(
        @NotNull(message = "Unidade e obrigatoria")
        Long idUnidade
) {
}
