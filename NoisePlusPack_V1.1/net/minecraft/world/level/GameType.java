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

   GameType(final int p_46390_, final String p_46391_) {
      this.id = p_46390_;
      this.name = p_46391_;
      this.shortName = Component.translatable("selectWorld.gameMode." + p_46391_);
      this.longName = Component.translatable("gameMode." + p_46391_);
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

   public void updatePlayerAbilities(Abilities p_46399_) {
      if (this == CREATIVE) {
         p_46399_.mayfly = true;
         p_46399_.instabuild = true;
         p_46399_.invulnerable = true;
      } else if (this == SPECTATOR) {
         p_46399_.mayfly = true;
         p_46399_.instabuild = false;
         p_46399_.invulnerable = true;
         p_46399_.flying = true;
      } else {
         p_46399_.mayfly = false;
         p_46399_.instabuild = false;
         p_46399_.invulnerable = false;
         p_46399_.flying = false;
      }

      p_46399_.mayBuild = !this.isBlockPlacingRestricted();
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

   public static GameType byId(int p_46394_) {
      return BY_ID.apply(p_46394_);
   }

   public static GameType byName(String p_46401_) {
      return byName(p_46401_, SURVIVAL);
   }

   @Contract("_,!null->!null;_,null->_")
   public static @Nullable GameType byName(String p_46403_, @Nullable GameType p_46404_) {
      GameType gametype = CODEC.byName(p_46403_);
      return gametype != null ? gametype : p_46404_;
   }

   public static int getNullableId(@Nullable GameType p_151496_) {
      return p_151496_ != null ? p_151496_.id : -1;
   }

   public static @Nullable GameType byNullableId(int p_151498_) {
      return p_151498_ == -1 ? null : byId(p_151498_);
   }

   public static boolean isValidId(int p_363661_) {
      return Arrays.stream(values()).anyMatch(p_365366_ -> p_365366_.id == p_363661_);
   }
}
