package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.EnderecoDTO;
import br.com.fiap.cademinhaconsulta.dto.UsuarioResponse;
import br.com.fiap.cademinhaconsulta.dto.UsuarioUpdateRequest;
import br.com.fiap.cademinhaconsulta.enums.ObsContato;
import br.com.fiap.cademinhaconsulta.exception.RecursoNaoEncontradoException;
import br.com.fiap.cademinhaconsulta.model.Contato;
import br.com.fiap.cademinhaconsulta.model.Endereco;
import br.com.fiap.cademinhaconsulta.model.Usuario;
import br.com.fiap.cademinhaconsulta.model.UsuarioContatoEndereco;
import br.com.fiap.cademinhaconsulta.repository.ContatoRepository;
import br.com.fiap.cademinhaconsulta.repository.EnderecoRepository;
import br.com.fiap.cademinhaconsulta.repository.UsuarioContatoEnderecoRepository;
import br.com.fiap.cademinhaconsulta.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ContatoRepository contatoRepository;
    private final EnderecoRepository enderecoRepository;
    private final UsuarioContatoEnderecoRepository usuarioContatoEnderecoRepository;

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        UsuarioContatoEndereco vinculo = buscarVinculoPrincipal(idUsuario).orElse(null);
        return montarResposta(usuario, vinculo);
    }

    @Transactional
    public UsuarioResponse atualizar(Long idUsuario, UsuarioUpdateRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);
        usuario.setNome(request.nome());
        usuario.setSobrenome(request.sobrenome());

        UsuarioContatoEndereco vinculo = buscarVinculoPrincipal(idUsuario).orElse(null);
        Contato contato;
        Endereco endereco;
        if (vinculo != null) {
            contato = vinculo.getContato();
            endereco = vinculo.getEndereco();
        } else {
            contato = new Contato();
            contato.setObs(ObsContato.PROPRIO);
            endereco = new Endereco();
        }

        contato.setTelefone(request.telefone());
        contato.setEmail(StringUtils.hasText(request.email()) ? request.email() : null);
        request.endereco().copiarPara(endereco);

        // Usuario sem vinculo: cria contato, endereco e o vinculo na ternaria
        if (vinculo == null) {
            contatoRepository.save(contato);
            enderecoRepository.save(endereco);
            vinculo = usuarioContatoEnderecoRepository.save(new UsuarioContatoEndereco(usuario, contato, endereco));
        }

        return montarResposta(usuario, vinculo);
    }

    private Usuario buscarUsuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado."));
    }

    // Prefere o vinculo com contato PROPRIO; se nao houver, usa o primeiro vinculo do usuario
    private Optional<UsuarioContatoEndereco> buscarVinculoPrincipal(Long idUsuario) {
        List<UsuarioContatoEndereco> vinculos = usuarioContatoEnderecoRepository.buscarVinculosDoUsuario(idUsuario);
        return vinculos.stream()
                .filter(v -> v.getContato().getObs() == ObsContato.PROPRIO)
                .findFirst()
                .or(() -> vinculos.stream().findFirst());
    }

    private UsuarioResponse montarResposta(Usuario usuario, UsuarioContatoEndereco vinculo) {
        Contato contato = vinculo != null ? vinculo.getContato() : null;
        Endereco endereco = vinculo != null ? vinculo.getEndereco() : null;
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getSobrenome(),
                usuario.getCpf(),
                usuario.getSexo(),
                usuario.getDataNascimento(),
                usuario.getLogin(),
                usuario.getCredencial(),
                contato != null ? contato.getTelefone() : null,
                contato != null ? contato.getEmail() : null,
                endereco != null ? EnderecoDTO.de(endereco) : null);
    }
}
