package br.com.transportesocial.controle;

import br.com.transportesocial.dao.VeiculoDAO;
import br.com.transportesocial.model.Motorista;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.Veiculo;
import br.com.transportesocial.util.Json;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /api/motorista
 *   PUT  /disponibilidade      disponivel ou indisponivel (RF06)
 *   GET  /veiculos             lista os veiculos do motorista (RF05)
 *   POST /veiculos             cadastra um veiculo (RF05)
 *   PUT  /veiculos/{id}        altera um veiculo (RF05)
 */
public class MotoristaControle extends ControleBase {

    private final VeiculoDAO veiculoDAO = new VeiculoDAO();

    @Override
    protected void tratar(HttpExchange ex, String metodo, String[] partes) throws Exception {
        Motorista m = exigirPerfil(ex, Motorista.class);
        String recurso = partes.length > 1 ? partes[1] : "";

        if (recurso.equals("disponibilidade") && partes.length == 2 && metodo.equals("PUT")) {
            boolean disponivel = "true".equals(lerCorpo(ex).get("disponivel"));
            if (disponivel) {
                m.ficarDisponivel();
            } else {
                m.ficarIndisponivel();
            }
            usuarioDAO.atualizarDisponibilidade(m);
            ok(ex, JsonModelo.usuario(m));
        } else if (recurso.equals("veiculos") && partes.length == 2 && metodo.equals("GET")) {
            List<String> itens = new ArrayList<>();
            for (Veiculo v : m.getVeiculos()) {
                itens.add(JsonModelo.veiculo(v));
            }
            ok(ex, Json.lista(itens));
        } else if (recurso.equals("veiculos") && partes.length == 2 && metodo.equals("POST")) {
            Veiculo v = VeiculoApoio.lerVeiculo(lerCorpo(ex));
            m.registrarVeiculo(v);
            veiculoDAO.inserir(m.getId(), v);
            responder(ex, 201, JsonModelo.veiculo(v));
        } else if (recurso.equals("veiculos") && partes.length == 3 && metodo.equals("PUT")) {
            Map<String, String> d = lerCorpo(ex);
            Veiculo v = VeiculoApoio.lerVeiculo(d);
            v.setId(idDaRota(partes, 2));
            v.validar();
            if (!veiculoDAO.atualizar(m.getId(), v)) {
                throw new RegraNegocioException("Veiculo nao encontrado.");
            }
            ok(ex, JsonModelo.veiculo(v));
        } else {
            naoEncontrado(ex);
        }
    }
}
