package br.com.transportesocial.controle;

import br.com.transportesocial.dao.PagamentoDAO;
import br.com.transportesocial.dao.ViagemDAO;
import br.com.transportesocial.model.Cidadao;
import br.com.transportesocial.model.FormaPagamento;
import br.com.transportesocial.model.Motorista;
import br.com.transportesocial.model.Pagamento;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.StatusViagem;
import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.model.Viagem;
import br.com.transportesocial.util.Json;
import com.sun.net.httpserver.HttpExchange;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * /api/viagens
 *   POST /simular            valores antes de confirmar (RF08)                 [cidadao]
 *   POST                     solicita a viagem (RF07, RF12)                    [cidadao]
 *   GET  /minhas             status, tempo estimado e historico (RF10, RF12, RF13)
 *   GET  /abertas            solicitacoes em aberto (RF09)                     [motorista]
 *   POST /{id}/aceitar       aceita a solicitacao (RF09)                       [motorista]
 *   POST /{id}/iniciar       inicia a viagem                                   [motorista]
 *   POST /{id}/finalizar     conclui a viagem                                  [motorista]
 *   POST /{id}/cancelar      cancela sem custo (RN09)                          [cidadao]
 *   POST /{id}/pagar         paga a parte do cidadao (RF11)                    [cidadao]
 */
public class ViagemControle extends ControleBase {

    private static final ViagemDAO VIAGEM_DAO = new ViagemDAO();
    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();

    /** RN09: solicitacao que ninguem aceitar em 10 minutos e cancelada pelo sistema. */
    public static int cancelarSolicitacoesExpiradas() throws SQLException {
        return VIAGEM_DAO.cancelarSolicitacoesExpiradas(
                LocalDateTime.now().minusMinutes(Viagem.MINUTOS_PARA_EXPIRAR));
    }

    @Override
    protected void tratar(HttpExchange ex, String metodo, String[] partes) throws Exception {
        cancelarSolicitacoesExpiradas();

        if (partes.length == 1 && metodo.equals("POST")) {
            solicitar(ex);
        } else if (partes.length == 2 && metodo.equals("POST") && partes[1].equals("simular")) {
            simular(ex);
        } else if (partes.length == 2 && metodo.equals("GET") && partes[1].equals("minhas")) {
            minhas(ex);
        } else if (partes.length == 2 && metodo.equals("GET") && partes[1].equals("abertas")) {
            abertas(ex);
        } else if (partes.length == 3 && metodo.equals("POST")) {
            acao(ex, idDaRota(partes, 1), partes[2]);
        } else {
            naoEncontrado(ex);
        }
    }

    /** RF08: calcula total, subsidio e parte do cidadao antes da confirmacao. */
    private void simular(HttpExchange ex) throws Exception {
        Cidadao c = exigirPerfil(ex, Cidadao.class);
        if (!c.estaAprovado()) {
            throw new RegraNegocioException("Seu cadastro ainda nao foi aprovado pela prefeitura.");
        }
        Map<String, String> d = lerCorpo(ex);
        Viagem v = new Viagem();
        v.setDistanciaKm(numero(valor(d, "distanciaKm"), "distancia"));
        v.calcularValor();
        v.calcularSubsidio();
        ok(ex, Json.obj()
                .add("distanciaKm", v.getDistanciaKm())
                .add("valorTotal", v.getValorTotal())
                .add("valorSubsidio", v.getValorSubsidio())
                .add("valorCidadao", v.getValorCidadao())
                .add("tempoEstimadoMin", v.getTempoEstimadoMinutos())
                .fim());
    }

