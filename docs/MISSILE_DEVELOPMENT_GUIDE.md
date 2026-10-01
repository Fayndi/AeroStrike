# Руководство по архитектуре ракет AeroStrike

Этот документ предназначен для разработчиков команды AeroStrike. Здесь описано устройство базовых классов ракет, чанклоадинга, целеуказания и интеграции с GeckoLib.

---

## 1. Иерархия классов ракет

```text
AbstractCruiseMissileEntity (Базовый класс крылатых ракет)
  ├── Fp5FlamingoEntity      [В разработке вторым разработчиком]
  │     - Тяжелая крылатая ракета наземного старта
  │     - Стартовый бустер РДТТ (40 тиков, угол 40°)
  │     - Маршевый двигатель АИ-25ТЛ, тяжелая БЧ 1150 кг
  │
  └── StormShadowEntity      [Разрабатывается параллельно]
        - Авиационная малозаметная крылатая ракета (Стелс)
        - Воздушный сброс (Air-drop / гравитационное падение без бустера)
        - ТРД Microturbo TRI-60, тандемная проникающая БЧ BROACH
        - Сверхнизкий эшелон огибания (12-15 блоков)
```

---

## 2. Стейт-машина полета (`FlightPhase`)

Каждая ракета проходит через следующие фазы:
1. `STANDBY`: ракета стоит на пусковой направляющей / подвешена на пилоне.
2. `BOOST` (для наземных) / `AIR_DROP` (для авиационных):
   - У Flamingo: 40 тиков на твердотопливном ускорителе с набором высоты.
   - У Storm Shadow: 15 тиков свободного падения, раскрытие крыльев и поджиг турбины.
3. `CRUISE`:
   - Автопилот держит курс на `targetPos`.
   - Рейкасты по вектору скорости (`calculateDesiredCruiseAltitude`): сканируют землю строго под ракетой и препятствия на 30 блоков вперед.
   - При обнаружении преграды рули высоты задираются, после пролета ракета возвращается на эшелон `getCruiseClearance()`.
4. `TERMINAL`:
   - При приближении к цели автопилот переключается в прямое пикирование на максимальной скорости.

---

## 3. Чанклоадинг (`MissileChunkManager`)
Ракета не может упереться в незагруженный чанк благодаря билетам чанков Forge:
- В `tick()` на сервере автоматически вызывается `MissileChunkManager.forceChunk(serverLevel, this)`.
- В `remove(RemovalReason)` при детонации или сбитии билет ОБЯЗАТЕЛЬНО освобождается через `MissileChunkManager.releaseTicket(serverLevel, this)`.

---

## 4. Запись и чтение целевых координат (1.21.1 Ready)
**Запрещено** напрямую обращаться к NBT `stack.getTag()`!
Всегда используйте фасад:
```java
// Записать цель:
ItemDataFacade.setTargetPos(itemStack, blockPos);

// Прочитать цель:
BlockPos target = ItemDataFacade.getTargetPos(itemStack);
```
Это позволит мгновенно обновиться до NeoForge 1.21.1 (где NBT заменен на `DataComponentType`), просто переписав внутренности класса `ItemDataFacade`.

---

## 5. GeckoLib 4.8.4
- Модель: `src/main/resources/assets/aerostrike/geo/entity/<имя>.geo.json`
- Анимации: `src/main/resources/assets/aerostrike/animations/entity/<имя>.animation.json`
- Текстура: `src/main/resources/assets/aerostrike/textures/entity/<имя>.png`
- Регистрация рендерера: строго в `net.strike.aerostrike.client.ClientSetup` через `EntityRenderersEvent.RegisterRenderers`.
