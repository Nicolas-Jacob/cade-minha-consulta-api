package br.com.fiap.cademinhaconsulta.dto;

import br.com.fiap.cademinhaconsulta.enums.OrigemOrientacao;

import java.time.LocalDateTime;

public record OrientacaoResponse(
        Long idConsulta,
        String mensagem,
        LocalDateTime geradoEm,
        OrigemOrientacao origem,
        String aviso
) {

    public static final String AVISO_IA = "Orientação gerada por IA. Não substitui a orientação médica.";
    public static final String AVISO_PADRAO = "Orientações gerais. Não substituem a orientação médica.";

    // O aviso depende da origem do texto (secao 7 do contrato)
    public static String aviso(OrigemOrientacao origem) {
        return origem == OrigemOrientacao.IA ? AVISO_IA : AVISO_PADRAO;
    }
}
