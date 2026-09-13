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
   private static final byte ALL_LAYERS = (byte)Arrays.stream(PlayerModelPart.values())
      .mapToInt(PlayerModelPart::getMask)
      .reduce(0, (p_431306_, p_428431_) -> p_431306_ | p_428431_);
   private static final Set<Pose> VALID_POSES = Set.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING, Pose.FALL_FLYING, Pose.SLEEPING);
   public static final Codec<Pose> POSE_CODEC = Pose.CODEC
      .validate(
         p_422458_ -> VALID_POSES.contains(p_422458_)
            ? DataResult.success(p_422458_)
            : DataResult.error(() -> "Invalid pose: " + p_422458_.getSerializedName())
      );
   private static final Codec<Byte> LAYERS_CODEC = PlayerModelPart.CODEC
      .listOf()
      .xmap(
         p_422893_ -> (byte)p_422893_.stream().mapToInt(PlayerModelPart::getMask).reduce(ALL_LAYERS, (p_430254_, p_428514_) -> p_430254_ & ~p_428514_),
         p_426029_ -> Arrays.stream(PlayerModelPart.values()).filter(p_428580_ -> (p_426029_ & p_428580_.getMask()) == 0).toList()
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

   public Mannequin(EntityType<Mannequin> p_429023_, Level p_428437_) {
      super(p_429023_, p_428437_);
      this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, ALL_LAYERS);
   }

   protected Mannequin(Level p_427767_) {
      this(EntityType.MANNEQUIN, p_427767_);
   }

   public static @Nullable Mannequin create(EntityType<Mannequin> p_429731_, Level p_427402_) {
      return constructor.create(p_429731_, p_427402_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_422847_) {
      super.defineSynchedData(p_422847_);
      p_422847_.define(DATA_PROFILE, DEFAULT_PROFILE);
      p_422847_.define(DATA_IMMOVABLE, false);
      p_422847_.define(DATA_DESCRIPTION, Optional.of(DEFAULT_DESCRIPTION));
   }

   protected ResolvableProfile getProfile() {
      return this.entityData.get(DATA_PROFILE);
   }

   private void setProfile(ResolvableProfile p_431040_) {
      this.entityData.set(DATA_PROFILE, p_431040_);
   }

   private boolean getImmovable() {
      return this.entityData.get(DATA_IMMOVABLE);
   }

   private void setImmovable(boolean p_422902_) {
      this.entityData.set(DATA_IMMOVABLE, p_422902_);
   }

   protected @Nullable Component getDescription() {
      return this.entityData.get(DATA_DESCRIPTION).orElse(null);
   }

   private void setDescription(Component p_426397_) {
      this.description = p_426397_;
      this.updateDescription();
   }

   private void setHideDescription(boolean p_427548_) {
      this.hideDescription = p_427548_;
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
   protected void addAdditionalSaveData(ValueOutput p_431411_) {
      super.addAdditionalSaveData(p_431411_);
      p_431411_.store("profile", ResolvableProfile.CODEC, this.getProfile());
      p_431411_.store("hidden_layers", LAYERS_CODEC, this.entityData.get(DATA_PLAYER_MODE_CUSTOMISATION));
      p_431411_.store("main_hand", HumanoidArm.CODEC, this.getMainArm());
      p_431411_.store("pose", POSE_CODEC, this.getPose());
      p_431411_.putBoolean("immovable", this.getImmovable());
      Component component = this.getDescription();
      if (component != null) {
         if (!component.equals(DEFAULT_DESCRIPTION)) {
            p_431411_.store("description", ComponentSerialization.CODEC, component);
         }
      } else {
         p_431411_.putBoolean("hide_description", true);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_431030_) {
      super.readAdditionalSaveData(p_431030_);
      p_431030_.<ResolvableProfile>read("profile", ResolvableProfile.CODEC).ifPresent(this::setProfile);
      this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, p_431030_.<Byte>read("hidden_layers", LAYERS_CODEC).orElse(ALL_LAYERS));
      this.setMainArm(p_431030_.<HumanoidArm>read("main_hand", HumanoidArm.CODEC).orElse(DEFAULT_MAIN_HAND));
      this.setPose(p_431030_.<Pose>read("pose", POSE_CODEC).orElse(Pose.STANDING));
      this.setImmovable(p_431030_.getBooleanOr("immovable", false));
      this.setHideDescription(p_431030_.getBooleanOr("hide_description", false));
      this.setDescription(p_431030_.<Component>read("description", ComponentSerialization.CODEC).orElse(DEFAULT_DESCRIPTION));
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_422747_) {
      return p_422747_ == DataComponents.PROFILE ? castComponentValue((DataComponentType<T>)p_422747_, this.getProfile()) : super.get(p_422747_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_428149_) {
      this.applyImplicitComponentIfPresent(p_428149_, DataComponents.PROFILE);
      super.applyImplicitComponents(p_428149_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_423584_, T p_426129_) {
      if (p_423584_ == DataComponents.PROFILE) {
         this.setProfile(castComponentValue(DataComponents.PROFILE, p_426129_));
         return true;
      } else {
         return super.applyImplicitComponent(p_423584_, p_426129_);
      }
   }
}
