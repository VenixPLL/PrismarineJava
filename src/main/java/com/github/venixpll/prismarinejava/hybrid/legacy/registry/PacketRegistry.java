package com.github.venixpll.prismarinejava.hybrid.legacy.registry;

import com.github.venixpll.prismarinejava.hybrid.legacy.registry.element.PacketRegistryElement;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.LegacyPacket;
import java.util.EnumMap;

public class PacketRegistry {

    private static final EnumMap<ConnectionState, PacketRegistryElement> packetElements = new EnumMap<>(ConnectionState.class);

    public void registerPacket(final ConnectionState connectionState, final PacketDirection packetDirection, final LegacyPacket packet){
        packetElements.merge(connectionState,new PacketRegistryElement(), (prev, current) -> prev)
                .subRegister(packetDirection,packet);
    }

    public void unregisterPacket(final ConnectionState connectionState, final PacketDirection packetDirection, final String packetName){
        final var element = packetElements.get(connectionState);
        assert element != null : "Connection state not found in registry";
        element.subUnregister(packetDirection,packetName);
    }

    public LegacyPacket getPacket(final ConnectionState connectionState, final PacketDirection packetDirection, final String packetName){
        if(!packetElements.containsKey(connectionState)) return null;
        return this.getNewInstance(packetElements.get(connectionState).getPacket(packetDirection,packetName));
    }

    /**
     * Creating new packet instance
     * @param packetIn Packet to instantiate
     * @return New packet instance
     */
    private LegacyPacket getNewInstance(final LegacyPacket packetIn) {
        if (packetIn == null) return null;
        final Class<? extends LegacyPacket> packet = packetIn.getClass();
        try {
            final var constructor = packet.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to instantiate packet \"" + packetIn.getPacketId() + ", " + packet.getName() + "\".", e);
        }
    }

}
