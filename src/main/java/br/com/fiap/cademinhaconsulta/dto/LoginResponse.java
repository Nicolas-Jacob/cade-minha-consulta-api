package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.Credencial;

public record LoginResponse(String token, UsuarioLogin usuario) {

    public record UsuarioLogin(Long id, String nome, Credencial credencial) {
    }
}
