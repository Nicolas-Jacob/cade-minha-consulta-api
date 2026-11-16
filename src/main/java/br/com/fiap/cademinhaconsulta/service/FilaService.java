package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.FilaRequest;
import br.com.fiap.cademinhaconsulta.dto.FilaResponse;
import br.com.fiap.cademinhaconsulta.dto.FilaUpdateRequest;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.dto.PrioridadeRequest;
import br.com.fiap.cademinhaconsulta.enums.StatusFila;
import br.com.fiap.cademinhaconsulta.enums.StatusUnidade;
import br.com.fiap.cademinhaconsulta.exception.AcessoNegadoException;
import br.com.fiap.cademinhaconsulta.exception.ConflitoException;
import br.com.fiap.cademinhaconsulta.exception.RecursoNaoEncontradoException;
import br.com.fiap.cademinhaconsulta.exception.RegraNegocioException;
import br.com.fiap.cademinhaconsulta.model.Especialidade;
import br.com.fiap.cademinhaconsulta.model.Fila;
import br.com.fiap.cademinhaconsulta.model.Unidade;
import br.com.fiap.cademinhaconsulta.repository.EspecialidadeRepository;
import br.com.fiap.cademinhaconsulta.repository.FilaRepository;
import br.com.fiap.cademinhaconsulta.repository.UnidadeRepository;
import br.com.fiap.cademinhaconsulta.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// Regras da secao 6 do contrato. Ordem das validacoes: 404, depois 422, depois 409.
@Service
@RequiredArgsConstructor
public class FilaService {

    private static final long PRIORIDADE_INICIAL = 1L;

    private final FilaRepository filaRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public FilaResponse criar(Long idUsuario, FilaRequest request) {
        Especialidade especialidade = especialidadeRepository.findById(request.idEspecialidade())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Especialidade nao encontrada."));
        Unidade unidade = buscarUnidadeAtiva(request.idUnidade());
        if (filaRepository.existsByUsuario_IdAndEspecialidade_IdAndStatus(
                idUsuario, especialidade.getId(), StatusFila.AGUARDANDO)) {
            throw new ConflitoException("Voce ja tem uma solicitacao aguardando para esta especialidade.");
        }

        Fila fila = new Fila();
        fila.setUsuario(usuarioRepository.getReferenceById(idUsuario));
        fila.setEspecialidade(especialidade);
        fila.setUnidade(unidade);
        fila.setPrioridade(PRIORIDADE_INICIAL);
        // DATE do Oracle guarda ate segundos
        fila.setDataEntrada(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        fila.setStatus(StatusFila.AGUARDANDO);
        return montarResposta(filaRepository.save(fila));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<FilaResponse> listarMinhas(Long idUsuario, Pageable pageable) {
        return PaginaResponse.de(filaRepository.findByUsuario_Id(idUsuario, pageable).map(this::montarResposta));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<FilaResponse> listar(StatusFila status, Long idEspecialidade, Long idUnidade,
                                               Pageable pageable) {
        return PaginaResponse.de(filaRepository.buscar(status, idEspecialidade, idUnidade, pageable)
                .map(this::montarResposta));
    }

    // Dono ou GESTOR; outro cidadao recebe 403
    @Transactional(readOnly = true)
    public FilaResponse buscar(Long id, Long idUsuario, boolean gestor) {
        Fila fila = buscarFila(id);
        if (!gestor) {
            validarDono(fila, idUsuario);
        }
        return montarResposta(fila);
    }

    // Troca de unidade pelo dono, so enquanto AGUARDANDO
    @Transactional
    public FilaResponse atualizar(Long id, Long idUsuario, FilaUpdateRequest request) {
        Fila fila = buscarFila(id);
        validarDono(fila, idUsuario);
        Unidade unidade = buscarUnidadeAtiva(request.idUnidade());
        validarAguardando(fila);

        fila.setUnidade(unidade);
        return montarResposta(fila);
    }

    // Desistencia pelo dono: CANCELADA + data_saida
    @Transactional
    public void cancelar(Long id, Long idUsuario) {
        Fila fila = buscarFila(id);
        validarDono(fila, idUsuario);
        validarAguardando(fila);

        fila.setStatus(StatusFila.CANCELADA);
        fila.setDataSaida(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    @Transactional
    public FilaResponse alterarPrioridade(Long id, PrioridadeRequest request) {
        Fila fila = buscarFila(id);
        validarAguardando(fila);

        fila.setPrioridade(request.prioridade());
        return montarResposta(fila);
    }

    private Fila buscarFila(Long id) {
        return filaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao nao encontrada."));
    }

    private Unidade buscarUnidadeAtiva(Long idUnidade) {
        Unidade unidade = unidadeRepository.findById(idUnidade)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade nao encontrada."));
        if (unidade.getStatus() != StatusUnidade.ATIVO) {
            throw new RegraNegocioException("Unidade inativa. Escolha outra unidade.");
        }
        return unidade;
    }

    private void validarDono(Fila fila, Long idUsuario) {
        if (!fila.getUsuario().getId().equals(idUsuario)) {
            throw new AcessoNegadoException("Esta solicitacao pertence a outro usuario.");
        }
    }

    private void validarAguardando(Fila fila) {
        if (fila.getStatus() != StatusFila.AGUARDANDO) {
            throw new ConflitoException("So e possivel alterar solicitacoes com status AGUARDANDO. Status atual: "
                    + fila.getStatus() + ".");
        }
    }

    // posicaoEstimada = 1 + filas AGUARDANDO na mesma especialidade e unidade com data_entrada anterior
    private FilaResponse montarResposta(Fila fila) {
        Long posicao = null;
        if (fila.getStatus() == StatusFila.AGUARDANDO) {
            posicao = 1 + filaRepository.contarAFrente(
                    fila.getEspecialidade().getId(), fila.getUnidade().getId(), fila.getDataEntrada());
        }
        return FilaResponse.de(fila, posicao);
    }
}
