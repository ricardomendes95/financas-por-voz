#!/usr/bin/env bash
# Modo dev: sobe o emulador (se preciso), builda, instala e abre o app.
# Uso: ./scripts/dev.sh
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/temurin-17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

AVD_NAME="${AVD_NAME:-financas_test}"
APP_ID="br.com.financas.app"
MAIN_ACTIVITY="$APP_ID/.MainActivity"

emulator_running() {
  adb devices | grep -q "^emulator-.*device$"
}

start_emulator() {
  echo "==> Nenhum emulador rodando. Iniciando AVD '$AVD_NAME'..."

  local emulator_cmd="emulator -avd $AVD_NAME -netdelay none -netspeed full"

  # 'id -nG "$USER"' lê os grupos estáticos do /etc/group (sempre inclui
  # kvm, já que o usermod já rodou) — não serve para saber se ESTA sessão
  # já tem o grupo efetivo. 'id -nG' sem argumento é o que reflete a sessão
  # atual; só ele muda depois de um novo login.
  if id -nG | tr ' ' '\n' | grep -qw kvm; then
    $emulator_cmd >/tmp/financas-emulator.log 2>&1 &
  else
    # A sessão atual de shell pode não ter recarregado o grupo 'kvm' ainda
    # (adicionado via usermod, mas só efetivo em nova sessão/login). O 'sg'
    # roda o comando com o grupo efetivo correto sem precisar relogar.
    echo "==> Grupo 'kvm' não ativo nesta sessão — usando 'sg kvm' para aceleração de hardware."
    sg kvm -c "$emulator_cmd" >/tmp/financas-emulator.log 2>&1 &
  fi
  local emulator_pid=$!
  disown

  echo "==> Aguardando o dispositivo aparecer no adb..."
  while true; do
    if ! kill -0 "$emulator_pid" 2>/dev/null; then
      echo "==> ERRO: o processo do emulador encerrou antes de aparecer no adb."
      echo "    Log completo em /tmp/financas-emulator.log:"
      tail -n 30 /tmp/financas-emulator.log
      exit 1
    fi
    if adb devices | grep -q "^emulator-.*device$"; then
      break
    fi
    sleep 1
  done

  echo "==> Aguardando o boot completar (pode levar 1-2 min na primeira vez)..."
  until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    sleep 2
  done
  echo "==> Emulador pronto."
}

if emulator_running; then
  echo "==> Emulador já está rodando, reaproveitando."
else
  start_emulator
fi

echo "==> Compilando e instalando build debug..."
./gradlew installDebug

echo "==> Abrindo o app..."
adb shell am start -n "$MAIN_ACTIVITY"

echo ""
echo "==> Pronto. Para acompanhar os logs do app:"
echo "    adb logcat --pid=\$(adb shell pidof -s $APP_ID)"
