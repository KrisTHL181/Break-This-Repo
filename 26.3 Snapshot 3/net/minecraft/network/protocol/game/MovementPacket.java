package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public interface MovementPacket<T extends PacketListener> extends Packet<T> {
   @Override
   PacketType<? extends MovementPacket<T>> type();

   boolean hasPosition();

   boolean hasRotation();
}
