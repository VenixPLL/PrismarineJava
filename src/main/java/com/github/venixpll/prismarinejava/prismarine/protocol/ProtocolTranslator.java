package com.github.venixpll.prismarinejava.prismarine.protocol;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.venixpll.prismarinejava.hybrid.shared.ConnectionState;
import com.github.venixpll.prismarinejava.hybrid.shared.PacketDirection;
import com.github.venixpll.prismarinejava.prismarine.protocol.translation.TranslationRegistry;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

@Getter
public class ProtocolTranslator {

    private final HashMap<Integer,String> protocolMap = new HashMap<>();
    private final List<String> releasesList = new ArrayList<>();

    private final TranslationRegistry translationRegistry = new TranslationRegistry();

    public String fromProtocol(final int protocol){
        return this.protocolMap.get(protocol);
    }

    public int getPacketId(final String name, final String version, final ConnectionState connectionState, final PacketDirection packetDirection) throws Exception {
        var translatedVersion = this.translationRegistry.getTranslatedVersionMap().get(version);
        if(translatedVersion == null) {
            this.loadPacketIds(version);
            translatedVersion = this.translationRegistry.getTranslatedVersionMap().get(version);;
        }

        return switch(packetDirection){
            case CLIENTBOUND -> translatedVersion.getToServer().get(connectionState).get(name);
            case SERVERBOUND -> translatedVersion.getToClient().get(connectionState).get(name);
        };
    }

    public void load(){
        try {
            final var LOAD_URL = "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/common/protocolVersions.json";
            final var url = new URL(LOAD_URL);
            final var json = JSON.parseArray(url);

            for(int i=0;i<json.size();i++){
                final var obj = json.getJSONObject(i);
                if(obj.getBoolean("usesNetty")) {
                    final var version = obj.getString("minecraftVersion");
                    final var protocol = obj.getInteger("version");
                    this.protocolMap.put(protocol, version);

                    if(obj.containsKey("releaseType") && obj.getString("releaseType").equals("release")){
                        this.releasesList.add(version);
                    }else if(!version.contains("pre") && !version.contains("w") && !version.contains("rc")){
                        this.releasesList.add(version);
                    }

                }
            }
        }catch(final Exception exception){
            exception.printStackTrace();
        }
    }

    public void loadPacketIds(final String version) throws Exception {
        final var LOAD_URL = "https://raw.githubusercontent.com/PrismarineJS/minecraft-data/master/data/pc/" + version + "/protocol.json";
        final var url = new URL(LOAD_URL);
        final var json = JSON.parseObject(url);

        final var toServer = new HashMap<ConnectionState,Map<String,Integer>>();
        final var toClient = new HashMap<ConnectionState,Map<String,Integer>>();

        for(final var connectionState : ConnectionState.values()) {
            final var connState = json.getJSONObject(connectionState.getName());
            if(Objects.nonNull(connState)){
                for(final var direction : PacketDirection.values()){
                    final var packetDirection = connState.getJSONObject(direction.getName());
                    final var packetIds = this.readPacketIds(packetDirection);

                    //System.out.println("Mapped " + packetIds.size() + " ids in " + version + " state; " + connectionState.getName() + " direction: " + direction.getName());

                    switch (direction){
                        case SERVERBOUND -> toServer.put(connectionState,packetIds);
                        case CLIENTBOUND -> toClient.put(connectionState,packetIds);
                    }

                }
            }
        }

        this.translationRegistry.getTranslatedVersionMap().putIfAbsent(version,new TranslationRegistry.TranslatedVersion(toServer,toClient));
    }

    private Map<String,Integer> readPacketIds(final JSONObject types){
        final var result = new HashMap<String,Integer>();
        final var type = types.getJSONObject("types");

        final var packet = type.getJSONArray("packet");
        final var mapper = packet.getJSONArray(1);
        final var nameMapper = mapper.getJSONObject(0);
        assert nameMapper.containsKey("name") && nameMapper.getString("name").equals("name");

        final var typeMapper = nameMapper.getJSONArray("type");
        final var mappingsRoot = typeMapper.getJSONObject(1);

        assert  mappingsRoot.containsKey("mappings");
        final var idMap = mappingsRoot.getJSONObject("mappings");

        for(final var key : idMap.keySet()){
            final var name = idMap.getString(key);
            result.put(name,Integer.decode(key));
        }

        return result;
    }

}
