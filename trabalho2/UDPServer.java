import java.net.*;
import java.io.*;

public class UDPServer {
    public static void main(String[] args) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(9876);
            System.out.println("Servidor UDP iniciado na porta 9876");

            byte[] buffer = new byte[1024];
            
            while (true) {
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivedPacket);
                
                String message = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), "UTF-8");
                System.out.println("Mensagem recebida: " + message);
                
                String reversed = new StringBuilder(message).reverse().toString();
                byte[] responseData = reversed.getBytes("UTF-8");
                
                InetAddress clientAddress = receivedPacket.getAddress();
                int clientPort = receivedPacket.getPort();
                
                DatagramPacket responsePacket = new DatagramPacket(
                    responseData, 
                    responseData.length, 
                    clientAddress, 
                    clientPort
                );
                socket.send(responsePacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}