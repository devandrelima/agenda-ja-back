
CREATE TABLE tokens_recuperacao_senha (
    id BIGSERIAL PRIMARY KEY,

    usuario_id BIGINT NOT NULL
      REFERENCES usuarios(id) ON DELETE CASCADE,

    token_hash VARCHAR(64) NOT NULL UNIQUE,

    criado_em TIMESTAMPTZ NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    utilizado_em TIMESTAMPTZ
);

CREATE INDEX idx_recuperacao_senha_usuario_criacao
    ON tokens_recuperacao_senha (
        usuario_id,
        criado_em DESC
);
