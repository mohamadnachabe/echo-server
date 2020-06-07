import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;


public class EchoServer {

    public static void main(String[] args) throws InterruptedException {

        CountDownLatch serverStarted = new CountDownLatch(1);
        CountDownLatch serverExited = new CountDownLatch(1);

        startServer(serverStarted, serverExited);

        serverStarted.await();

        runClients();

        serverExited.await();

    }

    private static void runClients() {
        int clientNumber = 1;
        ExecutorService clientPool = Executors.newFixedThreadPool(clientNumber);

        clientPool.submit(() -> {
            try(
                    Socket socket = new Socket("127.0.0.1", 23333);
                    PrintWriter out =
                            new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
            ) {
                out.write("Hello World\n");
                out.flush();
                out.write("exit");
                out.flush();
                System.out.println(in.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void startServer(CountDownLatch serverStarted, CountDownLatch serverExited) {
        ExecutorService serverExecutor = Executors.newFixedThreadPool(1);
        serverExecutor.submit(() -> {

            ExecutorService connectionPool = Executors.newFixedThreadPool(2);

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                ThreadPoolExecutor threadPool = (ThreadPoolExecutor) connectionPool;
                System.out.println(String.format("active: %s; max: %s",
                        threadPool.getActiveCount(), threadPool.getMaximumPoolSize()));
            }, 0, 1, TimeUnit.SECONDS);

            try(ServerSocket serverSocket = new ServerSocket(23333)) {
                serverStarted.countDown();
                System.out.println("LISTENING");

                while(serverExited.getCount() != 0) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("CONNECTED");

                        SocketRunnable socketRunnable = new SocketRunnable(clientSocket);
                        connectionPool.submit(socketRunnable);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    static class SocketRunnable implements Runnable {

        private Socket clientSocket;

        public SocketRunnable(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            System.out.println("READING");
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out =
                         new PrintWriter(clientSocket.getOutputStream(), true);
            ) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("exit")) {
                        break;
                    }

                    System.out.println(String.format("Server received: %s", line));
                    out.write(String.format("Server echo: %s\n", line));
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
