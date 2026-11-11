package br.com.transportesocial.dao;

import br.com.transportesocial.model.Documento;
import br.com.transportesocial.model.TipoDocumento;
import br.com.transportesocial.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Persistencia da tabela documento. */
public class DocumentoDAO {

    /** Insere usando a conexao (transacao) de quem chamou. */
    public void inserir(Connection con, int usuarioId, Documento doc) throws SQLException {
        String sql = "INSERT INTO documento (usuario_id, tipo, arquivo, data_envio) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, doc.getTipo().name());
            ps.setString(3, doc.getArquivo());
            ps.setTimestamp(4, Timestamp.valueOf(doc.getDataEnvio()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    doc.setId(rs.getInt(1));
                }
            }
        }
    }

    public void inserir(int usuarioId, Documento doc) throws SQLException {
        try (Connection con = Conexao.abrir()) {
            inserir(con, usuarioId, doc);
        }
    }

    public List<Documento> listarPorUsuario(Connection con, int usuarioId) throws SQLException {
        List<Documento> lista = new ArrayList<>();
        String sql = "SELECT * FROM documento WHERE usuario_id = ? ORDER BY data_envio, id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montar(rs));
                }
            }
        }
        return lista;
    }

    public Documento buscarPorId(int id) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM documento WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    private Documento montar(ResultSet rs) throws SQLException {
        Documento d = new Documento();
        d.setId(rs.getInt("id"));
        d.setTipo(TipoDocumento.valueOf(rs.getString("tipo")));
        d.setArquivo(rs.getString("arquivo"));
        d.setDataEnvio(rs.getTimestamp("data_envio").toLocalDateTime());
        return d;
    }
}
