package exercicioDNSServer;
import java.net.*;

public class DNSTestClient {
    public static void main(String[] args) throws Exception {
        byte[] query = new byte[]{
            // DNS query for messenger2.com
            0x7B, 0x1D, // Transaction ID (random)
            0x01, 0x00, // Flags: Standard query
            0x00, 0x01, // Questions: 1
            0x00, 0x00, // Answer RRs: 0
            0x00, 0x00, // Authority RRs: 0
            0x00, 0x00, // Additional RRs: 0
            
            // Query: messenger2.com
            0x0A, 'm','e','s','s','e','n','g','e','r','2',
            0x03, 'c','o','m',
            0x00, // End of name
            0x00, 0x01, // Type A
            0x00, 0x01  // Class IN
        };

        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName("127.0.0.1");
        DatagramPacket packet = new DatagramPacket(query, query.length, address, 5453);
        socket.send(packet);

        byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        socket.receive(response);
        
        System.out.println("Response for messenger2.com:");
        for(byte b : response.getData()) {
            System.out.printf("%02x ", b);
        }
    }
}