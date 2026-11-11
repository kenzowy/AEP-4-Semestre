package br.com.transportesocial.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centraliza a conexao com o MySQL (XAMPP/MariaDB).
 * Altere as constantes abaixo se o seu ambiente usar outra porta ou senha.
 */
public final class Conexao {

    private static final String HOST  = "localhost";
    private static final String PORTA = "3306";
    private static final String BANCO = "transporte_social";
    private static final String USUARIO = "root";
    private static final String SENHA = "";   // XAMPP: root sem senha

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORTA + "/" + BANCO
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Sao_Paulo";

    private Conexao() {
    }

    public static Connection abrir() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver do MySQL nao encontrado. "
                    + "Coloque o mysql-connector-j.jar na pasta lib.", e);
        }
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

    /** Testa a conexao na subida do servidor. */
    public static boolean testar() {
        try (Connection c = abrir()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            System.out.println("[BANCO] Falha ao conectar: " + e.getMessage());
            return false;
        }
    }
}
