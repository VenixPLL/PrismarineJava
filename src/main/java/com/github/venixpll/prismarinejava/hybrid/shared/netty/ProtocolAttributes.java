package com.github.venixpll.prismarinejava.hybrid.shared.netty;

import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import io.netty.util.AttributeKey;

public final class ProtocolAttributes {

    public static final AttributeKey<ConnectionState> connectionStateKey = AttributeKey.valueOf("connectionState");
    public static final AttributeKey<PacketDirection> packetDirectionKey = AttributeKey.valueOf("packetDirection");
    public static final AttributeKey<Integer> compressionThresholdKey = AttributeKey.valueOf("compressionThreshold");
    public static final AttributeKey<String> protocolVersionKey = AttributeKey.valueOf("protocolVersion");


}
