package br.com.transportesocial.controle;

import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.util.Json;
import br.com.transportesocial.util.Sessao;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/** POST /api/login e POST /api/logout. */
public class AutenticacaoControle extends ControleBase {

    @Override
    protected void tratar(HttpExchange ex, String metodo, String[] partes) throws Exception {
        if (!metodo.equals("POST") || partes.length != 1) {
            naoEncontrado(ex);
            return;
        }
        if (partes[0].equals("logout")) {
            Sessao.encerrar(token(ex));
            ok(ex, Json.obj().add("ok", true).fim());
            return;
        }
        Map<String, String> dados = lerCorpo(ex);
        String cpf = dados.get("cpf");
        String senha = dados.get("senha");
        Usuario u = (cpf == null || senha == null) ? null : usuarioDAO.buscarPorCpf(cpf);
        if (u == null || !u.conferirSenha(senha)) {
            responder(ex, 401, Json.erro("CPF ou senha incorretos."));
            return;
        }
        // Todos entram; quem nao foi aprovado ve a situacao do cadastro
        // e tem as funcoes de viagem bloqueadas (RF04).
        String token = Sessao.criar(u.getId());
        ok(ex, Json.obj().add("token", token).bruto("usuario", JsonModelo.usuario(u)).fim());
    }
}
