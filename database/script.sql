-- =====================================================================
-- Projeto Transporte Social - AEP 4o Semestre (Engenharia de Software)
-- Script de criacao do banco (MySQL / MariaDB via XAMPP + phpMyAdmin)
-- Segue o DER da 1a entrega (secao 5.3 do documento).
--
-- Como usar: phpMyAdmin > aba SQL > colar este script > Executar
-- (ou aba Importar > escolher este arquivo > Executar)
-- =====================================================================

DROP DATABASE IF EXISTS transporte_social;
CREATE DATABASE transporte_social DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE transporte_social;

-- ---------------------------------------------------------------------
-- usuario: dados comuns a todos (superclasse Usuario)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id              INT(11)      NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(100) NOT NULL,
    cpf             VARCHAR(14)  NOT NULL,
    data_nascimento DATE         NOT NULL,
    email           VARCHAR(100) NOT NULL,
    telefone        VARCHAR(20),
    senha           VARCHAR(255) NOT NULL,          -- hash BCrypt, nunca a senha digitada
    tipo            ENUM('CIDADAO','MOTORISTA','ADMIN') NOT NULL,
    status          ENUM('PENDENTE','APROVADO','RECUSADO') NOT NULL DEFAULT 'PENDENTE',
    motivo_recusa   VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_cpf (cpf)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- cidadao e motorista: heranca no banco (usuario_id e PK e FK ao mesmo tempo)
-- ---------------------------------------------------------------------
CREATE TABLE cidadao (
    usuario_id     INT(11)       NOT NULL,
    renda_familiar DECIMAL(10,2) NOT NULL,
    nis            VARCHAR(11),
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_cidadao_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE motorista (
    usuario_id INT(11)     NOT NULL,
    cnh        VARCHAR(11) NOT NULL,
    disponivel TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_motorista_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- documento: composicao 1:N com usuario (somem junto com o usuario)
-- ---------------------------------------------------------------------
CREATE TABLE documento (
    id         INT(11)      NOT NULL AUTO_INCREMENT,
    usuario_id INT(11)      NOT NULL,
    tipo       ENUM('CPF','COMPROVANTE_RENDA','ANTECEDENTES_CRIMINAIS','CNH') NOT NULL,
    arquivo    VARCHAR(255) NOT NULL,
    data_envio DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_documento_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- veiculo: composicao 1:N com motorista (placa unica)
-- ---------------------------------------------------------------------
CREATE TABLE veiculo (
    id           INT(11)     NOT NULL AUTO_INCREMENT,
    motorista_id INT(11)     NOT NULL,
    placa        VARCHAR(8)  NOT NULL,
    marca        VARCHAR(50) NOT NULL,
    modelo       VARCHAR(50) NOT NULL,
    ano          INT(11)     NOT NULL,
    cor          VARCHAR(30),
    PRIMARY KEY (id),
    UNIQUE KEY uk_veiculo_placa (placa),
    CONSTRAINT fk_veiculo_motorista FOREIGN KEY (motorista_id)
        REFERENCES motorista (usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- viagem: cidadao solicita (1:N); motorista realiza (0..1 : N)
-- ---------------------------------------------------------------------
CREATE TABLE viagem (
    id                INT(11)      NOT NULL AUTO_INCREMENT,
    cidadao_id        INT(11)      NOT NULL,
    motorista_id      INT(11)      NULL,
    origem            VARCHAR(200) NOT NULL,
    destino           VARCHAR(200) NOT NULL,
    distancia_km      DECIMAL(6,2) NOT NULL,
    valor_total       DECIMAL(6,2) NOT NULL,
    valor_subsidio    DECIMAL(6,2) NOT NULL,
    valor_cidadao     DECIMAL(6,2) NOT NULL,
    qtd_acompanhantes INT(11)      NOT NULL DEFAULT 0,
    data_hora         DATETIME     NOT NULL,
    status            ENUM('SOLICITADA','ACEITA','EM_ANDAMENTO','CONCLUIDA','CANCELADA')
                      NOT NULL DEFAULT 'SOLICITADA',
    PRIMARY KEY (id),
    CONSTRAINT fk_viagem_cidadao   FOREIGN KEY (cidadao_id)   REFERENCES cidadao (usuario_id),
    CONSTRAINT fk_viagem_motorista FOREIGN KEY (motorista_id) REFERENCES motorista (usuario_id),
    -- indice usado na contagem do limite de 5 viagens por semana (RN05)
    KEY idx_viagem_limite_semanal (cidadao_id, status, data_hora)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- pagamento: composicao com viagem (so existe ligado a uma viagem)
-- ---------------------------------------------------------------------
CREATE TABLE pagamento (
    id             INT(11)      NOT NULL AUTO_INCREMENT,
    viagem_id      INT(11)      NOT NULL,
    valor          DECIMAL(6,2) NOT NULL,           -- parte paga pelo cidadao
    forma          ENUM('CREDITO','DEBITO','PIX') NOT NULL,
    aprovado       TINYINT(1)   NOT NULL DEFAULT 0,
    data_pagamento DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pagamento_viagem (viagem_id),
    CONSTRAINT fk_pagamento_viagem FOREIGN KEY (viagem_id)
        REFERENCES viagem (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Administrador inicial: criado pela propria prefeitura (nao se cadastra pelo sistema)
-- Login: CPF 000.000.000-00  |  Senha: admin123  (hash BCrypt abaixo)
-- ---------------------------------------------------------------------
INSERT INTO usuario (nome, cpf, data_nascimento, email, telefone, senha, tipo, status) VALUES
('Administrador da Prefeitura', '000.000.000-00', '1990-01-01', 'admin@prefeitura.gov.br',
 '(00) 0000-0000', '$2a$10$kPQyc/nlUx26ZS8MTHXR8.liyaIS.azzPZL8lolnRWXnHaPf2G5EG',
 'ADMIN', 'APROVADO');
