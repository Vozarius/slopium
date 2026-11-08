# Аудит Slopium: Minecraft, Sodium, Create/Flywheel и Nvidium

Дата: 9 ноября 2026. Цель — выявить дублирование, ненужную подготовку данных и обоснованные кандидаты на оптимизацию. Проверены исходники Slopium, NeoForge/Minecraft 1.21.1, Create 6.0.11, байткод и GLSL установленного Sodium 0.8.13/Flywheel 1.0.6, а также приложенные репозитории Sodium и Nvidium. Исходники мода и пользовательские настройки в этом аудите не изменялись. Новых замеров FPS не проводилось.

## 1. Версии и границы выводов

- Установленный Sodium — 0.8.13+mc1.21.1, Flywheel — 1.0.6, Create — 6.0.11. Для вывода «уже реализовано в используемой сборке» проверялись именно эти бинарные артефакты, а не только свежий репозиторий.
- Приложенный Sodium — 0.9.3-beta.1 для Minecraft 26.3. Источник версии: [BuildConfig.kt](<D:/Minecraft files/Zuboglaziki/_test/sodium/buildSrc/src/main/kotlin/BuildConfig.kt:4>). Его RayOcclusionSectionTree — источник идей, но не доказательство наличия того же алгоритма в 0.8.13: [RayOcclusionSectionTree.java](<D:/Minecraft files/Zuboglaziki/_test/sodium/common/src/main/java/net/caffeinemc/mods/sodium/client/render/chunk/occlusion/RayOcclusionSectionTree.java:12>).
- По текущим файлам D:/_projects/nvidium это Nvidium 0.3.0, Fabric, Minecraft 1.21, а не Forge 1.20.1: [gradle.properties](<D:/_projects/nvidium/gradle.properties:6>). В build.gradle используется fabric-loom. Текущая локальная папка оценивалась в этом виде; другие ветки не проверялись.
- Версии, пути и SHA-256 зафиксированы в sources.json. Классы установленных Sodium/Flywheel были локально декомпилированы в build/inspection/audit/source; шейдеры прочитаны непосредственно из JAR. Соседний Create содержит AGENTS.md; его правила чтения/сохранения архитектуры учтены. Изменений в этих репозиториях нет.

## 2. Уже выполненная работа нижних уровней

| Уровень | Что уже имеется | Что это НЕ означает |
|---|---|---|
| Minecraft/NeoForge 1.21.1 | Отбор секций, frustum/дистанция Entity, viewDistance/shouldRender у BER, NeoForge hook видимости BER, очереди частиц по render type и их общий буфер, depth test | Отсутствие пикселей на экране само по себе не отменяет CPU tick и подготовку вершин |
| Sodium 0.8.13 | Section frustum/portal/fog culling, списки BER из RenderSection-кандидатов, section-based Entity culling, буферные арены/staging, multi-draw, быстрый вывод quad particles, анимации только активных текстур | Список кандидатов не является точной картой видимых пикселей; обычное стекло не обязано отсекать находящийся за ним мир |
| Create 6.0.11 | CachedRenderBBBlockEntity, CachedBuffers для моделей, кеш path/source у фабричных соединений, lazy ticks, прекращение обычного kinetic-render при активной visualization | Целый client tick безопасно выключать нельзя для любого блока без проверки поведения |
| Flywheel 1.0.6 | Instance batching, mesh pool, dirty spans, планы и worker executor; indirect backend использует sphere frustum + depth pyramid на GPU | Это не отменяет client BE tick и не гарантирует отсутствие затрат на CPU visual updates |
| Nvidium из приложенной папки | Другой terrain backend с собственными регионами/секциями/геометрией в VRAM, GPU-командами, raster visibility и temporal recovery | Это не готовый ускоритель Create tick или всех Block Entity |

Проверяемые места: [ParticleEngine.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/build/inspection/ParticleEngine.java:451>); [OcclusionCuller.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/build/inspection/audit/source/net/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller.java:32>); [SodiumWorldRenderer.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/build/inspection/audit/source/net/caffeinemc/mods/sodium/client/render/SodiumWorldRenderer.java:357>); [cull.glsl](<D:/Minecraft files/Zuboglaziki/_test/slopium/build/inspection/audit/resources/assets/flywheel/flywheel/internal/indirect/cull.glsl:68>); [Storage.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/build/inspection/audit/source/dev/engine_room/flywheel/impl/visualization/storage/Storage.java:39>); [InstancedInstancer.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/build/inspection/audit/source/dev/engine_room/flywheel/backend/engine/instancing/InstancedInstancer.java:43>).

