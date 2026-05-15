package group.networking.raft;

import io.microraft.RaftNode;
import io.microraft.model.message.RaftMessage;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class RaftServer {

    private final int port;
    private RaftNode raftNode;

    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ExecutorService connectionPool = Executors.newCachedThreadPool();

    private final String certInputStream;
    private final String certificatePass;
    private final String trustInputStream;
    private final String trustPass;

    public RaftServer(int port) {
        this(port,"node.p12", "password", "truststore.p12", "password");
    }

    public RaftServer(int port, String certInputStream, String certificatePass, String trustInputStream, String trustPass) {
        this.port = port;
        this.certInputStream = certInputStream;
        this.certificatePass = certificatePass;
        this.trustInputStream = trustInputStream;
        this.trustPass = trustPass;
    }

    public void start(RaftNode node) throws Exception {
        this.raftNode = node;
	if (certInputStream == null || trustInputStream == null) {
        throw new IllegalStateException("mTLS certificates are missing! You dont belong here.");
   	 }
         SSLContext sslContext = createSSLContext();
         SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
         SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
         sslServerSocket.setNeedClientAuth(true);
         this.serverSocket = sslServerSocket;
                

        this.running.set(true);

        Thread acceptThread = new Thread(this::acceptLoop, "raft-server-" + port);
        acceptThread.setDaemon(true);
        acceptThread.start();

        System.out.println("[RaftServer] Listening on port " + port);
    }

    public SSLContext createSSLContext() throws Exception{
        //SSLContexts get described as "factories" for SSLSockets. They use keystores to do this.
        //TLSv1.3 is just the version of tls being used. Could be 1.0 or 1.2.
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        //Keystores... store keys and certificates.
        //To my understanding. JKS is a java specific keystore, pkcs12 is industry standard, so lets use that one.
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream keyStoreFile = new FileInputStream(certInputStream)) {
            keyStore.load(keyStoreFile, certificatePass.toCharArray());
        }
        //KeyManagerFactory. Factory for key managers which gets put into the previous SSLContext factory...
        //How many factories can there possibly be..
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, certificatePass.toCharArray());

        //Server Certificates. Same as above really, just server side.
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream trustStoreFile = new FileInputStream(trustInputStream)) {
            trustStore.load(trustStoreFile, trustPass.toCharArray());
        }
        //Are we even surprised that theres another factory.. It makes... trust managers.
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext;
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