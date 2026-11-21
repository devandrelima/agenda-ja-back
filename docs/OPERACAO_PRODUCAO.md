# Operação em produção

## Topologia

O frontend encaminha `/api/*` para o contêiner `agenda-ja-back:8080`. Os contêineres `agenda-ja-front`, `agenda-ja-back` e `agendaja-postgres` devem pertencer à rede Docker `agendaja-net`.

Após um deploy, valide:

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Networks}}'
docker exec agenda-ja-front wget -S -O- http://agenda-ja-back:8080/api/auth/csrf
curl -i http://SEU_HOST:8092/api/auth/csrf
```

O último comando deve devolver `200`. Um `502` no frontend significa que o backend não iniciou ou não está acessível pela rede Docker.

## Recuperação de falha do Flyway nas migrations 19 e 20

Antes de qualquer alteração, faça backup:

```bash
docker exec agendaja-postgres pg_dump -U agendaja -Fc agendaja \
  > agendaja-before-flyway-repair.dump
```

As versões 19 e 20 foram renumeradas durante o desenvolvimento. Em bancos que já registraram as versões anteriores, valide o histórico e a coluna antes de reparar os checksums:

```bash
docker exec agendaja-postgres psql -U agendaja -d agendaja -c \
  "SELECT version, description, checksum, success FROM flyway_schema_history WHERE version IN ('19', '20', '21') ORDER BY installed_rank;"

docker exec agendaja-postgres psql -U agendaja -d agendaja -c \
  "ALTER TABLE agendamentos ADD COLUMN IF NOT EXISTS lembrete_enviado BOOLEAN NOT NULL DEFAULT FALSE;"
```

Em seguida, execute `flyway repair` com as migrations da versão que será implantada. Só então suba novamente o backend: o Flyway aplicará a `V21__atualizar_logos_do_catalogo.sql` pendente.

Não apague nem recrie `flyway_schema_history`: isso pode reexecutar seeds e causar duplicação de dados.

## WhatsApp (HU27)

A aplicação já possui eventos pós-commit para criação, confirmação, reagendamento e lembrete diário. Para envio real por WhatsApp ainda é necessário configurar um provedor, preferencialmente a **WhatsApp Cloud API da Meta**:

1. Conta Meta Business verificada e número remetente aprovado.
2. `phone_number_id` e token de acesso armazenados como segredos no ambiente de deploy.
3. Templates transacionais aprovados, pois lembretes podem ser enviados fora da janela de 24 horas.
4. Definição dos templates e seus parâmetros (cliente, serviço, data/hora e local).

Nunca grave token, telefone ou segredo no Git. Após esses dados existirem, o adaptador deve ser habilitado por variável de ambiente e testado primeiro com um número autorizado pela Meta.
