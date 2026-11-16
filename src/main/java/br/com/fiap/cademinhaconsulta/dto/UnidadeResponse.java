package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.StatusUnidade;
import br.com.fiap.cademinhaconsulta.model.Endereco;
import br.com.fiap.cademinhaconsulta.model.Unidade;
import br.com.fiap.cademinhaconsulta.model.UnidadeContatoEndereco;

public record UnidadeResponse(
        Long id,
        String nome,
        StatusUnidade status,
        String endereco,
        String telefone
) {

    // vinculo pode ser null quando a unidade nao tem contato/endereco cadastrado
    public static UnidadeResponse de(Unidade unidade, UnidadeContatoEndereco vinculo) {
        return new UnidadeResponse(
                unidade.getId(),
                unidade.getNome(),
                unidade.getStatus(),
                vinculo != null ? formatar(vinculo.getEndereco()) : null,
                vinculo != null ? vinculo.getContato().getTelefone() : null);
    }

    // "Logradouro, 110 - Bairro, Cidade/UF"
    private static String formatar(Endereco e) {
        return "%s, %d - %s, %s/%s".formatted(e.getLogradouro(), e.getNumero(), e.getBairro(), e.getCidade(), e.getUf());
    }
}
