package se.mickelus.tetra.blocks;

import se.mickelus.mutil.network.PacketHandler;

public interface InitializableBlock {
    default void registerPackets(PacketHandler packetHandler) {
    }

    default void clientInit() {
    }

    default void commonInit(PacketHandler packetHandler) {
    }
}
