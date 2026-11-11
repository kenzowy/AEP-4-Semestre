package br.com.transportesocial.controle;

import br.com.transportesocial.model.Administrador;
import br.com.transportesocial.model.Documento;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.util.ArmazenamentoArquivo;
import br.com.transportesocial.util.Conexao;
import br.com.transportesocial.util.Json;
import br.com.transportesocial.util.Sessao;
import br.com.transportesocial.dao.DocumentoDAO;
import com.sun.net.httpserver.HttpExchange;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /api/conta  (a propria conta do usuario logado)
 *   GET                 consultar os proprios dados (RF01)
 *   PUT                 alterar os proprios dados (RF01)
 *   DELETE              excluir a propria conta (RF01)
 *   POST /documentos    reenviar documentos apos recusa (RF02, RN04)
 */
public class ContaControle extends ControleBase {

    private final DocumentoDAO documentoDAO = new DocumentoDAO();

    @Override
    protected void tratar(HttpExchange ex, String metodo, String[] partes) throws Exception {
        Usuario u = usuarioLogado(ex);
        if (partes.length == 1) {
            switch (metodo) {
                case "GET" -> ok(ex, JsonModelo.usuario(u));
                case "PUT" -> alterar(ex, u);
                case "DELETE" -> excluir(ex, u);
                default -> naoEncontrado(ex);
            }
        } else if (partes.length == 2 && partes[1].equals("documentos") && metodo.equals("POST")) {
            reenviarDocumentos(ex, u);
        } else {
            naoEncontrado(ex);
        }
    }

    private void alterar(HttpExchange ex, Usuario u) throws Exception {
        Map<String, String> d = lerCorpo(ex);
        u.atualizarConta(d.get("nome"), d.get("email"), d.get("telefone"));
        String senhaNova = d.get("senhaNova");
        if (senhaNova != null && !senhaNova.isEmpty()) {
            if (!u.conferirSenha(d.get("senhaAtual") == null ? "" : d.get("senhaAtual"))) {
                throw new RegraNegocioException("Senha atual incorreta.");
            }
            u.definirSenha(senhaNova);
        }
        usuarioDAO.atualizar(u);
        ok(ex, JsonModelo.usuario(u));
    }

    private void excluir(HttpExchange ex, Usuario u) throws Exception {
        if (u instanceof Administrador) {
            throw new RegraNegocioException("A conta do administrador nao pode ser excluida pelo sistema.");
        }
        List<String> arquivos = new ArrayList<>();
        for (Documento d : u.getDocumentos()) {
            arquivos.add(d.getArquivo());
        }
        u.deletarConta();
        usuarioDAO.excluir(u.getId());   // documentos e veiculos saem junto (composicao)
        arquivos.forEach(ArmazenamentoArquivo::remover);
        Sessao.encerrarDoUsuario(u.getId());
        ok(ex, Json.obj().add("ok", true).fim());
    }

    /** RN04: cadastro recusado pode ser refeito com novos documentos. */
    private void reenviarDocumentos(HttpExchange ex, Usuario u) throws Exception {
        Map<String, String> d = lerCorpo(ex);
        int antes = u.getDocumentos().size();
        List<String> salvos = new ArrayList<>();
        try {
            if (CadastroApoio.anexarDocumentos(u, d, salvos) == 0) {
                throw new RegraNegocioException("Selecione pelo menos um documento.");
            }
            List<Documento> novos = u.getDocumentos().subList(antes, u.getDocumentos().size());
            try (Connection con = Conexao.abrir()) {
                con.setAutoCommit(false);
                try {
                    for (Documento doc : novos) {
                        documentoDAO.inserir(con, u.getId(), doc);
                    }
                    con.commit();
                } catch (Exception e) {
                    con.rollback();
                    throw e;
                }
            }
        } catch (Exception e) {
            CadastroApoio.desfazer(salvos);
            throw e;
        }
        usuarioDAO.atualizarStatus(u);  // RECUSADO -> PENDENTE
        ok(ex, JsonModelo.usuario(u));
    }
}
