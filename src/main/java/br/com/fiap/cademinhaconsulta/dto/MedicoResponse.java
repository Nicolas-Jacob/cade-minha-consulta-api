package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.StatusMedico;
import br.com.fiap.cademinhaconsulta.model.Medico;

import java.util.Comparator;
import java.util.List;

public record MedicoResponse(
        Long id,
        String nome,
        String sobrenome,
        String crm,
        String ufCrm,
        StatusMedico status,
        List<EspecialidadeResponse> especialidades,
        List<ResumoDTO> unidades
) {

    public static MedicoResponse de(Medico medico) {
        return new MedicoResponse(
                medico.getId(),
                medico.getNome(),
                medico.getSobrenome(),
                // Coluna CHAR(10): o Oracle completa com espacos a direita
                medico.getCrm().trim(),
                medico.getUfCrm(),
                medico.getStatus(),
                medico.getEspecialidades().stream()
                        .map(EspecialidadeResponse::de)
                        .sorted(Comparator.comparing(EspecialidadeResponse::nome))
                        .toList(),
                medico.getUnidades().stream()
                        .map(u -> new ResumoDTO(u.getId(), u.getNome()))
                        .sorted(Comparator.comparing(ResumoDTO::nome))
                        .toList());
    }
}
