package com.agendaja.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginDTOTest {
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
    void deveNormalizarEmailComArrobaRemovendoEspacosEMinusculas() {
        var dto = new LoginDTO("  USER@EXEMPLO.COM  ", "Senha123");

        assertEquals("user@exemplo.com", dto.email());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void deveManterApenasDigitosQuandoIdentificadorForTelefone() {
        var dto = new LoginDTO("(84) 9 9999-8888", "Senha123");

        assertEquals("84999998888", dto.email());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void deveManterEmailNuloNaInstanciacaoERejeitarNaValidacao() {
        var dto = new LoginDTO(null, "Senha123");

        assertNull(dto.email());
        assertFalse(validator.validate(dto).isEmpty());
    }
}
