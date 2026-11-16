package br.com.fiap.cademinhaconsulta.exception;

// HTTP 409 - conflito com o estado atual (ex.: login ou CPF ja cadastrado)
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
