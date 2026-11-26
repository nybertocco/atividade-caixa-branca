#!/usr/bin/env bash
# Compila e executa as duas suítes. Uso (na raiz do repositório): ./testes/run-tests.sh
# Requer apenas JDK 11 ou superior. Saídas gravadas em evidencias/.
set -u
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac  # separador do classpath: ";" no Windows
cd "$(dirname "$0")/.."
rm -rf build && mkdir -p build evidencias

echo ">> Compilando original/"
javac -encoding UTF-8 -d build/original original/ProcessadorPedidos.java || exit 2
javac -encoding UTF-8 -cp build/original -d build/testes-original testes/MiniTeste.java testes/TestesOriginal.java || exit 2

echo ">> Compilando revisado/"
javac -encoding UTF-8 -Xlint:all -d build/revisado revisado/ProcessadorPedidos.java || exit 2
javac -encoding UTF-8 -cp build/revisado -d build/testes-revisado testes/MiniTeste.java testes/TestesRevisado.java || exit 2

echo ">> Executando suíte ORIGINAL (falhas esperadas = defeitos do legado)"
CMD_O="java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp build/original${SEP}build/testes-original TestesOriginal"
{ echo "# comando: $CMD_O"; echo "# data: $(date -Iseconds)"; $CMD_O; } | tee evidencias/saida_testes_original.txt

echo ">> Executando suíte REVISADA (todos devem passar)"
CMD_R="java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp build/revisado${SEP}build/testes-revisado TestesRevisado"
{ echo "# comando: $CMD_R"; echo "# data: $(date -Iseconds)"; $CMD_R; echo "# código de saída: $?"; } | tee evidencias/saida_testes_revisado.txt
status=$(grep -o "código de saída: [0-9]*" evidencias/saida_testes_revisado.txt | grep -o "[0-9]*$")
echo ">> Código de saída da suíte revisada: $status"
exit $status
