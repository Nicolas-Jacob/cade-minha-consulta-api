package br.com.fiap.cademinhaconsulta.risco;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

// Converte uma linha de FN_DADOS_PREVISAO_FALTA (+ ComplementoPrevisao) no item que o servico de IA espera.
// Classe pura: sem Spring, sem banco e sem rede.
//
// Aproximacoes e limites dos dados:
// - data_agendamento: o sistema nao guarda a data de criacao da consulta. Usa a entrada do paciente na fila
//   (TB_GS_FILA.data_entrada), que e quando a demanda comecou; se for posterior a consulta, usa a data da consulta.
// - bolsa_familia, hipertensao, diabetes, alcoolismo, deficiencia e sms_recebido: o banco nao tem esses dados,
//   entao vao sempre false (o servico nao aceita null).
// - consultas_anteriores = presencas + faltas do historico; faltas_anteriores = faltas (os cancelamentos nao entram).
public final class MapeadorRiscoFalta {

    static final int IDADE_MAXIMA = 110;
    static final int CONSULTAS_MAXIMAS = 1000;

    private MapeadorRiscoFalta() {
    }

    // Vazio quando faltam dados para montar um item valido (ex.: complemento ausente ou sexo desconhecido)
    public static Optional<ItemRiscoRequest> paraItem(DadosPrevisaoFalta dados, ComplementoPrevisao complemento) {
        if (dados == null || dados.idConsulta() == null || dados.dataConsulta() == null
                || complemento == null || complemento.sexo() == null || complemento.dataEntradaFila() == null) {
            return Optional.empty();
        }

        LocalDate dataConsulta = dados.dataConsulta().toLocalDate();
        LocalDate dataAgendamento = complemento.dataEntradaFila().toLocalDate();
        if (dataAgendamento.isAfter(dataConsulta)) {
            dataAgendamento = dataConsulta;
        }

        int faltas = Math.max(dados.qtdFaltasHistorico(), 0);
        int presencas = Math.max(dados.qtdPresencasHistorico(), 0);
        int consultasAnteriores = (int) Math.min((long) presencas + faltas, CONSULTAS_MAXIMAS);
        int faltasAnteriores = Math.min(faltas, consultasAnteriores);

        ItemRiscoRequest item = new ItemRiscoRequest(
                String.valueOf(dados.idConsulta()),
                limitar(dados.idadePaciente(), 0, IDADE_MAXIMA),
                complemento.sexo().name().trim().toUpperCase(Locale.ROOT),
                dataAgendamento,
                dataConsulta,
                false, false, false, false, false, false,
                consultasAnteriores,
                faltasAnteriores);
        return valido(item) ? Optional.of(item) : Optional.empty();
    }

    // As mesmas regras do servico: um item invalido derrubaria o lote inteiro com 422
    static boolean valido(ItemRiscoRequest item) {
        return item.consultaId() != null && !item.consultaId().isBlank()
                && item.idade() >= 0 && item.idade() <= IDADE_MAXIMA
                && ("F".equals(item.genero()) || "M".equals(item.genero()))
                && item.dataAgendamento() != null && item.dataConsulta() != null
                && !item.dataConsulta().isBefore(item.dataAgendamento())
                && item.consultasAnteriores() >= 0 && item.consultasAnteriores() <= CONSULTAS_MAXIMAS
                && item.faltasAnteriores() >= 0 && item.faltasAnteriores() <= item.consultasAnteriores();
    }

    private static int limitar(int valor, int minimo, int maximo) {
        return Math.max(minimo, Math.min(valor, maximo));
    }
}
