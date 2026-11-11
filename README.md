# Projeto Transporte Social — AEP 4º Semestre (Engenharia de Software)

Aplicativo de corridas simplificado, voltado para pessoas com renda familiar abaixo de R$ 2.000,00.
A prefeitura paga metade da corrida (teto de R$ 15,00) e cada cidadão tem direito a 5 viagens por semana.
ODS 10 (Redução das Desigualdades) e ODS 11 (Cidades e Comunidades Sustentáveis — meta 11.2).

**Equipe:** Kenzo Wilder Yamamoto (R.A. 25247917-2) · Sam Gomes Cangirana Osako (R.A. 25000548-2)
**Instituição:** Unicesumar (Maringá - PR)
**Stack:** Java 21 · MySQL (XAMPP/phpMyAdmin) · HTML, CSS e JavaScript

---

## Como executar

### Pré-requisitos
| Item | Onde baixar |
|------|-------------|
| Java JDK 21 ou superior | https://www.oracle.com/java/technologies/downloads/ |
| XAMPP (MySQL + phpMyAdmin) | https://www.apachefriends.org/ |
| MySQL Connector/J (Plataforma Independente) | https://dev.mysql.com/downloads/connector/j/ |

1. Coloque o `.jar` do Connector/J na pasta `lib/` com o nome **`mysql-connector-j.jar`**.
2. No XAMPP, ligue o **MySQL**. Abra http://localhost/phpmyadmin, aba **SQL**, cole o conteúdo de `database/script.sql` e clique em **Executar**.
3. Na pasta do projeto, compile e execute:

**Windows (Git Bash)**
```bash
javac -encoding UTF-8 -cp "lib/mysql-connector-j.jar" -d bin $(find src -name "*.java")
java -cp "bin;lib/mysql-connector-j.jar" br.com.transportesocial.Main
```

**Linux / Mac**
```bash
javac -encoding UTF-8 -cp "lib/mysql-connector-j.jar" -d bin $(find src -name "*.java")
java -cp "bin:lib/mysql-connector-j.jar" br.com.transportesocial.Main
```

4. Abra no navegador: **http://localhost:8080**

**Administrador da prefeitura:** CPF `000.000.000-00` · senha `admin123`

> O MySQL do XAMPP usa o usuário `root` sem senha. Se o seu for diferente, altere `src/br/com/transportesocial/util/Conexao.java`.

---

## Lista de Requisitos Funcionais (escopo)

- **RF01** – O sistema deve permitir cadastrar, consultar, alterar e excluir usuários, identificando se o usuário é cidadão ou motorista.
- **RF02** – O sistema deve permitir o envio dos documentos exigidos no cadastro: CPF e comprovante de renda para o cidadão, CNH para o motorista e certidão de antecedentes criminais para os dois (RN01 e RN03).
- **RF03** – O sistema deve permitir que o administrador da prefeitura consulte os cadastros pendentes, aprove ou recuse cada um e registre o motivo da recusa (RN04).
- **RF04** – O sistema deve bloquear as funções de viagem enquanto o cadastro do usuário não estiver aprovado (RN04).
- **RF05** – O sistema deve permitir que o motorista cadastre e altere os dados do seu veículo (placa, marca, modelo, ano e cor).
- **RF06** – O sistema deve permitir que o motorista informe se está disponível ou indisponível para receber viagens.
- **RF07** – O sistema deve permitir que o cidadão solicite uma viagem informando origem, destino e quantos menores vão acompanhá-lo (RN02).
- **RF08** – O sistema deve calcular e mostrar, antes da confirmação da viagem, o valor total, o valor pago pela prefeitura e o valor que o cidadão vai pagar (RN06 e RN07).
- **RF09** – O sistema deve permitir que o motorista disponível veja as solicitações em aberto e aceite uma delas.
- **RF10** – O sistema deve mostrar ao cidadão o status da viagem e o tempo estimado de chegada do motorista.
- **RF11** – O sistema deve registrar o pagamento da parte do cidadão por cartão de crédito, cartão de débito ou Pix, guardando também o valor do subsídio que a prefeitura vai repassar (RN08).
- **RF12** – O sistema deve informar quantas viagens ainda restam na semana e impedir novas solicitações quando o limite for atingido (RN05).
- **RF13** – O sistema deve permitir que o cidadão e o motorista consultem o histórico das suas viagens.
- **RF14** – O sistema deve permitir que o administrador gere um relatório com as viagens realizadas e o total de subsídio pago em um período.

### Requisitos Não Funcionais (RNF)
- **RNF01 – Segurança:** as senhas são gravadas com hash (BCrypt) e cada tela só é liberada para o perfil correspondente (cidadão, motorista ou administrador).
- **RNF02 – Privacidade (LGPD):** os documentos enviados no cadastro só podem ser abertos pelo administrador da prefeitura e são usados apenas para a análise de elegibilidade.
- **RNF03 – Usabilidade:** o sistema roda no navegador do celular, com layout responsivo.
- **RNF04 – Desempenho:** a tela de solicitação deve mostrar o valor calculado da viagem em até 3 segundos.
- **RNF05 – Disponibilidade:** o sistema deve ficar no ar das 5h às 23h, que é a faixa de horário prevista para as viagens.
- **RNF06 – Compatibilidade:** funcionar nas versões atuais do Chrome, do Firefox e do Edge.
- **RNF07 – Manutenibilidade:** o back-end é dividido em camadas (modelo, persistência e controle), separando as regras de negócio do acesso ao banco.

