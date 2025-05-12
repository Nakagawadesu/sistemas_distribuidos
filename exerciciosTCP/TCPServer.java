package exerciciosTCP; 
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class TCPServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor TCP iniciado na porta 12345");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            
            String message = in.readLine();
            System.out.println("Recebido de " + clientSocket.getInetAddress() );
            System.out.println("Mensagem: " + message);
            System.out.println("Tamanho da mensagem: " + message.length() + " bytes");
            
            String resposta = new StringBuilder(message).reverse().toString();
            out.println(resposta);
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}