Важная точность: установленный SodiumWorldRenderer.renderBlockEntity сам не содержит дополнительного точного frustum-теста каждого BER AABB — он приходит к BER через список секций. В NeoForge LevelRenderer без Sodium такой hook присутствует. Поэтому дополнительный per-BER тест внутри частично видимой секции не всегда дублирует эквивалентную проверку Sodium. Решение нужно принимать по конкретному пути, а не удалять все AABB проверки подряд.

ParticleRangeMixin уже пропускает собственный frustum, когда передан nativeFrustum: [ParticleRangeMixin.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/mixin/ParticleRangeMixin.java:22>). Эту защиту нельзя объявить безусловным дублированием. Обычный depth test скрывает пиксели за непрозрачной поверхностью, но не освобождает CPU от обхода частиц и записи вершин. «Один проход на все частицы» уже частично реализован штатным batching по render type; объединение разных blend/depth/texture режимов в один draw требует другого рендера и не является простым общим пулом.

## 3. Подтверждённые проблемы самого Slopium

### F01. Четыре фильтра выбранных блоков в одной цепочке tick

Путь BoundTickingBlockEntityMixin → CreateWallControl.skipTick → hidden/decision → outsideCamera содержит до четырёх ProcessedBlocks.allows для одного BE. Каждый вызывает getBlockState, получение registry ID и чтение таблицы. Даже когда все функции изменения тиков выключены, ранняя проверка фильтра остаётся после enabled: [BoundTickingBlockEntityMixin.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/mixin/BoundTickingBlockEntityMixin.java:32>), [CreateWallControl.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/CreateWallControl.java:33>), [CreateWallControl.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/CreateWallControl.java:26>).

Решение: в начале вычислять «нужна ли вообще эта подсистема»; кешировать eligibility в привязанном ticker/visual state с revision фильтра; проверять его один раз. Последующие функции получают уже классифицированный контекст. Это конкретное устранение повторов, а не добавление новой проверки.

### F02. Бюджет лучей не ограничивает стоимость читателей

750 мкс в VisibilityPool.process относится к очереди трассировок. Дорогая цепочка подготовки перед вызовом process находится вне этого бюджета: renderer lookup, получение/inflate AABB, Vec3, Sable guard, новый Key и HashMap lookup. Key создаётся на каждый запрос, даже при попадании в кеш: [VisibilityPool.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/VisibilityPool.java:94>); [CreateWallControl.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/CreateWallControl.java:26>).

captureCamera увеличивает cameraRevision и создаёт Frustum на каждом новом кадре даже при неподвижных матрицах. Поэтому per-entry frustum cache живёт в основном внутри кадра; стационарная сцена проверяется заново: [VisibilityPool.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/VisibilityPool.java:63>). Нужен отдельный camera-seen-frame и revision только при изменении матриц/глаз; изменяющиеся bounds всё равно должны инвалидировать результат.

Подтверждение приоритета: Spark текущей сцены отдаёт CreateWallControl.skipTick около 23.7% inclusive выборок Render thread; self в dev.slopium около 20.8%, при отключённой проверке тиков — около 4%. Эти вложенные доли не суммируются и не являются обещанием процента FPS.

### F03. Выключенный Slopium не полностью возвращает исходный путь

FlywheelOwnershipMixin без проверки enabled/необходимости всегда создаёт OwnedInstances, оборачивает VisualizationContext, каждый Instancer и создание/перенос Instance. Есть ConcurrentHashMap и очереди. Это происходит и в режиме «только генерация кадров»: [FlywheelOwnershipMixin.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/mixin/FlywheelOwnershipMixin.java:13>); [OwnedInstances.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/OwnedInstances.java:20>).

