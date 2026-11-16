package br.com.fiap.cademinhaconsulta.exception;

// HTTP 404 - recurso inexistente
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
