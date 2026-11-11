package br.com.transportesocial.model;

/**
 * Lancada quando uma regra de negocio e violada (ex.: limite semanal atingido).
 * A mensagem e mostrada para o usuario na tela.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
