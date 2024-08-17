package com.github.venixpll.prismarinejava.hybrid.client;

import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.hybrid.shared.netty.ProtocolAttributes;
import com.github.venixpll.prismarinejava.hybrid.shared.netty.frame.NettyFramingCodec;
import com.github.venixpll.prismarinejava.hybrid.shared.netty.hybrid.HybridPacketCodec;
import com.github.venixpll.prismarinejava.hybrid.shared.session.Session;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MinecraftClient {

    private final String username;
    private final String version;

    private Session clientSession;

    public void connect(final String host, final int port, final NioEventLoopGroup loopGroup, final Consumer<Session> callback) {
        final var bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(loopGroup)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel socketChannel) {
                        // Initializing pipeline elements
                        final var pipeline = socketChannel.pipeline();

                        pipeline.addLast("timeout", new ReadTimeoutHandler(30));
                        pipeline.addLast("frameCodec", new NettyFramingCodec());
                        pipeline.addLast("packetCodec", new HybridPacketCodec());


                        // Initializing client protocol
                        MinecraftClient.this.clientSession = new Session(socketChannel, PacketDirection.CLIENTBOUND);
                        MinecraftClient.this.clientSession.getChannel().attr(ProtocolAttributes.protocolVersionKey).set(MinecraftClient.this.version);

                        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
                        pipeline.addLast(MinecraftClient.this.clientSession);
                    }
                });
        bootstrap.connect(host, port).addListener((future) -> {
            if(future.isSuccess()) callback.accept(this.clientSession);
        });
    }

}
