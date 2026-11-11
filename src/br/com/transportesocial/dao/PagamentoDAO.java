package br.com.transportesocial.dao;

import br.com.transportesocial.model.FormaPagamento;
import br.com.transportesocial.model.Pagamento;
import br.com.transportesocial.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/** Persistencia dos pagamentos (RF11). */
public class PagamentoDAO {

    public long inserir(Pagamento pagamento) throws SQLException {
        String sql = "INSERT INTO pagamento "
                + "(viagem_id, valor_total, valor_subsidio, valor_cidadao, forma_pagamento, autorizado, data_pagamento) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, pagamento.getViagemId());
            ps.setBigDecimal(2, pagamento.getValorTotal());
            ps.setBigDecimal(3, pagamento.getValorSubsidio());
            ps.setBigDecimal(4, pagamento.getValorCidadao());
            ps.setString(5, pagamento.getFormaPagamento().name());
            ps.setBoolean(6, pagamento.isAutorizado());
            ps.setTimestamp(7, Timestamp.valueOf(pagamento.getDataPagamento()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                pagamento.setId(rs.getLong(1));
            }
        }
        return pagamento.getId();
    }

    public Pagamento buscarPorViagem(long viagemId) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM pagamento WHERE viagem_id = ?")) {
            ps.setLong(1, viagemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montar(rs);
            }
        }
    }

    private Pagamento montar(ResultSet rs) throws SQLException {
        Pagamento p = new Pagamento();
        p.setId(rs.getLong("id"));
        p.setViagemId(rs.getLong("viagem_id"));
        p.setValorTotal(rs.getBigDecimal("valor_total"));
        p.setValorSubsidio(rs.getBigDecimal("valor_subsidio"));
        p.setValorCidadao(rs.getBigDecimal("valor_cidadao"));
        p.setFormaPagamento(FormaPagamento.valueOf(rs.getString("forma_pagamento")));
        p.setAutorizado(rs.getBoolean("autorizado"));
        Timestamp ts = rs.getTimestamp("data_pagamento");
        if (ts != null) p.setDataPagamento(ts.toLocalDateTime());
        return p;
    }
}
