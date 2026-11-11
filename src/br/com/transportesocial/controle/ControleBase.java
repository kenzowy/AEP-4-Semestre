package br.com.transportesocial.controle;

import br.com.transportesocial.dao.UsuarioDAO;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.util.Json;
import br.com.transportesocial.util.Sessao;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Classe abstrata da camada de controle. Cuida do que e igual em todas as
 * rotas da API: ler o JSON, responder, tratar erros e conferir o login.
 * Cada controle concreto implementa apenas tratar() (polimorfismo).
 */
public abstract class ControleBase implements HttpHandler {

    protected final UsuarioDAO usuarioDAO = new UsuarioDAO();

    /** Erro de acesso: 401 (nao logado) ou 403 (perfil sem permissao). */
    protected static class AcessoNegadoException extends RuntimeException {
        private final int codigo;

        AcessoNegadoException(int codigo, String mensagem) {
            super(mensagem);
            this.codigo = codigo;
        }
    }

    /**
     * Trata a requisicao.
     *
     * @param partes caminho depois de /api/, ex.: /api/viagens/5/aceitar -> [viagens, 5, aceitar]
     */
    protected abstract void tratar(HttpExchange ex, String metodo, String[] partes) throws Exception;

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        try {
            String caminho = ex.getRequestURI().getPath().replaceFirst("^/api/?", "");
            String[] partes = caminho.isEmpty() ? new String[0] : caminho.split("/");
            tratar(ex, ex.getRequestMethod().toUpperCase(), partes);
        } catch (RegraNegocioException e) {
            responder(ex, 400, Json.erro(e.getMessage()));
        } catch (AcessoNegadoException e) {
            responder(ex, e.codigo, Json.erro(e.getMessage()));
        } catch (Exception e) {
            System.out.println("[ERRO] " + ex.getRequestMethod() + " " + ex.getRequestURI() + " -> " + e);
            e.printStackTrace();
            responder(ex, 500, Json.erro("Erro interno no servidor: " + e.getMessage()));
        } finally {
            ex.close();
        }
    }

    // ------------------------------------------------------------------
    // Entrada e saida
    // ------------------------------------------------------------------

    protected Map<String, String> lerCorpo(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return Json.ler(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    protected void responder(HttpExchange ex, int codigo, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    protected void ok(HttpExchange ex, String json) throws IOException {
        responder(ex, 200, json);
    }

    protected void naoEncontrado(HttpExchange ex) throws IOException {
        responder(ex, 404, Json.erro("Rota nao encontrada."));
    }

    /** Le um parametro da URL (?chave=valor). */
    protected String parametro(HttpExchange ex, String chave) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv[0].equals(chave)) {
                return kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Login e perfis (RNF01)
    // ------------------------------------------------------------------

    protected String token(HttpExchange ex) {
        String cabecalho = ex.getRequestHeaders().getFirst("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            return cabecalho.substring(7).trim();
        }
        return null;
    }

    protected Usuario usuarioLogado(HttpExchange ex) throws Exception {
        Integer id = Sessao.usuarioDoToken(token(ex));
        Usuario u = id == null ? null : usuarioDAO.buscarPorId(id);
        if (u == null) {
            throw new AcessoNegadoException(401, "Faca login para continuar.");
        }
        return u;
    }

    /** Exige que o usuario logado seja do perfil informado. */
    protected <T extends Usuario> T exigirPerfil(HttpExchange ex, Class<T> perfil) throws Exception {
        Usuario u = usuarioLogado(ex);
        if (!perfil.isInstance(u)) {
            throw new AcessoNegadoException(403, "Esta tela nao esta liberada para o seu perfil.");
        }
        return perfil.cast(u);
    }

    // ------------------------------------------------------------------
    // Conversao de campos
    // ------------------------------------------------------------------

    protected static int inteiro(String valor, String campo) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (RuntimeException e) {
            throw new RegraNegocioException("Valor invalido para " + campo + ".");
        }
    }

    protected static int idDaRota(String[] partes, int indice) {
        if (partes.length <= indice) {
            throw new RegraNegocioException("Identificador nao informado.");
        }
        return inteiro(partes[indice], "id");
    }

    protected static double numero(String valor, String campo) {
        try {
            return Double.parseDouble(valor.trim().replace(',', '.'));
        } catch (RuntimeException e) {
            throw new RegraNegocioException("Valor invalido para " + campo + ".");
        }
    }

    protected static BigDecimal dinheiro(String valor, String campo) {
        try {
            String v = valor.trim();
            if (v.contains(",")) {
                v = v.replace(".", "").replace(',', '.');
            }
            return new BigDecimal(v);
        } catch (RuntimeException e) {
            throw new RegraNegocioException("Valor invalido para " + campo + ".");
        }
    }

    protected static LocalDate data(String valor, String campo) {
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RegraNegocioException("Data invalida para " + campo + ".");
        }
    }
}
