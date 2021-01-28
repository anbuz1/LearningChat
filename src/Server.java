import sun.rmi.runtime.Log;

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

    @Override
    public void run() {

        String clientInfo = socket.getInetAddress().toString().replace("/","") + ":" + socket.getPort();
        System.out.println("Connect user: " + clientInfo);
        try{
            String incomingServerKey;
            String[] keys;
            String error = null;
            String forIdent = rd.readLine();

            if(forIdent.contains("|")){
                keys = forIdent.split("\\|");
                incomingServerKey = keys[0];

                if(keys.length==2) client = Main.getClient(keys[1]);

                if(!incomingServerKey.equals(serverKey)) {
                    error = Main.properties.getProperty("status_error1");
                }
                else if (client == null){

                    if(Main.serverMode.equals("1")){
                        createNewClient(rd,writer);

                        if(client==null){
                            error = Main.properties.getProperty("status_error3");
                        }
                    }
                    if(Main.serverMode.equals("2")){
                        error = Main.properties.getProperty("status_error2");
                    }
                }

            }else {
                error = Main.properties.getProperty("status_error2");
            }


            if(error != null){
                writer.println(error);
            }else {
                LOG.log(Level.SEVERE, "Authorization successful: " + clientInfo);
                writer.println(Main.properties.getProperty("status_ok"));
                client.setStatus(true);

                String request;
                while ((request = rd.readLine()) != null){



                }


            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNewClient(BufferedReader rd, PrintWriter writer) {
    }
}
