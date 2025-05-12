package exerciciosTCP;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class TCPClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        
        try (Socket socket = new Socket("localhost", 12345);
             PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            
            System.out.println("Conectado ao servidor. Digite 'sair' para encerrar.");
            
            while (true) {
                System.out.print("Digite sua mensagem: ");
                String mensagem = scanner.nextLine();
                
                if (mensagem.equalsIgnoreCase("sair")) {
                    break;
                }
                
                out.println(mensagem);
                System.out.println("Mensagem enviada: " + mensagem);
                
                String resposta = in.readLine();
                System.out.println("Resposta do servidor: " + resposta);
            }
            
        } catch (IOException e) {
            System.err.println("Erro na conexão: " + e.getMessage());
        } finally {
            scanner.close();
            System.out.println("Conexão encerrada.");
        }
    }
}