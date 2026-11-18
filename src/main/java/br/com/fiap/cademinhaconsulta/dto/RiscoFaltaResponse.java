package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.NivelRisco;

import java.time.LocalDateTime;

// risco (0 a 1) e nivel vem do servico de IA (vaga-certa-ia); null sem RISCO_FALTA_URL ou se o servico falhar
public record RiscoFaltaResponse(
        Long idConsulta,
        String paciente,
        LocalDateTime dataHora,
        String especialidade,
        Double risco,
        NivelRisco nivel
) {
}
