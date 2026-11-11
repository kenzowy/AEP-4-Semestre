package br.com.transportesocial.util;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Gera e confere o hash das senhas (RNF01).
 * Usa PBKDF2 com salt, algoritmo que ja vem no proprio Java,
 * para que nenhuma senha seja gravada em texto puro no banco.
 */
public final class Senha {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 65536;
    private static final int TAMANHO = 256;

    private Senha() {
    }

    public static String gerarHash(String senha) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = calcular(senha, salt);
            return Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar o hash da senha", e);
        }
    }

    public static boolean conferir(String senha, String hashSalvo) {
        try {
            if (hashSalvo == null || !hashSalvo.contains(":")) {
                return false;
            }
            String[] partes = hashSalvo.split(":");
            byte[] salt = Base64.getDecoder().decode(partes[0]);
            byte[] esperado = Base64.getDecoder().decode(partes[1]);
            byte[] calculado = calcular(senha, salt);
            if (esperado.length != calculado.length) {
                return false;
            }
            int diferenca = 0;
            for (int i = 0; i < esperado.length; i++) {
                diferenca |= esperado[i] ^ calculado[i];
            }
            return diferenca == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] calcular(String senha, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, ITERACOES, TAMANHO);
        SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
        return fabrica.generateSecret(spec).getEncoded();
    }

    /** Utilitario: gera o hash do administrador para colar no script.sql. */
    public static void main(String[] args) {
        String senha = args.length > 0 ? args[0] : "admin123";
        System.out.println(gerarHash(senha));
    }
}
