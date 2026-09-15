package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class LivingEntity extends Entity implements Attackable, WaypointTransmitter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TAG_ACTIVE_EFFECTS = "active_effects";
   public static final String TAG_ATTRIBUTES = "attributes";
   public static final String TAG_SLEEPING_POS = "sleeping_pos";
   public static final String TAG_EQUIPMENT = "equipment";
   public static final String TAG_BRAIN = "Brain";
   public static final String TAG_FALL_FLYING = "FallFlying";
   public static final String TAG_HURT_TIME = "HurtTime";
   public static final String TAG_DEATH_TIME = "DeathTime";
   public static final String TAG_HURT_BY_TIMESTAMP = "HurtByTimestamp";
   public static final String TAG_HEALTH = "Health";
   private static final Identifier SPEED_MODIFIER_POWDER_SNOW_ID = Identifier.withDefaultNamespace("powder_snow");
   private static final Identifier SPRINTING_MODIFIER_ID = Identifier.withDefaultNamespace("sprinting");
   private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(
      SPRINTING_MODIFIER_ID, 0.3F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
   );
   public static final int EQUIPMENT_SLOT_OFFSET = 98;
   public static final int ARMOR_SLOT_OFFSET = 100;
   public static final int BODY_ARMOR_OFFSET = 105;
   public static final int SADDLE_OFFSET = 106;
   public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
   private static final int DAMAGE_SOURCE_TIMEOUT = 40;
   public static final double MIN_MOVEMENT_DISTANCE = 0.003;
   public static final double DEFAULT_BASE_GRAVITY = 0.08;
   public static final int DEATH_DURATION = 20;
   protected static final float INPUT_FRICTION = 0.98F;
   private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
   private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
   public static final float BASE_JUMP_POWER = 0.42F;
   protected static final float DEFAULT_KNOCKBACK = 0.4F;
   protected static final int INVULNERABLE_DURATION = 20;
   private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
   protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
   protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
   protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
   protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES = SynchedEntityData.defineId(
      LivingEntity.class, EntityDataSerializers.PARTICLES
   );
   private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(
      LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
   );
   private static final int PARTICLE_FREQUENCY_WHEN_INVISIBLE = 15;
   protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
   public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
   public static final float DEFAULT_BABY_SCALE = 0.5F;
   private static final float WATER_FLOAT_IMPULSE = 0.04F;
   public static final Predicate<LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM = p_390518_ -> {
      if (p_390518_ instanceof Player player) {
         ItemStack itemstack = player.getItemBySlot(EquipmentSlot.HEAD);
         return !itemstack.is(ItemTags.GAZE_DISGUISE_EQUIPMENT);
      } else {
         return true;
      }
   };
   private static final Dynamic<?> EMPTY_BRAIN = new Dynamic(JavaOps.INSTANCE, Map.of("memories", Map.of()));
   private final AttributeMap attributes;
   private final CombatTracker combatTracker = new CombatTracker(this);
   private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
   private final Map<EquipmentSlot, ItemStack> lastEquipmentItems = Util.makeEnumMap(EquipmentSlot.class, p_390519_ -> ItemStack.EMPTY);
   public boolean swinging;
   private boolean discardFriction = false;
   public InteractionHand swingingArm;
   public int swingTime;
   public int removeArrowTime;
   public int removeStingerTime;
   public int hurtTime;
   public int hurtDuration;
   public int deathTime;
   public float oAttackAnim;
   public float attackAnim;
   protected int attackStrengthTicker;
   protected int itemSwapTicker;
   public final WalkAnimationState walkAnimation = new WalkAnimationState();
   public float yBodyRot;
   public float yBodyRotO;
   public float yHeadRot;
   public float yHeadRotO;
   public final ElytraAnimationState elytraAnimationState = new ElytraAnimationState(this);
   protected @Nullable EntityReference<Player> lastHurtByPlayer;
   protected int lastHurtByPlayerMemoryTime;
   protected boolean dead;
   protected int noActionTime;
   protected float lastHurt;
   protected boolean jumping;
   public float xxa;
   public float yya;
   public float zza;
   protected InterpolationHandler interpolation = new InterpolationHandler(this);
   protected double lerpYHeadRot;
   protected int lerpHeadSteps;
   private boolean effectsDirty = true;
   private @Nullable EntityReference<LivingEntity> lastHurtByMob;
   private int lastHurtByMobTimestamp;
   private @Nullable LivingEntity lastHurtMob;
   private int lastHurtMobTimestamp;
   private float speed;
   private int noJumpDelay;
   private float absorptionAmount;
   protected ItemStack useItem = ItemStack.EMPTY;
   protected int useItemRemaining;
   protected int fallFlyTicks;
   private long lastKineticHitFeedbackTime = -2147483648L;
   private BlockPos lastPos;
   private Optional<BlockPos> lastClimbablePos = Optional.empty();
   private @Nullable DamageSource lastDamageSource;
   private long lastDamageStamp;
   protected int autoSpinAttackTicks;
   protected float autoSpinAttackDmg;
   protected @Nullable ItemStack autoSpinAttackItemStack;
   protected @Nullable Object2LongMap<Entity> recentKineticEnemies;
   private float swimAmount;
   private float swimAmountO;
   protected Brain<?> brain;
   private boolean skipDropExperience;
   private final EnumMap<EquipmentSlot, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>> activeLocationDependentEnchantments = new EnumMap<>(
      EquipmentSlot.class
   );
   protected final EntityEquipment equipment;
   private Waypoint.Icon locatorBarIcon = new Waypoint.Icon();

   protected LivingEntity(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
      super(p_20966_, p_20967_);
      this.attributes = new AttributeMap(DefaultAttributes.getSupplier(p_20966_));
      this.setHealth(this.getMaxHealth());
      this.equipment = this.createEquipment();
      this.blocksBuilding = true;
      this.reapplyPosition();
      this.setYRot(this.random.nextFloat() * (float) (Math.PI * 2));
      this.yHeadRot = this.getYRot();
      this.brain = this.makeBrain(EMPTY_BRAIN);
   }

   @Override
   public @Nullable LivingEntity asLivingEntity() {
      return this;
   }

   @Contract(pure = true)
   protected EntityEquipment createEquipment() {
      return new EntityEquipment();
   }

   public Brain<?> getBrain() {
      return this.brain;
   }

   protected Brain.Provider<?> brainProvider() {
      return Brain.provider(ImmutableList.of(), ImmutableList.of());
   }

   protected Brain<?> makeBrain(Dynamic<?> p_21069_) {
      return this.brainProvider().makeBrain(p_21069_);
   }

   @Override
   public void kill(ServerLevel p_367431_) {
      this.hurtServer(p_367431_, this.damageSources().genericKill(), Float.MAX_VALUE);
   }

   public boolean canAttackType(EntityType<?> p_21032_) {
      return true;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_329703_) {
      p_329703_.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
      p_329703_.define(DATA_EFFECT_PARTICLES, List.of());
      p_329703_.define(DATA_EFFECT_AMBIENCE_ID, false);
      p_329703_.define(DATA_ARROW_COUNT_ID, 0);
      p_329703_.define(DATA_STINGER_COUNT_ID, 0);
      p_329703_.define(DATA_HEALTH_ID, 1.0F);
      p_329703_.define(SLEEPING_POS_ID, Optional.empty());
   }

   public static AttributeSupplier.Builder createLivingAttributes() {
      return AttributeSupplier.builder()
         .add(Attributes.MAX_HEALTH)
         .add(Attributes.KNOCKBACK_RESISTANCE)
         .add(Attributes.MOVEMENT_SPEED)
         .add(Attributes.ARMOR)
         .add(Attributes.ARMOR_TOUGHNESS)
         .add(Attributes.MAX_ABSORPTION)
         .add(Attributes.STEP_HEIGHT)
         .add(Attributes.SCALE)
         .add(Attributes.GRAVITY)
         .add(Attributes.SAFE_FALL_DISTANCE)
         .add(Attributes.FALL_DAMAGE_MULTIPLIER)
         .add(Attributes.JUMP_STRENGTH)
         .add(Attributes.OXYGEN_BONUS)
         .add(Attributes.BURNING_TIME)
         .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
         .add(Attributes.WATER_MOVEMENT_EFFICIENCY)
         .add(Attributes.MOVEMENT_EFFICIENCY)
         .add(Attributes.ATTACK_KNOCKBACK)
         .add(Attributes.CAMERA_DISTANCE)
         .add(Attributes.WAYPOINT_TRANSMIT_RANGE);
   }

   @Override
   protected void checkFallDamage(double p_20990_, boolean p_20991_, BlockState p_20992_, BlockPos p_20993_) {
      if (!this.isInWater()) {
         this.updateInWaterStateAndDoWaterCurrentPushing();
      }

      if (this.level() instanceof ServerLevel serverlevel && p_20991_ && this.fallDistance > 0.0) {
         this.onChangedBlock(serverlevel, p_20993_);
         double d6 = Math.max(0, Mth.floor(this.calculateFallPower(this.fallDistance)));
         if (d6 > 0.0 && !p_20992_.isAir()) {
            double d0 = this.getX();
            double d1 = this.getY();
            double d2 = this.getZ();
            BlockPos blockpos = this.blockPosition();
            if (p_20993_.getX() != blockpos.getX() || p_20993_.getZ() != blockpos.getZ()) {
               double d3 = d0 - p_20993_.getX() - 0.5;
               double d4 = d2 - p_20993_.getZ() - 0.5;
               double d5 = Math.max(Math.abs(d3), Math.abs(d4));
               d0 = p_20993_.getX() + 0.5 + d3 / d5 * 0.5;
               d2 = p_20993_.getZ() + 0.5 + d4 / d5 * 0.5;
            }

            double d7 = Math.min(0.2F + d6 / 15.0, 2.5);
            int i = (int)(150.0 * d7);
            serverlevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, p_20992_), d0, d1, d2, i, 0.0, 0.0, 0.0, 0.15F);
         }
      }

      super.checkFallDamage(p_20990_, p_20991_, p_20992_, p_20993_);
      if (p_20991_) {
         this.lastClimbablePos = Optional.empty();
      }
   }

   public boolean canBreatheUnderwater() {
      return this.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
   }

   public float getSwimAmount(float p_20999_) {
      return Mth.lerp(p_20999_, this.swimAmountO, this.swimAmount);
   }

   public boolean hasLandedInLiquid() {
      return this.getDeltaMovement().y() < 1.0E-5F && this.isInLiquid();
   }

   @Override
   public void baseTick() {
      this.oAttackAnim = this.attackAnim;
      if (this.firstTick) {
         this.getSleepingPos().ifPresent(this::setPosToBed);
      }

      if (this.level() instanceof ServerLevel serverlevel) {
         EnchantmentHelper.tickEffects(serverlevel, this);
      }

      super.baseTick();
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("livingEntityBaseTick");
      if (this.isAlive() && this.level() instanceof ServerLevel serverlevel1) {
         boolean flag = this instanceof Player;
         if (this.isInWall()) {
            this.hurtServer(serverlevel1, this.damageSources().inWall(), 1.0F);
         } else if (flag && !serverlevel1.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
            double d0 = serverlevel1.getWorldBorder().getDistanceToBorder(this) + serverlevel1.getWorldBorder().getSafeZone();
            if (d0 < 0.0) {
               double d1 = serverlevel1.getWorldBorder().getDamagePerBlock();
               if (d1 > 0.0) {
                  this.hurtServer(serverlevel1, this.damageSources().outOfBorder(), Math.max(1, Mth.floor(-d0 * d1)));
               }
            }
         }

         if (this.isEyeInFluid(FluidTags.WATER)
            && !serverlevel1.getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
            boolean flag1 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!flag || !((Player)this).getAbilities().invulnerable);
            if (flag1) {
               this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
               if (this.shouldTakeDrowningDamage()) {
                  this.setAirSupply(0);
                  serverlevel1.broadcastEntityEvent(this, (byte)67);
                  this.hurtServer(serverlevel1, this.damageSources().drown(), 2.0F);
               }
            } else if (this.getAirSupply() < this.getMaxAirSupply() && MobEffectUtil.shouldEffectsRefillAirsupply(this)) {
               this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }

            if (this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
               this.stopRiding();
            }
         } else if (this.getAirSupply() < this.getMaxAirSupply()) {
            this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
         }

         BlockPos blockpos = this.blockPosition();
         if (!Objects.equal(this.lastPos, blockpos)) {
            this.lastPos = blockpos;
            this.onChangedBlock(serverlevel1, blockpos);
         }
      }

      if (this.hurtTime > 0) {
         this.hurtTime--;
      }

      if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
         this.invulnerableTime--;
      }

      if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
         this.tickDeath();
      }

      if (this.lastHurtByPlayerMemoryTime > 0) {
         this.lastHurtByPlayerMemoryTime--;
      } else {
         this.lastHurtByPlayer = null;
      }

      if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
         this.lastHurtMob = null;
      }

      LivingEntity livingentity = this.getLastHurtByMob();
      if (livingentity != null) {
         if (!livingentity.isAlive()) {
            this.setLastHurtByMob(null);
         } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
            this.setLastHurtByMob(null);
         }
      }

      this.tickEffects();
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      profilerfiller.pop();
   }

   protected boolean shouldTakeDrowningDamage() {
      return this.getAirSupply() <= -20;
   }

   @Override
   protected float getBlockSpeedFactor() {
      return Mth.lerp((float)this.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
   }

   public float getLuck() {
      return 0.0F;
   }

   protected void removeFrost() {
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attributeinstance != null && attributeinstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
         attributeinstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
      }
   }

   protected void tryAddFrost() {
      if (!this.getBlockStateOnLegacy().isAir()) {
         int i = this.getTicksFrozen();
         if (i > 0) {
            AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attributeinstance == null) {
               return;
            }

            float f = -0.05F * this.getPercentFrozen();
            attributeinstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, f, AttributeModifier.Operation.ADD_VALUE));
         }
      }
   }

   protected void onChangedBlock(ServerLevel p_343619_, BlockPos p_21175_) {
      EnchantmentHelper.runLocationChangedEffects(p_343619_, this);
   }

   public boolean isBaby() {
      return false;
   }

   public float getAgeScale() {
      return this.isBaby() ? 0.5F : 1.0F;
   }

   public final float getScale() {
      AttributeMap attributemap = this.getAttributes();
      return attributemap == null ? 1.0F : this.sanitizeScale((float)attributemap.getValue(Attributes.SCALE));
   }

   protected float sanitizeScale(float p_330116_) {
      return p_330116_;
   }

   public boolean isAffectedByFluids() {
      return true;
   }

   protected void tickDeath() {
      this.deathTime++;
      if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
         this.level().broadcastEntityEvent(this, (byte)60);
         this.remove(Entity.RemovalReason.KILLED);
      }
   }

   public boolean shouldDropExperience() {
      return !this.isBaby();
   }

   protected boolean shouldDropLoot(ServerLevel p_429041_) {
      return !this.isBaby() && p_429041_.getGameRules().get(GameRules.MOB_DROPS);
   }

   protected int decreaseAirSupply(int p_21303_) {
      AttributeInstance attributeinstance = this.getAttribute(Attributes.OXYGEN_BONUS);
      double d0;
      if (attributeinstance != null) {
         d0 = attributeinstance.getValue();
      } else {
         d0 = 0.0;
      }

      return d0 > 0.0 && this.random.nextDouble() >= 1.0 / (d0 + 1.0) ? p_21303_ : p_21303_ - 1;
   }

   protected int increaseAirSupply(int p_21307_) {
      return Math.min(p_21307_ + 4, this.getMaxAirSupply());
   }

   public final int getExperienceReward(ServerLevel p_342563_, @Nullable Entity p_343916_) {
      return EnchantmentHelper.processMobExperience(p_342563_, p_343916_, this, this.getBaseExperienceReward(p_342563_));
   }

   protected int getBaseExperienceReward(ServerLevel p_361539_) {
      return 0;
   }

   protected boolean isAlwaysExperienceDropper() {
      return false;
   }

   public @Nullable LivingEntity getLastHurtByMob() {
      return EntityReference.getLivingEntity(this.lastHurtByMob, this.level());
   }

   public @Nullable Player getLastHurtByPlayer() {
      return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
   }

   @Override
   public LivingEntity getLastAttacker() {
      return this.getLastHurtByMob();
   }

   public int getLastHurtByMobTimestamp() {
      return this.lastHurtByMobTimestamp;
   }

   public void setLastHurtByPlayer(Player p_395411_, int p_391223_) {
      this.setLastHurtByPlayer(EntityReference.of(p_395411_), p_391223_);
   }

   public void setLastHurtByPlayer(UUID p_393536_, int p_396369_) {
      this.setLastHurtByPlayer(EntityReference.of(p_393536_), p_396369_);
   }

   private void setLastHurtByPlayer(EntityReference<Player> p_394032_, int p_392614_) {
      this.lastHurtByPlayer = p_394032_;
      this.lastHurtByPlayerMemoryTime = p_392614_;
   }

   public void setLastHurtByMob(@Nullable LivingEntity p_21039_) {
      this.lastHurtByMob = EntityReference.of(p_21039_);
      this.lastHurtByMobTimestamp = this.tickCount;
   }

   public @Nullable LivingEntity getLastHurtMob() {
      return this.lastHurtMob;
   }

   public int getLastHurtMobTimestamp() {
      return this.lastHurtMobTimestamp;
   }

   public void setLastHurtMob(Entity p_21336_) {
      if (p_21336_ instanceof LivingEntity) {
         this.lastHurtMob = (LivingEntity)p_21336_;
      } else {
         this.lastHurtMob = null;
      }

      this.lastHurtMobTimestamp = this.tickCount;
   }

   public int getNoActionTime() {
      return this.noActionTime;
   }

   public void setNoActionTime(int p_21311_) {
      this.noActionTime = p_21311_;
   }

   public boolean shouldDiscardFriction() {
      return this.discardFriction;
   }

   public void setDiscardFriction(boolean p_147245_) {
      this.discardFriction = p_147245_;
   }

   protected boolean doesEmitEquipEvent(EquipmentSlot p_217035_) {
      return true;
   }

   public void onEquipItem(EquipmentSlot p_238393_, ItemStack p_238394_, ItemStack p_238395_) {
      if (!this.level().isClientSide() && !this.isSpectator() && !ItemStack.isSameItemSameComponents(p_238394_, p_238395_) && !this.firstTick) {
         Equippable equippable = p_238395_.get(DataComponents.EQUIPPABLE);
         if (!this.isSilent() && equippable != null && p_238393_ == equippable.slot()) {
            this.level()
               .playSeededSound(
                  null,
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  this.getEquipSound(p_238393_, p_238395_, equippable),
                  this.getSoundSource(),
                  1.0F,
                  1.0F,
                  this.random.nextLong()
               );
         }

         if (this.doesEmitEquipEvent(p_238393_)) {
            this.gameEvent(equippable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
         }
      }
   }

   protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_397900_, ItemStack p_395451_, Equippable p_396028_) {
      return p_396028_.equipSound();
   }

   @Override
   public void remove(Entity.RemovalReason p_276115_) {
      if ((p_276115_ == Entity.RemovalReason.KILLED || p_276115_ == Entity.RemovalReason.DISCARDED) && this.level() instanceof ServerLevel serverlevel) {
         this.triggerOnDeathMobEffects(serverlevel, p_276115_);
      }

      super.remove(p_276115_);
      this.brain.clearMemories();
   }

   @Override
   public void onRemoval(Entity.RemovalReason p_409817_) {
      super.onRemoval(p_409817_);
      if (this.level() instanceof ServerLevel serverlevel) {
         serverlevel.getWaypointManager().untrackWaypoint(this);
      }
   }

   protected void triggerOnDeathMobEffects(ServerLevel p_366893_, Entity.RemovalReason p_344022_) {
      for (MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
         mobeffectinstance.onMobRemoved(p_366893_, this, p_344022_);
      }

      this.activeEffects.clear();
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_408314_) {
      p_408314_.putFloat("Health", this.getHealth());
      p_408314_.putShort("HurtTime", (short)this.hurtTime);
      p_408314_.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
      p_408314_.putShort("DeathTime", (short)this.deathTime);
      p_408314_.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
      p_408314_.store("attributes", AttributeInstance.Packed.LIST_CODEC, this.getAttributes().pack());
      if (!this.activeEffects.isEmpty()) {
         p_408314_.store("active_effects", MobEffectInstance.CODEC.listOf(), List.copyOf(this.activeEffects.values()));
      }

      p_408314_.putBoolean("FallFlying", this.isFallFlying());
      this.getSleepingPos().ifPresent(p_405284_ -> p_408314_.store("sleeping_pos", BlockPos.CODEC, p_405284_));
      DataResult<Dynamic<?>> dataresult = this.brain.serializeStart(NbtOps.INSTANCE).map(p_405291_ -> new Dynamic(NbtOps.INSTANCE, p_405291_));
      dataresult.resultOrPartial(LOGGER::error).ifPresent(p_405286_ -> p_408314_.store("Brain", Codec.PASSTHROUGH, p_405286_));
      if (this.lastHurtByPlayer != null) {
         this.lastHurtByPlayer.store(p_408314_, "last_hurt_by_player");
         p_408314_.putInt("last_hurt_by_player_memory_time", this.lastHurtByPlayerMemoryTime);
      }

      if (this.lastHurtByMob != null) {
         this.lastHurtByMob.store(p_408314_, "last_hurt_by_mob");
         p_408314_.putInt("ticks_since_last_hurt_by_mob", this.tickCount - this.lastHurtByMobTimestamp);
      }

      if (!this.equipment.isEmpty()) {
         p_408314_.store("equipment", EntityEquipment.CODEC, this.equipment);
      }

      if (this.locatorBarIcon.hasData()) {
         p_408314_.store("locator_bar_icon", Waypoint.Icon.CODEC, this.locatorBarIcon);
      }
   }

   public @Nullable ItemEntity drop(ItemStack p_395943_, boolean p_393118_, boolean p_392158_) {
      if (p_395943_.isEmpty()) {
         return null;
      }

      if (this.level().isClientSide()) {
         this.swing(InteractionHand.MAIN_HAND);
         return null;
      }

      ItemEntity itementity = this.createItemStackToDrop(p_395943_, p_393118_, p_392158_);
      if (itementity != null) {
         this.level().addFreshEntity(itementity);
      }

      return itementity;
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406679_) {
      this.internalSetAbsorptionAmount(p_406679_.getFloatOr("AbsorptionAmount", 0.0F));
      if (this.level() != null && !this.level().isClientSide()) {
         p_406679_.<List<AttributeInstance.Packed>>read("attributes", AttributeInstance.Packed.LIST_CODEC).ifPresent(this.getAttributes()::apply);
      }

      List<MobEffectInstance> list = p_406679_.<List<MobEffectInstance>>read("active_effects", MobEffectInstance.CODEC.listOf()).orElse(List.of());
      this.activeEffects.clear();

      for (MobEffectInstance mobeffectinstance : list) {
         this.activeEffects.put(mobeffectinstance.getEffect(), mobeffectinstance);
         this.effectsDirty = true;
      }

      this.setHealth(p_406679_.getFloatOr("Health", this.getMaxHealth()));
      this.hurtTime = p_406679_.getShortOr("HurtTime", (short)0);
      this.deathTime = p_406679_.getShortOr("DeathTime", (short)0);
      this.lastHurtByMobTimestamp = p_406679_.getIntOr("HurtByTimestamp", 0);
      p_406679_.getString("Team").ifPresent(p_449419_ -> {
         Scoreboard scoreboard = this.level().getScoreboard();
         PlayerTeam playerteam = scoreboard.getPlayerTeam(p_449419_);
         boolean flag = playerteam != null && scoreboard.addPlayerToTeam(this.getStringUUID(), playerteam);
         if (!flag) {
            LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", p_449419_);
         }
      });
      this.setSharedFlag(7, p_406679_.getBooleanOr("FallFlying", false));
      p_406679_.<BlockPos>read("sleeping_pos", BlockPos.CODEC).ifPresentOrElse(p_390515_ -> {
         this.setSleepingPos(p_390515_);
         this.entityData.set(DATA_POSE, Pose.SLEEPING);
         if (!this.firstTick) {
            this.setPosToBed(p_390515_);
         }
      }, this::clearSleepingPos);
      p_406679_.<Dynamic>read("Brain", Codec.PASSTHROUGH).ifPresent(p_405289_ -> this.brain = this.makeBrain((Dynamic<?>)p_405289_));
      this.lastHurtByPlayer = EntityReference.read(p_406679_, "last_hurt_by_player");
      this.lastHurtByPlayerMemoryTime = p_406679_.getIntOr("last_hurt_by_player_memory_time", 0);
      this.lastHurtByMob = EntityReference.read(p_406679_, "last_hurt_by_mob");
      this.lastHurtByMobTimestamp = p_406679_.getIntOr("ticks_since_last_hurt_by_mob", 0) + this.tickCount;
      this.equipment.setAll(p_406679_.<EntityEquipment>read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new));
      this.locatorBarIcon = p_406679_.<Waypoint.Icon>read("locator_bar_icon", Waypoint.Icon.CODEC).orElseGet(Waypoint.Icon::new);
   }

   @Override
   public void updateDataBeforeSync() {
      super.updateDataBeforeSync();
      this.updateDirtyEffects();
   }

   protected void tickEffects() {
      if (this.level() instanceof ServerLevel serverlevel) {
         Iterator<Holder<MobEffect>> iterator = this.activeEffects.keySet().iterator();

         try {
            while (iterator.hasNext()) {
               Holder<MobEffect> holder = iterator.next();
               MobEffectInstance mobeffectinstance = this.activeEffects.get(holder);
               if (!mobeffectinstance.tickServer(serverlevel, this, () -> this.onEffectUpdated(mobeffectinstance, true, null))) {
                  iterator.remove();
                  this.onEffectsRemoved(List.of(mobeffectinstance));
               } else if (mobeffectinstance.getDuration() % 600 == 0) {
                  this.onEffectUpdated(mobeffectinstance, false, null);
               }
            }
         } catch (ConcurrentModificationException var6) {
         }
      } else {
         for (MobEffectInstance mobeffectinstance1 : this.activeEffects.values()) {
            mobeffectinstance1.tickClient();
         }

         List<ParticleOptions> list = this.entityData.get(DATA_EFFECT_PARTICLES);
         if (!list.isEmpty()) {
            boolean flag = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
            int j = this.isInvisible() ? 15 : 4;
            int i = flag ? 5 : 1;
            if (this.random.nextInt(j * i) == 0) {
               this.level().addParticle(Util.getRandom(list, this.random), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 1.0, 1.0, 1.0);
            }
         }
      }
   }

   private void updateDirtyEffects() {
      if (this.effectsDirty) {
         this.updateInvisibilityStatus();
         this.updateGlowingStatus();
         this.effectsDirty = false;
      }
   }

   protected void updateInvisibilityStatus() {
      if (this.activeEffects.isEmpty()) {
         this.removeEffectParticles();
         this.setInvisible(false);
      } else {
         this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
         this.updateSynchronizedMobEffectParticles();
      }
   }

   private void updateSynchronizedMobEffectParticles() {
      List<ParticleOptions> list = this.activeEffects
         .values()
         .stream()
         .filter(MobEffectInstance::isVisible)
         .map(MobEffectInstance::getParticleOptions)
         .toList();
      this.entityData.set(DATA_EFFECT_PARTICLES, list);
      this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(this.activeEffects.values()));
   }

   private void updateGlowingStatus() {
      boolean flag = this.isCurrentlyGlowing();
      if (this.getSharedFlag(6) != flag) {
         this.setSharedFlag(6, flag);
      }
   }

   public double getVisibilityPercent(@Nullable Entity p_20969_) {
      double d0 = 1.0;
      if (this.isDiscrete()) {
         d0 *= 0.8;
      }

      if (this.isInvisible()) {
         float f = this.getArmorCoverPercentage();
         if (f < 0.1F) {
            f = 0.1F;
         }

         d0 *= 0.7 * f;
      }

      if (p_20969_ != null) {
         ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
         EntityType<?> entitytype = p_20969_.getType();
         if (entitytype == EntityType.SKELETON && itemstack.is(Items.SKELETON_SKULL)
            || entitytype == EntityType.ZOMBIE && itemstack.is(Items.ZOMBIE_HEAD)
            || entitytype == EntityType.PIGLIN && itemstack.is(Items.PIGLIN_HEAD)
            || entitytype == EntityType.PIGLIN_BRUTE && itemstack.is(Items.PIGLIN_HEAD)
            || entitytype == EntityType.CREEPER && itemstack.is(Items.CREEPER_HEAD)) {
            d0 *= 0.5;
         }
      }

      return d0;
   }

   public boolean canAttack(LivingEntity p_21171_) {
      return p_21171_ instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : p_21171_.canBeSeenAsEnemy();
   }

   public boolean canBeSeenAsEnemy() {
      return !this.isInvulnerable() && this.canBeSeenByAnyone();
   }

   public boolean canBeSeenByAnyone() {
      return !this.isSpectator() && this.isAlive();
   }

   public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> p_21180_) {
      for (MobEffectInstance mobeffectinstance : p_21180_) {
         if (mobeffectinstance.isVisible() && !mobeffectinstance.isAmbient()) {
            return false;
         }
      }

      return true;
   }

   protected void removeEffectParticles() {
      this.entityData.set(DATA_EFFECT_PARTICLES, List.of());
   }

   public boolean removeAllEffects() {
      if (this.level().isClientSide()) {
         return false;
      }

      if (this.activeEffects.isEmpty()) {
         return false;
      }

      Map<Holder<MobEffect>, MobEffectInstance> map = Maps.newHashMap(this.activeEffects);
      this.activeEffects.clear();
      this.onEffectsRemoved(map.values());
      return true;
   }

   public Collection<MobEffectInstance> getActiveEffects() {
      return this.activeEffects.values();
   }

   public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
      return this.activeEffects;
   }

   public boolean hasEffect(Holder<MobEffect> p_329256_) {
      return this.activeEffects.containsKey(p_329256_);
   }

   public @Nullable MobEffectInstance getEffect(Holder<MobEffect> p_328338_) {
      return this.activeEffects.get(p_328338_);
   }

   public float getEffectBlendFactor(Holder<MobEffect> p_397089_, float p_396175_) {
      MobEffectInstance mobeffectinstance = this.getEffect(p_397089_);
      return mobeffectinstance != null ? mobeffectinstance.getBlendFactor(this, p_396175_) : 0.0F;
   }

   public final boolean addEffect(MobEffectInstance p_21165_) {
      return this.addEffect(p_21165_, null);
   }

   public boolean addEffect(MobEffectInstance p_147208_, @Nullable Entity p_147209_) {
      if (!this.canBeAffected(p_147208_)) {
         return false;
      }

      MobEffectInstance mobeffectinstance = this.activeEffects.get(p_147208_.getEffect());
      boolean flag = false;
      if (mobeffectinstance == null) {
         this.activeEffects.put(p_147208_.getEffect(), p_147208_);
         this.onEffectAdded(p_147208_, p_147209_);
         flag = true;
         p_147208_.onEffectAdded(this);
      } else if (mobeffectinstance.update(p_147208_)) {
         this.onEffectUpdated(mobeffectinstance, true, p_147209_);
         flag = true;
      }

      p_147208_.onEffectStarted(this);
      return flag;
   }

   public boolean canBeAffected(MobEffectInstance p_21197_) {
      if (this.getType().is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
         return !p_21197_.is(MobEffects.INFESTED);
      } else if (this.getType().is(EntityTypeTags.IMMUNE_TO_OOZING)) {
         return !p_21197_.is(MobEffects.OOZING);
      } else {
         return !this.getType().is(EntityTypeTags.IGNORES_POISON_AND_REGEN) ? true : !p_21197_.is(MobEffects.REGENERATION) && !p_21197_.is(MobEffects.POISON);
      }
   }

   public void forceAddEffect(MobEffectInstance p_147216_, @Nullable Entity p_147217_) {
      if (this.canBeAffected(p_147216_)) {
         MobEffectInstance mobeffectinstance = this.activeEffects.put(p_147216_.getEffect(), p_147216_);
         if (mobeffectinstance == null) {
            this.onEffectAdded(p_147216_, p_147217_);
         } else {
            p_147216_.copyBlendState(mobeffectinstance);
            this.onEffectUpdated(p_147216_, true, p_147217_);
         }
      }
   }

   public boolean isInvertedHealAndHarm() {
      return this.getType().is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
   }

   public final @Nullable MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> p_329442_) {
      return this.activeEffects.remove(p_329442_);
   }

   public boolean removeEffect(Holder<MobEffect> p_335910_) {
      MobEffectInstance mobeffectinstance = this.removeEffectNoUpdate(p_335910_);
      if (mobeffectinstance != null) {
         this.onEffectsRemoved(List.of(mobeffectinstance));
         return true;
      } else {
         return false;
      }
   }

   protected void onEffectAdded(MobEffectInstance p_147190_, @Nullable Entity p_147191_) {
      if (!this.level().isClientSide()) {
         this.effectsDirty = true;
         p_147190_.getEffect().value().addAttributeModifiers(this.getAttributes(), p_147190_.getAmplifier());
         this.sendEffectToPassengers(p_147190_);
      }
   }

   public void sendEffectToPassengers(MobEffectInstance p_289695_) {
      for (Entity entity : this.getPassengers()) {
         if (entity instanceof ServerPlayer serverplayer) {
            serverplayer.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), p_289695_, false));
         }
      }
   }

   protected void onEffectUpdated(MobEffectInstance p_147192_, boolean p_147193_, @Nullable Entity p_147194_) {
      if (!this.level().isClientSide()) {
         this.effectsDirty = true;
         if (p_147193_) {
            MobEffect mobeffect = p_147192_.getEffect().value();
            mobeffect.removeAttributeModifiers(this.getAttributes());
            mobeffect.addAttributeModifiers(this.getAttributes(), p_147192_.getAmplifier());
            this.refreshDirtyAttributes();
         }

         this.sendEffectToPassengers(p_147192_);
      }
   }

   protected void onEffectsRemoved(Collection<MobEffectInstance> p_364717_) {
      if (!this.level().isClientSide()) {
         this.effectsDirty = true;

         for (MobEffectInstance mobeffectinstance : p_364717_) {
            mobeffectinstance.getEffect().value().removeAttributeModifiers(this.getAttributes());

            for (Entity entity : this.getPassengers()) {
               if (entity instanceof ServerPlayer serverplayer) {
                  serverplayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), mobeffectinstance.getEffect()));
               }
            }
         }

         this.refreshDirtyAttributes();
      }
   }

   private void refreshDirtyAttributes() {
      Set<AttributeInstance> set = this.getAttributes().getAttributesToUpdate();

      for (AttributeInstance attributeinstance : set) {
         this.onAttributeUpdated(attributeinstance.getAttribute());
      }

      set.clear();
   }

   protected void onAttributeUpdated(Holder<Attribute> p_328945_) {
      if (p_328945_.is(Attributes.MAX_HEALTH)) {
         float f = this.getMaxHealth();
         if (this.getHealth() > f) {
            this.setHealth(f);
         }
      } else if (p_328945_.is(Attributes.MAX_ABSORPTION)) {
         float f1 = this.getMaxAbsorption();
         if (this.getAbsorptionAmount() > f1) {
            this.setAbsorptionAmount(f1);
         }
      } else if (p_328945_.is(Attributes.SCALE)) {
         this.refreshDimensions();
      } else if (p_328945_.is(Attributes.WAYPOINT_TRANSMIT_RANGE) && this.level() instanceof ServerLevel serverlevel) {
         ServerWaypointManager serverwaypointmanager = serverlevel.getWaypointManager();
         if (this.attributes.getValue(p_328945_) > 0.0) {
            serverwaypointmanager.trackWaypoint(this);
         } else {
            serverwaypointmanager.untrackWaypoint(this);
         }
      }
   }

   public void heal(float p_21116_) {
      float f = this.getHealth();
      if (f > 0.0F) {
         this.setHealth(f + p_21116_);
      }
   }

   public float getHealth() {
      return this.entityData.get(DATA_HEALTH_ID);
   }

   public void setHealth(float p_21154_) {
      this.entityData.set(DATA_HEALTH_ID, Mth.clamp(p_21154_, 0.0F, this.getMaxHealth()));
   }

   public boolean isDeadOrDying() {
      return this.getHealth() <= 0.0F;
   }

   @Override
   public boolean hurtServer(ServerLevel p_361743_, DamageSource p_361865_, float p_365677_) {
      if (this.isInvulnerableTo(p_361743_, p_361865_)) {
         return false;
      }

      if (this.isDeadOrDying()) {
         return false;
      }

      if (p_361865_.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
         return false;
      }

      if (this.isSleeping()) {
         this.stopSleeping();
      }

      this.noActionTime = 0;
      if (p_365677_ < 0.0F) {
         p_365677_ = 0.0F;
      }

      ItemStack itemstack = this.getUseItem();
      float f = this.applyItemBlocking(p_361743_, p_361865_, p_365677_);
      p_365677_ -= f;
      boolean flag = f > 0.0F;
      if (p_361865_.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
         p_365677_ *= 5.0F;
      }

      if (p_361865_.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
         this.hurtHelmet(p_361865_, p_365677_);
         p_365677_ *= 0.75F;
      }

      if (Float.isNaN(p_365677_) || Float.isInfinite(p_365677_)) {
         p_365677_ = Float.MAX_VALUE;
      }

      boolean flag1 = true;
      if (this.invulnerableTime > 10.0F && !p_361865_.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
         if (p_365677_ <= this.lastHurt) {
            return false;
         }

         this.actuallyHurt(p_361743_, p_361865_, p_365677_ - this.lastHurt);
         this.lastHurt = p_365677_;
         flag1 = false;
      } else {
         this.lastHurt = p_365677_;
         this.invulnerableTime = 20;
         this.actuallyHurt(p_361743_, p_361865_, p_365677_);
         this.hurtDuration = 10;
         this.hurtTime = this.hurtDuration;
      }

      this.resolveMobResponsibleForDamage(p_361865_);
      this.resolvePlayerResponsibleForDamage(p_361865_);
      if (flag1) {
         BlocksAttacks blocksattacks = itemstack.get(DataComponents.BLOCKS_ATTACKS);
         if (flag && blocksattacks != null) {
            blocksattacks.onBlocked(p_361743_, this);
         } else {
            p_361743_.broadcastDamageEvent(this, p_361865_);
         }

         if (!p_361865_.is(DamageTypeTags.NO_IMPACT) && (!flag || p_365677_ > 0.0F)) {
            this.markHurt();
         }

         if (!p_361865_.is(DamageTypeTags.NO_KNOCKBACK)) {
            double d0 = 0.0;
            double d1 = 0.0;
            if (p_361865_.getDirectEntity() instanceof Projectile projectile) {
               DoubleDoubleImmutablePair doubledoubleimmutablepair = projectile.calculateHorizontalHurtKnockbackDirection(this, p_361865_);
               d0 = -doubledoubleimmutablepair.leftDouble();
               d1 = -doubledoubleimmutablepair.rightDouble();
            } else if (p_361865_.getSourcePosition() != null) {
               d0 = p_361865_.getSourcePosition().x() - this.getX();
               d1 = p_361865_.getSourcePosition().z() - this.getZ();
            }

            this.knockback(0.4F, d0, d1);
            if (!flag) {
               this.indicateDamage(d0, d1);
            }
         }
      }

      if (this.isDeadOrDying()) {
         if (!this.checkTotemDeathProtection(p_361865_)) {
            if (flag1) {
               this.makeSound(this.getDeathSound());
               this.playSecondaryHurtSound(p_361865_);
            }

            this.die(p_361865_);
         }
      } else if (flag1) {
         this.playHurtSound(p_361865_);
         this.playSecondaryHurtSound(p_361865_);
      }

      boolean flag2 = !flag || p_365677_ > 0.0F;
      if (flag2) {
         this.lastDamageSource = p_361865_;
         this.lastDamageStamp = this.level().getGameTime();

         for (MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
            mobeffectinstance.onMobHurt(p_361743_, this, p_361865_, p_365677_);
         }
      }

      if (this instanceof ServerPlayer serverplayer) {
         CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(serverplayer, p_361865_, p_365677_, p_365677_, flag);
         if (f > 0.0F && f < 3.4028235E37F) {
            serverplayer.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f * 10.0F));
         }
      }

      if (p_361865_.getEntity() instanceof ServerPlayer serverplayer1) {
         CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverplayer1, this, p_361865_, p_365677_, p_365677_, flag);
      }

      return flag2;
   }

   public float applyItemBlocking(ServerLevel p_396198_, DamageSource p_392511_, float p_394545_) {
      if (p_394545_ <= 0.0F) {
         return 0.0F;
      }

      ItemStack itemstack = this.getItemBlockingWith();
      if (itemstack == null) {
         return 0.0F;
      }

      BlocksAttacks blocksattacks = itemstack.get(DataComponents.BLOCKS_ATTACKS);
      if (blocksattacks != null && !blocksattacks.bypassedBy().map(p_392511_::is).orElse(false)) {
         if (p_392511_.getDirectEntity() instanceof AbstractArrow abstractarrow && abstractarrow.getPierceLevel() > 0) {
            return 0.0F;
         } else {
            Vec3 vec3 = p_392511_.getSourcePosition();
            double d0;
            if (vec3 != null) {
               Vec3 vec31 = this.calculateViewVector(0.0F, this.getYHeadRot());
               Vec3 vec32 = vec3.subtract(this.position());
               vec32 = new Vec3(vec32.x, 0.0, vec32.z).normalize();
               d0 = Math.acos(vec32.dot(vec31));
            } else {
               d0 = (float) Math.PI;
            }

            float f = blocksattacks.resolveBlockedDamage(p_392511_, p_394545_, d0);
            blocksattacks.hurtBlockingItem(this.level(), itemstack, this, this.getUsedItemHand(), f);
            if (f > 0.0F && !p_392511_.is(DamageTypeTags.IS_PROJECTILE) && p_392511_.getDirectEntity() instanceof LivingEntity livingentity) {
               this.blockUsingItem(p_396198_, livingentity);
            }

            return f;
         }
      } else {
         return 0.0F;
      }
   }

   private void playSecondaryHurtSound(DamageSource p_394905_) {
      if (p_394905_.is(DamageTypes.THORNS)) {
         SoundSource soundsource = this instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
         this.level().playSound(null, this.position().x, this.position().y, this.position().z, SoundEvents.THORNS_HIT, soundsource);
      }
   }

   protected void resolveMobResponsibleForDamage(DamageSource p_377003_) {
      if (p_377003_.getEntity() instanceof LivingEntity livingentity
         && !p_377003_.is(DamageTypeTags.NO_ANGER)
         && (!p_377003_.is(DamageTypes.WIND_CHARGE) || !this.getType().is(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))) {
         this.setLastHurtByMob(livingentity);
      }
   }

   protected @Nullable Player resolvePlayerResponsibleForDamage(DamageSource p_378258_) {
      Entity entity = p_378258_.getEntity();
      if (entity instanceof Player player) {
         this.setLastHurtByPlayer(player, 100);
      } else if (entity instanceof Wolf wolf && wolf.isTame()) {
         if (wolf.getOwnerReference() != null) {
            this.setLastHurtByPlayer(wolf.getOwnerReference().getUUID(), 100);
         } else {
            this.lastHurtByPlayer = null;
            this.lastHurtByPlayerMemoryTime = 0;
         }
      }

      return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
   }

   protected void blockUsingItem(ServerLevel p_395319_, LivingEntity p_392570_) {
      p_392570_.blockedByItem(this);
   }

   protected void blockedByItem(LivingEntity p_21246_) {
      p_21246_.knockback(0.5, p_21246_.getX() - this.getX(), p_21246_.getZ() - this.getZ());
   }

   private boolean checkTotemDeathProtection(DamageSource p_21263_) {
      if (p_21263_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }

      ItemStack itemstack = null;
      DeathProtection deathprotection = null;

      for (InteractionHand interactionhand : InteractionHand.values()) {
         ItemStack itemstack1 = this.getItemInHand(interactionhand);
         deathprotection = itemstack1.get(DataComponents.DEATH_PROTECTION);
         if (deathprotection != null) {
            itemstack = itemstack1.copy();
            itemstack1.shrink(1);
            break;
         }
      }

      if (itemstack != null) {
         if (this instanceof ServerPlayer serverplayer) {
            serverplayer.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
            CriteriaTriggers.USED_TOTEM.trigger(serverplayer, itemstack);
            itemstack.causeUseVibration(this, GameEvent.ITEM_INTERACT_FINISH);
         }

         this.setHealth(1.0F);
         deathprotection.applyEffects(itemstack, this);
         this.level().broadcastEntityEvent(this, (byte)35);
      }

      return deathprotection != null;
   }

   public @Nullable DamageSource getLastDamageSource() {
      if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
         this.lastDamageSource = null;
      }

      return this.lastDamageSource;
   }

   protected void playHurtSound(DamageSource p_21160_) {
      this.makeSound(this.getHurtSound(p_21160_));
   }

   public void makeSound(@Nullable SoundEvent p_334191_) {
      if (p_334191_ != null) {
         this.playSound(p_334191_, this.getSoundVolume(), this.getVoicePitch());
      }
   }

   private void breakItem(ItemStack p_21279_) {
      if (!p_21279_.isEmpty()) {
         Holder<SoundEvent> holder = p_21279_.get(DataComponents.BREAK_SOUND);
         if (holder != null && !this.isSilent()) {
            this.level()
               .playLocalSound(
                  this.getX(), this.getY(), this.getZ(), holder.value(), this.getSoundSource(), 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F, false
               );
         }

         this.spawnItemParticles(p_21279_, 5);
      }
   }

   public void die(DamageSource p_21014_) {
      if (!this.isRemoved() && !this.dead) {
         Entity entity = p_21014_.getEntity();
         LivingEntity livingentity = this.getKillCredit();
         if (livingentity != null) {
            livingentity.awardKillScore(this, p_21014_);
         }

         if (this.isSleeping()) {
            this.stopSleeping();
         }

         this.stopUsingItem();
         if (!this.level().isClientSide() && this.hasCustomName()) {
            LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
         }

         this.dead = true;
         this.getCombatTracker().recheckStatus();
         if (this.level() instanceof ServerLevel serverlevel) {
            if (entity == null || entity.killedEntity(serverlevel, this, p_21014_)) {
               this.gameEvent(GameEvent.ENTITY_DIE);
               this.dropAllDeathLoot(serverlevel, p_21014_);
               this.createWitherRose(livingentity);
            }

            this.level().broadcastEntityEvent(this, (byte)3);
         }

         this.setPose(Pose.DYING);
      }
   }

   protected void createWitherRose(@Nullable LivingEntity p_21269_) {
      if (this.level() instanceof ServerLevel serverlevel) {
         boolean flag = false;
         if (p_21269_ instanceof WitherBoss) {
            if (serverlevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
               BlockPos blockpos = this.blockPosition();
               BlockState blockstate = Blocks.WITHER_ROSE.defaultBlockState();
               if (this.level().getBlockState(blockpos).isAir() && blockstate.canSurvive(this.level(), blockpos)) {
                  this.level().setBlock(blockpos, blockstate, 3);
                  flag = true;
               }
            }

            if (!flag) {
               ItemEntity itementity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
               this.level().addFreshEntity(itementity);
            }
         }
      }
   }

   protected void dropAllDeathLoot(ServerLevel p_342160_, DamageSource p_21192_) {
      boolean flag = this.lastHurtByPlayerMemoryTime > 0;
      if (this.shouldDropLoot(p_342160_)) {
         this.dropFromLootTable(p_342160_, p_21192_, flag);
         this.dropCustomDeathLoot(p_342160_, p_21192_, flag);
      }

      this.dropEquipment(p_342160_);
      this.dropExperience(p_342160_, p_21192_.getEntity());
   }

   protected void dropEquipment(ServerLevel p_365823_) {
   }

   protected void dropExperience(ServerLevel p_370040_, @Nullable Entity p_342525_) {
      if (!this.wasExperienceConsumed()
         && (
            this.isAlwaysExperienceDropper()
               || this.lastHurtByPlayerMemoryTime > 0 && this.shouldDropExperience() && p_370040_.getGameRules().get(GameRules.MOB_DROPS)
         )) {
         ExperienceOrb.award(p_370040_, this.position(), this.getExperienceReward(p_370040_, p_342525_));
      }
   }

   protected void dropCustomDeathLoot(ServerLevel p_344205_, DamageSource p_21018_, boolean p_21020_) {
   }

   public long getLootTableSeed() {
      return 0L;
   }

   protected float getKnockback(Entity p_344037_, DamageSource p_343881_) {
      float f = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      return this.level() instanceof ServerLevel serverlevel
         ? EnchantmentHelper.modifyKnockback(serverlevel, this.getWeaponItem(), p_344037_, p_343881_, f) / 2.0F
         : f / 2.0F;
   }

   protected void dropFromLootTable(ServerLevel p_365510_, DamageSource p_364726_, boolean p_367117_) {
      Optional<ResourceKey<LootTable>> optional = this.getLootTable();
      if (!optional.isEmpty()) {
         this.dropFromLootTable(p_365510_, p_364726_, p_367117_, optional.get());
      }
   }

   public void dropFromLootTable(ServerLevel p_423504_, DamageSource p_431427_, boolean p_427346_, ResourceKey<LootTable> p_426450_) {
      this.dropFromLootTable(p_423504_, p_431427_, p_427346_, p_426450_, p_358880_ -> this.spawnAtLocation(p_423504_, p_358880_));
   }

   public void dropFromLootTable(
      ServerLevel p_425407_, DamageSource p_427053_, boolean p_428226_, ResourceKey<LootTable> p_422362_, Consumer<ItemStack> p_429171_
   ) {
      LootTable loottable = p_425407_.getServer().reloadableRegistries().getLootTable(p_422362_);
      LootParams.Builder lootparams$builder = new LootParams.Builder(p_425407_)
         .withParameter(LootContextParams.THIS_ENTITY, this)
         .withParameter(LootContextParams.ORIGIN, this.position())
         .withParameter(LootContextParams.DAMAGE_SOURCE, p_427053_)
         .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, p_427053_.getEntity())
         .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, p_427053_.getDirectEntity());
      Player player = this.getLastHurtByPlayer();
      if (p_428226_ && player != null) {
         lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
      }

      LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
      loottable.getRandomItems(lootparams, this.getLootTableSeed(), p_429171_);
   }

   public boolean dropFromEntityInteractLootTable(
      ServerLevel p_426199_, ResourceKey<LootTable> p_429278_, @Nullable Entity p_428787_, ItemStack p_422740_, BiConsumer<ServerLevel, ItemStack> p_426513_
   ) {
      return this.dropFromLootTable(
         p_426199_,
         p_429278_,
         p_421594_ -> p_421594_.withParameter(LootContextParams.TARGET_ENTITY, this)
            .withOptionalParameter(LootContextParams.INTERACTING_ENTITY, p_428787_)
            .withParameter(LootContextParams.TOOL, p_422740_)
            .create(LootContextParamSets.ENTITY_INTERACT),
         p_426513_
      );
   }

   public boolean dropFromGiftLootTable(ServerLevel p_363120_, ResourceKey<LootTable> p_363326_, BiConsumer<ServerLevel, ItemStack> p_368812_) {
      return this.dropFromLootTable(
         p_363120_,
         p_363326_,
         p_449423_ -> p_449423_.withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .create(LootContextParamSets.GIFT),
         p_368812_
      );
   }

   protected void dropFromShearingLootTable(
      ServerLevel p_361428_, ResourceKey<LootTable> p_365429_, ItemStack p_361064_, BiConsumer<ServerLevel, ItemStack> p_369621_
   ) {
      this.dropFromLootTable(
         p_361428_,
         p_365429_,
         p_449421_ -> p_449421_.withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .withParameter(LootContextParams.TOOL, p_361064_)
            .create(LootContextParamSets.SHEARING),
         p_369621_
      );
   }

   protected boolean dropFromLootTable(
      ServerLevel p_363272_, ResourceKey<LootTable> p_363593_, Function<LootParams.Builder, LootParams> p_362309_, BiConsumer<ServerLevel, ItemStack> p_366393_
   ) {
      LootTable loottable = p_363272_.getServer().reloadableRegistries().getLootTable(p_363593_);
      LootParams lootparams = p_362309_.apply(new LootParams.Builder(p_363272_));
      List<ItemStack> list = loottable.getRandomItems(lootparams);
      if (!list.isEmpty()) {
         list.forEach(p_358893_ -> p_366393_.accept(p_363272_, p_358893_));
         return true;
      } else {
         return false;
      }
   }

   public void knockback(double p_147241_, double p_147242_, double p_147243_) {
      p_147241_ *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      if (!(p_147241_ <= 0.0)) {
         this.needsSync = true;
         Vec3 vec3 = this.getDeltaMovement();

         while (p_147242_ * p_147242_ + p_147243_ * p_147243_ < 1.0E-5F) {
            p_147242_ = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
            p_147243_ = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
         }

         Vec3 vec31 = new Vec3(p_147242_, 0.0, p_147243_).normalize().scale(p_147241_);
         this.setDeltaMovement(vec3.x / 2.0 - vec31.x, this.onGround() ? Math.min(0.4, vec3.y / 2.0 + p_147241_) : vec3.y, vec3.z / 2.0 - vec31.z);
      }
   }

   public void indicateDamage(double p_270514_, double p_270826_) {
   }

   protected @Nullable SoundEvent getHurtSound(DamageSource p_21239_) {
      return SoundEvents.GENERIC_HURT;
   }

   protected @Nullable SoundEvent getDeathSound() {
      return SoundEvents.GENERIC_DEATH;
   }

   private SoundEvent getFallDamageSound(int p_21313_) {
      return p_21313_ > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
   }

   public void skipDropExperience() {
      this.skipDropExperience = true;
   }

   public boolean wasExperienceConsumed() {
      return this.skipDropExperience;
   }

   public float getHurtDir() {
      return 0.0F;
   }

   protected AABB getHitbox() {
      AABB aabb = this.getBoundingBox();
      Entity entity = this.getVehicle();
      if (entity != null) {
         Vec3 vec3 = entity.getPassengerRidingPosition(this);
         return aabb.setMinY(Math.max(vec3.y, aabb.minY));
      } else {
         return aabb;
      }
   }

   public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments(EquipmentSlot p_363512_) {
      return (Map<Enchantment, Set<EnchantmentLocationBasedEffect>>)this.activeLocationDependentEnchantments
         .computeIfAbsent(p_363512_, p_358895_ -> new Reference2ObjectArrayMap());
   }

   public void lungeForwardMaybe() {
      if (this.level() instanceof ServerLevel serverlevel) {
         EnchantmentHelper.doLungeEffects(serverlevel, this);
      }
   }

   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
   }

   public Optional<BlockPos> getLastClimbablePos() {
      return this.lastClimbablePos;
   }

   public boolean onClimbable() {
      if (this.isSpectator()) {
         return false;
      } else {
         BlockPos blockpos = this.blockPosition();
         BlockState blockstate = this.getInBlockState();
         if (this.isFallFlying() && blockstate.is(BlockTags.CAN_GLIDE_THROUGH)) {
            return false;
         } else if (blockstate.is(BlockTags.CLIMBABLE)) {
            this.lastClimbablePos = Optional.of(blockpos);
            return true;
         } else if (blockstate.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(blockpos, blockstate)) {
            this.lastClimbablePos = Optional.of(blockpos);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean trapdoorUsableAsLadder(BlockPos p_21177_, BlockState p_21178_) {
      if (!p_21178_.getValue(TrapDoorBlock.OPEN)) {
         return false;
      }

      BlockState blockstate = this.level().getBlockState(p_21177_.below());
      return blockstate.is(Blocks.LADDER) && blockstate.getValue(LadderBlock.FACING) == p_21178_.getValue(TrapDoorBlock.FACING);
   }

   @Override
   public boolean isAlive() {
      return !this.isRemoved() && this.getHealth() > 0.0F;
   }

   public boolean isLookingAtMe(LivingEntity p_365362_, double p_366130_, boolean p_364554_, boolean p_368791_, double... p_377772_) {
      Vec3 vec3 = p_365362_.getViewVector(1.0F).normalize();

      for (double d0 : p_377772_) {
         Vec3 vec31 = new Vec3(this.getX() - p_365362_.getX(), d0 - p_365362_.getEyeY(), this.getZ() - p_365362_.getZ());
         double d1 = vec31.length();
         vec31 = vec31.normalize();
         double d2 = vec3.dot(vec31);
         if (d2 > 1.0 - p_366130_ / (p_364554_ ? d1 : 1.0)
            && p_365362_.hasLineOfSight(this, p_368791_ ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, d0)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int getMaxFallDistance() {
      return this.getComfortableFallDistance(0.0F);
   }

   protected final int getComfortableFallDistance(float p_327795_) {
      return Mth.floor(p_327795_ + 3.0F);
   }

   @Override
   public boolean causeFallDamage(double p_393354_, float p_147187_, DamageSource p_147189_) {
      boolean flag = super.causeFallDamage(p_393354_, p_147187_, p_147189_);
      int i = this.calculateFallDamage(p_393354_, p_147187_);
      if (i > 0) {
         this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
         this.playBlockFallSound();
         this.hurt(p_147189_, i);
         return true;
      } else {
         return flag;
      }
   }

   protected int calculateFallDamage(double p_393496_, float p_21237_) {
      if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
         return 0;
      }

      double d0 = this.calculateFallPower(p_393496_);
      return Mth.floor(d0 * p_21237_ * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
   }

   private double calculateFallPower(double p_393609_) {
      return p_393609_ + 1.0E-6 - this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
   }

   protected void playBlockFallSound() {
      if (!this.isSilent()) {
         int i = Mth.floor(this.getX());
         int j = Mth.floor(this.getY() - 0.2F);
         int k = Mth.floor(this.getZ());
         BlockState blockstate = this.level().getBlockState(new BlockPos(i, j, k));
         if (!blockstate.isAir()) {
            SoundType soundtype = blockstate.getSoundType();
            this.playSound(soundtype.getFallSound(), soundtype.getVolume() * 0.5F, soundtype.getPitch() * 0.75F);
         }
      }
   }

   @Override
   public void animateHurt(float p_265265_) {
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
   }

   public int getArmorValue() {
      return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
   }

   protected void hurtArmor(DamageSource p_21122_, float p_21123_) {
   }

   protected void hurtHelmet(DamageSource p_147213_, float p_147214_) {
   }

   protected void doHurtEquipment(DamageSource p_330939_, float p_333962_, EquipmentSlot... p_335230_) {
      if (!(p_333962_ <= 0.0F)) {
         int i = (int)Math.max(1.0F, p_333962_ / 4.0F);

         for (EquipmentSlot equipmentslot : p_335230_) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.damageOnHurt() && itemstack.isDamageableItem() && itemstack.canBeHurtBy(p_330939_)) {
               itemstack.hurtAndBreak(i, this, equipmentslot);
            }
         }
      }
   }

   protected float getDamageAfterArmorAbsorb(DamageSource p_21162_, float p_21163_) {
      if (!p_21162_.is(DamageTypeTags.BYPASSES_ARMOR)) {
         this.hurtArmor(p_21162_, p_21163_);
         p_21163_ = CombatRules.getDamageAfterAbsorb(this, p_21163_, p_21162_, this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
      }

      return p_21163_;
   }

   protected float getDamageAfterMagicAbsorb(DamageSource p_21193_, float p_21194_) {
      if (p_21193_.is(DamageTypeTags.BYPASSES_EFFECTS)) {
         return p_21194_;
      }

      if (this.hasEffect(MobEffects.RESISTANCE) && !p_21193_.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
         int i = (this.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
         int j = 25 - i;
         float f = p_21194_ * j;
         float f1 = p_21194_;
         p_21194_ = Math.max(f / 25.0F, 0.0F);
         float f2 = f1 - p_21194_;
         if (f2 > 0.0F && f2 < 3.4028235E37F) {
            if (this instanceof ServerPlayer) {
               ((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(f2 * 10.0F));
            } else if (p_21193_.getEntity() instanceof ServerPlayer) {
               ((ServerPlayer)p_21193_.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(f2 * 10.0F));
            }
         }
      }

      if (p_21194_ <= 0.0F) {
         return 0.0F;
      }

      if (p_21193_.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
         return p_21194_;
      }

      float f3;
      if (this.level() instanceof ServerLevel serverlevel) {
         f3 = EnchantmentHelper.getDamageProtection(serverlevel, this, p_21193_);
      } else {
         f3 = 0.0F;
      }

      if (f3 > 0.0F) {
         p_21194_ = CombatRules.getDamageAfterMagicAbsorb(p_21194_, f3);
      }

      return p_21194_;
   }

   protected void actuallyHurt(ServerLevel p_365124_, DamageSource p_21240_, float p_21241_) {
      if (!this.isInvulnerableTo(p_365124_, p_21240_)) {
         p_21241_ = this.getDamageAfterArmorAbsorb(p_21240_, p_21241_);
         p_21241_ = this.getDamageAfterMagicAbsorb(p_21240_, p_21241_);
         float f1 = Math.max(p_21241_ - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (p_21241_ - f1));
         float f = p_21241_ - f1;
         if (f > 0.0F && f < 3.4028235E37F && p_21240_.getEntity() instanceof ServerPlayer serverplayer) {
            serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0F));
         }

         if (f1 != 0.0F) {
            this.getCombatTracker().recordDamage(p_21240_, f1);
            this.setHealth(this.getHealth() - f1);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - f1);
            this.gameEvent(GameEvent.ENTITY_DAMAGE);
         }
      }
   }

   public CombatTracker getCombatTracker() {
      return this.combatTracker;
   }

   public @Nullable LivingEntity getKillCredit() {
      if (this.lastHurtByPlayer != null) {
         return this.lastHurtByPlayer.getEntity(this.level(), Player.class);
      } else {
         return this.lastHurtByMob != null ? this.lastHurtByMob.getEntity(this.level(), LivingEntity.class) : null;
      }
   }

   public final float getMaxHealth() {
      return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
   }

   public final float getMaxAbsorption() {
      return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
   }

   public final int getArrowCount() {
      return this.entityData.get(DATA_ARROW_COUNT_ID);
   }

   public final void setArrowCount(int p_21318_) {
      this.entityData.set(DATA_ARROW_COUNT_ID, p_21318_);
   }

   public final int getStingerCount() {
      return this.entityData.get(DATA_STINGER_COUNT_ID);
   }

   public final void setStingerCount(int p_21322_) {
      this.entityData.set(DATA_STINGER_COUNT_ID, p_21322_);
   }

   private int getCurrentSwingDuration() {
      ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);
      int i = itemstack.getSwingAnimation().duration();
      if (MobEffectUtil.hasDigSpeed(this)) {
         return i - (1 + MobEffectUtil.getDigSpeedAmplification(this));
      } else {
         return this.hasEffect(MobEffects.MINING_FATIGUE) ? i + (1 + this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2 : i;
      }
   }

   public void swing(InteractionHand p_21007_) {
      this.swing(p_21007_, false);
   }

   public void swing(InteractionHand p_21012_, boolean p_21013_) {
      if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
         this.swingTime = -1;
         this.swinging = true;
         this.swingingArm = p_21012_;
         if (this.level() instanceof ServerLevel) {
            ClientboundAnimatePacket clientboundanimatepacket = new ClientboundAnimatePacket(this, p_21012_ == InteractionHand.MAIN_HAND ? 0 : 3);
            ServerChunkCache serverchunkcache = ((ServerLevel)this.level()).getChunkSource();
            if (p_21013_) {
               serverchunkcache.sendToTrackingPlayersAndSelf(this, clientboundanimatepacket);
            } else {
               serverchunkcache.sendToTrackingPlayers(this, clientboundanimatepacket);
            }
         }
      }
   }

   @Override
   public void handleDamageEvent(DamageSource p_270229_) {
      this.walkAnimation.setSpeed(1.5F);
      this.invulnerableTime = 20;
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
      SoundEvent soundevent = this.getHurtSound(p_270229_);
      if (soundevent != null) {
         this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
      }

      this.lastDamageSource = p_270229_;
      this.lastDamageStamp = this.level().getGameTime();
   }

   @Override
   public void handleEntityEvent(byte p_20975_) {
      switch (p_20975_) {
         case 2:
            this.onKineticHit();
            break;
         case 3:
            SoundEvent soundevent = this.getDeathSound();
            if (soundevent != null) {
               this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            if (!(this instanceof Player)) {
               this.setHealth(0.0F);
               this.die(this.damageSources().generic());
            }
            break;
         case 46:
            int i = 128;

            for (int j = 0; j < 128; j++) {
               double d0 = j / 127.0;
               float f = (this.random.nextFloat() - 0.5F) * 0.2F;
               float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
               float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
               double d1 = Mth.lerp(d0, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
               double d2 = Mth.lerp(d0, this.yo, this.getY()) + this.random.nextDouble() * this.getBbHeight();
               double d3 = Mth.lerp(d0, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
               this.level().addParticle(ParticleTypes.PORTAL, d1, d2, d3, f, f1, f2);
            }
            break;
         case 47:
            this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
            break;
         case 48:
            this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
            break;
         case 49:
            this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
            break;
         case 50:
            this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
            break;
         case 51:
            this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
            break;
         case 52:
            this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
            break;
         case 54:
            HoneyBlock.showJumpParticles(this);
            break;
         case 55:
            this.swapHandItems();
            break;
         case 60:
            this.makePoofParticles();
            break;
         case 65:
            this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
            break;
         case 67:
            this.makeDrownParticles();
            break;
         case 68:
            this.breakItem(this.getItemBySlot(EquipmentSlot.SADDLE));
            break;
         default:
            super.handleEntityEvent(p_20975_);
      }
   }

   public float getTicksSinceLastKineticHitFeedback(float p_451822_) {
      return this.lastKineticHitFeedbackTime < 0L ? 0.0F : (float)(this.level().getGameTime() - this.lastKineticHitFeedbackTime) + p_451822_;
   }

   public void makePoofParticles() {
      for (int i = 0; i < 20; i++) {
         double d0 = this.random.nextGaussian() * 0.02;
         double d1 = this.random.nextGaussian() * 0.02;
         double d2 = this.random.nextGaussian() * 0.02;
         double d3 = 10.0;
         this.level()
            .addParticle(ParticleTypes.POOF, this.getRandomX(1.0) - d0 * 10.0, this.getRandomY() - d1 * 10.0, this.getRandomZ(1.0) - d2 * 10.0, d0, d1, d2);
      }
   }

   private void makeDrownParticles() {
      Vec3 vec3 = this.getDeltaMovement();

      for (int i = 0; i < 8; i++) {
         double d0 = this.random.triangle(0.0, 1.0);
         double d1 = this.random.triangle(0.0, 1.0);
         double d2 = this.random.triangle(0.0, 1.0);
         this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + d0, this.getY() + d1, this.getZ() + d2, vec3.x, vec3.y, vec3.z);
      }
   }

   private void onKineticHit() {
      if (this.level().getGameTime() - this.lastKineticHitFeedbackTime > 10L) {
         this.lastKineticHitFeedbackTime = this.level().getGameTime();
         KineticWeapon kineticweapon = this.useItem.get(DataComponents.KINETIC_WEAPON);
         if (kineticweapon != null) {
            kineticweapon.makeLocalHitSound(this);
         }
      }
   }

   private void swapHandItems() {
      ItemStack itemstack = this.getItemBySlot(EquipmentSlot.OFFHAND);
      this.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
      this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
   }

   @Override
   protected void onBelowWorld() {
      this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
   }

   protected void updateSwingTime() {
      int i = this.getCurrentSwingDuration();
      if (this.swinging) {
         this.swingTime++;
         if (this.swingTime >= i) {
            this.swingTime = 0;
            this.swinging = false;
         }
      } else {
         this.swingTime = 0;
      }

      this.attackAnim = (float)this.swingTime / i;
   }

   public @Nullable AttributeInstance getAttribute(Holder<Attribute> p_332356_) {
      return this.getAttributes().getInstance(p_332356_);
   }

   public double getAttributeValue(Holder<Attribute> p_251296_) {
      return this.getAttributes().getValue(p_251296_);
   }

   public double getAttributeBaseValue(Holder<Attribute> p_248605_) {
      return this.getAttributes().getBaseValue(p_248605_);
   }

   public AttributeMap getAttributes() {
      return this.attributes;
   }

   public ItemStack getMainHandItem() {
      return this.getItemBySlot(EquipmentSlot.MAINHAND);
   }

   public ItemStack getOffhandItem() {
      return this.getItemBySlot(EquipmentSlot.OFFHAND);
   }

   public ItemStack getItemHeldByArm(HumanoidArm p_365937_) {
      return this.getMainArm() == p_365937_ ? this.getMainHandItem() : this.getOffhandItem();
   }

   @Override
   public ItemStack getWeaponItem() {
      return this.getMainHandItem();
   }

   public AttackRange entityAttackRange() {
      AttackRange attackrange = this.getActiveItem().get(DataComponents.ATTACK_RANGE);
      return attackrange != null ? attackrange : AttackRange.defaultFor(this);
   }

   public ItemStack getActiveItem() {
      return this.isUsingItem() ? this.getUseItem() : this.getMainHandItem();
   }

   public boolean isHolding(Item p_21056_) {
      return this.isHolding(p_147200_ -> p_147200_.is(p_21056_));
   }

   public boolean isHolding(Predicate<ItemStack> p_21094_) {
      return p_21094_.test(this.getMainHandItem()) || p_21094_.test(this.getOffhandItem());
   }

   public ItemStack getItemInHand(InteractionHand p_21121_) {
      if (p_21121_ == InteractionHand.MAIN_HAND) {
         return this.getItemBySlot(EquipmentSlot.MAINHAND);
      } else if (p_21121_ == InteractionHand.OFF_HAND) {
         return this.getItemBySlot(EquipmentSlot.OFFHAND);
      } else {
         throw new IllegalArgumentException("Invalid hand " + p_21121_);
      }
   }

   public void setItemInHand(InteractionHand p_21009_, ItemStack p_21010_) {
      if (p_21009_ == InteractionHand.MAIN_HAND) {
         this.setItemSlot(EquipmentSlot.MAINHAND, p_21010_);
      } else {
         if (p_21009_ != InteractionHand.OFF_HAND) {
            throw new IllegalArgumentException("Invalid hand " + p_21009_);
         }

         this.setItemSlot(EquipmentSlot.OFFHAND, p_21010_);
      }
   }

   public boolean hasItemInSlot(EquipmentSlot p_21034_) {
      return !this.getItemBySlot(p_21034_).isEmpty();
   }

   public boolean canUseSlot(EquipmentSlot p_328587_) {
      return true;
   }

   public ItemStack getItemBySlot(EquipmentSlot p_21127_) {
      return this.equipment.get(p_21127_);
   }

   public void setItemSlot(EquipmentSlot p_21036_, ItemStack p_21037_) {
      this.onEquipItem(p_21036_, this.equipment.set(p_21036_, p_21037_), p_21037_);
   }

   public float getArmorCoverPercentage() {
      int i = 0;
      int j = 0;

      for (EquipmentSlot equipmentslot : EquipmentSlotGroup.ARMOR) {
         if (equipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            ItemStack itemstack = this.getItemBySlot(equipmentslot);
            if (!itemstack.isEmpty()) {
               j++;
            }

            i++;
         }
      }

      return i > 0 ? (float)j / i : 0.0F;
   }

   @Override
   public void setSprinting(boolean p_21284_) {
      super.setSprinting(p_21284_);
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      attributeinstance.removeModifier(SPEED_MODIFIER_SPRINTING.id());
      if (p_21284_) {
         attributeinstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
      }
   }

   protected float getSoundVolume() {
      return 1.0F;
   }

   public float getVoicePitch() {
      return this.isBaby()
         ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F
         : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
   }

   protected boolean isImmobile() {
      return this.isDeadOrDying();
   }

   @Override
   public void push(Entity p_21294_) {
      if (!this.isSleeping()) {
         super.push(p_21294_);
      }
   }

   private void dismountVehicle(Entity p_21029_) {
      Vec3 vec3;
      if (this.isRemoved()) {
         vec3 = this.position();
      } else if (!p_21029_.isRemoved() && !this.level().getBlockState(p_21029_.blockPosition()).is(BlockTags.PORTALS)) {
         vec3 = p_21029_.getDismountLocationForPassenger(this);
      } else {
         double d0 = Math.max(this.getY(), p_21029_.getY());
         vec3 = new Vec3(this.getX(), d0, this.getZ());
         boolean flag = this.getBbWidth() <= 4.0F && this.getBbHeight() <= 4.0F;
         if (flag) {
            double d1 = this.getBbHeight() / 2.0;
            Vec3 vec31 = vec3.add(0.0, d1, 0.0);
            VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec31, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()));
            vec3 = this.level()
               .findFreePosition(this, voxelshape, vec31, this.getBbWidth(), this.getBbHeight(), this.getBbWidth())
               .map(p_358887_ -> p_358887_.add(0.0, -d1, 0.0))
               .orElse(vec3);
         }
      }

      this.dismountTo(vec3.x, vec3.y, vec3.z);
   }

   @Override
   public boolean shouldShowName() {
      return this.isCustomNameVisible();
   }

   protected float getJumpPower() {
      return this.getJumpPower(1.0F);
   }

   protected float getJumpPower(float p_329899_) {
      return (float)this.getAttributeValue(Attributes.JUMP_STRENGTH) * p_329899_ * this.getBlockJumpFactor() + this.getJumpBoostPower();
   }

   public float getJumpBoostPower() {
      return this.hasEffect(MobEffects.JUMP_BOOST) ? 0.1F * (this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1.0F) : 0.0F;
   }

   @VisibleForTesting
   public void jumpFromGround() {
      float f = this.getJumpPower();
      if (!(f <= 1.0E-5F)) {
         Vec3 vec3 = this.getDeltaMovement();
         this.setDeltaMovement(vec3.x, Math.max(f, vec3.y), vec3.z);
         if (this.isSprinting()) {
            float f1 = this.getYRot() * (float) (Math.PI / 180.0);
            this.addDeltaMovement(new Vec3(-Mth.sin(f1) * 0.2, 0.0, Mth.cos(f1) * 0.2));
         }

         this.needsSync = true;
      }
   }

   protected void goDownInWater() {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04F, 0.0));
   }

   protected void jumpInLiquid(TagKey<Fluid> p_204043_) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
   }

   protected float getWaterSlowDown() {
      return 0.8F;
   }

   public boolean canStandOnFluid(FluidState p_204042_) {
      return false;
   }

   @Override
   protected double getDefaultGravity() {
      return this.getAttributeValue(Attributes.GRAVITY);
   }

   protected double getEffectiveGravity() {
      boolean flag = this.getDeltaMovement().y <= 0.0;
      return flag && this.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(this.getGravity(), 0.01) : this.getGravity();
   }

   public void travel(Vec3 p_21280_) {
      if (this.shouldTravelInFluid(this.level().getFluidState(this.blockPosition()))) {
         this.travelInFluid(p_21280_);
      } else if (this.isFallFlying()) {
         this.travelFallFlying(p_21280_);
      } else {
         this.travelInAir(p_21280_);
      }
   }

   protected boolean shouldTravelInFluid(FluidState p_453099_) {
      return (this.isInWater() || this.isInLava()) && this.isAffectedByFluids() && !this.canStandOnFluid(p_453099_);
   }

   protected void travelFlying(Vec3 p_407126_, float p_407105_) {
      this.travelFlying(p_407126_, 0.02F, 0.02F, p_407105_);
   }

   protected void travelFlying(Vec3 p_409764_, float p_406455_, float p_409948_, float p_410640_) {
      if (this.isInWater()) {
         this.moveRelative(p_406455_, p_409764_);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
      } else if (this.isInLava()) {
         this.moveRelative(p_409948_, p_409764_);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      } else {
         this.moveRelative(p_410640_, p_409764_);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
      }
   }

   private void travelInAir(Vec3 p_362087_) {
      BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
      float f = this.onGround() ? this.level().getBlockState(blockpos).getBlock().getFriction() : 1.0F;
      float f1 = f * 0.91F;
      Vec3 vec3 = this.handleRelativeFrictionAndCalculateMovement(p_362087_, f);
      double d0 = vec3.y;
      MobEffectInstance mobeffectinstance = this.getEffect(MobEffects.LEVITATION);
      if (mobeffectinstance != null) {
         d0 += (0.05 * (mobeffectinstance.getAmplifier() + 1) - vec3.y) * 0.2;
      } else if (!this.level().isClientSide() || this.level().hasChunkAt(blockpos)) {
         d0 -= this.getEffectiveGravity();
      } else if (this.getY() > this.level().getMinY()) {
         d0 = -0.1;
      } else {
         d0 = 0.0;
      }

      if (this.shouldDiscardFriction()) {
         this.setDeltaMovement(vec3.x, d0, vec3.z);
      } else {
         float f2 = this instanceof FlyingAnimal ? f1 : 0.98F;
         this.setDeltaMovement(vec3.x * f1, d0 * f2, vec3.z * f1);
      }
   }

   private void travelInFluid(Vec3 p_362114_) {
      boolean flag = this.getDeltaMovement().y <= 0.0;
      double d0 = this.getY();
      double d1 = this.getEffectiveGravity();
      if (this.isInWater()) {
         this.travelInWater(p_362114_, d1, flag, d0);
         this.floatInWaterWhileRidden();
      } else {
         this.travelInLava(p_362114_, d1, flag, d0);
      }
   }

   protected void travelInWater(Vec3 p_451732_, double p_452538_, boolean p_455561_, double p_450436_) {
      float f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
      float f1 = 0.02F;
      float f2 = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
      if (!this.onGround()) {
         f2 *= 0.5F;
      }

      if (f2 > 0.0F) {
         f += (0.54600006F - f) * f2;
         f1 += (this.getSpeed() - f1) * f2;
      }

      if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
         f = 0.96F;
      }

      this.moveRelative(f1, p_451732_);
      this.move(MoverType.SELF, this.getDeltaMovement());
      Vec3 vec3 = this.getDeltaMovement();
      if (this.horizontalCollision && this.onClimbable()) {
         vec3 = new Vec3(vec3.x, 0.2, vec3.z);
      }

      vec3 = vec3.multiply(f, 0.8F, f);
      this.setDeltaMovement(this.getFluidFallingAdjustedMovement(p_452538_, p_455561_, vec3));
      this.jumpOutOfFluid(p_450436_);
   }

   private void travelInLava(Vec3 p_452494_, double p_452041_, boolean p_451529_, double p_457703_) {
      this.moveRelative(0.02F, p_452494_);
      this.move(MoverType.SELF, this.getDeltaMovement());
      if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
         Vec3 vec3 = this.getFluidFallingAdjustedMovement(p_452041_, p_451529_, this.getDeltaMovement());
         this.setDeltaMovement(vec3);
      } else {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      }

      if (p_452041_ != 0.0) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -p_452041_ / 4.0, 0.0));
      }

      this.jumpOutOfFluid(p_457703_);
   }

   private void jumpOutOfFluid(double p_453724_) {
      Vec3 vec3 = this.getDeltaMovement();
      if (this.horizontalCollision && this.isFree(vec3.x, vec3.y + 0.6F - this.getY() + p_453724_, vec3.z)) {
         this.setDeltaMovement(vec3.x, 0.3F, vec3.z);
      }
   }

   private void floatInWaterWhileRidden() {
      boolean flag = this.getType().is(EntityTypeTags.CAN_FLOAT_WHILE_RIDDEN);
      if (flag && this.isVehicle() && this.getFluidHeight(FluidTags.WATER) > this.getFluidJumpThreshold()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
      }
   }

   private void travelFallFlying(Vec3 p_391378_) {
      if (this.onClimbable()) {
         this.travelInAir(p_391378_);
         this.stopFallFlying();
      } else {
         Vec3 vec3 = this.getDeltaMovement();
         double d0 = vec3.horizontalDistance();
         this.setDeltaMovement(this.updateFallFlyingMovement(vec3));
         this.move(MoverType.SELF, this.getDeltaMovement());
         if (!this.level().isClientSide()) {
            double d1 = this.getDeltaMovement().horizontalDistance();
            this.handleFallFlyingCollisions(d0, d1);
         }
      }
   }

   public void stopFallFlying() {
      this.setSharedFlag(7, true);
      this.setSharedFlag(7, false);
   }

   private Vec3 updateFallFlyingMovement(Vec3 p_366729_) {
      Vec3 vec3 = this.getLookAngle();
      float f = this.getXRot() * (float) (Math.PI / 180.0);
      double d0 = Math.sqrt(vec3.x * vec3.x + vec3.z * vec3.z);
      double d1 = p_366729_.horizontalDistance();
      double d2 = this.getEffectiveGravity();
      double d3 = Mth.square(Math.cos(f));
      p_366729_ = p_366729_.add(0.0, d2 * (-1.0 + d3 * 0.75), 0.0);
      if (p_366729_.y < 0.0 && d0 > 0.0) {
         double d4 = p_366729_.y * -0.1 * d3;
         p_366729_ = p_366729_.add(vec3.x * d4 / d0, d4, vec3.z * d4 / d0);
      }

      if (f < 0.0F && d0 > 0.0) {
         double d5 = d1 * -Mth.sin(f) * 0.04;
         p_366729_ = p_366729_.add(-vec3.x * d5 / d0, d5 * 3.2, -vec3.z * d5 / d0);
      }

      if (d0 > 0.0) {
         p_366729_ = p_366729_.add((vec3.x / d0 * d1 - p_366729_.x) * 0.1, 0.0, (vec3.z / d0 * d1 - p_366729_.z) * 0.1);
      }

      return p_366729_.multiply(0.99F, 0.98F, 0.99F);
   }

   private void handleFallFlyingCollisions(double p_368432_, double p_367807_) {
      if (this.horizontalCollision) {
         double d0 = p_368432_ - p_367807_;
         float f = (float)(d0 * 10.0 - 3.0);
         if (f > 0.0F) {
            this.playSound(this.getFallDamageSound((int)f), 1.0F, 1.0F);
            this.hurt(this.damageSources().flyIntoWall(), f);
         }
      }
   }

   private void travelRidden(Player p_278244_, Vec3 p_278231_) {
      Vec3 vec3 = this.getRiddenInput(p_278244_, p_278231_);
      this.tickRidden(p_278244_, vec3);
      if (this.canSimulateMovement()) {
         this.setSpeed(this.getRiddenSpeed(p_278244_));
         this.travel(vec3);
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }
   }

   protected void tickRidden(Player p_278262_, Vec3 p_275242_) {
   }

   protected Vec3 getRiddenInput(Player p_278326_, Vec3 p_275300_) {
      return p_275300_;
   }

   protected float getRiddenSpeed(Player p_278286_) {
      return this.getSpeed();
   }

   public void calculateEntityAnimation(boolean p_268129_) {
      float f = (float)Mth.length(this.getX() - this.xo, p_268129_ ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
      if (!this.isPassenger() && this.isAlive()) {
         this.updateWalkAnimation(f);
      } else {
         this.walkAnimation.stop();
      }
   }

   protected void updateWalkAnimation(float p_268283_) {
      float f = Math.min(p_268283_ * 4.0F, 1.0F);
      this.walkAnimation.update(f, 0.4F, this.isBaby() ? 3.0F : 1.0F);
   }

   private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 p_21075_, float p_21076_) {
      this.moveRelative(this.getFrictionInfluencedSpeed(p_21076_), p_21075_);
      this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
      this.move(MoverType.SELF, this.getDeltaMovement());
      Vec3 vec3 = this.getDeltaMovement();
      if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
         vec3 = new Vec3(vec3.x, 0.2, vec3.z);
      }

      return vec3;
   }

   public Vec3 getFluidFallingAdjustedMovement(double p_20995_, boolean p_20996_, Vec3 p_20997_) {
      if (p_20995_ != 0.0 && !this.isSprinting()) {
         double d0;
         if (p_20996_ && Math.abs(p_20997_.y - 0.005) >= 0.003 && Math.abs(p_20997_.y - p_20995_ / 16.0) < 0.003) {
            d0 = -0.003;
         } else {
            d0 = p_20997_.y - p_20995_ / 16.0;
         }

         return new Vec3(p_20997_.x, d0, p_20997_.z);
      } else {
         return p_20997_;
      }
   }

   private Vec3 handleOnClimbable(Vec3 p_21298_) {
      if (this.onClimbable()) {
         this.resetFallDistance();
         float f = 0.15F;
         double d0 = Mth.clamp(p_21298_.x, -0.15F, 0.15F);
         double d1 = Mth.clamp(p_21298_.z, -0.15F, 0.15F);
         double d2 = Math.max(p_21298_.y, -0.15F);
         if (d2 < 0.0 && !this.getInBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
            d2 = 0.0;
         }

         p_21298_ = new Vec3(d0, d2, d1);
      }

      return p_21298_;
   }

   private float getFrictionInfluencedSpeed(float p_21331_) {
      return this.onGround() ? this.getSpeed() * (0.21600002F / (p_21331_ * p_21331_ * p_21331_)) : this.getFlyingSpeed();
   }

   protected float getFlyingSpeed() {
      return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float p_21320_) {
      this.speed = p_21320_;
   }

   public boolean doHurtTarget(ServerLevel p_366333_, Entity p_20970_) {
      this.setLastHurtMob(p_20970_);
      return false;
   }

   public void causeExtraKnockback(Entity p_453814_, float p_452611_, Vec3 p_456040_) {
      if (p_452611_ > 0.0F && p_453814_ instanceof LivingEntity livingentity) {
         livingentity.knockback(p_452611_, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
      }
   }

   protected void playAttackSound() {
   }

   @Override
   public void tick() {
      super.tick();
      this.updatingUsingItem();
      this.updateSwimAmount();
      if (!this.level().isClientSide()) {
         int i = this.getArrowCount();
         if (i > 0) {
            if (this.removeArrowTime <= 0) {
               this.removeArrowTime = 20 * (30 - i);
            }

            this.removeArrowTime--;
            if (this.removeArrowTime <= 0) {
               this.setArrowCount(i - 1);
            }
         }

         int j = this.getStingerCount();
         if (j > 0) {
            if (this.removeStingerTime <= 0) {
               this.removeStingerTime = 20 * (30 - j);
            }

            this.removeStingerTime--;
            if (this.removeStingerTime <= 0) {
               this.setStingerCount(j - 1);
            }
         }

         this.detectEquipmentUpdates();
         if (this.tickCount % 20 == 0) {
            this.getCombatTracker().recheckStatus();
         }

         if (this.isSleeping() && (!this.canInteractWithLevel() || !this.checkBedExists())) {
            this.stopSleeping();
         }
      }

      if (!this.isRemoved()) {
         this.aiStep();
      }

      double d1 = this.getX() - this.xo;
      double d0 = this.getZ() - this.zo;
      float f = (float)(d1 * d1 + d0 * d0);
      float f1 = this.yBodyRot;
      if (f > 0.0025000002F) {
         float f2 = (float)Mth.atan2(d0, d1) * (180.0F / (float)Math.PI) - 90.0F;
         float f3 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - f2);
         if (95.0F < f3 && f3 < 265.0F) {
            f1 = f2 - 180.0F;
         } else {
            f1 = f2;
         }
      }

      if (this.attackAnim > 0.0F) {
         f1 = this.getYRot();
      }

      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("headTurn");
      this.tickHeadTurn(f1);
      profilerfiller.pop();
      profilerfiller.push("rangeChecks");

      while (this.getYRot() - this.yRotO < -180.0F) {
         this.yRotO -= 360.0F;
      }

      while (this.getYRot() - this.yRotO >= 180.0F) {
         this.yRotO += 360.0F;
      }

      while (this.yBodyRot - this.yBodyRotO < -180.0F) {
         this.yBodyRotO -= 360.0F;
      }

      while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
         this.yBodyRotO += 360.0F;
      }

      while (this.getXRot() - this.xRotO < -180.0F) {
         this.xRotO -= 360.0F;
      }

      while (this.getXRot() - this.xRotO >= 180.0F) {
         this.xRotO += 360.0F;
      }

      while (this.yHeadRot - this.yHeadRotO < -180.0F) {
         this.yHeadRotO -= 360.0F;
      }

      while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
         this.yHeadRotO += 360.0F;
      }

      profilerfiller.pop();
      if (this.isFallFlying()) {
         this.fallFlyTicks++;
      } else {
         this.fallFlyTicks = 0;
      }

      if (this.isSleeping()) {
         this.setXRot(0.0F);
      }

      this.refreshDirtyAttributes();
      this.elytraAnimationState.tick();
   }

   public boolean wasRecentlyStabbed(Entity p_453379_, int p_453316_) {
      if (this.recentKineticEnemies == null) {
         return false;
      } else {
         return this.recentKineticEnemies.containsKey(p_453379_)
            ? this.level().getGameTime() - this.recentKineticEnemies.getLong(p_453379_) < p_453316_
            : false;
      }
   }

   public void rememberStabbedEntity(Entity p_460223_) {
      if (this.recentKineticEnemies != null) {
         this.recentKineticEnemies.put(p_460223_, this.level().getGameTime());
      }
   }

   public int stabbedEntities(Predicate<Entity> p_457499_) {
      return this.recentKineticEnemies == null ? 0 : (int)this.recentKineticEnemies.keySet().stream().filter(p_457499_).count();
   }

   public boolean stabAttack(EquipmentSlot p_450512_, Entity p_454015_, float p_460028_, boolean p_451169_, boolean p_460628_, boolean p_459099_) {
      if (!(this.level() instanceof ServerLevel serverlevel)) {
         return false;
      } else {
         ItemStack itemstack = this.getItemBySlot(p_450512_);
         DamageSource $$9 = itemstack.getDamageSource(this, () -> this.damageSources().mobAttack(this));
         float $$10 = EnchantmentHelper.modifyDamage(serverlevel, itemstack, p_454015_, $$9, p_460028_);
         Vec3 $$11 = p_454015_.getDeltaMovement();
         boolean flag1 = p_451169_ && p_454015_.hurtServer(serverlevel, $$9, $$10);
         boolean $$12 = p_460628_ | flag1;
         if (p_460628_) {
            this.causeExtraKnockback(p_454015_, 0.4F + this.getKnockback(p_454015_, $$9), $$11);
         }

         if (p_459099_ && p_454015_.isPassenger()) {
            $$12 = true;
            p_454015_.stopRiding();
         }

         if (p_454015_ instanceof LivingEntity livingentity) {
            itemstack.hurtEnemy(livingentity, this);
         }

         if (flag1) {
            EnchantmentHelper.doPostAttackEffects(serverlevel, p_454015_, $$9);
         }

         if (!$$12) {
            return false;
         }

         this.setLastHurtMob(p_454015_);
         this.playAttackSound();
         return true;
      }
   }

   public void onAttack() {
   }

   private void detectEquipmentUpdates() {
      Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
      if (map != null) {
         this.handleHandSwap(map);
         if (!map.isEmpty()) {
            this.handleEquipmentChanges(map);
         }
      }
   }

   private @Nullable Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
      Map<EquipmentSlot, ItemStack> map = null;

      for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
         ItemStack itemstack = this.lastEquipmentItems.get(equipmentslot);
         ItemStack itemstack1 = this.getItemBySlot(equipmentslot);
         if (this.equipmentHasChanged(itemstack, itemstack1)) {
            if (map == null) {
               map = Maps.newEnumMap(EquipmentSlot.class);
            }

            map.put(equipmentslot, itemstack1);
            AttributeMap attributemap = this.getAttributes();
            if (!itemstack.isEmpty()) {
               this.stopLocationBasedEffects(itemstack, equipmentslot, attributemap);
            }
         }
      }

      if (map != null) {
         for (Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
            EquipmentSlot equipmentslot1 = entry.getKey();
            ItemStack itemstack2 = entry.getValue();
            if (!itemstack2.isEmpty() && !itemstack2.isBroken()) {
               itemstack2.forEachModifier(equipmentslot1, (p_449417_, p_449418_) -> {
                  AttributeInstance attributeinstance = this.attributes.getInstance(p_449417_);
                  if (attributeinstance != null) {
                     attributeinstance.removeModifier(p_449418_.id());
                     attributeinstance.addTransientModifier(p_449418_);
                  }
               });
               if (this.level() instanceof ServerLevel serverlevel) {
                  EnchantmentHelper.runLocationChangedEffects(serverlevel, itemstack2, this, equipmentslot1);
               }
            }
         }
      }

      return map;
   }

   public boolean equipmentHasChanged(ItemStack p_252265_, ItemStack p_251043_) {
      return !ItemStack.matches(p_251043_, p_252265_);
   }

   private void handleHandSwap(Map<EquipmentSlot, ItemStack> p_21092_) {
      ItemStack itemstack = p_21092_.get(EquipmentSlot.MAINHAND);
      ItemStack itemstack1 = p_21092_.get(EquipmentSlot.OFFHAND);
      if (itemstack != null
         && itemstack1 != null
         && ItemStack.matches(itemstack, this.lastEquipmentItems.get(EquipmentSlot.OFFHAND))
         && ItemStack.matches(itemstack1, this.lastEquipmentItems.get(EquipmentSlot.MAINHAND))) {
         ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundEntityEventPacket(this, (byte)55));
         p_21092_.remove(EquipmentSlot.MAINHAND);
         p_21092_.remove(EquipmentSlot.OFFHAND);
         this.lastEquipmentItems.put(EquipmentSlot.MAINHAND, itemstack.copy());
         this.lastEquipmentItems.put(EquipmentSlot.OFFHAND, itemstack1.copy());
      }
   }

   private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> p_21143_) {
      List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(p_21143_.size());
      p_21143_.forEach((p_390521_, p_390522_) -> {
         ItemStack itemstack = p_390522_.copy();
         list.add(Pair.of(p_390521_, itemstack));
         this.lastEquipmentItems.put(p_390521_, itemstack);
      });
      ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEquipmentPacket(this.getId(), list));
   }

   protected void tickHeadTurn(float p_21260_) {
      float f = Mth.wrapDegrees(p_21260_ - this.yBodyRot);
      this.yBodyRot += f * 0.3F;
      float f1 = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
      float f2 = this.getMaxHeadRotationRelativeToBody();
      if (Math.abs(f1) > f2) {
         this.yBodyRot = this.yBodyRot + (f1 - Mth.sign(f1) * f2);
      }
   }

   protected float getMaxHeadRotationRelativeToBody() {
      return 50.0F;
   }

   public void aiStep() {
      if (this.noJumpDelay > 0) {
         this.noJumpDelay--;
      }

      if (this.isInterpolating()) {
         this.getInterpolation().interpolate();
      } else if (!this.canSimulateMovement()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
      }

      if (this.lerpHeadSteps > 0) {
         this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
         this.lerpHeadSteps--;
      }

      this.equipment.tick(this);
      Vec3 vec3 = this.getDeltaMovement();
      double d0 = vec3.x;
      double d1 = vec3.y;
      double d2 = vec3.z;
      if (this.getType().equals(EntityType.PLAYER)) {
         if (vec3.horizontalDistanceSqr() < 9.0E-6) {
            d0 = 0.0;
            d2 = 0.0;
         }
      } else {
         if (Math.abs(vec3.x) < 0.003) {
            d0 = 0.0;
         }

         if (Math.abs(vec3.z) < 0.003) {
            d2 = 0.0;
         }
      }

      if (Math.abs(vec3.y) < 0.003) {
         d1 = 0.0;
      }

      this.setDeltaMovement(d0, d1, d2);
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("ai");
      this.applyInput();
      if (this.isImmobile()) {
         this.jumping = false;
         this.xxa = 0.0F;
         this.zza = 0.0F;
      } else if (this.isEffectiveAi() && !this.level().isClientSide()) {
         profilerfiller.push("newAi");
         this.serverAiStep();
         profilerfiller.pop();
      }

      profilerfiller.pop();
      profilerfiller.push("jump");
      if (this.jumping && this.isAffectedByFluids()) {
         double d3;
         if (this.isInLava()) {
            d3 = this.getFluidHeight(FluidTags.LAVA);
         } else {
            d3 = this.getFluidHeight(FluidTags.WATER);
         }

         boolean flag = this.isInWater() && d3 > 0.0;
         double d4 = this.getFluidJumpThreshold();
         if (!flag || this.onGround() && !(d3 > d4)) {
            if (!this.isInLava() || this.onGround() && !(d3 > d4)) {
               if ((this.onGround() || flag && d3 <= d4) && this.noJumpDelay == 0) {
                  this.jumpFromGround();
                  this.noJumpDelay = 10;
               }
            } else {
               this.jumpInLiquid(FluidTags.LAVA);
            }
         } else {
            this.jumpInLiquid(FluidTags.WATER);
         }
      } else {
         this.noJumpDelay = 0;
      }

      profilerfiller.pop();
      profilerfiller.push("travel");
      if (this.isFallFlying()) {
         this.updateFallFlying();
      }

      AABB aabb = this.getBoundingBox();
      Vec3 vec31 = new Vec3(this.xxa, this.yya, this.zza);
      if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
         this.resetFallDistance();
      }

      if (this.getControllingPassenger() instanceof Player player && this.isAlive()) {
         this.travelRidden(player, vec31);
      } else if (this.canSimulateMovement() && this.isEffectiveAi()) {
         this.travel(vec31);
      }

      if (!this.level().isClientSide() || this.isLocalInstanceAuthoritative()) {
         this.applyEffectsFromBlocks();
      }

      if (this.level().isClientSide()) {
         this.calculateEntityAnimation(this instanceof FlyingAnimal);
      }

      profilerfiller.pop();
      if (this.level() instanceof ServerLevel serverlevel) {
         profilerfiller.push("freezing");
         if (!this.isInPowderSnow || !this.canFreeze()) {
            this.setTicksFrozen(Math.max(0, this.getTicksFrozen() - 2));
         }

         this.removeFrost();
         this.tryAddFrost();
         if (this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
            this.hurtServer(serverlevel, this.damageSources().freeze(), 1.0F);
         }

         profilerfiller.pop();
      }

      profilerfiller.push("push");
      if (this.autoSpinAttackTicks > 0) {
         this.autoSpinAttackTicks--;
         this.checkAutoSpinAttack(aabb, this.getBoundingBox());
      }

      this.pushEntities();
      profilerfiller.pop();
      if (this.level() instanceof ServerLevel serverlevel1 && this.isSensitiveToWater() && this.isInWaterOrRain()) {
         this.hurtServer(serverlevel1, this.damageSources().drown(), 1.0F);
      }
   }

   protected void applyInput() {
      this.xxa *= 0.98F;
      this.zza *= 0.98F;
   }

   public boolean isSensitiveToWater() {
      return false;
   }

   public boolean isJumping() {
      return this.jumping;
   }

   protected void updateFallFlying() {
      this.checkFallDistanceAccumulation();
      if (!this.level().isClientSide()) {
         if (!this.canGlide()) {
            this.setSharedFlag(7, false);
            return;
         }

         int i = this.fallFlyTicks + 1;
         if (i % 10 == 0) {
            int j = i / 10;
            if (j % 2 == 0) {
               List<EquipmentSlot> list = EquipmentSlot.VALUES.stream().filter(p_358890_ -> canGlideUsing(this.getItemBySlot(p_358890_), p_358890_)).toList();
               EquipmentSlot equipmentslot = Util.getRandom(list, this.random);
               this.getItemBySlot(equipmentslot).hurtAndBreak(1, this, equipmentslot);
            }

            this.gameEvent(GameEvent.ELYTRA_GLIDE);
         }
      }
   }

   protected boolean canGlide() {
      if (!this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
         for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            if (canGlideUsing(this.getItemBySlot(equipmentslot), equipmentslot)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   protected void serverAiStep() {
   }

   protected void pushEntities() {
      List<Entity> list = this.level().getPushableEntities(this, this.getBoundingBox());
      if (!list.isEmpty()) {
         if (this.level() instanceof ServerLevel serverlevel) {
            int j = serverlevel.getGameRules().get(GameRules.MAX_ENTITY_CRAMMING);
            if (j > 0 && list.size() > j - 1 && this.random.nextInt(4) == 0) {
               int i = 0;

               for (Entity entity : list) {
                  if (!entity.isPassenger()) {
                     i++;
                  }
               }

               if (i > j - 1) {
                  this.hurtServer(serverlevel, this.damageSources().cramming(), 6.0F);
               }
            }
         }

         for (Entity entity1 : list) {
            this.doPush(entity1);
         }
      }
   }

   protected void checkAutoSpinAttack(AABB p_21072_, AABB p_21073_) {
      AABB aabb = p_21072_.minmax(p_21073_);
      List<Entity> list = this.level().getEntities(this, aabb);
      if (!list.isEmpty()) {
         for (Entity entity : list) {
            if (entity instanceof LivingEntity) {
               this.doAutoAttackOnTouch((LivingEntity)entity);
               this.autoSpinAttackTicks = 0;
               this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
               break;
            }
         }
      } else if (this.horizontalCollision) {
         this.autoSpinAttackTicks = 0;
      }

      if (!this.level().isClientSide() && this.autoSpinAttackTicks <= 0) {
         this.setLivingEntityFlag(4, false);
         this.autoSpinAttackDmg = 0.0F;
         this.autoSpinAttackItemStack = null;
      }
   }

   protected void doPush(Entity p_20971_) {
      p_20971_.push(this);
   }

   protected void doAutoAttackOnTouch(LivingEntity p_21277_) {
   }

   public boolean isAutoSpinAttack() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
   }

   @Override
   public void stopRiding() {
      Entity entity = this.getVehicle();
      super.stopRiding();
      if (entity != null && entity != this.getVehicle() && !this.level().isClientSide()) {
         this.dismountVehicle(entity);
      }
   }

   @Override
   public void rideTick() {
      super.rideTick();
      this.resetFallDistance();
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return this.interpolation;
   }

   @Override
   public void lerpHeadTo(float p_21005_, int p_21006_) {
      this.lerpYHeadRot = p_21005_;
      this.lerpHeadSteps = p_21006_;
   }

   public void setJumping(boolean p_21314_) {
      this.jumping = p_21314_;
   }

   public void onItemPickup(ItemEntity p_21054_) {
      Entity entity = p_21054_.getOwner();
      if (entity instanceof ServerPlayer) {
         CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)entity, p_21054_.getItem(), this);
      }
   }

   public void take(Entity p_21030_, int p_21031_) {
      if (!p_21030_.isRemoved()
         && !this.level().isClientSide()
         && (p_21030_ instanceof ItemEntity || p_21030_ instanceof AbstractArrow || p_21030_ instanceof ExperienceOrb)) {
         ((ServerLevel)this.level())
            .getChunkSource()
            .sendToTrackingPlayers(p_21030_, new ClientboundTakeItemEntityPacket(p_21030_.getId(), this.getId(), p_21031_));
      }
   }

   public boolean hasLineOfSight(Entity p_147185_) {
      return this.hasLineOfSight(p_147185_, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p_147185_.getEyeY());
   }

   public boolean hasLineOfSight(Entity p_364584_, ClipContext.Block p_368226_, ClipContext.Fluid p_368626_, double p_375593_) {
      if (p_364584_.level() != this.level()) {
         return false;
      }

      Vec3 vec3 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
      Vec3 vec31 = new Vec3(p_364584_.getX(), p_375593_, p_364584_.getZ());
      return vec31.distanceTo(vec3) > 128.0
         ? false
         : this.level().clip(new ClipContext(vec3, vec31, p_368226_, p_368626_, this)).getType() == HitResult.Type.MISS;
   }

   @Override
   public float getViewYRot(float p_21286_) {
      return p_21286_ == 1.0F ? this.yHeadRot : Mth.rotLerp(p_21286_, this.yHeadRotO, this.yHeadRot);
   }

   public float getAttackAnim(float p_21325_) {
      float f = this.attackAnim - this.oAttackAnim;
      if (f < 0.0F) {
         f++;
      }

      return this.oAttackAnim + f * p_21325_;
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   @Override
   public boolean isPushable() {
      return this.isAlive() && !this.isSpectator() && !this.onClimbable();
   }

   @Override
   public float getYHeadRot() {
      return this.yHeadRot;
   }

   @Override
   public void setYHeadRot(float p_21306_) {
      this.yHeadRot = p_21306_;
   }

   @Override
   public void setYBodyRot(float p_21309_) {
      this.yBodyRot = p_21309_;
   }

   @Override
   public Vec3 getRelativePortalPosition(Direction.Axis p_21085_, BlockUtil.FoundRectangle p_459153_) {
      return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(p_21085_, p_459153_));
   }

   public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 p_21290_) {
      return new Vec3(p_21290_.x, p_21290_.y, 0.0);
   }

   public float getAbsorptionAmount() {
      return this.absorptionAmount;
   }

   public final void setAbsorptionAmount(float p_21328_) {
      this.internalSetAbsorptionAmount(Mth.clamp(p_21328_, 0.0F, this.getMaxAbsorption()));
   }

   protected void internalSetAbsorptionAmount(float p_299471_) {
      this.absorptionAmount = p_299471_;
   }

   public void onEnterCombat() {
   }

   public void onLeaveCombat() {
   }

   protected void updateEffectVisibility() {
      this.effectsDirty = true;
   }

   public abstract HumanoidArm getMainArm();

   public boolean isUsingItem() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
   }

   public InteractionHand getUsedItemHand() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   private void updatingUsingItem() {
      if (this.isUsingItem()) {
         if (ItemStack.isSameItem(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            this.updateUsingItem(this.useItem);
         } else {
            this.stopUsingItem();
         }
      }
   }

   private @Nullable ItemEntity createItemStackToDrop(ItemStack p_395029_, boolean p_394798_, boolean p_392209_) {
      if (p_395029_.isEmpty()) {
         return null;
      }

      double d0 = this.getEyeY() - 0.3F;
      ItemEntity itementity = new ItemEntity(this.level(), this.getX(), d0, this.getZ(), p_395029_);
      itementity.setPickUpDelay(40);
      if (p_392209_) {
         itementity.setThrower(this);
      }

      if (p_394798_) {
         float f = this.random.nextFloat() * 0.5F;
         float f1 = this.random.nextFloat() * (float) (Math.PI * 2);
         itementity.setDeltaMovement(-Mth.sin(f1) * f, 0.2F, Mth.cos(f1) * f);
      } else {
         float f7 = 0.3F;
         float f8 = Mth.sin(this.getXRot() * (float) (Math.PI / 180.0));
         float f2 = Mth.cos(this.getXRot() * (float) (Math.PI / 180.0));
         float f3 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
         float f4 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
         float f5 = this.random.nextFloat() * (float) (Math.PI * 2);
         float f6 = 0.02F * this.random.nextFloat();
         itementity.setDeltaMovement(
            -f3 * f2 * 0.3F + Math.cos(f5) * f6,
            -f8 * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
            f4 * f2 * 0.3F + Math.sin(f5) * f6
         );
      }

      return itementity;
   }

   protected void updateUsingItem(ItemStack p_147201_) {
      p_147201_.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
      if (--this.useItemRemaining == 0 && !this.level().isClientSide() && !p_147201_.useOnRelease()) {
         this.completeUsingItem();
      }
   }

   private void updateSwimAmount() {
      this.swimAmountO = this.swimAmount;
      if (this.isVisuallySwimming()) {
         this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
      } else {
         this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
      }
   }

   protected void setLivingEntityFlag(int p_21156_, boolean p_21157_) {
      int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
      if (p_21157_) {
         i |= p_21156_;
      } else {
         i &= ~p_21156_;
      }

      this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)i);
   }

   public void startUsingItem(InteractionHand p_21159_) {
      ItemStack itemstack = this.getItemInHand(p_21159_);
      if (!itemstack.isEmpty() && !this.isUsingItem()) {
         this.useItem = itemstack;
         this.useItemRemaining = itemstack.getUseDuration(this);
         if (!this.level().isClientSide()) {
            this.setLivingEntityFlag(1, true);
            this.setLivingEntityFlag(2, p_21159_ == InteractionHand.OFF_HAND);
            this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_START);
            if (this.useItem.has(DataComponents.KINETIC_WEAPON)) {
               this.recentKineticEnemies = new Object2LongOpenHashMap();
            }
         }
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_21104_) {
      super.onSyncedDataUpdated(p_21104_);
      if (SLEEPING_POS_ID.equals(p_21104_)) {
         if (this.level().isClientSide()) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
         }
      } else if (DATA_LIVING_ENTITY_FLAGS.equals(p_21104_) && this.level().isClientSide()) {
         if (this.isUsingItem() && this.useItem.isEmpty()) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            if (!this.useItem.isEmpty()) {
               this.useItemRemaining = this.useItem.getUseDuration(this);
            }
         } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
            this.useItem = ItemStack.EMPTY;
            this.useItemRemaining = 0;
         }
      }
   }

   @Override
   public void lookAt(EntityAnchorArgument.Anchor p_21078_, Vec3 p_21079_) {
      super.lookAt(p_21078_, p_21079_);
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRot = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
   }

   @Override
   public float getPreciseBodyRotation(float p_345405_) {
      return Mth.lerp(p_345405_, this.yBodyRotO, this.yBodyRot);
   }

   public void spawnItemParticles(ItemStack p_21061_, int p_21062_) {
      for (int i = 0; i < p_21062_; i++) {
         Vec3 vec3 = new Vec3((this.random.nextFloat() - 0.5) * 0.1, this.random.nextFloat() * 0.1 + 0.1, 0.0);
         vec3 = vec3.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
         vec3 = vec3.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         double d0 = -this.random.nextFloat() * 0.6 - 0.3;
         Vec3 vec31 = new Vec3((this.random.nextFloat() - 0.5) * 0.3, d0, 0.6);
         vec31 = vec31.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
         vec31 = vec31.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         vec31 = vec31.add(this.getX(), this.getEyeY(), this.getZ());
         this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, p_21061_), vec31.x, vec31.y, vec31.z, vec3.x, vec3.y + 0.05, vec3.z);
      }
   }

   protected void completeUsingItem() {
      if (!this.level().isClientSide() || this.isUsingItem()) {
         InteractionHand interactionhand = this.getUsedItemHand();
         if (!this.useItem.equals(this.getItemInHand(interactionhand))) {
            this.releaseUsingItem();
         } else if (!this.useItem.isEmpty() && this.isUsingItem()) {
            ItemStack itemstack = this.useItem.finishUsingItem(this.level(), this);
            if (itemstack != this.useItem) {
               this.setItemInHand(interactionhand, itemstack);
            }

            this.stopUsingItem();
         }
      }
   }

   public void handleExtraItemsCreatedOnUse(ItemStack p_367452_) {
   }

   public ItemStack getUseItem() {
      return this.useItem;
   }

   public int getUseItemRemainingTicks() {
      return this.useItemRemaining;
   }

   public int getTicksUsingItem() {
      return this.isUsingItem() ? this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks() : 0;
   }

   public float getTicksUsingItem(float p_456290_) {
      return !this.isUsingItem() ? 0.0F : this.getTicksUsingItem() + p_456290_;
   }

   public void releaseUsingItem() {
      ItemStack itemstack = this.getItemInHand(this.getUsedItemHand());
      if (!this.useItem.isEmpty() && ItemStack.isSameItem(itemstack, this.useItem)) {
         this.useItem = itemstack;
         this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
         if (this.useItem.useOnRelease()) {
            this.updatingUsingItem();
         }
      }

      this.stopUsingItem();
   }

   public void stopUsingItem() {
      if (!this.level().isClientSide()) {
         boolean flag = this.isUsingItem();
         this.recentKineticEnemies = null;
         this.setLivingEntityFlag(1, false);
         if (flag) {
            this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_FINISH);
         }
      }

      this.useItem = ItemStack.EMPTY;
      this.useItemRemaining = 0;
   }

   public boolean isBlocking() {
      return this.getItemBlockingWith() != null;
   }

   public @Nullable ItemStack getItemBlockingWith() {
      if (!this.isUsingItem()) {
         return null;
      }

      BlocksAttacks blocksattacks = this.useItem.get(DataComponents.BLOCKS_ATTACKS);
      if (blocksattacks != null) {
         int i = this.useItem.getItem().getUseDuration(this.useItem, this) - this.useItemRemaining;
         if (i >= blocksattacks.blockDelayTicks()) {
            return this.useItem;
         }
      }

      return null;
   }

   public boolean isSuppressingSlidingDownLadder() {
      return this.isShiftKeyDown();
   }

   public boolean isFallFlying() {
      return this.getSharedFlag(7);
   }

   @Override
   public boolean isVisuallySwimming() {
      return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
   }

   public int getFallFlyingTicks() {
      return this.fallFlyTicks;
   }

   public boolean randomTeleport(double p_20985_, double p_20986_, double p_20987_, boolean p_20988_) {
      double d0 = this.getX();
      double d1 = this.getY();
      double d2 = this.getZ();
      double d3 = p_20986_;
      boolean flag = false;
      BlockPos blockpos = BlockPos.containing(p_20985_, p_20986_, p_20987_);
      Level level = this.level();
      if (level.hasChunkAt(blockpos)) {
         boolean flag1 = false;

         while (!flag1 && blockpos.getY() > level.getMinY()) {
            BlockPos blockpos1 = blockpos.below();
            BlockState blockstate = level.getBlockState(blockpos1);
            if (blockstate.blocksMotion()) {
               flag1 = true;
            } else {
               d3--;
               blockpos = blockpos1;
            }
         }

         if (flag1) {
            this.teleportTo(p_20985_, d3, p_20987_);
            if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
               flag = true;
            }
         }
      }

      if (!flag) {
         this.teleportTo(d0, d1, d2);
         return false;
      }

      if (p_20988_) {
         level.broadcastEntityEvent(this, (byte)46);
      }

      if (this instanceof PathfinderMob pathfindermob) {
         pathfindermob.getNavigation().stop();
      }

      return true;
   }

   public boolean isAffectedByPotions() {
      return !this.isDeadOrDying();
   }

   public boolean attackable() {
      return true;
   }

   public void setRecordPlayingNearby(BlockPos p_21082_, boolean p_21083_) {
   }

   public boolean canPickUpLoot() {
      return false;
   }

   @Override
   public final EntityDimensions getDimensions(Pose p_21047_) {
      return p_21047_ == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(p_21047_).scale(this.getScale());
   }

   protected EntityDimensions getDefaultDimensions(Pose p_334284_) {
      return this.getType().getDimensions().scale(this.getAgeScale());
   }

   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING);
   }

   public AABB getLocalBoundsForPose(Pose p_21271_) {
      EntityDimensions entitydimensions = this.getDimensions(p_21271_);
      return new AABB(
         -entitydimensions.width() / 2.0F,
         0.0,
         -entitydimensions.width() / 2.0F,
         entitydimensions.width() / 2.0F,
         entitydimensions.height(),
         entitydimensions.width() / 2.0F
      );
   }

   protected boolean wouldNotSuffocateAtTargetPose(Pose p_297537_) {
      AABB aabb = this.getDimensions(p_297537_).makeBoundingBox(this.position());
      return this.level().noBlockCollision(this, aabb);
   }

   @Override
   public boolean canUsePortal(boolean p_342370_) {
      return super.canUsePortal(p_342370_) && !this.isSleeping();
   }

   public Optional<BlockPos> getSleepingPos() {
      return this.entityData.get(SLEEPING_POS_ID);
   }

   public void setSleepingPos(BlockPos p_21251_) {
      this.entityData.set(SLEEPING_POS_ID, Optional.of(p_21251_));
   }

   public void clearSleepingPos() {
      this.entityData.set(SLEEPING_POS_ID, Optional.empty());
   }

   public boolean isSleeping() {
      return this.getSleepingPos().isPresent();
   }

   public void startSleeping(BlockPos p_21141_) {
      if (this.isPassenger()) {
         this.stopRiding();
      }

      BlockState blockstate = this.level().getBlockState(p_21141_);
      if (blockstate.getBlock() instanceof BedBlock) {
         this.level().setBlock(p_21141_, blockstate.setValue(BedBlock.OCCUPIED, true), 3);
      }

      this.setPose(Pose.SLEEPING);
      this.setPosToBed(p_21141_);
      this.setSleepingPos(p_21141_);
      this.setDeltaMovement(Vec3.ZERO);
      this.needsSync = true;
   }

   private void setPosToBed(BlockPos p_21081_) {
      this.setPos(p_21081_.getX() + 0.5, p_21081_.getY() + 0.6875, p_21081_.getZ() + 0.5);
   }

   private boolean checkBedExists() {
      return this.getSleepingPos().map(p_449422_ -> this.level().getBlockState(p_449422_).getBlock() instanceof BedBlock).orElse(false);
   }

   public void stopSleeping() {
      this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(p_261435_ -> {
         BlockState blockstate = this.level().getBlockState(p_261435_);
         if (blockstate.getBlock() instanceof BedBlock) {
            Direction direction = blockstate.getValue(BedBlock.FACING);
            this.level().setBlock(p_261435_, blockstate.setValue(BedBlock.OCCUPIED, false), 3);
            Vec3 vec31 = BedBlock.findStandUpPosition(this.getType(), this.level(), p_261435_, direction, this.getYRot()).orElseGet(() -> {
               BlockPos blockpos = p_261435_.above();
               return new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.1, blockpos.getZ() + 0.5);
            });
            Vec3 vec32 = Vec3.atBottomCenterOf(p_261435_).subtract(vec31).normalize();
            float f = (float)Mth.wrapDegrees(Mth.atan2(vec32.z, vec32.x) * 180.0 / (float) Math.PI - 90.0);
            this.setPos(vec31.x, vec31.y, vec31.z);
            this.setYRot(f);
            this.setXRot(0.0F);
         }
      });
      Vec3 vec3 = this.position();
      this.setPose(Pose.STANDING);
      this.setPos(vec3.x, vec3.y, vec3.z);
      this.clearSleepingPos();
   }

   public @Nullable Direction getBedOrientation() {
      BlockPos blockpos = this.getSleepingPos().orElse(null);
      return blockpos != null ? BedBlock.getBedOrientation(this.level(), blockpos) : null;
   }

   @Override
   public boolean isInWall() {
      return !this.isSleeping() && super.isInWall();
   }

   public ItemStack getProjectile(ItemStack p_21272_) {
      return ItemStack.EMPTY;
   }

   private static byte entityEventForEquipmentBreak(EquipmentSlot p_21267_) {
      return switch (p_21267_) {
         case MAINHAND -> 47;
         case OFFHAND -> 48;
         case HEAD -> 49;
         case CHEST -> 50;
         case FEET -> 52;
         case LEGS -> 51;
         case BODY -> 65;
         case SADDLE -> 68;
      };
   }

   public void onEquippedItemBroken(Item p_343772_, EquipmentSlot p_345353_) {
      this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(p_345353_));
      this.stopLocationBasedEffects(this.getItemBySlot(p_345353_), p_345353_, this.attributes);
   }

   private void stopLocationBasedEffects(ItemStack p_369098_, EquipmentSlot p_365471_, AttributeMap p_363505_) {
      p_369098_.forEachModifier(p_365471_, (p_358882_, p_358883_) -> {
         AttributeInstance attributeinstance = p_363505_.getInstance(p_358882_);
         if (attributeinstance != null) {
            attributeinstance.removeModifier(p_358883_);
         }
      });
      EnchantmentHelper.stopLocationBasedEffects(p_369098_, this, p_365471_);
   }

   public final boolean canEquipWithDispenser(ItemStack p_362526_) {
      if (this.isAlive() && !this.isSpectator()) {
         Equippable equippable = p_362526_.get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.dispensable()) {
            EquipmentSlot equipmentslot = equippable.slot();
            return this.canUseSlot(equipmentslot) && equippable.canBeEquippedBy(this.getType())
               ? this.getItemBySlot(equipmentslot).isEmpty() && this.canDispenserEquipIntoSlot(equipmentslot)
               : false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_362370_) {
      return true;
   }

   public final EquipmentSlot getEquipmentSlotForItem(ItemStack p_147234_) {
      Equippable equippable = p_147234_.get(DataComponents.EQUIPPABLE);
      return equippable != null && this.canUseSlot(equippable.slot()) ? equippable.slot() : EquipmentSlot.MAINHAND;
   }

   public final boolean isEquippableInSlot(ItemStack p_21138_, EquipmentSlot p_361200_) {
      Equippable equippable = p_21138_.get(DataComponents.EQUIPPABLE);
      return equippable == null
         ? p_361200_ == EquipmentSlot.MAINHAND && this.canUseSlot(EquipmentSlot.MAINHAND)
         : p_361200_ == equippable.slot() && this.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(this.getType());
   }

   private static SlotAccess createEquipmentSlotAccess(LivingEntity p_147196_, EquipmentSlot p_147197_) {
      return p_147197_ != EquipmentSlot.HEAD && p_147197_ != EquipmentSlot.MAINHAND && p_147197_ != EquipmentSlot.OFFHAND
         ? SlotAccess.forEquipmentSlot(p_147196_, p_147197_, p_341262_ -> p_341262_.isEmpty() || p_147196_.getEquipmentSlotForItem(p_341262_) == p_147197_)
         : SlotAccess.forEquipmentSlot(p_147196_, p_147197_);
   }

   private static @Nullable EquipmentSlot getEquipmentSlot(int p_147212_) {
      if (p_147212_ == 100 + EquipmentSlot.HEAD.getIndex()) {
         return EquipmentSlot.HEAD;
      } else if (p_147212_ == 100 + EquipmentSlot.CHEST.getIndex()) {
         return EquipmentSlot.CHEST;
      } else if (p_147212_ == 100 + EquipmentSlot.LEGS.getIndex()) {
         return EquipmentSlot.LEGS;
      } else if (p_147212_ == 100 + EquipmentSlot.FEET.getIndex()) {
         return EquipmentSlot.FEET;
      } else if (p_147212_ == 98) {
         return EquipmentSlot.MAINHAND;
      } else if (p_147212_ == 99) {
         return EquipmentSlot.OFFHAND;
      } else if (p_147212_ == 105) {
         return EquipmentSlot.BODY;
      } else {
         return p_147212_ == 106 ? EquipmentSlot.SADDLE : null;
      }
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_147238_) {
      EquipmentSlot equipmentslot = getEquipmentSlot(p_147238_);
      return equipmentslot != null ? createEquipmentSlotAccess(this, equipmentslot) : super.getSlot(p_147238_);
   }

   @Override
   public boolean canFreeze() {
      if (this.isSpectator()) {
         return false;
      }

      for (EquipmentSlot equipmentslot : EquipmentSlotGroup.ARMOR) {
         if (this.getItemBySlot(equipmentslot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
            return false;
         }
      }

      return super.canFreeze();
   }

   @Override
   public boolean isCurrentlyGlowing() {
      return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
   }

   @Override
   public float getVisualRotationYInDegrees() {
      return this.yBodyRot;
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_217037_) {
      double d0 = p_217037_.getX();
      double d1 = p_217037_.getY();
      double d2 = p_217037_.getZ();
      float f = p_217037_.getYRot();
      float f1 = p_217037_.getXRot();
      this.syncPacketPositionCodec(d0, d1, d2);
      this.yBodyRot = p_217037_.getYHeadRot();
      this.yHeadRot = p_217037_.getYHeadRot();
      this.yBodyRotO = this.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.setId(p_217037_.getId());
      this.setUUID(p_217037_.getUUID());
      this.absSnapTo(d0, d1, d2, f, f1);
      this.setDeltaMovement(p_217037_.getMovement());
   }

   public float getSecondsToDisableBlocking() {
      ItemStack itemstack = this.getWeaponItem();
      Weapon weapon = itemstack.get(DataComponents.WEAPON);
      return weapon != null && itemstack == this.getActiveItem() ? weapon.disableBlockingForSeconds() : 0.0F;
   }

   @Override
   public float maxUpStep() {
      float f = (float)this.getAttributeValue(Attributes.STEP_HEIGHT);
      return this.getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;
   }

   @Override
   public Vec3 getPassengerRidingPosition(Entity p_299288_) {
      return this.position().add(this.getPassengerAttachmentPoint(p_299288_, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
   }

   protected void lerpHeadRotationStep(int p_297258_, double p_301409_) {
      this.yHeadRot = (float)Mth.rotLerp(1.0 / p_297258_, this.yHeadRot, p_301409_);
   }

   @Override
   public void igniteForTicks(int p_328356_) {
      super.igniteForTicks(Mth.ceil(p_328356_ * this.getAttributeValue(Attributes.BURNING_TIME)));
   }

   public boolean hasInfiniteMaterials() {
      return false;
   }

   public boolean isInvulnerableTo(ServerLevel p_361436_, DamageSource p_345519_) {
      return this.isInvulnerableToBase(p_345519_) || EnchantmentHelper.isImmuneToDamage(p_361436_, this, p_345519_);
   }

   public static boolean canGlideUsing(ItemStack p_369788_, EquipmentSlot p_365879_) {
      if (!p_369788_.has(DataComponents.GLIDER)) {
         return false;
      }

      Equippable equippable = p_369788_.get(DataComponents.EQUIPPABLE);
      return equippable != null && p_365879_ == equippable.slot() && !p_369788_.nextDamageWillBreak();
   }

   @VisibleForTesting
   public int getLastHurtByPlayerMemoryTime() {
      return this.lastHurtByPlayerMemoryTime;
   }

   @Override
   public boolean isTransmittingWaypoint() {
      return this.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE) > 0.0;
   }

   @Override
   public Optional<WaypointTransmitter.Connection> makeWaypointConnectionWith(ServerPlayer p_410245_) {
      if (this.firstTick || p_410245_ == this) {
         return Optional.empty();
      } else if (WaypointTransmitter.doesSourceIgnoreReceiver(this, p_410245_)) {
         return Optional.empty();
      } else {
         Waypoint.Icon waypoint$icon = this.locatorBarIcon.cloneAndAssignStyle(this);
         if (WaypointTransmitter.isReallyFar(this, p_410245_)) {
            return Optional.of(new WaypointTransmitter.EntityAzimuthConnection(this, waypoint$icon, p_410245_));
         } else {
            return !WaypointTransmitter.isChunkVisible(this.chunkPosition(), p_410245_)
               ? Optional.of(new WaypointTransmitter.EntityChunkConnection(this, waypoint$icon, p_410245_))
               : Optional.of(new WaypointTransmitter.EntityBlockConnection(this, waypoint$icon, p_410245_));
         }
      }
   }

   @Override
   public Waypoint.Icon waypointIcon() {
      return this.locatorBarIcon;
   }

   public record Fallsounds(SoundEvent small, SoundEvent big) {
   }
}
