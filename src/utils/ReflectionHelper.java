package utils;

import java.lang.reflect.Field;

public class ReflectionHelper {

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

            System.out.println(field.getClass().getName() + " = " + value);
            field.setAccessible(false);
        }
    }
}
