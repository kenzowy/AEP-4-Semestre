# Transporte Social — AEP 4º Semestre

Sistema de transporte subsidiado para cidadãos de baixa renda.  
**Stack:** Java 21 · MySQL (XAMPP) · HTML/CSS/JS puro

---

## Pré-requisitos

| Item | Versão |
|------|--------|
| Java JDK | 21 ou superior |
| XAMPP | Qualquer versão com MariaDB/MySQL |
| mysql-connector-j.jar | 9.x (baixar em https://dev.mysql.com/downloads/connector/j/) |

---

## 1 — Configurar o banco de dados

1. Abra o **XAMPP Control Panel** e inicie o serviço **MySQL**.
2. Acesse **phpMyAdmin** em `http://localhost/phpmyadmin`.
3. Vá em **SQL** e importe o arquivo:  
   ```
   database/script.sql
   ```
   Isso criará o banco `transporte_social` com todas as tabelas.

---

## 2 — Gerar a senha do administrador

O usuário admin já está inserido no script, mas a senha (`admin123`) precisa ser convertida em hash PBKDF2 antes de ser salva.

**Passo 1 — Compilar:**
```bash
# Windows
javac -cp "lib/mysql-connector-j.jar" -d bin src\br\com\transportesocial\util\Senha.java

# Linux/Mac
javac -cp "lib/mysql-connector-j.jar" -d bin src/br/com/transportesocial/util/Senha.java
```

**Passo 2 — Gerar o hash:**
```bash
# Windows
java -cp "bin;lib/mysql-connector-j.jar" br.com.transportesocial.util.Senha admin123

# Linux/Mac
java -cp "bin:lib/mysql-connector-j.jar" br.com.transportesocial.util.Senha admin123
```

**Passo 3 — Atualizar o banco:**  
Copie o hash exibido no terminal e execute no phpMyAdmin:
```sql
UPDATE usuario
SET senha = 'COLE_O_HASH_AQUI'
WHERE email = 'admin@marialva.pr.gov.br';
```

---

## 3 — Compilar o projeto completo

Coloque o arquivo `mysql-connector-j-*.jar` dentro da pasta `lib/` e renomeie para `mysql-connector-j.jar`.

```bash
# Windows (a partir da raiz do projeto)
javac -cp "lib/mysql-connector-j.jar" -d bin src\br\com\transportesocial\util\*.java src\br\com\transportesocial\model\*.java src\br\com\transportesocial\dao\*.java src\br\com\transportesocial\web\*.java src\br\com\transportesocial\Main.java

# Ou use o atalho (compila tudo de uma vez):
for /r src %%f in (*.java) do javac -cp "lib/mysql-connector-j.jar" -d bin "%%f"
```

```bash
# Linux/Mac (a partir da raiz do projeto)
javac -cp "lib/mysql-connector-j.jar" -d bin $(find src -name "*.java")
```

---

## 4 — Executar o sistema

```bash
# Windows
java -cp "bin;lib/mysql-connector-j.jar" br.com.transportesocial.Main

# Linux/Mac
java -cp "bin:lib/mysql-connector-j.jar" br.com.transportesocial.Main
```

Acesse no navegador: **http://localhost:8080**

---

## Credenciais padrão do administrador

| Campo | Valor |
|-------|-------|
| E-mail | `admin@marialva.pr.gov.br` |
| Senha | `admin123` |

---

## Estrutura do projeto

```
AEP-4-Semestre/
├── database/
│   └── script.sql          # Script de criação do banco
├── docs/                   # Documento da 1ª entrega (PDF + diagramas)
├── lib/
│   └── mysql-connector-j.jar  # Driver JDBC (baixar separado)
├── src/br/com/transportesocial/
│   ├── Main.java
│   ├── dao/                # UsuarioDAO, ViagemDAO, VeiculoDAO, PagamentoDAO
│   ├── model/              # Usuario (abstrato), Cidadao, Motorista, Administrador, Viagem...
│   ├── util/               # Conexao, Json, Senha
│   └── web/                # Servidor.java (HttpServer)
├── web/
│   ├── index.html
│   ├── css/estilo.css
│   └── js/app.js
└── README.md
```

---

## Requisitos funcionais implementados

| RF | Descrição |
|----|-----------|
| RF01 | Cadastro de cidadão com renda familiar e NIS |
| RF02 | Cadastro de motorista com CNH |
| RF03 | Aprovação/recusa de cadastros pelo administrador |
| RF04 | Login com autenticação PBKDF2 |
| RF05 | Cadastro e gerenciamento de veículos |
| RF06 | Solicitação de viagem pelo cidadão |
| RF07 | Cálculo automático de tarifas e subsídio (50%, teto R$15) |
| RF08 | Listagem de solicitações disponíveis para motoristas |
| RF09 | Aceitação de viagem pelo motorista |
| RF10 | Controle de status da viagem (SOLICITADA→ACEITA→EM_ANDAMENTO→CONCLUIDA/CANCELADA) |
| RF11 | Pagamento da viagem (Pix, Débito, Crédito) |
| RF12 | Limite de 5 viagens concluídas por semana por cidadão |
| RF13 | Controle de disponibilidade do motorista |
| RF14 | Exclusão de usuários pelo administrador |

---

## Tecnologias e padrões utilizados

- **Java 21** — sem Maven, sem frameworks externos
- **POO:** Classe abstrata `Usuario` com herança (`Cidadao`, `Motorista`, `Administrador`) e polimorfismo com `@Override` em `validarCadastro()` e `getTipo()`
- **JDBC** com `mysql-connector-j.jar` para persistência no MySQL
- **com.sun.net.httpserver.HttpServer** para o servidor HTTP (porta 8080)
- **PBKDF2WithHmacSHA256** para hash seguro de senhas (embutido no JDK)
- **BigDecimal** para todos os valores monetários
- **Arquitetura em camadas:** `model` → `dao` → `web`
