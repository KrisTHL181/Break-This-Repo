package net.minecraft.world.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Mannequin extends Avatar {
   protected static final EntityDataAccessor<ResolvableProfile> DATA_PROFILE = SynchedEntityData.defineId(
      Mannequin.class, EntityDataSerializers.RESOLVABLE_PROFILE
   );
   private static final EntityDataAccessor<Boolean> DATA_IMMOVABLE = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Optional<Component>> DATA_DESCRIPTION = SynchedEntityData.defineId(
      Mannequin.class, EntityDataSerializers.OPTIONAL_COMPONENT
   );
   private static final byte ALL_LAYERS = (byte)Arrays.stream(PlayerModelPart.values()).mapToInt(PlayerModelPart::getMask).reduce(0, (a, b) -> a | b);
   private static final Set<Pose> VALID_POSES = Set.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING, Pose.FALL_FLYING, Pose.SLEEPING);
   public static final Codec<Pose> POSE_CODEC = Pose.CODEC
      .validate(pose -> VALID_POSES.contains(pose) ? DataResult.success(pose) : DataResult.error(() -> "Invalid pose: " + pose.getSerializedName()));
   private static final Codec<Byte> LAYERS_CODEC = PlayerModelPart.CODEC
      .listOf()
      .xmap(
         list -> (byte)list.stream().mapToInt(PlayerModelPart::getMask).reduce(ALL_LAYERS, (a, b) -> a & ~b),
         mask -> Arrays.stream(PlayerModelPart.values()).filter(part -> (mask & part.getMask()) == 0).toList()
      );
   public static final ResolvableProfile DEFAULT_PROFILE = ResolvableProfile.Static.EMPTY;
   private static final Component DEFAULT_DESCRIPTION = Component.translatable("entity.minecraft.mannequin.label");
   protected static EntityType.EntityFactory<Mannequin> constructor = Mannequin::new;
   private static final String PROFILE_FIELD = "profile";
   private static final String HIDDEN_LAYERS_FIELD = "hidden_layers";
   private static final String MAIN_HAND_FIELD = "main_hand";
   private static final String POSE_FIELD = "pose";
   private static final String IMMOVABLE_FIELD = "immovable";
   private static final String DESCRIPTION_FIELD = "description";
   private static final String HIDE_DESCRIPTION_FIELD = "hide_description";
   private Component description = DEFAULT_DESCRIPTION;
   private boolean hideDescription = false;

   public Mannequin(final EntityType<Mannequin> type, final Level level) {
      super(type, level);
      this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, ALL_LAYERS);
   }

   protected Mannequin(final Level level) {
      this(EntityTypes.MANNEQUIN, level);
   }

   public static @Nullable Mannequin create(final EntityType<Mannequin> type, final Level level) {
      return constructor.create(type, level);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      super.defineSynchedData(entityData);
      entityData.define(DATA_PROFILE, DEFAULT_PROFILE);
      entityData.define(DATA_IMMOVABLE, false);
      entityData.define(DATA_DESCRIPTION, Optional.of(DEFAULT_DESCRIPTION));
   }

   @Override
   public ResolvableProfile getProfile() {
      return this.entityData.get(DATA_PROFILE);
   }

   private void setProfile(final ResolvableProfile profile) {
      this.entityData.set(DATA_PROFILE, profile);
   }

   private boolean getImmovable() {
      return this.entityData.get(DATA_IMMOVABLE);
   }

   private void setImmovable(final boolean immovable) {
      this.entityData.set(DATA_IMMOVABLE, immovable);
   }

   protected @Nullable Component getDescription() {
      return this.entityData.get(DATA_DESCRIPTION).orElse(null);
   }

   private void setDescription(final Component description) {
      this.description = description;
      this.updateDescription();
   }

   private void setHideDescription(final boolean hideDescription) {
      this.hideDescription = hideDescription;
      this.updateDescription();
   }

   private void updateDescription() {
      this.entityData.set(DATA_DESCRIPTION, this.hideDescription ? Optional.empty() : Optional.of(this.description));
   }

   @Override
   protected boolean isImmobile() {
      return this.getImmovable() || super.isImmobile();
   }

   @Override
   public boolean isEffectiveAi() {
      return !this.getImmovable() && super.isEffectiveAi();
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.store("profile", ResolvableProfile.CODEC, this.getProfile());
      output.store("hidden_layers", LAYERS_CODEC, this.entityData.get(DATA_PLAYER_MODE_CUSTOMISATION));
      output.store("main_hand", HumanoidArm.CODEC, this.getMainArm());
      output.store("pose", POSE_CODEC, this.getPose());
      output.putBoolean("immovable", this.getImmovable());
      Component description = this.getDescription();
      if (description != null) {
         if (!description.equals(DEFAULT_DESCRIPTION)) {
            output.store("description", ComponentSerialization.CODEC, description);
         }
      } else {
         output.putBoolean("hide_description", true);
      }
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      super.readAdditionalSaveData(input);
      input.<ResolvableProfile>read("profile", ResolvableProfile.CODEC).ifPresent(this::setProfile);
      this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, input.<Byte>read("hidden_layers", LAYERS_CODEC).orElse(ALL_LAYERS));
      this.setMainArm(input.<HumanoidArm>read("main_hand", HumanoidArm.CODEC).orElse(DEFAULT_MAIN_HAND));
      this.setPose(input.<Pose>read("pose", POSE_CODEC).orElse(Pose.STANDING));
      this.setImmovable(input.getBooleanOr("immovable", false));
      this.setHideDescription(input.getBooleanOr("hide_description", false));
      this.setDescription(input.<Component>read("description", ComponentSerialization.CODEC).orElse(DEFAULT_DESCRIPTION));
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      return type == DataComponents.PROFILE ? castComponentValue((DataComponentType<T>)type, this.getProfile()) : super.get(type);
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.PROFILE);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.PROFILE) {
         this.setProfile(castComponentValue(DataComponents.PROFILE, value));
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }

   @Override
   public void aiStep() {
      super.aiStep();
      this.updateSwingTime();
   }
}