VisualUpdatePolicy в общем worker-пути делает lookup владельца, unwrap до восьми уровней, фильтрацию и чтение состояния. При visibleSections=true он проходит часть этой подготовки даже когда конечные флаги обновления visual ничего не запрещают: [VisualUpdatePolicy.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/VisualUpdatePolicy.java:7>).

Решение: ownership нужен только выбранным функциям, с ленивой активацией и корректным восстановлением; bind metadata один раз при lifecycle событии, worker читает опубликованный флаг/прямую ссылку. При включении после загрузки мира нужен контролируемый lifecycle/recreate, а не отсутствие данных или новый полный обход всех блоков мира.

### F04. Обычные BER тоже проходят лишний фильтр в неактивных режимах

В SodiumBlockEntityBudgetMixin проверка ProcessedBlocks.allows стоит до проверки, включены ли hide/frustum/wall функции. При enabled=true фильтр работает даже когда никакое отсечение не запрошено: [SodiumBlockEntityBudgetMixin.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/mixin/SodiumBlockEntityBudgetMixin.java:27>). В generic dispatcher повторяется часть hide-политики. Последний hook полезен как fallback для путей, обходящих Sodium; можно убирать повтор внутри уже обработанного пути, но не удалять fallback вслепую.

### F05. Заявленный GPU-путь не совпадает с действующим

FlywheelCullingIndex считает gpuModels=true при GpuSections.requested, а GpuSections.bindFlywheel требует ProcessedBlocks.all() и актуальный совместимый контекст: [FlywheelCullingIndex.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/FlywheelCullingIndex.java:105>); [GpuSections.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/GpuSections.java:197>). BLACKLIST запрещает GPU-маску, но предыдущая проверка уже может отключить CPU путь. Это согласуется со счётчиком Flywheel=0.

Нужен единый resolved capability/policy: requested, supported, current mask и backend — разные состояния. Выбор ALL нельзя навязать вместо пользовательского фильтра. Для избирательной GPU-маски потребуется информация eligibility на уровне owner/instance, а не только ID секции. Instance с embedded matrix и крупные модели должны оставаться видимыми, если привязка к проверяемой секции не доказана.

### F06. Дополнительный terrain depth pass не доказал окупаемость

GpuSections каждый кадр заново собирает candidate mapping/таблицы, выделяет int[] hash table, загружает bounds/keys/mask через glBufferData, делает depth/ID проход и compute: [GpuSections.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/GpuSections.java:122>). Затем адаптирует команды из Sodium, сохраняет/восстанавливает множество GL состояний и выполняет indirect: [GpuSections.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/visibility/GpuSections.java:160>).

Сам этот объём не становится бесплатным от размещения на GPU. Для opaque Flywheel уже существует native HiZ. Отдельная политика ANY для стекла — дополнительная визуальная функция, а не дубль opaque проверки. В старом Beta14 isolated A/B общий результат был медленнее; пользовательские отчёты не дают доказанного чистого выигрыша section pass. До новой реализации — выключенный эксперимент, не основа рекомендуемого пресета.

### F07. Остановка вращения в shader не означает экономию draw

Create rotating.vert вычисляет угол из flw_renderSeconds*speed: [rotating.vert](<D:/Minecraft files/Zuboglaziki/_test/Create/src/main/resources/assets/create/flywheel/instance/rotating.vert:4>). Slopium выставляет speed=0/offset и помечает instance changed; количество draw/вершин и структура шейдера сохраняются. Обычный SingleAxisRotatingVisual — SimpleTickableVisual и не имеет beginFrame, который можно было бы удешевить переключателем дальних per-frame updates: [SingleAxisRotatingVisual.java](<D:/Minecraft files/Zuboglaziki/_test/Create/src/main/java/com/simibubi/create/content/kinetics/base/SingleAxisRotatingVisual.java:73>).

Нужно различать: (1) shader motion, (2) CPU visual update, (3) client BE tick, (4) видимость draw. Полная «остановка анимации» полезна для диагностики, но как общий performance-тумблер вводит в заблуждение. Оптимизировать actual CPU update стоит по типу visual и по изменению данных.

### F08. Два слоя кеша света частиц

