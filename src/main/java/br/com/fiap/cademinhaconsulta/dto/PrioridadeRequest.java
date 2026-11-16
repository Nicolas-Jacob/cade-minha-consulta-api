package br.com.fiap.cademinhaconsulta.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PrioridadeRequest(
        @NotNull(message = "Prioridade e obrigatoria")
        @Min(value = 1, message = "Prioridade deve ser de 1 a 5")
        @Max(value = 5, message = "Prioridade deve ser de 1 a 5")
        Long prioridade
) {
}
