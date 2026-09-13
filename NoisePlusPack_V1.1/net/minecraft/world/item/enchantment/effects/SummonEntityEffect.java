package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record SummonEntityEffect(HolderSet<EntityType<?>> entityTypes, boolean joinTeam) implements EnchantmentEntityEffect {
   public static final MapCodec<SummonEntityEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_345460_ -> p_345460_.group(
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity").forGetter(SummonEntityEffect::entityTypes),
            Codec.BOOL.optionalFieldOf("join_team", false).forGetter(SummonEntityEffect::joinTeam)
         )
         .apply(p_345460_, SummonEntityEffect::new)
   );

   @Override
   public void apply(ServerLevel p_344501_, int p_344802_, EnchantedItemInUse p_342872_, Entity p_342384_, Vec3 p_342075_) {
      BlockPos blockpos = BlockPos.containing(p_342075_);
      if (Level.isInSpawnableBounds(blockpos)) {
         Optional<Holder<EntityType<?>>> optional = this.entityTypes().getRandomElement(p_344501_.getRandom());
         if (!optional.isEmpty()) {
            Entity entity = optional.get().value().spawn(p_344501_, blockpos, EntitySpawnReason.TRIGGERED);
            if (entity != null) {
               if (entity instanceof LightningBolt lightningbolt && p_342872_.owner() instanceof ServerPlayer serverplayer) {
                  lightningbolt.setCause(serverplayer);
               }

               if (this.joinTeam && p_342384_.getTeam() != null) {
                  p_344501_.getScoreboard().addPlayerToTeam(entity.getScoreboardName(), p_342384_.getTeam());
               }

               entity.snapTo(p_342075_.x, p_342075_.y, p_342075_.z, entity.getYRot(), entity.getXRot());
            }
         }
      }
   }

   @Override
   public MapCodec<SummonEntityEffect> codec() {
      return CODEC;
   }
}
