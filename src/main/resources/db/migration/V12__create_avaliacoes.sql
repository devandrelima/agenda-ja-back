CREATE TABLE avaliacoes (
    id BIGSERIAL PRIMARY KEY,

    cliente_id BIGINT NOT NULL,
    estabelecimento_id BIGINT NOT NULL,

    nota INTEGER NOT NULL,
    comentario VARCHAR(1000) NOT NULL,

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_avaliacoes_cliente
        FOREIGN KEY (cliente_id)
            REFERENCES usuarios(id),

    CONSTRAINT fk_avaliacoes_estabelecimento
        FOREIGN KEY (estabelecimento_id)
            REFERENCES estabelecimentos(id),

    CONSTRAINT uk_avaliacoes_cliente_estabelecimento
        UNIQUE (cliente_id, estabelecimento_id),

    CONSTRAINT ck_avaliacoes_nota
        CHECK (nota BETWEEN 1 AND 5),

    CONSTRAINT ck_avaliacoes_comentario
        CHECK (LENGTH(TRIM(comentario)) BETWEEN 1 AND 1000)
);

CREATE INDEX idx_avaliacoes_estabelecimento
    ON avaliacoes(estabelecimento_id);
