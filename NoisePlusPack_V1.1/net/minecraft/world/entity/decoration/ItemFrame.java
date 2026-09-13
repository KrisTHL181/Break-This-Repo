package net.minecraft.world.entity.decoration;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ItemFrame extends HangingEntity {
   private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.ITEM_STACK);
   private static final EntityDataAccessor<Integer> DATA_ROTATION = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.INT);
   public static final int NUM_ROTATIONS = 8;
   private static final float DEPTH = 0.0625F;
   private static final float WIDTH = 0.75F;
   private static final float HEIGHT = 0.75F;
   private static final byte DEFAULT_ROTATION = 0;
   private static final float DEFAULT_DROP_CHANCE = 1.0F;
   private static final boolean DEFAULT_INVISIBLE = false;
   private static final boolean DEFAULT_FIXED = false;
   private float dropChance = 1.0F;
   private boolean fixed = false;

   public ItemFrame(EntityType<? extends ItemFrame> p_31761_, Level p_31762_) {
      super(p_31761_, p_31762_);
      this.setInvisible(false);
   }

   public ItemFrame(Level p_31764_, BlockPos p_31765_, Direction p_31766_) {
      this(EntityType.ITEM_FRAME, p_31764_, p_31765_, p_31766_);
   }

   public ItemFrame(EntityType<? extends ItemFrame> p_149621_, Level p_149622_, BlockPos p_149623_, Direction p_149624_) {
      super(p_149621_, p_149622_, p_149623_);
      this.setDirection(p_149624_);
      this.setInvisible(false);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_330856_) {
      super.defineSynchedData(p_330856_);
      p_330856_.define(DATA_ITEM, ItemStack.EMPTY);
      p_330856_.define(DATA_ROTATION, 0);
   }

   @Override
   protected void setDirection(Direction p_31793_) {
      Objects.requireNonNull(p_31793_);
      super.setDirectionRaw(p_31793_);
      if (p_31793_.getAxis().isHorizontal()) {
         this.setXRot(0.0F);
         this.setYRot(p_31793_.get2DDataValue() * 90);
      } else {
         this.setXRot(-90 * p_31793_.getAxisDirection().getStep());
         this.setYRot(0.0F);
      }

      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
      this.recalculateBoundingBox();
   }

   @Override
   protected final void recalculateBoundingBox() {
      super.recalculateBoundingBox();
      this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
   }

   @Override
   protected AABB calculateBoundingBox(BlockPos p_343359_, Direction p_343934_) {
      return this.createBoundingBox(p_343359_, p_343934_, this.hasFramedMap());
   }

   @Override
   protected AABB getPopBox() {
      return this.createBoundingBox(this.pos, this.getDirection(), false);
   }

   private AABB createBoundingBox(BlockPos p_432027_, Direction p_432054_, boolean p_432030_) {
      float f = 0.46875F;
      Vec3 vec3 = Vec3.atCenterOf(p_432027_).relative(p_432054_, -0.46875);
      float f1 = p_432030_ ? 1.0F : 0.75F;
      float f2 = p_432030_ ? 1.0F : 0.75F;
      Direction.Axis direction$axis = p_432054_.getAxis();
      double d0 = direction$axis == Direction.Axis.X ? 0.0625 : f1;
      double d1 = direction$axis == Direction.Axis.Y ? 0.0625 : f2;
      double d2 = direction$axis == Direction.Axis.Z ? 0.0625 : f1;
      return AABB.ofSize(vec3, d0, d1, d2);
   }

   @Override
   public boolean survives() {
      if (this.fixed) {
         return true;
      }

      if (this.hasLevelCollision(this.getPopBox())) {
         return false;
      }

      BlockState blockstate = this.level().getBlockState(this.pos.relative(this.getDirection().getOpposite()));
      return !blockstate.isSolid() && (!this.getDirection().getAxis().isHorizontal() || !DiodeBlock.isDiode(blockstate)) ? false : this.canCoexist(true);
   }

   @Override
   public void move(MoverType p_31781_, Vec3 p_31782_) {
      if (!this.fixed) {
         super.move(p_31781_, p_31782_);
      }
   }

   @Override
   public void push(double p_31817_, double p_31818_, double p_31819_) {
      if (!this.fixed) {
         super.push(p_31817_, p_31818_, p_31819_);
      }
   }

   @Override
   public void kill(ServerLevel p_369840_) {
      this.removeFramedMap(this.getItem());
      super.kill(p_369840_);
   }

   private boolean shouldDamageDropItem(DamageSource p_369303_) {
      return !p_369303_.is(DamageTypeTags.IS_EXPLOSION) && !this.getItem().isEmpty();
   }

   private static boolean canHurtWhenFixed(DamageSource p_363479_) {
      return p_363479_.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || p_363479_.isCreativePlayer();
   }

   @Override
   public boolean hurtClient(DamageSource p_367549_) {
      return this.fixed && !canHurtWhenFixed(p_367549_) ? false : !this.isInvulnerableToBase(p_367549_);
   }

   @Override
   public boolean hurtServer(ServerLevel p_362682_, DamageSource p_364307_, float p_368153_) {
      if (!this.fixed) {
         if (this.isInvulnerableToBase(p_364307_)) {
            return false;
         } else if (this.shouldDamageDropItem(p_364307_)) {
            this.dropItem(p_362682_, p_364307_.getEntity(), false);
            this.gameEvent(GameEvent.BLOCK_CHANGE, p_364307_.getEntity());
            this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
            return true;
         } else {
            return super.hurtServer(p_362682_, p_364307_, p_368153_);
         }
      } else {
         return canHurtWhenFixed(p_364307_) && super.hurtServer(p_362682_, p_364307_, p_368153_);
      }
   }

   public SoundEvent getRemoveItemSound() {
      return SoundEvents.ITEM_FRAME_REMOVE_ITEM;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_31769_) {
      double d0 = 16.0;
      d0 *= 64.0 * getViewScale();
      return p_31769_ < d0 * d0;
   }

   @Override
   public void dropItem(ServerLevel p_366964_, @Nullable Entity p_31803_) {
      this.playSound(this.getBreakSound(), 1.0F, 1.0F);
      this.dropItem(p_366964_, p_31803_, true);
      this.gameEvent(GameEvent.BLOCK_CHANGE, p_31803_);
   }

   public SoundEvent getBreakSound() {
      return SoundEvents.ITEM_FRAME_BREAK;
   }

   @Override
   public void playPlacementSound() {
      this.playSound(this.getPlaceSound(), 1.0F, 1.0F);
   }

   public SoundEvent getPlaceSound() {
      return SoundEvents.ITEM_FRAME_PLACE;
   }

   private void dropItem(ServerLevel p_360759_, @Nullable Entity p_31779_, boolean p_368306_) {
      if (!this.fixed) {
         ItemStack itemstack = this.getItem();
         this.setItem(ItemStack.EMPTY);
         if (!p_360759_.getGameRules().get(GameRules.ENTITY_DROPS)) {
            if (p_31779_ == null) {
               this.removeFramedMap(itemstack);
            }
         } else if (p_31779_ instanceof Player player && player.hasInfiniteMaterials()) {
            this.removeFramedMap(itemstack);
         } else {
            if (p_368306_) {
               this.spawnAtLocation(p_360759_, this.getFrameItemStack());
            }

            if (!itemstack.isEmpty()) {
               itemstack = itemstack.copy();
               this.removeFramedMap(itemstack);
               if (this.random.nextFloat() < this.dropChance) {
                  this.spawnAtLocation(p_360759_, itemstack);
               }
            }
         }
      }
   }

   private void removeFramedMap(ItemStack p_31811_) {
      MapId mapid = this.getFramedMapId(p_31811_);
      if (mapid != null) {
         MapItemSavedData mapitemsaveddata = MapItem.getSavedData(mapid, this.level());
         if (mapitemsaveddata != null) {
            mapitemsaveddata.removedFromFrame(this.pos, this.getId());
         }
      }

      p_31811_.setEntityRepresentation(null);
   }

   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM);
   }

   public @Nullable MapId getFramedMapId(ItemStack p_342645_) {
      return p_342645_.get(DataComponents.MAP_ID);
   }

   public boolean hasFramedMap() {
      return this.getItem().has(DataComponents.MAP_ID);
   }

   public void setItem(ItemStack p_31806_) {
      this.setItem(p_31806_, true);
   }

   public void setItem(ItemStack p_31790_, boolean p_31791_) {
      if (!p_31790_.isEmpty()) {
         p_31790_ = p_31790_.copyWithCount(1);
      }

      this.onItemChanged(p_31790_);
      this.getEntityData().set(DATA_ITEM, p_31790_);
      if (!p_31790_.isEmpty()) {
         this.playSound(this.getAddItemSound(), 1.0F, 1.0F);
      }

      if (p_31791_ && this.pos != null) {
         this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
      }
   }

   public SoundEvent getAddItemSound() {
      return SoundEvents.ITEM_FRAME_ADD_ITEM;
   }

   @Override
   public @Nullable SlotAccess getSlot(int p_149629_) {
      return p_149629_ == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(p_149629_);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_31797_) {
      super.onSyncedDataUpdated(p_31797_);
      if (p_31797_.equals(DATA_ITEM)) {
         this.onItemChanged(this.getItem());
      }
   }

   private void onItemChanged(ItemStack p_218866_) {
      if (!p_218866_.isEmpty() && p_218866_.getFrame() != this) {
         p_218866_.setEntityRepresentation(this);
      }

      this.recalculateBoundingBox();
   }

   public int getRotation() {
      return this.getEntityData().get(DATA_ROTATION);
   }

   public void setRotation(int p_31771_) {
      this.setRotation(p_31771_, true);
   }

   private void setRotation(int p_31773_, boolean p_31774_) {
      this.getEntityData().set(DATA_ROTATION, p_31773_ % 8);
      if (p_31774_ && this.pos != null) {
         this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_407324_) {
      super.addAdditionalSaveData(p_407324_);
      ItemStack itemstack = this.getItem();
      if (!itemstack.isEmpty()) {
         p_407324_.store("Item", ItemStack.CODEC, itemstack);
      }

      p_407324_.putByte("ItemRotation", (byte)this.getRotation());
      p_407324_.putFloat("ItemDropChance", this.dropChance);
      p_407324_.store("Facing", Direction.LEGACY_ID_CODEC, this.getDirection());
      p_407324_.putBoolean("Invisible", this.isInvisible());
      p_407324_.putBoolean("Fixed", this.fixed);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_406660_) {
      super.readAdditionalSaveData(p_406660_);
      ItemStack itemstack = p_406660_.<ItemStack>read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
      ItemStack itemstack1 = this.getItem();
      if (!itemstack1.isEmpty() && !ItemStack.matches(itemstack, itemstack1)) {
         this.removeFramedMap(itemstack1);
      }

      this.setItem(itemstack, false);
      this.setRotation(p_406660_.getByteOr("ItemRotation", (byte)0), false);
      this.dropChance = p_406660_.getFloatOr("ItemDropChance", 1.0F);
      this.setDirection(p_406660_.<Direction>read("Facing", Direction.LEGACY_ID_CODEC).orElse(Direction.DOWN));
      this.setInvisible(p_406660_.getBooleanOr("Invisible", false));
      this.fixed = p_406660_.getBooleanOr("Fixed", false);
   }

   @Override
   public InteractionResult interact(Player p_31787_, InteractionHand p_31788_) {
      ItemStack itemstack = p_31787_.getItemInHand(p_31788_);
      boolean flag = !this.getItem().isEmpty();
      boolean flag1 = !itemstack.isEmpty();
      if (this.fixed) {
         return InteractionResult.PASS;
      }

      if (!p_31787_.level().isClientSide()) {
         if (!flag) {
            if (flag1 && !this.isRemoved()) {
               MapItemSavedData mapitemsaveddata = MapItem.getSavedData(itemstack, this.level());
               if (mapitemsaveddata != null && mapitemsaveddata.isTrackedCountOverLimit(256)) {
                  return InteractionResult.FAIL;
               }

               this.setItem(itemstack);
               this.gameEvent(GameEvent.BLOCK_CHANGE, p_31787_);
               itemstack.consume(1, p_31787_);
               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.PASS;
            }
         } else {
            this.playSound(this.getRotateItemSound(), 1.0F, 1.0F);
            this.setRotation(this.getRotation() + 1);
            this.gameEvent(GameEvent.BLOCK_CHANGE, p_31787_);
            return InteractionResult.SUCCESS;
         }
      } else {
         return (InteractionResult)(!flag && !flag1 ? InteractionResult.PASS : InteractionResult.SUCCESS);
      }
   }

   public SoundEvent getRotateItemSound() {
      return SoundEvents.ITEM_FRAME_ROTATE_ITEM;
   }

   public int getAnalogOutput() {
      return this.getItem().isEmpty() ? 0 : this.getRotation() % 8 + 1;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_343038_) {
      return new ClientboundAddEntityPacket(this, this.getDirection().get3DDataValue(), this.getPos());
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket p_149626_) {
      super.recreateFromPacket(p_149626_);
      this.setDirection(Direction.from3DDataValue(p_149626_.getData()));
   }

   @Override
   public ItemStack getPickResult() {
      ItemStack itemstack = this.getItem();
      return itemstack.isEmpty() ? this.getFrameItemStack() : itemstack.copy();
   }

   protected ItemStack getFrameItemStack() {
      return new ItemStack(Items.ITEM_FRAME);
   }

   @Override
   public float getVisualRotationYInDegrees() {
      Direction direction = this.getDirection();
      int i = direction.getAxis().isVertical() ? 90 * direction.getAxisDirection().getStep() : 0;
      return Mth.wrapDegrees(180 + direction.get2DDataValue() * 90 + this.getRotation() * 45 + i);
   }
}
