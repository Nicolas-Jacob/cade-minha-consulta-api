package br.com.fiap.cademinhaconsulta.dto;

import jakarta.validation.constraints.NotNull;

public record FilaRequest(
        @NotNull(message = "Especialidade e obrigatoria")
        Long idEspecialidade,

        @NotNull(message = "Unidade e obrigatoria")
        Long idUnidade
) {
}
