package br.com.fiap.cademinhaconsulta.repository;

import br.com.fiap.cademinhaconsulta.model.Medico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicoRepository extends JpaRepository<Medico, Long> {

    // Filtros opcionais: parametro null = sem filtro
    @Query("""
            select m from Medico m
            where (:idEspecialidade is null or exists (
                      select 1 from Medico m2 join m2.especialidades e
                      where m2 = m and e.id = :idEspecialidade))
              and (:idUnidade is null or exists (
                      select 1 from Medico m2 join m2.unidades u
                      where m2 = m and u.id = :idUnidade))
            """)
    Page<Medico> buscar(@Param("idEspecialidade") Long idEspecialidade,
                        @Param("idUnidade") Long idUnidade,
                        Pageable pageable);

    // crm e CHAR(10) no Oracle (completado com espacos), por isso o trim na comparacao.
    // idIgnorado: o proprio medico, no PUT.
    @Query("""
            select case when count(m) > 0 then true else false end
            from Medico m
            where trim(m.crm) = :crm and m.ufCrm = :ufCrm
              and (:idIgnorado is null or m.id <> :idIgnorado)
            """)
    boolean existeCrm(@Param("crm") String crm,
                      @Param("ufCrm") String ufCrm,
                      @Param("idIgnorado") Long idIgnorado);

    boolean existsByEspecialidades_Id(Long idEspecialidade);
}
