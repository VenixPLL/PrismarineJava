package com.github.venixpll.prismarinejava.hybrid.legacy.registry.element;

import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.impl.LegacyPacket;
import java.util.HashMap;

public class PacketRegistryElement {

    private final HashMap<String, LegacyPacket> CLIENT_BOUND = new HashMap<>(6,0.9f);
    private final HashMap<String, LegacyPacket> SERVER_BOUND = new HashMap<>(6,0.9f);

    public void subRegister(final PacketDirection direction, final LegacyPacket packet){
        final String packetName = packet.getName();
        switch(direction){
            case SERVERBOUND -> this.SERVER_BOUND.put(packetName,packet);
            case CLIENTBOUND -> this.CLIENT_BOUND.put(packetName,packet);
        }
    }

    public void subUnregister(final PacketDirection direction, final String packetName) {
        switch(direction){
            case SERVERBOUND -> this.SERVER_BOUND.remove(packetName);
            case CLIENTBOUND -> this.CLIENT_BOUND.remove(packetName);
        }
    }

    public LegacyPacket getPacket(final PacketDirection direction, final String name){
        return switch(direction){
            case SERVERBOUND -> this.SERVER_BOUND.get(name);
            case CLIENTBOUND -> this.CLIENT_BOUND.get(name);
        };
    }

}
