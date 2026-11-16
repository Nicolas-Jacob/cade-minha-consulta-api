package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.ConsultaResponse;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.dto.PresencaRequest;
import br.com.fiap.cademinhaconsulta.dto.UnidadeResponse;
import br.com.fiap.cademinhaconsulta.enums.ObsContato;
import br.com.fiap.cademinhaconsulta.enums.StatusConsulta;
import br.com.fiap.cademinhaconsulta.enums.StatusFila;
import br.com.fiap.cademinhaconsulta.exception.AcessoNegadoException;
import br.com.fiap.cademinhaconsulta.exception.ConflitoException;
import br.com.fiap.cademinhaconsulta.exception.RecursoNaoEncontradoException;
import br.com.fiap.cademinhaconsulta.messaging.VagaLiberadaEvent;
import br.com.fiap.cademinhaconsulta.model.Consulta;
import br.com.fiap.cademinhaconsulta.model.Fila;
import br.com.fiap.cademinhaconsulta.model.UnidadeContatoEndereco;
import br.com.fiap.cademinhaconsulta.model.UsuarioContatoEndereco;
import br.com.fiap.cademinhaconsulta.repository.ConsultaRepository;
import br.com.fiap.cademinhaconsulta.repository.UnidadeContatoEnderecoRepository;
import br.com.fiap.cademinhaconsulta.repository.UsuarioContatoEnderecoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Regras da secao 6 do contrato (Consultas e Gestor). Ordem das validacoes: 404, depois 403, depois 409.
@Service
@RequiredArgsConstructor
public class ConsultaService {

    private static final Set<StatusConsulta> ATIVAS = Set.of(StatusConsulta.AGENDADA, StatusConsulta.CONFIRMADA);

    private final ConsultaRepository consultaRepository;
    private final UnidadeContatoEnderecoRepository unidadeContatoEnderecoRepository;
    private final UsuarioContatoEnderecoRepository usuarioContatoEnderecoRepository;
    private final ApplicationEventPublisher eventPublisher;

    // futuras: AGENDADA/CONFIRMADA com data futura (padrao dataHora,asc). historico: o resto (padrao dataHora,desc)
    @Transactional(readOnly = true)
    public PaginaResponse<ConsultaResponse> listarMinhas(Long idUsuario, boolean futuras, Pageable pageable) {
        LocalDateTime agora = LocalDateTime.now();
        Page<Consulta> pagina;
        if (futuras) {
            pagina = consultaRepository.buscarFuturas(idUsuario, agora, ordenar(pageable, Sort.Direction.ASC));
        } else {
            pagina = consultaRepository.buscarHistorico(idUsuario, agora, ordenar(pageable, Sort.Direction.DESC));
        }
        return montarPagina(pagina, false);
    }

    // Dono ou GESTOR; outro cidadao recebe 403. O GESTOR recebe tambem o paciente.
    @Transactional(readOnly = true)
    public ConsultaResponse buscar(Long id, Long idUsuario, boolean gestor) {
        Consulta consulta = buscarConsulta(id);
        if (!gestor) {
            validarDono(consulta, idUsuario);
        }
        return montarResposta(consulta, gestor);
    }

    // AGENDADA e futura -> CONFIRMADA
    @Transactional
    public ConsultaResponse confirmar(Long id, Long idUsuario) {
        Consulta consulta = buscarConsulta(id);
        validarDono(consulta, idUsuario);
        if (consulta.getStatus() != StatusConsulta.AGENDADA || !ehFutura(consulta)) {
            throw new ConflitoException("So e possivel confirmar consultas AGENDADA com data futura. Status atual: "
                    + consulta.getStatus() + ".");
        }

        consulta.setStatus(StatusConsulta.CONFIRMADA);
        return montarResposta(consulta, false);
    }

