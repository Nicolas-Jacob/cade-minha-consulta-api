package br.com.fiap.cademinhaconsulta.risco;

import br.com.fiap.cademinhaconsulta.enums.Sexo;

import java.time.LocalDateTime;

// Dados que o modelo precisa e que o cursor de FN_DADOS_PREVISAO_FALTA nao traz: o sexo do paciente
// (TB_GS_USUARIO.sexo_usuario) e a entrada na fila (TB_GS_FILA.data_entrada). Buscados em lote pelos ids das
// consultas (ConsultaRepository.buscarComplementosPrevisao), sem alterar o PL/SQL.
public record ComplementoPrevisao(Long idConsulta, Sexo sexo, LocalDateTime dataEntradaFila) {
}
