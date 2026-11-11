package br.com.transportesocial.controle;

import br.com.transportesocial.dao.DocumentoDAO;
import br.com.transportesocial.model.Administrador;
import br.com.transportesocial.model.Documento;
import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.util.ArmazenamentoArquivo;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;

/**
 * GET /api/documentos/{id}
 * RNF02 (LGPD): o arquivo do documento so pode ser aberto pelo administrador.
 */
public class DocumentoControle extends ControleBase {

    private final DocumentoDAO documentoDAO = new DocumentoDAO();

    @Override
    protected void tratar(HttpExchange ex, String metodo, String[] partes) throws Exception {
        if (!metodo.equals("GET") || partes.length != 2) {
            naoEncontrado(ex);
            return;
        }
        exigirPerfil(ex, Administrador.class);
        Documento d = documentoDAO.buscarPorId(idDaRota(partes, 1));
        if (d == null) {
            throw new RegraNegocioException("Documento nao encontrado.");
        }
        byte[] bytes = ArmazenamentoArquivo.ler(d.getArquivo());
        ex.getResponseHeaders().set("Content-Type", ArmazenamentoArquivo.tipoConteudo(d.getArquivo()));
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
