package br.com.fiap.cademinhaconsulta.risco;

import br.com.fiap.cademinhaconsulta.enums.NivelRisco;

// Saida do modelo para uma consulta. risco de 0 a 1; risco e nivel null quando nao ha previsao.
public record PrevisaoRisco(Long idConsulta, Double risco, NivelRisco nivel) {
}
