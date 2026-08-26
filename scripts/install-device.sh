#!/usr/bin/env bash
# Builda e instala no celular físico via Depuração sem fio (Wireless debugging).
# Uso:
#   ./scripts/install-device.sh                    # usa device já conectado
#   ./scripts/install-device.sh <ip>:<porta_conexao>  # conecta antes de instalar
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/temurin-17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

APP_ID="br.com.financas.app"
MAIN_ACTIVITY="$APP_ID/.MainActivity"

if [[ $# -ge 1 ]]; then
  echo "==> Conectando em $1..."
  adb connect "$1"
fi

if ! adb devices | grep -v "^List" | grep -q "device$"; then
  cat <<'EOF'
==> Nenhum dispositivo conectado.

Primeira vez (ou se o pareamento expirou), no celular:
  Configurações > Sistema > Opções do desenvolvedor > Depuração sem fio > ativar
  > "Parear dispositivo com código de pareamento"
  (celular e PC precisam estar na mesma rede Wi-Fi)

Isso mostra um <ip>:<porta_pareamento> e um código de 6 dígitos. Com o
celular na tela, rode aqui:

  adb pair <ip>:<porta_pareamento> <codigo>

Depois do pareamento, volte para a tela anterior "Depuração sem fio" —
ela mostra outro <ip>:<porta_conexao> (diferente da porta de pareamento).
Rode este script passando esse endereço:

  ./scripts/install-device.sh <ip>:<porta_conexao>

Da próxima vez, se o celular continuar com Depuração sem fio ativa na
mesma rede, o mesmo <ip>:<porta_conexao> costuma funcionar direto, sem
precisar parear de novo.
EOF
  exit 1
fi

echo "==> Dispositivo:"
adb devices

echo "==> Compilando e instalando build debug..."
./gradlew installDebug

echo "==> Abrindo o app..."
adb shell am start -n "$MAIN_ACTIVITY"

echo ""
echo "==> Pronto. Para acompanhar os logs do app:"
echo "    adb logcat --pid=\$(adb shell pidof -s $APP_ID)"
