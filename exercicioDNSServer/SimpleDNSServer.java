package exercicioDNSServer;
import java.io.*;
import java.net.*;

public class SimpleDNSServer {
    private static final int DNS_PORT = 5453;
    private static final String TARGET_DOMAIN = "messenger2222.com";
    private static final String RESPONSE_IP = "192.168.1.100";

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(DNS_PORT)) {
            System.out.println("DNS Server listening for " + TARGET_DOMAIN + " on port " + DNS_PORT);
            
            byte[] buffer = new byte[512];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);
                byte[] requestData = packet.getData();
                
                // Extract domain from request
                String requestedDomain = parseDomainName(requestData, 12);
                
                if(TARGET_DOMAIN.equalsIgnoreCase(requestedDomain)) {
                    byte[] transactionId = new byte[2];
                    System.arraycopy(requestData, 0, transactionId, 0, 2);
                    byte[] response = buildDnsResponse(transactionId);
                    
                    DatagramPacket responsePacket = new DatagramPacket(
                        response, response.length, 
                        packet.getAddress(), packet.getPort()
                    );
                    socket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String parseDomainName(byte[] data, int offset) {
        StringBuilder domain = new StringBuilder();
        int pos = offset;
        
        while (data[pos] != 0) {
            int labelLength = data[pos++] & 0xFF;
            for(int i=0; i<labelLength; i++) {
                domain.append((char) data[pos++]);
            }
            domain.append('.');
        }
        
        return domain.length() > 0 ? 
               domain.substring(0, domain.length()-1) : 
               "";
    }

    private static byte[] buildDnsResponse(byte[] transactionId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Header
            baos.write(transactionId);
            baos.write(new byte[] { (byte)0x81, (byte)0x80 }); // Flags
            baos.write(new byte[] { 0x00, 0x01 }); // Questions
            baos.write(new byte[] { 0x00, 0x01 }); // Answers
            baos.write(new byte[] { 0x00, 0x00, 0x00, 0x00 }); // Authority/Additional

            // Question section (messenger2.com)
            baos.write(new byte[] { 
                0x0A, 'm','e','s','s','e','n','g','e','r','2',
                0x03, 'c','o','m',
                0x00,
                0x00, 0x01,  // Type A
                0x00, 0x01   // Class IN
            });

            // Answer section
            baos.write(new byte[] { (byte)0xc0, 0x0c }); // Name pointer
            baos.write(new byte[] { 0x00, 0x01 }); // Type A
            baos.write(new byte[] { 0x00, 0x01 }); // Class IN
            baos.write(new byte[] { 0x00, 0x00, 0x0e, 0x10 }); // TTL
            baos.write(new byte[] { 0x00, 0x04 }); // Data length
            
            // IP Address bytes
            for(String part : RESPONSE_IP.split("\\.")) {
                baos.write((byte) Integer.parseInt(part));
            }

            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}