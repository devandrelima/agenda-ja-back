
package com.agendaja.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EstabelecimentoCadastroDTOTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void configurarValidador() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void fecharValidador() {
		factory.close();
	}

	@Test
	void deveRejeitarEmailInvalido() {

		var dto = criarDto("email-invalido", "SenhaSegura123");

		Set<ConstraintViolation<EstabelecimentoCadastroDTO>> erros =
				validator.validate(dto);

		assertTrue(contemErroNoCampo(erros, "email"));
	}

	@Test
	void deveRejeitarSenhaCurta() {

		var dto = criarDto("joao@email.com", "123");

		var erros = validator.validate(dto);

		assertTrue(contemErroNoCampo(erros, "senha"));
	}

	@Test
	void deveAceitarDadosValidos() {

		var dto = criarDto("joao@email.com", "SenhaSegura123");

		var erros = validator.validate(dto);

		assertTrue(erros.isEmpty());
	}

	@Test
	void deveRemoverEspacosDoEmailNoDTO() {

		var dto = criarDto(
				"  joao@email.com  ",
				"SenhaSegura123"
		);

		assertEquals("joao@email.com", dto.email());

		assertTrue(validator.validate(dto).isEmpty());
	}

	private EstabelecimentoCadastroDTO criarDto(
			String email,
			String senha) {

		return new EstabelecimentoCadastroDTO(
				"Barbearia Teste",
				"BARBEARIA",
				null,
				"Mossoró/RN",
				"João Silva",
				email,
				senha
		);
	}

	private boolean contemErroNoCampo(
			Set<ConstraintViolation<EstabelecimentoCadastroDTO>> erros,
			String campo) {

		return erros.stream().anyMatch(
				erro -> erro.getPropertyPath().toString().equals(campo)
		);
	}
}
