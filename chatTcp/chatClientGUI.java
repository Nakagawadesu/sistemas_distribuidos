package chatTcp;

import java.awt.*; // Importa classes para interface gráfica (Abstract Window Toolkit)
import java.awt.event.ActionEvent; // Importa classe para eventos de ação (ex: clique de botão)
import java.awt.event.ActionListener; // Importa interface para ouvir eventos de ação
import java.awt.event.WindowAdapter; // Importa classe para lidar com eventos de janela de forma mais simples
import java.awt.event.WindowEvent; // Importa classe para eventos de janela (ex: fechar janela)
import java.io.*; // Importa classes para entrada e saída de dados (Input/Output)
import java.net.*; // Importa classes para programação de rede (ex: Sockets)
import javax.swing.*; // Importa classes para interface gráfica Swing (mais moderna que AWT)

// Classe principal do cliente GUI, herda de JFrame para ser uma janela
public class chatClientGUI extends JFrame {
    // Constantes para o endereço IP e porta do servidor
    private static final String SERVER_IP = "localhost"; // IP do servidor (neste caso, a máquina local)
    private static final int SERVER_PORT = 12345; // Porta em que o servidor está escutando

    // Variáveis de instância para o cliente
    private String username; // Nome do usuário no chat
    private Socket socket; // Socket para comunicação com o servidor
    private ObjectOutputStream out; // Stream para enviar objetos (mensagens) ao servidor
    private ObjectInputStream in; // Stream para receber objetos (mensagens) do servidor

    // Componentes da Interface Gráfica (GUI)
    private JTextArea messagesArea; // Área de texto para exibir as mensagens do chat
    private JTextField messageField; // Campo de texto para o usuário digitar mensagens
    private JButton sendButton; // Botão para enviar mensagens
    private JLabel statusLabel; // Rótulo para exibir o status da conexão

    // Construtor da classe chatClientGUI
    public chatClientGUI() {
        initializeGUI(); // Chama o método para configurar a interface gráfica
        connectToServer(); // Chama o método para se conectar ao servidor
    }

    // Método para inicializar e configurar os componentes da GUI
    private void initializeGUI() {
        setTitle("Chat TCP - Cliente GUI"); // Define o título da janela
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Define a ação padrão ao fechar a janela (terminar a aplicação)
        setSize(600, 500); // Define o tamanho da janela
        setLocationRelativeTo(null); // Centraliza a janela na tela

        setLayout(new BorderLayout()); // Define o layout principal da janela como BorderLayout

        // Configuração da área de mensagens
        messagesArea = new JTextArea(); // Cria a área de texto para mensagens
        messagesArea.setEditable(false); // Impede que o usuário edite diretamente a área de mensagens
        messagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); // Define a fonte
        messagesArea.setBackground(Color.WHITE); // Define a cor de fundo
        messagesArea.setForeground(Color.BLACK); // Define a cor do texto
        messagesArea.setLineWrap(true); // Habilita a quebra de linha automática
        messagesArea.setWrapStyleWord(true); // Faz a quebra de linha respeitando palavras inteiras

