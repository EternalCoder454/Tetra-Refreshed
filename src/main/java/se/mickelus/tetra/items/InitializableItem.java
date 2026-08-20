package se.mickelus.tetra.items;

import se.mickelus.mutil.network.PacketHandler;

public interface InitializableItem {
    default void registerPackets(PacketHandler packetHandler) {
    }

    default void clientInit() {
    }

    default void commonInit(PacketHandler packetHandler) {
    }
}
