import com.github.venixpll.prismarinejava.PrismJava;
import com.github.venixpll.prismarinejava.hybrid.client.MinecraftClient;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.JsonPacket;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.Arrays;

public final class InitializerTest {

    public static void main(final String[] args) throws Exception {
        final var client = new MinecraftClient("abecadlo321","1.18.2");
        client.connect("127.0.0.1",25565,new NioEventLoopGroup(1), session -> {
            session.on("connect", (id,state) -> {
                session.setProtocolId(758);
                session.send("set_protocol", (packet) ->
                        packet.set("protocolVersion",758)
                        .set("serverHost","localhost")
                        .set("serverPort",25565)
                        .set("nextState",2));

                session.setConnectionState(ConnectionState.LOGIN);
                session.send("login_start", (packet) -> packet.set("username","abecadlo321"));
                session.removeListener(id);
            });

            session.on("compress", (id, packet) -> {
                final var threshold = (int) packet.get("threshold");
                session.setCompression(threshold);
            });

            session.on("*",(id,packet) -> {
                final var str = packet.getParams().toString();
                if(str.length() > 255) return;

                System.out.println("PRISM - RCV: " + packet.getName() + " -> " + str);
            });

            session.onLegacy((id,packet) -> {
                System.out.println("HYBRID - RCV Legacy -> " + packet.getName() + " -> " + packet.toString());;
                //session.removeLegacyListener(id);
            });

            session.on("success", (id, packet) -> {
                session.setConnectionState(ConnectionState.PLAY);
                session.removeListener(id);
            });
        });
    }

}
