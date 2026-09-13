package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Abilities;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public enum GameType implements StringRepresentable {
   SURVIVAL(0, "survival"),
   CREATIVE(1, "creative"),
   ADVENTURE(2, "adventure"),
   SPECTATOR(3, "spectator");

   public static final GameType DEFAULT_MODE = SURVIVAL;
   public static final StringRepresentable.EnumCodec<GameType> CODEC = StringRepresentable.fromEnum(GameType::values);
   private static final IntFunction<GameType> BY_ID = ByIdMap.continuous(GameType::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StreamCodec<ByteBuf, GameType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, GameType::getId);
   @Deprecated
   public static final Codec<GameType> LEGACY_ID_CODEC = Codec.INT.xmap(GameType::byId, GameType::getId);
   private static final int NOT_SET = -1;
   private final int id;
   private final String name;
   private final Component shortName;
   private final Component longName;

   GameType(final int id, final String name) {
      this.id = id;
      this.name = name;
      this.shortName = Component.translatable("selectWorld.gameMode." + name);
      this.longName = Component.translatable("gameMode." + name);
   }

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public Component getLongDisplayName() {
      return this.longName;
   }

   public Component getShortDisplayName() {
      return this.shortName;
   }

   public void updatePlayerAbilities(final Abilities abilities) {
      if (this == CREATIVE) {
         abilities.mayfly = true;
         abilities.instabuild = true;
         abilities.invulnerable = true;
      } else if (this == SPECTATOR) {
         abilities.mayfly = true;
         abilities.instabuild = false;
         abilities.invulnerable = true;
         abilities.flying = true;
      } else {
         abilities.mayfly = false;
         abilities.instabuild = false;
         abilities.invulnerable = false;
         abilities.flying = false;
      }

      abilities.mayBuild = !this.isBlockPlacingRestricted();
   }

   public boolean isBlockPlacingRestricted() {
      return this == ADVENTURE || this == SPECTATOR;
   }

   public boolean isCreative() {
      return this == CREATIVE;
   }

   public boolean isSurvival() {
      return this == SURVIVAL || this == ADVENTURE;
   }

   public static GameType byId(final int id) {
      return BY_ID.apply(id);
   }

   public static GameType byName(final String name) {
      return byName(name, SURVIVAL);
   }

   @Contract("_,!null->!null;_,null->_")
   public static @Nullable GameType byName(final String name, final @Nullable GameType defaultMode) {
      GameType result = CODEC.byName(name);
      return result != null ? result : defaultMode;
   }

   public static int getNullableId(final @Nullable GameType gameType) {
      return gameType != null ? gameType.id : -1;
   }

   public static @Nullable GameType byNullableId(final int id) {
      return id == -1 ? null : byId(id);
   }

   public static boolean isValidId(final int id) {
      return Arrays.stream(values()).anyMatch(gameType -> gameType.id == id);
   }
}
