package connectionPool;

import utils.PropertiesUtil;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionManager {

    private static final String DB_USERNAME_KEY = "db.username";
    private static final String DB_PASSWORD_KEY = "db.password";
    private static final String DB_URL_KEY = "db.url";
    private static final String DB_CONNECTION_POOL_SIZE_KEY = "db.pool.size";
    private static final List<Connection> list = new ArrayList<>();
    private static BlockingQueue<Connection> pool;

    static{
        init();
    }

    private static void init(){
        int size = Integer.parseInt(PropertiesUtil.get(DB_CONNECTION_POOL_SIZE_KEY));
        pool = new ArrayBlockingQueue<>(size);
        for (int i = 0; i < size; i++) {
            Connection connection = open();
            var proxyConnection = (Connection) Proxy.newProxyInstance(ConnectionManager.class.getClassLoader(), new Class[]{Connection.class},
                    (proxy, method, args) -> method.getName().equals("close") ? pool.add(connection) : method.invoke(connection,args));
            pool.add(proxyConnection);
            list.add(connection);
        }
    }

    public static void closeConnections(){
        for(int i = 0; i < list.size(); i++){
            try {
                list.get(i).close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Connection open(){

        try {
            return DriverManager.getConnection(
                    PropertiesUtil.get(DB_URL_KEY),
                    PropertiesUtil.get(DB_USERNAME_KEY),
                    PropertiesUtil.get(DB_PASSWORD_KEY)
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection get(){
        try {
            return pool.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}