Per-particle кеш действует целый game tick и не учитывает geometry/config epoch в пределах тика; общий block-light кеш действует на кадр/epoch/config. Это разная свежесть данных и два дополнительных пути чтения: [ParticleLightCacheMixin.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/mixin/ParticleLightCacheMixin.java:49>). Достаточность и окупаемость определяются hit rate/ценой чтения света. Для строгого сохранения картинки логичнее один корректно инвалидируемый слой; нельзя обещать пользу при уже малом числе частиц.

### F09. Реальные дубли UI и семантически разные quality cuts

Отдельное выключение entity shadows дублирует штатный параметр vanilla. Три старых поля throttleFlywheelDistance/flywheelFullRateDistance/flywheelFrameInterval мигрируют в createVisuals, но legacy runtime ветка остаётся. Её можно убрать после миграции, оставив чтение старых конфигов.

Напротив, texture interpolation off не эквивалентен Sodium animate-only-visible: Sodium уже экономит на невидимых текстурах, Slopium отказывается от интерполяции видимых. Радиусы деталей, одна копия выпавшего стака, impostor и particle budget тоже добавляют собственный компромисс качества. Их нельзя классифицировать как бесплатное ускорение, но и нельзя выкидывать лишь потому, что в сцене без выпавших предметов они дали 0%.

## 4. Дополнительные оптимизации, которых действительно стоит проверить

### N01. Create: не помечать неизменившийся цвет вращающейся модели dirty

Установленный Create 6.0.11 в KineticBlockEntityVisual.applyOverstressEffect безусловно вызывает setChanged для каждого RotatingInstance даже при нулевом overStressedEffect и уже белом цвете. Это проверено и в исходнике, и javap байткоде: [KineticBlockEntityVisual.java](<D:/Minecraft files/Zuboglaziki/_test/Create/src/main/java/com/simibubi/create/content/kinetics/base/KineticBlockEntityVisual.java:43>). SingleAxisRotatingVisual вызывает это каждый tick: [SingleAxisRotatingVisual.java](<D:/Minecraft files/Zuboglaziki/_test/Create/src/main/java/com/simibubi/create/content/kinetics/base/SingleAxisRotatingVisual.java:73>).

Flywheel уже умеет загружать только changed spans: [InstancedInstancer.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/build/inspection/audit/source/dev/engine_room/flywheel/backend/engine/instancing/InstancedInstancer.java:43>). Принудительная маркировка каждый tick мешает этой экономии. При большой массе статичных по данным валов возможны повторные записи/передачи одинакового instance payload; вращение на GPU при этом не требует новой CPU-записи угла.

Кандидат: сравнивать действительные packed RGB/состояние перед setChanged либо пропускать только подтверждённый no-op участок для белого неизменившегося состояния. Не выключать весь tick визуала и не терять обновления скорости, света, цвета, стресс-эффекта. Проверки: появление/затухание overload, смена RPM/оси, ресурсный reload, Sable/Azimuth и другие модификаторы instance. Доказано наличие лишней маркировки; FPS-выигрыш ещё не измерен.

### N02. Удалить постоянную цену подготовки, а не только число лучей

Для обычных BE хранить прямой state: immutable позиция/ID секции, eligibility+revision, flags видимости, срок/причина неопределённости. Tick читает готовые flags; один процессор обновляет выбранный небольшой набор. Для worker visual — прямой опубликованный state после регистрации. Это устраняет Key/HashMap/renderer lookup в наиболее горячем месте.

Кешировать отбор и координаты можно без потери картинки, если предусмотрена инвалидация на замену BE/состояния/фильтра/перемещение подмира. Результат видимости не хранится вечно: unknown/изменённая геометрия должны сохранять обработку, а не произвольно скрывать.

### N03. Event/dirty-driven CPU visuals вместо «каждый N-й кадр для всех»

Использовать штатные Flywheel планы и lifecycle, сокращать обновления дорогих visual по фактической смене состояния. Не добавлять второй пул потоков: Flywheel уже выполняет задачи параллельно. Обновления света и состояния не следует смешивать с декоративным beginFrame; своевременное восстановление при появлении обязательно.

### N04. Фабричные панели: batch деталей и один расчёт LOD

