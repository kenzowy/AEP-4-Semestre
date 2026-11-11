package br.com.transportesocial.model;

import java.math.BigDecimal;
import java.util.List;

/** Servidor da prefeitura que analisa cadastros e acompanha os gastos. */
public class Administrador extends Usuario {

    @Override
    public String getTipo() {
        return "ADMIN";
    }

    @Override
    public List<TipoDocumento> getDocumentosObrigatorios() {
        return List.of();
    }

    /**
     * O administrador so e criado pela propria prefeitura (direto no banco).
     * Por isso o cadastro pelo sistema e sempre recusado.
     */
    @Override
    public boolean validarCadastro() {
        return false;
    }

    /** RF03: aprova um cadastro pendente. */
    public void aprovarCadastro(Usuario u) {
        exigirPendente(u);
        u.setStatus(StatusCadastro.APROVADO);
        u.setMotivoRecusa(null);
    }

    /** RF03: recusa um cadastro pendente registrando o motivo. */
    public void recusarCadastro(Usuario u, String motivo) {
        exigirPendente(u);
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Informe o motivo da recusa.");
        }
        u.setStatus(StatusCadastro.RECUSADO);
        u.setMotivoRecusa(motivo.trim());
    }

    /**
     * RF14: soma o subsidio pago pela prefeitura nas viagens do periodo.
     * Viagem cancelada nao gera subsidio (RN09), entao so contam as concluidas.
     */
    public BigDecimal gerarRelatorioSubsidios(List<Viagem> viagensDoPeriodo) {
        BigDecimal total = BigDecimal.ZERO;
        for (Viagem v : viagensDoPeriodo) {
            if (v.getStatus() == StatusViagem.CONCLUIDA) {
                total = total.add(v.getValorSubsidio());
            }
        }
        return total;
    }

    private void exigirPendente(Usuario u) {
        if (u == null || u instanceof Administrador) {
            throw new RegraNegocioException("Usuario nao encontrado.");
        }
        if (u.getStatus() != StatusCadastro.PENDENTE) {
            throw new RegraNegocioException("Este cadastro nao esta pendente.");
        }
    }
}
