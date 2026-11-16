package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.model.UnidadeContatoEndereco;
import br.com.fiap.cademinhaconsulta.model.UnidadeContatoEnderecoId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeContatoEnderecoRepository extends JpaRepository<UnidadeContatoEndereco, UnidadeContatoEnderecoId> {
}