        // Adiciona a área de mensagens a um painel com barra de rolagem
        JScrollPane scrollPane = new JScrollPane(messagesArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Mostra sempre a barra de rolagem vertical
        add(scrollPane, BorderLayout.CENTER); // Adiciona o painel com rolagem ao centro da janela

        // Configuração do painel de entrada de mensagens (campo de texto e botão de enviar)
        JPanel inputPanel = new JPanel(new BorderLayout()); // Cria um novo painel com BorderLayout
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Adiciona uma borda vazia (espaçamento)

        messageField = new JTextField(); // Cria o campo de texto para digitar mensagens
        messageField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12)); // Define a fonte

        sendButton = new JButton("Enviar"); // Cria o botão "Enviar"
        sendButton.setPreferredSize(new Dimension(80, 25)); // Define o tamanho preferido do botão

        // Adiciona o campo de texto e o botão ao painel de entrada
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH); // Adiciona o painel de entrada à parte inferior da janela

        // Configuração da barra de status
        statusLabel = new JLabel("Desconectado"); // Cria o rótulo de status
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Adiciona uma borda vazia
        statusLabel.setBackground(Color.LIGHT_GRAY); // Define a cor de fundo
        statusLabel.setOpaque(true); // Torna o fundo do rótulo visível
        add(statusLabel, BorderLayout.NORTH); // Adiciona a barra de status à parte superior da janela

        // Adiciona listeners de evento para o botão e o campo de texto
        // (para enviar mensagem ao clicar no botão ou pressionar Enter no campo)
        sendButton.addActionListener(new SendMessageListener());
        messageField.addActionListener(new SendMessageListener());

        // Adiciona um listener para o evento de fechamento da janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { // Método chamado quando a janela está sendo fechada
                disconnect(); // Chama o método para desconectar do servidor
                System.exit(0); // Encerra a aplicação
            }
        });

        // Adiciona mensagens iniciais de instrução na área de chat
        appendMessageLocal("=== Chat TCP ===");
        appendMessageLocal("Comandos disponíveis:");
        appendMessageLocal("/privado <destinatario> <mensagem> - Enviar mensagem privada");
        appendMessageLocal("/usuarios - Listar usuários conectados");
        appendMessageLocal("Digite uma mensagem e pressione Enter para enviar");
        appendMessageLocal("");
    }

    // Método para conectar o cliente ao servidor
    private void connectToServer() {
        try {
            // Solicita o nome de usuário através de uma caixa de diálogo
            username = JOptionPane.showInputDialog(
                this, // Componente pai da caixa de diálogo (a própria janela)
                "Digite seu nome de usuário:", // Mensagem da caixa de diálogo
                "Nome de Usuário", // Título da caixa de diálogo
                JOptionPane.QUESTION_MESSAGE // Tipo de mensagem (ícone de interrogação)
            );

            // Verifica se o nome de usuário é válido
            if (username == null || username.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome de usuário é obrigatório!", "Erro", JOptionPane.ERROR_MESSAGE);
                System.exit(0); // Encerra se o nome for inválido
                return;
            }
            username = username.trim(); // Remove espaços em branco extras

            // Estabelece a conexão com o servidor
            socket = new Socket(SERVER_IP, SERVER_PORT); // Cria o socket
            // Cria os streams de entrada e saída de objetos para comunicação serializada
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Envia a primeira mensagem ao servidor (mensagem de login com o nome de usuário)
            out.writeObject(new Mensagem(username, null, ""));

            // Inicia uma nova thread para receber mensagens do servidor continuamente
            new Thread(new MessageReceiver()).start();

            // Atualiza a interface gráfica para indicar que está conectado
            statusLabel.setText("Conectado como: " + username);
            statusLabel.setBackground(new Color(144, 238, 144)); // Verde claro
            appendMessageLocal("Conectado ao servidor como: " + username);

            // Habilita os campos de entrada de mensagem
            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            messageField.requestFocus(); // Coloca o foco no campo de mensagem

        } catch (IOException e) { // Captura exceções de entrada/saída (ex: servidor offline)
            JOptionPane.showMessageDialog(
                this,
                "Erro ao conectar com o servidor: " + e.getMessage(),
                "Erro de Conexão",
                JOptionPane.ERROR_MESSAGE
            );
            // Atualiza a GUI para indicar erro de conexão
            statusLabel.setText("Erro de conexão");
            statusLabel.setBackground(new Color(255, 102, 102)); // Vermelho claro
            // Desabilita campos de entrada
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
        }
    }

    // Método para enviar uma mensagem
    private void sendMessage() {
        String messageText = messageField.getText().trim(); // Pega o texto do campo e remove espaços
        if (messageText.isEmpty()) { // Se a mensagem estiver vazia, não faz nada
            return;
        }

        try {
            Mensagem msgToSend; // Objeto Mensagem a ser enviado

            // Verifica se é um comando de mensagem privada
            if (messageText.startsWith("/privado ")) {
                String[] parts = messageText.split(" ", 3); // Divide a mensagem em 3 partes: "/privado", "destinatario", "conteudo"
                if (parts.length < 3) { // Se não tiver as 3 partes, o formato está incorreto
                    appendMessageLocal("[CLIENTE] Formato correto: /privado <destinatario> <mensagem>");
                    messageField.setText(""); // Limpa o campo de mensagem
                    return;
                }
                String recipientName = parts[1]; // Nome do destinatário
                String privateContent = parts[2]; // Conteúdo da mensagem privada

                // Prepara o conteúdo no formato que o servidor espera para identificar mensagens privadas
                String serverExpectedContent = "/privado:" + recipientName + ":" + privateContent;
                // Cria o objeto Mensagem com remetente, destinatário e o conteúdo formatado
                msgToSend = new Mensagem(username, recipientName, serverExpectedContent);

                // Exibe a mensagem privada na tela do remetente
                appendMessageLocal("Você para " + recipientName + " (privado): " + privateContent);

            } else if (messageText.equals("/usuarios")) { // Verifica se é o comando para listar usuários
                msgToSend = new Mensagem(username, null, "/usuarios"); // Cria mensagem de comando
                // Não precisa de append local, o servidor enviará a lista como resposta
            } else {
                // Se não for comando, é uma mensagem pública (broadcast)
                msgToSend = new Mensagem(username, null, messageText); // Destinatário null indica broadcast
                appendMessageLocal("Você: " + messageText); // Exibe a mensagem na tela do remetente
            }

            out.writeObject(msgToSend); // Envia o objeto Mensagem para o servidor
            messageField.setText(""); // Limpa o campo de mensagem após o envio

        } catch (IOException e) { // Captura exceção se houver erro ao enviar
            appendMessageLocal("[CLIENTE] Erro ao enviar mensagem: " + e.getMessage());
            statusLabel.setText("Erro de comunicação");
            statusLabel.setBackground(new Color(255, 102, 102)); // Vermelho claro
        }
    }

    // Método para adicionar mensagens geradas localmente (pelo próprio cliente) à área de chat
    // Renomeado para clareza
    private void appendMessageLocal(String message) {
        // SwingUtilities.invokeLater garante que a atualização da GUI ocorra na Event Dispatch Thread (EDT)
        // Isso é crucial para a segurança de threads em aplicações Swing
        SwingUtilities.invokeLater(() -> {
            messagesArea.append(message + "\n"); // Adiciona a mensagem seguida de uma nova linha
            messagesArea.setCaretPosition(messagesArea.getDocument().getLength()); // Move o cursor (e a rolagem) para o final
        });
    }

    // Método para adicionar mensagens vindas do servidor à área de chat
    // Usado exclusivamente pelo MessageReceiver
    private void appendMessageFromServer(String message) {
        SwingUtilities.invokeLater(() -> {
            messagesArea.append(message + "\n");
            messagesArea.setCaretPosition(messagesArea.getDocument().getLength());
        });
    }


    // Método para desconectar o cliente do servidor
    private void disconnect() {
        try {
            appendMessageLocal("Desconectando..."); // Mensagem local
            if (out != null) {
                 // Opcional: Enviar uma mensagem de desconexão ao servidor se o protocolo suportar
                 // out.writeObject(new Mensagem(username, null, "/desconectar"));
            }
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Fecha o socket, o que também interrompe os streams e a thread MessageReceiver
            }
        } catch (IOException e) { // Captura erro ao fechar o socket
            System.err.println("[CLIENTE] Erro ao tentar fechar conexão: " + e.getMessage());
        } finally {
            // Bloco finally para garantir que os streams sejam fechados, mesmo que ocorram exceções
            try { if (in != null) in.close(); } catch (IOException e) { /* ignora erro ao fechar stream */ }
            try { if (out != null) out.close(); } catch (IOException e) { /* ignora erro ao fechar stream */ }
        }
    }

    // Classe interna para tratar eventos de ação (clique no botão "Enviar" ou Enter no campo de texto)
    private class SendMessageListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) { // Método chamado quando o evento ocorre
            sendMessage(); // Chama o método para enviar a mensagem
        }
    }

    // Classe interna que implementa Runnable para ser executada em uma nova thread
    // Responsável por receber mensagens do servidor continuamente
    private class MessageReceiver implements Runnable {
        @Override
        public void run() { // Código que será executado na nova thread
            try {
                // Loop para ler mensagens enquanto o socket estiver conectado e o stream de entrada existir
                while (socket != null && !socket.isClosed() && in != null) {
                    // Lê um objeto Mensagem do stream de entrada (bloqueia até receber algo)
                    Mensagem msgFromServer = (Mensagem) in.readObject();

                    // Lógica para não reexibir mensagens de broadcast que este cliente mesmo enviou,
                    // pois já foram adicionadas localmente pelo método sendMessage().
                    if (msgFromServer.getRemetente().equals(username) && msgFromServer.getDestinatario() == null) {
                        continue; // Pula para a próxima iteração do loop
                    }

                    String displayMessage; // String formatada para exibição
                    // Verifica o tipo de mensagem recebida
                    if (msgFromServer.getRemetente().equals("Servidor")) { // Mensagem do sistema
                        displayMessage = "[SISTEMA] " + msgFromServer.getConteudo();
                    } else if (msgFromServer.getDestinatario() != null && msgFromServer.getDestinatario().equals(username)) {
                        // Mensagem privada PARA ESTE CLIENTE vinda de outro usuário
                        // O servidor já tratou e limpou o conteúdo.
                        displayMessage = "[PRIVADO de " + msgFromServer.getRemetente() + "] " + msgFromServer.getConteudo();
                    } else if (msgFromServer.getDestinatario() == null) {
                        // Mensagem de broadcast de OUTRO usuário
                        displayMessage = msgFromServer.getRemetente() + ": " + msgFromServer.getConteudo();
                    } else {
                        // Mensagem privada entre outros dois usuários (não deveria chegar aqui, pois o servidor direciona)
                        // ou algum outro tipo de mensagem não esperada.
                        System.out.println("[CLIENTE GUI] Mensagem não esperada recebida: " + msgFromServer.toString());
                        continue; // Pula para a próxima iteração
                    }
                    appendMessageFromServer(displayMessage); // Adiciona a mensagem formatada à área de chat
                }
            } catch (EOFException e) { // Exceção comum quando o outro lado fecha a conexão abruptamente
                appendMessageFromServer("[CLIENTE] Desconectado do servidor (EOF).");
            } catch (SocketException e) { // Exceção relacionada a problemas no socket
                if (socket != null && socket.isClosed()) { // Se o socket foi fechado (ex: pelo disconnect())
                    appendMessageFromServer("[CLIENTE] Conexão fechada.");
                } else {
                    appendMessageFromServer("[CLIENTE] Erro de conexão (SocketException): " + e.getMessage());
                }
            } catch (IOException | ClassNotFoundException e) { // Outras exceções de I/O ou se a classe Mensagem não for encontrada
                appendMessageFromServer("[CLIENTE] Conexão com o servidor perdida ou erro de dados: " + e.getMessage());
            } catch (Exception e) { // Captura genérica para outros erros inesperados na thread
                appendMessageFromServer("[CLIENTE] Erro inesperado no recebimento: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                 e.printStackTrace(); // Imprime o stack trace do erro para depuração
            } finally {
                // Bloco finally para atualizar a GUI e desabilitar campos em caso de desconexão ou erro
                SwingUtilities.invokeLater(() -> {
                    // Só atualiza para "Desconectado" se já não estiver assim (evita sobrescrever msg de disconnect manual)
                    if (statusLabel != null && !statusLabel.getText().startsWith("Desconectado")) {
                        statusLabel.setText("Desconectado");
                        statusLabel.setBackground(new Color(255, 102, 102)); // Vermelho claro
                    }
                    if (messageField != null) messageField.setEnabled(false);
                    if (sendButton != null) sendButton.setEnabled(false);
                });
            }
        }
    }

    // Método principal da aplicação cliente GUI
    public static void main(String[] args) {
        // Tenta configurar o Look and Feel da interface para o padrão do sistema operacional
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Se não conseguir, usa o Look and Feel padrão do Java (Metal)
        }

        // Cria e exibe a janela da GUI na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new chatClientGUI().setVisible(true); // Cria uma instância e a torna visível
        });
    }
}