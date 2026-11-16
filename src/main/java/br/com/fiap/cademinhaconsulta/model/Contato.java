package br.com.fiap.cademinhaconsulta.model;

import br.com.fiap.cademinhaconsulta.enums.ObsContato;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TB_GS_CONTATO")
@Getter
@Setter
@NoArgsConstructor
public class Contato {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contato_seq")
    @SequenceGenerator(name = "contato_seq", sequenceName = "TB_GS_CONTATO_id_contato_SEQ", allocationSize = 1)
    @Column(name = "id_contato")
    private Long id;

    @Column(name = "telefone_contato", nullable = false, length = 11)
    private String telefone;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "obs_contato", length = 10)
    private ObsContato obs;
}
