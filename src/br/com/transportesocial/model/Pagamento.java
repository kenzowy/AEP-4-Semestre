package br.com.transportesocial.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pagamento da parte do cidadao em uma viagem (RN08). */
public class Pagamento {

    private long id;
    private long viagemId;
    private BigDecimal valorTotal;
    private BigDecimal valorSubsidio;
    private BigDecimal valorCidadao;
    private FormaPagamento formaPagamento;
    private boolean autorizado;
    private LocalDateTime dataPagamento = LocalDateTime.now();

    public Pagamento() {
    }

    public Pagamento(long viagemId, BigDecimal valorTotal, BigDecimal valorSubsidio,
                     BigDecimal valorCidadao, FormaPagamento formaPagamento) {
        this.viagemId = viagemId;
        this.valorTotal = valorTotal;
        this.valorSubsidio = valorSubsidio;
        this.valorCidadao = valorCidadao;
        this.formaPagamento = formaPagamento;
    }

    /**
     * Simula a autorizacao da operadora de cartao / Pix.
     * O projeto nao integra com instituicao financeira real.
     */
    public boolean processar() {
        this.autorizado = valorCidadao != null
                && valorCidadao.compareTo(BigDecimal.ZERO) >= 0
                && formaPagamento != null;
        this.dataPagamento = LocalDateTime.now();
        return autorizado;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getViagemId() { return viagemId; }
    public void setViagemId(long viagemId) { this.viagemId = viagemId; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public BigDecimal getValorSubsidio() { return valorSubsidio; }
    public void setValorSubsidio(BigDecimal valorSubsidio) { this.valorSubsidio = valorSubsidio; }

    public BigDecimal getValorCidadao() { return valorCidadao; }
    public void setValorCidadao(BigDecimal valorCidadao) { this.valorCidadao = valorCidadao; }

    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }

    public boolean isAutorizado() { return autorizado; }
    public void setAutorizado(boolean autorizado) { this.autorizado = autorizado; }

    public LocalDateTime getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDateTime dataPagamento) { this.dataPagamento = dataPagamento; }
}
