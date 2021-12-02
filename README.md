
## Краткое описание

Бэкэнд-часть приложения для изучения немецкого языка.
Приложение построено по модульному принципу и состоит из нескольких составных частей.

## Модули

### Словарь

Основной модуль приложения, на основе которого строятся все остальные составляющие.
У каждого пользователя есть собственный словарь, в который он может добавлять слова. Каждое слово определяется следующими характеристиками:

 - Перевод. 
 - Часть речи (из заданного в системе набора). 
 - Дополнительная информация по усмотрению пользователя.

Все характеристики являются необязательными. Для добавления слова в словарь достаточно ввести само слово.

Помимо добавления слов, пользователь также должен иметь возможность редактировать уже существующие слова в словаре и удалять их.

Слова можно добавлять по одному, через интерфейс веб-приложения, а также с помощью механизма импорта текстовых файлов, составленных по заданному формату.

В силу специфики немецкого языка: спряжения глаголов, склонения существительных и прилагательных, наличия рода и пр. - для некоторых частей речи выделены отдельные сущности, отражающие их грамматические особенности. Технически, слова хранятся однородно, а дополнительная информация, связанная с частью речи, хранится в отдельных таблицах. Благодаря этому появляется возможность постепенно расширять функционал словаря для разных частей речи, не затрагивая уже существующий функционал.

### Группы слов
Пользователь может объединить слова из словаря в группы произвольным образом, например по тематике, чтобы использовать их в других модулях приложения.

### Флеш-карточки
Пользователь может учить слова с помощью методики флеш-карточек, переводя их с немецкого языка на русский и наоборот. 

Перевод с немецкого языка на русский реализуется выбором варианта ответа.
Перевод с русского языка на немецкий реализуется дополнением слова, в котором не хватает нескольких букв, до правильного ответа.

Набор слов, используемых в флеш-карточках, формируется пользователем либо вручную, либо копированием групп слов, заданных им заранее (см п.2). Пользователь может сохранять наборы слов (для повторения в будущем) и удалять их.

Пользователь также может сделать коллекцию слов доступной по ссылке. Тогда другие авторизованные пользователи смогут сыграть в игру с данными словами.

### der-die-das
Для запоминания рода существительных пользователь может сыграть в игру der-die-das, цель которой - выбрать артикль существительного. 

Набор слов, используемых в игре, формируется пользователем либо вручную, либо копированием групп слов, заданных им заранее. Пользователь может сохранять наборы слов (для повторения в будущем) и удалять их.

Пользователь также может сделать коллекцию слов доступной по ссылке. Тогда другие авторизованные пользователи смогут сыграть в игру с данными словами.

## Авторизация и регистрация
Неавторизованные пользователи не имеют доступа к модулям приложения. Для получения доступа к возможностям сервиса пользователь должен авторизоваться. 

Для регистрации достаточно указать никнейм, email и пароль. После регистрации на почту пользователя придет письмо, в котором будет указана ссылка для активации учетной записи.

## Профиль пользователя
Пройдя регистрацию, пользователь может ввести дополнительные данные о себе, а также изменить никнейм. Новый никнейм должен быть уникальным.

## Главная страница
Содержит основную информацию о модулях приложения: краткое описание и ссылки для создания коллекций слов в играх. Также содержит список недавно использованных пользователем коллекций слов в играх с возможностью быстрого перехода к ним.

## Уровни доступа
 - Гость (неавторизованный пользователь) 
 - Пользователь 
 - Администратор

### Возможности гостя
- Создание учетной записи

### Возможности пользователя
- Авторизация
- Выход из аккаунта
- Добавление слов в личный словарь
- Редактирование и удаление слов из личного словаря
- Создание групп слов
- Создание коллекций слов для модулей приложения (флеш-карточки, der-die-das)
- Начало любой игры с созданным набором слов
- Начало любой игры с набором слов, доступным по ссылке

### Возможности администратора
- Все возможности пользователя
- Удаление пользователей
- Удаление наборов слов для игр

## Модель данных
### User
- id - serial, primary key
- email - varchar(256), unique, not null
- username - varchar(32), unique, not null
- uuid - varchar(32), unique, not null
- activated - boolean, not null
- password - varchar(128)
- first_name - varchar(32)
- last_name - varchar(32)

### Word
**word_type**: enum('sub', 'verb', 'adj', 'zahl', 'pron', 'adverb', 'konj', 'prep', 'part')
- id - serial, primary key
- user_id - integer, foreign key to User
- word - varchar(256), not null
- translate - varchar(256)
- info - varchar(256)
- has_type - boolean, not null

### Noun
**gender**: enum('m', 'f', 'n')
- id - serial, primary key
- word_id - integer, foreign key to Word
- plural - varchar(256)
- w_gender - gender, not null

### WordGroup
- id - serial, primary key
- user_id - integer, foreign key to User
- g_name - varchar(256), not null
- is_shared - boolean, not null

### GroupsToWords
- group_id - integer, foreign key to WordGroup
- word_id - integer, foreign key to Word

### FlashGroup
- id - serial, primary key
- user_id - integer, foreign key to User

### FlashGroupsToWords
- flash_id - integer, foreign key to FlashGroup
- word_id - integr, foreign key to Word

### DerDieDas
- ddd_id - serial, primary key
- user_id - integer, foreign key to User

### DDDToWords
- ddd_id - integer, foreign key to DerDieDas
- word_id - integer, foreign key to Word
