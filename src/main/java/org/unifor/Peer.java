package org.unifor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Representa um nó (peer) na rede P2P. É responsável por ouvir por conexões,
 * iniciar conexões com outros peers e transmitir mensagens.
 */
public class Peer {
    private final String userName;
    private final String peerId;
    private final ServerSocket serverSocket;
    private final ChatHistory history;

    // Lista thread-safe para armazenar todas as conexões ativas.
    private List<PeerConnection> connections = new CopyOnWriteArrayList<>();

    /**
     * Constrói um Peer, iniciando um ServerSocket para ouvir na porta especificada.
     *
     * @param userName O nome de usuário para este peer.
     * @param port     A porta em que o servidor irá ouvir.
     * @throws IOException Se a porta já estiver em uso ou ocorrer outro erro de I/O.
     */
    public Peer(String userName, int port) throws IOException {
        String id = PeerIdentity.getPeerId(userName);
        if (id == null) {
            id = PeerIdentity.createPeerId(userName);
            System.out.println("[INFO] Novo ID criado para " + userName + ": " + id);
        } else {
            System.out.println("[INFO] ID existente encontrado para " + userName + ": " + id);
        }
        this.peerId = id;

        this.userName = userName;
        try {
            // Inicia o servidor para ouvir em todas as interfaces de rede disponíveis ("0.0.0.0").
            // Isso aumenta a robustez em máquinas com múltiplas placas de rede.
            this.serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
            this.history = new ChatHistory(peerId);
            System.out.println("[INFO] Peer '" + userName + "' está ouvindo na porta " + port);
        } catch (IOException e) {
            System.err.println("[ERRO CRÍTICO] Não foi possível ouvir na porta " + port + ". Ela pode já estar em uso.");
            throw e; // Relança a exceção para que a classe Main possa tratá-la.
        }
    }

    /**
     * Inicia a thread principal do servidor para aceitar novas conexões.
     */
    public void start() {
        new Thread(this::listenForConnections).start();
    }

    /**
     * Loop infinito que aguarda e aceita novas conexões de entrada.
     * Para cada nova conexão, uma nova thread de tratamento é iniciada.
     */
    private void listenForConnections() {
        while (true) {
            try {
                Socket socket = serverSocket.accept(); // Bloqueia até uma nova conexão chegar.
                PeerConnection pc = new PeerConnection(socket);
                connections.add(pc);
                System.out.println("[INFO] Nova conexão recebida de " + socket.getRemoteSocketAddress());
                new Thread(() -> handleConnection(pc)).start();
            } catch (IOException e) {
                System.err.println("[ERRO] Falha ao aceitar nova conexão: " + e.getMessage());
            }
        }
    }

    /**
     * Lida com a comunicação de um peer específico, lendo as mensagens recebidas.
     * Este método roda em sua própria thread para cada conexão.
     *
     * @param pc A conexão do peer a ser tratada.
     */
    private void handleConnection(PeerConnection pc) {
        try {
            String pendingMessage = null;

            // Aguarda até receber um /id: válido
            while (pc.remotePeerId == null) {
                String firstLine = pc.in.readLine();
                if (firstLine == null) {
                    throw new IOException("Conexão encerrada antes de enviar ID");
                }
                if (firstLine.startsWith("/id:")) {
                    pc.remotePeerId = firstLine.substring(4).trim();
                    System.out.println("[INFO] Conectado ao peer remoto com ID: " + pc.remotePeerId);

                    // Resposta
                    pc.out.println("/id:" + this.peerId);

                    // Carrega histórico, caso exista
                    var oldMessages = history.loadHistory(pc.remotePeerId);
                    if (!oldMessages.isEmpty()) {
                        System.out.println("[INFO] Histórico de mensagens com este peer:");
                        oldMessages.forEach(System.out::println);
                    }

                    // Se havia uma mensagem recebida antes do ID, processa agora
                    if (pendingMessage != null) {
                        System.out.println(pendingMessage);
                        history.saveMessage(pc.remotePeerId, pendingMessage);
                    }
                } else {
                    pendingMessage = firstLine;
                }
            }

            String message;
            while ((message = pc.in.readLine()) != null) {
                System.out.println(message); // Imprime a mensagem recebida (já formatada).
                history.saveMessage(pc.remotePeerId, message);
            }
        } catch (IOException e) {
            System.out.println("[AVISO] Conexão com " + pc.socket.getRemoteSocketAddress() + " foi perdida.");
        } finally {
            // Bloco crucial para remover a conexão da lista quando ela for encerrada.
            // Isso evita o problema de "conexões zumbis".
            connections.remove(pc);
            System.out.println("[INFO] Conexão com " + pc.socket.getRemoteSocketAddress() + " foi encerrada.");
        }
    }

    /**
     * Envia uma mensagem para todos os peers conectados.
     *
     * @param message A mensagem a ser enviada.
     */
    public void broadcastMessage(String message) {
        String formattedMessage = "[" + userName + "]: " + message;
        for (PeerConnection pc : connections) {
            pc.out.println(formattedMessage);
            if (pc.remotePeerId != null) {
                history.saveMessage(pc.remotePeerId, formattedMessage);
            } else {
                history.saveMessage(pc.socket.getRemoteSocketAddress().toString(), formattedMessage);
            }
        }
    }

    /**
     * Inicia uma nova conexão de saída para outro peer na rede.
     *
     * @param host O endereço IP ou nome do host do peer de destino.
     * @param port A porta do peer de destino.
     */
    public void connectToPeer(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            PeerConnection pc = new PeerConnection(socket);

            pc.out.println("/id:" + this.peerId);

            String hello = pc.in.readLine();
            if (hello != null && hello.startsWith("/id:")) {
                pc.remotePeerId = hello.substring(4).trim();
                System.out.println("[INFO] Recebido peerId remoto: " + pc.remotePeerId);
            } else {
                System.out.println("[WARN] Não recebeu peerId do remoto.");
            }

            connections.add(pc);

            // Carrega histórico já com o peerId conhecido
            if (pc.remotePeerId != null) {
                var old = history.loadHistory(pc.remotePeerId);
                if (!old.isEmpty()) {
                    System.out.println("[INFO] Histórico com este peer:");
                    old.forEach(System.out::println);
                }
            }

            new Thread(() -> handleConnection(pc)).start();
            System.out.println("[INFO] Conectado com sucesso ao peer em " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("[ERRO] Falha ao conectar ao peer " + host + ":" + port + ". Motivo: " + e.getMessage());
        }
    }

    /**
     * Classe interna para encapsular todos os objetos relacionados a uma única conexão.
     */
    private static class PeerConnection {
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        String remotePeerId;

        PeerConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true); // autoFlush = true
        }
    }
}