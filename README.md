# 🇪🇳 Description

BFBaffovar — a convenient and flexible plugin for combining potions into one powerful potion ⚗️

With this plugin, players can combine up to 7 potions into a single one, creating custom effects and enhancing gameplay. The plugin is fully customizable and suitable for any type of server — from RPG to PvP.

## 🔧 Main Features:

- The plugin is supported up to Java version 26.
- Combine up to 7 potions into one
- Fully customizable GUI menu
- All messages can be edited in config.yml
  
- Support for:

- EssentialsX Economy

- Vault

- PlaceholderAPI

- Permissions system

* HEX color support (&#RRGGBB) and standard color codes

## 💰 Economy:

1. Two pricing modes:
2. Fixed price
3. Price per potion
4. Ability to make combining free via permission

## 🛡️ Reliability:

* Protection against item duplication, anti-button spam, potion return when: menu is closed, player disconnects
## 📜 Commands:

* /bfbaffovar (or /bfb) — open menu | bfbaffovar.use
* /bfbaffovar reload — reload config | bfbaffovar.reload


## 🪛 Todo:

- ~~Support for PlayerPoints~~
- Debug messages
- Logs



# 🇷🇺 Описание

BFBaffovar — это удобный и гибкий плагин для объединения зелий в одно мощное зелье ⚗️


С его помощью игроки могут объединить до 7 зелий в одно, создавая кастомные эффекты и усиливая игровой процесс. Плагин полностью настраиваемый и подходит для любых типов серверов — от RPG до PvP.

## 🔧 Основные возможности:

- Плагин поддерживается до 26 версии Java
  
- Объединение до 7 зелий в одно
  
- Полностью настраиваемая GUI-менюшка
  
- Все сообщения редактируются в config.yml
  
- Поддержка:
- EssentialsX Economy
- Vault
- PlaceholderAPI
- Система прав (Permissions)
- Поддержка HEX-цветов (&#RRGGBB) и стандартных цветовых кодов


## 📟 Русский конфиг:

<details>
<summary>Spoiler</summary>


```
# ============================================================
#   BFBaffovar - Конфигурация плагина
#   Поддержка: &коды_цветов и &#RRGGBB HEX цвета
# ============================================================

debug:
  # Выводить debug сообщения в консоль (открытие/закрытие GUI, объединения, ошибки)
  console: true
  # Сохранять историю объединений в plugins/BFBaffovar/database.log
  database: true

economy:
  # Провайдер экономики: VAULT или PLAYERPOINTS
  # Если выбранный плагин не найден — автоматически переключится на второй
  provider: VAULT
  enabled: true
  # Режим цены: FIXED (фиксированная) или PER_POTION (цена × количество)
  pricing-mode: PER_POTION
  cost: 50.0
  # Название валюты для PlayerPoints (у Vault берётся автоматически)
  playerpoints-currency-name: "очков"

merge:
  # Максимум зелий за одно объединение (не более 7)
  max-potions: 7
  # Разрешать объединение только зелий одного типа и уровня
  require-same-type: true

permissions:
  use: "bfbaffovar.use"
  reload: "bfbaffovar.reload"
  free: "bfbaffovar.free"

sounds:
  open-gui: "BLOCK_CHEST_OPEN"
  merge-success: "ENTITY_PLAYER_LEVELUP"
  merge-fail: "ENTITY_VILLAGER_NO"
  no-money: "BLOCK_NOTE_BLOCK_BASS"

gui:
  # Размер инвентаря — кратно 9 (9, 18, 27, 36, 45, 54)
  size: 27
  title: "&#DDDDDD Объединение зелий"

  # Слоты для зелий
  potion-slots: [1, 2, 3, 4, 5, 6, 7]

  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    name: " "
    slots: [0, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26]

  merge-button:
    slot: 22
    material: "LIME_STAINED_GLASS_PANE"
    name: "&#7EFF9F Объединить"
    lore:
      - "&#DDDDDD Положите зелья в слоты выше"
      - ""
      - "&#DDDDDD Стоимость: &#FFCE69{cost} {currency}"
      - "&#DDDDDD Баланс: &#FFCE69{balance} {currency}"

  clear-button:
    slot: 18
    material: "RED_STAINED_GLASS_PANE"
    name: "&#FF6969 Вернуть всё"
    lore:
      - "&#DDDDDD Возвращает зелья в инвентарь"

  info-button:
    slot: 26
    material: "WHITE_STAINED_GLASS_PANE"
    name: "&#DDDDDD Инфо"
    lore:
      - "&#DDDDDD Макс. зелий: &#FFCE69{max}"
      - "&#DDDDDD Стоимость: &#FFCE69{cost} {currency}"

messages:
  prefix: "&#DDDDDD » &r"
  no-permission: "{prefix}&#FF6969Недостаточно прав."
  vault-not-found: "{prefix}&#FF6969Плагин экономики не найден!"
  reload-success: "{prefix}&#7EFF9FКонфиг перезагружен."
  no-potions: "{prefix}&#FF6969Положите хотя бы одно зелье."
  not-a-potion: "{prefix}&#FF6969Сюда можно класть только зелья!"
  not-same-type: "{prefix}&#FF6969Все зелья должны быть одного типа и уровня."
  not-enough-money: "{prefix}&#FF6969Нужно &#FFCE69{cost} {currency}&#FF6969, у вас &#FFCE69{balance}&#FF6969."
  merge-success: "{prefix}&#7EFF9FОбъединено &#FFCE69{count} &#7EFF9Fзелий за &#FFCE69{cost} {currency}&#7EFF9F."
  merge-free: "{prefix}&#7EFF9FОбъединено &#FFCE69{count} &#7EFF9Fзелий бесплатно."
  inventory-full: "{prefix}&#FF6969Инвентарь полон! Предметы выброшены рядом."
  invalid-command: "{prefix}&#DDDDDDИспользование: /bfbaffovar [reload]"
```


</details>



  
## 💰 Экономика:
1. Два режима оплаты:
1. Фиксированная цена
1. Цена за каждое зелье
1. Возможность сделать объединение бесплатным через permission


## 🛡️ Надёжность:
- Защита от дюпа предметов, анти-спам кнопок, возврат зелий при: закрытии меню, выходе игрока


## 📜 Команды:


- /bfbaffovar (или /bfb) — открыть меню | bfbaffovar.use
- /bfbaffovar reload — перезагрузка конфига | bfbaffovar.reload


## 🪛 В будущем:

- ~~Поддержка плагина PlayerPoints~~
- Debug сообщения
- Логи
