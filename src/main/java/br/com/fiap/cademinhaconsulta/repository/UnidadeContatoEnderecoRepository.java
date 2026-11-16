package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.model.UnidadeContatoEndereco;
import br.com.fiap.cademinhaconsulta.model.UnidadeContatoEnderecoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UnidadeContatoEnderecoRepository extends JpaRepository<UnidadeContatoEndereco, UnidadeContatoEnderecoId> {

    // Vinculos (contato + endereco) de varias unidades numa consulta so, para montar uma pagina
    @Query("""
            select v from UnidadeContatoEndereco v
            join fetch v.contato
            join fetch v.endereco
            where v.unidade.id in :idsUnidades
            order by v.contato.id
            """)
    List<UnidadeContatoEndereco> buscarVinculosDasUnidades(@Param("idsUnidades") Collection<Long> idsUnidades);
}
