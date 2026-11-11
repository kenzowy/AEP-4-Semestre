package br.com.transportesocial.controle;

import br.com.transportesocial.dao.ViagemDAO;
import br.com.transportesocial.model.Administrador;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.Viagem;
import br.com.transportesocial.util.Json;
import com.sun.net.httpserver.HttpExchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/relatorio?inicio=AAAA-MM-DD&fim=AAAA-MM-DD
 * RF14: viagens realizadas e total de subsidio pago no periodo. [administrador]
 */
public class RelatorioControle extends ControleBase {

    private final ViagemDAO viagemDAO = new ViagemDAO();

    @Override
    protected void tratar(HttpExchange ex, String metodo, String[] partes) throws Exception {
        if (!metodo.equals("GET") || partes.length != 1) {
            naoEncontrado(ex);
            return;
        }
        Administrador admin = exigirPerfil(ex, Administrador.class);
        LocalDate inicio = data(parametro(ex, "inicio"), "data inicial");
        LocalDate fim = data(parametro(ex, "fim"), "data final");
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("A data final deve ser igual ou depois da data inicial.");
        }

        List<Viagem> viagens = viagemDAO.listarConcluidasNoPeriodo(
                inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
        BigDecimal totalSubsidio = admin.gerarRelatorioSubsidios(viagens);
        BigDecimal totalViagens = BigDecimal.ZERO;
        BigDecimal totalCidadaos = BigDecimal.ZERO;
        for (Viagem v : viagens) {
            totalViagens = totalViagens.add(v.getValorTotal());
            totalCidadaos = totalCidadaos.add(v.getValorCidadao());
        }
        ok(ex, Json.obj()
                .add("inicio", inicio.toString())
                .add("fim", fim.toString())
                .add("quantidade", viagens.size())
                .add("totalViagens", totalViagens)
                .add("totalSubsidio", totalSubsidio)
                .add("totalCidadaos", totalCidadaos)
                .bruto("viagens", JsonModelo.viagens(viagens))
                .fim());
    }
}
