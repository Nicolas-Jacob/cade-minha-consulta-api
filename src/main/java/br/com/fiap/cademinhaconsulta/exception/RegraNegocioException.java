package br.com.fiap.cademinhaconsulta.exception;

// HTTP 422 - regra de negocio violada
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
