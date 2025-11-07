package dao;

import connectionPool.ConnectionManager;
import dto.Budget;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetDao {

    private static final BudgetDao INSTANCE = new BudgetDao();
    private static final String CREATE_BUDGET_SQL = """
            INSERT INTO finance_storage.budgets (amount, start_date, end_date)
            VALUES(?, ?, ?)
            """;
    private static final String SELECT_ACTIVE_BUDGET_SQL = """
            SELECT
               id,
               amount,
               end_date,
               start_date,
               amount - COALESCE((SELECT SUM(amount) FROM finance_storage.transactions t
           JOIN finance_storage.categories c
               ON c.id = t.category_id
           JOIN finance_storage.transaction_type t_t
               ON c.type_id = t_t.id
           WHERE t_t.type = 'Списание' AND t.date BETWEEN start_date AND end_date
           ), 0)money_left
           FROM finance_storage.budgets
           WHERE ? BETWEEN start_date AND end_date
           """;
    private static final String SELECT_ALL_SQL = """
            SELECT
               id,
               amount,
               end_date,
               start_date
           FROM finance_storage.budgets
           """;

    private BudgetDao(){

    }

    public static BudgetDao getInstance(){
        return INSTANCE;
    }

    public void createBudget(Budget budget){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(CREATE_BUDGET_SQL)){
            statement.setDouble(1, budget.amount());
            statement.setTimestamp(2, Timestamp.from(budget.startDate().toInstant(ZoneOffset.ofHours(0))));
            statement.setTimestamp(3, Timestamp.from(budget.endDate().toInstant(ZoneOffset.ofHours(0))));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public List<Budget> findAll(){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_ALL_SQL)){
            var resultSet = statement.executeQuery();
            List<Budget> list = new ArrayList<>();
            while(resultSet.next()){
                Budget budget = new Budget(
                        resultSet.getLong("id"),
                        resultSet.getDouble("amount"),
                        resultSet.getTimestamp("start_date").toLocalDateTime(),
                        resultSet.getTimestamp("end_date").toLocalDateTime()
                );
                list.add(budget);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public Map<Budget, Float> findActiveBudget(LocalDateTime date){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_ACTIVE_BUDGET_SQL)){
            statement.setTimestamp(1, Timestamp.from(date.toInstant(ZoneOffset.ofHours(0))));
            var resultSet = statement.executeQuery();
            Map<Budget, Float> map = new HashMap<>();
            while(resultSet.next()){
                Budget budget = new Budget(
                        resultSet.getLong("id"),
                        resultSet.getDouble("amount"),
                        resultSet.getTimestamp("start_date").toLocalDateTime(),
                        resultSet.getTimestamp("end_date").toLocalDateTime()
                );

                Float money_left = resultSet.getFloat("money_left");
                map.put(budget, money_left);
            }
            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }
}
