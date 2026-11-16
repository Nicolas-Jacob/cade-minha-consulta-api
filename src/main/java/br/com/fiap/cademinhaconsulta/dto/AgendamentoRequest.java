package br.com.fiap.cademinhaconsulta.dto;

import jakarta.validation.constraints.NotNull;

public record AgendamentoRequest(
        @NotNull(message = "Medico e obrigatorio")
        Long idMedico
) {
}
