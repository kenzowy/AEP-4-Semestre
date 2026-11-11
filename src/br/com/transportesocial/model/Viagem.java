package br.com.transportesocial.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Corrida solicitada por um cidadao e realizada por um motorista. */
public class Viagem {

    /** RN06: a prefeitura paga 50% da viagem, com teto de R$ 15,00. */
    public static final BigDecimal TETO_SUBSIDIO = new BigDecimal("15.00");
    public static final BigDecimal PERCENTUAL_SUBSIDIO = new BigDecimal("0.50");

    /** RN07: tarifa base + valor por km, definidos pela prefeitura. */
    public static final BigDecimal TARIFA_BASE = new BigDecimal("5.00");
    public static final BigDecimal VALOR_POR_KM = new BigDecimal("2.00");

    /** RN09: solicitacao sem motorista apos 10 minutos e cancelada pelo sistema. */
    public static final int MINUTOS_PARA_EXPIRAR = 10;

    /** Velocidade media urbana usada para estimar o tempo de chegada (RF10). */
    public static final double VELOCIDADE_MEDIA_KMH = 30.0;

    private int id;
    private String origem;
    private String destino;
    private double distanciaKm;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private BigDecimal valorSubsidio = BigDecimal.ZERO;
    private BigDecimal valorCidadao = BigDecimal.ZERO;
    private int qtdAcompanhantes;
    private LocalDateTime dataHora;
    private StatusViagem status = StatusViagem.SOLICITADA;

    private Cidadao cidadao;        // quem solicitou (1)
    private Motorista motorista;    // quem realiza (0..1)
    private Pagamento pagamento;    // composicao (0..1)

    public Viagem() {
    }

    public Viagem(String origem, String destino, int qtdAcompanhantes) {
        if (origem == null || origem.isBlank() || destino == null || destino.isBlank()) {
            throw new RegraNegocioException("Informe a origem e o destino.");
        }
        if (qtdAcompanhantes < 0) {
            throw new RegraNegocioException("Quantidade de menores acompanhantes invalida.");
        }
        this.origem = origem.trim();
        this.destino = destino.trim();
        this.qtdAcompanhantes = qtdAcompanhantes;
        this.dataHora = LocalDateTime.now();
    }

    /** RN07: valor total = tarifa base + km x valor por km. */
    public BigDecimal calcularValor() {
        if (distanciaKm <= 0) {
            throw new RegraNegocioException("Informe a distancia da viagem.");
        }
        valorTotal = TARIFA_BASE
                .add(VALOR_POR_KM.multiply(BigDecimal.valueOf(distanciaKm)))
                .setScale(2, RoundingMode.HALF_UP);
        return valorTotal;
    }

    /** RN06: subsidio = menor valor entre (total x 50%) e R$ 15,00. */
    public BigDecimal calcularSubsidio() {
        BigDecimal metade = valorTotal.multiply(PERCENTUAL_SUBSIDIO).setScale(2, RoundingMode.HALF_UP);
        valorSubsidio = metade.min(TETO_SUBSIDIO);
        valorCidadao = valorTotal.subtract(valorSubsidio);
        return valorSubsidio;
    }

    /** Controla o fluxo de status permitido para a viagem. */
    public void alterarStatus(StatusViagem novo) {
        boolean permitido = switch (status) {
            case SOLICITADA -> novo == StatusViagem.ACEITA || novo == StatusViagem.CANCELADA;
            case ACEITA -> novo == StatusViagem.EM_ANDAMENTO || novo == StatusViagem.CANCELADA;
            case EM_ANDAMENTO -> novo == StatusViagem.CONCLUIDA;
            default -> false;
        };
        if (!permitido) {
            throw new RegraNegocioException("Nao e possivel mudar a viagem de " + status + " para " + novo + ".");
        }
        status = novo;
        if (novo == StatusViagem.CANCELADA) {
            // RN09: viagem cancelada nao gera subsidio nem cobranca
            valorSubsidio = BigDecimal.ZERO.setScale(2);
            valorCidadao = BigDecimal.ZERO.setScale(2);
        }
    }

    /** RF10: tempo estimado de chegada, em minutos, pela distancia e velocidade media. */
    public int getTempoEstimadoMinutos() {
        return (int) Math.max(1, Math.ceil(distanciaKm / VELOCIDADE_MEDIA_KMH * 60));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(double distanciaKm) {
        if (distanciaKm <= 0 || distanciaKm > 200) {
            throw new RegraNegocioException("Distancia invalida (entre 0,1 e 200 km).");
        }
        this.distanciaKm = distanciaKm;
    }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public BigDecimal getValorSubsidio() { return valorSubsidio; }
    public void setValorSubsidio(BigDecimal valorSubsidio) { this.valorSubsidio = valorSubsidio; }

    public BigDecimal getValorCidadao() { return valorCidadao; }
    public void setValorCidadao(BigDecimal valorCidadao) { this.valorCidadao = valorCidadao; }

    public int getQtdAcompanhantes() { return qtdAcompanhantes; }
    public void setQtdAcompanhantes(int qtdAcompanhantes) { this.qtdAcompanhantes = qtdAcompanhantes; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public StatusViagem getStatus() { return status; }
    /** Usado pelo DAO ao carregar do banco (sem validar o fluxo). */
    public void setStatus(StatusViagem status) { this.status = status; }

    public Cidadao getCidadao() { return cidadao; }
    public void setCidadao(Cidadao cidadao) { this.cidadao = cidadao; }

    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }

    public Pagamento getPagamento() { return pagamento; }
    public void setPagamento(Pagamento pagamento) { this.pagamento = pagamento; }
}
