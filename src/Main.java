import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    static Properties properties = new Properties();

    private static Map<String, Client> clientList = new HashMap<>();
    private static List<Server> serverList = new ArrayList<>();
    private volatile static StringBuilder chat = new StringBuilder();
    private volatile static StringBuilder chatLog = new StringBuilder();
    private static SimpleDateFormat dt = new SimpleDateFormat("dd MMMM HH:mm:ss");
    protected static String serverPort;
    protected static String serverMode;
    private static String serverKey = "4f084e2ed5b7a422733b240320a9e223";

    private static final Logger LOG = Logger.getLogger(Server.class.getName());
    private static FileWriter logFw;


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

        String logFilePath = System.getProperty("user.dir") + "/log/chat.log";
        try {
            logFw = new FileWriter(logFilePath, true);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "chat.log не найден");
        }
    }

    /**
     * @param args 1. Путь к файлу конфигурации (0 - для пропуска)
     *             2. Номер порта (0 - для пропуска)
     *             3. Режим сервера
     */
    public static void main(String[] args) {

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

        String messLog = dtime + " " + nickname + "\n" + request + "\n";
        chatLog.append(messLog);

        for (Server server : serverList) {
            server.writeMessage(mess);
        }

        saveChatToLogFile();
    }

    private static void saveChatToLogFile() {
        //todo подумать как сохранить лог если он так и не достиг нужного размера
        if (chatLog.toString().split("\n").length > 4) {
            try {
                logFw.write(chatLog.toString());
                logFw.flush();
                chatLog.setLength(0);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "не получается записать сообщения в chat.log");
            }
        }
    }
}