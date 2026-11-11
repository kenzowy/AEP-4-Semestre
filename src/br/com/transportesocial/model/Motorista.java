package br.com.transportesocial.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Motorista que atende as viagens. */
public class Motorista extends Usuario {

    private String cnh;
    private boolean disponivel;

    /** Composicao 1:N: o motorista e dono dos seus veiculos. */
    private final List<Veiculo> veiculos = new ArrayList<>();

    @Override
    public String getTipo() {
        return "MOTORISTA";
    }

    @Override
    public List<TipoDocumento> getDocumentosObrigatorios() {
        return List.of(TipoDocumento.CNH, TipoDocumento.ANTECEDENTES_CRIMINAIS);
    }

    /**
     * RN03: confere a CNH e o envio da CNH (com a observacao EAR) e da certidao
     * de antecedentes. A conferencia do conteudo e feita pelo administrador.
     */
    @Override
    public boolean validarCadastro() {
        validarDadosBasicos();
        if (cnh == null || !cnh.matches("\\d{11}")) {
            throw new RegraNegocioException("A CNH deve ter 11 numeros.");
        }
        validarDocumentos();
        return true;
    }

    /** RF05: cadastra um veiculo do motorista. */
    public void registrarVeiculo(Veiculo v) {
        if (v == null) {
            throw new RegraNegocioException("Informe os dados do veiculo.");
        }
        v.validar();
        veiculos.add(v);
    }

    /** RF06: motorista informa que esta disponivel para receber viagens. */
    public void ficarDisponivel() {
        exigirCadastroAprovado();
        if (veiculos.isEmpty()) {
            throw new RegraNegocioException("Cadastre um veiculo antes de ficar disponivel.");
        }
        disponivel = true;
    }

    /** RF06: motorista informa que esta indisponivel. */
    public void ficarIndisponivel() {
        disponivel = false;
    }

    /** RF09: motorista disponivel aceita uma solicitacao em aberto. */
    public void aceitarViagem(Viagem v) {
        exigirCadastroAprovado();
        if (!disponivel) {
            throw new RegraNegocioException("Fique disponivel para aceitar viagens.");
        }
        if (v == null || v.getStatus() != StatusViagem.SOLICITADA) {
            throw new RegraNegocioException("Esta solicitacao nao esta mais em aberto.");
        }
        v.setMotorista(this);
        v.alterarStatus(StatusViagem.ACEITA);
    }

    /** Motorista chegou ao embarque e iniciou a viagem. */
    public void iniciarViagem(Viagem v) {
        exigirViagemPropria(v);
        v.alterarStatus(StatusViagem.EM_ANDAMENTO);
    }

    /** Motorista chegou ao destino e concluiu a viagem. */
    public void finalizarViagem(Viagem v) {
        exigirViagemPropria(v);
        v.alterarStatus(StatusViagem.CONCLUIDA);
    }

    private void exigirViagemPropria(Viagem v) {
        exigirCadastroAprovado();
        if (v == null || v.getMotorista() == null || v.getMotorista().getId() != getId()) {
            throw new RegraNegocioException("Viagem nao encontrada.");
        }
    }

    public String getCnh() { return cnh; }
    public void setCnh(String cnh) { this.cnh = cnh == null ? null : cnh.replaceAll("\\D", ""); }

    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }

    public List<Veiculo> getVeiculos() { return Collections.unmodifiableList(veiculos); }
    public void carregarVeiculos(List<Veiculo> lista) {
        veiculos.clear();
        veiculos.addAll(lista);
    }
}
