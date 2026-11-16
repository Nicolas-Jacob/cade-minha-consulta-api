package br.com.fiap.cademinhaconsulta.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Tabela ternaria sem PK propria: a identidade e formada pelas 3 FKs
@Entity
@Table(name = "TB_GS_US_CONTATO_ENDERECO")
@IdClass(UsuarioContatoEnderecoId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioContatoEndereco {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contato")
    private Contato contato;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_endereco")
    private Endereco endereco;
}
