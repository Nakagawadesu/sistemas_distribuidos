package chatTcp; // Define o pacote onde a classe está localizada.

import java.io.*; // Importa classes para entrada e saída de dados (Input/Output).
import java.net.*; // Importa classes para programação de rede (ex: Sockets, ServerSocket).
import java.util.*; // Importa classes de utilidades (ex: Map, para armazenar clientes).
import java.util.concurrent.ConcurrentHashMap; // Importa uma implementação de Map que é segura para uso concorrente (múltiplas threads).

// Classe principal do servidor de chat.
public class ChatServer {
    private static final int PORT = 12345; // Define a porta em que o servidor escutará por conexões. É uma constante.
    // Mapa para armazenar os manipuladores de cliente (ClientHandler) conectados.
    // A chave é o nome de usuário (String) e o valor é a instância de ClientHandler.
    // ConcurrentHashMap é usado para evitar problemas de concorrência quando múltiplas threads acessam o mapa.
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    // Método principal da aplicação servidor. É o ponto de entrada quando o servidor é executado.
    public static void main(String[] args) {
        // Usa try-with-resources: o ServerSocket será fechado automaticamente ao final do bloco try ou se ocorrer uma exceção.
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT); // Exibe uma mensagem no console indicando que o servidor iniciou.

            // Loop infinito para continuar aceitando novas conexões de clientes.
            while (true) {
                // O método accept() bloqueia a execução até que um novo cliente se conecte.
                // Quando um cliente se conecta, um objeto Socket é retornado, representando a conexão com esse cliente.
                Socket clientSocket = serverSocket.accept();
                // Para cada cliente que se conecta, uma nova instância de ClientHandler é criada.
                // ClientHandler é uma Thread, então .start() inicia a execução do método run() dessa thread.
                // Isso permite que o servidor lide com múltiplos clientes simultaneamente.
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            // Captura exceções de Entrada/Saída que podem ocorrer ao tentar criar o ServerSocket (ex: porta já em uso).
            System.err.println("Erro no servidor: " + e.getMessage()); // Exibe a mensagem de erro no console de erro.
        }
    }

    // Classe interna estática que estende Thread. Cada instância desta classe manipula a comunicação com um cliente conectado.
    static class ClientHandler extends Thread {
        private Socket socket; // O socket para este cliente específico.
        private ObjectOutputStream out; // Stream para enviar objetos (Mensagem) para este cliente.
        private String username; // O nome de usuário deste cliente.

        // Construtor da classe ClientHandler. Recebe o socket do cliente conectado.
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        // O método run() contém a lógica que será executada pela thread do ClientHandler.
        @Override
        public void run() {
            // Usa try-with-resources: o ObjectInputStream será fechado automaticamente.
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) { // Stream para receber objetos do cliente.
                // Cria o ObjectOutputStream para enviar dados para o cliente.
                out = new ObjectOutputStream(socket.getOutputStream());

                // A primeira mensagem enviada pelo cliente deve ser uma instância de Mensagem contendo o nome de usuário.
                Mensagem loginMsg = (Mensagem) in.readObject(); // Lê o objeto Mensagem do cliente.
                this.username = loginMsg.getRemetente(); // Extrai o nome de usuário do remetente da mensagem.
                clients.put(username, this); // Adiciona este ClientHandler ao mapa de clientes ativos, usando o username como chave.
                // Envia uma mensagem de sistema para todos os clientes informando que um novo usuário entrou.
                broadcastSystemMessage(username + " entrou no chat!");
                System.out.println(username + " conectou-se. IP: " + socket.getInetAddress().getHostAddress()); // Log no console do servidor

                // Loop infinito para continuar lendo e processando mensagens deste cliente.
                while (true) {
                    Mensagem msg = (Mensagem) in.readObject(); // Lê a próxima mensagem enviada pelo cliente.
                    processMessage(msg); // Chama o método para processar a mensagem recebida.
                }
            } catch (EOFException | SocketException e) { // Captura exceções que geralmente indicam desconexão do cliente.
                System.out.println(username + " desconectou-se (EOF/SocketException)."); // Log de desconexão
            } catch (IOException | ClassNotFoundException e) { // Captura outros erros de I/O ou se a classe Mensagem não for encontrada.
                System.err.println("Erro no handler do cliente " + (username != null ? username : "[NÃO LOGADO]") + ": " + e.getMessage());
            } catch (Exception e) { // Captura genérica para outras exceções inesperadas.
                 System.err.println("Exceção inesperada no handler do cliente " + (username != null ? username : "[NÃO LOGADO]") + ": " + e.getMessage());
                 e.printStackTrace(); // Imprime o stack trace para depuração.
            }
            finally {
                // Bloco finally é sempre executado, garantindo que o cliente seja desconectado e removido.
                disconnectClient();
            }
        }

        // Método para processar uma mensagem recebida do cliente e decidir o que fazer com ela.
        private void processMessage(Mensagem msg) throws IOException {
            System.out.println("Msg de " + username + ": " + msg.getConteudo() + " (Dest: "+msg.getDestinatario()+")"); // Log no servidor
            // Verifica se o conteúdo da mensagem começa com "/privado:", indicando uma mensagem privada.
            if (msg.getConteudo().startsWith("/privado:")) {
                sendPrivateMessage(msg); // Chama o método para tratar mensagens privadas.
            } else if (msg.getConteudo().equals("/usuarios")) { // Verifica se é o comando para listar usuários.
                sendUserList(); // Chama o método para enviar a lista de usuários.
            } else {
                // Se não for um comando conhecido, é uma mensagem pública para ser transmitida a todos (broadcast).
                // É importante recriar a mensagem ou garantir que o remetente esteja correto antes do broadcast.
                // No código original, msg.getRemetente() já seria o username do cliente que enviou,
                // mas o campo destinatario do objeto msg original do cliente poderia estar preenchido.
                // Para broadcast, o destinatario deve ser null.
                broadcastMessage(new Mensagem(this.username, null, msg.getConteudo()));
            }
        }

        // Método para tratar e enviar uma mensagem privada.
        private void sendPrivateMessage(Mensagem msg) { // O conteúdo da msg aqui é esperado como "/privado:destinatario:mensagem_de_texto"
            String[] parts = msg.getConteudo().split(":", 3); // Divide a string do conteúdo em até 3 partes usando ":" como delimitador.
            // parts[0] seria "/privado", parts[1] o destinatário, parts[2] a mensagem.
            if (parts.length >= 3) { // Verifica se o formato do comando está correto.
                String recipientUsername = parts[1].trim(); // Extrai o nome do destinatário e remove espaços.
                String content = parts[2].trim();   // Extrai o conteúdo da mensagem e remove espaços.

                // Cria uma nova instância de Mensagem. O remetente é o usuário atual (this.username).
                // O destinatário é 'recipientUsername' e o conteúdo é 'content' (a mensagem limpa).
                Mensagem privateMsgToSend = new Mensagem(this.username, recipientUsername, content);

                ClientHandler recipientClient = clients.get(recipientUsername); // Tenta obter o ClientHandler do destinatário a partir do mapa.
                if (recipientClient != null) { // Verifica se o destinatário está conectado.
                    try {
                        recipientClient.out.writeObject(privateMsgToSend); // Envia a mensagem privada para o ObjectOutputStream do destinatário.
                        System.out.println("Msg privada de " + this.username + " para " + recipientUsername + " enviada."); // Log
                    } catch (IOException e) {
                        System.err.println("Erro ao enviar mensagem privada de " + this.username + " para " + recipientUsername + ": " + e.getMessage());
                    }
                } else { // Se o destinatário não for encontrado no mapa (não está online ou não existe).
                    try {
                        // Envia uma mensagem de volta para o remetente informando que o usuário não foi encontrado.
                        this.out.writeObject(new Mensagem("Servidor", this.username, "Usuário '" + recipientUsername + "' não encontrado ou offline."));
                         System.out.println(this.username + " tentou enviar msg para " + recipientUsername + " (offline)."); // Log
                    } catch (IOException e) {
                        System.err.println("Erro ao notificar remetente sobre usuário offline: " + e.getMessage());
                    }
                }
            } else { // Se o formato do comando "/privado" estiver incorreto.
                try {
                    this.out.writeObject(new Mensagem("Servidor", this.username, "Formato inválido para mensagem privada. Use /privado:destinatario:mensagem"));
                } catch (IOException e) {
                    System.err.println("Erro ao notificar remetente sobre formato inválido de msg privada: " + e.getMessage());
                }
            }
        }

        // Método para enviar a lista de usuários conectados de volta para o cliente que solicitou.
        private void sendUserList() throws IOException {
            // Cria uma string com os nomes de todos os usuários conectados (chaves do mapa 'clients'), separados por nova linha.
            String userListString = "Usuários conectados:\n" + String.join("\n", clients.keySet());
            // Envia a lista como uma mensagem do "Servidor" para o usuário que fez a solicitação (this.username).
            out.writeObject(new Mensagem("Servidor", this.username, userListString));
            System.out.println("Lista de usuários enviada para " + this.username); // Log
        }

        // Método para enviar uma mensagem para todos os outros clientes conectados (broadcast).
        private void broadcastMessage(Mensagem msg) { // Removido throws IOException para tratar dentro do loop
            System.out.println("Broadcast de " + msg.getRemetente() + " para todos: " + msg.getConteudo()); // Log
            // Itera sobre todos os ClientHandlers no mapa 'clients'.
            for (ClientHandler client : clients.values()) {
                // Não envia a mensagem de volta para o cliente que a originou.
                if (!client.username.equals(this.username)) {
                    try {
                        client.out.writeObject(msg); // Envia a mensagem para o ObjectOutputStream do outro cliente.
                    } catch (IOException e) {
                        // Se ocorrer um erro ao enviar para um cliente específico (ex: ele desconectou),
                        // exibe um erro mas continua tentando enviar para os demais.
                        System.err.println("Erro ao fazer broadcast para " + client.username + ": " + e.getMessage());
                        // Poderia-se adicionar lógica aqui para remover clientes que causam erro consistentemente.
                    }
                }
            }
        }

        // Método para enviar uma mensagem de sistema para TODOS os clientes conectados (incluindo o originador, se aplicável).
        private void broadcastSystemMessage(String messageText) { // Removido throws IOException para tratar dentro do loop
            System.out.println("Mensagem de Sistema (broadcast): " + messageText); // Log
            // Cria uma nova Mensagem com "Servidor" como remetente e sem destinatário específico (broadcast).
            Mensagem systemMsg = new Mensagem("Servidor", null, messageText);
            // Itera sobre todos os ClientHandlers.
            for (ClientHandler client : clients.values()) {
                try {
                    client.out.writeObject(systemMsg); // Envia a mensagem de sistema.
                } catch (IOException e) {
                     System.err.println("Erro ao enviar mensagem de sistema para " + client.username + ": " + e.getMessage());
                }
            }
        }

        // Método para lidar com a desconexão de um cliente.
        private void disconnectClient() {
            try {
                if (username != null) { // Verifica se o username foi definido (ou seja, se o cliente chegou a se "logar").
                    clients.remove(username); // Remove o cliente do mapa de clientes ativos.
                    // Envia uma mensagem de sistema para todos informando que o usuário saiu.
                    broadcastSystemMessage(username + " saiu do chat!");
                    System.out.println(username + " desconectado(a) e removido(a) do servidor."); // Log no console do servidor.
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close(); // Fecha o socket deste cliente.
                }
            } catch (Exception e) { // Captura qualquer exceção que possa ocorrer durante a desconexão.
                System.err.println("Erro ao desconectar cliente " + (username != null ? username : "[NÃO LOGADO]") + ": " + e.getMessage());
            } finally {
                // Garante que os streams sejam fechados, embora o fechamento do socket já deva cuidar disso.
                // Isso é uma boa prática para liberar recursos.
                try { if (out != null) out.close(); } catch (IOException e) { /* ignora */ }
                // O 'in' é fechado pelo try-with-resources no método run().
            }
        }
    }
}