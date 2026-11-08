![LunarCore](https://socialify.git.ci/Melledy/LunarCore/image?description=1&descriptionEditable=A%20game%20server%20reimplementation%20for%20version%202.2.0%20of%20a%20certain%20turn-based%20anime%20game%20for%20educational%20purposes.%20&font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pulls=1&stargazers=1&theme=Light)
<div align="center"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/Melledy/LunarCore?logo=java&style=for-the-badge"> <img alt="GitHub" src="https://img.shields.io/github/license/Melledy/LunarCore?style=for-the-badge"> <img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/Melledy/LunarCore?style=for-the-badge"> <img alt="GitHub Workflow Status" src="https://img.shields.io/github/actions/workflow/status/Melledy/LunarCore/build.yml?branch=development&logo=github&style=for-the-badge"></div>

<div align="center"><a href="https://discord.gg/cfPKJ6N5hw"><img alt="Discord - LunarCore" src="https://img.shields.io/discord/1163718404067303444?label=Discord&logo=discord&style=for-the-badge"></a></div>

[EN](../README.md) | [简中](README_zh-CN.md) | [繁中](README_zh-TW.md) | [JP](README_ja-JP.md) | [RU](README_ru-RU.md) | [FR](README_fr-FR.md) | [KR](README_ko-KR.md) | [VI](README_vi-VI.md)

**Внимание:** Для получения дополнительной поддержки, вопросов или обсуждений посетите наш [Discord](https://discord.gg/cfPKJ6N5hw).

### Примечательные особенности
- Базовые игровые функции: Внутриигровой вход, подбор команд, инвентарь, базовое управление сценой боя/объектами
- Механики монстров в бою
- Появление монстров/объектов/NPC в мире игры
- Техники персонажей
- Перекрафт/Работа с расходными материалами
- Магазины NPC
- Гача-Система
- Почта
- Система друзей (пока недоступно)
- Зал забвения
- Чистый вымысел
- Виртуальная вселенная (работает, но большая часть функций отсутствует)

# Запуск сервера и клиента

### Предварительные условия
* [Java 17 JDK](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

### Рекомендовано
* [MongoDB 4.0+](https://www.mongodb.com/try/download/community)

### Компиляция сервера
1. Откройте системный терминал и скомпилируйте сервер с помощью `./gradlew jar`
2. Создайте папку названную `resources` в директории Вашего сервера
3. Загрузите папки `Config`, `TextMap`, и `ExcelBin` из [https://github.com/Dimbreath/StarRailData](https://github.com/Dimbreath/StarRailData) и поместите их в папку `resources`.
4. Удалите папку `/resources/Config/LevelOutput`.
5. Скачайте папку `Config` из [https://gitlab.com/Melledy/LunarCore-Configs](https://gitlab.com/Melledy/LunarCore-Configs) и поместите их в папку ресурсов. Они предназначены для обозначения спавнов в мире и очень важны для сервера.
6. Запустите сервер с `java -jar LunarCore.jar` через свой системный терминал. Lunar Core поставляется со встроенным внутренним сервером MongoDB для своей базы данных, поэтому установка Mongodb не требуется. Тем не менее, в любом случае настоятельно рекомендуется установить Mongodb. 

### Соединение с клиентом (метод Fiddler)
1. **Войдите с клиентом на официальный сервер и в учетную запись Hoyoverse хотя бы один раз, чтобы загрузить игровые данные.**
2. Скачайте и запустите [Fiddler Classic](https://www.telerik.com/fiddler).
3. Установите fiddler для расшифровки https-трафика. (Tools -> Options -> HTTPS -> Decrypt HTTPS traffic) Убедитесь, что ошибка сертификатов сервера игнорируется.
4. Скопируйте и вставьте следующий код во вкладку Fiddlerscript в Fiddler Classic:

```
import System;
import System.Windows.Forms;
import Fiddler;
import System.Text.RegularExpressions;

class Handlers
{
    static function OnBeforeRequest(oS: Session) {
        if (oS.host.EndsWith(".starrails.com") || oS.host.EndsWith(".hoyoverse.com") || oS.host.EndsWith(".mihoyo.com") || oS.host.EndsWith(".bhsr.com")) {
            oS.host = "localhost"; // Его также можно заменить другим IP-адресом.
        }
    }
};
```

5. Если в конфигурации для параметра `autoCreateAccount` установлено значение true, вы можете пропустить этот шаг. В другом случае введите `/account create [имя учетной записи]` в консоли сервера, чтобы создать учетную запись.
6. Войдите с именем своего аккаунта, поле пароля игнорируется сервером, можете написать что угодно.

### Команды сервера
Команды сервера могут быть использованы в консоли сервера или в игре. В списке друзей каждого игрока есть поддельный пользователь с именем "Server", которому вы можете отправить сообщение чтобы он использовал внутриигровые команды.

```
/account {create | delete} [username] (reserved player uid). Создаёт или удаляет учётную запись.
/avatar lv(level) p(ascension) r(eidolon) s(skill levels). Открывает свойства текущего аватара.
/clear {relics | lightcones | materials | items}. Удаляет отфильтрованные предметы из инвентаря игрока.
/gender {male | female}. Устанавливает пол игрока.
/give [item id] x[amount] lv[number]. Выдаёт определённому игроку предмет.
/giveall {materials | avatars | lightcones | relics}. Выдает игрокам предметы.
/heal. Исцеляет ваших персонажей.
/help. Отображает список доступных команд.
/kick @[player id]. Выгоняет игрока с сервера.
/mail [content]. Отправляет определённому игроку системное письмо.
/permission {add | remove | clear} Дает/забирает разрешение у определённого игрока.
/refill. Восстанавливает очки навыков в игре.
/reload. Перезагружает конфигурацию сервера.
/scene [scene id] [floor id]. Перемещает игрока в определённое место.
/spawn [npc monster id/prop id] s[stage id] x[amount] lv[level] r[radius] <battle monster ids...>. Призывает монстра или объект рядом с определённым игроком.
/stop. Останавливает сервер.
/unstuck @[player id]. Отключает от игры игрока, если он находится в сцене, которая не загружается.
/worldlevel [world level]. Устанавливает определённый уровень Равновесия игроку.
```


С любовью от Турри Франс <3
