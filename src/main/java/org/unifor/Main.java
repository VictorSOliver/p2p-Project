package org.unifor;

import java.io.IOException;
import java.util.Scanner;

/**
 * Ponto de entrada da aplicação de Chat P2P.
 * Responsável por coletar a entrada do usuário e coordenar o objeto Peer.
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("--- Bem-vindo ao Chat P2P ---");

        try {
            System.out.print("Digite seu nome de usuário: ");
            String userName = scanner.nextLine();

            System.out.print("Digite a porta em que seu peer irá ouvir (ex: 8081): ");
            int port = Integer.parseInt(scanner.nextLine());

            Peer peer = new Peer(userName, port);
            peer.start(); // Inicia a thread do servidor para aceitar conexões.

            // --- MODO DE CONFIGURAÇÃO ---
            // Loop para permitir que o usuário se conecte a outros peers.
            while (true) {
                System.out.print("Deseja conectar a outro peer? (s/n): ");
                String resposta = scanner.nextLine().trim();

                if (resposta.equalsIgnoreCase("s")) {
                    System.out.print("Digite o endereço do peer (host): ");
                    String peerHost = scanner.nextLine().trim();

                    System.out.print("Digite a porta do peer: ");
                    try {
                        int peerPort = Integer.parseInt(scanner.nextLine().trim());
                        peer.connectToPeer(peerHost, peerPort);
                    } catch (NumberFormatException e) {
                        System.err.println("[ERRO] Porta inválida. Apenas números são permitidos.");
                    }
                } else if (resposta.equalsIgnoreCase("n")) {
                    break; // Sai do modo de configuração e entra no modo de chat.
                } else {
                    System.out.println("Opção inválida. Digite 's' ou 'n'.");
                }
            }

            // --- MODO DE CHAT ---
            // A thread principal agora é a única responsável por ler o console para enviar mensagens.
            System.out.println("\n[INFO] Configuração finalizada. O chat está ativo!");
            System.out.println("Digite suas mensagens e pressione Enter para enviar. (Digite '/sair' para encerrar)");
            while (true) {
                String message = scanner.nextLine();
                if (message == null || message.equalsIgnoreCase("/sair")) {
                    break; // Permite que o usuário saia do chat.
                } else {
                    peer.broadcastMessage(message);
                }

            }

        } catch (NumberFormatException e) {
            System.err.println("[ERRO CRÍTICO] A porta deve ser um número. Encerrando.");
        } catch (IOException e) {
            // Este erro é capturado se o Peer não conseguir iniciar (ex: porta já em uso).
            System.err.println("[ERRO CRÍTICO] Falha ao iniciar o peer. Encerrando.");
        } finally {
            System.out.println("Encerrando o chat...");
            scanner.close(); // Fecha o scanner ao final da execução.
        }
    }
}