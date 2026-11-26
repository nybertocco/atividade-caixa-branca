import java.util.function.Supplier;

/**
 * Mini-framework de testes sem dependências externas (roda só com o JDK).
 *
 * Cada caso informa ID, método, caminho coberto, entrada, pré-condição e resultado esperado.
 * O resultado observado é o texto devolvido pelo caso. A comparação considera só o trecho antes
 * de " | " — o que vem depois é detalhe (por exemplo, a mensagem da exceção) e só é impresso.
 */
public final class MiniTeste {

    private final String suite;
    private int passou;
    private int falhou;

    public MiniTeste(String suite) {
        this.suite = suite;
        System.out.println("==================================================================");
        System.out.println(" " + suite);
        System.out.println(" Java " + System.getProperty("java.version"));
        System.out.println("==================================================================");
    }

    public void caso(String id, String metodo, String caminho, String entrada, String preCondicao,
                     String esperado, Supplier<String> execucao) {
        String observado;
        try {
            observado = execucao.get();
        } catch (Throwable t) {
            observado = "exceção não tratada " + t.getClass().getSimpleName() + " | " + t.getMessage();
        }
        String comparavel = observado.split(" \\| ", 2)[0];
        boolean ok = comparavel.equals(esperado);
        if (ok) {
            passou++;
        } else {
            falhou++;
        }
        System.out.println();
        System.out.println((ok ? "[PASSOU] " : "[FALHOU] ") + id + "  " + metodo + "  caminho: " + caminho);
        System.out.println("   pré-condição: " + preCondicao);
        System.out.println("   entrada     : " + entrada);
        System.out.println("   esperado    : " + esperado);
        System.out.println("   observado   : " + observado);
    }

    /** Imprime o resumo e devolve a quantidade de falhas. */
    public int resumo() {
        System.out.println();
        System.out.println("------------------------------------------------------------------");
        System.out.println(" " + suite + " -> total: " + (passou + falhou)
                + " | passou: " + passou + " | falhou: " + falhou);
        System.out.println("------------------------------------------------------------------");
        return falhou;
    }
}
