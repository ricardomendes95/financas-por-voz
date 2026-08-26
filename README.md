# Finanças por Voz

App Android nativo de finanças pessoais com entrada por voz em português
brasileiro. Uso pessoal, single-user, offline-first, sideload.

Veja `CLAUDE.md` para as regras do projeto e `spec-app-financas-voz.md` para
o requisito completo.

## Pré-requisitos (uma vez só)

- JDK 17 em `~/.jdks/temurin-17`
- Android SDK em `~/Android/Sdk`, com `platform-tools`, `platforms;android-36`
  e o `emulator` instalados
- Um AVD chamado `financas_test` (`avdmanager create avd -n financas_test ...`)
- No WSL2/Linux com KVM: seu usuário precisa estar no grupo `kvm`
  (`sudo usermod -aG kvm $USER`, depois reabra o terminal)

Os scripts abaixo já exportam `JAVA_HOME`/`ANDROID_HOME` sozinhos — não
precisa configurar nada na sua shell.

## Build

```bash
./scripts/build.sh
```

Gera `app/build/outputs/apk/debug/app-debug.apk`.

## Modo dev (emulador + instala + abre o app)

```bash
./scripts/dev.sh
```

Um único comando que:

1. Sobe o emulador `financas_test` se nenhum estiver rodando (reaproveita se
   já estiver aberto).
2. Espera o boot completar.
3. Builda e instala o APK debug (`installDebug`).
4. Abre o app (`MainActivity`) no emulador.

Ao final ele imprime o comando para acompanhar os logs:

```bash
adb logcat --pid=$(adb shell pidof -s br.com.financas.app)
```

## Instalar no celular físico (Wi-Fi)

O WSL2 não enxerga dispositivos USB por padrão, então o caminho mais simples
é a Depuração sem fio do Android (11+), sem precisar instalar nada no
Windows. Celular e PC precisam estar na mesma rede Wi-Fi.

No celular, uma vez (ou sempre que o pareamento expirar):

1. `Configurações > Sistema > Opções do desenvolvedor` (se não existir,
   ative em `Sobre o telefone > Informações de software`, tocando 7x em
   "Número da versão do build").
2. `Depuração sem fio` → ativar → `Parear dispositivo com código de
   pareamento`. Aparece um `<ip>:<porta_pareamento>` e um código de 6
   dígitos.
3. No terminal:

   ```bash
   adb pair <ip>:<porta_pareamento> <codigo>
   ```

4. Volte para a tela anterior "Depuração sem fio" — ela mostra outro
   `<ip>:<porta_conexao>` (diferente da porta de pareamento usada acima).

```bash
./scripts/install-device.sh <ip>:<porta_conexao>
```

Da próxima vez, se o celular continuar com Depuração sem fio ativa na
mesma rede, rodar `./scripts/install-device.sh <ip>:<porta_conexao>` de
novo costuma bastar, sem precisar parear outra vez. Rodar
`./scripts/install-device.sh` sem argumento reusa um dispositivo já
conectado.

Se preferir USB físico em vez de Wi-Fi: o WSL2 não repassa a porta USB
sozinho, é preciso instalar o `usbipd-win` no Windows e compartilhar o
dispositivo com o WSL antes do `adb` do Linux enxergá-lo. Nas telas do
próprio Android/Samsung ao conectar o cabo: em **"USB controlado por"**
escolha **"Dispositivo conectado"**; em **"Usar USB para"** escolha
**"Transferência de arquivos"**.

## Outros comandos úteis

```bash
./gradlew :nlu:test                    # corpus do parser de linguagem natural
./gradlew test                         # todos os testes JVM/Robolectric
./gradlew lintDebug                    # lint
adb shell am start -a android.intent.action.VIEW \
  -d "financas://add?text=20%20reais%20de%20pastel"   # deep link de entrada por voz
```
