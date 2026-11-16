package br.com.fiap.cademinhaconsulta.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// Formato de pagina da secao 5 do contrato (content, totalElements, totalPages, number, size)
public record PaginaResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.getNumber(),
                pagina.getSize());
    }
}
