package br.com.fiap.cademinhaconsulta.exception;

import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Le os erros de negocio do banco (RAISE_APPLICATION_ERROR -20001 a -20007, secao 3 do contrato).
// Usado pelo GlobalExceptionHandler (HTTP) e pelo consumidor do RabbitMQ (log).
public final class ErroOracle {

    public static final int FILA_VAZIA = 20003;

    private static final Set<Integer> CODIGOS = Set.of(20001, 20002, 20003, 20004, 20005, 20006, 20007);

    // Pega so o texto depois de "ORA-200xx: " ate a primeira quebra de linha (descarta ORA-06512 etc.)
    private static final Pattern MENSAGEM = Pattern.compile("ORA-200\\d{2}: ([^\\r\\n]*)");

    private ErroOracle() {
    }

    // Percorre as causas procurando a SQLException com codigo -20001 a -20007; null se nao houver
    public static SQLException buscar(Throwable ex) {
        Throwable atual = ex;
        while (atual != null) {
            if (atual instanceof SQLException sql && CODIGOS.contains(sql.getErrorCode())) {
                return sql;
            }
            atual = atual.getCause() == atual ? null : atual.getCause();
        }
        return null;
    }

    public static String mensagem(SQLException sql) {
        String mensagem = sql.getMessage() == null ? "" : sql.getMessage();
        Matcher matcher = MENSAGEM.matcher(mensagem);
        return matcher.find() ? matcher.group(1).trim() : mensagem.trim();
    }
}