    /** RF07: cidadao solicita informando origem, destino e menores acompanhantes. */
    private void solicitar(HttpExchange ex) throws Exception {
        Cidadao c = exigirPerfil(ex, Cidadao.class);
        Map<String, String> d = lerCorpo(ex);
        c.carregarViagens(VIAGEM_DAO.listarPorCidadao(c.getId()));
        String acompanhantes = d.get("qtdAcompanhantes");
        int qtd = acompanhantes == null || acompanhantes.isBlank() ? 0 : inteiro(acompanhantes, "acompanhantes");

        Viagem v = c.solicitarViagem(d.get("origem"), d.get("destino"), qtd);
        v.setDistanciaKm(numero(valor(d, "distanciaKm"), "distancia"));
        v.calcularValor();
        v.calcularSubsidio();
        VIAGEM_DAO.inserir(v);
        responder(ex, 201, JsonModelo.viagem(v));
    }

    /** RF10 / RF12 / RF13: viagens do cidadao ou do motorista logado. */
    private void minhas(HttpExchange ex) throws Exception {
        Usuario u = usuarioLogado(ex);
        if (u instanceof Cidadao c) {
            c.carregarViagens(VIAGEM_DAO.listarPorCidadao(c.getId()));
            ok(ex, Json.obj()
                    .add("limiteSemanal", Cidadao.LIMITE_SEMANAL)
                    .add("restantesNaSemana", c.viagensRestantesNaSemana())
                    .bruto("viagens", JsonModelo.viagens(c.getViagens()))
                    .fim());
        } else if (u instanceof Motorista m) {
            ok(ex, Json.obj()
                    .add("disponivel", m.isDisponivel())
                    .bruto("viagens", JsonModelo.viagens(VIAGEM_DAO.listarPorMotorista(m.getId())))
                    .fim());
        } else {
            throw new AcessoNegadoException(403, "Esta tela nao esta liberada para o seu perfil.");
        }
    }

    /** RF09: o motorista disponivel ve as solicitacoes em aberto. */
    private void abertas(HttpExchange ex) throws Exception {
        Motorista m = exigirPerfil(ex, Motorista.class);
        if (!m.estaAprovado()) {
            throw new RegraNegocioException("Seu cadastro ainda nao foi aprovado pela prefeitura.");
        }
        String lista = m.isDisponivel() ? JsonModelo.viagens(VIAGEM_DAO.listarAbertas()) : "[]";
        ok(ex, Json.obj().add("disponivel", m.isDisponivel()).bruto("viagens", lista).fim());
    }

    private void acao(HttpExchange ex, int id, String acao) throws Exception {
        Viagem v = VIAGEM_DAO.buscarPorId(id);
        if (v == null) {
            throw new RegraNegocioException("Viagem nao encontrada.");
        }
        StatusViagem anterior = v.getStatus();
        switch (acao) {
            case "aceitar" -> exigirPerfil(ex, Motorista.class).aceitarViagem(v);
            case "iniciar" -> exigirPerfil(ex, Motorista.class).iniciarViagem(v);
            case "finalizar" -> exigirPerfil(ex, Motorista.class).finalizarViagem(v);
            case "cancelar" -> exigirPerfil(ex, Cidadao.class).cancelarViagem(v);
            case "pagar" -> {
                Cidadao c = exigirPerfil(ex, Cidadao.class);
                FormaPagamento forma;
                try {
                    forma = FormaPagamento.valueOf(valor(lerCorpo(ex), "forma"));
                } catch (IllegalArgumentException e) {
                    throw new RegraNegocioException("Escolha credito, debito ou Pix.");
                }
                Pagamento p = c.pagarViagem(v, forma);
                pagamentoDAO.inserir(v.getId(), p);
                ok(ex, JsonModelo.viagem(v));
                return;
            }
            default -> {
                naoEncontrado(ex);
                return;
            }
        }
        if (!VIAGEM_DAO.atualizar(v, anterior)) {
            throw new RegraNegocioException("A viagem mudou de situacao. Atualize a tela e tente de novo.");
        }
        ok(ex, JsonModelo.viagem(v));
    }

    private static String valor(Map<String, String> d, String chave) {
        String v = d.get(chave);
        if (v == null || v.isBlank()) {
            throw new RegraNegocioException("Campo obrigatorio: " + chave + ".");
        }
        return v;
    }
}
