package br.com.transportesocial;

import br.com.transportesocial.controle.Servidor;
import br.com.transportesocial.controle.ViagemControle;
import br.com.transportesocial.util.Conexao;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Ponto de entrada: confere o banco, liga o servidor e a rotina de cancelamento automatico. */
public class Main {

    private static final int PORTA = 8080;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Transporte Social ===");
        if (!Conexao.testar()) {
            System.out.println("Nao foi possivel conectar ao MySQL.");
            System.out.println("Confira: 1) MySQL ligado no XAMPP  2) script database/script.sql importado");
            System.out.println("         3) usuario/senha em src/br/com/transportesocial/util/Conexao.java");
            System.exit(1);
        }
        System.out.println("[BANCO] Conectado ao banco transporte_social.");

        // RN09: a cada minuto cancela as solicitacoes sem motorista ha mais de 10 minutos.
        ScheduledExecutorService agenda = Executors.newSingleThreadScheduledExecutor();
        agenda.scheduleAtFixedRate(() -> {
            try {
                int canceladas = ViagemControle.cancelarSolicitacoesExpiradas();
                if (canceladas > 0) {
                    System.out.println("[RN09] " + canceladas + " solicitacao(oes) cancelada(s) por falta de motorista.");
                }
            } catch (Exception e) {
                System.out.println("[RN09] Falha ao cancelar solicitacoes: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);

        new Servidor(PORTA).iniciar();
        System.out.println("[SERVIDOR] Sistema no ar. Abra no navegador: http://localhost:" + PORTA);
        System.out.println("Para encerrar, pressione Ctrl + C.");
    }
}
