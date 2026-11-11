package br.com.transportesocial.dao;

import br.com.transportesocial.model.Documento;
import br.com.transportesocial.model.TipoDocumento;
import br.com.transportesocial.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Documentos enviados no cadastro. */
public class DocumentoDAO {

    public void inserir(Documento documento) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO documento (usuario_id, tipo, arquivo, data_envio) VALUES (?,?,?,?)")) {
            ps.setInt(1, documento.getUsuarioId());
            ps.setString(2, documento.getTipo().name());
            ps.setString(3, documento.getArquivo());
            ps.setTimestamp(4, Timestamp.valueOf(documento.getDataEnvio()));
            ps.executeUpdate();
        }
    }

    public List<Documento> listarPorUsuario(int usuarioId) throws SQLException {
        List<Documento> lista = new ArrayList<>();
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM documento WHERE usuario_id = ? ORDER BY id")) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Documento d = new Documento();
                    d.setId(rs.getInt("id"));
                    d.setUsuarioId(rs.getInt("usuario_id"));
                    d.setTipo(TipoDocumento.valueOf(rs.getString("tipo")));
                    d.setArquivo(rs.getString("arquivo"));
                    d.setDataEnvio(rs.getTimestamp("data_envio").toLocalDateTime());
                    lista.add(d);
                }
            }
        }
        return lista;
    }
}
