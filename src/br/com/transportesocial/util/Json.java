package br.com.transportesocial.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Montador e leitor simples de JSON, suficiente para a comunicacao
 * entre as telas (JavaScript) e a API do sistema.
 */
public final class Json {

    private final StringBuilder sb = new StringBuilder("{");
    private boolean primeiro = true;

    public static Json obj() {
        return new Json();
    }

    public Json add(String chave, String valor) {
        virgula();
        sb.append('"').append(chave).append("\":");
        if (valor == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escapar(valor)).append('"');
        }
        return this;
    }

    public Json add(String chave, Object valor) {
        return add(chave, valor == null ? null : valor.toString());
    }

    /** Numeros, booleanos, listas e objetos ja prontos entram sem aspas. */
    public Json bruto(String chave, String jsonValor) {
        virgula();
        sb.append('"').append(chave).append("\":").append(jsonValor == null ? "null" : jsonValor);
        return this;
    }

    /** Valores em dinheiro vao como numero com 2 casas (ex.: 12.50). */
    public Json add(String chave, java.math.BigDecimal valor) {
        return bruto(chave, valor == null ? null : valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
    }

    public Json add(String chave, double valor) {
        return bruto(chave, String.valueOf(valor));
    }

    public Json add(String chave, int valor) {
        return bruto(chave, String.valueOf(valor));
    }

    public Json add(String chave, boolean valor) {
        return bruto(chave, String.valueOf(valor));
    }

    public String fim() {
        return sb.append('}').toString();
    }

    private void virgula() {
        if (!primeiro) {
            sb.append(',');
        }
        primeiro = false;
    }

    public static String lista(List<String> itens) {
        return "[" + String.join(",", itens) + "]";
    }

    public static String erro(String mensagem) {
        return obj().add("erro", mensagem).fim();
    }

    public static String escapar(String texto) {
        StringBuilder out = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"'  -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default   -> out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Leitor de JSON simples (apenas pares chave/valor em um unico nivel),
     * que e o formato enviado pelas telas do sistema.
     */
    public static Map<String, String> ler(String json) {
        Map<String, String> mapa = new HashMap<>();
        if (json == null) {
            return mapa;
        }
        String texto = json.trim();
        if (texto.startsWith("{")) {
            texto = texto.substring(1);
        }
        if (texto.endsWith("}")) {
            texto = texto.substring(0, texto.length() - 1);
        }
        List<String> partes = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroDeTexto = false;
        boolean escape = false;
        for (char c : texto.toCharArray()) {
            if (escape) {
                atual.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                atual.append(c);
                escape = true;
                continue;
            }
            if (c == '"') {
                dentroDeTexto = !dentroDeTexto;
            }
            if (c == ',' && !dentroDeTexto) {
                partes.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        if (atual.length() > 0) {
            partes.add(atual.toString());
        }
        for (String par : partes) {
            int sep = indiceDosDoisPontos(par);
            if (sep < 0) {
                continue;
            }
            String chave = limpar(par.substring(0, sep));
            String valor = limpar(par.substring(sep + 1));
            mapa.put(chave, "null".equals(valor) ? null : valor);
        }
        return mapa;
    }

    private static int indiceDosDoisPontos(String par) {
        boolean dentroDeTexto = false;
        for (int i = 0; i < par.length(); i++) {
            char c = par.charAt(i);
            if (c == '"' && (i == 0 || par.charAt(i - 1) != '\\')) {
                dentroDeTexto = !dentroDeTexto;
            }
            if (c == ':' && !dentroDeTexto) {
                return i;
            }
        }
        return -1;
    }

    private static String limpar(String valor) {
        String v = valor.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return desescapar(v);
    }

    private static String desescapar(String v) {
        if (v.indexOf('\\') < 0) {
            return v;
        }
        StringBuilder out = new StringBuilder(v.length());
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c != '\\' || i + 1 >= v.length()) {
                out.append(c);
                continue;
            }
            char n = v.charAt(++i);
            switch (n) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (i + 4 < v.length()) {
                        out.append((char) Integer.parseInt(v.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                }
                default -> out.append(n);   // \" \\ \/
            }
        }
        return out.toString();
    }
}
