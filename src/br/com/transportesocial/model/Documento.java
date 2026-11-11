package br.com.transportesocial.model;

import java.time.LocalDateTime;

/** Documento enviado pelo usuario na hora do cadastro (RF02). */
public class Documento {

    private int id;
    private int usuarioId;
    private TipoDocumento tipo;
    private String arquivo;
    private LocalDateTime dataEnvio = LocalDateTime.now();

    public Documento() {
    }

    public Documento(int usuarioId, TipoDocumento tipo, String arquivo) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.arquivo = arquivo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }

    public String getArquivo() { return arquivo; }
    public void setArquivo(String arquivo) { this.arquivo = arquivo; }

    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }
}
