package br.com.transportesocial.model;

import java.time.Year;

/** Veiculo do motorista (RF05). */
public class Veiculo {

    private int id;
    private String placa;
    private String marca;
    private String modelo;
    private int ano;
    private String cor;

    /** Placa no padrao antigo (ABC1234) ou Mercosul (ABC1D23). */
    public void validar() {
        if (placa == null || !placa.matches("[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}")) {
            throw new RegraNegocioException("Placa invalida. Exemplos: ABC1234 ou ABC1D23.");
        }
        if (marca == null || marca.isBlank()) {
            throw new RegraNegocioException("Informe a marca do veiculo.");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new RegraNegocioException("Informe o modelo do veiculo.");
        }
        int anoAtual = Year.now().getValue();
        if (ano < 1980 || ano > anoAtual + 1) {
            throw new RegraNegocioException("Ano do veiculo invalido.");
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) {
        this.placa = placa == null ? null : placa.trim().toUpperCase().replace(" ", "");
    }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca == null ? null : marca.trim(); }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo == null ? null : modelo.trim(); }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = (cor == null || cor.isBlank()) ? null : cor.trim(); }
}
