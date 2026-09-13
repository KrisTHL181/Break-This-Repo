package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpawnEggItem extends Item {
   private static final Map<EntityType<?>, SpawnEggItem> BY_ID = Maps.newIdentityHashMap();

   public SpawnEggItem(Item.Properties p_43210_) {
      super(p_43210_);
      TypedEntityData<EntityType<?>> typedentitydata = this.components().get(DataComponents.ENTITY_DATA);
      if (typedentitydata != null) {
         BY_ID.put(typedentitydata.type(), this);
      }
   }

   @Override
   public InteractionResult useOn(UseOnContext p_43223_) {
      Level level = p_43223_.getLevel();
      if (!(level instanceof ServerLevel serverlevel)) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack $$4 = p_43223_.getItemInHand();
         BlockPos $$5 = p_43223_.getClickedPos();
         Direction $$6 = p_43223_.getClickedFace();
         BlockState $$7 = level.getBlockState($$5);
         if (level.getBlockEntity($$5) instanceof Spawner spawner) {
            EntityType<?> entitytype = this.getType($$4);
            if (entitytype == null) {
               return InteractionResult.FAIL;
            }

            if (!serverlevel.isSpawnerBlockEnabled()) {
               if (p_43223_.getPlayer() instanceof ServerPlayer serverplayer) {
                  serverplayer.sendSystemMessage(Component.translatable("advMode.notEnabled.spawner"));
               }

               return InteractionResult.FAIL;
            } else {
               spawner.setEntityId(entitytype, level.getRandom());
               level.sendBlockUpdated($$5, $$7, $$7, 3);
               level.gameEvent(p_43223_.getPlayer(), GameEvent.BLOCK_CHANGE, $$5);
               $$4.shrink(1);
               return InteractionResult.SUCCESS;
            }
         } else {
            BlockPos blockpos1;
            if ($$7.getCollisionShape(level, $$5).isEmpty()) {
               blockpos1 = $$5;
            } else {
               blockpos1 = $$5.relative($$6);
            }

            return this.spawnMob(p_43223_.getPlayer(), $$4, level, blockpos1, true, !Objects.equals($$5, blockpos1) && $$6 == Direction.UP);
         }
      }
   }

   private InteractionResult spawnMob(
      @Nullable LivingEntity p_428583_, ItemStack p_426433_, Level p_423756_, BlockPos p_431634_, boolean p_430577_, boolean p_427167_
   ) {
      EntityType<?> entitytype = this.getType(p_426433_);
      if (entitytype == null) {
         return InteractionResult.FAIL;
      }

      if (!entitytype.isAllowedInPeaceful() && p_423756_.getDifficulty() == Difficulty.PEACEFUL) {
         return InteractionResult.FAIL;
      }

      if (entitytype.spawn((ServerLevel)p_423756_, p_426433_, p_428583_, p_431634_, EntitySpawnReason.SPAWN_ITEM_USE, p_430577_, p_427167_) != null) {
         p_426433_.consume(1, p_428583_);
         p_423756_.gameEvent(p_428583_, GameEvent.ENTITY_PLACE, p_431634_);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   public InteractionResult use(Level p_43225_, Player p_43226_, InteractionHand p_43227_) {
      ItemStack itemstack = p_43226_.getItemInHand(p_43227_);
      BlockHitResult blockhitresult = getPlayerPOVHitResult(p_43225_, p_43226_, ClipContext.Fluid.SOURCE_ONLY);
      if (blockhitresult.getType() != HitResult.Type.BLOCK) {
         return InteractionResult.PASS;
      }

      if (p_43225_ instanceof ServerLevel serverlevel) {
         BlockPos $$7 = blockhitresult.getBlockPos();
         if (!(p_43225_.getBlockState($$7).getBlock() instanceof LiquidBlock)) {
            return InteractionResult.PASS;
         }

         if (p_43225_.mayInteract(p_43226_, $$7) && p_43226_.mayUseItemAt($$7, blockhitresult.getDirection(), itemstack)) {
            InteractionResult interactionresult = this.spawnMob(p_43226_, itemstack, p_43225_, $$7, false, false);
            if (interactionresult == InteractionResult.SUCCESS) {
               p_43226_.awardStat(Stats.ITEM_USED.get(this));
            }

            return interactionresult;
         } else {
            return InteractionResult.FAIL;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public boolean spawnsEntity(ItemStack p_331553_, EntityType<?> p_43232_) {
      return Objects.equals(this.getType(p_331553_), p_43232_);
   }

   public static @Nullable SpawnEggItem byId(@Nullable EntityType<?> p_43214_) {
      return BY_ID.get(p_43214_);
   }

   public static Iterable<SpawnEggItem> eggs() {
      return Iterables.unmodifiableIterable(BY_ID.values());
   }

   public @Nullable EntityType<?> getType(ItemStack p_334231_) {
      TypedEntityData<EntityType<?>> typedentitydata = p_334231_.get(DataComponents.ENTITY_DATA);
      return typedentitydata != null ? typedentitydata.type() : null;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return Optional.ofNullable(this.components().get(DataComponents.ENTITY_DATA))
         .map(TypedEntityData::type)
         .map(EntityType::requiredFeatures)
         .orElseGet(FeatureFlagSet::of);
   }

   public Optional<Mob> spawnOffspringFromSpawnEgg(
      Player p_43216_, Mob p_43217_, EntityType<? extends Mob> p_43218_, ServerLevel p_43219_, Vec3 p_43220_, ItemStack p_43221_
   ) {
      if (!this.spawnsEntity(p_43221_, p_43218_)) {
         return Optional.empty();
      }

      Mob mob;
      if (p_43217_ instanceof AgeableMob) {
         mob = ((AgeableMob)p_43217_).getBreedOffspring(p_43219_, (AgeableMob)p_43217_);
      } else {
         mob = p_43218_.create(p_43219_, EntitySpawnReason.SPAWN_ITEM_USE);
      }

      if (mob == null) {
         return Optional.empty();
      }

      mob.setBaby(true);
      if (!mob.isBaby()) {
         return Optional.empty();
      }

      mob.snapTo(p_43220_.x(), p_43220_.y(), p_43220_.z(), 0.0F, 0.0F);
      mob.applyComponentsFromItemStack(p_43221_);
      p_43219_.addFreshEntityWithPassengers(mob);
      p_43221_.consume(1, p_43216_);
      return Optional.of(mob);
   }

   @Override
   public boolean shouldPrintOpWarning(ItemStack p_378492_, @Nullable Player p_377094_) {
      if (p_377094_ != null && p_377094_.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
         TypedEntityData<EntityType<?>> typedentitydata = p_378492_.get(DataComponents.ENTITY_DATA);
         if (typedentitydata != null) {
            return typedentitydata.type().onlyOpCanSetNbt();
         }
      }

      return false;
   }
}
