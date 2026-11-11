package br.com.transportesocial.model;

/** Veiculo pertencente a um motorista (composicao 1:N). */
public class Veiculo {

    private long id;
    private long motoristaId;
    private String placa;
    private String marca;
    private String modelo;
    private int ano;
    private String cor;

    public Veiculo() {
    }

    public Veiculo(long motoristaId, String placa, String marca, String modelo, int ano, String cor) {
        this.motoristaId = motoristaId;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.cor = cor;
    }

    /** @return mensagem de erro ou null quando o veiculo esta valido */
    public String validar() {
        if (placa == null || placa.trim().length() < 7) {
            return "Informe a placa do veiculo (minimo 7 caracteres).";
        }
        if (marca == null || marca.isBlank() || modelo == null || modelo.isBlank()) {
            return "Informe marca e modelo.";
        }
        if (ano < 2005) {
            return "O veiculo precisa ser do ano 2005 ou mais novo.";
        }
        return null;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(long motoristaId) { this.motoristaId = motoristaId; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
}
