import java.util.Random;

/**
 * Testes de caixa branca do código ORIGINAL (original/ProcessadorPedidos.java).
 *
 * O "esperado" de cada caso é o comportamento CORRETO pelo requisito, escrito no contrato do
 * legado: em calcularAcrescimo, entrada inválida deveria gerar IllegalArgumentException; em
 * processar, pedido inválido deveria devolver false sem alterar o último total.
 * Por isso os casos que FALHAM aqui são a demonstração dos defeitos do legado.
 *
 * Caminhos: A1..A4 = calcularAcrescimo; P1..P7 = processar (ver README, seção 6).
 */
public class TestesOriginal {

    public static void main(String[] args) {
        MiniTeste t = new MiniTeste("SUITE ORIGINAL - original/ProcessadorPedidos.java");
        String novo = "objeto novo (aprovado=false, ultimoTotal=0)";

        // ---------------------------------------------------------- calcularAcrescimo
        t.caso("CT01", "calcularAcrescimo", "A1", "PIX, subtotal=100, parcelas=1", novo,
                "0.0", () -> acr("PIX", 100, 1));
        t.caso("CT02", "calcularAcrescimo", "A2", "CREDITO, subtotal=100, parcelas=3", novo,
                "8.0", () -> acr("CREDITO", 100, 3));
        t.caso("CT03", "calcularAcrescimo", "A3", "CREDITO, subtotal=100, parcelas=1", novo,
                "3.0", () -> acr("CREDITO", 100, 1));
        t.caso("CT04", "calcularAcrescimo", "A4", "BOLETO (forma inexistente), subtotal=100, parcelas=1", novo,
                "exceção IllegalArgumentException", () -> acr("BOLETO", 100, 1));
        t.caso("CT05", "calcularAcrescimo", "A2 (limite)", "CREDITO, subtotal=100, parcelas=2", novo,
                "8.0", () -> acr("CREDITO", 100, 2));
        t.caso("CT06", "calcularAcrescimo", "A4", "\"credito\" (minúsculo), subtotal=100, parcelas=3", novo,
                "8.0", () -> acr("credito", 100, 3));
        t.caso("CT07", "calcularAcrescimo", "A3", "CREDITO, subtotal=100, parcelas=0", novo,
                "exceção IllegalArgumentException", () -> acr("CREDITO", 100, 0));
        t.caso("CT08", "calcularAcrescimo", "A1", "PIX, subtotal=100, parcelas=5", novo,
                "exceção IllegalArgumentException", () -> acr("PIX", 100, 5));
        t.caso("CT09", "calcularAcrescimo", "A3", "CREDITO, subtotal=-100, parcelas=1", novo,
                "exceção IllegalArgumentException", () -> acr("CREDITO", -100, 1));
        t.caso("CT10", "calcularAcrescimo", "exceção no nó 2", "pagamento=null, subtotal=100, parcelas=1", novo,
                "exceção IllegalArgumentException", () -> acr(null, 100, 1));
        t.caso("CT30", "calcularAcrescimo", "A2 (acima do limite)", "CREDITO, subtotal=100, parcelas=13", novo,
                "exceção IllegalArgumentException", () -> acr("CREDITO", 100, 13));
        t.caso("CT31", "calcularAcrescimo", "A2 (limite máximo)", "CREDITO, subtotal=100, parcelas=12", novo,
                "8.0", () -> acr("CREDITO", 100, 12));

        // ---------------------------------------------------------- processar
        t.caso("CT11", "processar", "P1", "{50}, PIX, vip=false, parcelas=1", novo,
                "retorno=true; ultimoTotal=50.0", () -> proc(new ProcessadorPedidos(), new double[]{50}, "PIX", false, 1));
        t.caso("CT12", "processar", "P2", "{} (pedido vazio), PIX, vip=false, parcelas=1", novo,
                "retorno=false; ultimoTotal=0.0", () -> proc(new ProcessadorPedidos(), new double[]{}, "PIX", false, 1));
        t.caso("CT13", "processar", "P3", "{-10}, PIX, vip=false, parcelas=1", novo,
                "retorno=false; ultimoTotal=0.0", () -> proc(new ProcessadorPedidos(), new double[]{-10}, "PIX", false, 1));
        t.caso("CT14", "processar", "P1+P3 (2 iterações)", "{50, -10}, PIX, vip=false, parcelas=1", novo,
                "retorno=false; ultimoTotal=0.0", () -> proc(new ProcessadorPedidos(), new double[]{50, -10}, "PIX", false, 1));
        t.caso("CT15", "processar", "P4", "{200}, PIX, vip=true, parcelas=1", novo,
                "retorno=true; ultimoTotal=180.0", () -> proc(new ProcessadorPedidos(), new double[]{200}, "PIX", true, 1));
        t.caso("CT16", "processar", "P1 (limite VIP)", "{100}, PIX, vip=true, parcelas=1", novo,
                "retorno=true; ultimoTotal=100.0", () -> proc(new ProcessadorPedidos(), new double[]{100}, "PIX", true, 1));
        t.caso("CT17", "processar", "P4 (limite VIP)", "{100.01}, PIX, vip=true, parcelas=1", novo,
                "retorno=true; ultimoTotal=90.01", () -> proc(new ProcessadorPedidos(), new double[]{100.01}, "PIX", true, 1));
        t.caso("CT18", "processar", "P1 (vip=false, subtotal>100)", "{200}, PIX, vip=false, parcelas=1", novo,
                "retorno=true; ultimoTotal=200.0", () -> proc(new ProcessadorPedidos(), new double[]{200}, "PIX", false, 1));
        t.caso("CT19", "processar", "P6", "precos=null, PIX, vip=false, parcelas=1", novo,
                "retorno=false; ultimoTotal=0.0", () -> proc(new ProcessadorPedidos(), null, "PIX", false, 1));
        t.caso("CT20", "processar", "P7", "{50}, pagamento=null, vip=false, parcelas=1", novo,
                "retorno=false; ultimoTotal=0.0", () -> proc(new ProcessadorPedidos(), new double[]{50}, null, false, 1));
        t.caso("CT21", "processar", "P1 seguido de P6", "precos=null, PIX, vip=false, parcelas=1",
                "mesmo objeto já aprovou {50} (aprovado=true, ultimoTotal=50)",
                "retorno=false; ultimoTotal=50.0", () -> {
                    ProcessadorPedidos p = new ProcessadorPedidos();
                    p.processar(new double[]{50}, "PIX", false, 1);
                    return proc(p, null, "PIX", false, 1);
                });
        t.caso("CT22", "processar", "P1 seguido de P7", "{999}, pagamento=null, vip=false, parcelas=1",
                "mesmo objeto já aprovou {50} (aprovado=true, ultimoTotal=50)",
                "retorno=false; ultimoTotal=50.0", () -> {
                    ProcessadorPedidos p = new ProcessadorPedidos();
                    p.processar(new double[]{50}, "PIX", false, 1);
                    return proc(p, new double[]{999}, null, false, 1);
                });
        t.caso("CT23", "processar + calcularAcrescimo", "P1 + A2", "{100}, CREDITO, vip=false, parcelas=3", novo,
                "retorno=true; ultimoTotal=108.0", () -> proc(new ProcessadorPedidos(), new double[]{100}, "CREDITO", false, 3));
        t.caso("CT24", "processar + calcularAcrescimo", "P4 + A3", "{200}, CREDITO, vip=true, parcelas=1", novo,
                "retorno=true; ultimoTotal=185.4", () -> proc(new ProcessadorPedidos(), new double[]{200}, "CREDITO", true, 1));
        t.caso("CT25", "processar", "P1 (2 iterações)", "{0.1, 0.2}, PIX, vip=false, parcelas=1", novo,
                "retorno=true; ultimoTotal=0.3", () -> proc(new ProcessadorPedidos(), new double[]{0.1, 0.2}, "PIX", false, 1));
        t.caso("CT26", "processar", "P3 + P1", "{NaN, 50}, PIX, vip=false, parcelas=1", novo,
                "retorno=false; ultimoTotal=0.0", () -> proc(new ProcessadorPedidos(), new double[]{Double.NaN, 50}, "PIX", false, 1));
        t.caso("CT27", "processar", "P1", "{Infinity}, PIX, vip=false, parcelas=1", novo,
                "retorno=false; ultimoTotal=0.0", () -> proc(new ProcessadorPedidos(), new double[]{Double.POSITIVE_INFINITY}, "PIX", false, 1));
        t.caso("CT28", "processar + calcularAcrescimo", "P1 + A4", "{50}, BOLETO, vip=false, parcelas=1", novo,
                "retorno=false; ultimoTotal=0.0", () -> proc(new ProcessadorPedidos(), new double[]{50}, "BOLETO", false, 1));
        t.caso("CT29", "processar", "P5 (viabilidade)", "100.000 pedidos aleatórios (semente 42)",
                "objeto novo a cada pedido; precos e pagamento não nulos",
                "ocorrências de total<0: 0", TestesOriginal::buscaTotalNegativo);

        t.caso("CT32", "processar (test double)", "P7 com stub", "{50}, PIX, vip=false, parcelas=1",
                "stub (subclasse) cujo calcularAcrescimo lança IllegalStateException",
                "exceção IllegalStateException; ultimoTotal=0.0", () -> {
                    ProcessadorPedidos stub = new ProcessadorPedidos() {
                        @Override
                        public double calcularAcrescimo(String pagamento, double subtotal, int parcelas) {
                            throw new IllegalStateException("falha simulada do serviço de taxas");
                        }
                    };
                    return proc(stub, new double[]{50}, "PIX", false, 1);
                });

        int falhas = t.resumo();
        System.out.println(" Observação: nesta suíte as falhas são ESPERADAS — cada uma documenta um defeito");
        System.out.println(" do código legado (ver README, seção 7). Falhas encontradas: " + falhas);
    }

