-- Adiciona os campos planejados na arquitetura
ALTER TABLE profissionais ADD COLUMN foto_url VARCHAR(500);
ALTER TABLE profissionais ADD COLUMN usuario_id BIGINT;

ALTER TABLE profissionais ADD CONSTRAINT fk_profissionais_usuario 
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL;

-- Remove a tabela redundante de especialidades
DROP TABLE IF EXISTS profissional_especialidades;
