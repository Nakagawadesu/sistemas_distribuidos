package chatTcp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;

public class chatClientGUI extends JFrame {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    
    private String username;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    // Componentes da GUI
    private JTextArea messagesArea;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel statusLabel;

    public chatClientGUI() {
        initializeGUI();
        connectToServer();
    }

    private void initializeGUI() {
        setTitle("Chat TCP - Cliente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        
        // Layout principal
        setLayout(new BorderLayout());
        
        // Área de mensagens
        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        messagesArea.setBackground(Color.WHITE);
        messagesArea.setLineWrap(true);
        messagesArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(messagesArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
        
        // Painel inferior para entrada de mensagens
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        messageField = new JTextField();
        messageField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        sendButton = new JButton("Enviar");
        sendButton.setPreferredSize(new Dimension(80, 25));
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        add(inputPanel, BorderLayout.SOUTH);
        
        // Status bar
        statusLabel = new JLabel("Desconectado");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        statusLabel.setBackground(Color.LIGHT_GRAY);
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.NORTH);
        
        // Event listeners
        sendButton.addActionListener(new SendMessageListener());
        messageField.addActionListener(new SendMessageListener());
        
        // Fechar conexão ao fechar janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });
        
        // Instruções iniciais
        appendMessage("=== Chat TCP ===");
        appendMessage("Comandos disponíveis:");
        appendMessage("/privado:usuario mensagem - Enviar mensagem privada");
        appendMessage("/usuarios - Listar usuários conectados");
        appendMessage("Digite uma mensagem e pressione Enter para enviar");
        appendMessage("");
    }

    private void connectToServer() {
        try {
            // Solicitar nome de usuário
            username = JOptionPane.showInputDialog(
                this, 
                "Digite seu nome de usuário:", 
                "Nome de Usuário", 
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (username == null || username.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome de usuário é obrigatório!");
                System.exit(0);
                return;
            }
            
            username = username.trim();
            
            // Conectar ao servidor
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Enviar nome de usuário
            out.writeObject(new Mensagem(username, null, ""));
            
            // Iniciar thread para receber mensagens
            new Thread(new MessageReceiver()).start();
            
            statusLabel.setText("Conectado como: " + username);
            statusLabel.setBackground(Color.GREEN);
            appendMessage("Conectado ao servidor como: " + username);
            
            // Habilitar entrada de mensagens
            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            messageField.requestFocus();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                this, 
                "Erro ao conectar com o servidor: " + e.getMessage(), 
                "Erro de Conexão", 
                JOptionPane.ERROR_MESSAGE
            );
            statusLabel.setText("Erro de conexão");
            statusLabel.setBackground(Color.RED);
            
            // Desabilitar entrada de mensagens
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        try {
            Mensagem msg;
            
            if (message.startsWith("/privado:")) {
                String[] parts = message.split(" ", 2);
                if (parts.length < 2) {
                    appendMessage("Formato: /privado:usuario mensagem");
                    messageField.clear();
                    return;
                }
                String[] dest = parts[0].split(":");
                if (dest.length < 2) {
                    appendMessage("Formato: /privado:usuario mensagem");
                    messageField.clear();
                    return;
                }
                msg = new Mensagem(username, dest[1], "/privado:" + dest[1] + ":" + parts[1]);
                appendMessage("(Privado para " + dest[1] + ") " + parts[1]);
            } else {
                msg = new Mensagem(username, null, message);
                if (!message.startsWith("/")) {
                    appendMessage("Você: " + message);
                }
            }
            
            out.writeObject(msg);
            messageField.clear();
            
        } catch (IOException e) {
            appendMessage("Erro ao enviar mensagem: " + e.getMessage());
            statusLabel.setText("Erro de comunicação");
            statusLabel.setBackground(Color.RED);
        }
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            messagesArea.append(message + "\n");
            messagesArea.setCaretPosition(messagesArea.getDocument().getLength());
        });
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }

    private class SendMessageListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendMessage();
        }
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Mensagem msg = (Mensagem) in.readObject();
                    
                    // Formatar mensagem para exibição
                    String displayMessage;
                    if (msg.getRemetente().equals("Servidor")) {
                        displayMessage = "[SISTEMA] " + msg.getConteudo();
                    } else if (msg.getDestinatario() != null && msg.getDestinatario().equals(username)) {
                        displayMessage = "[PRIVADO] " + msg.getRemetente() + ": " + msg.getConteudo();
                    } else {
                        displayMessage = msg.getRemetente() + ": " + msg.getConteudo();
                    }
                    
                    appendMessage(displayMessage);
                }
            } catch (Exception e) {
                appendMessage("Conexão com o servidor perdida.");
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Desconectado");
                    statusLabel.setBackground(Color.RED);
                    messageField.setEnabled(false);
                    sendButton.setEnabled(false);
                });
            }
        }
    }

    public static void main(String[] args) {
        // Configurar Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            // Usar Look and Feel padrão se não conseguir definir o do sistema
        }
        
        SwingUtilities.invokeLater(() -> {
            new chatClientGUI().setVisible(true);
        });
    }
}