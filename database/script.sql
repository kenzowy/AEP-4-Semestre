-- =====================================================================
-- Projeto Transporte Social - AEP 4o Semestre (Engenharia de Software)
-- Banco: MySQL via XAMPP / phpMyAdmin
-- Para criar: importe este arquivo no phpMyAdmin
-- =====================================================================

DROP DATABASE IF EXISTS transporte_social;
CREATE DATABASE transporte_social DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE transporte_social;

-- ---------------------------------------------------------------------
-- usuario: dados comuns (heranca no banco com tabelas especializadas)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id              INT          NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(100) NOT NULL,
    cpf             VARCHAR(14)  NOT NULL,
    data_nascimento DATE         NOT NULL,
    email           VARCHAR(100) NOT NULL,
    telefone        VARCHAR(20),
    senha           VARCHAR(255) NOT NULL,
    tipo            ENUM('CIDADAO','MOTORISTA','ADMINISTRADOR') NOT NULL,
    status          ENUM('PENDENTE','APROVADO','RECUSADO') NOT NULL DEFAULT 'PENDENTE',
    motivo_recusa   VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_cpf (cpf),
    UNIQUE KEY uk_usuario_email (email)
) ENGINE=InnoDB;

CREATE TABLE cidadao (
    usuario_id     INT           NOT NULL,
    renda_familiar DECIMAL(10,2) NOT NULL,
    nis            VARCHAR(11),
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_cidadao_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE motorista (
    usuario_id INT         NOT NULL,
    cnh        VARCHAR(11) NOT NULL,
    disponivel TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_motorista_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE documento (
    id         INT          NOT NULL AUTO_INCREMENT,
    usuario_id INT          NOT NULL,
    tipo       ENUM('CPF','COMPROVANTE_RENDA','ANTECEDENTES_CRIMINAIS','CNH') NOT NULL,
    arquivo    VARCHAR(255) NOT NULL,
    data_envio DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_documento_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE veiculo (
    id           INT         NOT NULL AUTO_INCREMENT,
    motorista_id INT         NOT NULL,
    placa        VARCHAR(8)  NOT NULL,
    marca        VARCHAR(50) NOT NULL DEFAULT '',
    modelo       VARCHAR(50) NOT NULL,
    ano          INT         NOT NULL,
    cor          VARCHAR(30),
    PRIMARY KEY (id),
    UNIQUE KEY uk_veiculo_placa (placa),
    CONSTRAINT fk_veiculo_motorista FOREIGN KEY (motorista_id)
        REFERENCES motorista (usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE viagem (
    id             INT          NOT NULL AUTO_INCREMENT,
    cidadao_id     INT          NOT NULL,
    motorista_id   INT          NULL,
    origem         VARCHAR(200) NOT NULL,
    destino        VARCHAR(200) NOT NULL,
    distancia_km   DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    valor_total    DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    valor_subsidio DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    valor_cidadao  DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    data_hora      DATETIME     NOT NULL,
    status         ENUM('SOLICITADA','ACEITA','EM_ANDAMENTO','CONCLUIDA','CANCELADA')
                   NOT NULL DEFAULT 'SOLICITADA',
    PRIMARY KEY (id),
    CONSTRAINT fk_viagem_cidadao   FOREIGN KEY (cidadao_id)   REFERENCES cidadao (usuario_id),
    CONSTRAINT fk_viagem_motorista FOREIGN KEY (motorista_id) REFERENCES motorista (usuario_id),
    -- indice usado na contagem do limite de 5 viagens por semana (RN05)
    KEY idx_viagem_limite_semanal (cidadao_id, status, data_hora)
) ENGINE=InnoDB;

CREATE TABLE pagamento (
    id             INT          NOT NULL AUTO_INCREMENT,
    viagem_id      INT          NOT NULL,
    valor_total    DECIMAL(6,2) NOT NULL,
    valor_subsidio DECIMAL(6,2) NOT NULL,
    valor_cidadao  DECIMAL(6,2) NOT NULL,
    forma_pagamento ENUM('CREDITO','DEBITO','PIX') NOT NULL,
    autorizado     TINYINT(1)   NOT NULL DEFAULT 0,
    data_pagamento DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pagamento_viagem (viagem_id),
    CONSTRAINT fk_pagamento_viagem FOREIGN KEY (viagem_id)
        REFERENCES viagem (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Usuario administrador inicial (senha: admin123)
-- IMPORTANTE: Gere o hash executando:
--   java -cp "bin;lib/mysql-connector-j.jar" br.com.transportesocial.util.Senha admin123
-- Depois substitua TROCAR_PELO_HASH pelo valor gerado.
-- ---------------------------------------------------------------------
INSERT INTO usuario (nome, cpf, data_nascimento, email, telefone, senha, tipo, status) VALUES
('Administrador da Prefeitura', '000.000.000-00', '1990-01-01', 'admin@marialva.pr.gov.br',
 '(44) 3232-0000', 'TROCAR_PELO_HASH', 'ADMINISTRADOR', 'APROVADO');
