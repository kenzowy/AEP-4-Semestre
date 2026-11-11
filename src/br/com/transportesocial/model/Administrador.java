package br.com.transportesocial.model;

import java.time.LocalDate;

/**
 * Servidor da prefeitura responsavel por analisar os cadastros.
 */
public class Administrador extends Usuario {

    public Administrador() {
    }

    public Administrador(String nome, String cpf, LocalDate dataNascimento, String email,
                         String telefone, String senha) {
        super(nome, cpf, dataNascimento, email, telefone, senha);
        setStatus(StatusCadastro.APROVADO);
    }

    @Override
    public String validarCadastro() {
        String erro = validarDadosBasicos();
        if (erro != null) {
            return erro;
        }
        if (calcularIdade() < 18) {
            return "O administrador precisa ser maior de idade.";
        }
        return null;
    }

    @Override
    public String getTipo() {
        return "ADMINISTRADOR";
    }
}
