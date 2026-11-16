package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.model.UsuarioContatoEndereco;
import br.com.fiap.cademinhaconsulta.model.UsuarioContatoEnderecoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsuarioContatoEnderecoRepository extends JpaRepository<UsuarioContatoEndereco, UsuarioContatoEnderecoId> {

    @Query("""
            select v from UsuarioContatoEndereco v
            join fetch v.contato
            join fetch v.endereco
            where v.usuario.id = :idUsuario
            order by v.contato.id
            """)
    List<UsuarioContatoEndereco> buscarVinculosDoUsuario(@Param("idUsuario") Long idUsuario);
}
