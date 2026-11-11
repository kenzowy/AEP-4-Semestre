package br.com.transportesocial.model;

import java.time.LocalDate;
import java.time.Period;

/**
 * Superclasse abstrata de todos os usuarios do sistema.
 * Guarda os atributos comuns e deixa a validacao do cadastro para as subclasses.
 */
public abstract class Usuario {

    private long id;
    private String nome;
    private String cpf;
    private LocalDate dataNascimento;
    private String email;
    private String telefone;
    private String senha;
    private StatusCadastro status = StatusCadastro.PENDENTE;
    private String motivoRecusa;

    public Usuario() {
    }

    public Usuario(String nome, String cpf, LocalDate dataNascimento, String email,
                   String telefone, String senha) {
        this.nome = nome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.email = email;
        this.telefone = telefone;
        this.senha = senha;
    }

    /**
     * Regra de elegibilidade de cada tipo de usuario.
     * Metodo polimorfico: cada subclasse sobrescreve com @Override.
     *
     * @return mensagem de erro, ou null quando o cadastro esta valido
     */
    public abstract String validarCadastro();

    /** Nome exibido do tipo de usuario (tambem sobrescrito nas subclasses). */
    public abstract String getTipo();

    public int calcularIdade() {
        if (dataNascimento == null) {
            return 0;
        }
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    /** Validacoes comuns a qualquer usuario, reaproveitadas pelas subclasses. */
    protected String validarDadosBasicos() {
        if (nome == null || nome.trim().length() < 3) {
            return "Informe o nome completo.";
        }
        if (cpf == null || cpf.replaceAll("\\D", "").length() != 11) {
            return "CPF invalido.";
        }
        if (email == null || !email.contains("@")) {
            return "E-mail invalido.";
        }
        if (senha == null || senha.length() < 6) {
            return "A senha deve ter no minimo 6 caracteres.";
        }
        if (dataNascimento == null) {
            return "Informe a data de nascimento.";
        }
        return null;
    }

    public boolean isAprovado() {
        return status == StatusCadastro.APROVADO;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public StatusCadastro getStatus() { return status; }
    public void setStatus(StatusCadastro status) { this.status = status; }

    public String getMotivoRecusa() { return motivoRecusa; }
    public void setMotivoRecusa(String motivoRecusa) { this.motivoRecusa = motivoRecusa; }

    @Override
    public String toString() {
        return getTipo() + " #" + id + " - " + nome;
    }
}
