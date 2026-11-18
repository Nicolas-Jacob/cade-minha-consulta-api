package br.com.fiap.cademinhaconsulta.risco;

import br.com.fiap.cademinhaconsulta.enums.NivelRisco;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Cliente do servico de risco de falta (vaga-certa-ia, FastAPI): POST {url}/v1/risco-falta/lote.
// Qualquer falha (conexao, timeout, 401, 422, 503, 500, JSON invalido) vira risco e nivel null para todas as
// consultas, sem excecao: o endpoint do GESTOR continua respondendo 200.
@Slf4j
public class HttpRiscoFaltaClient implements RiscoFaltaClient {

    static final String CAMINHO_LOTE = "/v1/risco-falta/lote";
    static final String HEADER_TOKEN = "X-Service-Token";
    // Limite do servico por chamada
    static final int TAMANHO_LOTE = 500;

    private final RestClient restClient;

    public HttpRiscoFaltaClient(RestClient restClient) {
        this.restClient = restClient;
    }

    // Cliente de producao: URL normalizada, token opcional e timeouts de conexao e leitura
    public static HttpRiscoFaltaClient criar(String url, String token, int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        fabrica.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return new HttpRiscoFaltaClient(configurar(RestClient.builder(), url, token).requestFactory(fabrica).build());
    }

    // URL base e header do token (so quando preenchido). Separado para os testes usarem o MockRestServiceServer.
    static RestClient.Builder configurar(RestClient.Builder builder, String url, String token) {
        builder.baseUrl(normalizarUrl(url));
        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HEADER_TOKEN, token.trim());
        }
        return builder;
    }

    // Sem espacos e sem barra no final
    static String normalizarUrl(String url) {
        String normalizada = url.trim();
        while (normalizada.endsWith("/")) {
            normalizada = normalizada.substring(0, normalizada.length() - 1);
        }
        return normalizada;
    }

    @Override
    public List<PrevisaoRisco> prever(List<DadosPrevisaoFalta> atributos, Map<Long, ComplementoPrevisao> complementos) {
        if (atributos.isEmpty()) {
            return List.of();
        }

        // Item invalido nao vai no lote (derrubaria todos com 422): fica com risco e nivel null
        List<ItemRiscoRequest> itens = new ArrayList<>();
        for (DadosPrevisaoFalta dados : atributos) {
            MapeadorRiscoFalta.paraItem(dados, complementos.get(dados.idConsulta()))
                    .ifPresentOrElse(itens::add, () -> log.warn(
                            "Consulta {} fora do lote de risco de falta: dados incompletos ou invalidos",
                            dados.idConsulta()));
        }

        Map<String, ItemRiscoResponse> respostas = enviar(itens);
        return atributos.stream()
                .map(dados -> paraPrevisao(dados.idConsulta(), respostas.get(String.valueOf(dados.idConsulta()))))
                .toList();
    }

    // Respostas por consulta_id. Vazio se nao ha itens ou se qualquer lote falhar (sem lotes parciais).
    private Map<String, ItemRiscoResponse> enviar(List<ItemRiscoRequest> itens) {
        if (itens.isEmpty()) {
            return Map.of();
        }
        long inicio = System.nanoTime();
        Map<String, ItemRiscoResponse> respostas = new HashMap<>();
        Set<String> modelos = new LinkedHashSet<>();
        try {
            for (int i = 0; i < itens.size(); i += TAMANHO_LOTE) {
                List<ItemRiscoRequest> lote = itens.subList(i, Math.min(i + TAMANHO_LOTE, itens.size()));
                List<ItemRiscoResponse> resposta = restClient.post()
                        .uri(CAMINHO_LOTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(new LoteRiscoRequest(lote))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<ItemRiscoResponse>>() {
                        });
                if (resposta == null) {
                    throw new IllegalStateException("resposta sem corpo");
                }
                for (ItemRiscoResponse item : resposta) {
                    if (item != null && item.consultaId() != null) {
                        respostas.putIfAbsent(item.consultaId(), item);
                        if (item.modelo() != null) {
                            modelos.add(item.modelo());
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            log.warn("Risco de falta indisponivel: {}", motivo(e));
            return Map.of();
        }
        long milissegundos = (System.nanoTime() - inicio) / 1_000_000;
        log.info("Risco de falta: {} consulta(s) enviada(s) em {} ms, modelo {}", itens.size(), milissegundos,
                modelos.isEmpty() ? "desconhecido" : String.join(", ", modelos));
        return respostas;
    }

    // Motivo para o log, sem o corpo da resposta (o 422 ecoa os dados enviados) e sem o token
    static String motivo(Throwable erro) {
        if (erro instanceof RestClientResponseException resposta) {
            return "HTTP " + resposta.getStatusCode().value();
        }
        Throwable causa = erro;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String motivo = erro.getClass().getSimpleName();
        if (causa != erro) {
            motivo += " (" + causa.getClass().getSimpleName();
            // Mensagens de E/S sao do tipo "Connection refused" ou "Read timed out"; as de JSON podem trazer trechos
            // da resposta, entao ficam de fora
            if (causa instanceof IOException && causa.getMessage() != null) {
                motivo += ": " + causa.getMessage();
            }
            motivo += ")";
        }
        return motivo;
    }

    private static PrevisaoRisco paraPrevisao(Long idConsulta, ItemRiscoResponse resposta) {
        if (resposta == null) {
            return new PrevisaoRisco(idConsulta, null, null);
        }
        return new PrevisaoRisco(idConsulta, risco(resposta.probabilidadeFalta()), nivel(resposta.faixaRisco()));
    }

    // 0 a 1 com 4 casas; fora do intervalo = null
    static Double risco(Double probabilidade) {
        if (probabilidade == null || probabilidade.isNaN() || probabilidade < 0 || probabilidade > 1) {
            return null;
        }
        return BigDecimal.valueOf(probabilidade).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    // Usa a faixa do proprio modelo (que ja considera a faixa etaria), sem recalcular pelo risco
    static NivelRisco nivel(String faixaRisco) {
        if (faixaRisco == null) {
            return null;
        }
        return switch (faixaRisco.trim().toLowerCase(Locale.ROOT)) {
            case "baixo" -> NivelRisco.BAIXO;
            case "medio" -> NivelRisco.MEDIO;
            case "alto" -> NivelRisco.ALTO;
            default -> null;
        };
    }
}
