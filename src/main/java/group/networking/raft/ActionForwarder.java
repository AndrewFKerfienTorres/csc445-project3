package group.networking.raft;

import group.networking.game.GameAction;
import io.microraft.RaftNode;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ActionForwarder {

    // Forward port = 27081 + nodeId  (e.g. node 1 = 27081, node 2 = 27082)
    public static int forwardPortFor(int nodeId) {
        return 27081 + nodeId;
    }

    private final int port;
    private RaftNode raftNode;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public ActionForwarder(int nodeId) {
        this.port = forwardPortFor(nodeId);
    }

    public void setRaftNode(RaftNode node) {
        this.raftNode = node;
    }

    public void start() {
        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                while (running) {
                    try {
                        serverSocket.setSoTimeout(500);
                        Socket conn = serverSocket.accept();
                        new Thread(() -> handle(conn), "forward-handler").start();
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        if (running) System.err.println("[ActionForwarder] Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[ActionForwarder] Could not bind to port " + port + ": " + e.getMessage());
            }
        }, "action-forwarder-" + port);
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    private void handle(Socket conn) {
        try (
            ObjectInputStream  in  = new ObjectInputStream(conn.getInputStream());
            DataOutputStream   out = new DataOutputStream(conn.getOutputStream())
        ) {
            GameAction action = (GameAction) in.readObject();
            try {
                Object result = raftNode.replicate(action).join().getResult();
                out.writeUTF("OK " + result);
            } catch (Exception e) {
                out.writeUTF("ERROR " + e.getMessage());
            }
            out.flush();
        } catch (Exception e) {
            System.err.println("[ActionForwarder] Handler error: " + e.getMessage());
        } finally {
            try { conn.close(); } catch (IOException ignored) {}
        }
    }
}