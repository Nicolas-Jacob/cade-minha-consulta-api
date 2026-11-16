package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.PaginaResponse;
import br.com.fiap.cademinhaconsulta.dto.UnidadeRequest;
import br.com.fiap.cademinhaconsulta.dto.UnidadeResponse;
import br.com.fiap.cademinhaconsulta.enums.ObsContato;
import br.com.fiap.cademinhaconsulta.enums.StatusUnidade;
import br.com.fiap.cademinhaconsulta.exception.RecursoNaoEncontradoException;
import br.com.fiap.cademinhaconsulta.model.Contato;
import br.com.fiap.cademinhaconsulta.model.Endereco;
import br.com.fiap.cademinhaconsulta.model.Unidade;
import br.com.fiap.cademinhaconsulta.model.UnidadeContatoEndereco;
import br.com.fiap.cademinhaconsulta.repository.ContatoRepository;
import br.com.fiap.cademinhaconsulta.repository.EnderecoRepository;
import br.com.fiap.cademinhaconsulta.repository.UnidadeContatoEnderecoRepository;
import br.com.fiap.cademinhaconsulta.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final ContatoRepository contatoRepository;
    private final EnderecoRepository enderecoRepository;
    private final UnidadeContatoEnderecoRepository unidadeContatoEnderecoRepository;

    // CIDADAO so ve unidades ATIVO (o filtro de status e ignorado); GESTOR ve todas e pode filtrar
    @Transactional(readOnly = true)
    public PaginaResponse<UnidadeResponse> listar(boolean gestor, StatusUnidade status, Long idEspecialidade,
                                                  Pageable pageable) {
        StatusUnidade filtroStatus = gestor ? status : StatusUnidade.ATIVO;
        Page<Unidade> pagina = unidadeRepository.buscar(filtroStatus, idEspecialidade, pageable);

        Map<Long, UnidadeContatoEndereco> vinculos = buscarVinculos(pagina.map(Unidade::getId).getContent());
        return PaginaResponse.de(pagina.map(u -> UnidadeResponse.de(u, vinculos.get(u.getId()))));
    }

    // Unidade INATIVO pedida por um CIDADAO: 404
    @Transactional(readOnly = true)
    public UnidadeResponse buscar(Long id, boolean gestor) {
        Unidade unidade = buscarUnidade(id);
        if (!gestor && unidade.getStatus() != StatusUnidade.ATIVO) {
            throw new RecursoNaoEncontradoException("Unidade nao encontrada.");
        }
        return montarResposta(unidade);
    }

    // Cria unidade + contato PROPRIO + endereco + vinculo em TB_GS_UN_CONTATO_ENDERECO
    @Transactional
    public UnidadeResponse criar(UnidadeRequest request) {
        Unidade unidade = new Unidade();
        unidade.setNome(request.nome().trim());
        unidade.setStatus(request.status() != null ? request.status() : StatusUnidade.ATIVO);
        unidadeRepository.save(unidade);

        Contato contato = new Contato();
        contato.setObs(ObsContato.PROPRIO);
        copiarContato(request, contato);
        contatoRepository.save(contato);

        Endereco endereco = new Endereco();
        request.endereco().copiarPara(endereco);
        enderecoRepository.save(endereco);

        UnidadeContatoEndereco vinculo = unidadeContatoEnderecoRepository.save(
                new UnidadeContatoEndereco(unidade, contato, endereco));
        return UnidadeResponse.de(unidade, vinculo);
    }

    // Atualiza nome, status (se vier) e o contato/endereco atual; cria o vinculo se a unidade nao tiver
    @Transactional
    public UnidadeResponse atualizar(Long id, UnidadeRequest request) {
        Unidade unidade = buscarUnidade(id);
        unidade.setNome(request.nome().trim());
        if (request.status() != null) {
            unidade.setStatus(request.status());
        }

        UnidadeContatoEndereco vinculo = buscarVinculos(List.of(id)).get(id);
        if (vinculo == null) {
            Contato contato = new Contato();
            contato.setObs(ObsContato.PROPRIO);
            copiarContato(request, contato);
            contatoRepository.save(contato);

            Endereco endereco = new Endereco();
            request.endereco().copiarPara(endereco);
            enderecoRepository.save(endereco);

            vinculo = unidadeContatoEnderecoRepository.save(new UnidadeContatoEndereco(unidade, contato, endereco));
        } else {
            copiarContato(request, vinculo.getContato());
            request.endereco().copiarPara(vinculo.getEndereco());
        }
        return UnidadeResponse.de(unidade, vinculo);
    }

    // DELETE logico: status INATIVO
    @Transactional
    public void inativar(Long id) {
        buscarUnidade(id).setStatus(StatusUnidade.INATIVO);
    }

    private Unidade buscarUnidade(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade nao encontrada."));
    }

    private UnidadeResponse montarResposta(Unidade unidade) {
        return UnidadeResponse.de(unidade, buscarVinculos(List.of(unidade.getId())).get(unidade.getId()));
    }

    // Um vinculo por unidade (o de menor id_contato, caso exista mais de um)
    private Map<Long, UnidadeContatoEndereco> buscarVinculos(List<Long> idsUnidades) {
        Map<Long, UnidadeContatoEndereco> vinculos = new HashMap<>();
        if (idsUnidades.isEmpty()) {
            return vinculos;
        }
        for (UnidadeContatoEndereco v : unidadeContatoEnderecoRepository.buscarVinculosDasUnidades(idsUnidades)) {
            vinculos.putIfAbsent(v.getUnidade().getId(), v);
        }
        return vinculos;
    }

    private void copiarContato(UnidadeRequest request, Contato contato) {
        contato.setTelefone(request.telefone());
        contato.setEmail(StringUtils.hasText(request.email()) ? request.email() : null);
    }
}
