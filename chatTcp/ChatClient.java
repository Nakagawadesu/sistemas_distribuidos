package chatTcp;

import java.io.*; // Importa classes para entrada e saída de dados
import java.net.*; // Importa classes para programação de rede
import java.util.Scanner; // Importa classe para ler entrada do usuário

// Classe principal do cliente de terminal
public class ChatClient {
    // Constantes para o endereço IP e porta do servidor
    private static final String SERVER_IP = "localhost"; // IP do servidor (máquina local)
    private static final int SERVER_PORT = 12345; // Porta do servidor
    private String username; // Nome do usuário no chat

    // Método principal que inicia o cliente
    public static void main(String[] args) {
        new ChatClient().start(); // Cria uma instância de ChatClient e chama o método start()
    }

    // Método para iniciar a lógica do cliente (conexão, envio e recebimento de mensagens)
    public void start() {
        // Usa try-with-resources para garantir que os Sockets e Streams sejam fechados automaticamente
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT); // Conecta-se ao servidor
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); // Stream para enviar objetos
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) { // Stream para receber objetos

            setupUsername(out); // Configura o nome de usuário e o envia ao servidor
            // Inicia uma nova thread para receber mensagens do servidor de forma assíncrona
            new Thread(new MessageReceiver(in)).start();
            sendMessages(out); // Mantém a thread principal para enviar mensagens digitadas pelo usuário

        } catch (IOException e) { // Captura exceções de I/O (ex: falha na conexão)
            System.err.println("Erro no cliente: " + e.getMessage());
        }
    }

    // Método para obter o nome de usuário e enviá-lo como primeira mensagem ao servidor
    private void setupUsername(ObjectOutputStream out) throws IOException {
        Scanner scanner = new Scanner(System.in); // Cria um Scanner para ler do console
        System.out.print("Digite seu nome de usuário: "); // Solicita o nome
        username = scanner.nextLine(); // Lê o nome digitado
        // Envia uma mensagem de "login" para o servidor com o nome de usuário
        out.writeObject(new Mensagem(username, null, "")); // Destinatário null, conteúdo vazio (só para identificação)
    }

    // Método para ler mensagens do console e enviá-las ao servidor
    private void sendMessages(ObjectOutputStream out) {
        // Usa try-with-resources para o Scanner
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) { // Loop infinito para continuar lendo e enviando mensagens
                String input = scanner.nextLine(); // Lê a linha digitada pelo usuário
                Mensagem msg; // Objeto Mensagem a ser enviado

                // Verifica se a mensagem é um comando de mensagem privada
                if (input.startsWith("/privado:")) {
                    // Formato esperado pelo usuário: /privado:destinatario:mensagem_de_texto
                    try {
                        // Remove o prefixo "/privado:" para obter "destinatario:mensagem_de_texto"
                        String commandPayload = input.substring("/privado:".length());
                        
                        // Encontra o índice do primeiro ':' que separa o destinatário da mensagem
                        int firstColonIndex = commandPayload.indexOf(':');

                        // Verifica se o formato é válido (deve haver um ':' e conteúdo antes e depois dele)
                        if (firstColonIndex > 0 && firstColonIndex < commandPayload.length() - 1) {
                            String recipientName = commandPayload.substring(0, firstColonIndex).trim(); // Extrai o nome do destinatário
                            String privateContent = commandPayload.substring(firstColonIndex + 1).trim(); // Extrai o conteúdo da mensagem privada

                            // O conteúdo a ser enviado ao servidor deve manter o formato /privado:dest:msg
                            // para que o servidor possa identificá-lo e processá-lo corretamente.
                            String serverExpectedContent = "/privado:" + recipientName + ":" + privateContent;
                            
                            // Cria o objeto Mensagem com o remetente, o destinatário extraído,
                            // e o conteúdo formatado para o servidor.
                            msg = new Mensagem(username, recipientName, serverExpectedContent);
                        } else {
                            // Se o formato do comando privado for inválido
                            System.out.println("[CLIENTE] Formato inválido para mensagem privada. Use /privado:destinatario:mensagem");
                            // Envia a mensagem como pública para não interromper o fluxo ou poderia simplesmente não enviar.
                            // Optou-se por enviar como pública para feedback ao usuário.
                            msg = new Mensagem(username, null, input + " (formato privado inválido)");
                        }
                    } catch (Exception e) {
                        // Em caso de erro inesperado no parsing do comando privado
                        System.out.println("[CLIENTE] Erro ao processar comando privado. Enviando como mensagem pública.");
                        msg = new Mensagem(username, null, input); // Envia como mensagem pública
                    }
                } else {
                    // Se não for comando privado, é uma mensagem pública (broadcast)
                    msg = new Mensagem(username, null, input); // Destinatário null indica broadcast
                }

                out.writeObject(msg); // Envia o objeto Mensagem para o servidor
            }
        } catch (Exception e) { // Captura qualquer exceção durante o envio
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            // A thread principal pode terminar aqui se o Scanner falhar,
            // mas a MessageReceiver continuaria tentando ler se a conexão ainda estiver ativa.
        }
    }

    // Classe interna que implementa Runnable para ser executada em uma nova thread
    // Responsável por receber mensagens do servidor continuamente
    private class MessageReceiver implements Runnable {
        private ObjectInputStream in; // Stream de entrada para ler objetos do servidor

        // Construtor que recebe o stream de entrada
        public MessageReceiver(ObjectInputStream in) {
            this.in = in;
        }

        @Override
        public void run() { // Código executado na nova thread
            try {
                while (true) { // Loop infinito para continuar recebendo mensagens
                    // Lê um objeto Mensagem do servidor (bloqueia até receber)
                    Mensagem msg = (Mensagem) in.readObject();
                    // Imprime a mensagem no console. O método toString() da classe Mensagem
                    // deve formatar a mensagem adequadamente (incluindo remetente, timestamp, etc.).
                    System.out.println(msg);
                }
            } catch (EOFException | SocketException e) { // Exceções comuns de desconexão
                 System.err.println("Conexão com o servidor perdida ou fechada.");
            } catch (IOException | ClassNotFoundException e) { // Outros erros de I/O ou de classe
                System.err.println("Erro ao receber mensagem: " + e.getMessage());
            } catch (Exception e) { // Captura genérica para outros erros inesperados
                System.err.println("Erro inesperado no recebimento: " + e.getMessage());
            }
            // Se sair do loop (devido a uma exceção), a thread termina.
            System.err.println("Thread de recebimento de mensagens encerrada.");
        }
    }
}
