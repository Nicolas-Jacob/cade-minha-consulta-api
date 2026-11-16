package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.model.Endereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Usado tanto no request (RegisterRequest/UsuarioUpdateRequest) quanto no UsuarioResponse
public record EnderecoDTO(
        @NotBlank(message = "CEP e obrigatorio")
        @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 digitos, sem traco")
        String cep,

        @NotBlank(message = "Logradouro e obrigatorio")
        @Size(max = 100, message = "Logradouro deve ter no maximo 100 caracteres")
        String logradouro,

        @NotNull(message = "Numero e obrigatorio")
        @Positive(message = "Numero deve ser maior que zero")
        Long numero,

        @Size(max = 20, message = "Complemento deve ter no maximo 20 caracteres")
        String complemento,

        @NotBlank(message = "Bairro e obrigatorio")
        @Size(max = 100, message = "Bairro deve ter no maximo 100 caracteres")
        String bairro,

        @NotBlank(message = "Cidade e obrigatoria")
        @Size(max = 50, message = "Cidade deve ter no maximo 50 caracteres")
        String cidade,

        @NotBlank(message = "UF e obrigatoria")
        @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "UF invalida")
        String uf
) {

    public static EnderecoDTO de(Endereco endereco) {
        return new EnderecoDTO(
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf());
    }

    public void copiarPara(Endereco endereco) {
        endereco.setCep(cep);
        endereco.setLogradouro(logradouro);
        endereco.setNumero(numero);
        endereco.setComplemento(complemento);
        endereco.setBairro(bairro);
        endereco.setCidade(cidade);
        endereco.setUf(uf);
    }
}
