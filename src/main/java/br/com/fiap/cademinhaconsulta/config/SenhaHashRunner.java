package br.com.fiap.cademinhaconsulta.config;

import br.com.fiap.cademinhaconsulta.model.Usuario;
import br.com.fiap.cademinhaconsulta.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// As senhas da carga (05-carga-dados.sql) estao em texto puro.
// Na inicializacao, toda senha que nao comeca com "$2" (prefixo do BCrypt) vira hash.
@Slf4j
@Component
@RequiredArgsConstructor
public class SenhaHashRunner implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        List<Usuario> usuarios = usuarioRepository.findBySenhaNotLike("$2%");
        if (usuarios.isEmpty()) {
            return;
        }
        usuarios.forEach(usuario -> usuario.setSenha(passwordEncoder.encode(usuario.getSenha())));
        usuarioRepository.saveAll(usuarios);
        log.info("SenhaHashRunner: {} senha(s) convertida(s) para BCrypt.", usuarios.size());
    }
}
