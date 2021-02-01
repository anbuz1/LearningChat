import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends Thread implements Serializable {
    private final Socket socket;
    private final String serverKey;
    private final BufferedReader rd;
    private final PrintWriter writer;
    private Client client;
    private static final String SERVER_KEY = "server_key";

    private static final Logger LOG = Logger.getLogger(Server.class.getName());


    public Server(Socket socket) throws IOException {
        this.socket = socket;
        serverKey = Main.properties.getProperty(SERVER_KEY);

        rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void writeMessage(String mess) {
    }

    public enum Const {
        LIST_CLIENT(Main.properties.getProperty("list_client")),
        SERVICE_KEY(Main.properties.getProperty("service_key")),
        DEFAULT("");

        private String key;

        Const(String key) {
            this.key = key;
        }


        @Override
        public String toString() {
            return this.key;
        }
    }

    @Override
    public void run() {

        String clientInfo = socket.getInetAddress().toString().replace("/", "") + ":" + socket.getPort();
        System.out.println("Connect user: " + clientInfo);
        try {
            String error = authorization();

            if (error != null) {
                writer.println(error);
            } else {
                LOG.log(Level.SEVERE, "Authorization successful: " + clientInfo);
                writer.println(Main.properties.getProperty("status_ok"));
                client.setStatus(true);

                String request;

                while ((request = rd.readLine()) != null) {
                    Const aConst = getConstByKey(request);

                    switch (aConst) {
                        case LIST_CLIENT: {
                            synchronized (Main.getActiveClientList()){
                                writer.println(Main.getActiveClientList());
                            }
                        }
                        break;

                        case SERVICE_KEY: {
                            String chat = Main.getChat();
                            if(chat.length()>0){
                                writer.println(chat);
                            }else writer.println("Welcome to chat</br>");

                        }
                        break;

                        case DEFAULT: {
                            Main.writeToChat(request, client.getNickname());
                        }
                        break;
                    }


                }


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNewClient(BufferedReader rd, PrintWriter writer) {
    }

    private String authorization() throws IOException {
        String incomingServerKey;
        String[] keys;
        String forIdentity = rd.readLine();

        if (!forIdentity.contains("|")) {
            return Main.properties.getProperty("status_error2");
        }

        keys = forIdentity.split("\\|");
        incomingServerKey = keys[0];

        if (keys.length == 2) {
            client = Main.getClient(keys[1]);
        }

        if (!incomingServerKey.equals(serverKey)) {
            return Main.properties.getProperty("status_error1");
        }

        if (client == null) {
            if (Main.serverMode.equals("2")) {
                return Main.properties.getProperty("status_error2");
            }

            createNewClient(rd, writer);
            if (client == null) {
                return Main.properties.getProperty("status_error3");
            }
        }

        return null;
    }

    Const getConstByKey(String key) {
        for (Const value : Const.values()) {
            if (value.key.equals(key)) return value;
        }
        return Const.DEFAULT;
    }

}
