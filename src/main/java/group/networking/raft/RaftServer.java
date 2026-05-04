package group.networking.raft;

import io.microraft.RaftNode;
import io.microraft.model.message.RaftMessage;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class RaftServer {

    private final int port;
    private RaftNode raftNode;

    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ExecutorService connectionPool = Executors.newCachedThreadPool();

    public RaftServer(int port) {
        this.port = port;
    }

    public void start(RaftNode node) throws Exception {
        this.raftNode = node;
        this.serverSocket = new ServerSocket(port);
        this.running.set(true);

        Thread acceptThread = new Thread(this::acceptLoop, "raft-server-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();

        System.out.println("[RaftServer] Listening on port " + port);
    }


    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                connectionPool.submit(() -> handleConnection(clientSocket));
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[RaftServer] Accept error: " + e.getMessage());
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Object obj = in.readObject();

            if (obj instanceof RaftMessage message) {
                raftNode.handle(message);
            } else {
                System.err.println("[RaftServer] Received unexpected object type: "
                        + obj.getClass().getName());
            }

        } catch (Exception e) {
            System.err.println("[RaftServer] Error handling connection: " + e.getMessage());
        }
    }

    public void stop() {
        running.set(false);
        connectionPool.shutdownNow();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.err.println("[RaftServer] Error closing server socket: " + e.getMessage());
        }
        System.out.println("[RaftServer] Stopped on port " + port);
    }
}