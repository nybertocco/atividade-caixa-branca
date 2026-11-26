@echo off
REM Compila e executa as duas suítes. Uso (na raiz do repositório): testes\run-tests.bat
REM Requer apenas JDK 11 ou superior. Saídas gravadas em evidencias\.
chcp 65001 >nul
cd /d "%~dp0.."
if exist build rmdir /s /q build
mkdir build 2>nul
mkdir evidencias 2>nul

echo ^>^> Compilando original/
javac -encoding UTF-8 -d build\original original\ProcessadorPedidos.java || exit /b 2
javac -encoding UTF-8 -cp build\original -d build\testes-original testes\MiniTeste.java testes\TestesOriginal.java || exit /b 2

echo ^>^> Compilando revisado/
javac -encoding UTF-8 -Xlint:all -d build\revisado revisado\ProcessadorPedidos.java || exit /b 2
javac -encoding UTF-8 -cp build\revisado -d build\testes-revisado testes\MiniTeste.java testes\TestesRevisado.java || exit /b 2

echo ^>^> Executando suite ORIGINAL (falhas esperadas = defeitos do legado)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp build\original;build\testes-original TestesOriginal > evidencias\saida_testes_original.txt
type evidencias\saida_testes_original.txt

echo ^>^> Executando suite REVISADA (todos devem passar)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp build\revisado;build\testes-revisado TestesRevisado > evidencias\saida_testes_revisado.txt
set STATUS=%ERRORLEVEL%
type evidencias\saida_testes_revisado.txt
echo ^>^> Codigo de saida da suite revisada: %STATUS%
exit /b %STATUS%
