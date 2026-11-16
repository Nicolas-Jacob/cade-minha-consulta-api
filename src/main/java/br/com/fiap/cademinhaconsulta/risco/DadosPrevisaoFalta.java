package br.com.fiap.cademinhaconsulta.risco;

import java.time.LocalDateTime;

// Uma linha do cursor de PKG_GS_CONSULTA_DADOS.FN_DADOS_PREVISAO_FALTA: os atributos de entrada do modelo de ML.
// Nao tem nome, CPF nem contato do paciente; o nome so entra depois, na resposta para o GESTOR.
public record DadosPrevisaoFalta(
        Long idConsulta,
        Long idUsuario,
        LocalDateTime dataConsulta,
        String statusAgendamento,
        int diasAteConsulta,
        // MON, TUE, ... (NLS_DATE_LANGUAGE=ENGLISH)
        String diaSemana,
        int horaConsulta,
        // MANHA ou TARDE
        String turnoConsulta,
        int idadePaciente,
        int prioridadeFila,
        Long idEspecialidade,
        String especialidade,
        Long idUnidade,
        String unidade,
        int qtdFaltasHistorico,
        int qtdPresencasHistorico,
        int qtdCancelamentosHistorico,
        // 0 a 100
        double taxaFaltasHistoricaPct
) {
}
