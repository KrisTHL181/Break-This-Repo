package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jspecify.annotations.Nullable;

public record CommonPlayerSpawnInfo(
   Holder<DimensionType> dimensionType,
   ResourceKey<Level> dimension,
   long seed,
   GameType gameType,
   @Nullable GameType previousGameType,
   boolean isDebug,
   boolean isFlat,
   Optional<GlobalPos> lastDeathLocation,
   int portalCooldown,
   int seaLevel
) {
   public CommonPlayerSpawnInfo(RegistryFriendlyByteBuf p_331063_) {
      this(
         DimensionType.STREAM_CODEC.decode(p_331063_),
         p_331063_.readResourceKey(Registries.DIMENSION),
         p_331063_.readLong(),
         GameType.byId(p_331063_.readByte()),
         GameType.byNullableId(p_331063_.readByte()),
         p_331063_.readBoolean(),
         p_331063_.readBoolean(),
         p_331063_.readOptional(FriendlyByteBuf::readGlobalPos),
         p_331063_.readVarInt(),
         p_331063_.readVarInt()
      );
   }

   public void write(RegistryFriendlyByteBuf p_335866_) {
      DimensionType.STREAM_CODEC.encode(p_335866_, this.dimensionType);
      p_335866_.writeResourceKey(this.dimension);
      p_335866_.writeLong(this.seed);
      p_335866_.writeByte(this.gameType.getId());
      p_335866_.writeByte(GameType.getNullableId(this.previousGameType));
      p_335866_.writeBoolean(this.isDebug);
      p_335866_.writeBoolean(this.isFlat);
      p_335866_.writeOptional(this.lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
      p_335866_.writeVarInt(this.portalCooldown);
      p_335866_.writeVarInt(this.seaLevel);
   }
}
