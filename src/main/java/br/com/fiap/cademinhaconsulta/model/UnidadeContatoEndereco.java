package br.com.fiap.cademinhaconsulta.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Tabela ternaria sem PK propria: a identidade e formada pelas 3 FKs
@Entity
@Table(name = "TB_GS_UN_CONTATO_ENDERECO")
@IdClass(UnidadeContatoEnderecoId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeContatoEndereco {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_unidade")
    private Unidade unidade;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contato")
    private Contato contato;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_endereco")
    private Endereco endereco;
}
