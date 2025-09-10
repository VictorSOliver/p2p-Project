package org.unifor;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Solicitar nome do usuário
        System.out.print("Digite seu nome do usuário: ");
        String userName = scanner.nextLine();

        // Solicitar porta
        System.out.print("Digite a porta do seu peer: ");
        int port = Integer.parseInt(scanner.nextLine());

        // Inicializar o Peer
        Peer peer = new Peer(userName, port);
        peer.start();

        // Loop para conectar a múltiplos peers
        while (true) {
            System.out.print("Deseja conectar a outro peer? (s/n): ");
            String resposta = scanner.nextLine().trim();

            if (resposta.equalsIgnoreCase("s")) {
                System.out.print("Digite o endereço do peer (host): ");
                String peerHost = scanner.nextLine().trim();

                System.out.print("Digite a porta do peer: ");
                int peerPort = Integer.parseInt(scanner.nextLine().trim());

                peer.connectToPeer(peerHost, peerPort);
            } else if (resposta.equalsIgnoreCase("n")) {
                break; // Sai do loop de conexão, continua apenas com chat
            } else {
                System.out.println("Opção inválida. Digite 's' ou 'n'.");
            }
        }

        // Agora o peer continua rodando, enviando mensagens digitadas pelo usuário
        System.out.println("[INFO] Você pode digitar mensagens para enviar a todos os peers conectados.");
    }
}
