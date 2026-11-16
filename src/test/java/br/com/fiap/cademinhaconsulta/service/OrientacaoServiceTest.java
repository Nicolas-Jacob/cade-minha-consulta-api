package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.OrientacaoResponse;
import br.com.fiap.cademinhaconsulta.enums.OrigemOrientacao;
import br.com.fiap.cademinhaconsulta.enums.StatusConsulta;
import br.com.fiap.cademinhaconsulta.model.Consulta;
import br.com.fiap.cademinhaconsulta.model.Especialidade;
import br.com.fiap.cademinhaconsulta.model.Fila;
import br.com.fiap.cademinhaconsulta.model.Unidade;
import br.com.fiap.cademinhaconsulta.model.Usuario;
import br.com.fiap.cademinhaconsulta.repository.ConsultaRepository;
import br.com.fiap.cademinhaconsulta.repository.UnidadeContatoEnderecoRepository;
import br.com.fiap.cademinhaconsulta.repository.UsuarioContatoEnderecoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Teste unitario sem Spring, sem banco e sem chamar a IA real: o ChatClient e um mock.
// O ConsultaService e real (com repositorios mockados) para provar que os dados sensiveis
// do paciente nao saem dele e nao chegam ao prompt.
class OrientacaoServiceTest {

    private static final Long ID_CONSULTA = 10L;
    private static final Long ID_PACIENTE = 7L;
    private static final String CPF = "12345678901";
    private static final String SOBRENOME = "Souzinha";
    private static final String SEGUNDO_NOME = "Clarice";
    private static final String NOME_MAE = "Joana Pereira";
    private static final String LOGIN = "mariaclarice";

    private ConsultaService consultaService;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requisicao;
    private ChatClient.CallResponseSpec resposta;

