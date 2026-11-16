package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.enums.StatusUnidade;
import br.com.fiap.cademinhaconsulta.model.Unidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    // Filtros opcionais: parametro null = sem filtro.
    // idEspecialidade: so unidades com pelo menos um medico ATIVO dessa especialidade
    // (TB_GS_MEDICO_UNIDADE + TB_GS_MEDICO_ESPECIALIDADE).
    @Query("""
            select un from Unidade un
            where (:status is null or un.status = :status)
              and (:idEspecialidade is null or exists (
                      select 1 from Medico m join m.unidades mu join m.especialidades me
                      where mu = un and me.id = :idEspecialidade
                        and m.status = br.com.fiap.cademinhaconsulta.enums.StatusMedico.ATIVO))
            """)
    Page<Unidade> buscar(@Param("status") StatusUnidade status,
                         @Param("idEspecialidade") Long idEspecialidade,
                         Pageable pageable);
}
