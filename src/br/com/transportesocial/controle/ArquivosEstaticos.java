package br.com.transportesocial.controle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Entrega as telas (HTML, CSS e JavaScript) da pasta "web". */
public class ArquivosEstaticos implements HttpHandler {

    private static final Path PASTA_WEB = Paths.get("web").toAbsolutePath().normalize();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String caminho = ex.getRequestURI().getPath();
            if (caminho.equals("/") || caminho.isEmpty()) {
                caminho = "/index.html";
            }
            Path arquivo = PASTA_WEB.resolve(caminho.substring(1)).normalize();
            if (!arquivo.startsWith(PASTA_WEB) || !Files.isRegularFile(arquivo)) {
                byte[] msg = "Pagina nao encontrada".getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(404, msg.length);
                try (OutputStream out = ex.getResponseBody()) {
                    out.write(msg);
                }
                return;
            }
            byte[] bytes = Files.readAllBytes(arquivo);
            ex.getResponseHeaders().set("Content-Type", tipo(arquivo.toString()));
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        } finally {
            ex.close();
        }
    }

    private String tipo(String nome) {
        if (nome.endsWith(".html")) return "text/html; charset=utf-8";
        if (nome.endsWith(".css")) return "text/css; charset=utf-8";
        if (nome.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (nome.endsWith(".png")) return "image/png";
        if (nome.endsWith(".svg")) return "image/svg+xml";
        if (nome.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
