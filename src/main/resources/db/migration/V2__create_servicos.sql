
CREATE TABLE servicos (
      id BIGSERIAL PRIMARY KEY,

      nome VARCHAR(255) NOT NULL,

      duracao_minutos INTEGER NOT NULL,

      valor NUMERIC(10, 2) NOT NULL,

      estabelecimento_id BIGINT NOT NULL,

      CONSTRAINT fk_servicos_estabelecimento
          FOREIGN KEY (estabelecimento_id)
              REFERENCES estabelecimentos(id),

      CONSTRAINT chk_servicos_duracao
          CHECK (duracao_minutos > 0),

      CONSTRAINT chk_servicos_valor
          CHECK (valor >= 0)
);

CREATE INDEX idx_servicos_estabelecimento
    ON servicos(estabelecimento_id);
