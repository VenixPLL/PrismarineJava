package com.github.venixpll.prismarinejava.hybrid.shared.netty.frame;

import com.github.venixpll.prismarinejava.hybrid.shared.buffer.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.Arrays;
import java.util.List;

public class NettyFramingCodec extends ByteToMessageCodec<ByteBuf> {

    /**
     * Encoding packet into frames
     * Writing total packet size in first bytes.
     *
     * @param channelHandlerContext
     * @param byteBuf               Input buffer
     * @param byteBuf2              Output buffer
     * @throws Exception
     */
    @Override
    protected void encode(final ChannelHandlerContext channelHandlerContext, final ByteBuf byteBuf, final ByteBuf byteBuf2) throws Exception {
        final int size = byteBuf.readableBytes();
        final int j = PacketBuffer.getVarIntSize(size);
        if (j > 3) {
            throw new IllegalArgumentException("unable to fit " + size + " into 3");
        }

        final PacketBuffer packetbuffer = new PacketBuffer(byteBuf2);
        packetbuffer.ensureWritable(j + size);
        packetbuffer.writeVarInt(size);
        packetbuffer.writeBytes(byteBuf, byteBuf.readerIndex(), size);
    }

    /**
     * Reading packet Frames
     * Reading packet size from frame
     *
     * @param channelHandlerContext
     * @param byteBuf               Input buffer
     * @param list                  Output Buffer list
     * @throws Exception
     */
    @Override
    protected void decode(final ChannelHandlerContext channelHandlerContext, final ByteBuf byteBuf, final List<Object> list) throws Exception {
        byteBuf.markReaderIndex();
        final byte[] bytes = new byte[3];

        for (int i = 0; i < bytes.length; ++i) {
            if (!byteBuf.isReadable()) {
                byteBuf.resetReaderIndex();
                return;
            }

            bytes[i] = byteBuf.readByte();

            if (bytes[i] >= 0) {
                final var packetbuffer = new PacketBuffer(Unpooled.wrappedBuffer(bytes));
                try {
                    final int j = packetbuffer.readVarInt();
                    if (byteBuf.readableBytes() >= j) {
                        list.add(byteBuf.readBytes(j));
                        return;
                    }

                    byteBuf.resetReaderIndex();
                } finally {
                    packetbuffer.release();
                }
                return;
            }
        }

        throw new CorruptedFrameException("length wider than 21-bit");
    }
}
