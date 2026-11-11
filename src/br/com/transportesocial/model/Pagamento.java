package br.com.transportesocial.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pagamento da parte do cidadao (RF11). So existe ligado a uma Viagem
 * (composicao). O pagamento por cartao e Pix e simulado, sem integracao
 * com instituicao financeira (fora do escopo).
 */
public class Pagamento {

    private int id;
    private BigDecimal valor;
    private FormaPagamento forma;
    private boolean aprovado;
    private LocalDateTime dataHora;

    public Pagamento() {
    }

    public Pagamento(BigDecimal valor, FormaPagamento forma) {
        this.valor = valor;
        this.forma = forma;
        this.dataHora = LocalDateTime.now();
    }

    /** Simula a autorizacao do pagamento. */
    public boolean processar() {
        aprovado = valor != null && valor.signum() > 0 && forma != null;
        return aprovado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public FormaPagamento getForma() { return forma; }
    public void setForma(FormaPagamento forma) { this.forma = forma; }

    public boolean isAprovado() { return aprovado; }
    public void setAprovado(boolean aprovado) { this.aprovado = aprovado; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
