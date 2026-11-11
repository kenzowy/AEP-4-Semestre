package br.com.transportesocial.dao;

import br.com.transportesocial.model.Cidadao;
import br.com.transportesocial.model.Motorista;
import br.com.transportesocial.model.StatusViagem;
import br.com.transportesocial.model.Viagem;
import br.com.transportesocial.util.Conexao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Persistencia da tabela viagem. */
public class ViagemDAO {

    private static final String SELECT_BASE =
            "SELECT v.*, uc.nome AS cidadao_nome, um.nome AS motorista_nome, "
            + "p.id AS pg_id, p.valor AS pg_valor, p.forma AS pg_forma, "
            + "p.aprovado AS pg_aprovado, p.data_pagamento AS pg_data "
            + "FROM viagem v "
            + "JOIN usuario uc ON uc.id = v.cidadao_id "
            + "LEFT JOIN usuario um ON um.id = v.motorista_id "
            + "LEFT JOIN pagamento p ON p.viagem_id = v.id ";

    /** RF07: grava a solicitacao com os valores calculados (RF08). */
    public void inserir(Viagem v) throws SQLException {
        String sql = "INSERT INTO viagem (cidadao_id, motorista_id, origem, destino, distancia_km, valor_total, "
                + "valor_subsidio, valor_cidadao, qtd_acompanhantes, data_hora, status) "
                + "VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getCidadao().getId());
            ps.setString(2, v.getOrigem());
            ps.setString(3, v.getDestino());
            ps.setBigDecimal(4, BigDecimal.valueOf(v.getDistanciaKm()));
            ps.setBigDecimal(5, v.getValorTotal());
            ps.setBigDecimal(6, v.getValorSubsidio());
            ps.setBigDecimal(7, v.getValorCidadao());
            ps.setInt(8, v.getQtdAcompanhantes());
            ps.setTimestamp(9, Timestamp.valueOf(v.getDataHora()));
            ps.setString(10, v.getStatus().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    v.setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Grava a mudanca de status (e o motorista/valores) somente se a viagem
     * ainda estiver no status esperado. Evita que dois motoristas aceitem a
     * mesma solicitacao ao mesmo tempo.
     */
    public boolean atualizar(Viagem v, StatusViagem statusEsperado) throws SQLException {
        String sql = "UPDATE viagem SET status = ?, motorista_id = ?, valor_subsidio = ?, valor_cidadao = ? "
                + "WHERE id = ? AND status = ?";
        try (Connection con = Conexao.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, v.getStatus().name());
            if (v.getMotorista() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, v.getMotorista().getId());
            }
            ps.setBigDecimal(3, v.getValorSubsidio());
            ps.setBigDecimal(4, v.getValorCidadao());
            ps.setInt(5, v.getId());
            ps.setString(6, statusEsperado.name());
            return ps.executeUpdate() == 1;
        }
    }

    /** RN09: cancela as solicitacoes que ninguem aceitou dentro do prazo. */
    public int cancelarSolicitacoesExpiradas(LocalDateTime limite) throws SQLException {
        String sql = "UPDATE viagem SET status = 'CANCELADA', valor_subsidio = 0, valor_cidadao = 0 "
                + "WHERE status = 'SOLICITADA' AND data_hora < ?";
        try (Connection con = Conexao.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(limite));
            return ps.executeUpdate();
        }
    }

    public Viagem buscarPorId(int id) throws SQLException {
        List<Viagem> lista = listar("WHERE v.id = ?", id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    /** RF13: historico do cidadao. */
    public List<Viagem> listarPorCidadao(int cidadaoId) throws SQLException {
        return listar("WHERE v.cidadao_id = ? ORDER BY v.data_hora DESC, v.id DESC", cidadaoId);
    }

    /** RF13: historico do motorista. */
    public List<Viagem> listarPorMotorista(int motoristaId) throws SQLException {
        return listar("WHERE v.motorista_id = ? ORDER BY v.data_hora DESC, v.id DESC", motoristaId);
    }

    /** RF09: solicitacoes em aberto, as mais antigas primeiro. */
    public List<Viagem> listarAbertas() throws SQLException {
        return listar("WHERE v.status = 'SOLICITADA' ORDER BY v.data_hora, v.id");
    }

    /** RF14: viagens realizadas (concluidas) no periodo [inicio, fim). */
    public List<Viagem> listarConcluidasNoPeriodo(LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        return listar("WHERE v.status = 'CONCLUIDA' AND v.data_hora >= ? AND v.data_hora < ? "
                + "ORDER BY v.data_hora, v.id", Timestamp.valueOf(inicio), Timestamp.valueOf(fim));
    }

    // ------------------------------------------------------------------

    private List<Viagem> listar(String filtro, Object... parametros) throws SQLException {
        List<Viagem> lista = new ArrayList<>();
        try (Connection con = Conexao.abrir(); PreparedStatement ps = con.prepareStatement(SELECT_BASE + filtro)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montar(rs));
                }
            }
        }
        return lista;
    }

    private Viagem montar(ResultSet rs) throws SQLException {
        Viagem v = new Viagem();
        v.setId(rs.getInt("id"));
        v.setOrigem(rs.getString("origem"));
        v.setDestino(rs.getString("destino"));
        v.setDistanciaKm(rs.getBigDecimal("distancia_km").doubleValue());
        v.setValorTotal(rs.getBigDecimal("valor_total"));
        v.setValorSubsidio(rs.getBigDecimal("valor_subsidio"));
        v.setValorCidadao(rs.getBigDecimal("valor_cidadao"));
        v.setQtdAcompanhantes(rs.getInt("qtd_acompanhantes"));
        v.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
        v.setStatus(StatusViagem.valueOf(rs.getString("status")));

        Cidadao c = new Cidadao();
        c.setId(rs.getInt("cidadao_id"));
        c.setNome(rs.getString("cidadao_nome"));
        v.setCidadao(c);

        int motoristaId = rs.getInt("motorista_id");
        if (!rs.wasNull()) {
            Motorista m = new Motorista();
            m.setId(motoristaId);
            m.setNome(rs.getString("motorista_nome"));
            v.setMotorista(m);
        }
        v.setPagamento(PagamentoDAO.montar(rs));
        return v;
    }
}
