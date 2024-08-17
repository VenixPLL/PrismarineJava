package com.github.venixpll.prismarinejava.hybrid.shared.session;

import com.alibaba.fastjson2.JSONObject;
import com.github.venixpll.prismarinejava.PrismJava;

import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.hybrid.shared.netty.ProtocolAttributes;
import com.github.venixpll.prismarinejava.hybrid.shared.netty.compress.NettyCompressionCodec;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.Packet;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.JsonPacket;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.LegacyPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.ConcurrentSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;

@Getter
public class Session extends SimpleChannelInboundHandler<Packet> {

    private final Channel channel;
    private int protocolId;

    private final ConcurrentHashMap<UUID, BiConsumer<UUID,LegacyPacket>> legacyListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentMap<UUID,BiConsumer<UUID,JsonPacket>>> listeners = new ConcurrentHashMap<>();

    public Session(final Channel channel, final PacketDirection packetDirection){
        this.channel = channel;
        this.channel.attr(ProtocolAttributes.compressionThresholdKey).set(-1); // Disabled
        this.channel.attr(ProtocolAttributes.connectionStateKey).set(ConnectionState.HANDSHAKING); // Start
        this.channel.attr(ProtocolAttributes.packetDirectionKey).set(packetDirection);
        this.channel.attr(ProtocolAttributes.protocolVersionKey).set("1.8"); // Default
    }

    public void propagateReceive(final Packet packet){
        switch(packet){
            case final JsonPacket jsonPacket -> this.callListeners(jsonPacket.getName(), jsonPacket);
            case final LegacyPacket legacyPacket -> this.legacyListeners.forEach((id, listener) -> listener.accept(id,legacyPacket));
            default -> throw new IllegalStateException("Unexpected value: " + packet);
        }
    }

    private void callListeners(final String name, final JsonPacket packet){
        final var listeners = this.listeners.get(name); // Direct listeners
        if(Objects.nonNull(listeners)) listeners.forEach((id,consumer) -> consumer.accept(id,packet));

        if(packet != null) {
            final var allTypeListeners = this.listeners.get("*"); // All types.
            if (Objects.nonNull(allTypeListeners))
                allTypeListeners.forEach((id, consumer) -> consumer.accept(id, packet));
        }
    }

    public void clearListeners(){
        this.listeners.clear();
    }

    public void removeListeners(final String packetName){
        if(!this.listeners.containsKey(packetName)) return;
        this.listeners.remove(packetName);
    }

    public void removeLegacyListener(final UUID uuid){
        this.legacyListeners.remove(uuid);
    }

    public void removeListener(final UUID uuid){
        final var values = this.listeners.entrySet();
        values.forEach(val -> val.getValue().remove(uuid));
    }

    public Session onLegacy(final BiConsumer<UUID,LegacyPacket> legacyListener){
        this.legacyListeners.put(UUID.randomUUID(), legacyListener);
        return this;
    }

    public Session on(final String packetName, final BiConsumer<UUID,JsonPacket> listener){
        if(!this.listeners.containsKey(packetName))
            this.listeners.put(packetName, new ConcurrentHashMap<>());

        this.listeners.get(packetName).put(UUID.randomUUID(), listener);
        return this;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        this.callListeners("connect",null);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext channelHandlerContext, final Packet packet) throws Exception {
        this.propagateReceive(packet);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        this.callListeners("disconnect",null);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    public ChannelId getId(){
        return this.channel.id();
    }

    public void setProtocolId(final int protocolId) {
        this.protocolId = protocolId;
        final var protocolName = PrismJava.getTranslator().fromProtocol(protocolId);
        this.channel.attr(ProtocolAttributes.protocolVersionKey).set(protocolName);
    }

    public void setConnectionState(final ConnectionState connectionState){
        this.channel.attr(ProtocolAttributes.connectionStateKey).set(connectionState);
    }

    public void setCompression(final int threshold){
        final var pipeline = this.channel.pipeline();
        this.channel.attr(ProtocolAttributes.compressionThresholdKey).set(threshold);
        if(threshold > 0){
            pipeline.addBefore("packetCodec","compression", new NettyCompressionCodec(threshold));
            return;
        }

        if(pipeline.get("compression") != null) pipeline.remove("compression");
    }

    public void autoRead(final boolean autoRead){
        this.channel.config().setAutoRead(autoRead);
    }

    public void send(final String name, final Consumer<JsonPacket> packetConsumer){
        final var packet = new JsonPacket(name);
        packetConsumer.accept(packet);
        this.sendPacket(packet);
    }

    public void send(final String packetName, final JSONObject params){
        this.sendPacket(new JsonPacket(packetName).setParams(params));
    }

    public void send(final JsonPacket jsonPacket){
        this.sendPacket(jsonPacket);
    }

    public void sendPacket(final Packet packet){
        this.channel.writeAndFlush(packet);
    }

}
