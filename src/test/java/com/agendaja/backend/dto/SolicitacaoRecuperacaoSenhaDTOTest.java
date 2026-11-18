package com.agendaja.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolicitacaoRecuperacaoSenhaDTOTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configurar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void finalizar() {
        factory.close();
    }

    @Test
    void deveNormalizarEmailRemovendoEspacosEConvertendoParaMinusculas() {
        var dto = new SolicitacaoRecuperacaoSenhaDTO("  MARIA@EXEMPLO.COM  ");

        assertEquals("maria@exemplo.com", dto.email());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void deveManterEmailNuloNaInstanciacaoERejeitarNaValidacao() {
        var dto = new SolicitacaoRecuperacaoSenhaDTO(null);

        assertNull(dto.email());
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void deveRejeitarEmailInvalidoOuAcimaDe100Caracteres() {
        var emailInvalido = new SolicitacaoRecuperacaoSenhaDTO("email-invalido");
        assertFalse(validator.validate(emailInvalido).isEmpty());

        var emailLongo = new SolicitacaoRecuperacaoSenhaDTO("a".repeat(95) + "@ex.com");
        assertFalse(validator.validate(emailLongo).isEmpty());
    }
}
