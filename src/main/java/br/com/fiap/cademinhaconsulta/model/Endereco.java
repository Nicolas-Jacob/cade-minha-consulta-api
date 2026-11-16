package br.com.fiap.cademinhaconsulta.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TB_GS_ENDERECO")
@Getter
@Setter
@NoArgsConstructor
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "endereco_seq")
    @SequenceGenerator(name = "endereco_seq", sequenceName = "TB_GS_ENDERECO_id_endereco_SEQ", allocationSize = 1)
    @Column(name = "id_endereco")
    private Long id;

    @Column(name = "logradouro", nullable = false, length = 100)
    private String logradouro;

    @Column(name = "numero_end", nullable = false)
    private Long numero;

    @Column(name = "complemento_end", length = 20)
    private String complemento;

    // CHAR(8), sem traco
    @Column(name = "CEP", nullable = false, length = 8)
    private String cep;

    @Column(name = "bairro_end", nullable = false, length = 100)
    private String bairro;

    @Column(name = "cidade_end", nullable = false, length = 50)
    private String cidade;

    @Column(name = "UF_end", nullable = false, length = 2)
    private String uf;
}
