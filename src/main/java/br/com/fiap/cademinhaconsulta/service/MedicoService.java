package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.MedicoRequest;
import br.com.fiap.cademinhaconsulta.dto.MedicoResponse;
import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.enums.StatusMedico;
import br.com.fiap.cademinhaconsulta.exception.ConflitoException;
import br.com.fiap.cademinhaconsulta.exception.RecursoNaoEncontradoException;
import br.com.fiap.cademinhaconsulta.model.Especialidade;
import br.com.fiap.cademinhaconsulta.model.Medico;
import br.com.fiap.cademinhaconsulta.model.Unidade;
import br.com.fiap.cademinhaconsulta.repository.EspecialidadeRepository;
import br.com.fiap.cademinhaconsulta.repository.MedicoRepository;
import br.com.fiap.cademinhaconsulta.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final UnidadeRepository unidadeRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<MedicoResponse> listar(Long idEspecialidade, Long idUnidade, Pageable pageable) {
        return PaginaResponse.de(medicoRepository.buscar(idEspecialidade, idUnidade, pageable).map(MedicoResponse::de));
    }

    @Transactional(readOnly = true)
    public MedicoResponse buscar(Long id) {
        return MedicoResponse.de(buscarMedico(id));
    }

    @Transactional
    public MedicoResponse criar(MedicoRequest request) {
        Medico medico = new Medico();
        medico.setStatus(request.status() != null ? request.status() : StatusMedico.ATIVO);
        preencher(medico, request, null);
        return MedicoResponse.de(medicoRepository.save(medico));
    }

    @Transactional
    public MedicoResponse atualizar(Long id, MedicoRequest request) {
        Medico medico = buscarMedico(id);
        if (request.status() != null) {
            medico.setStatus(request.status());
        }
        preencher(medico, request, id);
        return MedicoResponse.de(medico);
    }

    // DELETE logico: status INATIVO
    @Transactional
    public void inativar(Long id) {
        buscarMedico(id).setStatus(StatusMedico.INATIVO);
    }

    private Medico buscarMedico(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Medico nao encontrado."));
    }

    // Duplicado = mesmo CRM + mesma UF (ignorando o proprio medico no PUT)
    private void preencher(Medico medico, MedicoRequest request, Long idIgnorado) {
        String crm = request.crm().trim();
        if (medicoRepository.existeCrm(crm, request.ufCrm(), idIgnorado)) {
            throw new ConflitoException("Ja existe medico cadastrado com este CRM nesta UF.");
        }
        medico.setNome(request.nome().trim());
        medico.setSobrenome(request.sobrenome().trim());
        medico.setCrm(crm);
        medico.setUfCrm(request.ufCrm());
        medico.setEspecialidades(buscarEspecialidades(request.idsEspecialidades()));
        medico.setUnidades(buscarUnidades(request.idsUnidades()));
    }

    private Set<Especialidade> buscarEspecialidades(List<Long> ids) {
        Set<Long> idsUnicos = new LinkedHashSet<>(ids);
        List<Especialidade> encontradas = especialidadeRepository.findAllById(idsUnicos);
        if (encontradas.size() != idsUnicos.size()) {
            idsUnicos.removeAll(encontradas.stream().map(Especialidade::getId).toList());
            throw new RecursoNaoEncontradoException("Especialidade nao encontrada: " + idsUnicos);
        }
        return new HashSet<>(encontradas);
    }

    private Set<Unidade> buscarUnidades(List<Long> ids) {
        Set<Long> idsUnicos = new LinkedHashSet<>(ids);
        List<Unidade> encontradas = unidadeRepository.findAllById(idsUnicos);
        if (encontradas.size() != idsUnicos.size()) {
            idsUnicos.removeAll(encontradas.stream().map(Unidade::getId).toList());
            throw new RecursoNaoEncontradoException("Unidade nao encontrada: " + idsUnicos);
        }
        return new HashSet<>(encontradas);
    }
}
