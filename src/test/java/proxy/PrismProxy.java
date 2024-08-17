package proxy;

import com.github.venixpll.prismarinejava.PrismJava;
import com.github.venixpll.prismarinejava.hybrid.client.MinecraftClient;
import com.github.venixpll.prismarinejava.hybrid.server.MinecraftServer;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.JsonPacket;
import com.github.venixpll.prismarinejava.hybrid.shared.session.Session;
import com.github.venixpll.prismarinejava.prismarine.protocol.ProtocolTranslator;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.UUID;
import javax.swing.JOptionPane;

public class PrismProxy {

    private final String TARGET_HOST;
    private final int TARGET_PORT;

    private final MinecraftServer minecraftServer;

    public PrismProxy(){
        this.TARGET_HOST = "127.0.0.1";
        this.TARGET_PORT = 25565;
        System.out.println("Configured endpoint -> H: " + this.TARGET_HOST + " P: " + this.TARGET_PORT);

        this.minecraftServer = new MinecraftServer();
    }

    public void start(){
        this.minecraftServer.bind(1337,new NioEventLoopGroup(1), serverSession -> {

            serverSession.on("set_protocol", (id,packet) -> {
                serverSession.setProtocolId( (int) packet.get("protocolVersion"));
                final var nextState = (int) packet.get("nextState");
                serverSession.setConnectionState(nextState == 2 ? ConnectionState.LOGIN : ConnectionState.STATUS);
                serverSession.removeListener(id); // Remove Listener;

                System.out.println("[PROXY] INPUT SERVER -> RCV INTENT -> " + nextState);
                this.configureClient(serverSession, packet);
            });

            serverSession.on("login_start",(id,packet) -> {
                System.out.println("LOGIN RCV: " + packet.getName() + " p: " + packet.getParams().toString());
                serverSession.removeListener(id);
            });

        });
    }

    private void configureClient(final Session serverSession, final JsonPacket intentPacket){
        final var client = new MinecraftClient("PrismProtocol",
                PrismJava.getTranslator().fromProtocol(serverSession.getProtocolId())); // Set client to same protocol as server session;

        client.connect(this.TARGET_HOST,this.TARGET_PORT, (NioEventLoopGroup) serverSession.getChannel().eventLoop().parent(), clientSession -> {

            clientSession.on("connect",(ignored0,ignored) -> {
                clientSession.setProtocolId(serverSession.getProtocolId());
                System.out.println("[PROXY] ENDPOINT CLIENT -> CONNECTED -> " + this.TARGET_HOST + ":" + this.TARGET_PORT);

                clientSession.setConnectionState(ConnectionState.HANDSHAKING); // Set conn state to Handshake;
                clientSession.send(intentPacket); // Send pending intent;
                System.out.println("[PROXY] ENDPOINT CLIENT -> INTENT SENT");

                clientSession.setConnectionState(ConnectionState.LOGIN);
                serverSession.setConnectionState(ConnectionState.LOGIN);// Switch conn state to Login;
                if(clientSession.getProtocolId() < 764) {
                    clientSession.send("login_start", (packet) -> packet.set("username", "1381"));
                }else{
                    clientSession.send("login_start",packet -> packet.set("username","1381")
                            .set("playerUUID", UUID.randomUUID().toString()));
                }
                System.out.println("[PROXY] ENDPOINT CLIENT -> LOGIN SENT");

                clientSession.on("*", (id,packet) -> {
                    //System.out.println("[PROXY] ENDPOINT CLIENT -> RCV & FORWARD -> " + packet.getName());
                    serverSession.send(packet);

                    switch(packet.getName()){
                        case "success" -> {
                            System.out.println("[PROXY] SERVER SESS -> STATE SWITCH -> PLAY -> " + packet.getName());
                            serverSession.setConnectionState(ConnectionState.PLAY);
                        }
                    }

                }); // Forward packets from clientSession to serverSession;

                serverSession.on("*", (id,packet) -> {
                    //System.out.println("[PROXY] SERVER SESS -> RCV & FORWARD -> " + packet.getName());
                    clientSession.send(packet);
                }); // Forward packets from serverSession to clientSession;

                clientSession.onLegacy(((uuid, legacyPacket) -> {
                    System.out.println("[PROXY] CLIENT SESS LEGACY -> ROUTE -> LEGACY -> " + legacyPacket.toString());
                    serverSession.sendPacket(legacyPacket);
                }));
                serverSession.onLegacy(((uuid, legacyPacket) -> {
                    System.out.println("[PROXY] SERVER SESS LEGACY -> ENDPOINT -> LEGACY -> " + legacyPacket.toString());
                    clientSession.sendPacket(legacyPacket);
                }));

                // Additional conditional switches
                clientSession.on("compress", (id, packet) -> {
                    final var threshold = (int) packet.get("threshold");
                    clientSession.setCompression(threshold);
                    serverSession.setCompression(threshold); // Set on both;
                    System.out.println("[PROXY] ENDPOINT CLIENT -> RCV COMPRESS -> " + threshold);
                });

                clientSession.on("success", (id, packet) -> {
                    System.out.println("[PROXY] ENDPOINT CLIENT -> RCV SUCCESS -> id;" + id.getMostSignificantBits());
                    clientSession.setConnectionState(ConnectionState.PLAY);
                });

                //clientSession.removeListener(ignored0); // REMOVE THIS;
            });

        });
    }

    public static void main(final String[] args) {
        final var proxy = new PrismProxy();
        proxy.start();
    }

}
