package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class EchoServer {
    private final int port;
    private volatile boolean isRunning;

    public EchoServer(int port) {
        this.port = port;
    }

    public void start() {
        isRunning = true;
        try (ServerSocket serverSocket = newServerSocket(1000)) {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                waitOrHandleConnectionOn(serverSocket);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void waitOrHandleConnectionOn(ServerSocket serverSocket) throws IOException {
        try (Socket clientSocket = serverSocket.accept();
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                out.println(line);
            }
        } catch (SocketTimeoutException ignore) {
        }
    }

    private ServerSocket newServerSocket(int timeoutMs) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(timeoutMs);
        return serverSocket;
    }

    public void stop() {
        isRunning = false;
    }
}
