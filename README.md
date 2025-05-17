# CryptoCraft — Плагин для криптовалют в Minecraft

## Описание

CryptoCraft — плагин для сервера Minecraft, который позволяет отслеживать курсы криптовалют, хранить баланс игроков и совершать покупки/продажи через удобный GUI.

---

## Команды

### `/crypto`

Открывает главное меню плагина с текущими курсами криптовалют и балансом игрока.

- Доступно только игрокам.
- Через меню можно выбрать покупку или продажу криптовалют.
- Поддерживает отображение текущих курсов в реальном времени.

---

## Конфигурация (config.yml)

```yaml
currency: DIAMOND # Материал для отображения валюты в GUI (например, DIAMOND, GOLD_INGOT)

cryptocurrencies:  # Список криптовалют
  - id: bitcoin          # ID валюты (используется в API и в плагине)
    name: BTC            # Сокращённое название (отображается в GUI и actionbar)
    display_item: GOLD_INGOT # Материал для отображения валюты в GUI
  - id: ethereum
    name: ETH
    display_item: EMERALD
  - id: the-open-network
    name: TON
    display_item: DIAMOND
```
currency — валюта для оплаты (материал из Minecraft).

В cryptocurrencies каждый элемент описывает криптовалюту:

id — идентификатор для API.

name — сокращённое имя для отображения.

display_item — предмет, используемый в GUI.

Использование
Запустите сервер с плагином.

Выполните команду /crypto для открытия главного меню.

В меню выберите действие — купить или продать криптовалюту.

Выберите нужную криптовалюту.

Выберите количество для операции.

Подтвердите операцию или вернитесь назад.

Запуск обновлений курса и отображения
Обновление курса из API происходит раз в 60 секунд (асинхронно).

Отображение курса и баланса в ActionBar происходит каждую секунду.

Это реализовано с помощью двух параллельных задач в плагине.

GUI
Главное меню — показывает список криптовалют с текущими курсами, балансами и эквивалентами в валюте.

Меню выбора криптовалюты — позволяет выбрать валюту для операции.

Меню выбора количества — позволяет выбрать количество для покупки или продажи с кнопками +1, +10, +64 и их аналогами для уменьшения.

Пример команды и вызова
В Main классе плагина:

java
Копировать
Редактировать
@Override
public void onEnable() {
    saveDefaultConfig();
    createPlayerDataConfig();

    cryptoAPI = new CryptoAPI(this);
    getCommand("crypto").setExecutor(new CryptoCommand(this, cryptoAPI));
    getServer().getPluginManager().registerEvents(new CryptoGUI(this), this);

    CryptoPriceUpdater updater = new CryptoPriceUpdater(this, cryptoAPI);
    Bukkit.getScheduler().runTaskTimerAsynchronously(this, updater, 0L, 1200L);

    getLogger().info("CryptoCraft плагин включен!");
}
Спасибо за использование CryptoCraft!
