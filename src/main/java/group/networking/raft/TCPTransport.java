package group.networking.raft;

import io.microraft.RaftEndpoint;
import io.microraft.model.message.RaftMessage;
import io.microraft.transport.Transport;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TCPTransport implements Transport {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private TCPEndpoint localEndpoint = null;

    //mTLS was first done in raftServer. Most everything here(for MTLS) is just copy pasted from there.
    private final String certInputStream;
    private final String certificatePass;
    private final String trustInputStream;
    private final String trustPass;

    private final SSLSocketFactory sslSocketFactory;

    public TCPTransport(TCPEndpoint localEndpoint) {
        this.localEndpoint = localEndpoint;
        this.certInputStream = null;
        this.certificatePass = null;
        this.trustInputStream = null;
        this.trustPass = null;
        this.sslSocketFactory = null;
    }

    public TCPTransport(TCPEndpoint localEndpoint, String certInputStream, String certificatePass, String trustInputStream, String trustPass) throws Exception {

        this.localEndpoint = localEndpoint;
        this.certInputStream = certInputStream;
        this.certificatePass = certificatePass;
        this.trustInputStream = trustInputStream;
        this.trustPass = trustPass;

        this.sslSocketFactory = createSSLContext().getSocketFactory();
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
	if (sslSocketFactory == null) {
        System.err.println("[TcpTransport] SSL not initialized. I will not be sending that.");
        return;
    	}
	
        try (Socket socket = sslSocketFactory.createSocket(target.getHost(), target.getPort());
         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
        out.writeObject(message);
        out.flush();
    	} catch (Exception e) {
        System.err.println("[TcpTransport] Secure send failed: " + e.getMessage());
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

        try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket()) {
            socket.connect(
                new java.net.InetSocketAddress(tcpEndpoint.getHost(), tcpEndpoint.getPort()),
                500 // ms
            );
            socket.startHandshake();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}