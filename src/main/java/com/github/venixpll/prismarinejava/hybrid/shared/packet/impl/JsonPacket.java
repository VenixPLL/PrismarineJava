package com.github.venixpll.prismarinejava.hybrid.shared.packet.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.venixpll.prismarinejava.hybrid.shared.packet.Packet;
import lombok.Getter;
import lombok.ToString;

@ToString
public class JsonPacket extends Packet {

    @Getter
    private JSONObject params = new JSONObject();

    public JsonPacket(){
        super(null);
    }
    
    public JsonPacket(final String name) {
        super(name);
    }
    
    public JsonPacket(final String name, final JSONObject params) {
        super(name);
        this.params = params;
    }

    public JsonPacket set(final String key, final Object value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final JSONObject value){
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final JSONArray value){
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final String value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final Double value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final Float value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final Long value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final Integer value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final Short value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final Byte value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket set(final String key, final Boolean value) {
        this.params.put(key, value);
        return this;
    }

    public JsonPacket setParams(final JSONObject params) {
        this.params = params;
        return this;
    }

    public Object get(final String key) {
        return this.params.get(key);
    }

    public byte[] getBuffer(final String key) {
        final var params = this.get(key);
        if (params == null) return null;
        if (params instanceof final JSONObject json) {
            if (json.getString("type").equals("Buffer")) {

                final var data = json.getJSONArray("data");
                final var bytes = new byte[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    bytes[i] = (byte) (data.getInteger(i) & 0xFF);
                }

                return bytes;
            }
        }
        return null;
    }

    public JsonPacket setBuffer(final String key, final byte[] bytes) {
        final var json = new JSONObject();
        json.put("type", "Buffer");

        final var array = new JSONArray();
        for (final byte b : bytes) array.add(b & 0xFF);

        json.put("data", array);
        this.params.put(key, json);

        return this;
    }

    public JSONObject toJson() {
        final var json = new JSONObject();
        json.put("name", this.getName());
        json.put("params", this.params);
        return json;
    }

    public byte[] getRawData(){
        throw new UnsupportedOperationException("unsupported");
    }

    public static JsonPacket fromJson(final JSONObject buf) {
        return new JsonPacket(buf.getString("name"), buf.getJSONObject("params"));
    }
    
}
