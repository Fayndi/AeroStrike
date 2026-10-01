# AeroStrike (Minecraft 1.20.1 Forge)

Масштабный военно-технический мод для Minecraft 1.20.1 (Forge MDK 47.4.10) с прицелом на Dedicated серверы и последующий порт на 1.21.1 NeoForge.

---

## 🚀 Основные модули и функционал
1. **Крылатые ракеты**: низковысотный полет, огибание рельефа местности, следование координатным путевым точкам (GPS/Waypoint).
2. **Баллистические ракеты**: расчет параболических суборбитальных траекторий, разделение ступеней и боеголовок.
3. **Планирующие авиабомбы (ФАБ) и Оптимизированные взрывы**: кастомный движок кратеров с батчингом тиков (отсутствие спайков TPS при взрывах тяжелых боеголовок).
4. **Дроны**: разведывательные и FPV-дроны камикадзе с видом от первого лица и управлением через планшет.
5. **Комплексы ПВО и Радары**: радиолокационные станции с обнаружением воздушных целей в радиусе и автоматическим перехватом зенитными ракетами (SAM).

---

## 🛠 Архитектурные принципы
- **Strict Server/Client Separation**: строгая изоляция клиентского кода (`net.strike.aerostrike.client.*`) от серверного. Безопасная работа на выделенных серверах (Dedicated Server).
- **Chunkloading Manager**: управляемый чанклоадинг для высокоскоростных ракет (`ServerLevel.getChunkSource().addRegionTicket`) с гарантированным освобождением тикетов при детонации/удалении сущности.
- **NeoForge 1.21.1 Ready**:
  - Данные предметов изолированы через слой фасадов (`ItemDataFacade`), что позволит мгновенно перейти с NBT на `DataComponentType`.
  - Сетевые пакеты (`net.strike.aerostrike.common.network`) отделены от контекста `NetworkEvent.Context`.
- **GeckoLib 4.8.4**: анимации и геометрия 3D-моделей техники.
- **Mixin Ready**: базовая конфигурация Mixin (`aerostrike.mixins.json`) для кастомных манипуляций камерой дронов и рендером.
- **Developer Guide**: полное руководство по созданию ракет и работе с физикой находится в [`docs/MISSILE_DEVELOPMENT_GUIDE.md`](docs/MISSILE_DEVELOPMENT_GUIDE.md).

---

## 🎯 Текущий статус разработки ракет
- **FP-5 «Flamingo»** (`Fp5FlamingoEntity`): тяжелая крылатая ракета наземного базирования (находится в доработке вторым разработчиком).
- **Storm Shadow / SCALP-EG** (`StormShadowEntity`): авиационная малозаметная крылатая ракета со стелс-профилем и воздушным пуском (в активной разработке).

---

## 👥 Совместная работа через GitHub

### 1. Первичное подключение репозитория к GitHub:
Если репозиторий на GitHub еще не создан:
```bash
# Авторизация в GitHub CLI
gh auth login

# Создание удаленного репозитория и первый push
gh repo create AeroStrike --public --source=. --remote=origin --push
```
Или вручную через сайт github.com:
```bash
git remote add origin https://github.com/<ваш-логин>/AeroStrike.git
git branch -M main
git push -u origin main
```

### 2. Как подключиться другу:
1. Добавьте друга в репозиторий: **Settings -> Collaborators -> Add people**.
2. Друг клонирует репозиторий:
   ```bash
   git clone https://github.com/<ваш-логин>/AeroStrike.git
   ```
3. Открывает проект в **IntelliJ IDEA** (Open -> выбрать `build.gradle` -> Open as Project).

---

## 💻 Сборка и запуск в среде разработки

### Запуск клиента (Singleplayer / LAN):
```bash
./gradlew runClient
```

### Запуск выделенного сервера (Dedicated Server):
```bash
./gradlew runServer
```

### Сборка JAR-мода:
```bash
./gradlew build
```
Готовый jar-файл появится в папке `build/libs/`.
