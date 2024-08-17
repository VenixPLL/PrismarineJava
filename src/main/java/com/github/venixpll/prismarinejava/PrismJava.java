package com.github.venixpll.prismarinejava;

import com.github.venixpll.prismarinejava.hybrid.legacy.packets.CustomPayloadPacket;
import com.github.venixpll.prismarinejava.hybrid.legacy.registry.PacketRegistry;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.prismarine.PrismInitializer;
import com.github.venixpll.prismarinejava.prismarine.javet.JavetInterop;
import com.github.venixpll.prismarinejava.prismarine.protocol.ProtocolTranslator;
import java.util.Objects;
import lombok.Getter;

public final class PrismJava {

    private static final ProtocolTranslator protocolTranslator = new ProtocolTranslator();

    @Getter
    private static final PacketRegistry packetRegistry = new PacketRegistry();

    static { // Dumb prismarinejs...
        packetRegistry.registerPacket(ConnectionState.CONFIG, PacketDirection.SERVERBOUND, new CustomPayloadPacket());
        packetRegistry.registerPacket(ConnectionState.PLAY, PacketDirection.SERVERBOUND, new CustomPayloadPacket());
        packetRegistry.registerPacket(ConnectionState.PLAY, PacketDirection.CLIENTBOUND, new CustomPayloadPacket());
        packetRegistry.registerPacket(ConnectionState.CONFIG, PacketDirection.CLIENTBOUND, new CustomPayloadPacket());
    }

    public static JavetInterop getJavetInterop() throws Exception {
        if(Objects.isNull(PrismInitializer.getJavetInterop())) new PrismInitializer();
        return PrismInitializer.getJavetInterop();
    }

    public static ProtocolTranslator getTranslator() {
        if(protocolTranslator.getProtocolMap().isEmpty()) protocolTranslator.load();
        return protocolTranslator;
    }
}
