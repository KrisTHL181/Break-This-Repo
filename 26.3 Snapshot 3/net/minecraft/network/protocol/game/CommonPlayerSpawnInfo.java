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
   public CommonPlayerSpawnInfo(final RegistryFriendlyByteBuf input) {
      this(
         DimensionType.STREAM_CODEC.decode(input),
         input.readResourceKey(Registries.DIMENSION),
         input.readLong(),
         GameType.byId(input.readByte()),
         GameType.byNullableId(input.readByte()),
         input.readBoolean(),
         input.readBoolean(),
         input.readOptional(FriendlyByteBuf::readGlobalPos),
         input.readVarInt(),
         input.readVarInt()
      );
   }

   public void write(final RegistryFriendlyByteBuf output) {
      DimensionType.STREAM_CODEC.encode(output, this.dimensionType);
      output.writeResourceKey(this.dimension);
      output.writeLong(this.seed);
      output.writeByte(this.gameType.getId());
      output.writeByte(GameType.getNullableId(this.previousGameType));
      output.writeBoolean(this.isDebug);
      output.writeBoolean(this.isFlat);
      output.writeOptional(this.lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
      output.writeVarInt(this.portalCooldown);
      output.writeVarInt(this.seaLevel);
   }
}
