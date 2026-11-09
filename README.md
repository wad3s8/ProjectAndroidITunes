# iTunes Каталог — практика «Работа с сетью»

Приложение на Jetpack Compose: поиск песен и отдельный запрос деталей по ID.

## Архитектура

- domain: Track, контракт TrackRepository, SearchTracksUseCase, GetTrackUseCase, ошибки.
- data: Retrofit ItunesApi, DTO и преобразование в доменную модель, NetworkTrackRepository.
- ui: ViewModel управляют запросами через use case в viewModelScope; экраны наблюдают StateFlow.
- AppContainer: создание и связывание зависимостей.

Retrofit 3.0.0 + OkHttp, suspend-запросы и Dispatchers.IO. Отмена корутин сохраняется.
Поиск: GET /search?term=...&media=music&entity=song&limit=25&country=US.
Детали: GET /lookup?id=...&country=US.
Поиск отправляется по кнопке; пустой ввод не вызывает сеть. Начальный запрос — Daft Punk.
Есть загрузка, пустой результат, отсутствие трека, ошибки сети/сервера/лимита/ответа и повтор.
Навигация передаёт только ID. Поисковый текст и последний отправленный запрос сохраняются через SavedStateHandle.
Обложки остаются декоративными; данные треков и цены приходят из API.

Документация API: https://performance-partners.apple.com/search-api

## Проверки

    ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest

Unit-тесты проверяют состояния ViewModel, отмену поиска, повтор, параметры HTTP,
маппинг ответа, lookup, ошибки сервера и некорректный JSON.
UI-тесты используют отдельный тестовый репозиторий и не зависят от доступности iTunes.

## Демонстрация для сдачи

1. Запустить debug-приложение на устройстве/эмуляторе с интернетом и включить запись экрана.
2. Показать загрузку и список; ввести другого исполнителя и нажать «Найти».
3. Открыть трек и показать детали, вернуться назад.
4. Отключить Wi-Fi и мобильную сеть на устройстве (у эмулятора отключить именно доступ в интернет).
5. Нажать «Найти»: показать загрузку, затем сообщение ошибки (таймаут до 30 секунд).
6. Включить интернет, нажать «Повторить» и показать успешный результат.
7. В Android Studio открыть View → Tool Windows → App Inspection → Network Inspector,
   выбрать процесс com.example.projectandroid. Выполнить поиск и открыть детали.
   Сделать скриншот запросов search/lookup с URL, параметрами и кодом ответа.

Запись и скриншот нужно получить с реального запуска; тесты их не заменяют.


### Проверка в Windows

Если JVM выдаёт ClassNotFoundException для тестов из папки с кириллицей, запустите
Gradle через свободную букву диска (пример для P:):

    subst P: "полный путь к ProjectAndroidITunes"
    P:
    .\gradlew.bat :app:testDebugUnitTest --no-configuration-cache "-Pkotlin.incremental=false"

После работы перейдите обратно на C: и удалите только сопоставление командой
`subst P: /D`. Файлы проекта при этом не удаляются.
