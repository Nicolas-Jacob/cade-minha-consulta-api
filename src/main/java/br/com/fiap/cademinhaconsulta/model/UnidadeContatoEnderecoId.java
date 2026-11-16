package br.com.fiap.cademinhaconsulta.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Chave composta da ternaria TB_GS_UN_CONTATO_ENDERECO.
// Os nomes dos campos sao os mesmos dos atributos @Id de UnidadeContatoEndereco.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeContatoEnderecoId implements Serializable {
    private Long unidade;
    private Long contato;
    private Long endereco;
}
