package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.NivelRisco;

import java.time.LocalDateTime;

// risco (0 a 1) e nivel ficam null enquanto o modelo de ML nao estiver integrado
public record RiscoFaltaResponse(
        Long idConsulta,
        String paciente,
        LocalDateTime dataHora,
        String especialidade,
        Double risco,
        NivelRisco nivel
) {
}
