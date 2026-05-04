package group.networking.raft;

import io.microraft.RaftEndpoint;
import io.microraft.model.message.RaftMessage;
import io.microraft.transport.Transport;

import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TCPTransport implements Transport {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private TCPEndpoint localEndpoint = null;

    public TCPTransport(TCPEndpoint localEndpoint) {
        this.localEndpoint = localEndpoint;
    }


    @Override
    public void send(RaftEndpoint target, RaftMessage message) {
        if (!(target instanceof TCPEndpoint tcpTarget)) {
            System.err.println("[TcpTransport] send() called with non-TcpEndpoint: "
                    + target.getClass().getName());
            return;
        }

        executor.submit(() -> doSend(tcpTarget, message));
    }


    private void doSend(TCPEndpoint target, RaftMessage message) {
        try (Socket socket = new Socket(target.getHost(), target.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(message);
            out.flush();

        } catch (Exception e) {
            System.err.println("[TcpTransport] Failed to send to "
                    + target + ": " + e.getMessage());
        }
    }


    @Override
    public boolean isReachable(RaftEndpoint endpoint) {
        if (!(endpoint instanceof TCPEndpoint tcpEndpoint)) {
            return false;
        }

        // Don't probe ourselves
        if (tcpEndpoint.getId().equals(localEndpoint.getId())) {
            return true;
        }

        try (Socket socket = new Socket()) {
            socket.connect(
                new java.net.InetSocketAddress(tcpEndpoint.getHost(), tcpEndpoint.getPort()),
                500 // ms
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}