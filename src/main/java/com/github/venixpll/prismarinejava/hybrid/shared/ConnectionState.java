package com.github.venixpll.prismarinejava.hybrid.shared;

import lombok.Getter;

@Getter
public enum ConnectionState {

    HANDSHAKING("handshaking"),
    LOGIN("login"),
    CONFIG("configuration"),
    PLAY("play"),
    STATUS("status");

    private final String name;

    ConnectionState(final String name){
        this.name = name;
    }

}
