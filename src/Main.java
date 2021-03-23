import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    static Properties properties = new Properties();

    private static Map<String, Client> clientList;
    private static final List<Server> serverList = new ArrayList<>();
    private static final StringBuilder chat = new StringBuilder();
    private static final StringBuilder chatLog = new StringBuilder();
    private static final SimpleDateFormat dt = new SimpleDateFormat("dd MMMM HH:mm:ss");
    protected static String serverPort;
    protected static String serverMode;
    private static String serverKey = "4f084e2ed5b7a422733b240320a9e223";
    private static final String logFilePath;

    private static final Logger LOG = Logger.getLogger(Server.class.getName());
    private static int counterLogLines;


    static {
        String pathToLogProperties = System.getProperty("user.dir") + "/properties/logging.properties";
        if (Files.exists(Paths.get(pathToLogProperties))) {
            try {
                LogManager.getLogManager().readConfiguration(new FileInputStream(pathToLogProperties));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String configFilePath = System.getProperty("user.dir") + "/properties/config.properties";
        if (Files.exists(Paths.get(configFilePath))) {
            try {
                properties.load(new FileInputStream(configFilePath));
            } catch (IOException e) {
                LOG.log(Level.ALL, e.getMessage());
            }
        }

        logFilePath = System.getProperty("user.dir") + "/log/chat.log";
        if (!Files.exists(Paths.get(logFilePath))) {
            try {
                Files.createFile(Paths.get(logFilePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        clientList = deserializeClientList();
        counterLogLines = 0;
    }

    /**
     * @param args 1. Путь к файлу конфигурации (0 - для пропуска)
     *             2. Номер порта (0 - для пропуска)
     *             3. Режим сервера
     */
    public static void main(String[] args) {

        Runnable task = () -> {
            Scanner in = new Scanner(System.in);
            while (true) {
                String input = in.nextLine();
                if (input.equals("stop")) {
                    LOG.log(Level.SEVERE, "Server stopped");
                    System.exit(1);
                }
                if (input.equals("edit")) editClientList(in);
            }
        };
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(task);

        Runnable task2 = () -> {

            clientList.forEach((key, val) -> {
                val.setStatus(false);
            });
            if (serverList.size() > 0) {
                for (Server server : serverList) {
                    server.getClient().setStatus(true);
                }
                for (Server server : serverList) {
                    server.writeMessage("73285b2e2392a400a2323" + getActiveClients());
                }
            }
        };

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(task2, 0, 5, TimeUnit.SECONDS);


        if (args.length > 0) {

            for (int i = 0; i < args.length; i++) {

                switch (i) {
                    case 0: {

                        if (!args[i].equals("0")) {
                            try {
                                String pathToProperties = System.getProperty("user.dir") + args[i];
                                if (Files.exists(Paths.get(pathToProperties))) {
                                    properties.load(new FileInputStream(pathToProperties));
                                }
                            } catch (IOException e) {
                                LOG.log(Level.ALL, e.getMessage());
                            }
                        }

                    }

                    case 1: {

                        if (!args[i].equals("0")) {
                            serverPort = args[i];
                        }

                    }

                    case 2: {

                        serverMode = args[i];

                    }
                }
            }
        }

        serverMode = serverMode == null ? properties.getProperty("server_mode") != null ? properties.getProperty("server_mode") : "1" : "1";
        serverPort = serverPort == null ? properties.getProperty("port") != null ? properties.getProperty("port") : "8080" : "8080";
        serverKey = properties.getProperty("server_key") != null ? properties.getProperty("server_key") : serverKey;

        try (ServerSocket server = new ServerSocket(Integer.parseInt(serverPort))) {
            LOG.log(Level.SEVERE, "Server START!");
            int i = 0;
            while (true) {


                Socket clientSocket = server.accept();
                Server newServer = new Server(clientSocket);
                serverList.add(newServer);

                i++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        LOG.log(Level.ALL, "Server port: " + serverPort);
        LOG.log(Level.ALL, "Server mode: " + serverMode);
    }

    private static String getActiveClients() {
        StringBuilder clList = new StringBuilder();


        clientList.forEach((key, val) ->
        {
            if (val.getStatus()) {
                clList.append("<font color=red>" + val.getNickname())
                        .append("(online)" + "</font>")
                        .append("</br>");
            } else {
                clList.append("<font color=grey>" + val.getNickname())
                        .append("(offline)" + "</font>")
                        .append("</br>");
            }

        });

        return clList.toString();

    }

    private static void editClientList(Scanner in) {
        showActions();
        while (true) {
            String input = in.nextLine();
            if (input.equals("1")) {
                createNewUser(in);
            }
            if (input.equals("2")) {
                deleteUser(in);
            }
            if (input.equals("3")) {
                editUser(in);
            }
            if (input.equals("4")) {
                System.out.println("Exit from editmode");
                System.out.println("Enter command:");
                break;
            } else {
                System.out.println("Wrong input parameter. Please try again.");
                showActions();
            }
        }
    }

    private static void showActions() {
        int index_number = 1;
        System.out.println("----------------------------------------------------------------------");
        for (String personalKey : clientList.keySet()) {
            System.out.printf("%-10d%-20s%40s%n", index_number, clientList.get(personalKey).getNickname(), personalKey);
            index_number++;
        }
        System.out.println("----------------------------------------------------------------------");
        System.out.println();
        System.out.println("Select actions:");
        System.out.println("1 - Create new user");
        System.out.println("2 - Delete user");
        System.out.println("3 - Edit user name");
        System.out.println("4 - Exit edit mode");
    }

    private static void createNewUser(Scanner in) {
        System.out.println("Enter user name:");
        String nickName = in.nextLine();
        String personalKey = generate();
        clientList.put(personalKey, new Client(nickName, personalKey));
        serializeClientList();
        System.out.println("Success create new user: " + nickName + " key: " + personalKey);
        System.out.println();
        showActions();
    }

    private static void deleteUser(Scanner in) {
        System.out.println("Enter user key:");
        String personalKey = in.nextLine();
        String nickName = clientList.get(personalKey).getNickname();
        clientList.remove(personalKey);
        serializeClientList();
        System.out.println("Success delete user: " + nickName + " key: " + personalKey);
        System.out.println();
        showActions();
    }

    private static void editUser(Scanner in) {
        System.out.println("Enter user key:");
        String personalKey = in.nextLine();
        System.out.println("Enter new name:");
        String nickName = in.nextLine();
        Client client = clientList.get(personalKey);
        serializeClientList();
        client.setNickname(nickName);
        System.out.println("Success edited. New user name: " + nickName + " key: " + personalKey);
        System.out.println();
        showActions();
    }

    public static String generate() {
        SecureRandom random = new SecureRandom();
        byte[] values = random.generateSeed(16);
        random.nextBytes(values);

        StringBuilder sb = new StringBuilder();
        for (byte b : values) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    protected static Client getClient(String key) {
        return Main.clientList.get(key);
    }

    protected static String getActiveClientList() {
        StringBuilder clList = new StringBuilder();

        clientList.forEach((key, val) -> {
            if (val.getStatus()) {
                clList.append("<font color=red>").append(val.getNickname())
                        .append(" online" + "</font>")
                        .append("</br>");

            } else {
                clList.append("<font color=grey>").append(val.getNickname())
                        .append(" offline" + "</font>")
                        .append("</br>");

            }
        });


        return clList.toString();
    }

    public static String getChat() {
        return chat.toString();
    }

    public static void writeToChat(String request, String nickname) {
        String dtime = dt.format(new Date());
        String mess = "<font size=2 color=red>" + "(" + dtime + ")  " + "</font><font size=3 color=blue>" + nickname + ": </font></br>" + request + "</br></br>";
        chat.append(mess);

        String messLog = dtime + " " + nickname + ": " + request + "\n";
        chatLog.append(messLog);
        counterLogLines++;
        for (Server server : serverList) {
            server.writeMessage(mess);
        }

        if (counterLogLines == Integer.parseInt(properties.getProperty("max_log_values"))) saveChatToLogFile();
    }

    private static void saveChatToLogFile() {
        //todo подумать как сохранить лог если он так и не достиг нужного размера
        try (FileWriter logFw = new FileWriter(logFilePath, true)) {
            logFw.write(chatLog.toString());
            logFw.flush();
            chatLog.setLength(0);
            counterLogLines = 0;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage());
        }

    }

    static void serializeClientList() {

        try (FileOutputStream outputStream = new FileOutputStream(System.getProperty("user.dir") + "/properties/Client.lst");
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream))
        {
            objectOutputStream.writeObject(clientList);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, e.getMessage());
        }

    }

    static Map<String, Client> deserializeClientList() {
        Map<String, Client> clientList = new HashMap<>();
        try (FileInputStream inputStream = new FileInputStream(System.getProperty("user.dir") + "/properties/Client.lst");
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            clientList = (Map<String, Client>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOG.log(Level.SEVERE, e.getMessage());
            return clientList;
        }
        return clientList;
    }

    public static Map<String, Client> getClientList() {
        return clientList;
    }

    public static void addClientList(Client client) {
        clientList.put(client.getPersonalKey(),client);
    }
}