package br.com.transportesocial.util;

import br.com.transportesocial.model.RegraNegocioException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/**
 * Salva os arquivos dos documentos enviados no cadastro (RF02) na pasta
 * "uploads", fora da pasta publica "web". So o administrador consegue
 * abrir esses arquivos pela API (RNF02 - LGPD).
 */
public final class ArmazenamentoArquivo {

    public static final Path PASTA = Paths.get("uploads");
    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024; // 5 MB

    private ArmazenamentoArquivo() {
    }

    /**
     * Grava o arquivo enviado pela tela (conteudo em Base64) e retorna o
     * caminho que sera guardado na coluna documento.arquivo.
     */
    public static String salvar(String nomeOriginal, String conteudoBase64) throws IOException {
        String extensao = extensao(nomeOriginal);
        if (!extensao.equals("pdf") && !extensao.equals("jpg") && !extensao.equals("jpeg")
                && !extensao.equals("png")) {
            throw new RegraNegocioException("Envie os documentos em PDF, JPG ou PNG.");
        }
        String base64 = conteudoBase64;
        int virgula = base64.indexOf(',');
        if (base64.startsWith("data:") && virgula > 0) {
            base64 = base64.substring(virgula + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Arquivo do documento invalido.");
        }
        if (bytes.length == 0) {
            throw new RegraNegocioException("Arquivo do documento vazio.");
        }
        if (bytes.length > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("Cada documento pode ter no maximo 5 MB.");
        }
        Files.createDirectories(PASTA);
        String nome = UUID.randomUUID() + "." + extensao;
        Files.write(PASTA.resolve(nome), bytes);
        return "uploads/" + nome;
    }

    public static byte[] ler(String caminho) throws IOException {
        return Files.readAllBytes(resolver(caminho));
    }

    public static void remover(String caminho) {
        try {
            Files.deleteIfExists(resolver(caminho));
        } catch (IOException | RuntimeException e) {
            System.out.println("[ARQUIVO] Nao foi possivel remover " + caminho);
        }
    }

    public static String tipoConteudo(String caminho) {
        return switch (extensao(caminho)) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            default -> "image/jpeg";
        };
    }

    /** Impede caminhos fora da pasta uploads. */
    private static Path resolver(String caminho) {
        Path arquivo = Paths.get(caminho).normalize();
        if (!arquivo.startsWith(PASTA)) {
            throw new RegraNegocioException("Arquivo invalido.");
        }
        return arquivo;
    }

    private static String extensao(String nome) {
        if (nome == null) {
            return "";
        }
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }
}