    // ---------------------------------------------------------------- apoio

    private static String acr(String pagamento, double subtotal, int parcelas) {
        try {
            return String.valueOf(new ProcessadorPedidos().calcularAcrescimo(pagamento, subtotal, parcelas));
        } catch (RuntimeException e) {
            return "exceção " + e.getClass().getSimpleName() + " | " + e.getMessage();
        }
    }

    private static String proc(ProcessadorPedidos p, double[] precos, String pagamento, boolean vip, int parcelas) {
        try {
            boolean r = p.processar(precos, pagamento, vip, parcelas);
            return "retorno=" + r + "; ultimoTotal=" + p.getUltimoTotal();
        } catch (RuntimeException e) {
            return "exceção " + e.getClass().getSimpleName() + "; ultimoTotal=" + p.getUltimoTotal()
                    + " | " + e.getMessage();
        }
    }

    /**
     * Tenta induzir o ramo falso de "if (total >= 0)" (caminho P5). Com objeto novo e entradas não
     * nulas, processar só devolve false se esse ramo for executado. Inclui valores especiais.
     */
    private static String buscaTotalNegativo() {
        Random r = new Random(42);
        double[] especiais = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                -Double.MAX_VALUE, Double.MAX_VALUE, -0.0, 0.0, -1e-300};
        String[] formas = {"PIX", "CREDITO", "BOLETO", ""};
        int ocorrencias = 0;
        for (int n = 0; n < 100_000; n++) {
            double[] precos = new double[r.nextInt(6)];
            for (int i = 0; i < precos.length; i++) {
                precos[i] = r.nextInt(10) == 0 ? especiais[r.nextInt(especiais.length)]
                        : (r.nextDouble() * 2000) - 1000;
            }
            ProcessadorPedidos p = new ProcessadorPedidos();
            boolean aprovado = p.processar(precos, formas[r.nextInt(formas.length)], r.nextBoolean(), r.nextInt(17) - 2);
            if (!aprovado) {
                ocorrencias++;
            }
        }
        return "ocorrências de total<0: " + ocorrencias;
    }
}
