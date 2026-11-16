package br.com.fiap.cademinhaconsulta.messaging;

import java.time.LocalDateTime;

// Publicado quando o cidadao cancela uma consulta (PATCH /api/consultas/{id}/cancelar)
public record VagaLiberadaEvent(
        Long idConsulta,
        Long idMedico,
        Long idUnidade,
        Long idEspecialidade,
        LocalDateTime dataHora
) {
}