В папке factoryBoard нет собственного FactoryPanelVisual; FactoryPanelRenderer выполняет обычный CPU renderBulb/renderPath, FilteringRenderer — предметы. Поэтому это разумная отдельная цель. Но FactoryPanelConnection.getPath и cachedSource уже кешируются: [FactoryPanelConnection.java](<D:/Minecraft files/Zuboglaziki/_test/Create/src/main/java/com/simibubi/create/content/logistics/factoryBoard/FactoryPanelConnection.java:51>).

Сначала один выбор детализации/дистанции на панель на кадр вместо отдельных повторов на каждый connection/bulb/item. Далее прототип инстансирования повторяемых соединительных элементов/ламп с параметрами яркости в GPU; предметы требуют самостоятельной стратегии и корректности custom item renderer. Не строить ещё один кеш пути. Сроки/выгода без измерения не заявляются.

### N05. Адаптивный индекс динамического света

Уже существующий [light-grid-benchmark.json](<D:/Minecraft files/Zuboglaziki/_test/slopium/analysis/light-grid-benchmark.json>) — synthetic, не FPS-тест. Для 16 spread источников index build+queries 0.1133 мс против linear 0.1015 мс; для 2048 spread — 1.4173 против 11.7915 мс. Поэтому нельзя постоянно платить за построение индекса при нескольких источниках. Автоматический выбор по количеству источников/запросов и переиспользование при стабильном источнике — обоснованный кандидат. Нулевой snapshot уже имеет быстрый ответ 0; эту оптимизацию добавлять повторно не нужно: [SodiumDynamicLightsMixin.java](<D:/Minecraft files/Zuboglaziki/_test/slopium/src/main/java/dev/slopium/mixin/SodiumDynamicLightsMixin.java:48>).

### N06. Условное устранение backend-specific upload allocation

InstancedInstancer.writeChanged выделяет и освобождает временный native MemoryBlock для dirty span. При активном instancing backend и подтверждённых затратах можно проверить reuse staging/arena. IndirectDrawManager уже имеет StagingBuffer; второй универсальный upload manager дублировал бы существующий. Начинать с устранения ложного dirty N01, потому что это сокращает и записи, и allocations, и uploads вместе.

## 5. Что реально взять из Nvidium

[Nvidium.java](<D:/_projects/nvidium/src/main/java/me/cortex/nvidium/Nvidium.java:29>) проверяет GL_NV_mesh_shader, unified-memory, representative-fragment-test, sparse-buffer и bindless-multi-draw-indirect. Это собственный backend с другими зависимостями, layout геометрии и shader pipeline; переименование Fabric в NeoForge не переносит его автоматически.

| Механизм | Действие для Slopium | Повтор/ограничение |
|---|---|---|
| Стабильные section/region ID, обновления по lifecycle | Применить при реорганизации GpuSections вместо сборки HashMap+hash table каждый кадр | Брать идентичности секций Sodium, работать только с его текущими draw-кандидатами; не сканировать блоки мира |
| Геометрия и компактные команды постоянно в VRAM | Прототипировать только при доказанном terrain/draw-submit bottleneck | Sodium/Flywheel уже имеют GPU arenas/staging; сначала использовать их |
| GPU visibility → indirect commands | Оставить данные рендера на GPU, связывать native instance metadata с eligibility | У Flywheel indirect уже имеется этот принцип; новая маска оправдана другой политикой/гранулярностью |
| Temporal primary pass + recovery newly visible | Возможный способ избежать полного дополнительного depth-прохода | Нужен recovery текущего кадра и инвалидация геометрии/камеры; голая маска прошлого кадра вызывает пропадания |
| Ring/fence upload staging | Переиспользовать подход, если существующий backend действительно недостаёт | Не копировать fallback glFinish при переполнении; bounded growth/defer и замеры |
| Отложенное чтение результатов CPU | Только для статистики или отдельного консервативного кеша CPU-потребителей | Current-frame GPU маска не может бесплатно управлять CPU tick, который уже произошёл до рендера |
| NV mesh/task/bindless shader backend целиком | Отдельный большой прототип, не ближайшая оптимизация Create | NVIDIA extensions, свой формат буферов, совместимость Sodium/Veil/подмиров/translucency; не решение дорогого skipTick |

