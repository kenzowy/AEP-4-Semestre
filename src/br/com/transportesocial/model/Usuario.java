package br.com.transportesocial.model;

import br.com.transportesocial.util.BCrypt;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Superclasse abstrata com o que e comum a todos os usuarios do sistema.
 * Cidadao, Motorista e Administrador herdam dela e cada um sobrescreve
 * validarCadastro() do seu jeito (polimorfismo).
 */
public abstract class Usuario {

    private int id;
    private String nome;
    private String cpf;
    private LocalDate dataNascimento;
    private String email;
    private String telefone;
    private String senha;              // sempre o hash BCrypt
    private StatusCadastro status = StatusCadastro.PENDENTE;
    private String motivoRecusa;

    /** Composicao 1:N: os documentos pertencem ao usuario. */
    private final List<Documento> documentos = new ArrayList<>();

    // ------------------------------------------------------------------
    // Metodos do diagrama de classes
    // ------------------------------------------------------------------

    /** Cada tipo de usuario confere o proprio cadastro (polimorfismo). */
    public abstract boolean validarCadastro();

    /** Altera os dados da conta (RF01 - alterar). */
    public void atualizarConta(String nome, String email, String telefone) {
        setNome(nome);
        setEmail(email);
        setTelefone(telefone);
        validarDadosBasicos();
    }

    /**
     * Exclui a conta (RF01 - excluir). Pela composicao, os documentos
     * (e no motorista os veiculos) deixam de existir junto com o usuario.
     */
    public void deletarConta() {
        documentos.clear();
    }

    /**
     * Anexa um documento ao cadastro (RF02). Se o cadastro tinha sido recusado,
     * o envio de novos documentos devolve o cadastro para analise (RN04).
     */
    public void enviarDocumento(Documento doc) {
        if (doc == null || doc.getTipo() == null) {
            throw new RegraNegocioException("Documento invalido.");
        }
        if (status == StatusCadastro.APROVADO) {
            throw new RegraNegocioException("Cadastro ja aprovado: nao e preciso enviar documentos.");
        }
        documentos.add(doc);
        if (status == StatusCadastro.RECUSADO) {
            status = StatusCadastro.PENDENTE;
            motivoRecusa = null;
        }
    }

    public int calcularIdade() {
        if (dataNascimento == null) {
            return 0;
        }
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    /** Valor gravado na coluna usuario.tipo. */
    public abstract String getTipo();

    /** Documentos que cada tipo de usuario precisa enviar (RF02). */
    public abstract List<TipoDocumento> getDocumentosObrigatorios();

    public boolean estaAprovado() {
        return status == StatusCadastro.APROVADO;
    }

    /** RF04: funcoes de viagem so funcionam com cadastro aprovado. */
    protected void exigirCadastroAprovado() {
        if (!estaAprovado()) {
            throw new RegraNegocioException(
                    "Seu cadastro ainda nao foi aprovado pela prefeitura. As funcoes de viagem estao bloqueadas.");
        }
    }

    public boolean possuiDocumento(TipoDocumento tipo) {
        for (Documento d : documentos) {
            if (d.getTipo() == tipo) {
                return true;
            }
        }
        return false;
    }

    /** Confere os campos comuns; usado pelas subclasses em validarCadastro(). */
    protected void validarDadosBasicos() {
        if (nome == null || nome.trim().length() < 3) {
            throw new RegraNegocioException("Informe o nome completo.");
        }
        if (cpf == null || !cpf.matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) {
            throw new RegraNegocioException("CPF invalido. Use 11 numeros.");
        }
        if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new RegraNegocioException("E-mail invalido.");
        }
        if (dataNascimento == null || dataNascimento.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("Data de nascimento invalida.");
        }
        if (senha == null || senha.isEmpty()) {
            throw new RegraNegocioException("Informe uma senha.");
        }
    }

    /** Confere se todos os documentos obrigatorios foram enviados. */
    protected void validarDocumentos() {
        for (TipoDocumento tipo : getDocumentosObrigatorios()) {
            if (!possuiDocumento(tipo)) {
                throw new RegraNegocioException("Envie o documento: " + tipo.name().replace('_', ' '));
            }
        }
    }

    /** Grava apenas o hash BCrypt da senha (RNF01). */
    public void definirSenha(String senhaDigitada) {
        if (senhaDigitada == null || senhaDigitada.length() < 6) {
            throw new RegraNegocioException("A senha precisa ter pelo menos 6 caracteres.");
        }
        this.senha = BCrypt.gerarHash(senhaDigitada);
    }

    public boolean conferirSenha(String senhaDigitada) {
        return BCrypt.conferir(senhaDigitada, senha);
    }

    /** Deixa o CPF sempre no formato 000.000.000-00. */
    public static String formatarCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        String n = cpf.replaceAll("\\D", "");
        if (n.length() != 11) {
            return cpf.trim();
        }
        return n.substring(0, 3) + "." + n.substring(3, 6) + "." + n.substring(6, 9) + "-" + n.substring(9);
    }

    // ------------------------------------------------------------------
    // Getters e setters (encapsulamento)
    // ------------------------------------------------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome == null ? null : nome.trim(); }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = formatarCpf(cpf); }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone == null ? null : telefone.trim(); }

    public String getSenha() { return senha; }
    /** Usado pelo DAO ao carregar o hash do banco. */
    public void setSenha(String hash) { this.senha = hash; }

    public StatusCadastro getStatus() { return status; }
    public void setStatus(StatusCadastro status) { this.status = status; }

    public String getMotivoRecusa() { return motivoRecusa; }
    public void setMotivoRecusa(String motivoRecusa) { this.motivoRecusa = motivoRecusa; }

    public List<Documento> getDocumentos() { return Collections.unmodifiableList(documentos); }
    /** Usado pelo DAO ao carregar os documentos do banco. */
    public void carregarDocumentos(List<Documento> lista) {
        documentos.clear();
        documentos.addAll(lista);
    }
}
