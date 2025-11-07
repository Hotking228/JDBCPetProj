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
        commands.add("all"); // all
        commands.add("period"); // period <type> <sDate> <eDate>
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
            LocalDateTime fDate;
            LocalDateTime tDate;
            while(true){

                String cmd = inputStream.readLine().trim();
                if(cmd.equals("exit")) break;
                if(!commands.contains(cmd)) continue;

                switch (cmd){
                    case("add"):
                        System.out.print("amount : ");Double amount = Double.parseDouble(inputStream.readLine().trim());
                        System.out.print("type : ");String type = inputStream.readLine().trim();
                        System.out.print("name : ");String name = inputStream.readLine().trim();
                        System.out.print("Дата и время(yyyy-MM-ddTHH:mm:SS): ");LocalDateTime dateTime = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("Описание : ");String description = inputStream.readLine().trim();
                        add(amount, type,  name, dateTime, description);
                        break;
                    case("all"):
                        all();
                        break;
                    case("period"):
                        System.out.print("type : "); String pType = inputStream.readLine();
                        System.out.print("from date : "); fDate = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("to date : "); tDate = LocalDateTime.parse(inputStream.readLine().trim());
                        period(pType, fDate, tDate);
                        break;
                    case("top"):
                        System.out.print("from date : "); fDate = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("to date : "); tDate = LocalDateTime.parse(inputStream.readLine().trim());
                        top(fDate, tDate);
                        break;
                    case("total"):
                        System.out.print("from date : "); fDate = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("to date : "); tDate = LocalDateTime.parse(inputStream.readLine().trim());
                        total(fDate, tDate);
                        break;
                    case("delete"):
                        System.out.print("delete index : "); Long delIdx = Long.parseLong(inputStream.readLine());
                        delete(delIdx);
                        break;
                }
            }
        }
    }

    private static void add(Double amount, String type,  String name, LocalDateTime date, String description){
        Category category = CategoryDao.getInstance().findCategoryByTypeName(type, name).getFirst();
        Transaction transaction = new Transaction(ID_PLACEHOLDER, amount, category.id(), date, description);
        TransactionDao.getInstance().addTransaction(transaction);
    }

    private static void all(){
        List<Transaction> transactions = TransactionDao.getInstance().getAllTransactions();
        for (Transaction transaction : transactions) {
            System.out.println(transaction);
        }
    }

    private static void period(String type, LocalDateTime sDate, LocalDateTime eDate){
        Float sum = TransactionDao.getInstance().getSumPeriod(sDate, eDate, type);
        System.out.println(type + " за период : " + sum);
    }

    private static void top(LocalDateTime fDate, LocalDateTime tDate){
        Map<String, Float> transactions = TransactionDao.getInstance().getTopExpenses(fDate, tDate);
        for (var transaction : transactions.entrySet()) {
            System.out.println(transaction.getKey() + " : " + transaction.getValue());
        }
    }

    private static void total(LocalDateTime fDate, LocalDateTime tDate){
        Float sum = TransactionDao.getInstance().getMonthTotal(fDate, tDate);
        System.out.println(sum);
    }

    private static void delete(Long idx){
        System.out.println(TransactionDao.getInstance().deleteTransaction(idx) ? "transaction deleted" : "transaction not found");
    }
}
