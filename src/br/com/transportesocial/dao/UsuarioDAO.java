package br.com.transportesocial.dao;

import br.com.transportesocial.model.Administrador;
import br.com.transportesocial.model.Cidadao;
import br.com.transportesocial.model.Motorista;
import br.com.transportesocial.model.StatusCadastro;
import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.util.Conexao;
import br.com.transportesocial.util.Senha;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD da entidade principal do sistema (usuario e suas especializacoes).
 */
public class UsuarioDAO {

    private static final String SELECT_BASE =
            "SELECT u.*, c.renda_familiar, c.nis, m.cnh, m.disponivel "
          + "FROM usuario u "
          + "LEFT JOIN cidadao c   ON c.usuario_id = u.id "
          + "LEFT JOIN motorista m ON m.usuario_id = u.id ";

    // ------------------------------------------------------------------ CREATE
    public long inserir(Usuario usuario) throws SQLException {
        String sqlUsuario = "INSERT INTO usuario "
                + "(nome, cpf, data_nascimento, email, telefone, senha, tipo, status) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = Conexao.abrir()) {
            con.setAutoCommit(false);
            long id;
            try (PreparedStatement ps = con.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, usuario.getNome());
                ps.setString(2, usuario.getCpf());
                ps.setDate(3, Date.valueOf(usuario.getDataNascimento()));
                ps.setString(4, usuario.getEmail());
                ps.setString(5, usuario.getTelefone());
                ps.setString(6, Senha.gerarHash(usuario.getSenha()));
                ps.setString(7, usuario.getTipo());
                ps.setString(8, usuario.getStatus().name());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    id = rs.getLong(1);
                }
            }
            if (usuario instanceof Cidadao cidadao) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO cidadao (usuario_id, renda_familiar, nis) VALUES (?,?,?)")) {
                    ps.setLong(1, id);
                    ps.setBigDecimal(2, cidadao.getRendaFamiliar());
                    ps.setString(3, cidadao.getNis());
                    ps.executeUpdate();
                }
            } else if (usuario instanceof Motorista motorista) {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO motorista (usuario_id, cnh, disponivel) VALUES (?,?,0)")) {
                    ps.setLong(1, id);
                    ps.setString(2, motorista.getCnh());
                    ps.executeUpdate();
                }
            }
            con.commit();
            usuario.setId(id);
            return id;
        }
    }

    // -------------------------------------------------------------------- READ
    public Usuario buscarPorId(long id) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE u.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public Usuario buscarPorEmail(String email) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE u.email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public List<Usuario> listar(String tipo, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE + "WHERE 1 = 1 ");
        if (tipo != null && !tipo.isBlank()) {
            sql.append("AND u.tipo = ? ");
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND u.status = ? ");
        }
        sql.append("ORDER BY u.nome");
        List<Usuario> lista = new ArrayList<>();
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            if (tipo != null && !tipo.isBlank()) {
                ps.setString(i++, tipo);
            }
            if (status != null && !status.isBlank()) {
                ps.setString(i, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montar(rs));
                }
            }
        }
        return lista;
    }

    public boolean existeCpfOuEmail(String cpf, String email) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT 1 FROM usuario WHERE cpf = ? OR email = ?")) {
            ps.setString(1, cpf);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ------------------------------------------------------------------ UPDATE
    public void atualizar(Usuario usuario) throws SQLException {
        try (Connection con = Conexao.abrir()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE usuario SET nome = ?, telefone = ?, email = ? WHERE id = ?")) {
                ps.setString(1, usuario.getNome());
                ps.setString(2, usuario.getTelefone());
                ps.setString(3, usuario.getEmail());
                ps.setLong(4, usuario.getId());
                ps.executeUpdate();
            }
            if (usuario instanceof Cidadao cidadao) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE cidadao SET renda_familiar = ?, nis = ? WHERE usuario_id = ?")) {
                    ps.setBigDecimal(1, cidadao.getRendaFamiliar());
                    ps.setString(2, cidadao.getNis());
                    ps.setLong(3, usuario.getId());
                    ps.executeUpdate();
                }
            } else if (usuario instanceof Motorista motorista) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE motorista SET cnh = ? WHERE usuario_id = ?")) {
                    ps.setString(1, motorista.getCnh());
                    ps.setLong(2, usuario.getId());
                    ps.executeUpdate();
                }
            }
            con.commit();
        }
    }

    /** Aprovacao ou recusa feita pelo administrador (RF03 / RN04). */
    public void alterarStatus(long id, StatusCadastro status, String motivo) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE usuario SET status = ?, motivo_recusa = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, motivo);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void alterarDisponibilidade(long motoristaId, boolean disponivel) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE motorista SET disponivel = ? WHERE usuario_id = ?")) {
            ps.setBoolean(1, disponivel);
            ps.setLong(2, motoristaId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ DELETE
    public void excluir(long id) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------- LOGIN
    public Usuario autenticar(String email, String senha) throws SQLException {
        Usuario usuario = buscarPorEmail(email);
        if (usuario == null || !Senha.conferir(senha, usuario.getSenha())) {
            return null;
        }
        return usuario;
    }

    /**
     * Transforma a linha do banco no objeto da subclasse correta.
     * E aqui que a heranca do modelo se encontra com a heranca das tabelas.
     */
    private Usuario montar(ResultSet rs) throws SQLException {
        String tipo = rs.getString("tipo");
        Usuario usuario;
        if ("CIDADAO".equals(tipo)) {
            Cidadao cidadao = new Cidadao();
            BigDecimal renda = rs.getBigDecimal("renda_familiar");
            cidadao.setRendaFamiliar(renda == null ? BigDecimal.ZERO : renda);
            cidadao.setNis(rs.getString("nis"));
            usuario = cidadao;
        } else if ("MOTORISTA".equals(tipo)) {
            Motorista motorista = new Motorista();
            motorista.setCnh(rs.getString("cnh"));
            motorista.setDisponivel(rs.getBoolean("disponivel"));
            usuario = motorista;
        } else {
            usuario = new Administrador();
        }
        usuario.setId(rs.getLong("id"));
        usuario.setNome(rs.getString("nome"));
        usuario.setCpf(rs.getString("cpf"));
        Date nascimento = rs.getDate("data_nascimento");
        if (nascimento != null) {
            usuario.setDataNascimento(nascimento.toLocalDate());
        }
        usuario.setEmail(rs.getString("email"));
        usuario.setTelefone(rs.getString("telefone"));
        usuario.setSenha(rs.getString("senha"));
        usuario.setStatus(StatusCadastro.valueOf(rs.getString("status")));
        usuario.setMotivoRecusa(rs.getString("motivo_recusa"));
        return usuario;
    }
}
