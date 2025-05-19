
package chatTcp;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private String username;

    public static void main(String[] args) {
        new ChatClient().start();
    }

    public void start() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            setupUsername(out);
            new Thread(new MessageReceiver(in)).start();
            sendMessages(out);

        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
        }
    }

    private void setupUsername(ObjectOutputStream out) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite seu nome de usuário: ");
        username = scanner.nextLine();
        out.writeObject(new Mensagem(username, null, ""));
    }

    private void sendMessages(ObjectOutputStream out) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String input = scanner.nextLine();
                Mensagem msg;
                
                if (input.startsWith("/privado:")) {
                    String[] parts = input.split(" ", 2);
                    String[] dest = parts[0].split(":");
                    msg = new Mensagem(username, dest[1], "/privado:" + dest[1] + ":" + parts[1]);
                } else {
                    msg = new Mensagem(username, null, input);
                }
                
                out.writeObject(msg);
            }
        } catch (Exception e) {
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    private class MessageReceiver implements Runnable {
        private ObjectInputStream in;

        public MessageReceiver(ObjectInputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Mensagem msg = (Mensagem) in.readObject();
                    System.out.println(msg);
                }
            } catch (Exception e) {
                System.err.println("Conexão com o servidor perdida.");
            }
        }
    }
}