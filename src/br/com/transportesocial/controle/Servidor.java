package br.com.transportesocial.controle;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/** Servidor HTTP do sistema: entrega as telas e a API em JSON na mesma porta. */
public class Servidor {

    private final int porta;
    private HttpServer http;

    public Servidor(int porta) {
        this.porta = porta;
    }

    public void iniciar() throws IOException {
        http = HttpServer.create(new InetSocketAddress(porta), 0);
        http.createContext("/api/login", new AutenticacaoControle());
        http.createContext("/api/logout", new AutenticacaoControle());
        http.createContext("/api/conta", new ContaControle());
        http.createContext("/api/usuarios", new UsuarioControle());
        http.createContext("/api/documentos", new DocumentoControle());
        http.createContext("/api/motorista", new MotoristaControle());
        http.createContext("/api/viagens", new ViagemControle());
        http.createContext("/api/relatorio", new RelatorioControle());
        http.createContext("/", new ArquivosEstaticos());
        http.setExecutor(Executors.newFixedThreadPool(8));
        http.start();
    }
}
