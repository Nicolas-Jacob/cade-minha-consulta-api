package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.EspecialidadeRequest;
import br.com.fiap.cademinhaconsulta.dto.EspecialidadeResponse;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.exception.ConflitoException;
import br.com.fiap.cademinhaconsulta.exception.RecursoNaoEncontradoException;
import br.com.fiap.cademinhaconsulta.model.Especialidade;
import br.com.fiap.cademinhaconsulta.repository.EspecialidadeRepository;
import br.com.fiap.cademinhaconsulta.repository.FilaRepository;
import br.com.fiap.cademinhaconsulta.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EspecialidadeService {

    private final EspecialidadeRepository especialidadeRepository;
    private final MedicoRepository medicoRepository;
    private final FilaRepository filaRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<EspecialidadeResponse> listar(Pageable pageable) {
        return PaginaResponse.de(especialidadeRepository.findAll(pageable).map(EspecialidadeResponse::de));
    }

    @Transactional(readOnly = true)
    public EspecialidadeResponse buscar(Long id) {
        return EspecialidadeResponse.de(buscarEspecialidade(id));
    }

    @Transactional
    public EspecialidadeResponse criar(EspecialidadeRequest request) {
        Especialidade especialidade = new Especialidade();
        copiar(request, especialidade);
        return EspecialidadeResponse.de(especialidadeRepository.save(especialidade));
    }

    @Transactional
    public EspecialidadeResponse atualizar(Long id, EspecialidadeRequest request) {
        Especialidade especialidade = buscarEspecialidade(id);
        copiar(request, especialidade);
        return EspecialidadeResponse.de(especialidade);
    }

    // Exclusao fisica, bloqueada se a especialidade estiver em uso
    @Transactional
    public void excluir(Long id) {
        Especialidade especialidade = buscarEspecialidade(id);
        if (medicoRepository.existsByEspecialidades_Id(id)) {
            throw new ConflitoException("Especialidade vinculada a medicos. Remova o vinculo antes de excluir.");
        }
        if (filaRepository.existsByEspecialidade_Id(id)) {
            throw new ConflitoException("Especialidade possui solicitacoes na fila e nao pode ser excluida.");
        }
        especialidadeRepository.delete(especialidade);
    }

    private Especialidade buscarEspecialidade(Long id) {
        return especialidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Especialidade nao encontrada."));
    }

    private void copiar(EspecialidadeRequest request, Especialidade especialidade) {
        especialidade.setNome(request.nome().trim());
        especialidade.setDescricao(request.descricao().trim());
    }
}