    // AGENDADA/CONFIRMADA futura -> CANCELADA; a fila da consulta tambem vira CANCELADA.
    // O evento vaga.liberada so vai para o RabbitMQ depois do commit (VagaLiberadaPublisher).
    @Transactional
    public ConsultaResponse cancelar(Long id, Long idUsuario) {
        Consulta consulta = buscarConsulta(id);
        validarDono(consulta, idUsuario);
        if (!ATIVAS.contains(consulta.getStatus()) || !ehFutura(consulta)) {
            throw new ConflitoException("So e possivel cancelar consultas AGENDADA ou CONFIRMADA com data futura. "
                    + "Status atual: " + consulta.getStatus() + ".");
        }

        consulta.setStatus(StatusConsulta.CANCELADA);
        Fila fila = consulta.getFila();
        fila.setStatus(StatusFila.CANCELADA);
        fila.setDataSaida(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        eventPublisher.publishEvent(new VagaLiberadaEvent(
                consulta.getId(),
                consulta.getMedico().getId(),
                consulta.getUnidade().getId(),
                fila.getEspecialidade().getId(),
                consulta.getDataHora()));
        return montarResposta(consulta, false);
    }

    // GESTOR: AGENDADA/CONFIRMADA nos proximos "dias" dias (padrao dataHora,asc), com o paciente
    @Transactional(readOnly = true)
    public PaginaResponse<ConsultaResponse> listarProximas(int dias, Pageable pageable) {
        LocalDateTime agora = LocalDateTime.now();
        return montarPagina(consultaRepository.buscarProximas(agora, agora.plusDays(dias),
                ordenar(pageable, Sort.Direction.ASC)), true);
    }

    // GESTOR: registra PRESENTE ou FALTA em consulta AGENDADA/CONFIRMADA cuja data ja passou
    @Transactional
    public ConsultaResponse registrarPresenca(Long id, PresencaRequest request) {
        Consulta consulta = buscarConsulta(id);
        if (!ATIVAS.contains(consulta.getStatus()) || ehFutura(consulta)) {
            throw new ConflitoException("So e possivel registrar presenca em consultas AGENDADA ou CONFIRMADA "
                    + "cuja data ja passou. Status atual: " + consulta.getStatus() + ".");
        }

        consulta.setStatus(StatusConsulta.valueOf(request.status().name()));
        return montarResposta(consulta, true);
    }

    private Consulta buscarConsulta(Long id) {
        return consultaRepository.findDetalhadaById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta nao encontrada."));
    }

    private void validarDono(Consulta consulta, Long idUsuario) {
        if (!consulta.getUsuario().getId().equals(idUsuario)) {
            throw new AcessoNegadoException("Esta consulta pertence a outro usuario.");
        }
    }

    private static boolean ehFutura(Consulta consulta) {
        return consulta.getDataHora().isAfter(LocalDateTime.now());
    }

    // Sem ?sort= do cliente, ordena por dataHora na direcao padrao do endpoint
    private static Pageable ordenar(Pageable pageable, Sort.Direction direcaoPadrao) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(direcaoPadrao, "dataHora"));
    }

    private ConsultaResponse montarResposta(Consulta consulta, boolean comPaciente) {
        return montarLista(List.of(consulta), comPaciente).getFirst();
    }

    private PaginaResponse<ConsultaResponse> montarPagina(Page<Consulta> pagina, boolean comPaciente) {
        List<ConsultaResponse> respostas = montarLista(pagina.getContent(), comPaciente);
        return new PaginaResponse<>(respostas, pagina.getTotalElements(), pagina.getTotalPages(),
                pagina.getNumber(), pagina.getSize());
    }

    // Busca enderecos das unidades e telefones dos pacientes de uma vez para toda a lista
    private List<ConsultaResponse> montarLista(List<Consulta> consultas, boolean comPaciente) {
        Map<Long, String> enderecos = buscarEnderecos(consultas);
        Map<Long, String> telefones = comPaciente ? buscarTelefones(consultas) : Map.of();
        return consultas.stream()
                .map(c -> ConsultaResponse.de(c,
                        enderecos.get(c.getUnidade().getId()),
                        comPaciente ? ConsultaResponse.paciente(c.getUsuario(), telefones.get(c.getUsuario().getId()))
                                : null))
                .toList();
    }

    // Endereco formatado de cada unidade (vinculo de menor id_contato, como no UnidadeService)
    private Map<Long, String> buscarEnderecos(List<Consulta> consultas) {
        Map<Long, String> enderecos = new HashMap<>();
        List<Long> idsUnidades = consultas.stream().map(c -> c.getUnidade().getId()).distinct().toList();
        if (idsUnidades.isEmpty()) {
            return enderecos;
        }
        for (UnidadeContatoEndereco v : unidadeContatoEnderecoRepository.buscarVinculosDasUnidades(idsUnidades)) {
            enderecos.putIfAbsent(v.getUnidade().getId(), UnidadeResponse.formatar(v.getEndereco()));
        }
        return enderecos;
    }

    // Telefone de cada paciente: contato PROPRIO; se nao houver, o do primeiro vinculo (menor id_contato)
    private Map<Long, String> buscarTelefones(List<Consulta> consultas) {
        Map<Long, String> telefones = new HashMap<>();
        List<Long> idsUsuarios = consultas.stream().map(c -> c.getUsuario().getId()).distinct().toList();
        if (idsUsuarios.isEmpty()) {
            return telefones;
        }
        List<UsuarioContatoEndereco> vinculos = usuarioContatoEnderecoRepository.buscarVinculosDosUsuarios(idsUsuarios);
        for (UsuarioContatoEndereco v : vinculos) {
            if (v.getContato().getObs() == ObsContato.PROPRIO) {
                telefones.putIfAbsent(v.getUsuario().getId(), v.getContato().getTelefone());
            }
        }
        for (UsuarioContatoEndereco v : vinculos) {
            telefones.putIfAbsent(v.getUsuario().getId(), v.getContato().getTelefone());
        }
        return telefones;
    }
}
