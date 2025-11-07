package utils;

import java.io.IOException;
import java.util.Properties;

public class PropertiesUtil {

    private static final Properties PROPERTIES = new Properties();

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
}
