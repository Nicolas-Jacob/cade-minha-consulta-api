package br.com.fiap.cademinhaconsulta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EspecialidadeRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 50, message = "Nome deve ter no maximo 50 caracteres")
        String nome,

        @NotBlank(message = "Descricao e obrigatoria")
        @Size(max = 200, message = "Descricao deve ter no maximo 200 caracteres")
        String descricao
) {
}
