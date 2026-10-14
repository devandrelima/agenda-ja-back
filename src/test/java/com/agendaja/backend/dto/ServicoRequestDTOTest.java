
package com.agendaja.backend.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ServicoRequestDTOTest {

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

	private ServicoRequestDTO criar(
			String nome,
			Integer duracao,
			BigDecimal valor
	) {
		return new ServicoRequestDTO(nome, duracao, valor);
	}

	@Test
	void deveAceitarServicoValido() {
		var dto = criar(
				"Corte de cabelo",
				30,
				new BigDecimal("35.00")
		);

		assertTrue(validator.validate(dto).isEmpty());
	}

	@Test
	void deveRejeitarNomeEmBranco() {
		var dto = criar(
				"   ",
				30,
				new BigDecimal("35.00")
		);

		assertFalse(validator.validate(dto).isEmpty());
	}

	@Test
	void deveRejeitarDuracaoZero() {
		var dto = criar(
				"Corte",
				0,
				new BigDecimal("35.00")
		);

		assertFalse(validator.validate(dto).isEmpty());
	}

	@Test
	void deveRejeitarValorNegativo() {
		var dto = criar(
				"Corte",
				30,
				new BigDecimal("-1.00")
		);

		assertFalse(validator.validate(dto).isEmpty());
	}

	@Test
	void deveRejeitarMaisDeDuasCasasDecimais() {
		var dto = criar(
				"Corte",
				30,
				new BigDecimal("35.999")
		);

		assertFalse(validator.validate(dto).isEmpty());
	}

	@Test
	void deveRejeitarCamposNulos() {
		var dto = criar(null, null, null);

		assertEquals(3, validator.validate(dto).size());
	}

	@Test
	void deveAceitarServicoGratuito() {
		var dto = criar(
				"Avaliação gratuita",
				15,
				BigDecimal.ZERO
		);

		assertTrue(validator.validate(dto).isEmpty());
	}
}
