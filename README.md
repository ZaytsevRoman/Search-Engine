# Search Engine

По умолчанию веб-страница приложения находится по адресу: localhost:8080

![Image alt](https://github.com/ZaytsevRoman/SkillboxTask/blob/main/src/main/resources/screenshots/1.jpg)

На главное странице отображается статистика:
- количество проиндексированных сайтов
- количество проиндексированных страниц на этих сайтах
- количество встречающихся на этих страницах лемм

Так же можно посмотреть эту статистику для каждого сайта отдельно

На второй вкладке - Management

![Image alt](https://github.com/ZaytsevRoman/SkillboxTask/blob/main/src/main/resources/screenshots/2.jpg)

Мы можем запустить индексацию по всем указанным сайтам в настройках программы кнопкой - Start indexing, 
либо указать адрес одного сайта и запустить индексацию только по этому сайту кнопкой - Add/update

На третьей вкладке - Search

![Image alt](https://github.com/ZaytsevRoman/SkillboxTask/blob/main/src/main/resources/screenshots/3.jpg)

Мы можем осуществить поиск как по всем проиндексированным сайтам, так и выбрать один из проиндексированных сайтов для поиска

В пустое поле вводим искомый текст и нажимаем кнопку - Search

В этой же вкладке ниже отобразится результат поиска

![Image alt](https://github.com/ZaytsevRoman/SkillboxTask/blob/main/src/main/resources/screenshots/4.jpg)

Используемые технологии:
- Spring Framework
- MySQL
- Jsoup
- Lombok
- Lucene Morphology

Файл настроек:
application.yaml - находится в корневой папке программы, 
в нем указан порт, для отображения локальной страницы приложения
настройки для работы с базой данных и 
список сайтов с которыми будет работать программа
