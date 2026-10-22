CREATE TABLE agendamentos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    observacoes TEXT,
    CONSTRAINT fk_agendamento_cliente
        FOREIGN KEY (cliente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_agendamento_profissional
        FOREIGN KEY (profissional_id) REFERENCES profissionais(id),
    CONSTRAINT fk_agendamento_servico
        FOREIGN KEY (servico_id) REFERENCES servicos(id)
);

CREATE INDEX idx_agendamentos_profissional_data ON agendamentos(profissional_id, data_hora_inicio);
