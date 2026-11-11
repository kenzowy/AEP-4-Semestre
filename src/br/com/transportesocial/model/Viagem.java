package br.com.transportesocial.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Viagem solicitada por um cidadao. Concentra as regras de calculo
 * do valor e do subsidio pago pela prefeitura (RN06 e RN07).
 */
public class Viagem {

    /** Tarifa fixa cobrada em qualquer viagem (RN07). */
    public static final BigDecimal TARIFA_BASE = new BigDecimal("5.00");

    /** Valor cobrado por quilometro rodado (RN07). */
    public static final BigDecimal VALOR_KM = new BigDecimal("2.00");

    /** Percentual pago pela prefeitura (RN06). */
    public static final BigDecimal PERCENTUAL_SUBSIDIO = new BigDecimal("0.50");

    /** Valor maximo que a prefeitura paga por viagem (RN06). */
    public static final BigDecimal TETO_SUBSIDIO = new BigDecimal("15.00");

    private long id;
    private long cidadaoId;
    private Long motoristaId;
    private String origem;
    private String destino;
    private BigDecimal distanciaKm = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private BigDecimal valorSubsidio = BigDecimal.ZERO;
    private BigDecimal valorCidadao = BigDecimal.ZERO;
    private LocalDateTime dataHora = LocalDateTime.now();
    private StatusViagem status = StatusViagem.SOLICITADA;

    // campos auxiliares usados apenas para exibicao nas telas
    private String nomeCidadao;
    private String nomeMotorista;
    private boolean pago;

    public Viagem() {
    }

    public Viagem(long cidadaoId, String origem, String destino, BigDecimal distanciaKm) {
        this.cidadaoId = cidadaoId;
        this.origem = origem;
        this.destino = destino;
        this.distanciaKm = distanciaKm;
        calcularValores();
    }

    /** Valor cheio da corrida: tarifa base + valor por km. */
    public BigDecimal calcularValor() {
        BigDecimal km = distanciaKm == null ? BigDecimal.ZERO : distanciaKm;
        return TARIFA_BASE.add(VALOR_KM.multiply(km)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Parte paga pela prefeitura: 50% do valor, limitado ao teto de R$ 15,00. */
    public BigDecimal calcularSubsidio() {
        BigDecimal metade = calcularValor().multiply(PERCENTUAL_SUBSIDIO)
                .setScale(2, RoundingMode.HALF_UP);
        return metade.min(TETO_SUBSIDIO);
    }

    /** Recalcula e guarda os tres valores da viagem. */
    public void calcularValores() {
        this.valorTotal = calcularValor();
        this.valorSubsidio = calcularSubsidio();
        this.valorCidadao = valorTotal.subtract(valorSubsidio).setScale(2, RoundingMode.HALF_UP);
    }

    /** @return mensagem de erro ou null quando a solicitacao esta valida */
    public String validar() {
        if (origem == null || origem.isBlank() || destino == null || destino.isBlank()) {
            return "Informe a origem e o destino.";
        }
        if (distanciaKm == null || distanciaKm.compareTo(BigDecimal.ZERO) <= 0) {
            return "Informe a distancia da viagem.";
        }
        return null;
    }

    /**
     * Tempo estimado, em minutos, para o motorista chegar ate o passageiro (RF10).
     * Considera 3 minutos de deslocamento inicial mais 2 minutos por quilometro.
     */
    public int calcularTempoEstimado() {
        BigDecimal km = distanciaKm == null ? BigDecimal.ZERO : distanciaKm;
        return 3 + km.multiply(new BigDecimal("2")).setScale(0, RoundingMode.CEILING).intValue();
    }

    public boolean podeSerCancelada() {
        return status == StatusViagem.SOLICITADA || status == StatusViagem.ACEITA;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getCidadaoId() { return cidadaoId; }
    public void setCidadaoId(long cidadaoId) { this.cidadaoId = cidadaoId; }

    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long motoristaId) { this.motoristaId = motoristaId; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public BigDecimal getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(BigDecimal distanciaKm) { this.distanciaKm = distanciaKm; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public BigDecimal getValorSubsidio() { return valorSubsidio; }
    public void setValorSubsidio(BigDecimal valorSubsidio) { this.valorSubsidio = valorSubsidio; }

    public BigDecimal getValorCidadao() { return valorCidadao; }
    public void setValorCidadao(BigDecimal valorCidadao) { this.valorCidadao = valorCidadao; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public StatusViagem getStatus() { return status; }
    public void setStatus(StatusViagem status) { this.status = status; }

    public String getNomeCidadao() { return nomeCidadao; }
    public void setNomeCidadao(String nomeCidadao) { this.nomeCidadao = nomeCidadao; }

    public String getNomeMotorista() { return nomeMotorista; }
    public void setNomeMotorista(String nomeMotorista) { this.nomeMotorista = nomeMotorista; }

    public boolean isPago() { return pago; }
    public void setPago(boolean pago) { this.pago = pago; }
}
