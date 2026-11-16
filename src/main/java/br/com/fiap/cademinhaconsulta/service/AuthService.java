package br.com.fiap.cademinhaconsulta.service;

import br.com.fiap.cademinhaconsulta.dto.LoginRequest;
import br.com.fiap.cademinhaconsulta.dto.LoginResponse;
import br.com.fiap.cademinhaconsulta.dto.RegisterRequest;
import br.com.fiap.cademinhaconsulta.enums.Credencial;
import br.com.fiap.cademinhaconsulta.enums.ObsContato;
import br.com.fiap.cademinhaconsulta.enums.Sexo;
import br.com.fiap.cademinhaconsulta.exception.ConflitoException;
import br.com.fiap.cademinhaconsulta.exception.CredenciaisInvalidasException;
import br.com.fiap.cademinhaconsulta.model.Contato;
import br.com.fiap.cademinhaconsulta.model.Endereco;
import br.com.fiap.cademinhaconsulta.model.Usuario;
import br.com.fiap.cademinhaconsulta.model.UsuarioContatoEndereco;
import br.com.fiap.cademinhaconsulta.repository.ContatoRepository;
import br.com.fiap.cademinhaconsulta.repository.EnderecoRepository;
import br.com.fiap.cademinhaconsulta.repository.UsuarioContatoEnderecoRepository;
import br.com.fiap.cademinhaconsulta.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ContatoRepository contatoRepository;
    private final EnderecoRepository enderecoRepository;
    private final UsuarioContatoEnderecoRepository usuarioContatoEnderecoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByLogin(request.login())
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenha()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Login ou senha invalidos."));
        return gerarResposta(usuario);
    }

    // Cria Usuario CIDADAO + Contato PROPRIO + Endereco + vinculo, tudo na mesma transacao
    @Transactional
    public LoginResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByLogin(request.login())) {
            throw new ConflitoException("Login ja cadastrado.");
        }
        if (usuarioRepository.existsByCpf(request.cpf())) {
            throw new ConflitoException("CPF ja cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setSobrenome(request.sobrenome());
        usuario.setCpf(request.cpf());
        usuario.setSexo(Sexo.valueOf(request.sexo()));
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setNomeMae(request.nomeMae());
        usuario.setNomePai(StringUtils.hasText(request.nomePai()) ? request.nomePai() : null);
        usuario.setLogin(request.login());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setCredencial(Credencial.CIDADAO);
        usuarioRepository.save(usuario);

        Contato contato = new Contato();
        contato.setTelefone(request.telefone());
        contato.setEmail(StringUtils.hasText(request.email()) ? request.email() : null);
        contato.setObs(ObsContato.PROPRIO);
        contatoRepository.save(contato);

        Endereco endereco = new Endereco();
        request.endereco().copiarPara(endereco);
        enderecoRepository.save(endereco);

        usuarioContatoEnderecoRepository.save(new UsuarioContatoEndereco(usuario, contato, endereco));

        return gerarResposta(usuario);
    }

    private LoginResponse gerarResposta(Usuario usuario) {
        return new LoginResponse(
                tokenService.gerarToken(usuario),
                new LoginResponse.UsuarioLogin(usuario.getId(), usuario.getNome(), usuario.getCredencial()));
    }
}
