package com.github.venixpll.prismarinejava.hybrid.server;

import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.hybrid.shared.netty.frame.NettyFramingCodec;
import com.github.venixpll.prismarinejava.hybrid.shared.netty.hybrid.HybridPacketCodec;
import com.github.venixpll.prismarinejava.hybrid.shared.session.Session;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;

public class MinecraftServer {

    @Getter
    private final Map<ChannelId, Session> connectedSession = new HashMap<>();

    public void bind(final int port, final NioEventLoopGroup loopGroup, final Consumer<Session> connectListener) {
        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.group(loopGroup);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel socketChannel) {
                final var pipeline = socketChannel.pipeline();
                pipeline.addLast("timeout", new ReadTimeoutHandler(30));
                pipeline.addLast("frameCodec", new NettyFramingCodec());
                pipeline.addLast("packetCodec",new HybridPacketCodec());
                final var session = new Session(socketChannel, PacketDirection.SERVERBOUND);

                session.on("connect",(id,none) -> {
                   MinecraftServer.this.connectedSession.put(session.getId(),session);
                   connectListener.accept(session);
                });
                session.on("disconnect",(id,none) -> MinecraftServer.this.connectedSession.remove(session.getId()));
                pipeline.addLast(session);
            }
        });
        bootstrap.bind(port).syncUninterruptibly();
    }

}
