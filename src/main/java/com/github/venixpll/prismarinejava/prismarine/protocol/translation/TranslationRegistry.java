package com.github.venixpll.prismarinejava.prismarine.protocol.translation;

import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class TranslationRegistry {

    private final Map<String,TranslatedVersion> translatedVersionMap = new HashMap<>();

    @Getter
    @RequiredArgsConstructor
    public static class TranslatedVersion {

        private final Map<ConnectionState,Map<String,Integer>> toServer;
        private final Map<ConnectionState,Map<String,Integer>> toClient;

    }

}
