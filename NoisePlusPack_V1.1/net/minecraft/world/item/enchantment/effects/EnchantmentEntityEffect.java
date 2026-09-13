package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public interface EnchantmentEntityEffect extends EnchantmentLocationBasedEffect {
   Codec<EnchantmentEntityEffect> CODEC = BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE
      .byNameCodec()
      .dispatch(EnchantmentEntityEffect::codec, Function.identity());

   static MapCodec<? extends EnchantmentEntityEffect> bootstrap(Registry<MapCodec<? extends EnchantmentEntityEffect>> p_342629_) {
      Registry.register(p_342629_, "all_of", AllOf.EntityEffects.CODEC);
      Registry.register(p_342629_, "apply_mob_effect", ApplyMobEffect.CODEC);
      Registry.register(p_342629_, "change_item_damage", ChangeItemDamage.CODEC);
      Registry.register(p_342629_, "damage_entity", DamageEntity.CODEC);
      Registry.register(p_342629_, "explode", ExplodeEffect.CODEC);
      Registry.register(p_342629_, "ignite", Ignite.CODEC);
      Registry.register(p_342629_, "apply_impulse", ApplyEntityImpulse.CODEC);
      Registry.register(p_342629_, "apply_exhaustion", ApplyExhaustion.CODEC);
      Registry.register(p_342629_, "play_sound", PlaySoundEffect.CODEC);
      Registry.register(p_342629_, "replace_block", ReplaceBlock.CODEC);
      Registry.register(p_342629_, "replace_disk", ReplaceDisk.CODEC);
      Registry.register(p_342629_, "run_function", RunFunction.CODEC);
      Registry.register(p_342629_, "set_block_properties", SetBlockProperties.CODEC);
      Registry.register(p_342629_, "spawn_particles", SpawnParticlesEffect.CODEC);
      return Registry.register(p_342629_, "summon_entity", SummonEntityEffect.CODEC);
   }

   void apply(ServerLevel var1, int var2, EnchantedItemInUse var3, Entity var4, Vec3 var5);

   @Override
   default void onChangedBlock(ServerLevel p_344398_, int p_343126_, EnchantedItemInUse p_344258_, Entity p_344833_, Vec3 p_344089_, boolean p_343329_) {
      this.apply(p_344398_, p_343126_, p_344258_, p_344833_, p_344089_);
   }

   @Override
   MapCodec<? extends EnchantmentEntityEffect> codec();
}
