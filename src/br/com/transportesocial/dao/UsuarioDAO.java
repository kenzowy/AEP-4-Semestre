package br.com.transportesocial.dao;

import br.com.transportesocial.model.Administrador;
import br.com.transportesocial.model.Cidadao;
import br.com.transportesocial.model.Documento;
import br.com.transportesocial.model.Motorista;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.StatusCadastro;
import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.model.Veiculo;
import br.com.transportesocial.util.Conexao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia dos usuarios. A heranca e gravada em duas tabelas:
 * usuario (dados comuns) + cidadao ou motorista (dados especificos).
 */
public class UsuarioDAO {

    private static final String SELECT_BASE =
            "SELECT u.*, c.renda_familiar, c.nis, m.cnh, m.disponivel FROM usuario u "
            + "LEFT JOIN cidadao c ON c.usuario_id = u.id "
            + "LEFT JOIN motorista m ON m.usuario_id = u.id ";

    private final DocumentoDAO documentoDAO = new DocumentoDAO();
    private final VeiculoDAO veiculoDAO = new VeiculoDAO();

    /** RF01 + RF02: grava o usuario, seus documentos e (motorista) seus veiculos numa transacao. */
    public void inserir(Usuario u) throws SQLException {
        try (Connection con = Conexao.abrir()) {
            con.setAutoCommit(false);
            try {
                String sql = "INSERT INTO usuario (nome, cpf, data_nascimento, email, telefone, senha, tipo, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, u.getNome());
                    ps.setString(2, u.getCpf());
                    ps.setDate(3, Date.valueOf(u.getDataNascimento()));
                    ps.setString(4, u.getEmail());
                    ps.setString(5, u.getTelefone());
                    ps.setString(6, u.getSenha());
                    ps.setString(7, u.getTipo());
                    ps.setString(8, u.getStatus().name());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        u.setId(rs.getInt(1));
                    }
                }
                if (u instanceof Cidadao c) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO cidadao (usuario_id, renda_familiar, nis) VALUES (?, ?, ?)")) {
                        ps.setInt(1, c.getId());
                        ps.setBigDecimal(2, c.getRendaFamiliar());
                        ps.setString(3, c.getNis());
                        ps.executeUpdate();
                    }
                } else if (u instanceof Motorista m) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO motorista (usuario_id, cnh, disponivel) VALUES (?, ?, ?)")) {
                        ps.setInt(1, m.getId());
                        ps.setString(2, m.getCnh());
                        ps.setBoolean(3, m.isDisponivel());
                        ps.executeUpdate();
                    }
                    for (Veiculo v : m.getVeiculos()) {
                        veiculoDAO.inserir(con, m.getId(), v);
                    }
                }
                for (Documento d : u.getDocumentos()) {
                    documentoDAO.inserir(con, u.getId(), d);
                }
                con.commit();
            } catch (SQLIntegrityConstraintViolationException e) {
                con.rollback();
                throw new RegraNegocioException("Ja existe um cadastro com este CPF.");
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public Usuario buscarPorId(int id) throws SQLException {
        return buscarUm(SELECT_BASE + "WHERE u.id = ?", String.valueOf(id));
    }

    public Usuario buscarPorCpf(String cpf) throws SQLException {
        return buscarUm(SELECT_BASE + "WHERE u.cpf = ?", Usuario.formatarCpf(cpf));
    }

    /** RF01 (consultar) e RF03 (pendentes): lista com filtros opcionais. */
    public List<Usuario> listar(String tipo, StatusCadastro status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE + "WHERE u.tipo <> 'ADMIN'");
        if (tipo != null) {
            sql.append(" AND u.tipo = ?");
        }
        if (status != null) {
            sql.append(" AND u.status = ?");
        }
        sql.append(" ORDER BY u.nome");
        List<Usuario> lista = new ArrayList<>();
        try (Connection con = Conexao.abrir(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            if (tipo != null) {
                ps.setString(i++, tipo);
            }
            if (status != null) {
                ps.setString(i, status.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montar(rs));
                }
            }
            for (Usuario u : lista) {
                completar(con, u);
            }
        }
        return lista;
    }

    /** RF01 (alterar): dados da conta e, se trocada, a senha. */
    public void atualizar(Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET nome = ?, email = ?, telefone = ?, senha = ? WHERE id = ?";
        try (Connection con = Conexao.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getTelefone());
            ps.setString(4, u.getSenha());
            ps.setInt(5, u.getId());
            ps.executeUpdate();
        }
    }

    /** RF03 / RN04: grava a aprovacao, a recusa ou a volta para analise. */
    public void atualizarStatus(Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET status = ?, motivo_recusa = ? WHERE id = ?";
        try (Connection con = Conexao.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getStatus().name());
            ps.setString(2, u.getMotivoRecusa());
            ps.setInt(3, u.getId());
            ps.executeUpdate();
        }
    }

    /** RF06: disponibilidade do motorista. */
    public void atualizarDisponibilidade(Motorista m) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement("UPDATE motorista SET disponivel = ? WHERE usuario_id = ?")) {
            ps.setBoolean(1, m.isDisponivel());
            ps.setInt(2, m.getId());
            ps.executeUpdate();
        }
    }

    /**
     * RF01 (excluir). Documentos e veiculos saem junto (ON DELETE CASCADE).
     * Quem ja tem viagens registradas nao pode ser excluido, para nao perder
     * o historico de subsidios pagos pela prefeitura.
     */
    public void excluir(int id) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RegraNegocioException(
                    "Esta conta possui viagens registradas e nao pode ser excluida (historico de subsidios).");
        }
    }

    // ------------------------------------------------------------------

    private Usuario buscarUm(String sql, String parametro) throws SQLException {
        try (Connection con = Conexao.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, parametro);
            Usuario u = null;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    u = montar(rs);
                }
            }
            if (u != null) {
                completar(con, u);
            }
            return u;
        }
    }

    /** Carrega as composicoes: documentos e, no motorista, os veiculos. */
    private void completar(Connection con, Usuario u) throws SQLException {
        u.carregarDocumentos(documentoDAO.listarPorUsuario(con, u.getId()));
        if (u instanceof Motorista m) {
            m.carregarVeiculos(veiculoDAO.listarPorMotorista(con, m.getId()));
        }
    }

    /** Cria o objeto da subclasse certa conforme a coluna tipo. */
    private Usuario montar(ResultSet rs) throws SQLException {
        Usuario u;
        switch (rs.getString("tipo")) {
            case "CIDADAO" -> {
                Cidadao c = new Cidadao();
                c.setRendaFamiliar(rs.getBigDecimal("renda_familiar"));
                c.setNis(rs.getString("nis"));
                u = c;
            }
            case "MOTORISTA" -> {
                Motorista m = new Motorista();
                m.setCnh(rs.getString("cnh"));
                m.setDisponivel(rs.getBoolean("disponivel"));
                u = m;
            }
            default -> u = new Administrador();
        }
        u.setId(rs.getInt("id"));
        u.setNome(rs.getString("nome"));
        u.setCpf(rs.getString("cpf"));
        u.setDataNascimento(rs.getDate("data_nascimento").toLocalDate());
        u.setEmail(rs.getString("email"));
        u.setTelefone(rs.getString("telefone"));
        u.setSenha(rs.getString("senha"));
        u.setStatus(StatusCadastro.valueOf(rs.getString("status")));
        u.setMotivoRecusa(rs.getString("motivo_recusa"));
        return u;
    }
}
