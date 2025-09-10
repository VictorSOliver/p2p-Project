package org.unifor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Peer {
    private String userName;
    private ServerSocket serverSocket;

    // Agora a lista guarda objetos PeerConnection
    private List<PeerConnection> connections = new ArrayList<>();

    public Peer(String userName, int port) {
        this.userName = userName;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[INFO] Peer " + userName + " está ouvindo na porta " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(this::listenForConnections).start();
        new Thread(this::listenForUserInput).start();
        System.out.println("[DEBUG] Peer " + userName + " iniciado");
    }

    private void listenForConnections() {
        System.out.println("[DEBUG] Entrou em listenForConnections()");
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                PeerConnection pc = new PeerConnection(socket);
                connections.add(pc);

                System.out.println("[INFO] Nova conexão recebida de " + socket.getRemoteSocketAddress());

                new Thread(() -> handleConnection(pc)).start();
            } catch (IOException e) {
                System.out.println("[ERROR] Falha ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    private void handleConnection(PeerConnection pc) {
        try {
            String message;
            while ((message = pc.in.readLine()) != null) {
                System.out.println("[RECEBIDO de " + pc.socket.getRemoteSocketAddress() + "] " + message);
            }
            System.out.println("[INFO] Conexão encerrada com " + pc.socket.getRemoteSocketAddress());
        } catch (IOException e) {
            System.out.println("[WARN] Conexão perdida: " + pc.socket.getRemoteSocketAddress());
        }
    }

    private void listenForUserInput() {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String message = userInput.readLine();
                broadcastMessage(message);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Falha ao ler entrada do usuário");
        }
    }

    private void broadcastMessage(String message) {
        for (PeerConnection pc : connections) {
            pc.out.println(userName + ": " + message);
            System.out.println("[DEBUG] Enviado para " + pc.socket.getRemoteSocketAddress() + ": " + message);
        }
    }

    public void connectToPeer(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            PeerConnection pc = new PeerConnection(socket);
            connections.add(pc);

            new Thread(() -> handleConnection(pc)).start();
            System.out.println("[INFO] Conectado ao peer em " + host + ":" + port);
        } catch (IOException e) {
            System.out.println("[ERROR] Erro ao conectar ao peer em " + host + ":" + port);
        }
    }

    // Classe auxiliar para encapsular a conexão
    private static class PeerConnection {
        Socket socket;
        BufferedReader in;
        PrintWriter out;

        PeerConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }
    }
}
