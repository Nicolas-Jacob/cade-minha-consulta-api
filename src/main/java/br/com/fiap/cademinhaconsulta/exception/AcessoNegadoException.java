package br.com.fiap.cademinhaconsulta.exception;

// HTTP 403 - o recurso existe, mas pertence a outro usuario
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
