package br.com.fiap.cademinhaconsulta.risco;

import br.com.fiap.cademinhaconsulta.enums.NivelRisco;
import br.com.fiap.cademinhaconsulta.enums.Sexo;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Teste unitario sem rede real: o RestClient e ligado a um MockRestServiceServer
class HttpRiscoFaltaClientTest {

    private static final String URL = "http://ia.teste";
    private static final String URL_LOTE = URL + "/v1/risco-falta/lote";
    private static final String TOKEN = "token-secreto-de-teste";
    private static final LocalDateTime DATA_CONSULTA = LocalDateTime.of(2026, 12, 10, 14, 30);
    private static final LocalDateTime ENTRADA_FILA = LocalDateTime.of(2026, 11, 1, 9, 0);

    private MockRestServiceServer servidor;
    private HttpRiscoFaltaClient cliente;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void configurarLogs() {
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(HttpRiscoFaltaClient.class)).addAppender(logs);
    }

    @AfterEach
    void removerLogs() {
        ((Logger) LoggerFactory.getLogger(HttpRiscoFaltaClient.class)).detachAppender(logs);
    }

    private void criarCliente(String url, String token) {
        RestClient.Builder builder = HttpRiscoFaltaClient.configurar(RestClient.builder(), url, token);
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new HttpRiscoFaltaClient(builder.build());
    }

    private static DadosPrevisaoFalta dados(long idConsulta) {
        return new DadosPrevisaoFalta(idConsulta, 7L, DATA_CONSULTA, "AGENDADA", 22, "THU", 14, "TARDE", 24, 3,
                2L, "Cardiologia", 1L, "UBS Centro", 2, 1, 0, 66.67);
    }

    private static Map<Long, ComplementoPrevisao> complementos(long... ids) {
        Map<Long, ComplementoPrevisao> mapa = new HashMap<>();
        for (long id : ids) {
            mapa.put(id, new ComplementoPrevisao(id, Sexo.M, ENTRADA_FILA));
        }
        return mapa;
    }

    private static String resposta(String id, double probabilidade, String faixa) {
        return """
                {"consulta_id":"%s","probabilidade_falta":%s,"faixa_risco":"%s","sinais":["x"],
                 "recomendacao":"Ligar para o paciente","modelo":"gradient_boosting","campo_novo":1}
                """.formatted(id, probabilidade, faixa);
    }

    private List<String> mensagens(Level nivel) {
        return logs.list.stream().filter(e -> e.getLevel() == nivel).map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void sucessoPreencheRiscoENivelPorConsultaId() {
        criarCliente(URL + "/", TOKEN);
        servidor.expect(requestTo(URL_LOTE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Service-Token", TOKEN))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"consultas":[
                          {"consulta_id":"1","idade":24,"genero":"M","data_agendamento":"2026-11-01",
                           "data_consulta":"2026-12-10","bolsa_familia":false,"hipertensao":false,"diabetes":false,
                           "alcoolismo":false,"deficiencia":false,"sms_recebido":false,
                           "consultas_anteriores":3,"faltas_anteriores":2},
                          {"consulta_id":"2"}, {"consulta_id":"3"}]}
                        """))
                // Ordem diferente da enviada: o casamento e pelo consulta_id
                .andRespond(withSuccess("[" + resposta("3", 0.15, "baixo") + "," + resposta("1", 0.72154, "alto")
                        + "," + resposta("2", 0.4, "medio") + "]", MediaType.APPLICATION_JSON));

        List<PrevisaoRisco> previsoes = cliente.prever(List.of(dados(1), dados(2), dados(3)), complementos(1, 2, 3));

        servidor.verify();
        assertThat(previsoes).containsExactly(
                new PrevisaoRisco(1L, 0.7215, NivelRisco.ALTO),
                new PrevisaoRisco(2L, 0.4, NivelRisco.MEDIO),
                new PrevisaoRisco(3L, 0.15, NivelRisco.BAIXO));
        assertThat(mensagens(Level.INFO)).anySatisfy(m -> assertThat(m)
                .contains("3 consulta(s) enviada(s)").contains("gradient_boosting"));
    }

    @Test
    void semTokenNaoEnviaOHeader() {
        criarCliente(URL, "  ");
        servidor.expect(requestTo(URL_LOTE))
                .andExpect(headerDoesNotExist("X-Service-Token"))
                .andRespond(withSuccess("[" + resposta("1", 0.1, "baixo") + "]", MediaType.APPLICATION_JSON));

        assertThat(cliente.prever(List.of(dados(1)), complementos(1)))
                .containsExactly(new PrevisaoRisco(1L, 0.1, NivelRisco.BAIXO));
        servidor.verify();
    }

    @Test
    void idAusenteNaRespostaFicaNull() {
        criarCliente(URL, null);
        servidor.expect(requestTo(URL_LOTE))
                .andRespond(withSuccess("[" + resposta("1", 0.3, "medio") + "]", MediaType.APPLICATION_JSON));

        assertThat(cliente.prever(List.of(dados(1), dados(2)), complementos(1, 2))).containsExactly(
                new PrevisaoRisco(1L, 0.3, NivelRisco.MEDIO),
                new PrevisaoRisco(2L, null, null));
    }

    @Test
    void probabilidadeForaDoIntervaloEFaixaDesconhecidaFicamNull() {
        criarCliente(URL, null);
        servidor.expect(requestTo(URL_LOTE))
                .andRespond(withSuccess("[" + resposta("1", 1.5, "alto") + "," + resposta("2", 0.5, "altissimo")
                        + "]", MediaType.APPLICATION_JSON));

        assertThat(cliente.prever(List.of(dados(1), dados(2)), complementos(1, 2))).containsExactly(
                new PrevisaoRisco(1L, null, NivelRisco.ALTO),
                new PrevisaoRisco(2L, 0.5, null));
    }

    @Test
    void itemInvalidoNaoVaiNoLoteEFicaNull() {
        criarCliente(URL, null);
        servidor.expect(requestTo(URL_LOTE))
                .andExpect(jsonPath("$.consultas.length()").value(1))
                .andExpect(jsonPath("$.consultas[0].consulta_id").value("1"))
                .andRespond(withSuccess("[" + resposta("1", 0.2, "baixo") + "]", MediaType.APPLICATION_JSON));

        // Consulta 2 sem complemento (sexo e entrada na fila desconhecidos)
        assertThat(cliente.prever(List.of(dados(1), dados(2)), complementos(1))).containsExactly(
                new PrevisaoRisco(1L, 0.2, NivelRisco.BAIXO),
                new PrevisaoRisco(2L, null, null));
        servidor.verify();
        assertThat(mensagens(Level.WARN)).anySatisfy(m -> assertThat(m).contains("Consulta 2"));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 422, 500, 503})
    void erroHttpDeixaTodasNullSemExcecaoESemCorpoNemTokenNoLog(int status) {
        criarCliente(URL, TOKEN);
        servidor.expect(requestTo(URL_LOTE))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":[{\"input\":\"dado-sensivel-ecoado\"}]}"));

        assertThat(cliente.prever(List.of(dados(1), dados(2)), complementos(1, 2))).containsExactly(
                new PrevisaoRisco(1L, null, null),
                new PrevisaoRisco(2L, null, null));
        assertThat(mensagens(Level.WARN)).containsExactly("Risco de falta indisponivel: HTTP " + status);
        assertThat(logs.list).noneSatisfy(e -> assertThat(e.getFormattedMessage())
                .containsAnyOf(TOKEN, "dado-sensivel-ecoado"));
    }

    @Test
    void timeoutDeixaTodasNull() {
        criarCliente(URL, null);
        servidor.expect(requestTo(URL_LOTE)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThat(cliente.prever(List.of(dados(1)), complementos(1)))
                .containsExactly(new PrevisaoRisco(1L, null, null));
        assertThat(mensagens(Level.WARN)).singleElement().satisfies(m -> assertThat(m)
                .startsWith("Risco de falta indisponivel: ").contains("SocketTimeoutException: Read timed out"));
    }

    @Test
    void conexaoRecusadaDeixaTodasNull() {
        criarCliente(URL, null);
        servidor.expect(requestTo(URL_LOTE)).andRespond(withException(new ConnectException("Connection refused")));

        assertThat(cliente.prever(List.of(dados(1)), complementos(1)))
                .containsExactly(new PrevisaoRisco(1L, null, null));
        assertThat(mensagens(Level.WARN)).singleElement().satisfies(m -> assertThat(m)
                .contains("Connection refused"));
    }

    @Test
    void jsonInvalidoDeixaTodasNull() {
        criarCliente(URL, null);
        servidor.expect(requestTo(URL_LOTE)).andRespond(withSuccess("isto nao e json", MediaType.APPLICATION_JSON));

        assertThat(cliente.prever(List.of(dados(1)), complementos(1)))
                .containsExactly(new PrevisaoRisco(1L, null, null));
        assertThat(mensagens(Level.WARN)).singleElement().satisfies(m -> assertThat(m)
                .startsWith("Risco de falta indisponivel: ").doesNotContain("isto nao e json"));
    }

    @Test
    void listaVaziaNaoChamaOServico() {
        criarCliente(URL, null);
        servidor.expect(ExpectedCount.never(), requestTo(URL_LOTE));

        assertThat(cliente.prever(List.of(), Map.of())).isEmpty();
        servidor.verify();
    }

    @Test
    void semItensValidosNaoChamaOServico() {
        criarCliente(URL, null);
        servidor.expect(ExpectedCount.never(), requestTo(URL_LOTE));

        assertThat(cliente.prever(List.of(dados(1)), Map.of())).containsExactly(new PrevisaoRisco(1L, null, null));
        servidor.verify();
    }

    @Test
    void quinhentosEUmItensViramDuasChamadas() {
        criarCliente(URL, null);
        List<DadosPrevisaoFalta> atributos = new ArrayList<>();
        long[] ids = new long[501];
        for (int i = 0; i < 501; i++) {
            ids[i] = i + 1;
            atributos.add(dados(i + 1));
        }
        servidor.expect(requestTo(URL_LOTE))
                .andExpect(jsonPath("$.consultas.length()").value(500))
                .andRespond(withSuccess("[" + resposta("1", 0.9, "alto") + "]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL_LOTE))
                .andExpect(jsonPath("$.consultas.length()").value(1))
                .andExpect(jsonPath("$.consultas[0].consulta_id").value("501"))
                .andRespond(withSuccess("[" + resposta("501", 0.1, "baixo") + "]", MediaType.APPLICATION_JSON));

        List<PrevisaoRisco> previsoes = cliente.prever(atributos, complementos(ids));

        servidor.verify();
        assertThat(previsoes).hasSize(501);
        assertThat(previsoes.getFirst()).isEqualTo(new PrevisaoRisco(1L, 0.9, NivelRisco.ALTO));
        assertThat(previsoes.getLast()).isEqualTo(new PrevisaoRisco(501L, 0.1, NivelRisco.BAIXO));
    }

    @Test
    void falhaNoSegundoLoteZeraTodas() {
        criarCliente(URL, null);
        List<DadosPrevisaoFalta> atributos = new ArrayList<>();
        long[] ids = new long[501];
        for (int i = 0; i < 501; i++) {
            ids[i] = i + 1;
            atributos.add(dados(i + 1));
        }
        servidor.expect(requestTo(URL_LOTE))
                .andRespond(withSuccess("[" + resposta("1", 0.9, "alto") + "]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL_LOTE)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(cliente.prever(atributos, complementos(ids)))
                .allSatisfy(p -> assertThat(p.risco()).isNull())
                .allSatisfy(p -> assertThat(p.nivel()).isNull());
        servidor.verify();
    }

    @Test
    void normalizaAUrl() {
        assertThat(HttpRiscoFaltaClient.normalizarUrl(" http://localhost:8000// ")).isEqualTo("http://localhost:8000");
    }
}
