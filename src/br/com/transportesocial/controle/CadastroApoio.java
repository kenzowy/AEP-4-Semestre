package br.com.transportesocial.controle;

import br.com.transportesocial.model.Documento;
import br.com.transportesocial.model.TipoDocumento;
import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.util.ArmazenamentoArquivo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Le os documentos enviados pela tela (RF02) e anexa ao usuario.
 * Campos esperados: doc_CPF_nome / doc_CPF_conteudo (Base64), etc.
 */
final class CadastroApoio {

    private CadastroApoio() {
    }

    /** Salva os arquivos enviados e retorna os caminhos gravados (para desfazer em caso de erro). */
    static int anexarDocumentos(Usuario u, Map<String, String> dados, List<String> salvos) throws IOException {
        int enviados = 0;
        for (TipoDocumento tipo : u.getDocumentosObrigatorios()) {
            String nome = dados.get("doc_" + tipo.name() + "_nome");
            String conteudo = dados.get("doc_" + tipo.name() + "_conteudo");
            if (nome == null || conteudo == null || conteudo.isEmpty()) {
                continue;
            }
            String caminho = ArmazenamentoArquivo.salvar(nome, conteudo);
            salvos.add(caminho);
            u.enviarDocumento(new Documento(tipo, caminho));
            enviados++;
        }
        return enviados;
    }

    static void desfazer(List<String> salvos) {
        for (String caminho : salvos) {
            ArmazenamentoArquivo.remover(caminho);
        }
    }
}
