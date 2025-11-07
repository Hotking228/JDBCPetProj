# Пет проект с использованием JDBC, SQL и java Core.

## Основные использованные технологии - ThreadPool, ConnectionPool, JDBC, PostgreSQL

Так же в целях практики использовал функциональное программирование, сложные SQL запросы и тд.
Для создания БД необходимо в консоли Postgres запустить весь код из файла dbCreate.txt в папке resources, после чего приложение будет работать, остается только настроить логин и пароль в файле application.properties в соответствующих полях,
остальные можете изменить по своему усмотрению, в рамках данного приложения особой роли не играют, сделаны были мной для практики.

## Ход работы:

### 1) для началя я создал БД.
Весь приведенный SQL - код лежит в файле dbCreate.txt.
Для связи между таблицами использовал ключи, внешние и первичные, первые для уникального идентификатора строки, вторые - для ссылки на другие таблицы из БД.
Вот как выглядят такие классы:
```java
public record Budget(Long id,
                     Double amount,
                     LocalDateTime startDate,
                     LocalDateTime endDate) {
}
```

### 2) Далее разработал DTO.
DTO - data transfer object - объект для передачи данных между service слоем и DAO (напишу ниже).
Использовал record, поскольку удобно, сразу переопределены все удобные методы, есть удобные геттеры, конструктор и toString().

### 3) После разработки DTO создал DAO.
DAO - data access object - слой который служит для обмена данными между java приложением и БД.
Здесь активно используется ConnectionPool - класс который хранит пул подключений к БД. Это необходимо, поскольку подключение - дорогая операция, поэтому подключение происходит 1 раз - в блоке инициализации static{} класса ConnectionManager.
Поскольку целью было попрактиковаться в использовании threadPool - создаем ConnectionPool используя java.util.concur, а конкретно - BlockingQueue - очередь с потокобезопасностью, чтобы в дальнейшем разные потоки могли тащить подключения,
не опасаясь NullPointer. 
Рассмотрим пример метода, который тянет данные из БД:

```java
public Map<Budget, Float> findActiveBudget(LocalDateTime date){
        var connection = ConnectionManager.get();//Берем подключение из пула подключений
        try(var statement = connection.prepareStatement(SELECT_ACTIVE_BUDGET_SQL)){ // используем try with resources для автоматического закрытия statement после использования
            statement.setTimestamp(1, Timestamp.from(date.toInstant(ZoneOffset.ofHours(0)))); //устанавливаем вопросик в SQL запросе(рассмотрим запрос ниже)
            var resultSet = statement.executeQuery(); // выполняем запрос, используя executeQuery, поскльку он возвращает ResultSet
            Map<Budget, Float> map = new HashMap<>();
            while(resultSet.next()){
                Budget budget = new Budget( // создаем экземпляр Budget
                        resultSet.getLong("id"),
                        resultSet.getDouble("amount"),
                        resultSet.getTimestamp("start_date").toLocalDateTime(),
                        resultSet.getTimestamp("end_date").toLocalDateTime()
                );

                Float money_left = resultSet.getFloat("money_left"); //рассчет остатка средств
                map.put(budget, money_left);
            }
            return map; // и возвращаем словарик
        } catch (SQLException e) {
            throw new RuntimeException(e); // исключения было лучше создать свои, как перспектива развития проекта
        }finally {
            ConnectionManager.returnConnection(connection); // и поскольку finally выполняется ВСЕГДА возвращаем соединение в пул после использования
                                                            // я пробовал переопределить метод close и обернуть подключение в Proxy,
                                                            // но почему то они в таком случае не всегда возвращаются в пул, поэтому решил делать это вручную
        }
    }
```
### 4) Так же в процессе разработки писались SQL запросы :
```SQL
  SELECT
     id,
     amount,
     end_date,
     start_date,--получем все поля
     amount - COALESCE((SELECT SUM(amount) FROM finance_storage.transactions t -- Используем COALESCE, поскольку можем получить NULL, если нет транзакций
 JOIN finance_storage.categories c -- подключаем таблицу категорий, чтобы вычитать только списания
     ON c.id = t.category_id
 JOIN finance_storage.transaction_type t_t
     ON c.type_id = t_t.id -- и подключаем таблицу типов операций для определения является ли операция списанием
 WHERE t_t.type = 'Списание' AND t.date BETWEEN start_date AND end_date -- должная находится в интервале дат текущего бюджета и являться списанием
 ), 0)money_left
 FROM finance_storage.budgets
 WHERE ? BETWEEN start_date AND end_date -- дата должна быть внутри интервала дат бюджета
```
Данный запрос возвращает активный бюджет на указанный пользователем момент времени и остаток по счету который у него остается

### 5) Так же для удобного отображения информации использовался ReflectionAPI : 
```java
   public static void printObjectFields(Object obj){
      Field[]fileds = obj.getClass().getDeclaredFields();
      for (Field field : fileds) {
          field.setAccessible(true);
          Object value;
          try {
              value = field.get(obj);
          } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
          }

          System.out.print(field.getName() + " = " + value + " ");
          field.setAccessible(false);
      }
      System.out.println();
  }
```
  Здесь мы просто получаем все поля класса, далем их публичными и печатаем в удобном формате.

### 6)Properties
  Так же был создан еще 1 простенький утилитный класс для работы с application.properties:
      private static final Properties PROPERTIES = new Properties();
```java
    private PropertiesUtil(){

    }

    static {
        load();
    }

    public static String get(String key){
        return PROPERTIES.getProperty(key);
    }

    private static void load(){
        try(var inputStream = PropertiesUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
            PROPERTIES.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
```
класс загружает в словарик все Properties и по ключу их возвращает тому классу, которому они нужны.
## Вывод
Таким образом, было разработано простое консольное приложения для закрепления навыков указанных в начале.
Чтобы им пользоваться нужно просто запустить консоль и далее писать команды, которые при старте вы увидите в консоли или можете написать 
"help" чтобы снова их увидеть.
