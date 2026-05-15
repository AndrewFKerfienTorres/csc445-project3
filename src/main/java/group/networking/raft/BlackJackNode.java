package group.networking.raft;

import group.networking.game.GameAction;
import io.microraft.RaftConfig;
import io.microraft.RaftEndpoint;
import io.microraft.RaftNode;
import io.microraft.RaftRole;
import io.microraft.exception.NotLeaderException;
import io.microraft.model.impl.DefaultRaftModelFactory;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


public class BlackJackNode {

    private static final int LOBBY_PORT   = 7777;
    private static final int RAFT_BASE    = 9000;
    private static final int MAX_RETRIES  = 15;
    private static final int RETRY_DELAY  = 100;

    private RaftNode raftNode;
    private String playerName;
    private int nodeId;
    private static TCPEndpoint actionEndpoint;

    public BlackJackNode(RaftNode raftNode, String playerName, int nodeId, TCPEndpoint actionEndpoint) {
        this.raftNode = raftNode;
        this.playerName = playerName;
        this.nodeId = nodeId;
        this.actionEndpoint = actionEndpoint;
    }

    public static void main(String[] args) throws Exception {
        Scanner console = new Scanner(System.in);

        System.out.println("");
        System.out.println("      Blackjack      ");
        System.out.println("");
        System.out.print("Your name: ");
        String playerName = console.nextLine().trim();

        System.out.print("Host or join? (host/join): ");
        String choice = console.nextLine().trim().toLowerCase();

        BlackJackNode game;
        if (choice.equals("host")) {
            game = startAsHost(playerName, console);
        } else {
            game = startAsJoiner(playerName, console);
        }

        if (game != null) {
            game.runConsole(console);
            game.raftNode.terminate();
        }
    }

