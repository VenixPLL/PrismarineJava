package com.github.venixpll.prismarinejava.hybrid.shared.packet.impl;

import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.hybrid.shared.buffer.PacketBuffer;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.Packet;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public abstract class LegacyPacket extends Packet {

    private int packetId;

    public LegacyPacket(final String name) {
        super(name);
    }

    public LegacyPacket(final String name, final int packetId) {
        super(name);
        this.packetId = packetId;
    }

    public abstract int getPacketId(final  String version, final PacketDirection packetDirection, final ConnectionState connectionState) throws Exception;

    public abstract void translate(final JsonPacket jsonPacket) throws Exception;

    public abstract void write(final PacketBuffer buffer) throws Exception;

    public abstract void read(final PacketBuffer buffer) throws Exception;
}
