package com.github.venixpll.prismarinejava.hybrid.legacy.packets;

import com.github.venixpll.prismarinejava.PrismJava;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.hybrid.shared.buffer.PacketBuffer;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.JsonPacket;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.LegacyPacket;
import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
public class CustomPayloadPacket extends LegacyPacket {

    private String channel;
    private byte[] data;

    public CustomPayloadPacket() {
        super("custom_payload");
    }

    public CustomPayloadPacket(String channel, byte[] data){
        super("custom_payload");
        this.channel = channel;
        this.data = data;
    }

    @Override
    public int getPacketId(final String version, final PacketDirection packetDirection, final ConnectionState connectionState) throws Exception {
        return PrismJava.getTranslator().getPacketId(this.getName(),version, connectionState, packetDirection);
    }

    @Override
    public void translate(final JsonPacket jsonPacket) throws Exception {
        final var params = jsonPacket.getParams();
        if(params.containsKey("channel")) this.channel = params.getString("channel");
        if(params.containsKey("data")) this.data = jsonPacket.getBuffer("data");
    }

    @Override
    public void write(final PacketBuffer buffer) throws Exception {
        buffer.writeString(this.channel);
        buffer.writeBytes(this.data);
    }

    @Override
    public void read(final PacketBuffer buffer) throws Exception {
        this.channel = buffer.readString();
        this.data = new byte[buffer.readableBytes()];
        buffer.readBytes(this.data);
    }
}
