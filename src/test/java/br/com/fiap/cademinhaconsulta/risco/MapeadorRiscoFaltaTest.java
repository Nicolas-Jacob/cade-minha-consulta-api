package br.com.fiap.cademinhaconsulta.risco;

import br.com.fiap.cademinhaconsulta.enums.Sexo;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Teste unitario puro: sem Spring, sem banco e sem rede
class MapeadorRiscoFaltaTest {

    private static final LocalDateTime DATA_CONSULTA = LocalDateTime.of(2026, 12, 10, 14, 30);
    private static final LocalDateTime ENTRADA_FILA = LocalDateTime.of(2026, 11, 1, 9, 15);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    static DadosPrevisaoFalta dados(long idConsulta, int idade, int faltas, int presencas) {
        return new DadosPrevisaoFalta(idConsulta, 7L, DATA_CONSULTA, "AGENDADA", 22, "THU", 14, "TARDE", idade, 3,
                2L, "Cardiologia", 1L, "UBS Centro", faltas, presencas, 1, 40.0);
    }

    static ComplementoPrevisao complemento(long idConsulta, Sexo sexo, LocalDateTime entradaFila) {
        return new ComplementoPrevisao(idConsulta, sexo, entradaFila);
    }

    @Test
    void mapeiaTodosOsCampos() {
        ItemRiscoRequest item = MapeadorRiscoFalta
                .paraItem(dados(123L, 24, 2, 1), complemento(123L, Sexo.M, ENTRADA_FILA))
                .orElseThrow();

        assertThat(item.consultaId()).isEqualTo("123");
        assertThat(item.idade()).isEqualTo(24);
        assertThat(item.genero()).isEqualTo("M");
        assertThat(item.dataAgendamento()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(item.dataConsulta()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(item.consultasAnteriores()).isEqualTo(3);
        assertThat(item.faltasAnteriores()).isEqualTo(2);
        assertThat(List.of(item.bolsaFamilia(), item.hipertensao(), item.diabetes(), item.alcoolismo(),
                item.deficiencia(), item.smsRecebido())).containsOnly(false);
    }

    @Test
    void jsonEmSnakeCaseSemNullEComDatasYyyyMmDd() {
        ItemRiscoRequest item = MapeadorRiscoFalta
                .paraItem(dados(123L, 24, 2, 1), complemento(123L, Sexo.F, ENTRADA_FILA))
                .orElseThrow();

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(new LoteRiscoRequest(List.of(item))));
        JsonNode no = json.get("consultas").get(0);

        Set<String> campos = new HashSet<>(no.propertyNames());
        assertThat(campos).containsExactlyInAnyOrder("consulta_id", "idade", "genero", "data_agendamento",
                "data_consulta", "bolsa_familia", "hipertensao", "diabetes", "alcoolismo", "deficiencia",
                "sms_recebido", "consultas_anteriores", "faltas_anteriores");
        no.forEach(valor -> assertThat(valor.isNull()).isFalse());
        assertThat(no.get("consulta_id").isString()).isTrue();
        assertThat(no.get("consulta_id").asString()).isEqualTo("123");
        assertThat(no.get("genero").asString()).isEqualTo("F");
        assertThat(no.get("data_agendamento").asString()).isEqualTo("2026-11-01");
        assertThat(no.get("data_consulta").asString()).isEqualTo("2026-12-10");
        assertThat(no.get("idade").isInt()).isTrue();
        assertThat(no.get("bolsa_familia").isBoolean()).isTrue();
    }

    @Test
    void jsonNaoTemDadosPessoais() {
        ItemRiscoRequest item = MapeadorRiscoFalta
                .paraItem(dados(123L, 24, 2, 1), complemento(123L, Sexo.F, ENTRADA_FILA))
                .orElseThrow();

        String json = jsonMapper.writeValueAsString(new LoteRiscoRequest(List.of(item))).toLowerCase(Locale.ROOT);

        assertThat(json).doesNotContain("nome", "sobrenome", "cpf", "telefone", "email", "e-mail", "endereco",
                "logradouro", "cep", "id_usuario", "especialidade", "unidade", "cardiologia", "ubs centro");
    }

    @Test
    void entradaNaFilaDepoisDaConsultaUsaADataDaConsulta() {
        ItemRiscoRequest item = MapeadorRiscoFalta
                .paraItem(dados(1L, 30, 0, 0), complemento(1L, Sexo.F, DATA_CONSULTA.plusDays(5)))
                .orElseThrow();

        assertThat(item.dataAgendamento()).isEqualTo(item.dataConsulta());
    }

    @Test
    void entradaNaFilaNoMesmoDiaDaConsultaEhValida() {
        ItemRiscoRequest item = MapeadorRiscoFalta
                .paraItem(dados(1L, 30, 0, 0), complemento(1L, Sexo.F, DATA_CONSULTA.minusHours(3)))
                .orElseThrow();

        assertThat(item.dataAgendamento()).isEqualTo(LocalDate.of(2026, 12, 10));
    }

    @Test
    void historicoLimitadoA1000ComFaltasMenoresOuIguaisAsConsultas() {
        ItemRiscoRequest item = MapeadorRiscoFalta
                .paraItem(dados(1L, 30, 1500, 700), complemento(1L, Sexo.M, ENTRADA_FILA))
                .orElseThrow();

        assertThat(item.consultasAnteriores()).isEqualTo(1000);
        assertThat(item.faltasAnteriores()).isEqualTo(1000);
        assertThat(item.faltasAnteriores()).isLessThanOrEqualTo(item.consultasAnteriores());
    }

    @Test
    void historicoNegativoViraZero() {
        ItemRiscoRequest item = MapeadorRiscoFalta
                .paraItem(dados(1L, 30, -1, -4), complemento(1L, Sexo.M, ENTRADA_FILA))
                .orElseThrow();

        assertThat(item.consultasAnteriores()).isZero();
        assertThat(item.faltasAnteriores()).isZero();
    }

    @Test
    void idadeLimitadaDe0a110() {
        assertThat(MapeadorRiscoFalta.paraItem(dados(1L, 130, 0, 0), complemento(1L, Sexo.F, ENTRADA_FILA))
                .orElseThrow().idade()).isEqualTo(110);
        assertThat(MapeadorRiscoFalta.paraItem(dados(1L, -2, 0, 0), complemento(1L, Sexo.F, ENTRADA_FILA))
                .orElseThrow().idade()).isZero();
    }

    @Test
    void semComplementoOuComSexoDesconhecidoNaoGeraItem() {
        assertThat(MapeadorRiscoFalta.paraItem(dados(1L, 30, 0, 0), null)).isEmpty();
        assertThat(MapeadorRiscoFalta.paraItem(dados(1L, 30, 0, 0), complemento(1L, null, ENTRADA_FILA))).isEmpty();
        assertThat(MapeadorRiscoFalta.paraItem(dados(1L, 30, 0, 0), complemento(1L, Sexo.F, null))).isEmpty();
    }

    @Test
    void validacaoRecusaItensForaDasRegrasDoServico() {
        LocalDate dia = LocalDate.of(2026, 12, 10);
        assertThat(MapeadorRiscoFalta.valido(item("X", dia, dia, 1, 0))).isFalse();
        assertThat(MapeadorRiscoFalta.valido(item("F", dia.plusDays(1), dia, 1, 0))).isFalse();
        assertThat(MapeadorRiscoFalta.valido(item("F", dia, dia, 1, 2))).isFalse();
        assertThat(MapeadorRiscoFalta.valido(item("F", dia, dia, 1001, 0))).isFalse();
        assertThat(MapeadorRiscoFalta.valido(item("F", dia, dia, 1, 1))).isTrue();
        assertThat(Optional.of(item("M", dia.minusDays(3), dia, 0, 0)).filter(MapeadorRiscoFalta::valido))
                .isPresent();
    }

    private static ItemRiscoRequest item(String genero, LocalDate agendamento, LocalDate consulta, int consultas,
                                         int faltas) {
        return new ItemRiscoRequest("1", 30, genero, agendamento, consulta, false, false, false, false, false, false,
                consultas, faltas);
    }
}
