-- Ativa a extensão btree_gist para permitir misturar operadores de igualdade (=) 
-- com operadores de intersecção (&&) no mesmo índice GiST.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Cria a restrição de exclusão que barra nativamente qualquer INSERT ou UPDATE 
-- que tente colocar dois agendamentos no mesmo horário para o mesmo profissional.
ALTER TABLE agendamentos
ADD CONSTRAINT chk_sem_sobreposicao_agenda
EXCLUDE USING gist (
    profissional_id WITH =,
    tsrange(data_hora_inicio, data_hora_fim) WITH &&
)
WHERE (status != 'CANCELADO');
