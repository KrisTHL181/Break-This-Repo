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
      i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity").forGetter(SummonEntityEffect::entityTypes),
            Codec.BOOL.optionalFieldOf("join_team", false).forGetter(SummonEntityEffect::joinTeam)
         )
         .apply(i, SummonEntityEffect::new)
   );

   @Override
   public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
      BlockPos blockPos = BlockPos.containing(position);
      if (Level.isInSpawnableBounds(blockPos)) {
         Optional<Holder<EntityType<?>>> entityType = this.entityTypes().getRandomElement(serverLevel.getRandom());
         if (!entityType.isEmpty()) {
            Entity spawned = entityType.get().value().spawn(serverLevel, blockPos, EntitySpawnReason.TRIGGERED);
            if (spawned != null) {
               if (spawned instanceof LightningBolt lightningBolt && item.owner() instanceof ServerPlayer player) {
                  lightningBolt.setCause(player);
               }

               if (this.joinTeam && entity.getTeam() != null) {
                  serverLevel.getScoreboard().addPlayerToTeam(spawned.getScoreboardName(), entity.getTeam());
               }

               spawned.snapTo(position.x, position.y, position.z, spawned.getYRot(), spawned.getXRot());
            }
         }
      }
   }

   @Override
   public MapCodec<SummonEntityEffect> codec() {
      return CODEC;
   }
}
