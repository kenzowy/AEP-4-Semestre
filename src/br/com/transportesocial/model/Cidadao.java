package br.com.transportesocial.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cidadao de baixa renda que utiliza o transporte subsidiado.
 */
public class Cidadao extends Usuario {

    /** Maximo de viagens concluidas por semana (RN05). */
    public static final int LIMITE_SEMANAL = 5;

    /** Teto de renda familiar mensal para participar do programa (RN01). */
    public static final BigDecimal RENDA_MAXIMA = new BigDecimal("2000.00");

    /** Idade minima para ter cadastro proprio (RN01). */
    public static final int IDADE_MINIMA = 18;

    private BigDecimal rendaFamiliar;
    private String nis;

    public Cidadao() {
    }

    public Cidadao(String nome, String cpf, LocalDate dataNascimento, String email,
                   String telefone, String senha, BigDecimal rendaFamiliar, String nis) {
        super(nome, cpf, dataNascimento, email, telefone, senha);
        this.rendaFamiliar = rendaFamiliar;
        this.nis = nis;
    }

    @Override
    public String validarCadastro() {
        String erro = validarDadosBasicos();
        if (erro != null) {
            return erro;
        }
        if (calcularIdade() < IDADE_MINIMA) {
            return "O cidadao precisa ter no minimo " + IDADE_MINIMA + " anos.";
        }
        if (rendaFamiliar == null || rendaFamiliar.compareTo(BigDecimal.ZERO) < 0) {
            return "Informe a renda familiar.";
        }
        if (rendaFamiliar.compareTo(RENDA_MAXIMA) >= 0) {
            return "O programa atende apenas renda familiar abaixo de R$ 2.000,00.";
        }
        return null;
    }

    @Override
    public String getTipo() {
        return "CIDADAO";
    }

    public BigDecimal getRendaFamiliar() { return rendaFamiliar; }
    public void setRendaFamiliar(BigDecimal rendaFamiliar) { this.rendaFamiliar = rendaFamiliar; }

    public String getNis() { return nis; }
    public void setNis(String nis) { this.nis = nis; }
}
