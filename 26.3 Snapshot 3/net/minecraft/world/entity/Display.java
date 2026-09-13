package net.minecraft.world.entity;

import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ARGB;
import net.minecraft.util.Brightness;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Display extends Entity {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int NO_BRIGHTNESS_OVERRIDE = -1;
   private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID = SynchedEntityData.defineId(
      Display.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(
      Display.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Integer> DATA_POS_ROT_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(
      Display.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Vector3fc> DATA_TRANSLATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
   private static final EntityDataAccessor<Vector3fc> DATA_SCALE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
   private static final EntityDataAccessor<Quaternionfc> DATA_LEFT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
   private static final EntityDataAccessor<Quaternionfc> DATA_RIGHT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
   private static final EntityDataAccessor<Byte> DATA_BILLBOARD_RENDER_CONSTRAINTS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Integer> DATA_BRIGHTNESS_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_VIEW_RANGE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_SHADOW_RADIUS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_SHADOW_STRENGTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_WIDTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_HEIGHT_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_GLOW_COLOR_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
   private static final IntSet RENDER_STATE_IDS = IntSet.of(
      new int[]{
         DATA_TRANSLATION_ID.id(),
         DATA_SCALE_ID.id(),
         DATA_LEFT_ROTATION_ID.id(),
         DATA_RIGHT_ROTATION_ID.id(),
         DATA_BILLBOARD_RENDER_CONSTRAINTS_ID.id(),
         DATA_BRIGHTNESS_OVERRIDE_ID.id(),
         DATA_SHADOW_RADIUS_ID.id(),
         DATA_SHADOW_STRENGTH_ID.id()
      }
   );
   private static final int INITIAL_TRANSFORMATION_INTERPOLATION_DURATION = 0;
   private static final int INITIAL_TRANSFORMATION_START_INTERPOLATION = 0;
   private static final int INITIAL_POS_ROT_INTERPOLATION_DURATION = 0;
   private static final float INITIAL_SHADOW_RADIUS = 0.0F;
   private static final float INITIAL_SHADOW_STRENGTH = 1.0F;
   private static final float INITIAL_VIEW_RANGE = 1.0F;
   private static final float INITIAL_WIDTH = 0.0F;
   private static final float INITIAL_HEIGHT = 0.0F;
   private static final int NO_GLOW_COLOR_OVERRIDE = -1;
   public static final String TAG_POS_ROT_INTERPOLATION_DURATION = "teleport_duration";
   public static final String TAG_TRANSFORMATION_INTERPOLATION_DURATION = "interpolation_duration";
   public static final String TAG_TRANSFORMATION_START_INTERPOLATION = "start_interpolation";
   public static final String TAG_TRANSFORMATION = "transformation";
   public static final String TAG_BILLBOARD = "billboard";
   public static final String TAG_BRIGHTNESS = "brightness";
   public static final String TAG_VIEW_RANGE = "view_range";
   public static final String TAG_SHADOW_RADIUS = "shadow_radius";
   public static final String TAG_SHADOW_STRENGTH = "shadow_strength";
   public static final String TAG_WIDTH = "width";
   public static final String TAG_HEIGHT = "height";
   public static final String TAG_GLOW_COLOR_OVERRIDE = "glow_color_override";
   private long interpolationStartClientTick = -2147483648L;
   private int interpolationDuration;
   private float lastProgress;
   private AABB cullingBoundingBox;
   private boolean noCulling = true;
   protected boolean updateRenderState;
   private boolean updateStartTick;
   private boolean updateInterpolationDuration;
   private Display.@Nullable RenderState renderState;
   private final LinearInterpolationHandler interpolation = new LinearInterpolationHandler(this, 0);

   public Display(final EntityType<?> type, final Level level) {
      super(type, level);
      this.noPhysics = true;
      this.cullingBoundingBox = this.getBoundingBox();
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      super.onSyncedDataUpdated(accessor);
      if (DATA_HEIGHT_ID.equals(accessor) || DATA_WIDTH_ID.equals(accessor)) {
         this.updateCulling();
      }

      if (DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID.equals(accessor)) {
         this.updateStartTick = true;
      }

      if (DATA_POS_ROT_INTERPOLATION_DURATION_ID.equals(accessor)) {
         this.interpolation.setInterpolationLength(this.getPosRotInterpolationDuration());
      }

      if (DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID.equals(accessor)) {
         this.updateInterpolationDuration = true;
      }

      if (RENDER_STATE_IDS.contains(accessor.id())) {
         this.updateRenderState = true;
      }
   }

   @Override
   public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }

   private static Transformation createTransformation(final SynchedEntityData entityData) {
      Vector3fc translation = entityData.get(DATA_TRANSLATION_ID);
      Quaternionfc leftRotation = entityData.get(DATA_LEFT_ROTATION_ID);
      Vector3fc scale = entityData.get(DATA_SCALE_ID);
      Quaternionfc rightRotation = entityData.get(DATA_RIGHT_ROTATION_ID);
      return new Transformation(translation, leftRotation, scale, rightRotation);
   }

   @Override
   public void tick() {
      Entity vehicle = this.getVehicle();
      if (vehicle != null && vehicle.isRemoved()) {
         this.stopRiding();
      }

      if (this.level().isClientSide()) {
         if (this.updateStartTick) {
            this.updateStartTick = false;
            int interpolationStartDelta = this.getTransformationInterpolationDelay();
            this.interpolationStartClientTick = this.tickCount + interpolationStartDelta;
         }

         if (this.updateInterpolationDuration) {
            this.updateInterpolationDuration = false;
            this.interpolationDuration = this.getTransformationInterpolationDuration();
         }

         if (this.updateRenderState) {
            this.updateRenderState = false;
            boolean shouldInterpolate = this.interpolationDuration != 0;
            if (shouldInterpolate && this.renderState != null) {
               this.renderState = this.createInterpolatedRenderState(this.renderState, this.lastProgress);
            } else {
               this.renderState = this.createFreshRenderState();
            }

            this.updateRenderSubState(shouldInterpolate, this.lastProgress);
         }

         this.interpolation.interpolate();
      }
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return this.interpolation;
   }

   protected abstract void updateRenderSubState(boolean shouldInterpolate, float progress);

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 0);
      entityData.define(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0);
      entityData.define(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 0);
      entityData.define(DATA_TRANSLATION_ID, new Vector3f());
      entityData.define(DATA_SCALE_ID, new Vector3f(1.0F, 1.0F, 1.0F));
      entityData.define(DATA_RIGHT_ROTATION_ID, new Quaternionf());
      entityData.define(DATA_LEFT_ROTATION_ID, new Quaternionf());
      entityData.define(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, Display.BillboardConstraints.FIXED.getId());
      entityData.define(DATA_BRIGHTNESS_OVERRIDE_ID, -1);
      entityData.define(DATA_VIEW_RANGE_ID, 1.0F);
      entityData.define(DATA_SHADOW_RADIUS_ID, 0.0F);
      entityData.define(DATA_SHADOW_STRENGTH_ID, 1.0F);
      entityData.define(DATA_WIDTH_ID, 0.0F);
      entityData.define(DATA_HEIGHT_ID, 0.0F);
      entityData.define(DATA_GLOW_COLOR_OVERRIDE_ID, -1);
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.setTransformation(input.<Transformation>read("transformation", Transformation.EXTENDED_CODEC).orElse(Transformation.IDENTITY));
      this.setTransformationInterpolationDuration(input.getIntOr("interpolation_duration", 0));
      this.setTransformationInterpolationDelay(input.getIntOr("start_interpolation", 0));
      int teleportDuration = input.getIntOr("teleport_duration", 0);
      this.setPosRotInterpolationDuration(Mth.clamp(teleportDuration, 0, 59));
      this.setBillboardConstraints(
         input.<Display.BillboardConstraints>read("billboard", Display.BillboardConstraints.CODEC).orElse(Display.BillboardConstraints.FIXED)
      );
      this.setViewRange(input.getFloatOr("view_range", 1.0F));
      this.setShadowRadius(input.getFloatOr("shadow_radius", 0.0F));
      this.setShadowStrength(input.getFloatOr("shadow_strength", 1.0F));
      this.setWidth(input.getFloatOr("width", 0.0F));
      this.setHeight(input.getFloatOr("height", 0.0F));
      this.setGlowColorOverride(input.getIntOr("glow_color_override", -1));
      this.setBrightnessOverride(input.<Brightness>read("brightness", Brightness.CODEC).orElse(null));
   }

   private void setTransformation(final Transformation transformation) {
      this.entityData.set(DATA_TRANSLATION_ID, transformation.translation());
      this.entityData.set(DATA_LEFT_ROTATION_ID, transformation.leftRotation());
      this.entityData.set(DATA_SCALE_ID, transformation.scale());
      this.entityData.set(DATA_RIGHT_ROTATION_ID, transformation.rightRotation());
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      output.store("transformation", Transformation.EXTENDED_CODEC, createTransformation(this.entityData));
      output.store("billboard", Display.BillboardConstraints.CODEC, this.getBillboardConstraints());
      output.putInt("interpolation_duration", this.getTransformationInterpolationDuration());
      output.putInt("teleport_duration", this.getPosRotInterpolationDuration());
      output.putFloat("view_range", this.getViewRange());
      output.putFloat("shadow_radius", this.getShadowRadius());
      output.putFloat("shadow_strength", this.getShadowStrength());
      output.putFloat("width", this.getWidth());
      output.putFloat("height", this.getHeight());
      output.putInt("glow_color_override", this.getGlowColorOverride());
      output.storeNullable("brightness", Brightness.CODEC, this.getBrightnessOverride());
   }

   public AABB getBoundingBoxForCulling() {
      return this.cullingBoundingBox;
   }

   public boolean affectedByCulling() {
      return !this.noCulling;
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   public Display.@Nullable RenderState renderState() {
      return this.renderState;
   }

   private void setTransformationInterpolationDuration(final int duration) {
      this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, duration);
   }

   private int getTransformationInterpolationDuration() {
      return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID);
   }

   private void setTransformationInterpolationDelay(final int ticks) {
      this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, ticks, true);
   }

   private int getTransformationInterpolationDelay() {
      return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID);
   }

   private void setPosRotInterpolationDuration(final int duration) {
      this.entityData.set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, duration);
   }

   private int getPosRotInterpolationDuration() {
      return this.entityData.get(DATA_POS_ROT_INTERPOLATION_DURATION_ID);
   }

   private void setBillboardConstraints(final Display.BillboardConstraints constraints) {
      this.entityData.set(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, constraints.getId());
   }

   private Display.BillboardConstraints getBillboardConstraints() {
      return Display.BillboardConstraints.BY_ID.apply(this.entityData.get(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID));
   }

   private void setBrightnessOverride(final @Nullable Brightness brightness) {
      this.entityData.set(DATA_BRIGHTNESS_OVERRIDE_ID, brightness != null ? brightness.pack() : -1);
   }

   private @Nullable Brightness getBrightnessOverride() {
      int value = this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
      return value != -1 ? Brightness.unpack(value) : null;
   }

   private int getPackedBrightnessOverride() {
      return this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
   }

   private void setViewRange(final float range) {
      this.entityData.set(DATA_VIEW_RANGE_ID, range);
   }

   private float getViewRange() {
      return this.entityData.get(DATA_VIEW_RANGE_ID);
   }

   private void setShadowRadius(final float size) {
      this.entityData.set(DATA_SHADOW_RADIUS_ID, size);
   }

   private float getShadowRadius() {
      return this.entityData.get(DATA_SHADOW_RADIUS_ID);
   }

   private void setShadowStrength(final float strength) {
      this.entityData.set(DATA_SHADOW_STRENGTH_ID, strength);
   }

   private float getShadowStrength() {
      return this.entityData.get(DATA_SHADOW_STRENGTH_ID);
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

   private int getGlowColorOverride() {
      return this.entityData.get(DATA_GLOW_COLOR_OVERRIDE_ID);
   }

   private void setGlowColorOverride(final int value) {
      this.entityData.set(DATA_GLOW_COLOR_OVERRIDE_ID, value);
   }

   public float calculateInterpolationProgress(final float partialTickTime) {
      int duration = this.interpolationDuration;
      if (duration <= 0) {
         return 1.0F;
      }

      float ticksSinceUpdate = (float)(this.tickCount - this.interpolationStartClientTick);
      float partialTicksSinceLastUpdate = ticksSinceUpdate + partialTickTime;
      float result = Mth.clamp(Mth.inverseLerp(partialTicksSinceLastUpdate, 0.0F, duration), 0.0F, 1.0F);
      this.lastProgress = result;
      return result;
   }

   private float getHeight() {
      return this.entityData.get(DATA_HEIGHT_ID);
   }

   @Override
   public void setPos(final double x, final double y, final double z) {
      super.setPos(x, y, z);
      this.updateCulling();
   }

   private void updateCulling() {
      float width = this.getWidth();
      float height = this.getHeight();
      this.noCulling = width == 0.0F || height == 0.0F;
      float w = width / 2.0F;
      double x = this.getX();
      double y = this.getY();
      double z = this.getZ();
      this.cullingBoundingBox = new AABB(x - w, y, z - w, x + w, y + height, z + w);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(final double distanceSqr) {
      return distanceSqr < Mth.square(this.getViewRange() * 64.0 * getViewScale());
   }

   @Override
   public int getTeamColor() {
      int glowColorOverride = this.getGlowColorOverride();
      return glowColorOverride != -1 ? glowColorOverride : super.getTeamColor();
   }

   private Display.RenderState createFreshRenderState() {
      return new Display.RenderState(
         Display.GenericInterpolator.constant(createTransformation(this.entityData)),
         this.getBillboardConstraints(),
         this.getPackedBrightnessOverride(),
         Display.FloatInterpolator.constant(this.getShadowRadius()),
         Display.FloatInterpolator.constant(this.getShadowStrength()),
         this.getGlowColorOverride()
      );
   }

   private Display.RenderState createInterpolatedRenderState(final Display.RenderState previousState, final float progress) {
      Transformation currentTransform = previousState.transformation.get(progress);
      float currentShadowRadius = previousState.shadowRadius.get(progress);
      float currentShadowStrength = previousState.shadowStrength.get(progress);
      return new Display.RenderState(
         new Display.TransformationInterpolator(currentTransform, createTransformation(this.entityData)),
         this.getBillboardConstraints(),
         this.getPackedBrightnessOverride(),
         new Display.LinearFloatInterpolator(currentShadowRadius, this.getShadowRadius()),
         new Display.LinearFloatInterpolator(currentShadowStrength, this.getShadowStrength()),
         this.getGlowColorOverride()
      );
   }

   public enum BillboardConstraints implements StringRepresentable {
      FIXED((byte)0, "fixed"),
      VERTICAL((byte)1, "vertical"),
      HORIZONTAL((byte)2, "horizontal"),
      CENTER((byte)3, "center");

      public static final Codec<Display.BillboardConstraints> CODEC = StringRepresentable.fromEnum(Display.BillboardConstraints::values);
      public static final IntFunction<Display.BillboardConstraints> BY_ID = ByIdMap.continuous(
         Display.BillboardConstraints::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      private final byte id;
      private final String name;

      BillboardConstraints(final byte id, final String name) {
         this.name = name;
         this.id = id;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      private byte getId() {
         return this.id;
      }
   }

   public static class BlockDisplay extends Display {
      public static final String TAG_BLOCK_STATE = "block_state";
      private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(
         Display.BlockDisplay.class, EntityDataSerializers.BLOCK_STATE
      );
      private Display.BlockDisplay.@Nullable BlockRenderState blockRenderState;

      public BlockDisplay(final EntityType<?> type, final Level level) {
         super(type, level);
      }

      @Override
      protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
         super.defineSynchedData(entityData);
         entityData.define(DATA_BLOCK_STATE_ID, Blocks.AIR.defaultBlockState());
      }

      @Override
      public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
         super.onSyncedDataUpdated(accessor);
         if (accessor.equals(DATA_BLOCK_STATE_ID)) {
            this.updateRenderState = true;
         }
      }

      private BlockState getBlockState() {
         return this.entityData.get(DATA_BLOCK_STATE_ID);
      }

      private void setBlockState(final BlockState blockState) {
         this.entityData.set(DATA_BLOCK_STATE_ID, blockState);
      }

      @Override
      protected void readAdditionalSaveData(final ValueInput input) {
         super.readAdditionalSaveData(input);
         this.setBlockState(input.<BlockState>read("block_state", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState()));
      }

      @Override
      protected void addAdditionalSaveData(final ValueOutput output) {
         super.addAdditionalSaveData(output);
         output.store("block_state", BlockState.CODEC, this.getBlockState());
      }

      public Display.BlockDisplay.@Nullable BlockRenderState blockRenderState() {
         return this.blockRenderState;
      }

      @Override
      protected void updateRenderSubState(final boolean shouldInterpolate, final float progress) {
         this.blockRenderState = new Display.BlockDisplay.BlockRenderState(this.getBlockState());
      }

      public record BlockRenderState(BlockState blockState) {
      }
   }

   private record ColorInterpolator(int previous, int current) implements Display.IntInterpolator {
      @Override
      public int get(final float progress) {
         return ARGB.srgbLerp(progress, this.previous, this.current);
      }
   }

   @FunctionalInterface
   public interface FloatInterpolator {
      static Display.FloatInterpolator constant(final float value) {
         return progress -> value;
      }

      float get(final float progress);
   }

   @FunctionalInterface
   public interface GenericInterpolator<T> {
      static <T> Display.GenericInterpolator<T> constant(final T value) {
         return progress -> value;
      }

      T get(final float progress);
   }

   @FunctionalInterface
   public interface IntInterpolator {
      static Display.IntInterpolator constant(final int value) {
         return progress -> value;
      }

      int get(final float progress);
   }

   public static class ItemDisplay extends Display {
      private static final String TAG_ITEM = "item";
      private static final String TAG_ITEM_DISPLAY = "item_display";
      private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(
         Display.ItemDisplay.class, EntityDataSerializers.ITEM_STACK
      );
      private static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(Display.ItemDisplay.class, EntityDataSerializers.BYTE);
      private final SlotAccess slot = SlotAccess.of(this::getItemStack, this::setItemStack);
      private Display.ItemDisplay.@Nullable ItemRenderState itemRenderState;

      public ItemDisplay(final EntityType<?> type, final Level level) {
         super(type, level);
      }

      @Override
      protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
         super.defineSynchedData(entityData);
         entityData.define(DATA_ITEM_STACK_ID, ItemStack.EMPTY);
         entityData.define(DATA_ITEM_DISPLAY_ID, ItemDisplayContext.NONE.getId());
      }

      @Override
      public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
         super.onSyncedDataUpdated(accessor);
         if (DATA_ITEM_STACK_ID.equals(accessor) || DATA_ITEM_DISPLAY_ID.equals(accessor)) {
            this.updateRenderState = true;
         }
      }

      private ItemStack getItemStack() {
         return this.entityData.get(DATA_ITEM_STACK_ID);
      }

      private void setItemStack(final ItemStack item) {
         this.entityData.set(DATA_ITEM_STACK_ID, item);
      }

      private void setItemTransform(final ItemDisplayContext transform) {
         this.entityData.set(DATA_ITEM_DISPLAY_ID, transform.getId());
      }

      private ItemDisplayContext getItemTransform() {
         return ItemDisplayContext.BY_ID.apply(this.entityData.get(DATA_ITEM_DISPLAY_ID));
      }

      @Override
      protected void readAdditionalSaveData(final ValueInput input) {
         super.readAdditionalSaveData(input);
         this.setItemStack(input.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
         this.setItemTransform(input.<ItemDisplayContext>read("item_display", ItemDisplayContext.CODEC).orElse(ItemDisplayContext.NONE));
      }

      @Override
      protected void addAdditionalSaveData(final ValueOutput output) {
         super.addAdditionalSaveData(output);
         ItemStack itemStack = this.getItemStack();
         if (!itemStack.isEmpty()) {
            output.store("item", ItemStack.CODEC, itemStack);
         }

         output.store("item_display", ItemDisplayContext.CODEC, this.getItemTransform());
      }

      @Override
      public @Nullable SlotAccess getSlot(final int slot) {
         return slot == 0 ? this.slot : null;
      }

      public Display.ItemDisplay.@Nullable ItemRenderState itemRenderState() {
         return this.itemRenderState;
      }

      @Override
      protected void updateRenderSubState(final boolean shouldInterpolate, final float progress) {
         this.itemRenderState = new Display.ItemDisplay.ItemRenderState(this.getItemStack(), this.getItemTransform());
      }

      public record ItemRenderState(ItemStack itemStack, ItemDisplayContext itemTransform) {
      }
   }

   private record LinearFloatInterpolator(float previous, float current) implements Display.FloatInterpolator {
      @Override
      public float get(final float progress) {
         return Mth.lerp(progress, this.previous, this.current);
      }
   }

   private record LinearIntInterpolator(int previous, int current) implements Display.IntInterpolator {
      @Override
      public int get(final float progress) {
         return Mth.lerpInt(progress, this.previous, this.current);
      }
   }

   public record RenderState(
      Display.GenericInterpolator<Transformation> transformation,
      Display.BillboardConstraints billboardConstraints,
      int brightnessOverride,
      Display.FloatInterpolator shadowRadius,
      Display.FloatInterpolator shadowStrength,
      int glowColorOverride
   ) {
   }

   public static class TextDisplay extends Display {
      public static final String TAG_TEXT = "text";
      private static final String TAG_LINE_WIDTH = "line_width";
      private static final String TAG_TEXT_OPACITY = "text_opacity";
      private static final String TAG_BACKGROUND_COLOR = "background";
      private static final String TAG_SHADOW = "shadow";
      private static final String TAG_SEE_THROUGH = "see_through";
      private static final String TAG_USE_DEFAULT_BACKGROUND = "default_background";
      private static final String TAG_ALIGNMENT = "alignment";
      public static final byte FLAG_SHADOW = 1;
      public static final byte FLAG_SEE_THROUGH = 2;
      public static final byte FLAG_USE_DEFAULT_BACKGROUND = 4;
      public static final byte FLAG_ALIGN_LEFT = 8;
      public static final byte FLAG_ALIGN_RIGHT = 16;
      private static final byte INITIAL_TEXT_OPACITY = -1;
      public static final int INITIAL_BACKGROUND = 1073741824;
      private static final int INITIAL_LINE_WIDTH = 200;
      private static final EntityDataAccessor<Component> DATA_TEXT_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.COMPONENT);
      private static final EntityDataAccessor<Integer> DATA_LINE_WIDTH_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.INT);
      private static final EntityDataAccessor<Integer> DATA_BACKGROUND_COLOR_ID = SynchedEntityData.defineId(
         Display.TextDisplay.class, EntityDataSerializers.INT
      );
      private static final EntityDataAccessor<Byte> DATA_TEXT_OPACITY_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.BYTE);
      private static final EntityDataAccessor<Byte> DATA_STYLE_FLAGS_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.BYTE);
      private static final IntSet TEXT_RENDER_STATE_IDS = IntSet.of(
         new int[]{DATA_TEXT_ID.id(), DATA_LINE_WIDTH_ID.id(), DATA_BACKGROUND_COLOR_ID.id(), DATA_TEXT_OPACITY_ID.id(), DATA_STYLE_FLAGS_ID.id()}
      );
      private Display.TextDisplay.@Nullable CachedInfo clientDisplayCache;
      private Display.TextDisplay.@Nullable TextRenderState textRenderState;

      public TextDisplay(final EntityType<?> type, final Level level) {
         super(type, level);
      }

      @Override
      protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
         super.defineSynchedData(entityData);
         entityData.define(DATA_TEXT_ID, Component.empty());
         entityData.define(DATA_LINE_WIDTH_ID, 200);
         entityData.define(DATA_BACKGROUND_COLOR_ID, 1073741824);
         entityData.define(DATA_TEXT_OPACITY_ID, (byte)-1);
         entityData.define(DATA_STYLE_FLAGS_ID, (byte)0);
      }

      @Override
      public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
         super.onSyncedDataUpdated(accessor);
         if (TEXT_RENDER_STATE_IDS.contains(accessor.id())) {
            this.updateRenderState = true;
         }
      }

      private Component getText() {
         return this.entityData.get(DATA_TEXT_ID);
      }

      private void setText(final Component text) {
         this.entityData.set(DATA_TEXT_ID, text);
      }

      private int getLineWidth() {
         return this.entityData.get(DATA_LINE_WIDTH_ID);
      }

      private void setLineWidth(final int width) {
         this.entityData.set(DATA_LINE_WIDTH_ID, width);
      }

      private byte getTextOpacity() {
         return this.entityData.get(DATA_TEXT_OPACITY_ID);
      }

      private void setTextOpacity(final byte opacity) {
         this.entityData.set(DATA_TEXT_OPACITY_ID, opacity);
      }

      private int getBackgroundColor() {
         return this.entityData.get(DATA_BACKGROUND_COLOR_ID);
      }

      private void setBackgroundColor(final int color) {
         this.entityData.set(DATA_BACKGROUND_COLOR_ID, color);
      }

      private byte getFlags() {
         return this.entityData.get(DATA_STYLE_FLAGS_ID);
      }

      private void setFlags(final byte flags) {
         this.entityData.set(DATA_STYLE_FLAGS_ID, flags);
      }

      private static byte loadFlag(final byte flags, final ValueInput input, final String id, final byte mask) {
         return input.getBooleanOr(id, false) ? (byte)(flags | mask) : flags;
      }

      @Override
      protected void readAdditionalSaveData(final ValueInput input) {
         super.readAdditionalSaveData(input);
         this.setLineWidth(input.getIntOr("line_width", 200));
         this.setTextOpacity(input.getByteOr("text_opacity", (byte)-1));
         this.setBackgroundColor(input.getIntOr("background", 1073741824));
         byte flags = loadFlag((byte)0, input, "shadow", (byte)1);
         flags = loadFlag(flags, input, "see_through", (byte)2);
         flags = loadFlag(flags, input, "default_background", (byte)4);
         Optional<Display.TextDisplay.Align> alignment = input.read("alignment", Display.TextDisplay.Align.CODEC);
         if (alignment.isPresent()) {
            flags = switch ((Display.TextDisplay.Align)alignment.get()) {
               case CENTER -> flags;
               case LEFT -> (byte)(flags | 8);
               case RIGHT -> (byte)(flags | 16);
            };
         }

         this.setFlags(flags);
         Optional<Component> text = input.read("text", ComponentSerialization.CODEC);
         if (text.isPresent()) {
            try {
               if (this.level() instanceof ServerLevel serverLevel) {
                  CommandSourceStack context = this.createCommandSourceStackForNameResolution(serverLevel).withPermission(LevelBasedPermissionSet.GAMEMASTER);
                  Component resolvedText = ComponentUtils.resolve(ResolutionContext.create(context), text.get());
                  this.setText(resolvedText);
               } else {
                  this.setText(Component.empty());
               }
            } catch (Exception e) {
               Display.LOGGER.warn("Failed to parse display entity text {}", text, e);
            }
         }
      }

      private static void storeFlag(final byte flags, final ValueOutput output, final String id, final byte mask) {
         output.putBoolean(id, (flags & mask) != 0);
      }

      @Override
      protected void addAdditionalSaveData(final ValueOutput output) {
         super.addAdditionalSaveData(output);
         output.store("text", ComponentSerialization.CODEC, this.getText());
         output.putInt("line_width", this.getLineWidth());
         output.putInt("background", this.getBackgroundColor());
         output.putByte("text_opacity", this.getTextOpacity());
         byte flags = this.getFlags();
         storeFlag(flags, output, "shadow", (byte)1);
         storeFlag(flags, output, "see_through", (byte)2);
         storeFlag(flags, output, "default_background", (byte)4);
         output.store("alignment", Display.TextDisplay.Align.CODEC, getAlign(flags));
      }

      @Override
      protected void updateRenderSubState(final boolean shouldInterpolate, final float progress) {
         if (shouldInterpolate && this.textRenderState != null) {
            this.textRenderState = this.createInterpolatedTextRenderState(this.textRenderState, progress);
         } else {
            this.textRenderState = this.createFreshTextRenderState();
         }

         this.clientDisplayCache = null;
      }

      public Display.TextDisplay.@Nullable TextRenderState textRenderState() {
         return this.textRenderState;
      }

      private Display.TextDisplay.TextRenderState createFreshTextRenderState() {
         return new Display.TextDisplay.TextRenderState(
            this.getText(),
            this.getLineWidth(),
            Display.IntInterpolator.constant(this.getTextOpacity()),
            Display.IntInterpolator.constant(this.getBackgroundColor()),
            this.getFlags()
         );
      }

      private Display.TextDisplay.TextRenderState createInterpolatedTextRenderState(final Display.TextDisplay.TextRenderState previous, final float progress) {
         int currentBackground = previous.backgroundColor.get(progress);
         int currentOpacity = previous.textOpacity.get(progress);
         return new Display.TextDisplay.TextRenderState(
            this.getText(),
            this.getLineWidth(),
            new Display.LinearIntInterpolator(currentOpacity, this.getTextOpacity()),
            new Display.ColorInterpolator(currentBackground, this.getBackgroundColor()),
            this.getFlags()
         );
      }

      public Display.TextDisplay.CachedInfo cacheDisplay(final Display.TextDisplay.LineSplitter splitter) {
         if (this.clientDisplayCache == null) {
            if (this.textRenderState != null) {
               this.clientDisplayCache = splitter.split(this.textRenderState.text(), this.textRenderState.lineWidth());
            } else {
               this.clientDisplayCache = new Display.TextDisplay.CachedInfo(List.of(), 0);
            }
         }

         return this.clientDisplayCache;
      }

      public static Display.TextDisplay.Align getAlign(final byte flags) {
         if ((flags & 8) != 0) {
            return Display.TextDisplay.Align.LEFT;
         } else {
            return (flags & 16) != 0 ? Display.TextDisplay.Align.RIGHT : Display.TextDisplay.Align.CENTER;
         }
      }

      public enum Align implements StringRepresentable {
         CENTER("center"),
         LEFT("left"),
         RIGHT("right");

         public static final Codec<Display.TextDisplay.Align> CODEC = StringRepresentable.fromEnum(Display.TextDisplay.Align::values);
         private final String name;

         Align(final String name) {
            this.name = name;
         }

         @Override
         public String getSerializedName() {
            return this.name;
         }
      }

      public record CachedInfo(List<Display.TextDisplay.CachedLine> lines, int width) {
      }

      public record CachedLine(FormattedCharSequence contents, int width) {
      }

      @FunctionalInterface
      public interface LineSplitter {
         Display.TextDisplay.CachedInfo split(Component input, int width);
      }

      public record TextRenderState(Component text, int lineWidth, Display.IntInterpolator textOpacity, Display.IntInterpolator backgroundColor, byte flags) {
      }
   }

   private record TransformationInterpolator(Transformation previous, Transformation current) implements Display.GenericInterpolator<Transformation> {
      public Transformation get(final float progress) {
         return progress >= 1.0 ? this.current : this.previous.slerp(this.current, progress);
      }
   }
}
