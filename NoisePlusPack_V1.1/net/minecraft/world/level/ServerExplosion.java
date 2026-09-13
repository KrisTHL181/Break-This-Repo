package net.minecraft.world.level;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ServerExplosion implements Explosion {
   private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
   private static final int MAX_DROPS_PER_COMBINED_STACK = 16;
   private static final float LARGE_EXPLOSION_RADIUS = 2.0F;
   private final boolean fire;
   private final Explosion.BlockInteraction blockInteraction;
   private final ServerLevel level;
   private final Vec3 center;
   private final @Nullable Entity source;
   private final float radius;
   private final DamageSource damageSource;
   private final ExplosionDamageCalculator damageCalculator;
   private final Map<Player, Vec3> hitPlayers = new HashMap<>();

   public ServerExplosion(
      ServerLevel p_363225_,
      @Nullable Entity p_367780_,
      @Nullable DamageSource p_367845_,
      @Nullable ExplosionDamageCalculator p_361628_,
      Vec3 p_364875_,
      float p_361128_,
      boolean p_362786_,
      Explosion.BlockInteraction p_367128_
   ) {
      this.level = p_363225_;
      this.source = p_367780_;
      this.radius = p_361128_;
      this.center = p_364875_;
      this.fire = p_362786_;
      this.blockInteraction = p_367128_;
      this.damageSource = p_367845_ == null ? p_363225_.damageSources().explosion(this) : p_367845_;
      this.damageCalculator = p_361628_ == null ? this.makeDamageCalculator(p_367780_) : p_361628_;
   }

   private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity p_362997_) {
      return p_362997_ == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(p_362997_);
   }

   public static float getSeenPercent(Vec3 p_367358_, Entity p_369280_) {
      AABB aabb = p_369280_.getBoundingBox();
      double d0 = 1.0 / ((aabb.maxX - aabb.minX) * 2.0 + 1.0);
      double d1 = 1.0 / ((aabb.maxY - aabb.minY) * 2.0 + 1.0);
      double d2 = 1.0 / ((aabb.maxZ - aabb.minZ) * 2.0 + 1.0);
      double d3 = (1.0 - Math.floor(1.0 / d0) * d0) / 2.0;
      double d4 = (1.0 - Math.floor(1.0 / d2) * d2) / 2.0;
      if (!(d0 < 0.0) && !(d1 < 0.0) && !(d2 < 0.0)) {
         int i = 0;
         int j = 0;

         for (double d5 = 0.0; d5 <= 1.0; d5 += d0) {
            for (double d6 = 0.0; d6 <= 1.0; d6 += d1) {
               for (double d7 = 0.0; d7 <= 1.0; d7 += d2) {
                  double d8 = Mth.lerp(d5, aabb.minX, aabb.maxX);
                  double d9 = Mth.lerp(d6, aabb.minY, aabb.maxY);
                  double d10 = Mth.lerp(d7, aabb.minZ, aabb.maxZ);
                  Vec3 vec3 = new Vec3(d8 + d3, d9, d10 + d4);
                  if (p_369280_.level().clip(new ClipContext(vec3, p_367358_, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p_369280_)).getType()
                     == HitResult.Type.MISS) {
                     i++;
                  }

                  j++;
               }
            }
         }

         return (float)i / j;
      } else {
         return 0.0F;
      }
   }

   @Override
   public float radius() {
      return this.radius;
   }

   @Override
   public Vec3 center() {
      return this.center;
   }

   private List<BlockPos> calculateExplodedPositions() {
      Set<BlockPos> set = new HashSet<>();
      int i = 16;

      for (int j = 0; j < 16; j++) {
         for (int k = 0; k < 16; k++) {
            for (int l = 0; l < 16; l++) {
               if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                  double d0 = j / 15.0F * 2.0F - 1.0F;
                  double d1 = k / 15.0F * 2.0F - 1.0F;
                  double d2 = l / 15.0F * 2.0F - 1.0F;
                  double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                  d0 /= d3;
                  d1 /= d3;
                  d2 /= d3;
                  float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                  double d4 = this.center.x;
                  double d5 = this.center.y;
                  double d6 = this.center.z;
                  float f1 = 0.3F;

                  while (f > 0.0F) {
                     BlockPos blockpos = BlockPos.containing(d4, d5, d6);
                     BlockState blockstate = this.level.getBlockState(blockpos);
                     FluidState fluidstate = this.level.getFluidState(blockpos);
                     if (!this.level.isInWorldBounds(blockpos)) {
                        break;
                     }

                     Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluidstate);
                     if (optional.isPresent()) {
                        f -= (optional.get() + 0.3F) * 0.3F;
                     }

                     if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f)) {
                        set.add(blockpos);
                     }

                     d4 += d0 * 0.3F;
                     d5 += d1 * 0.3F;
                     d6 += d2 * 0.3F;
                     f -= 0.22500001F;
                  }
               }
            }
         }
      }

      return new ObjectArrayList(set);
   }

   private void hurtEntities() {
      if (!(this.radius < 1.0E-5F)) {
         float f = this.radius * 2.0F;
         int i = Mth.floor(this.center.x - f - 1.0);
         int j = Mth.floor(this.center.x + f + 1.0);
         int k = Mth.floor(this.center.y - f - 1.0);
         int l = Mth.floor(this.center.y + f + 1.0);
         int i1 = Mth.floor(this.center.z - f - 1.0);
         int j1 = Mth.floor(this.center.z + f + 1.0);

         for (Entity entity : this.level.getEntities(this.source, new AABB(i, k, i1, j, l, j1))) {
            if (!entity.ignoreExplosion(this)) {
               double d0 = Math.sqrt(entity.distanceToSqr(this.center)) / f;
               if (!(d0 > 1.0)) {
                  Vec3 vec3 = entity instanceof PrimedTnt ? entity.position() : entity.getEyePosition();
                  Vec3 vec31 = vec3.subtract(this.center).normalize();
                  boolean flag = this.damageCalculator.shouldDamageEntity(this, entity);
                  float f1 = this.damageCalculator.getKnockbackMultiplier(entity);
                  float f2 = !flag && f1 == 0.0F ? 0.0F : getSeenPercent(this.center, entity);
                  if (flag) {
                     entity.hurtServer(this.level, this.damageSource, this.damageCalculator.getEntityDamageAmount(this, entity, f2));
                  }

                  double d1 = entity instanceof LivingEntity livingentity ? livingentity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE) : 0.0;
                  double d2 = (1.0 - d0) * f2 * f1 * (1.0 - d1);
                  Vec3 vec32 = vec31.scale(d2);
                  entity.push(vec32);
                  if (entity.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectile) {
                     projectile.setOwner(this.damageSource.getEntity());
                  } else if (entity instanceof Player player && !player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                     this.hitPlayers.put(player, vec32);
                  }

                  entity.onExplosionHit(this.source);
               }
            }
         }
      }
   }

   private void interactWithBlocks(List<BlockPos> p_361066_) {
      List<ServerExplosion.StackCollector> list = new ArrayList<>();
      Util.shuffle(p_361066_, this.level.random);

      for (BlockPos blockpos : p_361066_) {
         this.level.getBlockState(blockpos).onExplosionHit(this.level, blockpos, this, (p_369158_, p_366512_) -> addOrAppendStack(list, p_369158_, p_366512_));
      }

      for (ServerExplosion.StackCollector serverexplosion$stackcollector : list) {
         Block.popResource(this.level, serverexplosion$stackcollector.pos, serverexplosion$stackcollector.stack);
      }
   }

   private void createFire(List<BlockPos> p_365156_) {
      for (BlockPos blockpos : p_365156_) {
         if (this.level.random.nextInt(3) == 0 && this.level.getBlockState(blockpos).isAir() && this.level.getBlockState(blockpos.below()).isSolidRender()) {
            this.level.setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level, blockpos));
         }
      }
   }

   public int explode() {
      this.level.gameEvent(this.source, GameEvent.EXPLODE, this.center);
      List<BlockPos> list = this.calculateExplodedPositions();
      this.hurtEntities();
      if (this.interactsWithBlocks()) {
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push("explosion_blocks");
         this.interactWithBlocks(list);
         profilerfiller.pop();
      }

      if (this.fire) {
         this.createFire(list);
      }

      return list.size();
   }

   private static void addOrAppendStack(List<ServerExplosion.StackCollector> p_364783_, ItemStack p_365928_, BlockPos p_366332_) {
      for (ServerExplosion.StackCollector serverexplosion$stackcollector : p_364783_) {
         serverexplosion$stackcollector.tryMerge(p_365928_);
         if (p_365928_.isEmpty()) {
            return;
         }
      }

      p_364783_.add(new ServerExplosion.StackCollector(p_366332_, p_365928_));
   }

   private boolean interactsWithBlocks() {
      return this.blockInteraction != Explosion.BlockInteraction.KEEP;
   }

   public Map<Player, Vec3> getHitPlayers() {
      return this.hitPlayers;
   }

   @Override
   public ServerLevel level() {
      return this.level;
   }

   @Override
   public @Nullable LivingEntity getIndirectSourceEntity() {
      return Explosion.getIndirectSourceEntity(this.source);
   }

   @Override
   public @Nullable Entity getDirectSourceEntity() {
      return this.source;
   }

   public DamageSource getDamageSource() {
      return this.damageSource;
   }

   @Override
   public Explosion.BlockInteraction getBlockInteraction() {
      return this.blockInteraction;
   }

   @Override
   public boolean canTriggerBlocks() {
      if (this.blockInteraction != Explosion.BlockInteraction.TRIGGER_BLOCK) {
         return false;
      } else {
         return this.source != null && this.source.getType() == EntityType.BREEZE_WIND_CHARGE ? this.level.getGameRules().get(GameRules.MOB_GRIEFING) : true;
      }
   }

   @Override
   public boolean shouldAffectBlocklikeEntities() {
      boolean flag = this.level.getGameRules().get(GameRules.MOB_GRIEFING);
      boolean flag1 = this.source == null || this.source.getType() != EntityType.BREEZE_WIND_CHARGE && this.source.getType() != EntityType.WIND_CHARGE;
      return flag ? flag1 : this.blockInteraction.shouldAffectBlocklikeEntities() && flag1;
   }

   public boolean isSmall() {
      return this.radius < 2.0F || !this.interactsWithBlocks();
   }

   static class StackCollector {
      final BlockPos pos;
      ItemStack stack;

      StackCollector(BlockPos p_361613_, ItemStack p_361574_) {
         this.pos = p_361613_;
         this.stack = p_361574_;
      }

      public void tryMerge(ItemStack p_362306_) {
         if (ItemEntity.areMergable(this.stack, p_362306_)) {
            this.stack = ItemEntity.merge(this.stack, p_362306_, 16);
         }
      }
   }
}
