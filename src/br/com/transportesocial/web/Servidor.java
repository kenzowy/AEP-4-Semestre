package br.com.transportesocial.web;

import br.com.transportesocial.dao.*;
import br.com.transportesocial.model.*;
import br.com.transportesocial.util.Conexao;
import br.com.transportesocial.util.Json;
import br.com.transportesocial.util.Senha;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP embutido (sem framework externo).
 * Porta padrao: 8080. Acesse http://localhost:8080 no navegador.
 */
public class Servidor {

    private static final int PORTA = 8080;
    private static final String WEB_DIR = "web";

    private final UsuarioDAO  usuarioDAO  = new UsuarioDAO();
    private final ViagemDAO   viagemDAO   = new ViagemDAO();
    private final VeiculoDAO  veiculoDAO  = new VeiculoDAO();
    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();

    public void iniciar() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORTA), 0);

        // Arquivos estaticos (HTML / CSS / JS)
        server.createContext("/", this::handleEstatico);

        // API
        server.createContext("/api/login",          this::handleLogin);
        server.createContext("/api/usuarios",        this::handleUsuarios);
        server.createContext("/api/viagens",         this::handleViagens);
        server.createContext("/api/veiculos",        this::handleVeiculos);
        server.createContext("/api/pagamentos",      this::handlePagamentos);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("[SERVIDOR] Rodando em http://localhost:" + PORTA);
    }

    // -------------------------------------------------------------------------
    // Arquivos estaticos
    // -------------------------------------------------------------------------

    private void handleEstatico(HttpExchange ex) throws IOException {
        String uri = ex.getRequestURI().getPath();
        if ("/".equals(uri) || uri.isEmpty()) uri = "/index.html";

        Path arquivo = Paths.get(WEB_DIR + uri);
        if (!Files.exists(arquivo) || Files.isDirectory(arquivo)) {
            responder(ex, 404, "text/plain", "Nao encontrado: " + uri);
            return;
        }

        String tipo = detectarMime(uri);
        byte[] bytes = Files.readAllBytes(arquivo);
        ex.getResponseHeaders().set("Content-Type", tipo + "; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String detectarMime(String uri) {
        if (uri.endsWith(".html")) return "text/html";
        if (uri.endsWith(".css"))  return "text/css";
        if (uri.endsWith(".js"))   return "application/javascript";
        if (uri.endsWith(".png"))  return "image/png";
        if (uri.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    // -------------------------------------------------------------------------
    // POST /api/login
    // -------------------------------------------------------------------------

    private void handleLogin(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { metodoNaoPermitido(ex); return; }
        Map<String, String> body = lerJson(ex);
        String email = body.get("email");
        String senhaRaw = body.get("senha");

        if (email == null || senhaRaw == null) {
            responderJson(ex, 400, Json.erro("Email e senha sao obrigatorios"));
            return;
        }

        try {
            Usuario u = usuarioDAO.autenticar(email, senhaRaw);
            if (u == null) {
                responderJson(ex, 401, Json.erro("Email ou senha incorretos"));
                return;
            }
            String json = Json.obj()
                    .add("id",     u.getId())
                    .add("nome",   u.getNome())
                    .add("email",  u.getEmail())
                    .add("tipo",   u.getTipo())
                    .add("status", u.getStatus().name())
                    .fim();
            responderJson(ex, 200, json);
        } catch (Exception e) {
            responderJson(ex, 500, Json.erro("Erro interno: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // /api/usuarios  (GET lista, POST cadastro, PUT atualizar, DELETE excluir)
    //   /api/usuarios/aprovar   POST { id, aprovado, motivo }
    //   /api/usuarios/senha     PUT  { id, senhaAtual, novaSenha }
    // -------------------------------------------------------------------------

    private void handleUsuarios(HttpExchange ex) throws IOException {
        addCors(ex);
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if ("OPTIONS".equals(method)) { responder(ex, 204, "text/plain", ""); return; }

            // POST /api/usuarios/aprovar
            if (path.endsWith("/aprovar") && "POST".equals(method)) {
                handleAprovarUsuario(ex); return;
            }

            // PUT /api/usuarios/disponibilidade
            if (path.endsWith("/disponibilidade") && "PUT".equals(method)) {
                handleDisponibilidade(ex); return;
            }

            String idStr = extrairId(path, "/api/usuarios");

            if ("GET".equals(method)) {
                if (idStr != null) {
                    // GET /api/usuarios/{id}
                    Usuario u = usuarioDAO.buscarPorId(Long.parseLong(idStr));
                    if (u == null) { responderJson(ex, 404, Json.erro("Usuario nao encontrado")); return; }
                    responderJson(ex, 200, serializarUsuario(u));
                } else {
                    // GET /api/usuarios?tipo=CIDADAO&status=APROVADO
                    Map<String, String> params = lerParams(ex);
                    String tipo   = params.get("tipo");
                    String status = params.get("status");
                    List<Usuario> lista = usuarioDAO.listar(tipo, status);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < lista.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(serializarUsuario(lista.get(i)));
                    }
                    sb.append("]");
                    responderJson(ex, 200, sb.toString());
                }
            } else if ("POST".equals(method)) {
                handleCadastrarUsuario(ex);
            } else if ("PUT".equals(method) && idStr != null) {
                handleAtualizarUsuario(ex, Long.parseLong(idStr));
            } else if ("DELETE".equals(method) && idStr != null) {
                usuarioDAO.excluir(Long.parseLong(idStr));
                responderJson(ex, 200, "{\"ok\":true}");
            } else {
                metodoNaoPermitido(ex);
            }
        } catch (Exception e) {
            responderJson(ex, 500, Json.erro("Erro: " + e.getMessage()));
        }
    }

    private void handleCadastrarUsuario(HttpExchange ex) throws Exception {
        Map<String, String> body = lerJson(ex);
        String tipo = body.getOrDefault("tipo", "CIDADAO").toUpperCase();

        Usuario u;
        if ("MOTORISTA".equals(tipo)) {
            Motorista m = new Motorista();
            m.setCnh(body.get("cnh"));
            u = m;
        } else {
            Cidadao c = new Cidadao();
            String renda = body.get("rendaFamiliar");
            if (renda != null) c.setRendaFamiliar(new BigDecimal(renda));
            c.setNis(body.get("nis"));
            u = c;
        }

        u.setNome(body.get("nome"));
        u.setCpf(body.get("cpf"));
        u.setEmail(body.get("email"));
        u.setTelefone(body.get("telefone"));
        u.setSenha(Senha.gerarHash(body.get("senha")));
        String dn = body.get("dataNascimento");
        if (dn != null) u.setDataNascimento(LocalDate.parse(dn));

        String erro = u.validarCadastro();
        if (erro != null) { responderJson(ex, 400, Json.erro(erro)); return; }

        if (usuarioDAO.existeCpfOuEmail(u.getCpf(), u.getEmail())) {
            responderJson(ex, 409, Json.erro("CPF ou email ja cadastrado"));
            return;
        }

        usuarioDAO.inserir(u);
        responderJson(ex, 201, serializarUsuario(u));
    }

    private void handleAtualizarUsuario(HttpExchange ex, long id) throws Exception {
        Map<String, String> body = lerJson(ex);
        Usuario u = usuarioDAO.buscarPorId(id);
        if (u == null) { responderJson(ex, 404, Json.erro("Usuario nao encontrado")); return; }
        if (body.containsKey("nome"))      u.setNome(body.get("nome"));
        if (body.containsKey("telefone"))  u.setTelefone(body.get("telefone"));
        if (body.containsKey("email"))     u.setEmail(body.get("email"));
        if (u instanceof Cidadao && body.containsKey("rendaFamiliar")) {
            ((Cidadao) u).setRendaFamiliar(new BigDecimal(body.get("rendaFamiliar")));
        }
        if (u instanceof Motorista && body.containsKey("cnh")) {
            ((Motorista) u).setCnh(body.get("cnh"));
        }
        usuarioDAO.atualizar(u);
        responderJson(ex, 200, serializarUsuario(u));
    }

    private void handleAprovarUsuario(HttpExchange ex) throws Exception {
        Map<String, String> body = lerJson(ex);
        long id = Long.parseLong(body.get("id"));
        boolean aprovado = Boolean.parseBoolean(body.get("aprovado"));
        String motivo = body.getOrDefault("motivo", "");
        StatusCadastro s = aprovado ? StatusCadastro.APROVADO : StatusCadastro.RECUSADO;
        usuarioDAO.alterarStatus(id, s, aprovado ? null : motivo);
        responderJson(ex, 200, "{\"ok\":true}");
    }

    private void handleDisponibilidade(HttpExchange ex) throws Exception {
        Map<String, String> body = lerJson(ex);
        long id = Long.parseLong(body.get("id"));
        boolean disp = Boolean.parseBoolean(body.get("disponivel"));
        usuarioDAO.alterarDisponibilidade(id, disp);
        responderJson(ex, 200, "{\"ok\":true}");
    }

    // -------------------------------------------------------------------------
    // /api/viagens
    // -------------------------------------------------------------------------

    private void handleViagens(HttpExchange ex) throws IOException {
        addCors(ex);
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if ("OPTIONS".equals(method)) { responder(ex, 204, "text/plain", ""); return; }

            if (path.endsWith("/aceitar") && "POST".equals(method)) {
                handleAceitarViagem(ex); return;
            }
            if (path.endsWith("/cancelar") && "POST".equals(method)) {
                handleCancelarViagem(ex); return;
            }
            if (path.endsWith("/status") && "PUT".equals(method)) {
                handleAlterarStatusViagem(ex); return;
            }
            if (path.endsWith("/limite") && "GET".equals(method)) {
                handleLimiteSemanal(ex); return;
            }
            if (path.endsWith("/solicitar") && "POST".equals(method)) {
                handleSolicitarViagem(ex); return;
            }

            String idStr = extrairId(path, "/api/viagens");

            if ("GET".equals(method)) {
                if (idStr != null) {
                    Viagem v = viagemDAO.buscarPorId(Long.parseLong(idStr));
                    if (v == null) { responderJson(ex, 404, Json.erro("Viagem nao encontrada")); return; }
                    responderJson(ex, 200, serializarViagem(v));
                } else {
                    Map<String, String> params = lerParams(ex);
                    String tipo = params.get("tipo");
                    String uid  = params.get("usuarioId");
                    List<Viagem> lista;
                    if ("solicitacoes".equals(tipo)) {
                        lista = viagemDAO.listarSolicitacoesAbertas();
                    } else if ("motorista".equals(tipo) && uid != null) {
                        lista = viagemDAO.listarPorMotorista(Long.parseLong(uid));
                    } else if (uid != null) {
                        lista = viagemDAO.listarPorCidadao(Long.parseLong(uid));
                    } else {
                        lista = viagemDAO.listarSolicitacoesAbertas();
                    }
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < lista.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(serializarViagem(lista.get(i)));
                    }
                    sb.append("]");
                    responderJson(ex, 200, sb.toString());
                }
            } else {
                metodoNaoPermitido(ex);
            }
        } catch (Exception e) {
            responderJson(ex, 500, Json.erro("Erro: " + e.getMessage()));
        }
    }

    private void handleSolicitarViagem(HttpExchange ex) throws Exception {
        Map<String, String> body = lerJson(ex);
        long cidadaoId = Long.parseLong(body.get("cidadaoId"));

        // Verifica limite semanal
        int usadas = viagemDAO.contarConcluidasNaSemana(cidadaoId);
        if (usadas >= Cidadao.LIMITE_SEMANAL) {
            responderJson(ex, 400, Json.erro("Limite semanal de " + Cidadao.LIMITE_SEMANAL + " viagens atingido"));
            return;
        }

        Viagem v = new Viagem();
        v.setCidadaoId(cidadaoId);
        v.setOrigem(body.get("origem"));
        v.setDestino(body.get("destino"));
        String km = body.get("distanciaKm");
        if (km != null) v.setDistanciaKm(new BigDecimal(km));
        v.calcularValores();
        v.setStatus(StatusViagem.SOLICITADA);

        viagemDAO.inserir(v);
        responderJson(ex, 201, serializarViagem(v));
    }

    private void handleAceitarViagem(HttpExchange ex) throws Exception {
        Map<String, String> body = lerJson(ex);
        long viagemId   = Long.parseLong(body.get("viagemId"));
        long motoristaId = Long.parseLong(body.get("motoristaId"));
        viagemDAO.definirMotorista(viagemId, motoristaId);
        viagemDAO.alterarStatus(viagemId, StatusViagem.ACEITA);
        usuarioDAO.alterarDisponibilidade(motoristaId, false);
        responderJson(ex, 200, "{\"ok\":true}");
    }

    private void handleCancelarViagem(HttpExchange ex) throws Exception {
        Map<String, String> body = lerJson(ex);
        long id = Long.parseLong(body.get("viagemId"));
        Viagem v = viagemDAO.buscarPorId(id);
        if (v == null) { responderJson(ex, 404, Json.erro("Viagem nao encontrada")); return; }
        if (!v.podeSerCancelada()) { responderJson(ex, 400, Json.erro("Viagem nao pode ser cancelada")); return; }
        viagemDAO.alterarStatus(id, StatusViagem.CANCELADA);
        responderJson(ex, 200, "{\"ok\":true}");
    }

    private void handleAlterarStatusViagem(HttpExchange ex) throws Exception {
        Map<String, String> body = lerJson(ex);
        long id = Long.parseLong(body.get("viagemId"));
        StatusViagem s = StatusViagem.valueOf(body.get("status").toUpperCase());
        viagemDAO.alterarStatus(id, s);
        if (s == StatusViagem.CONCLUIDA) {
            Viagem v = viagemDAO.buscarPorId(id);
            if (v != null && v.getMotoristaId() != null) {
                usuarioDAO.alterarDisponibilidade(v.getMotoristaId(), true);
            }
        }
        responderJson(ex, 200, "{\"ok\":true}");
    }

    private void handleLimiteSemanal(HttpExchange ex) throws Exception {
        Map<String, String> params = lerParams(ex);
        long cidadaoId = Long.parseLong(params.get("cidadaoId"));
        int usadas = viagemDAO.contarConcluidasNaSemana(cidadaoId);
        String json = Json.obj()
                .add("usadas",    usadas)
                .add("limite",    Cidadao.LIMITE_SEMANAL)
                .add("restantes", Math.max(0, Cidadao.LIMITE_SEMANAL - usadas))
                .fim();
        responderJson(ex, 200, json);
    }

    // -------------------------------------------------------------------------
    // /api/veiculos
    // -------------------------------------------------------------------------

    private void handleVeiculos(HttpExchange ex) throws IOException {
        addCors(ex);
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if ("OPTIONS".equals(method)) { responder(ex, 204, "text/plain", ""); return; }

            String idStr = extrairId(path, "/api/veiculos");

            if ("GET".equals(method)) {
                if (idStr != null) {
                    Veiculo v = veiculoDAO.buscarPorId(Long.parseLong(idStr));
                    if (v == null) { responderJson(ex, 404, Json.erro("Veiculo nao encontrado")); return; }
                    responderJson(ex, 200, serializarVeiculo(v));
                } else {
                    Map<String, String> p = lerParams(ex);
                    String mid = p.get("motoristaId");
                    List<Veiculo> lista = mid != null
                            ? veiculoDAO.listarPorMotorista(Long.parseLong(mid))
                            : veiculoDAO.listar();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < lista.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(serializarVeiculo(lista.get(i)));
                    }
                    sb.append("]");
                    responderJson(ex, 200, sb.toString());
                }
            } else if ("POST".equals(method)) {
                Map<String, String> body = lerJson(ex);
                Veiculo v = new Veiculo();
                v.setMotoristaId(Long.parseLong(body.get("motoristaId")));
                v.setPlaca(body.get("placa"));
                v.setModelo(body.get("modelo"));
                v.setCor(body.get("cor"));
                v.setAno(Integer.parseInt(body.getOrDefault("ano", "2020")));
                veiculoDAO.inserir(v);
                responderJson(ex, 201, serializarVeiculo(v));
            } else if ("PUT".equals(method) && idStr != null) {
                Map<String, String> body = lerJson(ex);
                Veiculo v = veiculoDAO.buscarPorId(Long.parseLong(idStr));
                if (v == null) { responderJson(ex, 404, Json.erro("Veiculo nao encontrado")); return; }
                if (body.containsKey("modelo")) v.setModelo(body.get("modelo"));
                if (body.containsKey("cor"))    v.setCor(body.get("cor"));
                if (body.containsKey("ano"))    v.setAno(Integer.parseInt(body.get("ano")));
                veiculoDAO.atualizar(v);
                responderJson(ex, 200, serializarVeiculo(v));
            } else if ("DELETE".equals(method) && idStr != null) {
                veiculoDAO.excluir(Long.parseLong(idStr));
                responderJson(ex, 200, "{\"ok\":true}");
            } else {
                metodoNaoPermitido(ex);
            }
        } catch (Exception e) {
            responderJson(ex, 500, Json.erro("Erro: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // /api/pagamentos
    // -------------------------------------------------------------------------

    private void handlePagamentos(HttpExchange ex) throws IOException {
        addCors(ex);
        String method = ex.getRequestMethod();

        try {
            if ("OPTIONS".equals(method)) { responder(ex, 204, "text/plain", ""); return; }

            if ("POST".equals(method)) {
                Map<String, String> body = lerJson(ex);
                long viagemId = Long.parseLong(body.get("viagemId"));
                FormaPagamento forma = FormaPagamento.valueOf(body.get("formaPagamento").toUpperCase());

                Viagem v = viagemDAO.buscarPorId(viagemId);
                if (v == null) { responderJson(ex, 404, Json.erro("Viagem nao encontrada")); return; }

                Pagamento pag = new Pagamento();
                pag.setViagemId(viagemId);
                pag.setValorTotal(v.getValorTotal());
                pag.setValorSubsidio(v.getValorSubsidio());
                pag.setValorCidadao(v.getValorCidadao());
                pag.setFormaPagamento(forma);
                pag.processar();

                pagamentoDAO.inserir(pag);
                viagemDAO.alterarStatus(viagemId, StatusViagem.CONCLUIDA);

                responderJson(ex, 201, Json.obj()
                        .add("pagamentoId",   pag.getId())
                        .add("valorTotal",    pag.getValorTotal())
                        .add("valorSubsidio", pag.getValorSubsidio())
                        .add("valorCidadao",  pag.getValorCidadao())
                        .add("autorizado",    pag.isAutorizado())
                        .fim());
            } else if ("GET".equals(method)) {
                Map<String, String> p = lerParams(ex);
                String vid = p.get("viagemId");
                if (vid != null) {
                    Pagamento pag = pagamentoDAO.buscarPorViagem(Long.parseLong(vid));
                    if (pag == null) { responderJson(ex, 404, Json.erro("Pagamento nao encontrado")); return; }
                    responderJson(ex, 200, Json.obj()
                            .add("pagamentoId",   pag.getId())
                            .add("valorTotal",    pag.getValorTotal())
                            .add("valorSubsidio", pag.getValorSubsidio())
                            .add("valorCidadao",  pag.getValorCidadao())
                            .add("autorizado",    pag.isAutorizado())
                            .fim());
                } else {
                    responderJson(ex, 400, Json.erro("Informe viagemId"));
                }
            } else {
                metodoNaoPermitido(ex);
            }
        } catch (Exception e) {
            responderJson(ex, 500, Json.erro("Erro: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Serializadores
    // -------------------------------------------------------------------------

    private String serializarUsuario(Usuario u) {
        Json b = Json.obj()
                .add("id",     u.getId())
                .add("nome",   u.getNome())
                .add("cpf",    u.getCpf())
                .add("email",  u.getEmail())
                .add("telefone", u.getTelefone())
                .add("tipo",   u.getTipo())
                .add("status", u.getStatus() != null ? u.getStatus().name() : "");
        if (u.getDataNascimento() != null)
            b.add("dataNascimento", u.getDataNascimento().toString());
        if (u.getMotivoRecusa() != null)
            b.add("motivoRecusa", u.getMotivoRecusa());
        if (u instanceof Cidadao) {
            Cidadao c = (Cidadao) u;
            if (c.getRendaFamiliar() != null) b.add("rendaFamiliar", c.getRendaFamiliar());
            if (c.getNis() != null) b.add("nis", c.getNis());
        }
        if (u instanceof Motorista) {
            Motorista m = (Motorista) u;
            b.add("cnh", m.getCnh());
            b.add("disponivel", m.isDisponivel());
        }
        return b.fim();
    }

    private String serializarViagem(Viagem v) {
        Json b = Json.obj()
                .add("id",          v.getId())
                .add("cidadaoId",   v.getCidadaoId())
                .add("origem",      v.getOrigem())
                .add("destino",     v.getDestino())
                .add("status",      v.getStatus() != null ? v.getStatus().name() : "");
        if (v.getMotoristaId() != null) b.add("motoristaId", v.getMotoristaId());
        if (v.getDistanciaKm() != null) b.add("distanciaKm", v.getDistanciaKm());
        if (v.getValorTotal()  != null) b.add("valorTotal",  v.getValorTotal());
        if (v.getValorSubsidio() != null) b.add("valorSubsidio", v.getValorSubsidio());
        if (v.getValorCidadao() != null) b.add("valorCidadao", v.getValorCidadao());
        if (v.getDataHora() != null) b.add("dataHora", v.getDataHora().toString());
        if (v.getNomeCidadao() != null)   b.add("nomeCidadao",   v.getNomeCidadao());
        if (v.getNomeMotorista() != null) b.add("nomeMotorista", v.getNomeMotorista());
        b.add("tempoEstimado", v.calcularTempoEstimado());
        return b.fim();
    }

    private String serializarVeiculo(Veiculo v) {
        return Json.obj()
                .add("id",         v.getId())
                .add("motoristaId", v.getMotoristaId())
                .add("placa",      v.getPlaca())
                .add("modelo",     v.getModelo())
                .add("cor",        v.getCor())
                .add("ano",        v.getAno())
                .fim();
    }

    // -------------------------------------------------------------------------
    // Utilitarios HTTP
    // -------------------------------------------------------------------------

    private void responderJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void responder(HttpExchange ex, int code, String mime, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", mime + "; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void metodoNaoPermitido(HttpExchange ex) throws IOException {
        responderJson(ex, 405, Json.erro("Metodo nao permitido"));
    }

    private void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private Map<String, String> lerJson(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Json.ler(body);
        }
    }

    private Map<String, String> lerParams(HttpExchange ex) {
        String query = ex.getRequestURI().getQuery();
        Map<String, String> map = new java.util.HashMap<>();
        if (query == null) return map;
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    private String extrairId(String path, String base) {
        String rest = path.substring(base.length());
        if (rest.startsWith("/")) rest = rest.substring(1);
        if (rest.isEmpty() || rest.contains("/")) return null;
        try { Long.parseLong(rest); return rest; } catch (NumberFormatException e) { return null; }
    }
}
