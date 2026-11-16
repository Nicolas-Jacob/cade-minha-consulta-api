package br.com.fiap.cademinhaconsulta.exception;

// HTTP 401 - login ou senha invalidos
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
