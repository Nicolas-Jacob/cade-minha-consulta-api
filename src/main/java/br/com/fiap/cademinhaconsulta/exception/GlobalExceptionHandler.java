package br.com.fiap.cademinhaconsulta.exception;

import br.com.fiap.cademinhaconsulta.dto.CampoErro;
import br.com.fiap.cademinhaconsulta.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.hibernate.query.sqm.UnknownPathException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
@SuppressWarnings("deprecation") // UNPROCESSABLE_ENTITY: mantido para o texto "Unprocessable Entity" do contrato
public class GlobalExceptionHandler {

    // Erros de negocio levantados pelo banco (RAISE_APPLICATION_ERROR) -> HTTP (secao 3 do contrato)
    private static final Map<Integer, HttpStatus> ERROS_ORACLE = Map.of(
            20001, HttpStatus.NOT_FOUND,            // Medico nao encontrado
            20002, HttpStatus.CONFLICT,             // Medico inativo
            20003, HttpStatus.UNPROCESSABLE_ENTITY, // Nao ha pacientes aguardando
            20004, HttpStatus.CONFLICT,             // Fila alterada durante o agendamento
            20005, HttpStatus.CONFLICT,             // Sem horario livre
            20006, HttpStatus.CONFLICT,             // Falha ao atualizar a fila
            20007, HttpStatus.UNPROCESSABLE_ENTITY  // Data da consulta precisa ser futura (trigger)
    );

    // Pega so o texto depois de "ORA-200xx: " ate a primeira quebra de linha (descarta ORA-06512 etc.)
    private static final Pattern MENSAGEM_ORACLE = Pattern.compile("ORA-200\\d{2}: ([^\\r\\n]*)");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoErro(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Dados invalidos. Verifique os campos informados.", request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisicao invalido ou mal formatado.", request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErroResponse> parametroInvalido(Exception ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Parametro invalido ou ausente na requisicao.", request);
    }

    // ?sort= com campo inexistente (consultas derivadas e findAll)
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResponse> ordenacaoInvalida(PropertyReferenceException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Campo de ordenacao invalido: " + ex.getPropertyName() + ".", request);
    }

    // ?sort= com campo inexistente nas consultas com @Query: o Hibernate nao resolve o atributo.
    // As @Query sao validadas na inicializacao, entao um caminho desconhecido aqui so pode vir do sort.
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErroResponse> usoInvalidoConsulta(InvalidDataAccessApiUsageException ex,
                                                            HttpServletRequest request) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof UnknownPathException) {
                return responder(HttpStatus.BAD_REQUEST, "Campo de ordenacao invalido.", request);
            }
        }
        return erroBanco(ex, request);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> credenciaisInvalidas(CredenciaisInvalidasException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> naoAutenticado(AuthenticationException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, "Token ausente, invalido ou expirado.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, "Voce nao tem permissao para acessar este recurso.", request);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> acessoNegadoRecurso(AcessoNegadoException ex, HttpServletRequest request) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaNaoEncontrada(NoResourceFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Recurso nao encontrado.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP nao suportado nesta rota.", request);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErroResponse> erroBanco(DataAccessException ex, HttpServletRequest request) {
        ResponseEntity<ErroResponse> erroOracle = erroOracle(ex, request);
        if (erroOracle != null) {
            return erroOracle;
        }
        if (ex instanceof DataIntegrityViolationException) {
            String causa = String.valueOf(ex.getMostSpecificCause().getMessage());
            log.warn("Violacao de integridade em {}: {}", request.getRequestURI(), causa);
            // UNIQUE de CPF (caso dois cadastros simultaneos passem pela verificacao do service)
            if (causa.toUpperCase().contains("TB_GS_USUARIO_CPF_UC")) {
                return responder(HttpStatus.CONFLICT, "CPF ja cadastrado.", request);
            }
            return responder(HttpStatus.CONFLICT, "Operacao viola uma restricao do banco de dados.", request);
        }
        return erroInterno(ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroGenerico(Exception ex, HttpServletRequest request) {
        // Erros do Oracle tambem podem chegar embrulhados em outras excecoes (ex.: no commit da transacao)
        ResponseEntity<ErroResponse> erroOracle = erroOracle(ex, request);
        if (erroOracle != null) {
            return erroOracle;
        }
        return erroInterno(ex, request);
    }

    private ResponseEntity<ErroResponse> erroOracle(Throwable ex, HttpServletRequest request) {
        SQLException sql = buscarErroOracle(ex);
        if (sql == null) {
            return null;
        }
        return responder(ERROS_ORACLE.get(sql.getErrorCode()), mensagemOracle(sql), request);
    }

    private ResponseEntity<ErroResponse> erroInterno(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor. Tente novamente mais tarde.", request);
    }

    // Percorre as causas procurando a SQLException com codigo -20001 a -20007
    private static SQLException buscarErroOracle(Throwable ex) {
        Throwable atual = ex;
        while (atual != null) {
            if (atual instanceof SQLException sql && ERROS_ORACLE.containsKey(sql.getErrorCode())) {
                return sql;
            }
            atual = atual.getCause() == atual ? null : atual.getCause();
        }
        return null;
    }

    static String mensagemOracle(SQLException sql) {
        String mensagem = sql.getMessage() == null ? "" : sql.getMessage();
        Matcher matcher = MENSAGEM_ORACLE.matcher(mensagem);
        return matcher.find() ? matcher.group(1).trim() : mensagem.trim();
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem, HttpServletRequest request) {
        return responder(status, mensagem, request, null);
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem, HttpServletRequest request,
                                                   List<CampoErro> campos) {
        return ResponseEntity.status(status)
                .body(ErroResponse.de(status, mensagem, request.getRequestURI(), campos));
    }
}