    /**
     * Host starts a single-node Raft cluster, opens the lobby server,
     * and waits for joiners. Each joiner gets added via changeMembership().
     * When host types 'start', membership is locked and the game begins.
     */
    private static BlackJackNode startAsHost(String playerName, Scanner console) throws Exception {
        // Host is node 1
        int myNodeId = 1;
        String myIp  = getLocalIp();
        int raftPort  = RAFT_BASE + myNodeId;

        TCPEndpoint myEndpoint = new TCPEndpoint(String.valueOf(myNodeId), myIp, raftPort);
        List<RaftEndpoint> initialGroup = List.of(myEndpoint);

        System.out.println("[Host] Your IP is: " + myIp + " share this with players joining.");
        System.out.println("[Host] Starting single-node Raft cluster...");

        TCPTransport transport = new TCPTransport(myEndpoint);
        RaftServer raftServer  = new RaftServer(raftPort);

        RaftNode node = RaftNode.newBuilder()
                .setGroupId("blackjack")
                .setLocalEndpoint(myEndpoint)
                .setInitialGroupMembers(initialGroup)
                .setConfig(raftConfig())
                .setTransport(transport)
                .setStateMachine(new StateManager())
                .setModelFactory(new DefaultRaftModelFactory())
                .build();

        raftServer.start(node);
        node.start();

        ActionForwarder forwarder = new ActionForwarder(myNodeId);
        forwarder.setRaftNode(node);
        forwarder.start();

        waitForLeader(node);

        // Track all endpoints so we can hand them to node later for GameClient
        List<TCPEndpoint> allEndpoints = new ArrayList<>();
        allEndpoints.add(myEndpoint);

        AtomicInteger nextNodeId = new AtomicInteger(2);

        Map<Integer, Socket> joinerSocketMap = new ConcurrentHashMap<>();
        Map<Integer, TCPEndpoint> joinerEndpointMap = new ConcurrentHashMap<>();

        ServerSocket lobbySocket = new ServerSocket(LOBBY_PORT);
        System.out.println("[Lobby] Waiting for players to join. Type 'start' when ready.\n");

        List<Socket>  joinerSockets = new CopyOnWriteArrayList<>();
        AtomicBoolean lobbyOpen     = new AtomicBoolean(true);

        Thread acceptThread = new Thread(() -> {
            while (lobbyOpen.get()) {
                try {
                    lobbySocket.setSoTimeout(500);
                    Socket joinerSocket = lobbySocket.accept();
                    joinerSockets.add(joinerSocket);

                    int assignedId = nextNodeId.getAndIncrement();

                    new Thread(() -> {
                        try {
                            DataInputStream  in  = new DataInputStream(joinerSocket.getInputStream());
                            DataOutputStream out = new DataOutputStream(joinerSocket.getOutputStream());

                            String joinerName = in.readUTF();
                            String joinerIp   = in.readUTF();
                            int    joinerRaftPort = RAFT_BASE + assignedId;

                            TCPEndpoint joinerEndpoint = new TCPEndpoint(String.valueOf(assignedId), joinerIp, joinerRaftPort);

                            // Send joiner their assignment
                            out.writeInt(assignedId);
                            out.writeInt(allEndpoints.size());
                            for (TCPEndpoint ep : allEndpoints) {
                                out.writeUTF(ep.getId());
                                out.writeUTF(ep.getHost());
                                out.writeInt(ep.getPort());
                            }
                            out.flush();

                            // WAIT for joiner to signal they're ready
                            boolean ready = in.readBoolean();
                            if (ready) {

                                long currentCommitIndex = node.getReport().join().getResult().getCommittedMembers().getLogIndex();

                                System.out.println("[Raft] Adding node " + assignedId + " at membership index: " + currentCommitIndex);

                                node.changeMembership(joinerEndpoint, 
                                    io.microraft.MembershipChangeMode.ADD_OR_PROMOTE_TO_FOLLOWER, 
                                    currentCommitIndex)
                                    .thenAccept(response -> {
                                        System.out.println("[Raft] Node " + assignedId + " is now a voting member.");
                                        allEndpoints.add(joinerEndpoint);
                                    })
                                    .exceptionally(ex -> {
                                        System.err.println("[Raft] Failed to add node " + assignedId + ": " + ex.getMessage());
                                        return null;
                                    });

                                joinerSocketMap.put(assignedId, joinerSocket);
                                joinerEndpointMap.put(assignedId, joinerEndpoint);
                            } else {
                                System.err.println("[Lobby] Joiner startup failed.");
                                return;
                            }

                            System.out.println("[Lobby] " + joinerName + " (node " + assignedId + ") ready!");

                            // Send preliminary "not rejected" signal (game hasn't started yet)
                            out.writeBoolean(true);
                            out.flush();

                        } catch (Exception e) {
                            if (lobbyOpen.get()) {
                                System.err.println("[Lobby] Error in joiner handler: " + e.getMessage());
                            }
                            try { joinerSocket.close(); } catch (Exception ignored) {}
                        }
                    }, "joiner-handler-" + assignedId).start();

                } catch (SocketTimeoutException e) {
                    // Expected
                } catch (Exception e) {
                    if (lobbyOpen.get()) {
                        System.err.println("[Lobby] Error accepting joiner: " + e.getMessage());
                    }
                }
            }
        }, "lobby-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        // Host waits for 'start' command
        while (true) {
            System.out.print("lobby> ");
            String cmd = console.nextLine().trim().toLowerCase();
            if (cmd.equals("start")) {
                if (allEndpoints.size() < 1) {
                    System.out.println("Need at least 1 player. Waiting...");
                    continue;
                }
                break;
            }
            System.out.println("Type 'start' to begin the game.");
        }

        // Lock lobby
        lobbyOpen.set(false);
        try { lobbySocket.close(); } catch (Exception ignored) {}
        Thread.sleep(500);

        System.out.println("[Host] Lobby closed. Adding " + joinerEndpointMap.size() 
                + " joiner(s) to Raft cluster...");


        Thread.sleep(2000);
        
        System.out.println("[Host] Game starting with " + allEndpoints.size() + " player(s).");

        if (!joinerSockets.isEmpty()) {
            ExecutorService notifier = Executors.newFixedThreadPool(joinerSockets.size());
            for (Socket s : joinerSockets) {
                notifier.submit(() -> {
                    try {
                        DataOutputStream out = new DataOutputStream(s.getOutputStream());
                        out.writeBoolean(true); // "game started" signal
                        out.flush();
                    } catch (Exception e) {
                        System.err.println("[Warning] Failed to notify joiner: " + e.getMessage());
                    }
                });
            }
            notifier.shutdown();
            notifier.awaitTermination(5, TimeUnit.SECONDS);
        }

        // Register the host as a player in the game
        submitAction(node, new GameAction(GameAction.Type.JOIN, playerName));
        submitAction(node, new GameAction(GameAction.Type.NEXT_PHASE, playerName));

        return new BlackJackNode(node, playerName, myNodeId, actionEndpoint);
    }


    private static BlackJackNode startAsJoiner(String playerName, Scanner console) throws Exception {
        System.out.print("Host IP address: ");
        String hostIp = console.nextLine().trim();

        System.out.println("[Joining] Connecting to " + hostIp + ":" + LOBBY_PORT + " ...");

        Socket lobbySocket = new Socket(hostIp, LOBBY_PORT);
        DataInputStream  in  = new DataInputStream(lobbySocket.getInputStream());
        DataOutputStream out = new DataOutputStream(lobbySocket.getOutputStream());

        out.writeUTF(playerName);
        String myIp = getLocalIp();
        out.writeUTF(myIp);
        out.flush();

        int myNodeId    = in.readInt();
        int memberCount = in.readInt();

        List<TCPEndpoint> existingMembers = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            String id   = in.readUTF();
            String host = in.readUTF();
            int    port = in.readInt();
            existingMembers.add(new TCPEndpoint(id, host, port));
        }

        System.out.println("[Joining] Assigned node ID: " + myNodeId
                + ". Starting Raft node...");

        List<RaftEndpoint> initialGroup = new ArrayList<>(existingMembers);
        TCPEndpoint myEndpoint = new TCPEndpoint(String.valueOf(myNodeId), myIp, RAFT_BASE + myNodeId);

        initialGroup.add(myEndpoint);

        TCPTransport transport = new TCPTransport(myEndpoint);
        RaftServer   raftServer = new RaftServer(RAFT_BASE + myNodeId);

        RaftNode node = RaftNode.newBuilder()
                .setGroupId("blackjack")
                .setLocalEndpoint(myEndpoint)
                .setInitialGroupMembers(initialGroup)  // Only existing members
                .setConfig(raftConfig())
                .setTransport(transport)
                .setStateMachine(new StateManager())
                .setModelFactory(new DefaultRaftModelFactory())
                .build();

        raftServer.start(node);
        node.start();
        ActionForwarder forwarder = new ActionForwarder(myNodeId);
        forwarder.setRaftNode(node);
        forwarder.start();

        System.out.println("[Joining] Raft node started on port " + (RAFT_BASE + myNodeId));

        out.writeBoolean(true);
        out.flush();

        boolean accepted = in.readBoolean();
        if (!accepted) {
            System.out.println("[Error] Host rejected connection.");
            return null;
        }

        System.out.println("[Joining] Confirmed by host. Waiting for game start...");

        in.readBoolean();

        System.out.println("[Lobby] Game starting!");
        waitForLeader(node);

        while (!node.getReport().join().getResult().getStatus().toString().equals("ACTIVE")) {
            System.out.println("[Joining] Syncing cluster state...");
            Thread.sleep(500);
        }

        submitAction(node, new GameAction(GameAction.Type.JOIN, playerName));

        return new BlackJackNode(node, playerName, myNodeId, actionEndpoint);
    }

