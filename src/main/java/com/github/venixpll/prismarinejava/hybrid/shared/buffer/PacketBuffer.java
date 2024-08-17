package com.github.venixpll.prismarinejava.hybrid.shared.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PacketBuffer extends ByteBuf {

    private final ByteBuf byteBuf;

    public PacketBuffer(final ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public static int getVarIntSize(int input) {
        for (int i = 1; i < 5; i++) {
            if ((input & -1 << i * 7) == 0) {
                return i;
            }
        }

        return 5;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public PacketBuffer writeByteArray(byte[] array) {
        this.writeVarInt(array.length);
        this.writeBytes(array);
        return this;
    }

    public void writeBytes(byte[] b, int length) throws IOException {
        this.writeBytes(b, 0, length);
    }

    public byte[] readByteArray() {
        return this.readByteArray(this.readableBytes());
    }

    public byte[] readByteArray(int maxLength) {
        final int i = this.readVarInt();
        if (i > maxLength) {
            throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxLength);
        }

        final byte[] abyte = new byte[i];
        this.readBytes(abyte);
        return abyte;
    }

    public PacketBuffer writeVarIntArray(int[] array) {
        this.writeVarInt(array.length);

        for (int i : array) {
            this.writeVarInt(i);
        }

        return this;
    }

    public int[] readVarIntArray() {
        return this.readVarIntArray(this.readableBytes());
    }

    public int[] readVarIntArray(int maxLength) {
        final int i = this.readVarInt();

        if (i > maxLength) {
            throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + maxLength);
        }

        final int[] aint = new int[i];
        for (int j = 0; j < aint.length; ++j) {
            aint[j] = this.readVarInt();
        }

        return aint;
    }

    /**
     * Writes an array of longs to the buffer, prefixed by the length of the array (as a VarInt).
     */
    public PacketBuffer writeLongArray(long[] array) {
        this.writeVarInt(array.length);

        for (long i : array) {
            this.writeLong(i);
        }

        return this;
    }

    public long[] readLongArray(long[] array) {
        return this.readLongArray(array, this.readableBytes() / 8);
    }

    public long[] readLongArray(long[] p_189423_1_, int p_189423_2_) {
        int i = this.readVarInt();

        if (p_189423_1_ == null || p_189423_1_.length != i) {
            if (i > p_189423_2_) {
                throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + p_189423_2_);
            }

            p_189423_1_ = new long[i];
        }

        for (int j = 0; j < p_189423_1_.length; ++j) {
            p_189423_1_[j] = this.readLong();
        }

        return p_189423_1_;
    }


    public <T extends Enum<T>> T readEnumValue(Class<T> enumClass) {
        return enumClass.getEnumConstants()[this.readVarInt()];
    }

    public PacketBuffer writeEnumValue(Enum<?> value) {
        return this.writeVarInt(value.ordinal());
    }

    public int readVarInt() {
        int i = 0;
        int j = 0;

        while (true) {
            byte b0 = this.readByte();
            i |= (b0 & 127) << j++ * 7;

            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }

            if ((b0 & 128) != 128) {
                break;
            }
        }

        return i;
    }

    public long readVarLong() {
        long i = 0L;
        int j = 0;

        while (true) {
            byte b0 = this.readByte();
            i |= (long) (b0 & 127) << j++ * 7;

            if (j > 10) {
                throw new RuntimeException("VarLong too big");
            }

            if ((b0 & 128) != 128) {
                break;
            }
        }

        return i;
    }

    public PacketBuffer writeUuid(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    public UUID readUuid() {
        return new UUID(this.readLong(), this.readLong());
    }

    public PacketBuffer writeVarInt(int input) {
        while ((input & -128) != 0) {
            this.writeByte(input & 127 | 128);
            input >>>= 7;
        }

        this.writeByte(input);
        return this;
    }

    public PacketBuffer writeVarLong(long value) {
        while ((value & -128L) != 0L) {
            this.writeByte((int) (value & 127L) | 128);
            value >>>= 7;
        }

        this.writeByte((int) value);
        return this;
    }

    public String readStringFromBuffer(int maxLength) {
        final int i = this.readVarInt();

        if (i > maxLength * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + i + " > " + maxLength * 4 + ")");
        } else if (i < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        }

        final String s = this.toString(this.readerIndex(), i, StandardCharsets.UTF_8);
        this.readerIndex(this.readerIndex() + i);

        if (s.length() > maxLength) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + i + " > " + maxLength + ")");
        }

        return s;
    }

    public String readString() throws IOException {
        int length = this.readVarInt();
        byte[] bytes = this.readBytesToReadString(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readBytesToReadString(int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("Array cannot have length less than 0.");
        }

        byte[] b = new byte[length];
        this.readBytes(b);
        return b;
    }

    public PacketBuffer writeString(String string) {
        final byte[] abyte = string.getBytes(StandardCharsets.UTF_8);

        if (abyte.length > 262144) {
            throw new EncoderException("String too big (was " + abyte.length + " bytes encoded, max " + 32767 + ")");
        }

        this.writeVarInt(abyte.length);
        this.writeBytes(abyte);
        return this;
    }

    @Override
    public ByteBuf capacity(int i) {
        return byteBuf.capacity(i);
    }

    @Override
    public int capacity() {
        return byteBuf.capacity();
    }

    @Override
    public int maxCapacity() {
        return byteBuf.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return byteBuf.alloc();
    }

    @Deprecated
    @Override
    public ByteOrder order() {
        return byteBuf.order();
    }

    @Override
    public ByteBuf order(ByteOrder byteOrder) {
        return byteBuf.order(byteOrder);
    }

    @Override
    public ByteBuf unwrap() {
        return byteBuf.unwrap();
    }

    @Override
    public boolean isDirect() {
        return byteBuf.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ByteBuf asReadOnly() {
        return null;
    }

    @Override
    public int readerIndex() {
        return byteBuf.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(int i) {
        return byteBuf.readerIndex(i);
    }

    @Override
    public int writerIndex() {
        return byteBuf.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(int i) {
        return byteBuf.writerIndex(i);
    }

    @Override
    public ByteBuf setIndex(int i, int i1) {
        return byteBuf.setIndex(i, i1);
    }

    @Override
    public int readableBytes() {
        return byteBuf.readableBytes();
    }

    @Override
    public int writableBytes() {
        return byteBuf.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return byteBuf.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return byteBuf.isReadable();
    }

    @Override
    public boolean isReadable(int i) {
        return byteBuf.isReadable(i);
    }

    @Override
    public boolean isWritable() {
        return byteBuf.isWritable();
    }

    @Override
    public boolean isWritable(int i) {
        return byteBuf.isWritable(i);
    }

    public ByteBuf clear() {
        return byteBuf.clear();
    }

    public ByteBuf markReaderIndex() {
        return byteBuf.markReaderIndex();
    }

    public ByteBuf resetReaderIndex() {
        return byteBuf.resetReaderIndex();
    }

    public ByteBuf markWriterIndex() {
        return byteBuf.markWriterIndex();
    }

    public ByteBuf resetWriterIndex() {
        return byteBuf.resetWriterIndex();
    }

    public ByteBuf discardReadBytes() {
        return byteBuf.discardReadBytes();
    }

    public ByteBuf discardSomeReadBytes() {
        return byteBuf.discardSomeReadBytes();
    }

    public ByteBuf ensureWritable(int p_ensureWritable_1_) {
        return byteBuf.ensureWritable(p_ensureWritable_1_);
    }

    public int ensureWritable(int p_ensureWritable_1_, boolean p_ensureWritable_2_) {
        return byteBuf.ensureWritable(p_ensureWritable_1_, p_ensureWritable_2_);
    }

    public boolean getBoolean(int p_getBoolean_1_) {
        return byteBuf.getBoolean(p_getBoolean_1_);
    }

    public byte getByte(int p_getByte_1_) {
        return byteBuf.getByte(p_getByte_1_);
    }

    public short getUnsignedByte(int p_getUnsignedByte_1_) {
        return byteBuf.getUnsignedByte(p_getUnsignedByte_1_);
    }

    public short getShort(int p_getShort_1_) {
        return byteBuf.getShort(p_getShort_1_);
    }

    @Override
    public short getShortLE(int i) {
        return 0;
    }

    public int getUnsignedShort(int p_getUnsignedShort_1_) {
        return byteBuf.getUnsignedShort(p_getUnsignedShort_1_);
    }

    @Override
    public int getUnsignedShortLE(int i) {
        return 0;
    }

    public int getMedium(int p_getMedium_1_) {
        return byteBuf.getMedium(p_getMedium_1_);
    }

    @Override
    public int getMediumLE(int i) {
        return 0;
    }


    public int getUnsignedMedium(int p_getUnsignedMedium_1_) {
        return byteBuf.getUnsignedMedium(p_getUnsignedMedium_1_);
    }

    @Override
    public int getUnsignedMediumLE(int i) {
        return 0;
    }

    public int getInt(int p_getInt_1_) {
        return byteBuf.getInt(p_getInt_1_);
    }

    @Override
    public int getIntLE(int i) {
        return 0;
    }

    public long getUnsignedInt(int p_getUnsignedInt_1_) {
        return byteBuf.getUnsignedInt(p_getUnsignedInt_1_);
    }

    @Override
    public long getUnsignedIntLE(int i) {
        return 0;
    }

    public long getLong(int p_getLong_1_) {
        return byteBuf.getLong(p_getLong_1_);
    }

    @Override
    public long getLongLE(int i) {
        return 0;
    }

    public char getChar(int p_getChar_1_) {
        return byteBuf.getChar(p_getChar_1_);
    }

    public float getFloat(int p_getFloat_1_) {
        return byteBuf.getFloat(p_getFloat_1_);
    }

    public double getDouble(int p_getDouble_1_) {
        return byteBuf.getDouble(p_getDouble_1_);
    }

    public ByteBuf getBytes(int p_getBytes_1_, ByteBuf p_getBytes_2_) {
        return byteBuf.getBytes(p_getBytes_1_, p_getBytes_2_);
    }

    public ByteBuf getBytes(int p_getBytes_1_, ByteBuf p_getBytes_2_, int p_getBytes_3_) {
        return byteBuf.getBytes(p_getBytes_1_, p_getBytes_2_, p_getBytes_3_);
    }

    public ByteBuf getBytes(int p_getBytes_1_, ByteBuf p_getBytes_2_, int p_getBytes_3_, int p_getBytes_4_) {
        return byteBuf.getBytes(p_getBytes_1_, p_getBytes_2_, p_getBytes_3_, p_getBytes_4_);
    }

    public ByteBuf getBytes(int p_getBytes_1_, byte[] p_getBytes_2_) {
        return byteBuf.getBytes(p_getBytes_1_, p_getBytes_2_);
    }

    public ByteBuf getBytes(int p_getBytes_1_, byte[] p_getBytes_2_, int p_getBytes_3_, int p_getBytes_4_) {
        return byteBuf.getBytes(p_getBytes_1_, p_getBytes_2_, p_getBytes_3_, p_getBytes_4_);
    }

    public ByteBuf getBytes(int p_getBytes_1_, ByteBuffer p_getBytes_2_) {
        return byteBuf.getBytes(p_getBytes_1_, p_getBytes_2_);
    }

    public ByteBuf getBytes(int p_getBytes_1_, OutputStream p_getBytes_2_, int p_getBytes_3_) throws IOException {
        return byteBuf.getBytes(p_getBytes_1_, p_getBytes_2_, p_getBytes_3_);
    }

    public int getBytes(int p_getBytes_1_, GatheringByteChannel p_getBytes_2_, int p_getBytes_3_) throws IOException {
        return byteBuf.getBytes(p_getBytes_1_, p_getBytes_2_, p_getBytes_3_);
    }

    @Override
    public int getBytes(int i, FileChannel fileChannel, long l, int i1) throws IOException {
        return 0;
    }

    @Override
    public CharSequence getCharSequence(int i, int i1, Charset charset) {
        return null;
    }

    public ByteBuf setBoolean(int p_setBoolean_1_, boolean p_setBoolean_2_) {
        return byteBuf.setBoolean(p_setBoolean_1_, p_setBoolean_2_);
    }

    public ByteBuf setByte(int p_setByte_1_, int p_setByte_2_) {
        return byteBuf.setByte(p_setByte_1_, p_setByte_2_);
    }

    public ByteBuf setShort(int p_setShort_1_, int p_setShort_2_) {
        return byteBuf.setShort(p_setShort_1_, p_setShort_2_);
    }

    @Override
    public ByteBuf setShortLE(int i, int i1) {
        return null;
    }

    public ByteBuf setMedium(int p_setMedium_1_, int p_setMedium_2_) {
        return byteBuf.setMedium(p_setMedium_1_, p_setMedium_2_);
    }

    @Override
    public ByteBuf setMediumLE(int i, int i1) {
        return null;
    }

    public ByteBuf setInt(int p_setInt_1_, int p_setInt_2_) {
        return byteBuf.setInt(p_setInt_1_, p_setInt_2_);
    }

    @Override
    public ByteBuf setIntLE(int i, int i1) {
        return null;
    }

    public ByteBuf setLong(int p_setLong_1_, long p_setLong_2_) {
        return byteBuf.setLong(p_setLong_1_, p_setLong_2_);
    }

    @Override
    public ByteBuf setLongLE(int i, long l) {
        return null;
    }

    public ByteBuf setChar(int p_setChar_1_, int p_setChar_2_) {
        return byteBuf.setChar(p_setChar_1_, p_setChar_2_);
    }

    public ByteBuf setFloat(int p_setFloat_1_, float p_setFloat_2_) {
        return byteBuf.setFloat(p_setFloat_1_, p_setFloat_2_);
    }

    public ByteBuf setDouble(int p_setDouble_1_, double p_setDouble_2_) {
        return byteBuf.setDouble(p_setDouble_1_, p_setDouble_2_);
    }

    public ByteBuf setBytes(int p_setBytes_1_, ByteBuf p_setBytes_2_) {
        return byteBuf.setBytes(p_setBytes_1_, p_setBytes_2_);
    }

    public ByteBuf setBytes(int p_setBytes_1_, ByteBuf p_setBytes_2_, int p_setBytes_3_) {
        return byteBuf.setBytes(p_setBytes_1_, p_setBytes_2_, p_setBytes_3_);
    }

    public ByteBuf setBytes(int p_setBytes_1_, ByteBuf p_setBytes_2_, int p_setBytes_3_, int p_setBytes_4_) {
        return byteBuf.setBytes(p_setBytes_1_, p_setBytes_2_, p_setBytes_3_, p_setBytes_4_);
    }

    public ByteBuf setBytes(int p_setBytes_1_, byte[] p_setBytes_2_) {
        return byteBuf.setBytes(p_setBytes_1_, p_setBytes_2_);
    }

    public ByteBuf setBytes(int p_setBytes_1_, byte[] p_setBytes_2_, int p_setBytes_3_, int p_setBytes_4_) {
        return byteBuf.setBytes(p_setBytes_1_, p_setBytes_2_, p_setBytes_3_, p_setBytes_4_);
    }

    public ByteBuf setBytes(int p_setBytes_1_, ByteBuffer p_setBytes_2_) {
        return byteBuf.setBytes(p_setBytes_1_, p_setBytes_2_);
    }

    public int setBytes(int p_setBytes_1_, InputStream p_setBytes_2_, int p_setBytes_3_) throws IOException {
        return byteBuf.setBytes(p_setBytes_1_, p_setBytes_2_, p_setBytes_3_);
    }

    public int setBytes(int p_setBytes_1_, ScatteringByteChannel p_setBytes_2_, int p_setBytes_3_) throws IOException {
        return byteBuf.setBytes(p_setBytes_1_, p_setBytes_2_, p_setBytes_3_);
    }

    @Override
    public int setBytes(int i, FileChannel fileChannel, long l, int i1) throws IOException {
        return 0;
    }

    public ByteBuf setZero(int p_setZero_1_, int p_setZero_2_) {
        return byteBuf.setZero(p_setZero_1_, p_setZero_2_);
    }

    @Override
    public int setCharSequence(int i, CharSequence charSequence, Charset charset) {
        return 0;
    }

    public boolean readBoolean() {
        return byteBuf.readBoolean();
    }

    public byte readByte() {
        return byteBuf.readByte();
    }

    public short readUnsignedByte() {
        return byteBuf.readUnsignedByte();
    }

    public short readShort() {
        return byteBuf.readShort();
    }

    @Override
    public short readShortLE() {
        return 0;
    }

    public int readUnsignedShort() {
        return byteBuf.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return 0;
    }

    public int readMedium() {
        return byteBuf.readMedium();
    }

    @Override
    public int readMediumLE() {
        return 0;
    }

    public int readUnsignedMedium() {
        return byteBuf.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return 0;
    }

    public int readInt() {
        return byteBuf.readInt();
    }

    @Override
    public int readIntLE() {
        return 0;
    }

    public long readUnsignedInt() {
        return byteBuf.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return 0;
    }

    public long readLong() {
        return byteBuf.readLong();
    }

    @Override
    public long readLongLE() {
        return 0;
    }

    public char readChar() {
        return byteBuf.readChar();
    }

    public float readFloat() {
        return byteBuf.readFloat();
    }

    public double readDouble() {
        return byteBuf.readDouble();
    }

    public ByteBuf readBytes(int p_readBytes_1_) {
        return byteBuf.readBytes(p_readBytes_1_);
    }

    public ByteBuf readSlice(int p_readSlice_1_) {
        return byteBuf.readSlice(p_readSlice_1_);
    }

    @Override
    public ByteBuf readRetainedSlice(int i) {
        return null;
    }

    public ByteBuf readBytes(ByteBuf p_readBytes_1_) {
        return byteBuf.readBytes(p_readBytes_1_);
    }

    public ByteBuf readBytes(ByteBuf p_readBytes_1_, int p_readBytes_2_) {
        return byteBuf.readBytes(p_readBytes_1_, p_readBytes_2_);
    }

    public ByteBuf readBytes(ByteBuf p_readBytes_1_, int p_readBytes_2_, int p_readBytes_3_) {
        return byteBuf.readBytes(p_readBytes_1_, p_readBytes_2_, p_readBytes_3_);
    }

    public ByteBuf readBytes(byte[] p_readBytes_1_) {
        return byteBuf.readBytes(p_readBytes_1_);
    }

    public ByteBuf readBytes(byte[] p_readBytes_1_, int p_readBytes_2_, int p_readBytes_3_) {
        return byteBuf.readBytes(p_readBytes_1_, p_readBytes_2_, p_readBytes_3_);
    }

    public ByteBuf readBytes(ByteBuffer p_readBytes_1_) {
        return byteBuf.readBytes(p_readBytes_1_);
    }

    public ByteBuf readBytes(OutputStream p_readBytes_1_, int p_readBytes_2_) throws IOException {
        return byteBuf.readBytes(p_readBytes_1_, p_readBytes_2_);
    }

    public int readBytes(GatheringByteChannel p_readBytes_1_, int p_readBytes_2_) throws IOException {
        return byteBuf.readBytes(p_readBytes_1_, p_readBytes_2_);
    }

    @Override
    public CharSequence readCharSequence(int i, Charset charset) {
        return null;
    }

    @Override
    public int readBytes(FileChannel fileChannel, long l, int i) throws IOException {
        return 0;
    }

    public ByteBuf skipBytes(int p_skipBytes_1_) {
        return byteBuf.skipBytes(p_skipBytes_1_);
    }

    public ByteBuf writeBoolean(boolean p_writeBoolean_1_) {
        return byteBuf.writeBoolean(p_writeBoolean_1_);
    }

    public ByteBuf writeByte(int p_writeByte_1_) {
        return byteBuf.writeByte(p_writeByte_1_);
    }

    public ByteBuf writeShort(int p_writeShort_1_) {
        return this.byteBuf.writeShort(p_writeShort_1_);
    }

    @Override
    public ByteBuf writeShortLE(int i) {
        return null;
    }

    public ByteBuf writeMedium(int p_writeMedium_1_) {
        return byteBuf.writeMedium(p_writeMedium_1_);
    }

    @Override
    public ByteBuf writeMediumLE(int i) {
        return null;
    }

    public ByteBuf writeInt(int p_writeInt_1_) {
        return byteBuf.writeInt(p_writeInt_1_);
    }

    @Override
    public ByteBuf writeIntLE(int i) {
        return null;
    }

    public ByteBuf writeLong(long p_writeLong_1_) {
        return byteBuf.writeLong(p_writeLong_1_);
    }

    @Override
    public ByteBuf writeLongLE(long l) {
        return null;
    }

    public ByteBuf writeChar(int p_writeChar_1_) {
        return byteBuf.writeChar(p_writeChar_1_);
    }

    public ByteBuf writeFloat(float p_writeFloat_1_) {
        return byteBuf.writeFloat(p_writeFloat_1_);
    }

    public ByteBuf writeDouble(double p_writeDouble_1_) {
        return byteBuf.writeDouble(p_writeDouble_1_);
    }

    public ByteBuf writeBytes(ByteBuf p_writeBytes_1_) {
        return byteBuf.writeBytes(p_writeBytes_1_);
    }

    public ByteBuf writeBytes(ByteBuf p_writeBytes_1_, int p_writeBytes_2_) {
        return byteBuf.writeBytes(p_writeBytes_1_, p_writeBytes_2_);
    }

    public ByteBuf writeBytes(ByteBuf p_writeBytes_1_, int p_writeBytes_2_, int p_writeBytes_3_) {
        return byteBuf.writeBytes(p_writeBytes_1_, p_writeBytes_2_, p_writeBytes_3_);
    }

    public ByteBuf writeBytes(byte[] p_writeBytes_1_) {
        return byteBuf.writeBytes(p_writeBytes_1_);
    }

    public ByteBuf writeBytes(byte[] p_writeBytes_1_, int p_writeBytes_2_, int p_writeBytes_3_) {
        return byteBuf.writeBytes(p_writeBytes_1_, p_writeBytes_2_, p_writeBytes_3_);
    }

    public ByteBuf writeBytes(ByteBuffer p_writeBytes_1_) {
        return byteBuf.writeBytes(p_writeBytes_1_);
    }

    public int writeBytes(InputStream p_writeBytes_1_, int p_writeBytes_2_) throws IOException {
        return byteBuf.writeBytes(p_writeBytes_1_, p_writeBytes_2_);
    }

    public int writeBytes(ScatteringByteChannel p_writeBytes_1_, int p_writeBytes_2_) throws IOException {
        return byteBuf.writeBytes(p_writeBytes_1_, p_writeBytes_2_);
    }

    @Override
    public int writeBytes(FileChannel fileChannel, long l, int i) throws IOException {
        return 0;
    }

    public ByteBuf writeZero(int p_writeZero_1_) {
        return byteBuf.writeZero(p_writeZero_1_);
    }

    @Override
    public int writeCharSequence(CharSequence charSequence, Charset charset) {
        return 0;
    }

    public int indexOf(int p_indexOf_1_, int p_indexOf_2_, byte p_indexOf_3_) {
        return byteBuf.indexOf(p_indexOf_1_, p_indexOf_2_, p_indexOf_3_);
    }

    public int bytesBefore(byte p_bytesBefore_1_) {
        return byteBuf.bytesBefore(p_bytesBefore_1_);
    }

    public int bytesBefore(int p_bytesBefore_1_, byte p_bytesBefore_2_) {
        return byteBuf.bytesBefore(p_bytesBefore_1_, p_bytesBefore_2_);
    }

    public int bytesBefore(int p_bytesBefore_1_, int p_bytesBefore_2_, byte p_bytesBefore_3_) {
        return byteBuf.bytesBefore(p_bytesBefore_1_, p_bytesBefore_2_, p_bytesBefore_3_);
    }

    @Override
    public int forEachByte(ByteProcessor byteProcessor) {
        return 0;
    }

    @Override
    public int forEachByte(int i, int i1, ByteProcessor byteProcessor) {
        return 0;
    }

    @Override
    public int forEachByteDesc(ByteProcessor byteProcessor) {
        return 0;
    }

    @Override
    public int forEachByteDesc(int i, int i1, ByteProcessor byteProcessor) {
        return 0;
    }

    public ByteBuf copy() {
        return byteBuf.copy();
    }

    public ByteBuf copy(int p_copy_1_, int p_copy_2_) {
        return byteBuf.copy(p_copy_1_, p_copy_2_);
    }

    public ByteBuf slice() {
        return byteBuf.slice();
    }

    @Override
    public ByteBuf retainedSlice() {
        return null;
    }

    public ByteBuf slice(int p_slice_1_, int p_slice_2_) {
        return byteBuf.slice(p_slice_1_, p_slice_2_);
    }

    @Override
    public ByteBuf retainedSlice(int i, int i1) {
        return null;
    }

    public ByteBuf duplicate() {
        return byteBuf.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return null;
    }

    public int nioBufferCount() {
        return byteBuf.nioBufferCount();
    }

    public ByteBuffer nioBuffer() {
        return byteBuf.nioBuffer();
    }

    public ByteBuffer nioBuffer(int p_nioBuffer_1_, int p_nioBuffer_2_) {
        return byteBuf.nioBuffer(p_nioBuffer_1_, p_nioBuffer_2_);
    }

    public ByteBuffer internalNioBuffer(int p_internalNioBuffer_1_, int p_internalNioBuffer_2_) {
        return byteBuf.internalNioBuffer(p_internalNioBuffer_1_, p_internalNioBuffer_2_);
    }

    public ByteBuffer[] nioBuffers() {
        return byteBuf.nioBuffers();
    }

    public ByteBuffer[] nioBuffers(int p_nioBuffers_1_, int p_nioBuffers_2_) {
        return byteBuf.nioBuffers(p_nioBuffers_1_, p_nioBuffers_2_);
    }

    public boolean hasArray() {
        return byteBuf.hasArray();
    }

    public byte[] array() {
        return byteBuf.array();
    }

    public int arrayOffset() {
        return byteBuf.arrayOffset();
    }

    public boolean hasMemoryAddress() {
        return byteBuf.hasMemoryAddress();
    }

    public long memoryAddress() {
        return byteBuf.memoryAddress();
    }

    public String toString(Charset p_toString_1_) {
        return byteBuf.toString(p_toString_1_);
    }

    public String toString(int p_toString_1_, int p_toString_2_, Charset p_toString_3_) {
        return byteBuf.toString(p_toString_1_, p_toString_2_, p_toString_3_);
    }

    public int hashCode() {
        return byteBuf.hashCode();
    }

    public boolean equals(Object p_equals_1_) {
        return byteBuf.equals(p_equals_1_);
    }

    public int compareTo(ByteBuf p_compareTo_1_) {
        return byteBuf.compareTo(p_compareTo_1_);
    }

    public String toString() {
        return byteBuf.toString();
    }

    public ByteBuf retain(int p_retain_1_) {
        return byteBuf.retain(p_retain_1_);
    }

    public ByteBuf retain() {
        return byteBuf.retain();
    }

    public ByteBuf touch() {
        return byteBuf.touch();
    }

    public ByteBuf touch(Object p_touch_1_) {
        return byteBuf.touch(p_touch_1_);
    }

    public int refCnt() {
        return byteBuf.refCnt();
    }

    public boolean release() {
        return byteBuf.release();
    }

    public boolean release(int p_release_1_) {
        return byteBuf.release(p_release_1_);
    }
}