Конкретные места: stable section2id — [SectionManager.java](<D:/_projects/nvidium/src/main/java/me/cortex/nvidium/managers/SectionManager.java:29>); render sequence — [RenderPipeline.java](<D:/_projects/nvidium/src/main/java/me/cortex/nvidium/RenderPipeline.java:162>); newly-visible temporal path — [temporal_task.glsl](<D:/_projects/nvidium/src/main/resources/assets/nvidium/shaders/terrain/temporal_task.glsl:23>). В UploadingBufferStream при исчерпании арены есть glFinish: [UploadingBufferStream.java](<D:/_projects/nvidium/src/main/java/me/cortex/nvidium/util/UploadingBufferStream.java:63>). Его нельзя переносить как «заведомо неблокирующий» код. DownloadTaskStream использует кольцо с задержкой; для собственной CPU-публикации нужно проверять готовность результата, а не считать фиксированное число кадров гарантией завершения GPU.

Поскольку CPU тик идёт до GPU draw, общая логическая политика не означает один физический буфер, читаемый CPU и GPU текущего кадра без обмена. Render consumer использует текущую GPU mask, CPU consumer — дешёвый опубликованный консервативный snapshot с явно заданной свежестью. Синхронного чтения GPU/ожидания ради пропуска тика быть не должно.

Лицензии в локальных папках зафиксированы: Nvidium — LGPL-3.0, приложенный Sodium — PolyForm Shield, Slopium root — MIT. В этом аудите код из этих проектов в Slopium не копировался. Решение о прямом заимствовании конкретных файлов должно учитывать их собственные условия и заголовки; идеи архитектуры и копирование файлов — разные объекты работы.

## 6. Что оставить в интерфейсе

Полная матрица всех 65 элементов: [options.ru.md](<D:/Minecraft files/Zuboglaziki/_test/slopium/analysis/audit-20261109/options.ru.md>).

- Основные возможности: автоматические узкие оптимизации с дешёвым выключенным путём; независимая генерация; явные бюджеты/дальность деталей как качество.
- Предпочтительный режим Create: сначала оптимизация неизменившихся данных и дорогих CPU visual updates; изменения целого client tick — продвинутый режим с проверяемой областью действия.
- Из обычных «ускорений» убрать standalone выключение теней, повторные frustum controls поверх native backend, shader freeze как обещание экономии, служебные счётчики и полные отключения графики. Для диагностики полные отключения полезны и остаются вне обычного пресета.
- GPU sections/ANY obstacle occlusion оставить выключенным экспериментом до окупаемого A/B; технические gpuGlass/visibleSections capability switches свести к одной понятной политике с фактическим backend/status.
- FG debug/сетка/поиск/refinement — расширенные параметры/пресеты, не отдельные рекомендации по ускорению мира. Последовательный путь сохранить как совместимость, пока не принято решение о поддерживаемых платформах.

Не все параметры должны быть доступны как независимые boolean. Один режим качества и один набор применимых параметров уменьшают количество противоречащих комбинаций. Поля старого JSON требуют миграции, чтобы очистка меню не включила скрытые объекты неожиданно.

## 7. Критерии «оставить только то, что помогает»

Функция принимается по сценариям, где она активна, а не по одному заводскому скриншоту: закрытая фабрика/открытая фабрика, плотная стена панелей, масса частиц, россыпь предметов, terrain при разных дальностях. Фиксируются flags, число реально пропущенных действий, CPU/GPU ms, объём instance upload, allocation rate, настоящие/выходные FPS и изменения картинки. Нулевой активный счётчик означает «не проверяли эту функцию», а не «бесполезна».

Перед измерением исправить ZGC cycle-as-pause и отделить внешнюю нагрузку от последствий самой настройки: уменьшение rebuild/GC/dirty uploads может быть полезным результатом оптимизации. Если A/B сохраняет требуемую картинку и стабильно уменьшает время при разумных расходах/совместимости — оставляем. Если включение лишь добавляет проверку без экономии, удаляем или выбираем native path. Quality cuts явно называются quality, diagnostic cuts не выдаются за оптимизацию с той же картинкой.

Первый практический прототип: zero-cost disabled path + N01 no-op dirty suppression + прямой eligibility state вместо F01/F02. Это меньше по объёму и ближе к текущим подтверждённым CPU-проблемам, чем перенос всего terrain backend.
