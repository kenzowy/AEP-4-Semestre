package br.com.transportesocial.model;

import java.time.LocalDateTime;

/** Documento enviado no cadastro (RF02). Pertence a um Usuario (composicao). */
public class Documento {

    private int id;
    private TipoDocumento tipo;
    private String arquivo;
    private LocalDateTime dataEnvio;

    public Documento() {
    }

    public Documento(TipoDocumento tipo, String arquivo) {
        this.tipo = tipo;
        this.arquivo = arquivo;
        this.dataEnvio = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }

    public String getArquivo() { return arquivo; }
    public void setArquivo(String arquivo) { this.arquivo = arquivo; }

    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }
}
