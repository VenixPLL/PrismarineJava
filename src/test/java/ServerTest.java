import com.github.venixpll.prismarinejava.hybrid.server.MinecraftServer;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import io.netty.channel.nio.NioEventLoopGroup;

public class ServerTest {

    public static void main(final String[] args) {
        new MinecraftServer().bind(25566,new NioEventLoopGroup(1),session -> {

            session.on("set_protocol", (id,packet) -> {
                session.setProtocolId( (int) packet.get("protocolVersion"));
                final var nextState = (int) packet.get("nextState");
                session.setConnectionState(nextState == 2 ? ConnectionState.LOGIN : ConnectionState.STATUS);
                session.removeListener(id);
            });

            session.on("login_start", (id,packet) -> {
                final var playerName = (String) packet.get("username");
                System.out.println("Player " + playerName + " id;" + session.getId().asShortText());

                session.removeListener(id); // Remove listener.
            });

            session.onLegacy((id,packet) -> {
                System.out.println("LEGACY RCV -> " + packet.getName() + " - " + packet.toString());
                session.sendPacket(packet); // sendPacket for legacy
            });

            session.on("*",(id,packet) -> {
                System.out.println("PRISM RCV -> " + packet.getName() + " - " + packet.getParams().toString());
                session.send(packet); // send or sendPacket for json type.
            });

        });
    }

}
