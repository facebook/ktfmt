# Подключение ktfmt (klimat style) без Gradle

Как использовать форк [simonvar/ktfmt](https://github.com/simonvar/ktfmt) со стилем
`--klimat-style` в Android Studio, в git pre-commit хуке и на CI — без Gradle-плагинов.

Klimat style — это kotlinlang-стиль (4 пробела) плюс:

- блочные выражения (`remember { … }`, `when`, `if` с фигурными скобками) не переносятся
  со строки `=` / `by`;
- авторские переносы в цепочках вызовов (`.foo()` с новой строки, например Compose
  `Modifier`-цепочки) сохраняются;
- trailing comma добавляются там, где нужны, но авторские не удаляются.

## Артефакты

Всё скачивается со страницы релизов: <https://github.com/simonvar/ktfmt/releases>.
К каждому релизу (тег вида `v0.65-klimat.1`) приложены:

| Артефакт | Что это |
|---|---|
| `ktfmt-<версия>-with-dependencies.jar` | CLI, запускается через `java -jar` (нужна Java 17+) |
| `ktfmt-<версия>-<os>-<arch>` | нативный бинарь CLI (GraalVM): `linux-x86_64`, `linux-aarch64`, `darwin-aarch64`; Java не нужна, стартует мгновенно |
| `ktfmt_idea_plugin-1.3.<версия>.zip` | плагин для Android Studio / IntelliJ IDEA |

### Установка CLI (для External Tools, хука и локальных запусков)

#### Homebrew (macOS и Linux) — проще всего

```sh
brew install simonvar/tap/ktfmt
```

Ставит нативный бинарь на Apple Silicon и Linux (x86_64/aarch64), а на Intel-маках —
jar через `openjdk` (зависимость подтянется сама). Homebrew кладёт формулы без
карантина, а сам бинарь подписан ad-hoc в CI — поэтому возни с `xattr` нет, `ktfmt`
сразу доступен в `PATH`. Обновление — `brew upgrade ktfmt`; формула в
[тапе](https://github.com/simonvar/homebrew-tap) обновляется автоматически на каждый релиз.

#### Скачать бинарь вручную

Если Homebrew недоступен — нативный бинарь со страницы релиза:

```sh
VERSION=0.65-klimat.1
# macOS (Apple Silicon): darwin-aarch64; Linux: linux-x86_64 / linux-aarch64
# для macOS на Intel нативного бинаря нет — используйте jar
curl -fsSL -o ~/bin/ktfmt \
  "https://github.com/simonvar/ktfmt/releases/download/v${VERSION}/ktfmt-${VERSION}-darwin-aarch64"
chmod +x ~/bin/ktfmt
# если качали браузером, macOS повесит карантин и заблокирует запуск — снимаем
# (для curl обычно не нужно; бинарь ad-hoc подписан, так что Apple Silicon его пускает):
xattr -d com.apple.quarantine ~/bin/ktfmt 2>/dev/null || true
```

(`~/bin` должен быть в `PATH`; подойдёт любое другое место.)

Либо jar: скачайте `ktfmt-<версия>-with-dependencies.jar` и запускайте как
`java -jar ktfmt.jar …` — везде ниже, где написано `ktfmt`, это взаимозаменяемо.

### Шпаргалка по CLI

```sh
ktfmt --klimat-style Foo.kt src/main/kotlin   # форматирует на месте; директории обходятся рекурсивно (*.kt, *.kts)
ktfmt --klimat-style -n --set-exit-if-changed src/   # только проверка: exit 1 и список файлов, ничего не пишет
git ls-files -z -- '*.kt' '*.kts' | xargs -0 ktfmt --klimat-style   # весь репозиторий (только файлы под git)
```

Для всего репозитория используйте вариант с `git ls-files`, а не `ktfmt .` — обход
директорий зацепит сгенерированный код в `build/`.

## Android Studio

### Вариант 1: плагин (рекомендуется)

Плагин подменяет штатное форматирование Kotlin-файлов, поэтому работают все привычные
механизмы IDE: `Reformat Code`, форматирование выделенного фрагмента, Actions on Save.

1. Скачайте `ktfmt_idea_plugin-1.3.<версия>.zip` из релиза.
   Стиль **Klimat** есть в плагине начиная с версии `1.3.0.65-klimat.2`
   (в `…klimat.1` его в списке ещё нет).
2. **Settings → Plugins → ⚙ → Install Plugin from Disk…** → выберите zip → перезапустите IDE.
3. **Settings → Editor → ktfmt Settings**: включите **Enable ktfmt**, в **Code style**
   выберите **Klimat**.
4. Готово: `Reformat Code` (⌥⌘L / Ctrl+Alt+L) теперь форматирует через ktfmt.
5. Формат при сохранении (по желанию): **Settings → Tools → Actions on Save →
   Reformat code**.

Обновление плагина — вручную тем же способом при выходе нового релиза.

### Вариант 2: External Tools (без плагина)

Подходит, если плагин ставить нельзя или нужен ровно тот же бинарь, что на CI.
Понадобится установленный CLI (см. выше).

1. **Settings → Tools → External Tools → +** и заполните:
   - **Name:** `ktfmt (klimat)`
   - **Program:** полный путь к бинарю, например `/Users/<you>/bin/ktfmt`
     (для jar: **Program** `java`, а в начало **Arguments** добавьте
     `-jar /path/to/ktfmt-with-dependencies.jar`)
   - **Arguments:** `--klimat-style --quiet "$FilePath$"`
   - **Working directory:** `$ProjectFileDir$`
   - В **Advanced Options** оставьте включённым **Synchronize files after execution**
     (иначе IDE не перечитает отформатированный файл) и снимите
     **Open console for tool output**, чтобы не мигала консоль.
2. Повесьте шорткат: **Settings → Keymap → External Tools → ktfmt (klimat)** →
   правый клик → **Add Keyboard Shortcut** (например ⌥⌘K, чтобы не конфликтовать
   со штатным ⌥⌘L).

IDE сохраняет файлы перед запуском external tool, так что несохранённые правки не теряются.
Ограничения по сравнению с плагином: форматируется только весь файл целиком (не выделение),
и `Reformat Code` / Actions on Save продолжают использовать встроенный форматтер — им
пользоваться не нужно.

## Git pre-commit hook

Хук хранится в репозитории и включается одной командой у каждого разработчика.

1. Создайте в корне репозитория файл `.githooks/pre-commit`:

   ```sh
   #!/bin/sh
   # Форматирует застейдженные Kotlin-файлы (klimat style) и возвращает их в индекс.
   # Включение: git config core.hooksPath .githooks
   # Путь к ktfmt можно переопределить: KTFMT="java -jar /path/to/ktfmt.jar" git commit …

   KTFMT="${KTFMT:-ktfmt}"

   files=$(git diff --cached --name-only --diff-filter=ACMR -- '*.kt' '*.kts')
   [ -z "$files" ] && exit 0

   if ! command -v "${KTFMT%% *}" >/dev/null 2>&1; then
     echo "pre-commit: ktfmt не найден в PATH (см. docs/klimat-setup.md)" >&2
     exit 1
   fi

   echo "$files" | tr '\n' '\0' | xargs -0 $KTFMT --klimat-style --quiet || exit 1
   echo "$files" | tr '\n' '\0' | xargs -0 git add
   ```

2. Сделайте его исполняемым и включите:

   ```sh
   chmod +x .githooks/pre-commit
   git config core.hooksPath .githooks   # один раз на каждый клон
   ```

Нюанс: при частичном стейджинге (`git add -p`) хук форматирует файл на диске и
командой `git add` заберёт в коммит **все** его изменения, не только застейдженные.
Если так работать некомфортно, замените две последние строки на «только проверку» —
коммит просто не пройдёт, пока файлы не отформатированы:

```sh
echo "$files" | tr '\n' '\0' | xargs -0 $KTFMT --klimat-style -n --set-exit-if-changed || {
  echo "pre-commit: запустите ktfmt --klimat-style по файлам выше и повторите коммит" >&2
  exit 1
}
```

## CI

Идея одна на любой CI: скачать ktfmt из релиза и прогнать его в режиме проверки по
всем Kotlin-файлам под git. Exit code 1 — есть неотформатированные файлы (их список
попадает в лог).

### GitHub Actions

```yaml
name: ktfmt

on:
  pull_request:
  push:
    branches: [main]

jobs:
  ktfmt:
    runs-on: ubuntu-latest
    env:
      KTFMT_VERSION: 0.65-klimat.1
    steps:
      - uses: actions/checkout@v4

      - name: Install ktfmt
        run: |
          curl -fsSL -o "$RUNNER_TEMP/ktfmt" \
            "https://github.com/simonvar/ktfmt/releases/download/v${KTFMT_VERSION}/ktfmt-${KTFMT_VERSION}-linux-x86_64"
          chmod +x "$RUNNER_TEMP/ktfmt"
          echo "$RUNNER_TEMP" >> "$GITHUB_PATH"

      - name: Check formatting
        run: git ls-files -z -- '*.kt' '*.kts' | xargs -0 ktfmt --klimat-style -n --set-exit-if-changed

      # Опционально: при падении показать в логе сам дифф, а не только список файлов
      - name: Show diff on failure
        if: failure()
        run: |
          git ls-files -z -- '*.kt' '*.kts' | xargs -0 ktfmt --klimat-style --quiet
          git diff
```

Вариант без нативного бинаря — через jar (например, если раннеры не x86_64/aarch64 Linux):

```yaml
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: zulu

      - name: Install ktfmt
        run: |
          curl -fsSL -o "$RUNNER_TEMP/ktfmt.jar" \
            "https://github.com/simonvar/ktfmt/releases/download/v${KTFMT_VERSION}/ktfmt-${KTFMT_VERSION}-with-dependencies.jar"

      - name: Check formatting
        run: git ls-files -z -- '*.kt' '*.kts' | xargs -0 java -jar "$RUNNER_TEMP/ktfmt.jar" --klimat-style -n --set-exit-if-changed
```

### GitLab CI

```yaml
ktfmt:
  image: eclipse-temurin:17-jre
  variables:
    KTFMT_VERSION: "0.65-klimat.1"
  script:
    - curl -fsSL -o ktfmt.jar "https://github.com/simonvar/ktfmt/releases/download/v${KTFMT_VERSION}/ktfmt-${KTFMT_VERSION}-with-dependencies.jar"
    - git ls-files -z -- '*.kt' '*.kts' | xargs -0 java -jar ktfmt.jar --klimat-style -n --set-exit-if-changed
```

## Обновление версии

Версия зашита в трёх местах: у разработчиков (CLI-бинарь и/или плагин), в хуке (берёт
`ktfmt` из `PATH`) и в CI (`KTFMT_VERSION`). При выпуске нового релиза обновите
`KTFMT_VERSION` в CI и попросите команду скачать свежий бинарь/плагин — расхождение
версий безопасно, но может давать разный формат в редких случаях. Разработчикам на
Homebrew достаточно `brew upgrade ktfmt` (формула в тапе бампается сама на релизе).
