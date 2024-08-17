package com.github.venixpll.prismarinejava.hybrid.shared;

import lombok.Getter;

@Getter
public enum PacketDirection {

    CLIENTBOUND("toClient"),SERVERBOUND("toServer");

    private final String name;

    PacketDirection(final String name){
        this.name = name;
    }

}
