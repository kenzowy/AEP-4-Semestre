package br.com.transportesocial.model;

import java.time.LocalDate;

/**
 * Motorista cadastrado para atender as viagens do programa.
 */
public class Motorista extends Usuario {

    private String cnh;
    private boolean disponivel;

    public Motorista() {
    }

    public Motorista(String nome, String cpf, LocalDate dataNascimento, String email,
                     String telefone, String senha, String cnh) {
        super(nome, cpf, dataNascimento, email, telefone, senha);
        this.cnh = cnh;
    }

    @Override
    public String validarCadastro() {
        String erro = validarDadosBasicos();
        if (erro != null) {
            return erro;
        }
        if (calcularIdade() < 21) {
            return "O motorista precisa ter no minimo 21 anos.";
        }
        if (cnh == null || cnh.replaceAll("\\D", "").length() != 11) {
            return "Informe uma CNH valida (11 digitos).";
        }
        return null;
    }

    @Override
    public String getTipo() {
        return "MOTORISTA";
    }

    public String getCnh() { return cnh; }
    public void setCnh(String cnh) { this.cnh = cnh; }

    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
}
