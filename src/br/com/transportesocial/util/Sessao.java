package br.com.transportesocial.util;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda quem esta logado. No login o sistema gera um token aleatorio que a
 * tela envia em toda requisicao; assim cada tela so e liberada para o perfil
 * correspondente (RNF01).
 */
public final class Sessao {

    private static final Map<String, Integer> TOKENS = new ConcurrentHashMap<>();
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Sessao() {
    }

    public static String criar(int usuarioId) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        TOKENS.put(token, usuarioId);
        return token;
    }

    /** Retorna o id do usuario dono do token, ou null se o token nao vale. */
    public static Integer usuarioDoToken(String token) {
        return token == null ? null : TOKENS.get(token);
    }

    public static void encerrar(String token) {
        if (token != null) {
            TOKENS.remove(token);
        }
    }

    /** Encerra todas as sessoes de um usuario (ex.: conta excluida). */
    public static void encerrarDoUsuario(int usuarioId) {
        TOKENS.values().removeIf(id -> id == usuarioId);
    }
}
