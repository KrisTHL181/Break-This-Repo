package net.minecraft.world.entity.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PrimedTnt extends Entity implements TraceableEntity {
   private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.BLOCK_STATE);
   public static final short DEFAULT_FUSE_TIME = 80;
   public static final int NO_FUSE = -1;
   private static final float DEFAULT_EXPLOSION_POWER = 4.0F;
   private static final BlockState DEFAULT_BLOCK_STATE = Blocks.TNT.defaultBlockState();
   private static final String TAG_BLOCK_STATE = "block_state";
   public static final String TAG_FUSE = "fuse";
   private static final String TAG_EXPLOSION_POWER = "explosion_power";
   public static final ExplosionDamageCalculator USED_PORTAL_DAMAGE_CALCULATOR = new ExplosionDamageCalculator() {
      @Override
      public boolean shouldBlockExplode(final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState state, final float power) {
         return state.is(Blocks.NETHER_PORTAL) ? false : super.shouldBlockExplode(explosion, level, pos, state, power);
      }

      @Override
      public Optional<Float> getBlockExplosionResistance(
         final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState block, final FluidState fluid
      ) {
         return block.is(Blocks.NETHER_PORTAL) ? Optional.empty() : super.getBlockExplosionResistance(explosion, level, pos, block, fluid);
      }
   };
   private @Nullable EntityReference<LivingEntity> owner;
   private boolean usedPortal;
   private float explosionPower = 4.0F;

   public PrimedTnt(final EntityType<? extends PrimedTnt> type, final Level level) {
      super(type, level);
      this.blocksBuilding = true;
   }

   public PrimedTnt(final Level level, final double x, final double y, final double z, final @Nullable LivingEntity owner) {
      this(EntityTypes.TNT, level);
      this.setPos(x, y, z);
      double rot = level.getRandom().nextDouble() * (float) (Math.PI * 2);
      this.setDeltaMovement(-Math.sin(rot) * 0.02, 0.2F, -Math.cos(rot) * 0.02);
      this.setFuse(80);
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.owner = EntityReference.of(owner);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_FUSE_ID, 80);
      entityData.define(DATA_BLOCK_STATE_ID, DEFAULT_BLOCK_STATE);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   @Override
   protected double getDefaultGravity() {
      return 0.04;
   }

   @Override
   public void tick() {
      this.handlePortal();
      this.applyGravity();
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.applyEffectsFromBlocks();
      this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
      if (this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
      }

      int fuse = this.getFuse() - 1;
      this.setFuse(fuse);
      if (fuse <= 0) {
         this.discard();
         if (!this.level().isClientSide()) {
            this.explode();
         }
      } else {
         this.updateFluidInteraction();
         if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
         }
      }

      this.setRequiresPrecisePosition(this.horizontalCollision || this.verticalCollision);
   }

   private void explode() {
      if (this.level() instanceof ServerLevel level && level.getGameRules().get(GameRules.TNT_EXPLODES)) {
         this.level()
            .explode(
               this,
               Explosion.getDefaultDamageSource(this.level(), this),
               this.usedPortal ? USED_PORTAL_DAMAGE_CALCULATOR : null,
               this.getX(),
               this.getY(0.0625),
               this.getZ(),
               this.explosionPower,
               false,
               Level.ExplosionInteraction.TNT
            );
      }
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      output.putShort("fuse", (short)this.getFuse());
      output.store("block_state", BlockState.CODEC, this.getBlockState());
      if (this.explosionPower != 4.0F) {
         output.putFloat("explosion_power", this.explosionPower);
      }

      EntityReference.store(this.owner, output, "owner");
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.setFuse(input.getShortOr("fuse", (short)80));
      this.setBlockState(input.<BlockState>read("block_state", BlockState.CODEC).orElse(DEFAULT_BLOCK_STATE));
      this.explosionPower = Mth.clamp(input.getFloatOr("explosion_power", 4.0F), 0.0F, 128.0F);
      this.owner = EntityReference.read(input, "owner");
   }

   public @Nullable LivingEntity getOwner() {
      return EntityReference.getLivingEntity(this.owner, this.level());
   }

   @Override
   public void restoreFrom(final Entity oldEntity) {
      super.restoreFrom(oldEntity);
      if (oldEntity instanceof PrimedTnt primedTnt) {
         this.owner = primedTnt.owner;
      }
   }

   public void setFuse(final int time) {
      this.entityData.set(DATA_FUSE_ID, time);
   }

   public int getFuse() {
      return this.entityData.get(DATA_FUSE_ID);
   }

   public static int getRandomShortFuse(final int fuse, final RandomSource random) {
      return random.nextInt(Math.max(1, fuse / 4)) + fuse / 8;
   }

   public void setBlockState(final BlockState blockState) {
      this.entityData.set(DATA_BLOCK_STATE_ID, blockState);
   }

   public BlockState getBlockState() {
      return this.entityData.get(DATA_BLOCK_STATE_ID);
   }

   private void setUsedPortal(final boolean usedPortal) {
      this.usedPortal = usedPortal;
   }

   @Override
   public @Nullable Entity teleport(final TeleportTransition transition) {
      Entity newEntity = super.teleport(transition);
      if (newEntity instanceof PrimedTnt tnt) {
         tnt.setUsedPortal(true);
      }

      return newEntity;
   }

   @Override
   public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }

   @Override
   public MoveSimulationType getMoveSimulationType() {
      return MoveSimulationType.SERVER_AND_CLIENT;
   }
}
