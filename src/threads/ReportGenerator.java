package threads;

import connectionPool.ConnectionManager;
import dao.CategoryDao;
import dao.TransactionDao;
import dto.Category;
import dto.Transaction;
import utils.PropertiesUtil;
import utils.ReflectionHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ReportGenerator {

    private static final Long ID_PLACEHOLDER = -1L;
    private static final String THREAD_POOL_SIZE_KEY = "thread.pool.size";

    static void main() throws IOException {
        Map<String, String> commands = new HashMap<>();
        commands.put("add", "Добавление новой транзакции");
        commands.put("all", "Показ всех транзакций");
        commands.put("all period", "показ всех транзакций за период");
        commands.put("period", "Показ суммы доходов/расходов за период");
        commands.put("top", "Показ топ 5 больших трат за период");
        commands.put("total", "Показ общей суммы доходов и расходов за период");
        commands.put("delete", "удаление транзакции по индексу");
        commands.put("cat", "показ всех категорий транзакций");
        ExecutorService threadPool = Executors.newFixedThreadPool(Integer.parseInt(PropertiesUtil.get(THREAD_POOL_SIZE_KEY)));
        try(BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in))){
            System.out.println("=========== Personal finance tracker ===========");
            System.out.println("Input one of following command: ");
            for (var command : commands.entrySet()) {
                System.out.println(command.getKey() + " : " + command.getValue());
            }
            LocalDateTime fDate;
            LocalDateTime tDate;
            while(true){
                String cmd = inputStream.readLine().trim();
                if(cmd.equals("exit")) break;
                if(!commands.containsKey(cmd)) continue;

                switch (cmd){
                    case("add"):
                        System.out.print("amount : ");Double amount = Double.parseDouble(inputStream.readLine().trim());
                        System.out.print("type : ");String type = inputStream.readLine().trim();
                        System.out.print("name : ");String name = inputStream.readLine().trim();
                        System.out.print("Date and time(yyyy-MM-ddTHH:mm:SS): ");LocalDateTime dateTime = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("Desription : ");String description = inputStream.readLine().trim();
                        threadPool.submit(() -> add(amount, type,  name, dateTime, description));
                        break;
                    case("all"):
                        threadPool.submit(() ->  all());
                        break;
                    case("period"):
                        System.out.print("type : "); String pType = inputStream.readLine();
                        System.out.print("from date(yyyy-MM-ddTHH:mm:SS) : "); fDate = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("to date(yyyy-MM-ddTHH:mm:SS) : "); tDate = LocalDateTime.parse(inputStream.readLine().trim());
                        LocalDateTime finalFDate = fDate;
                        LocalDateTime finalTDate = tDate;
                        threadPool.submit(() -> period(pType, finalFDate, finalTDate));
                        break;
                    case("top"):
                        System.out.print("from date(yyyy-MM-ddTHH:mm:SS) : "); fDate = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("to date(yyyy-MM-ddTHH:mm:SS) : "); tDate = LocalDateTime.parse(inputStream.readLine().trim());
                        LocalDateTime finalFDate1 = fDate;
                        LocalDateTime finalTDate1 = tDate;
                        threadPool.submit(() -> top(finalFDate1, finalTDate1));
                        break;
                    case("total"):
                        System.out.print("from date(yyyy-MM-ddTHH:mm:SS) : "); fDate = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("to date(yyyy-MM-ddTHH:mm:SS) : "); tDate = LocalDateTime.parse(inputStream.readLine().trim());
                        LocalDateTime finalFDate2 = fDate;
                        LocalDateTime finalTDate2 = tDate;
                        threadPool.submit(() -> total(finalFDate2, finalTDate2));
                        break;
                    case("delete"):
                        System.out.print("delete index : "); Long delIdx = Long.parseLong(inputStream.readLine());
                        threadPool.submit(() -> delete(delIdx));
                        break;
                    case("cat"):
                        threadPool.submit(() -> cat());
                        break;
                    case("all period"):
                        System.out.print("from date(yyyy-MM-ddTHH:mm:SS) : "); fDate = LocalDateTime.parse(inputStream.readLine().trim());
                        System.out.print("to date(yyyy-MM-ddTHH:mm:SS) : "); tDate = LocalDateTime.parse(inputStream.readLine().trim());
                        LocalDateTime finalFDate3 = fDate;
                        LocalDateTime finalTDate3 = tDate;
                        threadPool.submit(() -> allPeriod(finalFDate3, finalTDate3));
                        break;
                }
            }
        }

        threadPool.shutdown();
        try {
            threadPool.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ConnectionManager.closeConnections();
    }

    private static void add(Double amount, String type,  String name, LocalDateTime date, String description){
        Category category = CategoryDao.getInstance().findCategoryByTypeName(type, name).getFirst();
        Transaction transaction = new Transaction(ID_PLACEHOLDER, amount, category.id(), date, description);
        TransactionDao.getInstance().addTransaction(transaction);
    }

    private static void all(){
        System.out.println("Executed by : " + Thread.currentThread().getName());
        TransactionDao.getInstance().getAllTransactions().forEach(ReflectionHelper::printObjectFields);
    }

    private static void period(String type, LocalDateTime sDate, LocalDateTime eDate){
        Float sum = TransactionDao.getInstance().getSumPeriod(sDate, eDate, type);
        System.out.println(type + " за период : " + sum);
    }

    private static void top(LocalDateTime fDate, LocalDateTime tDate){
        Map<String, Float> transactions = TransactionDao.getInstance().getTopExpenses(fDate, tDate);
        for (var transaction : transactions.entrySet()) {
            System.out.println(transaction.getKey() + " " + transaction.getValue());
        }
    }

    private static void total(LocalDateTime fDate, LocalDateTime tDate){
        Float sum = TransactionDao.getInstance().getMonthTotal(fDate, tDate);
        System.out.println(sum);
    }

    private static void delete(Long idx){
        System.out.println(TransactionDao.getInstance().deleteTransaction(idx) ? "transaction deleted" : "transaction not found");
    }

    private static void cat(){
        CategoryDao.getInstance().getAllCategories().forEach(ReflectionHelper::printObjectFields);
    }

    private static void allPeriod(LocalDateTime from, LocalDateTime to){
        TransactionDao.getInstance().getAllTransactionsPeriod(from, to).forEach(ReflectionHelper::printObjectFields);
    }
}
