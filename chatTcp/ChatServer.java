package chatTcp;

import java.io.*;
import java.net.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private ObjectOutputStream out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out = new ObjectOutputStream(socket.getOutputStream());
                
                // Recebe o nome de usuário
                Mensagem loginMsg = (Mensagem) in.readObject();
                this.username = loginMsg.getRemetente();
                clients.put(username, this);
                broadcastSystemMessage(username + " entrou no chat!");

                // Processa mensagens
                while (true) {
                    Mensagem msg = (Mensagem) in.readObject();
                    processMessage(msg);
                }
            } catch (Exception e) {
                disconnectClient();
            }
        }

        private void processMessage(Mensagem msg) throws IOException {
            if (msg.getConteudo().startsWith("/privado:")) {
                sendPrivateMessage(msg);
            } else if (msg.getConteudo().equals("/usuarios")) {
                sendUserList();
            } else {
                broadcastMessage(msg);
            }
        }

        private void sendPrivateMessage(Mensagem msg) {
            String[] parts = msg.getConteudo().split(":", 3);
            if (parts.length >= 3) {
                String recipient = parts[1];
                String content = parts[2];
                Mensagem privateMsg = new Mensagem(username, recipient, content);
                
                ClientHandler recipientClient = clients.get(recipient);
                if (recipientClient != null) {
                    try {
                        recipientClient.out.writeObject(privateMsg);
                    } catch (IOException e) {
                        System.err.println("Erro ao enviar mensagem privada: " + e.getMessage());
                    }
                }
            }
        }

        private void sendUserList() throws IOException {
            String userList = "Usuários conectados:\n" + String.join("\n", clients.keySet());
            out.writeObject(new Mensagem("Servidor", username, userList));
        }

        private void broadcastMessage(Mensagem msg) throws IOException {
            for (ClientHandler client : clients.values()) {
                if (!client.username.equals(username)) {
                    client.out.writeObject(msg);
                }
            }
        }

        private void broadcastSystemMessage(String message) throws IOException {
            Mensagem systemMsg = new Mensagem("Servidor", null, message);
            for (ClientHandler client : clients.values()) {
                client.out.writeObject(systemMsg);
            }
        }

        private void disconnectClient() {
            try {
                if (username != null) {
                    clients.remove(username);
                    broadcastSystemMessage(username + " saiu do chat!");
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Erro ao desconectar cliente: " + e.getMessage());
            }
        }
    }
}