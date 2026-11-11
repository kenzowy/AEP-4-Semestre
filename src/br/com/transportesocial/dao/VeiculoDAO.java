package br.com.transportesocial.dao;

import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.Veiculo;
import br.com.transportesocial.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Persistencia da tabela veiculo (RF05). */
public class VeiculoDAO {

    public void inserir(Connection con, int motoristaId, Veiculo v) throws SQLException {
        String sql = "INSERT INTO veiculo (motorista_id, placa, marca, modelo, ano, cor) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, motoristaId);
            preencher(ps, v, 2);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    v.setId(rs.getInt(1));
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RegraNegocioException("Ja existe um veiculo cadastrado com esta placa.");
        }
    }

    public void inserir(int motoristaId, Veiculo v) throws SQLException {
        try (Connection con = Conexao.abrir()) {
            inserir(con, motoristaId, v);
        }
    }

    /** Altera os dados do veiculo, apenas se ele for do motorista informado. */
    public boolean atualizar(int motoristaId, Veiculo v) throws SQLException {
        String sql = "UPDATE veiculo SET placa = ?, marca = ?, modelo = ?, ano = ?, cor = ? "
                + "WHERE id = ? AND motorista_id = ?";
        try (Connection con = Conexao.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, v, 1);
            ps.setInt(6, v.getId());
            ps.setInt(7, motoristaId);
            return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RegraNegocioException("Ja existe um veiculo cadastrado com esta placa.");
        }
    }

    public List<Veiculo> listarPorMotorista(Connection con, int motoristaId) throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM veiculo WHERE motorista_id = ? ORDER BY id")) {
            ps.setInt(1, motoristaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Veiculo v = new Veiculo();
                    v.setId(rs.getInt("id"));
                    v.setPlaca(rs.getString("placa"));
                    v.setMarca(rs.getString("marca"));
                    v.setModelo(rs.getString("modelo"));
                    v.setAno(rs.getInt("ano"));
                    v.setCor(rs.getString("cor"));
                    lista.add(v);
                }
            }
        }
        return lista;
    }

    private void preencher(PreparedStatement ps, Veiculo v, int i) throws SQLException {
        ps.setString(i, v.getPlaca());
        ps.setString(i + 1, v.getMarca());
        ps.setString(i + 2, v.getModelo());
        ps.setInt(i + 3, v.getAno());
        ps.setString(i + 4, v.getCor());
    }
}
