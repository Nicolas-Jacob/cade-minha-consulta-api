package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.model.Especialidade;

public record EspecialidadeResponse(Long id, String nome, String descricao) {

    public static EspecialidadeResponse de(Especialidade especialidade) {
        return new EspecialidadeResponse(especialidade.getId(), especialidade.getNome(), especialidade.getDescricao());
    }
}
