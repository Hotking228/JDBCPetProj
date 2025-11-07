package dao;

import connectionPool.ConnectionManager;
import dto.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TransactionDao {

    private static final TransactionDao INSTANCE = new TransactionDao();
    private static final String INSERT_SQL = """
            INSERT INTO finance_storage.transactions (amount, category_id, date, description)
            VALUES(?, ?, ?, ?)
            """;
    private static final String SELECT_ALL_SQL = """
            SELECT 
                id,
                amount,
                --(SELECT name FROM finance_storage.transaction_name t_n JOIN finance_storage.categories c ON name_id = t_n.id WHERE t.category_id = c.id) name,
                --(SELECT type FROM finance_storage.transaction_type t_t JOIN finance_storage.categories c ON type_id = t_t.id WHERE t.category_id = c.id) type,
                category_id,
                date,
                description
                FROM finance_storage.transactions t
            """;
    private static final String SELECT_ALL_PERIOD_SQL = """
            SELECT 
                id,
                amount,
                --(SELECT name FROM finance_storage.transaction_name t_n JOIN finance_storage.categories c ON name_id = t_n.id WHERE t.category_id = c.id) name,
                --(SELECT type FROM finance_storage.transaction_type t_t JOIN finance_storage.categories c ON type_id = t_t.id WHERE t.category_id = c.id) type,
                category_id,
                date,
                description
                FROM finance_storage.transactions t
                WHERE t.date BETWEEN ? AND ?
            """;
    private static final String SELECT_SUM_PERIOD = """
            SELECT SUM(t.amount)
            FROM finance_storage.transactions t
            JOIN finance_storage.categories c 
                ON c.id = t.category_id
            WHERE c.type_id = (SELECT id FROM finance_storage.transaction_type WHERE type = ?)
                AND t.date BETWEEN ? AND ?
            GROUP BY c.type_id
            """;
    private static final String SELECT_TOP_EXPENSES = """
            SELECT SUM(t.amount) s,
                   (SELECT name FROM finance_storage.transaction_name t_n WHERE c.name_id = t_n.id) t_name
            FROM finance_storage.transactions t
            JOIN finance_storage.categories c 
                ON c.id = t.category_id
            WHERE c.type_id = (SELECT id FROM finance_storage.transaction_type WHERE type = 'Списание')
                AND t.date BETWEEN ? AND ?
            GROUP BY c.name_id
            ORDER BY s DESC
            LIMIT 5
            """;
    private static final String SELECT_MONTH_TOTAL = """
            SELECT SUM(CASE
                        WHEN t_t.type = 'Списание' THEN -t.amount
                        WHEN t_t.type = 'Зачисление' THEN t.amount
                       END)
            FROM finance_storage.transactions t
            JOIN finance_storage.categories c
                ON c.id = t.category_id
            JOIN finance_storage.transaction_type t_t ON t_t.id = c.type_id
            WHERE t.date BETWEEN ? AND ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM finance_storage.transactions
            WHERE id = ?
            """;

    private TransactionDao(){

    }

    public static TransactionDao getInstance(){
        return INSTANCE;
    }

    public Long addTransaction(Transaction transaction){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)){
            statement.setDouble(1, transaction.amount());
            statement.setLong(2, transaction.category_id());
            statement.setTimestamp(3, Timestamp.from(transaction.date().toInstant(ZoneOffset.ofHours(0))));
            statement.setString(4, transaction.description());
            statement.executeUpdate();
            var generatedKeys = statement.getGeneratedKeys();
            if(generatedKeys.next()){
                return generatedKeys.getLong("id");
            }
            return -1L;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public Float getSumPeriod(LocalDateTime from, LocalDateTime to, String type){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_SUM_PERIOD)){
            List<Transaction> list = new ArrayList<>();
            statement.setString(1, type);
            statement.setTimestamp(2, Timestamp.from(from.toInstant(ZoneOffset.ofHours(0))));
            statement.setTimestamp(3, Timestamp.from(to.toInstant(ZoneOffset.ofHours(0))));
            var resultSet = statement.executeQuery();
            if(resultSet.next()){
                var result = (Float) resultSet.getObject(1);
                return  result;
            }

            return -1.0f;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public Map<String, Float> getTopExpenses(LocalDateTime from, LocalDateTime to){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_TOP_EXPENSES)){
            List<Transaction> list = new ArrayList<>();
            statement.setTimestamp(1, Timestamp.from(from.toInstant(ZoneOffset.ofHours(0))));
            statement.setTimestamp(2, Timestamp.from(to.toInstant(ZoneOffset.ofHours(0))));
            var resultSet = statement.executeQuery();
            Map<String, Float> map = new LinkedHashMap<>();
            while(resultSet.next()){
                map.put(resultSet.getString("t_name"), resultSet.getFloat("s"));
            }

            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public Float getMonthTotal(LocalDateTime from, LocalDateTime to){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_MONTH_TOTAL)){
            List<Transaction> list = new ArrayList<>();
            statement.setTimestamp(1, Timestamp.from(from.toInstant(ZoneOffset.ofHours(0))));
            statement.setTimestamp(2, Timestamp.from(to.toInstant(ZoneOffset.ofHours(0))));
            var resultSet = statement.executeQuery();
            if(resultSet.next()){
              return resultSet.getFloat(1);
            }

            return -1.0f;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public List<Transaction> getAllTransactions(){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_ALL_SQL)){
            List<Transaction> list = new ArrayList<>();
            var resultSet = statement.executeQuery();
            while(resultSet.next()){
                Transaction transaction = new Transaction(resultSet.getLong("id"),
                        resultSet.getDouble("amount"),
                        resultSet.getLong("category_id"),
                        ZonedDateTime.ofInstant(resultSet.getTimestamp("date").toInstant(), ZoneId.of("Europe/Paris")).toLocalDateTime(),
                        resultSet.getString("description"));
                list.add(transaction);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public List<Transaction> getAllTransactionsPeriod(LocalDateTime from, LocalDateTime to){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_ALL_PERIOD_SQL)){
            List<Transaction> list = new ArrayList<>();
            statement.setTimestamp(1, Timestamp.from(from.toInstant(ZoneOffset.ofHours(0))));
            statement.setTimestamp(2, Timestamp.from(to.toInstant(ZoneOffset.ofHours(0))));
            var resultSet = statement.executeQuery();
            while(resultSet.next()){
                Transaction transaction = new Transaction(resultSet.getLong("id"),
                        resultSet.getDouble("amount"),
                        resultSet.getLong("category_id"),
                        ZonedDateTime.ofInstant(resultSet.getTimestamp("date").toInstant(), ZoneId.of("Europe/Paris")).toLocalDateTime(),
                        resultSet.getString("description"));
                list.add(transaction);
            }

            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public boolean deleteTransaction(Long id){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(DELETE_SQL, Statement.RETURN_GENERATED_KEYS)){
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            ConnectionManager.returnConnection(connection);
        }
    }
}
