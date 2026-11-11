package br.com.transportesocial.controle;

import br.com.transportesocial.model.RegraNegocioException;
import br.com.transportesocial.model.Veiculo;

import java.util.Map;

/** Monta um Veiculo a partir dos campos enviados pela tela. */
final class VeiculoApoio {

    private VeiculoApoio() {
    }

    static Veiculo lerVeiculo(Map<String, String> d) {
        Veiculo v = new Veiculo();
        v.setPlaca(d.get("placa"));
        v.setMarca(d.get("marca"));
        v.setModelo(d.get("modelo"));
        String ano = d.get("ano");
        if (ano == null || ano.isBlank()) {
            throw new RegraNegocioException("Informe o ano do veiculo.");
        }
        v.setAno(ControleBase.inteiro(ano, "ano"));
        v.setCor(d.get("cor"));
        return v;
    }
}
