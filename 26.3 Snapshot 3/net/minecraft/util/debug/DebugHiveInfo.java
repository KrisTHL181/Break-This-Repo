package net.minecraft.util.debug;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

public record DebugHiveInfo(Block type, int occupantCount, int honeyLevel, boolean sedated) {
   public static final StreamCodec<RegistryFriendlyByteBuf, DebugHiveInfo> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.registry(Registries.BLOCK),
      DebugHiveInfo::type,
      ByteBufCodecs.VAR_INT,
      DebugHiveInfo::occupantCount,
      ByteBufCodecs.VAR_INT,
      DebugHiveInfo::honeyLevel,
      ByteBufCodecs.BOOL,
      DebugHiveInfo::sedated,
      DebugHiveInfo::new
   );

   public static DebugHiveInfo pack(final BeehiveBlockEntity beehive) {
      return new DebugHiveInfo(
         beehive.getBlockState().getBlock(), beehive.getOccupantCount(), BeehiveBlockEntity.getHoneyLevel(beehive.getBlockState()), beehive.isSedated()
      );
   }
}
