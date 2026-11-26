import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Processa pedidos: soma os itens, aplica o desconto VIP e o acréscimo da forma de pagamento.
 *
 * <p>Versão revisada do código legado (ver {@code original/ProcessadorPedidos.java}).
 * Principais mudanças em relação ao original:</p>
 * <ul>
 *   <li>Valores monetários calculados com {@link BigDecimal} e arredondados a 2 casas
 *       (HALF_UP), eliminando erros de ponto flutuante como 0.1 + 0.2 = 0.30000000000000004.</li>
 *   <li>Todas as entradas são validadas; entradas inválidas geram
 *       {@link IllegalArgumentException} com mensagem clara, em vez de serem ignoradas.</li>
 *   <li>O {@code catch (RuntimeException)} vazio foi removido: nenhum erro é escondido.</li>
 *   <li>O estado de aprovação é zerado no início de cada processamento, então uma falha
 *       nunca devolve a aprovação de um pedido anterior.</li>
 *   <li>Constantes nomeadas no lugar dos "números mágicos" 0.08, 0.03, 0.90 e 100.</li>
 *   <li>A decisão {@code if (total >= 0)}, cujo ramo falso era inalcançável, foi eliminada.</li>
 * </ul>
 *
 * <p>As assinaturas públicas de {@code calcularAcrescimo} e {@code processar} foram mantidas
 * (String, double, int, double[]) para não quebrar os chamadores existentes.</p>
 *
 * <p>Os métodos que leem ou alteram o estado são {@code synchronized}, para que uma mesma
 * instância possa ser compartilhada entre threads sem leituras inconsistentes.</p>
 */
public class ProcessadorPedidos {

    /** Formas de pagamento aceitas. Qualquer outro texto é rejeitado. */
    public enum FormaPagamento {
        PIX,
        CREDITO;

        /**
         * Converte o texto recebido em uma forma de pagamento.
         * Ignora espaços nas pontas e diferença entre maiúsculas e minúsculas ("pix" = "PIX").
         *
         * @throws IllegalArgumentException se o texto for nulo, vazio ou desconhecido
         */
        static FormaPagamento converter(String texto) {
            if (texto == null || texto.trim().isEmpty()) {
                throw new IllegalArgumentException("Forma de pagamento é obrigatória.");
            }
            String normalizado = texto.trim().toUpperCase(Locale.ROOT);
            for (FormaPagamento forma : values()) {
                if (forma.name().equals(normalizado)) {
                    return forma;
                }
            }
            throw new IllegalArgumentException("Forma de pagamento desconhecida: " + texto);
        }
    }

    /** Acréscimo do cartão de crédito parcelado (mais de 1 parcela): 8%. */
    static final BigDecimal TAXA_CREDITO_PARCELADO = new BigDecimal("0.08");
    /** Acréscimo do cartão de crédito à vista (1 parcela): 3%. */
    static final BigDecimal TAXA_CREDITO_A_VISTA = new BigDecimal("0.03");
    /** Fator do desconto VIP (10% de desconto). */
    static final BigDecimal FATOR_DESCONTO_VIP = new BigDecimal("0.90");
    /** O desconto VIP vale só para subtotal ESTRITAMENTE maior que este valor (regra do legado). */
    static final BigDecimal LIMITE_DESCONTO_VIP = new BigDecimal("100");
    /** Limite de parcelas no crédito (premissa de negócio documentada no README). */
    static final int MAX_PARCELAS = 12;

    private static final int CASAS_DECIMAIS = 2;

    /** Total do último pedido APROVADO. Não muda quando um pedido é rejeitado. */
    private BigDecimal ultimoTotal = BigDecimal.ZERO.setScale(CASAS_DECIMAIS);
    /** Indica se a ÚLTIMA chamada a processar(...) foi aprovada. */
    private boolean ultimoPedidoAprovado = false;

    /**
     * Calcula o acréscimo da forma de pagamento sobre um subtotal.
     *
     * @param pagamento "PIX" ou "CREDITO" (sem diferenciar maiúsculas/minúsculas)
     * @param subtotal  valor finito e não negativo
     * @param parcelas  1 para PIX; de 1 a {@value #MAX_PARCELAS} para crédito
     * @return acréscimo arredondado a 2 casas decimais
     * @throws IllegalArgumentException se algum parâmetro for inválido
     */
    public double calcularAcrescimo(String pagamento, double subtotal, int parcelas) {
        FormaPagamento forma = FormaPagamento.converter(pagamento);
        validarParcelas(forma, parcelas);
        BigDecimal valor = valorMonetario(subtotal, "Subtotal");
        return acrescimoSobre(forma, valor, parcelas).doubleValue();
    }