    // Main game loop
    private void runConsole(Scanner scanner) {
        printHelp();
        while (true) {
            System.out.print(playerName + "> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.isEmpty()) continue;

            switch (line) {
                case "help"         -> printHelp();
                case "status"       -> printStatus();
                case "state"        -> submitAction(new GameAction(GameAction.Type.NEXT_PHASE, playerName)); // shows state via result
                case "quit", "exit" -> { System.out.println("Goodbye."); return; }
                case "hit"          -> submitAction(new GameAction(GameAction.Type.HIT, playerName));
                case "stand"        -> submitAction(new GameAction(GameAction.Type.STAND, playerName));
                case "double"       -> submitAction(new GameAction(GameAction.Type.DOUBLE_DOWN, playerName));
                case "deal"         -> submitAction(new GameAction(GameAction.Type.DEAL_CARDS, playerName));
                case "next"         -> submitAction(new GameAction(GameAction.Type.NEXT_PHASE, playerName));
                default -> {
                    if (line.startsWith("bet ")) {
                        try {
                            int amount = Integer.parseInt(line.substring(4).trim());
                            submitAction(new GameAction(GameAction.Type.PLACE_BET, playerName, amount));
                        } catch (NumberFormatException e) {
                            System.out.println("Usage: bet <amount>   e.g. bet 100");
                        }
                    } else {
                        System.out.println("Unknown command. Type 'help' for options.");
                    }
                }
            }
        }
    }


    private void submitAction(GameAction action) {
        submitAction(raftNode, action);
    }

    private static void submitAction(RaftNode node, GameAction action) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Object result = node.replicate(action).join().getResult();
                System.out.println("-> " + result);
                return;

            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof NotLeaderException nle && nle.getLeader() != null) {
                    TCPEndpoint leaderEp = (TCPEndpoint) nle.getLeader();
                    String result = forwardToLeader(leaderEp, action);
                    if (result != null) {
                        System.out.println("-> " + result);
                        return;
                    }
                    System.out.println("[Forward failed — waiting for re-election... " + attempt + "/" + MAX_RETRIES + "]");
                } else {
                    System.out.println("[Error: " + (cause != null ? cause.getMessage() : e.getMessage()) + "]");
                    return;
                }
                sleep(RETRY_DELAY);

            } catch (Exception e) {
                System.out.println("[Leader unreachable, waiting for re-election... " + attempt + "/" + MAX_RETRIES + "]");
                sleep(RETRY_DELAY);
            }
        }
        System.out.println("[Action failed after " + MAX_RETRIES + " attempts]");
    }

    private static String forwardToLeader(TCPEndpoint leaderEp, GameAction action) {
        int forwardPort = ActionForwarder.forwardPortFor(Integer.parseInt(leaderEp.getId()));
        try (
            Socket sock            = new Socket(leaderEp.getHost(), forwardPort);
            ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
            DataInputStream    in  = new DataInputStream(sock.getInputStream())
        ) {
            sock.setSoTimeout(5000);
            out.writeObject(action);
            out.flush();
            String response = in.readUTF();
            if (response.startsWith("OK ")) return response.substring(3);
            System.out.println("[Leader error: " + response + "]");
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    


    private static RaftConfig raftConfig() {
        return RaftConfig.newBuilder()
                .setLeaderHeartbeatPeriodSecs(1)
                .setLeaderElectionTimeoutMillis(3000)
                .build();
    }

    private static void waitForLeader(RaftNode node) throws Exception {
        System.out.println("Waiting for leader election...");
        for (int i = 0; i < 30; i++) {
            try {
                var report = node.getReport().join().getResult();
                if (report.getRole() == RaftRole.LEADER) {
                    System.out.println("[This node is the LEADER]");
                    return;
                }
                if (report.getTerm().getLeaderEndpoint() != null) {
                    System.out.println("[Leader is: " + report.getTerm().getLeaderEndpoint() + "]");
                    return;
                }
            } catch (Exception ignored) {}
            sleep(500);
        }
        System.out.println("[Warning: leader not detected yet — commands will retry automatically]");
    }

    private static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void printStatus() {
        try {
            var report = raftNode.getReport().join().getResult();
            System.out.println("Role:         " + report.getRole());
            System.out.println("Term:         " + report.getTerm().getTerm());
            System.out.println("Commit index: " + report.getLog().getCommitIndex());
            var leader = report.getTerm().getLeaderEndpoint();
            System.out.println("Leader:       " + (leader != null ? leader : "election in progress"));
        } catch (Exception e) {
            System.out.println("Could not get Raft status: " + e.getMessage());
        }
    }

    private void printHelp() {
        System.out.println();
        System.out.println(" ───────────────────────────────── ");
        System.out.println("│         COMMANDS                │");
        System.out.println("|─────────────────────────────────|");
        System.out.println("│  bet <n>   : place a bet        │");
        System.out.println("│  deal      : deal cards         │");
        System.out.println("│  hit       : take a card        │");
        System.out.println("│  stand     : end your turn      │");
        System.out.println("│  double    : double down        │");
        System.out.println("│  next      : start next round   │");
        System.out.println("│  status    : show Raft status   │");
        System.out.println("│  help      : show this menu     │");
        System.out.println("│  quit      : leave              │");
        System.out.println(" ───────────────────────────────── ");
        System.out.println();
    }
}