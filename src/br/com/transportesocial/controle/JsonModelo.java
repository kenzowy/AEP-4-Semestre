package br.com.transportesocial.controle;

import br.com.transportesocial.model.Cidadao;
import br.com.transportesocial.model.Documento;
import br.com.transportesocial.model.Motorista;
import br.com.transportesocial.model.Pagamento;
import br.com.transportesocial.model.Usuario;
import br.com.transportesocial.model.Veiculo;
import br.com.transportesocial.model.Viagem;
import br.com.transportesocial.util.Json;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Converte os objetos do modelo em JSON para as telas. A senha nunca sai. */
final class JsonModelo {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private JsonModelo() {
    }

    static String usuario(Usuario u) {
        Json j = Json.obj()
                .add("id", u.getId())
                .add("tipo", u.getTipo())
                .add("nome", u.getNome())
                .add("cpf", u.getCpf())
                .add("dataNascimento", u.getDataNascimento().toString())
                .add("idade", u.calcularIdade())
                .add("email", u.getEmail())
                .add("telefone", u.getTelefone())
                .add("status", u.getStatus().name())
                .add("motivoRecusa", u.getMotivoRecusa());
        if (u instanceof Cidadao c) {
            j.add("rendaFamiliar", c.getRendaFamiliar()).add("nis", c.getNis());
        } else if (u instanceof Motorista m) {
            j.add("cnh", m.getCnh()).add("disponivel", m.isDisponivel());
            List<String> veiculos = new ArrayList<>();
            for (Veiculo v : m.getVeiculos()) {
                veiculos.add(veiculo(v));
            }
            j.bruto("veiculos", Json.lista(veiculos));
        }
        List<String> docs = new ArrayList<>();
        for (Documento d : u.getDocumentos()) {
            docs.add(documento(d));
        }
        j.bruto("documentos", Json.lista(docs));
        List<String> obrigatorios = new ArrayList<>();
        u.getDocumentosObrigatorios().forEach(t -> obrigatorios.add("\"" + t.name() + "\""));
        j.bruto("documentosObrigatorios", Json.lista(obrigatorios));
        return j.fim();
    }

    static String documento(Documento d) {
        return Json.obj()
                .add("id", d.getId())
                .add("tipo", d.getTipo().name())
                .add("dataEnvio", d.getDataEnvio().format(DATA_HORA))
                .fim();
    }

    static String veiculo(Veiculo v) {
        return Json.obj()
                .add("id", v.getId())
                .add("placa", v.getPlaca())
                .add("marca", v.getMarca())
                .add("modelo", v.getModelo())
                .add("ano", v.getAno())
                .add("cor", v.getCor())
                .fim();
    }

    static String viagem(Viagem v) {
        Json j = Json.obj()
                .add("id", v.getId())
                .add("origem", v.getOrigem())
                .add("destino", v.getDestino())
                .add("distanciaKm", v.getDistanciaKm())
                .add("valorTotal", v.getValorTotal())
                .add("valorSubsidio", v.getValorSubsidio())
                .add("valorCidadao", v.getValorCidadao())
                .add("qtdAcompanhantes", v.getQtdAcompanhantes())
                .add("dataHora", v.getDataHora().format(DATA_HORA))
                .add("status", v.getStatus().name())
                .add("tempoEstimadoMin", v.getTempoEstimadoMinutos())
                .add("cidadaoNome", v.getCidadao() == null ? null : v.getCidadao().getNome())
                .add("motoristaNome", v.getMotorista() == null ? null : v.getMotorista().getNome());
        Pagamento p = v.getPagamento();
        if (p == null) {
            j.bruto("pagamento", null);
        } else {
            j.bruto("pagamento", Json.obj()
                    .add("valor", p.getValor())
                    .add("forma", p.getForma().name())
                    .add("aprovado", p.isAprovado())
                    .add("dataHora", p.getDataHora().format(DATA_HORA))
                    .fim());
        }
        return j.fim();
    }

    static String viagens(List<Viagem> lista) {
        List<String> itens = new ArrayList<>();
        for (Viagem v : lista) {
            itens.add(viagem(v));
        }
        return Json.lista(itens);
    }
}
