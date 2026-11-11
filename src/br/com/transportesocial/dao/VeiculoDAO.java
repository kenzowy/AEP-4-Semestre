package br.com.transportesocial.dao;

import br.com.transportesocial.model.Veiculo;
import br.com.transportesocial.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** CRUD dos veiculos de cada motorista (RF05). */
public class VeiculoDAO {

    public long inserir(Veiculo veiculo) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO veiculo (motorista_id, placa, marca, modelo, ano, cor) "
                   + "VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, veiculo.getMotoristaId());
            ps.setString(2, veiculo.getPlaca().toUpperCase());
            ps.setString(3, veiculo.getMarca() != null ? veiculo.getMarca() : "");
            ps.setString(4, veiculo.getModelo());
            ps.setInt(5, veiculo.getAno());
            ps.setString(6, veiculo.getCor());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                veiculo.setId(rs.getLong(1));
            }
        }
        return veiculo.getId();
    }

    public List<Veiculo> listar() throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM veiculo ORDER BY id")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(montar(rs));
            }
        }
        return lista;
    }

    public List<Veiculo> listarPorMotorista(long motoristaId) throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM veiculo WHERE motorista_id = ? ORDER BY id")) {
            ps.setLong(1, motoristaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(montar(rs));
            }
        }
        return lista;
    }

    public Veiculo buscarPorId(long id) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM veiculo WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public void atualizar(Veiculo veiculo) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE veiculo SET placa = ?, marca = ?, modelo = ?, ano = ?, cor = ? "
                   + "WHERE id = ?")) {
            ps.setString(1, veiculo.getPlaca().toUpperCase());
            ps.setString(2, veiculo.getMarca() != null ? veiculo.getMarca() : "");
            ps.setString(3, veiculo.getModelo());
            ps.setInt(4, veiculo.getAno());
            ps.setString(5, veiculo.getCor());
            ps.setLong(6, veiculo.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(long id) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement("DELETE FROM veiculo WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Veiculo montar(ResultSet rs) throws SQLException {
        Veiculo v = new Veiculo();
        v.setId(rs.getLong("id"));
        v.setMotoristaId(rs.getLong("motorista_id"));
        v.setPlaca(rs.getString("placa"));
        v.setMarca(rs.getString("marca"));
        v.setModelo(rs.getString("modelo"));
        v.setAno(rs.getInt("ano"));
        v.setCor(rs.getString("cor"));
        return v;
    }
}
