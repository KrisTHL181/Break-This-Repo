package net.minecraft.world.entity.npc.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record VillagerData(Holder<VillagerType> type, Holder<VillagerProfession> profession, int level) {
   public static final int MIN_VILLAGER_LEVEL = 1;
   public static final int MAX_VILLAGER_LEVEL = 5;
   private static final int[] NEXT_LEVEL_XP_THRESHOLDS = new int[]{0, 10, 70, 150, 250};
   public static final Codec<VillagerData> CODEC = RecordCodecBuilder.create(
      p_451771_ -> p_451771_.group(
            BuiltInRegistries.VILLAGER_TYPE
               .holderByNameCodec()
               .fieldOf("type")
               .orElseGet(() -> BuiltInRegistries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS))
               .forGetter(p_456812_ -> p_456812_.type),
            BuiltInRegistries.VILLAGER_PROFESSION
               .holderByNameCodec()
               .fieldOf("profession")
               .orElseGet(() -> BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE))
               .forGetter(p_454108_ -> p_454108_.profession),
            Codec.INT.fieldOf("level").orElse(1).forGetter(p_457465_ -> p_457465_.level)
         )
         .apply(p_451771_, VillagerData::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, VillagerData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.holderRegistry(Registries.VILLAGER_TYPE),
      VillagerData::type,
      ByteBufCodecs.holderRegistry(Registries.VILLAGER_PROFESSION),
      VillagerData::profession,
      ByteBufCodecs.VAR_INT,
      VillagerData::level,
      VillagerData::new
   );

   public VillagerData {
      level = Math.max(1, level);
   }

   public VillagerData withType(Holder<VillagerType> p_454149_) {
      return new VillagerData(p_454149_, this.profession, this.level);
   }

   public VillagerData withType(HolderGetter.Provider p_460864_, ResourceKey<VillagerType> p_454581_) {
      return this.withType(p_460864_.getOrThrow(p_454581_));
   }

   public VillagerData withProfession(Holder<VillagerProfession> p_454081_) {
      return new VillagerData(this.type, p_454081_, this.level);
   }

   public VillagerData withProfession(HolderGetter.Provider p_457716_, ResourceKey<VillagerProfession> p_458279_) {
      return this.withProfession(p_457716_.getOrThrow(p_458279_));
   }

   public VillagerData withLevel(int p_455884_) {
      return new VillagerData(this.type, this.profession, p_455884_);
   }

   public static int getMinXpPerLevel(int p_454074_) {
      return canLevelUp(p_454074_) ? NEXT_LEVEL_XP_THRESHOLDS[p_454074_ - 1] : 0;
   }

   public static int getMaxXpPerLevel(int p_460462_) {
      return canLevelUp(p_460462_) ? NEXT_LEVEL_XP_THRESHOLDS[p_460462_] : 0;
   }

   public static boolean canLevelUp(int p_455114_) {
      return p_455114_ >= 1 && p_455114_ < 5;
   }
}
