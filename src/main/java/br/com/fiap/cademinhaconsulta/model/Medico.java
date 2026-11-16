package br.com.fiap.cademinhaconsulta.model;

import br.com.fiap.cademinhaconsulta.enums.StatusMedico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "TB_GS_MEDICO")
@Getter
@Setter
@NoArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medico_seq")
    @SequenceGenerator(name = "medico_seq", sequenceName = "TB_GS_MEDICO_id_medico_SEQ", allocationSize = 1)
    @Column(name = "id_medico")
    private Long id;

    @Column(name = "nome_medico", nullable = false, length = 100)
    private String nome;

    @Column(name = "sobrenome_medico", nullable = false, length = 100)
    private String sobrenome;

    @Column(name = "crm", nullable = false, length = 10)
    private String crm;

    @Column(name = "uf_crm", nullable = false, length = 2)
    private String ufCrm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_medico", nullable = false, length = 7)
    private StatusMedico status;

    @ManyToMany
    @JoinTable(
            name = "TB_GS_MEDICO_ESPECIALIDADE",
            joinColumns = @JoinColumn(name = "id_medico"),
            inverseJoinColumns = @JoinColumn(name = "id_especialidade"))
    private Set<Especialidade> especialidades = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "TB_GS_MEDICO_UNIDADE",
            joinColumns = @JoinColumn(name = "id_medico"),
            inverseJoinColumns = @JoinColumn(name = "id_unidade"))
    private Set<Unidade> unidades = new HashSet<>();
}
