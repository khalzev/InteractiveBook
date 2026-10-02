# Interactive Book — Этап 1 (техническая база)

Реализовано по паспорту проекта, раздел 85 «Первый этап разработки»:
- Android-проект, Kotlin, Jetpack Compose;
- базовая навигация (Главное меню ↔ Экран сцены) через Navigation Compose;
- заготовка `GameState` и структура пакетов `core/` `content/` `ui/`, как в разделе 4;
- три тестовые сцены (`content/scenes/DemoContent.kt`), чтобы проверить переходы — **это временный
  контент прямо в коде**, на Этапе 12 он переедет в JSON согласно принципу ENGINE ≠ CONTENT.

Пока НЕ реализовано (сознательно, по плану этапов из раздела 85): Check Engine, Conditions/Effects,
Save/DataStore, Character creation, Inventory, Journal, Flags/Resources/Relationships, Checkpoint —
папки под них уже созданы (`core/checks`, `core/conditions`, `core/effects`, `core/save`) с
файлами-заглушками и комментариями, к какому этапу относится реализация.

## Как открыть проект

1. Распакуйте архив.
2. Android Studio → **Open** → выберите папку `InteractiveBook`.
3. Дождитесь Gradle Sync.
4. Run ▶ на эмуляторе или устройстве (minSdk 26 / Android 8.0+).

## Про Gradle wrapper

В архиве **нет** `gradlew`, `gradlew.bat` и `gradle-wrapper.jar` (это бинарный файл, я не могу
сгенерировать его в своей среде без доступа к сети). Есть только `gradle/wrapper/gradle-wrapper.properties`
с указанием версии Gradle (8.9).

При открытии проекта Android Studio, скорее всего, сама предложит один из вариантов:
- **"Create Gradle Wrapper"** — нажмите, Studio сама скачает нужный `gradle-wrapper.jar`;
- либо synс пройдёт через локально установленный Gradle/встроенный в Studio — тоже нормально.

Если Studio ничего не предлагает и Sync падает именно с ошибкой про отсутствующий wrapper —
пришлите мне точный текст ошибки, я подскажу, что нажать, либо просто сгенерирую wrapper вручную,
если это окажется нужно.

## Что должно получиться при запуске

1. Открывается главное меню: **Новая игра / Продолжить / Настройки / Выход**.
2. «Продолжить» неактивна, пока не начата игра.
3. «Новая игра» → создаётся `GameState`, открывается экран сцены: иллюстрация-плейсхолдер сверху,
   художественный текст, кнопки действий снизу (макет соответствует разделу 79 паспорта).
4. Действия «Осмотреться» / «Заговорить с трактирщиком» переключают сцены.
5. Кнопка «Вернуться в меню» возвращает в меню, при этом «Продолжить» становится активной.
6. Повторное нажатие «Новая игра» при уже начатой игре показывает диалог подтверждения
   (раздел 69 — сброс требует подтверждения).

## Исправлено: "Cannot find a Java installation... languageVersion=17"

Если раньше Sync/Build падал с ошибкой вида:

```
Cannot find a Java installation on your machine matching this tasks requirements:
{languageVersion=17, vendor=any, implementation=vendor-specific}
No locally installed toolchains match and toolchain download repositories have not been configured.
```

— это значит, что на машине физически нет установленного JDK 17, а Gradle не умел его
скачать сам. В этой версии в `settings.gradle.kts` добавлен плагин
`org.gradle.toolchains.foojay-resolver-convention`, который включает автозагрузку JDK
нужной версии — при следующем Sync Gradle должен сам скачать JDK 17 (нужен интернет).

Если download всё равно не проходит (например, из-за корпоративного firewall/прокси),
альтернативный ручной способ:
**Android Studio → Settings/Preferences → Build, Execution, Deployment → Build Tools → Gradle
→ Gradle JDK** — выберите там JDK 17 из списка (Android Studio обычно предлагает скачать его
прямо из этого меню, пункт «Download JDK») или уже установленный JDK 17 на компьютере.

## Если Android Studio покажет ошибку

Пришлите мне:
- точный текст ошибки из вкладки **Build** (или Logcat, если приложение собралось, но упало
  при запуске);
- в идеале — на каком именно шаге она возникла (Sync / Build / Run).

Я подготовлю исправленный проект и пришлю новый zip — так же, как вы работали раньше.

## Известный нюанс, учтённый заранее

По итогам предыдущей попытки (раздел 88 паспорта) в проект уже заложено:
- `org.jetbrains.kotlin.plugin.compose` версии `2.2.20`, подключённый и в корневом, и в app-модуле;
- явный `kotlin { jvmToolchain(17) }` и `compileOptions` на Java 17 — во избежание проблемы
  рассинхронизации JVM target / "Kotlin target-validation".

Если ошибка всё равно связана с версиями Kotlin/AGP/Gradle — это,скорее всего, конфликт с версией
Android Studio/JDK на вашей машине, и это нормально поправить точечно после первого лога ошибки.