    /**
     * Processa um pedido. Se for aprovado, guarda o total em {@link #getUltimoTotal()}.
     *
     * @param precos    preços dos itens; ao menos 1 item, todos finitos e não negativos
     * @param pagamento "PIX" ou "CREDITO"
     * @param vip       cliente VIP recebe 10% de desconto se o subtotal for maior que 100
     * @param parcelas  número de parcelas (ver {@link #calcularAcrescimo})
     * @return {@code true} quando o pedido é aprovado
     * @throws IllegalArgumentException se o pedido for inválido; nesse caso
     *         {@link #isUltimoPedidoAprovado()} passa a ser {@code false}
     */
    public synchronized boolean processar(double[] precos, String pagamento, boolean vip, int parcelas) {
        // Zera a aprovação ANTES de validar: uma falha nunca reaproveita o resultado anterior.
        ultimoPedidoAprovado = false;

        FormaPagamento forma = FormaPagamento.converter(pagamento);
        validarParcelas(forma, parcelas);
        BigDecimal subtotal = somarItens(precos);

        if (vip && subtotal.compareTo(LIMITE_DESCONTO_VIP) > 0) {
            subtotal = arredondar(subtotal.multiply(FATOR_DESCONTO_VIP));
        }

        // Subtotal >= 0 e acréscimo >= 0 garantidos pelas validações: o total nunca é negativo,
        // por isso a decisão "if (total >= 0)" do legado (ramo falso inalcançável) foi removida.
        BigDecimal total = arredondar(subtotal.add(acrescimoSobre(forma, subtotal, parcelas)));

        ultimoTotal = total;
        ultimoPedidoAprovado = true;
        return true;
    }

    /** @return total do último pedido aprovado (0.0 se nenhum foi aprovado) */
    public synchronized double getUltimoTotal() {
        return ultimoTotal.doubleValue();
    }

    /** @return {@code true} se a última chamada a processar(...) terminou aprovada */
    public synchronized boolean isUltimoPedidoAprovado() {
        return ultimoPedidoAprovado;
    }

    // ------------------------------------------------------------------ apoio

    private static BigDecimal acrescimoSobre(FormaPagamento forma, BigDecimal subtotal, int parcelas) {
        switch (forma) {
            case PIX:
                return BigDecimal.ZERO.setScale(CASAS_DECIMAIS);
            case CREDITO:
                BigDecimal taxa = parcelas > 1 ? TAXA_CREDITO_PARCELADO : TAXA_CREDITO_A_VISTA;
                return arredondar(subtotal.multiply(taxa));
            default:
                // Só acontece se alguém criar uma nova forma no enum e esquecer de tratá-la aqui.
                throw new IllegalStateException("Forma de pagamento sem regra de acréscimo: " + forma);
        }
    }

    private static void validarParcelas(FormaPagamento forma, int parcelas) {
        if (forma == FormaPagamento.PIX && parcelas != 1) {
            throw new IllegalArgumentException("PIX aceita somente 1 parcela; recebido: " + parcelas);
        }
        if (parcelas < 1 || parcelas > MAX_PARCELAS) {
            throw new IllegalArgumentException(
                    "Parcelas devem estar entre 1 e " + MAX_PARCELAS + "; recebido: " + parcelas);
        }
    }

    private static BigDecimal somarItens(double[] precos) {
        if (precos == null) {
            throw new IllegalArgumentException("A lista de preços é obrigatória.");
        }
        if (precos.length == 0) {
            throw new IllegalArgumentException("O pedido precisa ter ao menos 1 item.");
        }
        BigDecimal soma = BigDecimal.ZERO;
        for (int i = 0; i < precos.length; i++) {
            soma = soma.add(valorMonetario(precos[i], "Preço do item " + i));
        }
        return soma;
    }

    /** Converte um double em BigDecimal exato, recusando NaN, infinito e negativos. */
    private static BigDecimal valorMonetario(double valor, String nome) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new IllegalArgumentException(nome + " deve ser um número finito; recebido: " + valor);
        }
        if (valor < 0) {
            throw new IllegalArgumentException(nome + " não pode ser negativo; recebido: " + valor);
        }
        // valueOf usa a representação decimal do double ("0.1"), e não o binário (0.1000000000000000055...).
        return BigDecimal.valueOf(valor);
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP);
    }
}
