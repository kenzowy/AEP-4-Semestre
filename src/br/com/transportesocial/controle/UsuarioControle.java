package br.com.transportesocial.controle;

import br.com.transportesocial.model.Administrador;
import br.com.transportesocial.model.Cidadao;
import br.com.transportesocial.model.Motorista;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.StatusCadastro;
import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.model.Veiculo;
import br.com.transportesocial.util.Json;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /api/usuarios
 *   POST                     cadastro de cidadao ou motorista com documentos (RF01, RF02)
 *   GET  ?tipo=&status=      administrador consulta os usuarios (RF01, RF03)
 *   GET  /{id}               administrador consulta um cadastro e seus documentos (RF03)
 *   POST /{id}/aprovar       administrador aprova (RF03)
 *   POST /{id}/recusar       administrador recusa com motivo (RF03)
 */
public class UsuarioControle extends ControleBase {

    @Override
    protected void tratar(HttpExchange ex, String metodo, String[] partes) throws Exception {
        if (partes.length == 1 && metodo.equals("POST")) {
            cadastrar(ex);
            return;
        }
        Administrador admin = exigirPerfil(ex, Administrador.class);
        if (partes.length == 1 && metodo.equals("GET")) {
            consultar(ex);
        } else if (partes.length == 2 && metodo.equals("GET")) {
            Usuario u = buscar(idDaRota(partes, 1));
            ok(ex, JsonModelo.usuario(u));
        } else if (partes.length == 3 && metodo.equals("POST") && partes[2].equals("aprovar")) {
            Usuario u = buscar(idDaRota(partes, 1));
            admin.aprovarCadastro(u);
            usuarioDAO.atualizarStatus(u);
            ok(ex, JsonModelo.usuario(u));
        } else if (partes.length == 3 && metodo.equals("POST") && partes[2].equals("recusar")) {
            Usuario u = buscar(idDaRota(partes, 1));
            admin.recusarCadastro(u, lerCorpo(ex).get("motivo"));
            usuarioDAO.atualizarStatus(u);
            ok(ex, JsonModelo.usuario(u));
        } else {
            naoEncontrado(ex);
        }
    }

    /** RF01 + RF02: cadastro feito pelo proprio cidadao ou motorista. */
    private void cadastrar(HttpExchange ex) throws Exception {
        Map<String, String> d = lerCorpo(ex);
        String tipo = d.get("tipo") == null ? "" : d.get("tipo");
        Usuario u = switch (tipo) {
            case "CIDADAO" -> new Cidadao();
            case "MOTORISTA" -> new Motorista();
            default -> new Administrador();
        };
        u.setNome(d.get("nome"));
        u.setCpf(d.get("cpf"));
        u.setDataNascimento(data(d.get("dataNascimento"), "data de nascimento"));
        u.setEmail(d.get("email"));
        u.setTelefone(d.get("telefone"));
        u.definirSenha(d.get("senha"));

        if (u instanceof Cidadao c) {
            if (d.get("rendaFamiliar") == null || d.get("rendaFamiliar").isBlank()) {
                throw new RegraNegocioException("Informe a renda familiar.");
            }
            c.setRendaFamiliar(dinheiro(d.get("rendaFamiliar"), "renda familiar"));
            c.setNis(d.get("nis"));
        } else if (u instanceof Motorista m) {
            m.setCnh(d.get("cnh"));
            m.registrarVeiculo(VeiculoApoio.lerVeiculo(d));
        }

        if (u.getCpf() != null && usuarioDAO.buscarPorCpf(u.getCpf()) != null) {
            throw new RegraNegocioException("Ja existe um cadastro com este CPF.");
        }

        List<String> salvos = new ArrayList<>();
        try {
            CadastroApoio.anexarDocumentos(u, d, salvos);
            // Polimorfismo: cada subclasse confere o cadastro do seu jeito.
            if (!u.validarCadastro()) {
                throw new RegraNegocioException("O administrador e criado somente pela prefeitura.");
            }
            usuarioDAO.inserir(u);
        } catch (Exception e) {
            CadastroApoio.desfazer(salvos);
            throw e;
        }
        responder(ex, 201, JsonModelo.usuario(u));
    }

    /** RF01 / RF03: consulta com filtros opcionais de tipo e status. */
    private void consultar(HttpExchange ex) throws Exception {
        String tipo = parametro(ex, "tipo");
        String status = parametro(ex, "status");
        if (tipo != null && !tipo.equals("CIDADAO") && !tipo.equals("MOTORISTA")) {
            tipo = null;
        }
        StatusCadastro filtro = null;
        if (status != null && !status.isEmpty()) {
            try {
                filtro = StatusCadastro.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new RegraNegocioException("Status invalido.");
            }
        }
        List<String> itens = new ArrayList<>();
        for (Usuario u : usuarioDAO.listar(tipo, filtro)) {
            itens.add(JsonModelo.usuario(u));
        }
        ok(ex, Json.lista(itens));
    }

    private Usuario buscar(int id) throws Exception {
        Usuario u = usuarioDAO.buscarPorId(id);
        if (u == null || u instanceof Administrador) {
            throw new RegraNegocioException("Usuario nao encontrado.");
        }
        return u;
    }
}
