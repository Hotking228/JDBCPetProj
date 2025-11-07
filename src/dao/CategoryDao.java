package dao;

import connectionPool.ConnectionManager;
import dto.Category;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    private static final CategoryDao INSTANCE = new CategoryDao();
    private static final String SELECT_ALL_SQL = """
            SELECT c.id,
                t_n.name,
                t_t.type
            FROM finance_storage.categories c
                JOIN finance_storage.transaction_name t_n 
                ON t_n.id = c.name_id
                JOIN finance_storage.transaction_type t_t
                ON t_t.id = c.type_id
            """;
    private static final String SELECT_CATEGORY_BY_NAME_SQL = """
            SELECT c.id,
                t_n.name,
                t_t.type
            FROM finance_storage.categories c
                JOIN finance_storage.transaction_name t_n 
                ON t_n.id = c.name_id
                JOIN finance_storage.transaction_type t_t
                ON t_t.id = c.type_id
            WHERE t_n.name = ?
            """;
    private static final String SELECT_CATEGORY_BY_TYPE_SQL = """
            SELECT c.id,
                name,
                type
            FROM finance_storage.categories c
                JOIN finance_storage.transaction_name t_n 
                ON t_n.id = c.name_id
                JOIN finance_storage.transaction_type t_t
                ON t_t.id = c.type_id
            WHERE t_t.type = ?
            """;
    private static final String SELECT_CATEGORY_BY_TYPE_NAME_SQL = """
            SELECT c.id,
                name,
                type
            FROM finance_storage.categories c
                JOIN finance_storage.transaction_name t_n 
                ON t_n.id = c.name_id
                JOIN finance_storage.transaction_type t_t
                ON t_t.id = c.type_id
            WHERE t_t.type = ? AND t_n.name = ?
            """;

    private CategoryDao(){

    }

    public static CategoryDao getInstance(){
        return INSTANCE;
    }

    public List<Category> getAllCategories(){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_ALL_SQL)){
            var resultSet = statement.executeQuery();
            List<Category> list = new ArrayList<>();
            while(resultSet.next()){
                Category category = new Category(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("type")
                );
                list.add(category);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public List<Category> findCategoryByName(String name){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_CATEGORY_BY_NAME_SQL)){
            statement.setString(1, name);
            var resultSet = statement.executeQuery();
            List<Category> list = new ArrayList<>();
            while(resultSet.next()){
                Category category = new Category(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("type")
                );
                list.add(category);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public List<Category> findCategoryByType(String type){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_CATEGORY_BY_TYPE_SQL)){
            statement.setString(1, type);
            var resultSet = statement.executeQuery();
            List<Category> list = new ArrayList<>();
            while(resultSet.next()){
                Category category = new Category(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("type")
                );
                list.add(category);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            ConnectionManager.returnConnection(connection);
        }
    }

    public List<Category> findCategoryByTypeName(String type, String name){
        var connection = ConnectionManager.get();
        try(var statement = connection.prepareStatement(SELECT_CATEGORY_BY_TYPE_NAME_SQL)){
            statement.setString(1, type);
            statement.setString(2, name);
            var resultSet = statement.executeQuery();
            List<Category> list = new ArrayList<>();
            while(resultSet.next()){
                Category category = new Category(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getString("type")
                );
                list.add(category);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            ConnectionManager.returnConnection(connection);
        }
    }
}
