package org.unifor;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Solicitar nome do usuario
        System.out.println("Digite seu nome do usuario: ");
        String userName = scanner.nextLine();

        // Solicitar porta
        System.out.println("Digite a porta: ");
        int port = scanner.nextInt();
        scanner.nextLine();

        // Inicializar o Peer
        Peer peer = new Peer(userName, port);
        peer.start();

        // Perguntar se deseja conectar a outro peer
        // Criar tratamento para caso o usuário escolha "n"
        System.out.println("Deseja conectar a outro peer? s/n: ");
        String resposta = scanner.nextLine();

        if(resposta.equalsIgnoreCase("s")){
            System.out.println("Digite o endereco do peer (host): ");
            String peerHost = scanner.nextLine();

            System.out.println("Digite a porta do peer (port): ");
            int peerPort = scanner.nextInt();
            scanner.nextLine();

            peer.connectionToPeer(peerHost, peerPort);
        }
        scanner.close();

    }
}
