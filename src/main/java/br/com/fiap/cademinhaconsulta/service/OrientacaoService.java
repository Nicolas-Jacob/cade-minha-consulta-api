package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.DadosOrientacao;
import br.com.fiap.cademinhaconsulta.dto.OrientacaoResponse;
import br.com.fiap.cademinhaconsulta.enums.OrigemOrientacao;
import br.com.fiap.cademinhaconsulta.enums.StatusConsulta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Orientacoes da consulta com Spring AI (secao 6 do contrato). Nao e chat livre: o prompt e montado so com os
// dados da consulta (DadosOrientacao). Se a IA falhar, devolve o texto padrao (origem PADRAO), nunca 500.
// Sem @Transactional de proposito: a chamada a IA pode levar segundos e nao deve prender conexao do banco.
@Slf4j
@Service
public class OrientacaoService {

    // Valor do application.yml quando AI_API_KEY nao foi definida (so para a aplicacao subir)
    static final String CHAVE_NAO_CONFIGURADA = "nao-configurada";

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy 'às' HH:mm", Locale.of("pt", "BR"));

    private final ConsultaService consultaService;
    private final ChatClient chatClient;
    private final boolean chaveConfigurada;
    private final long timeoutSegundos;

    private final String promptSistema = ler("prompts/orientacao-sistema.st");
    private final PromptTemplate promptUsuario = template("prompts/orientacao-usuario.st");
    private final PromptTemplate textoPadrao = template("prompts/orientacao-padrao.st");
    private final PromptTemplate textoPadraoEncerrada = template("prompts/orientacao-padrao-encerrada.st");

    public OrientacaoService(ConsultaService consultaService,
                             ChatClient.Builder chatClientBuilder,
                             @Value("${spring.ai.openai.api-key:}") String apiKey,
                             @Value("${orientacao.timeout-segundos:20}") long timeoutSegundos) {
        this.consultaService = consultaService;
        this.chatClient = chatClientBuilder.build();
        this.chaveConfigurada = apiKey != null && !apiKey.isBlank() && !CHAVE_NAO_CONFIGURADA.equals(apiKey);
        this.timeoutSegundos = timeoutSegundos;
    }

    // Dono ou GESTOR (404/403 vem do ConsultaService). Tenta a IA; se nao der, usa o texto padrao.
    public OrientacaoResponse gerar(Long idConsulta, Long idUsuario, boolean gestor) {
        DadosOrientacao dados = consultaService.dadosParaOrientacao(idConsulta, idUsuario, gestor);

        String mensagem = null;
        if (!chaveConfigurada) {
            log.warn("Orientacao da consulta {}: chave da IA nao configurada (AI_API_KEY). Usando texto padrao.",
                    idConsulta);
        } else {
            mensagem = chamarIa(dados);
        }

        OrigemOrientacao origem = mensagem != null ? OrigemOrientacao.IA : OrigemOrientacao.PADRAO;
        if (mensagem == null) {
            mensagem = montarTextoPadrao(dados);
        }
        return new OrientacaoResponse(idConsulta, mensagem, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                origem, OrientacaoResponse.aviso(origem));
    }

    // Mensagem do usuario enviada ao modelo: so os campos de DadosOrientacao, ja formatados
    String montarPromptUsuario(DadosOrientacao dados) {
        return renderizar(promptUsuario, Map.<String, Object>of(
                "primeiroNome", dados.primeiroNome(),
                "especialidade", dados.especialidade(),
                "descricaoEspecialidade", dados.descricaoEspecialidade(),
                "dataHora", FORMATO_DATA_HORA.format(dados.dataHora()),
                "diasAteConsulta", dados.diasAteConsulta(),
                "unidade", dados.unidade(),
                "enderecoUnidade", endereco(dados),
                "status", situacao(dados.status())));
    }

    // Texto de reserva montado com os mesmos dados. Consulta encerrada nao recebe orientacao de preparo.
    String montarTextoPadrao(DadosOrientacao dados) {
        boolean ativa = (dados.status() == StatusConsulta.AGENDADA || dados.status() == StatusConsulta.CONFIRMADA)
                && dados.dataHora().isAfter(LocalDateTime.now());
        PromptTemplate template = ativa ? textoPadrao : textoPadraoEncerrada;
        return renderizar(template, Map.<String, Object>of(
                "primeiroNome", dados.primeiroNome(),
                "especialidade", dados.especialidade(),
                "dataHora", FORMATO_DATA_HORA.format(dados.dataHora()),
                "prazo", prazo(dados.diasAteConsulta()),
                "unidade", dados.unidade(),
                "enderecoUnidade", endereco(dados),
                "status", situacao(dados.status())));
    }

    // O StringTemplate usa a quebra de linha do sistema (\r\n no Windows); a API devolve sempre \n
    private static String renderizar(PromptTemplate template, Map<String, Object> variaveis) {
        return template.render(variaveis).replace("\r\n", "\n").strip();
    }

    // Chama o modelo com limite de tempo. Qualquer falha (excecao, timeout ou resposta vazia) devolve null.
    private String chamarIa(DadosOrientacao dados) {
        String promptUsuarioRenderizado = montarPromptUsuario(dados);
        CompletableFuture<String> chamada = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                .system(promptSistema)
                .user(promptUsuarioRenderizado)
                .call()
                .content());
        try {
            String resposta = chamada.get(timeoutSegundos, TimeUnit.SECONDS);
            if (resposta == null || resposta.isBlank()) {
                log.warn("Orientacao da consulta {}: IA devolveu resposta vazia. Usando texto padrao.",
                        dados.idConsulta());
                return null;
            }
            return resposta.strip();
        } catch (TimeoutException ex) {
            chamada.cancel(true);
            log.warn("Orientacao da consulta {}: IA nao respondeu em {} s. Usando texto padrao.",
                    dados.idConsulta(), timeoutSegundos);
        } catch (ExecutionException ex) {
            Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
            log.warn("Orientacao da consulta {}: falha ao chamar a IA ({}: {}). Usando texto padrao.",
                    dados.idConsulta(), causa.getClass().getSimpleName(), causa.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Orientacao da consulta {}: chamada a IA interrompida. Usando texto padrao.",
                    dados.idConsulta());
        }
        return null;
    }

    private static String endereco(DadosOrientacao dados) {
        return dados.enderecoUnidade() != null ? dados.enderecoUnidade() : "endereço não informado";
    }

    // Situacao em linguagem simples (vai para o prompt e para o texto padrao)
    private static String situacao(StatusConsulta status) {
        return switch (status) {
            case AGENDADA -> "agendada";
            case CONFIRMADA -> "confirmada";
            case CANCELADA -> "cancelada";
            case PRESENTE -> "marcada como realizada";
            case FALTA -> "marcada como falta";
        };
    }

    private static String prazo(long dias) {
        if (dias == 0) {
            return "hoje";
        }
        return dias == 1 ? "amanhã" : "daqui a " + dias + " dias";
    }

    private static PromptTemplate template(String caminho) {
        return new PromptTemplate(new ClassPathResource(caminho));
    }

    private static String ler(String caminho) {
        try {
            return new ClassPathResource(caminho).getContentAsString(StandardCharsets.UTF_8)
                    .replace("\r\n", "\n").strip();
        } catch (IOException ex) {
            throw new UncheckedIOException("Template de prompt nao encontrado: " + caminho, ex);
        }
    }
}
