package server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public class EchoServerTest {
    private static final String HOST = "localhost";
    private static final int PORT = 6000;
    private ExecutorService executor;
    private EchoServer server;
    private Future<?> serverTask;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        server = new EchoServer(PORT);
        serverTask = executor.submit(server::start);
    }

    @Test
    void connect_whenServerIsStarted() throws IOException {
        try (Socket socket = new Socket(HOST, PORT)) {
            assertTrue(socket.isConnected());
        }
    }

    @Test
    void connect_whenServerIsStopped() throws ExecutionException, InterruptedException, TimeoutException {
        server.stop();
        serverTask.get(2, TimeUnit.SECONDS);

        assertThrows(ConnectException.class, () -> new Socket(HOST, PORT));
    }

    @Test
    void writeSingleLineToServer() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("Hello World!");

            assertEquals("Hello World!", in.readLine());
        }
    }

    @Test
    void writeMultipleLinesToServer() throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("Hello World!");
            out.println("How are you?");

            assertEquals("Hello World!", in.readLine());
            assertEquals("How are you?", in.readLine());
        }
    }

    @Test
    void writeDataFromMultipleClientsToServer() throws InterruptedException, ExecutionException, TimeoutException {
        int numClients = 1000;
        List<CompletableFuture<String>> results = IntStream.range(0, numClients)
                .mapToObj(EchoClient::new)
                .map(CompletableFuture::supplyAsync)
                .collect(toList());

        CompletableFuture.allOf(results.toArray(new CompletableFuture[0])).join();

        for (int i = 0; i < numClients; i++)
            assertEquals("client-" + i + " hello!", results.get(i).get(2, TimeUnit.SECONDS));
    }

    private static class EchoClient implements Supplier<String> {
        private final int id;

        EchoClient(int id) {
            this.id = id;
        }

        @Override
        public String get() {
            try (Socket socket = new Socket(HOST, PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println("client-" + id + " hello!");
                return in.readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }
}
