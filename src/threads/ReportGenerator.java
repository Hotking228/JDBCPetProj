package threads;

import dao.CategoryDao;
import dao.TransactionDao;
import dto.Category;
import dto.Transaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportGenerator {

    private static final Long ID_PLACEHOLDER = -1L;

    static void main() throws IOException {
        Set<String> commands = new HashSet<>();
        commands.add("add"); // add <amount> <type> <name> <date> <description>
        commands.add("all"); // all <>
        commands.add("period");
        commands.add("top");
        commands.add("total");
        commands.add("delete");
        try(ExecutorService threadPool = Executors.newWorkStealingPool()){
            System.out.println("=========== Personal finance tracker ===========");
            System.out.println("Input one of following command: ");
            for (String command : commands) {
                System.out.println(command);
            }
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
            while(true){

                String[] cmd = inputStream.readLine().trim().split(" ");
                if(cmd.length == 0 || !commands.contains(cmd[0])) continue;
                if(cmd[0].equals("exit")) break;
                switch (cmd[0]){
                    case("add"):
                        add(Double.parseDouble(cmd[1]), cmd[2],  cmd[3], LocalDateTime.parse(cmd[4]), cmd[5]);
                        break;
                }

                System.out.println(cmd);
            }
        }
    }

    private static void add(Double amount, String type,  String name, LocalDateTime date, String description){
        Category category = CategoryDao.getInstance().findCategoryByTypeName(type, name).getFirst();
        System.out.println(category);
        Transaction transaction = new Transaction(ID_PLACEHOLDER, amount, category.id(), date, description);
        TransactionDao.getInstance().addTransaction(transaction);
    }

    private static void all(){

    }

    private static void period(){

    }

    private static void top(){

    }

    private static void total(){

    }

    private static void delete(){

    }
}