    @BeforeEach
    void preparar() {
        ConsultaRepository consultaRepository = mock(ConsultaRepository.class);
        UnidadeContatoEnderecoRepository unidadeContatoEnderecoRepository =
                mock(UnidadeContatoEnderecoRepository.class);
        when(consultaRepository.findDetalhadaById(ID_CONSULTA)).thenReturn(Optional.of(consultaFutura()));
        when(unidadeContatoEnderecoRepository.buscarVinculosDasUnidades(any())).thenReturn(List.of());
        consultaService = new ConsultaService(consultaRepository, unidadeContatoEnderecoRepository,
                mock(UsuarioContatoEnderecoRepository.class), mock(ApplicationEventPublisher.class));

        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requisicao = mock(ChatClient.ChatClientRequestSpec.class);
        resposta = mock(ChatClient.CallResponseSpec.class);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requisicao);
        when(requisicao.system(anyString())).thenReturn(requisicao);
        when(requisicao.user(anyString())).thenReturn(requisicao);
        when(requisicao.call()).thenReturn(resposta);
    }

    @Test
    void semChaveConfiguradaDevolveTextoPadraoSemChamarIa() {
        OrientacaoService service = criarService(OrientacaoService.CHAVE_NAO_CONFIGURADA, 20);

        OrientacaoResponse orientacao = service.gerar(ID_CONSULTA, ID_PACIENTE, false);

        assertThat(orientacao.origem()).isEqualTo(OrigemOrientacao.PADRAO);
        assertThat(orientacao.aviso()).isEqualTo(OrientacaoResponse.AVISO_PADRAO);
        assertThat(orientacao.idConsulta()).isEqualTo(ID_CONSULTA);
        assertThat(orientacao.mensagem())
                .contains("Maria", "Cardiologia", "UBS Centro", "endereço não informado", "cartão do SUS",
                        "libere a vaga pelo app", "\n• ")
                .doesNotContain("\r");
        verify(chatClient, never()).prompt();
    }

    @Test
    void chaveVaziaTambemDevolveTextoPadrao() {
        OrientacaoService service = criarService("", 20);

        assertThat(service.gerar(ID_CONSULTA, ID_PACIENTE, false).origem()).isEqualTo(OrigemOrientacao.PADRAO);
        verify(chatClient, never()).prompt();
    }

    @Test
    void falhaDaIaDevolveTextoPadrao() {
        when(resposta.content()).thenThrow(new RuntimeException("401 Unauthorized"));
        OrientacaoService service = criarService("chave-de-teste", 20);

        OrientacaoResponse orientacao = service.gerar(ID_CONSULTA, ID_PACIENTE, false);

        assertThat(orientacao.origem()).isEqualTo(OrigemOrientacao.PADRAO);
        assertThat(orientacao.aviso()).isEqualTo(OrientacaoResponse.AVISO_PADRAO);
        assertThat(orientacao.mensagem()).contains("Maria", "Cardiologia");
    }

    @Test
    void timeoutDaIaDevolveTextoPadrao() {
        when(resposta.content()).thenAnswer(invocacao -> {
            Thread.sleep(3000);
            return "resposta atrasada";
        });
        OrientacaoService service = criarService("chave-de-teste", 1);

        OrientacaoResponse orientacao = service.gerar(ID_CONSULTA, ID_PACIENTE, false);

        assertThat(orientacao.origem()).isEqualTo(OrigemOrientacao.PADRAO);
        assertThat(orientacao.mensagem()).doesNotContain("resposta atrasada");
    }

    @Test
    void respostaVaziaDaIaDevolveTextoPadrao() {
        when(resposta.content()).thenReturn("   ");
        OrientacaoService service = criarService("chave-de-teste", 20);

        assertThat(service.gerar(ID_CONSULTA, ID_PACIENTE, false).origem()).isEqualTo(OrigemOrientacao.PADRAO);
    }

    @Test
    void iaRespondendoDevolveOrigemIa() {
        when(resposta.content()).thenReturn("  Olá, Maria! Sua consulta de Cardiologia está chegando.  ");
        OrientacaoService service = criarService("chave-de-teste", 20);

        OrientacaoResponse orientacao = service.gerar(ID_CONSULTA, ID_PACIENTE, true);

        assertThat(orientacao.origem()).isEqualTo(OrigemOrientacao.IA);
        assertThat(orientacao.aviso()).isEqualTo(OrientacaoResponse.AVISO_IA);
        assertThat(orientacao.mensagem()).isEqualTo("Olá, Maria! Sua consulta de Cardiologia está chegando.");
    }

    @Test
    void promptTemDadosDaConsultaESemDadosSensiveis() {
        when(resposta.content()).thenReturn("texto da IA");
        OrientacaoService service = criarService("chave-de-teste", 20);

        service.gerar(ID_CONSULTA, ID_PACIENTE, false);

        ArgumentCaptor<String> sistema = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> usuario = ArgumentCaptor.forClass(String.class);
        verify(requisicao).system(sistema.capture());
        verify(requisicao).user(usuario.capture());
        String promptCompleto = sistema.getValue() + "\n" + usuario.getValue();

        assertThat(usuario.getValue())
                .contains("Maria", "Cardiologia", "Doencas do coracao", "UBS Centro", "agendada", "08:00");
        assertThat(promptCompleto)
                .doesNotContain(CPF, SOBRENOME, SEGUNDO_NOME, NOME_MAE, LOGIN);
        assertThat(sistema.getValue())
                .contains("Nunca faça diagnóstico", "medicamentos", "Não invente dados", "sem markdown",
                        "150 palavras", "libere a vaga pelo app");
    }

    private OrientacaoService criarService(String apiKey, long timeoutSegundos) {
        return new OrientacaoService(consultaService, chatClientBuilder, apiKey, timeoutSegundos);
    }

    // Consulta futura de uma paciente com todos os dados sensiveis preenchidos
    private static Consulta consultaFutura() {
        Usuario paciente = new Usuario();
        paciente.setId(ID_PACIENTE);
        paciente.setNome("Maria " + SEGUNDO_NOME);
        paciente.setSobrenome(SOBRENOME);
        paciente.setCpf(CPF);
        paciente.setNomeMae(NOME_MAE);
        paciente.setLogin(LOGIN);

        Especialidade especialidade = new Especialidade();
        especialidade.setId(3L);
        especialidade.setNome("Cardiologia");
        especialidade.setDescricao("Doencas do coracao");

        Fila fila = new Fila();
        fila.setId(5L);
        fila.setEspecialidade(especialidade);

        Unidade unidade = new Unidade();
        unidade.setId(2L);
        unidade.setNome("UBS Centro");

        Consulta consulta = new Consulta();
        consulta.setId(ID_CONSULTA);
        consulta.setUsuario(paciente);
        consulta.setFila(fila);
        consulta.setUnidade(unidade);
        consulta.setStatus(StatusConsulta.AGENDADA);
        consulta.setDataHora(LocalDateTime.now().plusDays(10).withHour(8).withMinute(0).withSecond(0).withNano(0));
        return consulta;
    }
}
