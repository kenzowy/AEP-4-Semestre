package br.com.transportesocial;

import br.com.transportesocial.util.Conexao;
import br.com.transportesocial.util.Senha;
import br.com.transportesocial.web.Servidor;

/**
 * Ponto de entrada da aplicacao Transporte Social.
 *
 * Uso:
 *   java -cp "bin;lib/mysql-connector-j.jar" br.com.transportesocial.Main
 *
 * No Linux/Mac substitua ; por : na linha acima.
 *
 * Para gerar o hash da senha do administrador:
 *   java -cp "bin;lib/mysql-connector-j.jar" br.com.transportesocial.util.Senha admin123
 */
public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("  Transporte Social - Sistema de Mobilidade");
        System.out.println("=================================================");

        // Verifica conexao com o banco
        System.out.print("[BANCO] Testando conexao... ");
        if (!Conexao.testar()) {
            System.out.println("FALHOU");
            System.out.println();
            System.out.println("ATENCAO: Nao foi possivel conectar ao banco de dados.");
            System.out.println("Verifique se o XAMPP esta rodando e o banco 'transporte_social' foi criado.");
            System.out.println("Execute o arquivo database/script.sql no phpMyAdmin.");
            System.exit(1);
        }
        System.out.println("OK");

        // Inicia o servidor HTTP
        Servidor servidor = new Servidor();
        servidor.iniciar();

        System.out.println("[INFO] Pressione Ctrl+C para encerrar.");
        System.out.println("=================================================");
    }
}
