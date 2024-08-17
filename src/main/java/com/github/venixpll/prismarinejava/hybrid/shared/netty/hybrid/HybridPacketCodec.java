package com.github.venixpll.prismarinejava.hybrid.shared.netty.hybrid;

import com.github.venixpll.prismarinejava.PrismJava;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.hybrid.shared.buffer.PacketBuffer;
import com.github.venixpll.prismarinejava.hybrid.shared.netty.ProtocolAttributes;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.Packet;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.JsonPacket;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.LegacyPacket;
import com.github.venixpll.prismarinejava.prismarine.PrismInitializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HybridPacketCodec extends MessageToMessageCodec<ByteBuf, Packet> {

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Packet packet, final List<Object> list) throws Exception {
        final var packetDirection = ctx.channel().attr(ProtocolAttributes.packetDirectionKey).get();
        final var isServer = packetDirection == PacketDirection.SERVERBOUND;
        final var connectionState = ctx.channel().attr(ProtocolAttributes.connectionStateKey).get();
        final var protocolVersion = ctx.channel().attr(ProtocolAttributes.protocolVersionKey).get();

        try {
            if (packet instanceof final JsonPacket jsonPacket) {
                final var startTime = System.currentTimeMillis();
                final var json = jsonPacket.toJson();

                final var data = PrismJava.getJavetInterop().serialize(connectionState, protocolVersion, json, isServer);
                final var encodeTime = System.currentTimeMillis() - startTime;

                if (data == null || data.length == 1)
                    throw new EncoderException("Failed to encode json packet -> " + json);

                if (encodeTime > 50) {
                    System.err.println("Encoding took too long " + encodeTime + "ms, max 50ms; name: " + jsonPacket.getName());
                }

                list.add(Unpooled.wrappedBuffer(data));
            } else if (packet instanceof final LegacyPacket legacyPacket) {
                final var buffer = new PacketBuffer(Unpooled.buffer());
                final var packetId = legacyPacket.getPacketId(protocolVersion, packetDirection, connectionState);
                buffer.writeVarInt(packetId);
                legacyPacket.write(buffer);

                list.add(buffer);
            } else {
                throw new EncoderException("Unknown packet type provided!");
            }
        }catch(Exception e){
            System.out.println("Input: " + packet.getName());
            e.printStackTrace();
        }
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf byteBuf, final List<Object> list) throws Exception {
        final var packetDirection = ctx.channel().attr(ProtocolAttributes.packetDirectionKey).get();
        final var isServer = packetDirection == PacketDirection.SERVERBOUND;
        final var connectionState = ctx.channel().attr(ProtocolAttributes.connectionStateKey).get();
        final var protocolVersion = ctx.channel().attr(ProtocolAttributes.protocolVersionKey).get();

        final var bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        try {

            // Hybrid Work
            // Read JSON Packet always
            final var startTime = System.currentTimeMillis();
            assert PrismInitializer.getJavetInterop().isInitialized() : "PrismarineJS Interop not initialized!";
            final var decodedJson = PrismJava.getJavetInterop().deserialize(connectionState,protocolVersion, bytes, isServer);
            final var packet = JsonPacket.fromJson(decodedJson);

            final var decodeTime = System.currentTimeMillis() - startTime;
            if (decodeTime > 50) {
                System.err.println("Decoding took too long " + decodeTime + "ms, max 50ms; name: " + packet.getName());
            }

            // Translate to legacy if present.
            final var legacyPacket = PrismJava.getPacketRegistry().getPacket(connectionState, packetDirection, packet.getName());
            if(Objects.nonNull(legacyPacket)){
                legacyPacket.translate(packet);
                list.add(legacyPacket);
                return;
            }

            // Go with json.
            list.add(packet);
        }catch (Exception e){
            System.out.println("Input: " + Arrays.toString(bytes));
            e.printStackTrace();
        }finally {
            byteBuf.clear();
        }
    }

}
