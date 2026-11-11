package br.com.transportesocial.dao;

import br.com.transportesocial.model.FormaPagamento;
import br.com.transportesocial.model.Pagamento;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;

/** Persistencia da tabela pagamento (RF11). */
public class PagamentoDAO {

    public void inserir(int viagemId, Pagamento p) throws SQLException {
        String sql = "INSERT INTO pagamento (viagem_id, valor, forma, aprovado, data_pagamento) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, viagemId);
            ps.setBigDecimal(2, p.getValor());
            ps.setString(3, p.getForma().name());
            ps.setBoolean(4, p.isAprovado());
            ps.setTimestamp(5, Timestamp.valueOf(p.getDataHora()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RegraNegocioException("Esta viagem ja foi paga.");
        }
    }

    /** Usado pelo ViagemDAO ao montar uma viagem com o pagamento junto. */
    static Pagamento montar(ResultSet rs) throws SQLException {
        int id = rs.getInt("pg_id");
        if (rs.wasNull()) {
            return null;
        }
        Pagamento p = new Pagamento();
        p.setId(id);
        p.setValor(rs.getBigDecimal("pg_valor"));
        p.setForma(FormaPagamento.valueOf(rs.getString("pg_forma")));
        p.setAprovado(rs.getBoolean("pg_aprovado"));
        p.setDataHora(rs.getTimestamp("pg_data").toLocalDateTime());
        return p;
    }
}