### Regras de Negócio (RN)
- **RN01 – Elegibilidade do cidadão:** renda familiar bruta mensal abaixo de R$ 2.000,00, idade mínima de 18 anos e nenhum antecedente criminal.
- **RN02 – Menores de idade:** menores de 18 anos não têm cadastro próprio e só viajam junto com um responsável cadastrado, que informa quantos menores vão na hora de pedir a viagem.
- **RN03 – Motorista:** CNH válida com a observação EAR (Exerce Atividade Remunerada), ficha limpa e nenhum processo em aberto por crime ou por conduta que coloque o passageiro em risco.
- **RN04 – Aprovação do cadastro:** todo cadastro nasce com o status PENDENTE e só passa para APROVADO depois da análise do administrador. O cadastro recusado recebe o motivo e pode ser refeito com novos documentos.
- **RN05 – Limite semanal:** no máximo 5 viagens por cidadão por semana, contando de segunda a domingo. Só entram na conta as viagens concluídas, e os menores acompanhantes não contam como viagem separada.
- **RN06 – Subsídio:** a prefeitura paga 50% do valor da viagem, com teto de R$ 15,00. Até R$ 30,00 ela paga a metade; acima disso paga R$ 15,00 e o restante fica com o cidadão.
- **RN07 – Valor da viagem:** tarifa base mais um valor por quilômetro rodado, os dois definidos pela prefeitura (exemplo: R$ 5,00 + R$ 2,00 por km).
- **RN08 – Pagamento:** só cartão de crédito, cartão de débito ou Pix, e apenas sobre a parte do cidadão. Não existe pagamento em dinheiro.
- **RN09 – Cancelamento:** o cidadão pode cancelar sem custo enquanto o motorista não chegou ao local de embarque, e a solicitação que ninguém aceitar em 10 minutos é cancelada pelo sistema. Viagem cancelada não gera subsídio nem cobrança.

---

## Cronograma (planejamento ágil)

| Sprint / Data | Épico | User Story | Responsável |
|---|---|---|---|
| Sprint 1 — 05/09 a 19/09 | Cadastro de usuários (RF01, RF02 e RF05) | COMO UM cidadão de baixa renda EU QUERO me cadastrar enviando meus documentos (CPF, comprovante de renda e antecedentes criminais) PARA QUE eu possa ter acesso ao transporte subsidiado. COMO UM motorista EU QUERO me cadastrar com minha CNH, antecedentes criminais e os dados do meu veículo PARA QUE eu possa realizar viagens pelo sistema. | Kenzo Wilder Yamamoto |
| Sprint 2 — 20/09 a 04/10 | Aprovação de cadastros (RF03 e RF04) | COMO UM administrador da prefeitura EU QUERO analisar os documentos e aprovar ou recusar cadastros de cidadãos e motoristas PARA QUE apenas pessoas elegíveis e com ficha limpa utilizem o sistema. | Sam Gomes Cangirana Osako |
| Sprint 3 — 05/10 a 19/10 | Solicitação e aceite de viagem (RF06 a RF10) | COMO UM cidadão aprovado EU QUERO solicitar uma viagem vendo o valor total, o desconto da prefeitura e quanto vou pagar PARA QUE eu possa me locomover pela cidade pagando menos. COMO UM motorista EU QUERO visualizar e aceitar as solicitações de viagem PARA QUE eu possa atender os passageiros. | Kenzo Wilder Yamamoto |
| Sprint 4 — 20/10 a 03/11 | Pagamento e limite semanal (RF11 a RF14) | COMO UM cidadão EU QUERO pagar a minha parte por crédito, débito ou Pix e ver quantas viagens ainda posso fazer na semana PARA QUE eu controle o uso do meu benefício. COMO UM administrador EU QUERO gerar um relatório de viagens e subsídios pagos PARA QUE a prefeitura acompanhe os gastos do programa. | Sam Gomes Cangirana Osako |

---

## Estrutura do repositório

```
AEP-4-Semestre/
├── database/script.sql        # criação do banco (igual ao DER da 1ª entrega)
├── docs/                      # documento da 1ª entrega (requisitos, diagramas, DER)
├── lib/mysql-connector-j.jar  # driver JDBC do MySQL
├── src/br/com/transportesocial/
│   ├── Main.java
│   ├── model/      # Usuario (abstrata), Cidadao, Motorista, Administrador, Viagem, Pagamento, Veiculo, Documento, enums
│   ├── dao/        # persistência: UsuarioDAO, DocumentoDAO, VeiculoDAO, ViagemDAO, PagamentoDAO
│   ├── controle/   # camada de controle: servidor HTTP e rotas da API (JSON)
│   └── util/       # Conexao, BCrypt, Json, Sessao, ArmazenamentoArquivo
└── web/            # telas: index.html, css/estilo.css, js/app.js
```

## POO aplicada
- **Classe abstrata e herança:** `Usuario` (abstrata) → `Cidadao`, `Motorista`, `Administrador`; `ControleBase` (abstrata) → controles da API.
- **Polimorfismo (@Override):** `validarCadastro()` — Cidadao confere idade ≥ 18, renda < R$ 2.000 e documentos; Motorista confere CNH e documentos; Administrador só é criado pela prefeitura.
- **Composição 1:N:** Usuario ◆ Documento, Motorista ◆ Veiculo, Viagem ◆ Pagamento.
- **Encapsulamento:** atributos privados, regras fixas como constantes (`LIMITE_SEMANAL`, `TETO_SUBSIDIO`, `PERCENTUAL_SUBSIDIO`) e dinheiro em `BigDecimal`.
