package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Interaction extends Entity implements Attackable, Targeting {
   private static final EntityDataAccessor<Float> DATA_WIDTH_ID = SynchedEntityData.defineId(Interaction.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_HEIGHT_ID = SynchedEntityData.defineId(Interaction.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_RESPONSE_ID = SynchedEntityData.defineId(Interaction.class, EntityDataSerializers.BOOLEAN);
   private static final String TAG_WIDTH = "width";
   private static final String TAG_HEIGHT = "height";
   private static final String TAG_ATTACK = "attack";
   private static final String TAG_INTERACTION = "interaction";
   private static final String TAG_RESPONSE = "response";
   private static final float DEFAULT_WIDTH = 1.0F;
   private static final float DEFAULT_HEIGHT = 1.0F;
   private static final boolean DEFAULT_RESPONSE = false;
   private Interaction.@Nullable PlayerAction attack;
   private Interaction.@Nullable PlayerAction interaction;

   public Interaction(final EntityType<?> type, final Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_WIDTH_ID, 1.0F);
      entityData.define(DATA_HEIGHT_ID, 1.0F);
      entityData.define(DATA_RESPONSE_ID, false);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.setWidth(input.getFloatOr("width", 1.0F));
      this.setHeight(input.getFloatOr("height", 1.0F));
      this.attack = input.<Interaction.PlayerAction>read("attack", Interaction.PlayerAction.CODEC).orElse(null);
      this.interaction = input.<Interaction.PlayerAction>read("interaction", Interaction.PlayerAction.CODEC).orElse(null);
      this.setResponse(input.getBooleanOr("response", false));
      this.setBoundingBox(this.makeBoundingBox());
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      output.putFloat("width", this.getWidth());
      output.putFloat("height", this.getHeight());
      output.storeNullable("attack", Interaction.PlayerAction.CODEC, this.attack);
      output.storeNullable("interaction", Interaction.PlayerAction.CODEC, this.interaction);
      output.putBoolean("response", this.getResponse());
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      super.onSyncedDataUpdated(accessor);
      if (DATA_HEIGHT_ID.equals(accessor) || DATA_WIDTH_ID.equals(accessor)) {
         this.refreshDimensions();
      }
   }

   @Override
   public boolean canBeHitByProjectile() {
      return false;
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   public boolean skipAttackInteraction(final Entity source) {
      if (source instanceof Player player) {
         this.attack = new Interaction.PlayerAction(player.getUUID(), this.level().getGameTime());
         if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverPlayer, this, player.damageSources().generic(), 1.0F, 1.0F, false);
         }

         return !this.getResponse();
      } else {
         return false;
      }
   }

   @Override
   public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      if (this.level().isClientSide()) {
         return this.getResponse() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
      }

      this.interaction = new Interaction.PlayerAction(player.getUUID(), this.level().getGameTime());
      return InteractionResult.CONSUME;
   }

   @Override
   public void tick() {
   }

   @Override
   public @Nullable LivingEntity getLastAttacker() {
      return this.attack != null ? this.level().getPlayerByUUID(this.attack.player()) : null;
   }

   @Override
   public @Nullable LivingEntity getTarget() {
      return this.interaction != null ? this.level().getPlayerByUUID(this.interaction.player()) : null;
   }

   private void setWidth(final float width) {
      this.entityData.set(DATA_WIDTH_ID, width);
   }

   private float getWidth() {
      return this.entityData.get(DATA_WIDTH_ID);
   }

   private void setHeight(final float width) {
      this.entityData.set(DATA_HEIGHT_ID, width);
   }

   private float getHeight() {
      return this.entityData.get(DATA_HEIGHT_ID);
   }

   private void setResponse(final boolean response) {
      this.entityData.set(DATA_RESPONSE_ID, response);
   }

   private boolean getResponse() {
      return this.entityData.get(DATA_RESPONSE_ID);
   }

   private EntityDimensions getDimensions() {
      return EntityDimensions.scalable(this.getWidth(), this.getHeight());
   }

   @Override
   public EntityDimensions getDimensions(final Pose pose) {
      return this.getDimensions();
   }

   @Override
   protected AABB makeBoundingBox(final Vec3 position) {
      return this.getDimensions().makeBoundingBox(position);
   }

   private record PlayerAction(UUID player, long timestamp) {
      public static final Codec<Interaction.PlayerAction> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               UUIDUtil.CODEC.fieldOf("player").forGetter(Interaction.PlayerAction::player),
               Codec.LONG.fieldOf("timestamp").forGetter(Interaction.PlayerAction::timestamp)
            )
            .apply(i, Interaction.PlayerAction::new)
      );
   }
}
