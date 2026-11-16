package br.com.fiap.cademinhaconsulta.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Chave composta da ternaria TB_GS_US_CONTATO_ENDERECO.
// Os nomes dos campos sao os mesmos dos atributos @Id de UsuarioContatoEndereco.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioContatoEnderecoId implements Serializable {
    private Long usuario;
    private Long contato;
    private Long endereco;
}
