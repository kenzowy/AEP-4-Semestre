package br.com.transportesocial.dao;

import br.com.transportesocial.model.StatusViagem;
import br.com.transportesocial.model.Viagem;
import br.com.transportesocial.util.Conexao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Persistencia das viagens e a contagem do limite semanal (RN05). */
public class ViagemDAO {

    private static final String SELECT_BASE =
            "SELECT v.*, uc.nome AS nome_cidadao, um.nome AS nome_motorista, "
          + "(SELECT COUNT(*) FROM pagamento p WHERE p.viagem_id = v.id) AS qtd_pagamento "
          + "FROM viagem v "
          + "JOIN usuario uc       ON uc.id = v.cidadao_id "
          + "LEFT JOIN usuario um  ON um.id = v.motorista_id ";

    public long inserir(Viagem viagem) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO viagem (cidadao_id, origem, destino, distancia_km, valor_total, "
                   + "valor_subsidio, valor_cidadao, data_hora, status) "
                   + "VALUES (?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, viagem.getCidadaoId());
            ps.setString(2, viagem.getOrigem());
            ps.setString(3, viagem.getDestino());
            ps.setBigDecimal(4, viagem.getDistanciaKm());
            ps.setBigDecimal(5, viagem.getValorTotal());
            ps.setBigDecimal(6, viagem.getValorSubsidio());
            ps.setBigDecimal(7, viagem.getValorCidadao());
            ps.setTimestamp(8, Timestamp.valueOf(viagem.getDataHora()));
            ps.setString(9, viagem.getStatus().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                viagem.setId(rs.getLong(1));
            }
        }
        return viagem.getId();
    }

    public Viagem buscarPorId(long id) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE v.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? montar(rs) : null;
            }
        }
    }

    public List<Viagem> listarPorCidadao(long cidadaoId) throws SQLException {
        return consultar(SELECT_BASE + "WHERE v.cidadao_id = ? ORDER BY v.data_hora DESC", cidadaoId);
    }

    public List<Viagem> listarPorMotorista(long motoristaId) throws SQLException {
        return consultar(SELECT_BASE + "WHERE v.motorista_id = ? ORDER BY v.data_hora DESC", motoristaId);
    }

    /** Solicitacoes ainda sem motorista, mostradas para quem esta disponivel (RF09). */
    public List<Viagem> listarSolicitacoesAbertas() throws SQLException {
        return consultar(SELECT_BASE + "WHERE v.status = 'SOLICITADA' AND v.motorista_id IS NULL "
                + "ORDER BY v.data_hora", null);
    }

    public List<Viagem> listarPorPeriodo(LocalDate inicio, LocalDate fim) throws SQLException {
        List<Viagem> lista = new ArrayList<>();
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE
                     + "WHERE v.data_hora BETWEEN ? AND ? ORDER BY v.data_hora")) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(fim.atTime(LocalTime.MAX)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(montar(rs));
            }
        }
        return lista;
    }

    /**
     * Conta as viagens concluidas do cidadao na semana atual (segunda a domingo).
     */
    public int contarConcluidasNaSemana(long cidadaoId) throws SQLException {
        LocalDate hoje = LocalDate.now();
        LocalDate segunda = hoje.with(DayOfWeek.MONDAY);
        LocalDate domingo = hoje.with(DayOfWeek.SUNDAY);
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM viagem WHERE cidadao_id = ? AND status = 'CONCLUIDA' "
                   + "AND data_hora BETWEEN ? AND ?")) {
            ps.setLong(1, cidadaoId);
            ps.setTimestamp(2, Timestamp.valueOf(segunda.atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(domingo.atTime(LocalTime.MAX)));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void definirMotorista(long viagemId, long motoristaId) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE viagem SET motorista_id = ?, status = 'ACEITA' "
                   + "WHERE id = ? AND status = 'SOLICITADA'")) {
            ps.setLong(1, motoristaId);
            ps.setLong(2, viagemId);
            ps.executeUpdate();
        }
    }

    public void alterarStatus(long viagemId, StatusViagem status) throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE viagem SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, viagemId);
            ps.executeUpdate();
        }
    }

    /** Cancela a solicitacao que ninguem aceitou em 10 minutos (RN09). */
    public int cancelarSolicitacoesExpiradas() throws SQLException {
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE viagem SET status = 'CANCELADA' "
                   + "WHERE status = 'SOLICITADA' AND data_hora < ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusMinutes(10)));
            return ps.executeUpdate();
        }
    }

    private List<Viagem> consultar(String sql, Long parametro) throws SQLException {
        List<Viagem> lista = new ArrayList<>();
        try (Connection con = Conexao.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) ps.setLong(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(montar(rs));
            }
        }
        return lista;
    }

    private Viagem montar(ResultSet rs) throws SQLException {
        Viagem v = new Viagem();
        v.setId(rs.getLong("id"));
        v.setCidadaoId(rs.getLong("cidadao_id"));
        long motorista = rs.getLong("motorista_id");
        v.setMotoristaId(rs.wasNull() ? null : motorista);
        v.setOrigem(rs.getString("origem"));
        v.setDestino(rs.getString("destino"));
        v.setDistanciaKm(rs.getBigDecimal("distancia_km"));
        v.setValorTotal(rs.getBigDecimal("valor_total"));
        v.setValorSubsidio(rs.getBigDecimal("valor_subsidio"));
        v.setValorCidadao(rs.getBigDecimal("valor_cidadao"));
        Timestamp ts = rs.getTimestamp("data_hora");
        if (ts != null) v.setDataHora(ts.toLocalDateTime());
        v.setStatus(StatusViagem.valueOf(rs.getString("status")));
        v.setNomeCidadao(rs.getString("nome_cidadao"));
        v.setNomeMotorista(rs.getString("nome_motorista"));
        v.setPago(rs.getInt("qtd_pagamento") > 0);
        return v;
    }
}
