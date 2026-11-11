package br.com.transportesocial.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Cidadao de baixa renda que usa o transporte subsidiado. */
public class Cidadao extends Usuario {

    /** RN05: no maximo 5 viagens concluidas por semana. */
    public static final int LIMITE_SEMANAL = 5;

    /** RN01: renda familiar bruta mensal abaixo de R$ 2.000,00. */
    public static final BigDecimal RENDA_MAXIMA = new BigDecimal("2000.00");

    /** RN01: idade minima para ter cadastro proprio. */
    public static final int IDADE_MINIMA = 18;

    private BigDecimal rendaFamiliar;
    private String nis;

    /** Associacao 1:N: viagens solicitadas pelo cidadao. */
    private final List<Viagem> viagens = new ArrayList<>();

    @Override
    public String getTipo() {
        return "CIDADAO";
    }

    @Override
    public List<TipoDocumento> getDocumentosObrigatorios() {
        return List.of(TipoDocumento.CPF, TipoDocumento.COMPROVANTE_RENDA,
                TipoDocumento.ANTECEDENTES_CRIMINAIS);
    }

    /** RN01: idade minima de 18 anos e renda abaixo de R$ 2.000,00. */
    @Override
    public boolean validarCadastro() {
        validarDadosBasicos();
        if (calcularIdade() < IDADE_MINIMA) {
            throw new RegraNegocioException(
                    "E preciso ter 18 anos ou mais. Menores viajam junto com um responsavel cadastrado (RN02).");
        }
        if (rendaFamiliar == null || rendaFamiliar.signum() < 0) {
            throw new RegraNegocioException("Informe a renda familiar.");
        }
        if (rendaFamiliar.compareTo(RENDA_MAXIMA) >= 0) {
            throw new RegraNegocioException("O beneficio e apenas para renda familiar abaixo de R$ 2.000,00.");
        }
        if (nis != null && !nis.isEmpty() && !nis.matches("\\d{11}")) {
            throw new RegraNegocioException("O NIS deve ter 11 numeros.");
        }
        validarDocumentos();
        return true;
    }

    /**
     * RF07: solicita uma viagem informando origem, destino e quantos menores
     * vao acompanhar. Bloqueado se o cadastro nao estiver aprovado (RF04) ou
     * se o limite da semana foi atingido (RF12 / RN05).
     */
    public Viagem solicitarViagem(String origem, String destino, int acompanhantes) {
        exigirCadastroAprovado();
        if (viagensRestantesNaSemana() - viagensEmAberto() <= 0) {
            throw new RegraNegocioException("Limite de " + LIMITE_SEMANAL
                    + " viagens por semana atingido. Novas solicitacoes so na proxima semana.");
        }
        Viagem viagem = new Viagem(origem, destino, acompanhantes);
        viagem.setCidadao(this);
        return viagem;
    }

    /** RN09: cancela sem custo enquanto o motorista nao chegou ao local de embarque. */
    public void cancelarViagem(Viagem v) {
        exigirViagemPropria(v);
        if (v.getStatus() != StatusViagem.SOLICITADA && v.getStatus() != StatusViagem.ACEITA) {
            throw new RegraNegocioException("So e possivel cancelar antes do motorista iniciar a viagem.");
        }
        v.alterarStatus(StatusViagem.CANCELADA);
    }

    /** RF11 / RN08: paga a parte do cidadao por credito, debito ou Pix. */
    public Pagamento pagarViagem(Viagem v, FormaPagamento forma) {
        exigirViagemPropria(v);
        if (v.getStatus() != StatusViagem.CONCLUIDA) {
            throw new RegraNegocioException("O pagamento e feito apos a viagem ser concluida.");
        }
        if (v.getPagamento() != null) {
            throw new RegraNegocioException("Esta viagem ja foi paga.");
        }
        if (forma == null) {
            throw new RegraNegocioException("Escolha credito, debito ou Pix.");
        }
        Pagamento pagamento = new Pagamento(v.getValorCidadao(), forma);
        if (!pagamento.processar()) {
            throw new RegraNegocioException("Pagamento nao autorizado.");
        }
        v.setPagamento(pagamento);
        return pagamento;
    }

    /** RF12 / RN05: so contam as viagens concluidas de segunda a domingo. */
    public int viagensRestantesNaSemana() {
        LocalDateTime inicio = inicioDaSemana();
        LocalDateTime fim = inicio.plusDays(7);
        int concluidas = 0;
        for (Viagem v : viagens) {
            if (v.getStatus() == StatusViagem.CONCLUIDA
                    && !v.getDataHora().isBefore(inicio) && v.getDataHora().isBefore(fim)) {
                concluidas++;
            }
        }
        return Math.max(0, LIMITE_SEMANAL - concluidas);
    }

    /** Viagens ainda em atendimento, que podem virar concluidas nesta semana. */
    private int viagensEmAberto() {
        int abertas = 0;
        for (Viagem v : viagens) {
            StatusViagem s = v.getStatus();
            if (s == StatusViagem.SOLICITADA || s == StatusViagem.ACEITA || s == StatusViagem.EM_ANDAMENTO) {
                abertas++;
            }
        }
        return abertas;
    }

    public static LocalDateTime inicioDaSemana() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
    }

    private void exigirViagemPropria(Viagem v) {
        if (v == null || v.getCidadao() == null || v.getCidadao().getId() != getId()) {
            throw new RegraNegocioException("Viagem nao encontrada.");
        }
    }

    public BigDecimal getRendaFamiliar() { return rendaFamiliar; }
    public void setRendaFamiliar(BigDecimal rendaFamiliar) { this.rendaFamiliar = rendaFamiliar; }

    public String getNis() { return nis; }
    public void setNis(String nis) {
        this.nis = (nis == null || nis.isBlank()) ? null : nis.replaceAll("\\D", "");
    }

    public List<Viagem> getViagens() { return viagens; }
    public void carregarViagens(List<Viagem> lista) {
        viagens.clear();
        viagens.addAll(lista);
    }
}
