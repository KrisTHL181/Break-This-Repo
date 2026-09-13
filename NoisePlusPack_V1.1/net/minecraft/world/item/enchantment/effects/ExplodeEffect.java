package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record ExplodeEffect(
   boolean attributeToUser,
   Optional<Holder<DamageType>> damageType,
   Optional<LevelBasedValue> knockbackMultiplier,
   Optional<HolderSet<Block>> immuneBlocks,
   Vec3 offset,
   LevelBasedValue radius,
   boolean createFire,
   Level.ExplosionInteraction blockInteraction,
   ParticleOptions smallParticle,
   ParticleOptions largeParticle,
   WeightedList<ExplosionParticleInfo> blockParticles,
   Holder<SoundEvent> sound
) implements EnchantmentEntityEffect {
   public static final MapCodec<ExplodeEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_422020_ -> p_422020_.group(
            Codec.BOOL.optionalFieldOf("attribute_to_user", false).forGetter(ExplodeEffect::attributeToUser),
            DamageType.CODEC.optionalFieldOf("damage_type").forGetter(ExplodeEffect::damageType),
            LevelBasedValue.CODEC.optionalFieldOf("knockback_multiplier").forGetter(ExplodeEffect::knockbackMultiplier),
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("immune_blocks").forGetter(ExplodeEffect::immuneBlocks),
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(ExplodeEffect::offset),
            LevelBasedValue.CODEC.fieldOf("radius").forGetter(ExplodeEffect::radius),
            Codec.BOOL.optionalFieldOf("create_fire", false).forGetter(ExplodeEffect::createFire),
            Level.ExplosionInteraction.CODEC.fieldOf("block_interaction").forGetter(ExplodeEffect::blockInteraction),
            ParticleTypes.CODEC.fieldOf("small_particle").forGetter(ExplodeEffect::smallParticle),
            ParticleTypes.CODEC.fieldOf("large_particle").forGetter(ExplodeEffect::largeParticle),
            WeightedList.codec(ExplosionParticleInfo.CODEC).optionalFieldOf("block_particles", WeightedList.of()).forGetter(ExplodeEffect::blockParticles),
            SoundEvent.CODEC.fieldOf("sound").forGetter(ExplodeEffect::sound)
         )
         .apply(p_422020_, ExplodeEffect::new)
   );

   @Override
   public void apply(ServerLevel p_343276_, int p_344767_, EnchantedItemInUse p_342948_, Entity p_344263_, Vec3 p_343582_) {
      Vec3 vec3 = p_343582_.add(this.offset);
      p_343276_.explode(
         this.attributeToUser ? p_344263_ : null,
         this.getDamageSource(p_344263_, vec3),
         new SimpleExplosionDamageCalculator(
            this.blockInteraction != Level.ExplosionInteraction.NONE,
            this.damageType.isPresent(),
            this.knockbackMultiplier.map(p_345148_ -> p_345148_.calculate(p_344767_)),
            this.immuneBlocks
         ),
         vec3.x(),
         vec3.y(),
         vec3.z(),
         Math.max(this.radius.calculate(p_344767_), 0.0F),
         this.createFire,
         this.blockInteraction,
         this.smallParticle,
         this.largeParticle,
         this.blockParticles,
         this.sound
      );
   }

   private @Nullable DamageSource getDamageSource(Entity p_343933_, Vec3 p_345099_) {
      if (this.damageType.isEmpty()) {
         return null;
      } else {
         return this.attributeToUser ? new DamageSource(this.damageType.get(), p_343933_) : new DamageSource(this.damageType.get(), p_345099_);
      }
   }

   @Override
   public MapCodec<ExplodeEffect> codec() {
      return CODEC;
   }
}
