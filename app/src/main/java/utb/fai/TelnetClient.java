package utb.fai;

import java.io.*;
import java.net.*;

public class TelnetClient {
    private String serverIp;
    private int port;
    private volatile boolean running = true;

    public TelnetClient(String serverIp, int port) {
        this.serverIp = serverIp;
        this.port = port;
    }

    public void run() {
        try (Socket socket = new Socket(serverIp, port)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            // Vlakno 1: Cteni ze soketu a okamzity vypis na System.out
            Thread receiverThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while (running) {
                        if (in.available() > 0) {
                            int bytesRead = in.read(buffer);
                            if (bytesRead != -1) {
                                System.out.write(buffer, 0, bytesRead);
                                System.out.flush(); // Donuti Javu okamzite predat data NATT testu
                            }
                        } else {
                            Thread.sleep(20);
                        }
                    }
                } catch (IOException e) {
                    // Spojeni ukonceno
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            // Vlakno 2: Cteni ze System.in a odesilani na soket
            Thread senderThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String inputLine;
                    while (running && (inputLine = reader.readLine()) != null) {
                        if ("/QUIT".equalsIgnoreCase(inputLine.trim())) {
                            running = false;
                            break;
                        }

                        out.write((inputLine + "\r\n").getBytes());
                        out.flush();
                    }
                } catch (IOException e) {
                    // Chyba pri cteni ze vstupu
                }
            });

            receiverThread.start();
            senderThread.start();

            // Pockame na dokonceni odesilaciho vlakna (napr. pri /QUIT)
            senderThread.join();

            // Ukonceni prijmoveho vlakna
            running = false;
            receiverThread.interrupt();

        } catch (IOException e) {
            System.err.println("Chyba spojeni: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}