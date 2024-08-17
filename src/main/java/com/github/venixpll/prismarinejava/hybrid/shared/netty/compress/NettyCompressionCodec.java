package com.github.venixpll.prismarinejava.hybrid.shared.netty.compress;

import com.github.venixpll.prismarinejava.hybrid.shared.buffer.PacketBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class NettyCompressionCodec extends ByteToMessageCodec<ByteBuf> {

    private final byte[] buffer = new byte[8192];
    private final Deflater deflater;
    private final Inflater inflater;
    private int threshold;

    public NettyCompressionCodec(final int thresholdIn) {
        this.threshold = thresholdIn;
        this.deflater = new Deflater();
        this.inflater = new Inflater();
    }

    /**
     * Compressing packet with desired threshold
     *
     * @param channelHandlerContext
     * @param in                    Input uncompressed buffer
     * @param out                   Output compressed buffer
     * @throws Exception
     */
    @Override
    protected void encode(final ChannelHandlerContext channelHandlerContext, final ByteBuf in, final ByteBuf out) throws Exception {
        final int readable = in.readableBytes();
        final var output = new PacketBuffer(out);
        if (readable < this.threshold) {
            output.writeVarInt(0);
            out.writeBytes(in);
        } else {
            final var bytes = new byte[readable];
            in.readBytes(bytes);
            output.writeVarInt(bytes.length);
            this.deflater.setInput(bytes, 0, readable);
            this.deflater.finish();
            while (!this.deflater.finished()) {
                final var length = this.deflater.deflate(this.buffer);
                output.writeBytes(this.buffer, length);
            }

            this.deflater.reset();
        }
    }

    /**
     * Decompressing packet with desired threshold
     *
     * @param channelHandlerContext
     * @param buf                   Input compressed Buffer
     * @param out                   Output decompressed Buffer list
     * @throws Exception
     */
    @Override
    protected void decode(final ChannelHandlerContext channelHandlerContext, final ByteBuf buf, final List<Object> out) throws Exception {
        if (buf.readableBytes() != 0) {
            final var in = new PacketBuffer(buf);
            final var size = in.readVarInt();
            if (size == 0) {
                out.add(buf.readBytes(buf.readableBytes()));
            } else {
                if (size < this.threshold) {
                    throw new DecoderException("Badly compressed packet: size of " + size + " is below threshold of " + this.threshold + ".");
                }

                if (size > 2097152) {
                    throw new DecoderException("Badly compressed packet: size of " + size + " is larger than protocol maximum of " + 2097152 + ".");
                }

                final var bytes = new byte[buf.readableBytes()];
                in.readBytes(bytes);
                this.inflater.setInput(bytes);
                final var inflated = new byte[size];
                this.inflater.inflate(inflated);
                out.add(Unpooled.wrappedBuffer(inflated));
                this.inflater.reset();
            }
        }
    }

    public void setCompressionThreshold(final int thresholdIn) {
        this.threshold = thresholdIn;
    }
}
