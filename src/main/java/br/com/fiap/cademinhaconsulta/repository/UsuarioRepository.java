package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByCpf(String cpf);

    // Senhas ainda em texto puro (hash BCrypt sempre comeca com "$2")
    List<Usuario> findBySenhaNotLike(String padrao);
}
