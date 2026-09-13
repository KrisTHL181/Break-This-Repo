package net.minecraft.world.entity.animal.fish;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TropicalFish extends AbstractSchoolingFish {
   public static final TropicalFish.Variant DEFAULT_VARIANT = new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.WHITE, DyeColor.WHITE);
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(TropicalFish.class, EntityDataSerializers.INT);
   public static final List<TropicalFish.Variant> COMMON_VARIANTS = List.of(
      new TropicalFish.Variant(TropicalFish.Pattern.STRIPEY, DyeColor.ORANGE, DyeColor.GRAY),
      new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.GRAY),
      new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.BLUE),
      new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.GRAY),
      new TropicalFish.Variant(TropicalFish.Pattern.SUNSTREAK, DyeColor.BLUE, DyeColor.GRAY),
      new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.ORANGE, DyeColor.WHITE),
      new TropicalFish.Variant(TropicalFish.Pattern.SPOTTY, DyeColor.PINK, DyeColor.LIGHT_BLUE),
      new TropicalFish.Variant(TropicalFish.Pattern.BLOCKFISH, DyeColor.PURPLE, DyeColor.YELLOW),
      new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.RED),
      new TropicalFish.Variant(TropicalFish.Pattern.SPOTTY, DyeColor.WHITE, DyeColor.YELLOW),
      new TropicalFish.Variant(TropicalFish.Pattern.GLITTER, DyeColor.WHITE, DyeColor.GRAY),
      new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.ORANGE),
      new TropicalFish.Variant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.PINK),
      new TropicalFish.Variant(TropicalFish.Pattern.BRINELY, DyeColor.LIME, DyeColor.LIGHT_BLUE),
      new TropicalFish.Variant(TropicalFish.Pattern.BETTY, DyeColor.RED, DyeColor.WHITE),
      new TropicalFish.Variant(TropicalFish.Pattern.SNOOPER, DyeColor.GRAY, DyeColor.RED),
      new TropicalFish.Variant(TropicalFish.Pattern.BLOCKFISH, DyeColor.RED, DyeColor.WHITE),
      new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.WHITE, DyeColor.YELLOW),
      new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.RED, DyeColor.WHITE),
      new TropicalFish.Variant(TropicalFish.Pattern.SUNSTREAK, DyeColor.GRAY, DyeColor.WHITE),
      new TropicalFish.Variant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.YELLOW),
      new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.YELLOW, DyeColor.YELLOW)
   );
   private boolean isSchool = true;

   public TropicalFish(EntityType<? extends TropicalFish> p_456769_, Level p_460013_) {
      super(p_456769_, p_460013_);
   }

   public static String getPredefinedName(int p_461072_) {
      return "entity.minecraft.tropical_fish.predefined." + p_461072_;
   }

   static int packVariant(TropicalFish.Pattern p_458816_, DyeColor p_457846_, DyeColor p_451129_) {
      return p_458816_.getPackedId() & 65535 | (p_457846_.getId() & 0xFF) << 16 | (p_451129_.getId() & 0xFF) << 24;
   }

   public static DyeColor getBaseColor(int p_456644_) {
      return DyeColor.byId(p_456644_ >> 16 & 0xFF);
   }

   public static DyeColor getPatternColor(int p_461041_) {
      return DyeColor.byId(p_461041_ >> 24 & 0xFF);
   }

   public static TropicalFish.Pattern getPattern(int p_453056_) {
      return TropicalFish.Pattern.byId(p_453056_ & 65535);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_451572_) {
      super.defineSynchedData(p_451572_);
      p_451572_.define(DATA_ID_TYPE_VARIANT, DEFAULT_VARIANT.getPackedId());
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_454928_) {
      super.addAdditionalSaveData(p_454928_);
      p_454928_.store("Variant", TropicalFish.Variant.CODEC, new TropicalFish.Variant(this.getPackedVariant()));
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_458988_) {
      super.readAdditionalSaveData(p_458988_);
      TropicalFish.Variant tropicalfish$variant = p_458988_.<TropicalFish.Variant>read("Variant", TropicalFish.Variant.CODEC).orElse(DEFAULT_VARIANT);
      this.setPackedVariant(tropicalfish$variant.getPackedId());
   }

   private void setPackedVariant(int p_452807_) {
      this.entityData.set(DATA_ID_TYPE_VARIANT, p_452807_);
   }

   @Override
   public boolean isMaxGroupSizeReached(int p_450150_) {
      return !this.isSchool;
   }

   private int getPackedVariant() {
      return this.entityData.get(DATA_ID_TYPE_VARIANT);
   }

   public DyeColor getBaseColor() {
      return getBaseColor(this.getPackedVariant());
   }

   public DyeColor getPatternColor() {
      return getPatternColor(this.getPackedVariant());
   }

   public TropicalFish.Pattern getPattern() {
      return getPattern(this.getPackedVariant());
   }

   private void setPattern(TropicalFish.Pattern p_451024_) {
      int i = this.getPackedVariant();
      DyeColor dyecolor = getBaseColor(i);
      DyeColor dyecolor1 = getPatternColor(i);
      this.setPackedVariant(packVariant(p_451024_, dyecolor, dyecolor1));
   }

   private void setBaseColor(DyeColor p_457875_) {
      int i = this.getPackedVariant();
      TropicalFish.Pattern tropicalfish$pattern = getPattern(i);
      DyeColor dyecolor = getPatternColor(i);
      this.setPackedVariant(packVariant(tropicalfish$pattern, p_457875_, dyecolor));
   }

   private void setPatternColor(DyeColor p_454447_) {
      int i = this.getPackedVariant();
      TropicalFish.Pattern tropicalfish$pattern = getPattern(i);
      DyeColor dyecolor = getBaseColor(i);
      this.setPackedVariant(packVariant(tropicalfish$pattern, dyecolor, p_454447_));
   }

   @Override
   public <T> @Nullable T get(DataComponentType<? extends T> p_454642_) {
      if (p_454642_ == DataComponents.TROPICAL_FISH_PATTERN) {
         return castComponentValue((DataComponentType<T>)p_454642_, this.getPattern());
      } else if (p_454642_ == DataComponents.TROPICAL_FISH_BASE_COLOR) {
         return castComponentValue((DataComponentType<T>)p_454642_, this.getBaseColor());
      } else {
         return p_454642_ == DataComponents.TROPICAL_FISH_PATTERN_COLOR
            ? castComponentValue((DataComponentType<T>)p_454642_, this.getPatternColor())
            : super.get(p_454642_);
      }
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_461053_) {
      this.applyImplicitComponentIfPresent(p_461053_, DataComponents.TROPICAL_FISH_PATTERN);
      this.applyImplicitComponentIfPresent(p_461053_, DataComponents.TROPICAL_FISH_BASE_COLOR);
      this.applyImplicitComponentIfPresent(p_461053_, DataComponents.TROPICAL_FISH_PATTERN_COLOR);
      super.applyImplicitComponents(p_461053_);
   }

   @Override
   protected <T> boolean applyImplicitComponent(DataComponentType<T> p_450372_, T p_459284_) {
      if (p_450372_ == DataComponents.TROPICAL_FISH_PATTERN) {
         this.setPattern(castComponentValue(DataComponents.TROPICAL_FISH_PATTERN, p_459284_));
         return true;
      } else if (p_450372_ == DataComponents.TROPICAL_FISH_BASE_COLOR) {
         this.setBaseColor(castComponentValue(DataComponents.TROPICAL_FISH_BASE_COLOR, p_459284_));
         return true;
      } else if (p_450372_ == DataComponents.TROPICAL_FISH_PATTERN_COLOR) {
         this.setPatternColor(castComponentValue(DataComponents.TROPICAL_FISH_PATTERN_COLOR, p_459284_));
         return true;
      } else {
         return super.applyImplicitComponent(p_450372_, p_459284_);
      }
   }

   @Override
   public void saveToBucketTag(ItemStack p_454947_) {
      super.saveToBucketTag(p_454947_);
      p_454947_.copyFrom(DataComponents.TROPICAL_FISH_PATTERN, this);
      p_454947_.copyFrom(DataComponents.TROPICAL_FISH_BASE_COLOR, this);
      p_454947_.copyFrom(DataComponents.TROPICAL_FISH_PATTERN_COLOR, this);
   }

   @Override
   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.TROPICAL_FISH_BUCKET);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.TROPICAL_FISH_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.TROPICAL_FISH_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_460912_) {
      return SoundEvents.TROPICAL_FISH_HURT;
   }

   @Override
   protected SoundEvent getFlopSound() {
      return SoundEvents.TROPICAL_FISH_FLOP;
   }

   @Override
   public @Nullable SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_456098_, DifficultyInstance p_460469_, EntitySpawnReason p_457358_, @Nullable SpawnGroupData p_459783_
   ) {
      p_459783_ = super.finalizeSpawn(p_456098_, p_460469_, p_457358_, p_459783_);
      RandomSource randomsource = p_456098_.getRandom();
      TropicalFish.Variant tropicalfish$variant;
      if (p_459783_ instanceof TropicalFish.TropicalFishGroupData tropicalfish$tropicalfishgroupdata) {
         tropicalfish$variant = tropicalfish$tropicalfishgroupdata.variant;
      } else if (randomsource.nextFloat() < 0.9) {
         tropicalfish$variant = Util.getRandom(COMMON_VARIANTS, randomsource);
         p_459783_ = new TropicalFish.TropicalFishGroupData(this, tropicalfish$variant);
      } else {
         this.isSchool = false;
         TropicalFish.Pattern[] atropicalfish$pattern = TropicalFish.Pattern.values();
         DyeColor[] adyecolor = DyeColor.values();
         TropicalFish.Pattern tropicalfish$pattern = Util.getRandom(atropicalfish$pattern, randomsource);
         DyeColor dyecolor = Util.getRandom(adyecolor, randomsource);
         DyeColor dyecolor1 = Util.getRandom(adyecolor, randomsource);
         tropicalfish$variant = new TropicalFish.Variant(tropicalfish$pattern, dyecolor, dyecolor1);
      }

      this.setPackedVariant(tropicalfish$variant.getPackedId());
      return p_459783_;
   }

   public static boolean checkTropicalFishSpawnRules(
      EntityType<TropicalFish> p_451021_, LevelAccessor p_456913_, EntitySpawnReason p_453461_, BlockPos p_453880_, RandomSource p_457133_
   ) {
      return p_456913_.getFluidState(p_453880_.below()).is(FluidTags.WATER)
         && p_456913_.getBlockState(p_453880_.above()).is(Blocks.WATER)
         && (
            p_456913_.getBiome(p_453880_).is(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT)
               || WaterAnimal.checkSurfaceWaterAnimalSpawnRules(p_451021_, p_456913_, p_453461_, p_453880_, p_457133_)
         );
   }

   public enum Base {
      SMALL(0),
      LARGE(1);

      final int id;

      Base(final int p_456023_) {
         this.id = p_456023_;
      }
   }

   public enum Pattern implements StringRepresentable, TooltipProvider {
      KOB("kob", TropicalFish.Base.SMALL, 0),
      SUNSTREAK("sunstreak", TropicalFish.Base.SMALL, 1),
      SNOOPER("snooper", TropicalFish.Base.SMALL, 2),
      DASHER("dasher", TropicalFish.Base.SMALL, 3),
      BRINELY("brinely", TropicalFish.Base.SMALL, 4),
      SPOTTY("spotty", TropicalFish.Base.SMALL, 5),
      FLOPPER("flopper", TropicalFish.Base.LARGE, 0),
      STRIPEY("stripey", TropicalFish.Base.LARGE, 1),
      GLITTER("glitter", TropicalFish.Base.LARGE, 2),
      BLOCKFISH("blockfish", TropicalFish.Base.LARGE, 3),
      BETTY("betty", TropicalFish.Base.LARGE, 4),
      CLAYFISH("clayfish", TropicalFish.Base.LARGE, 5);

      public static final Codec<TropicalFish.Pattern> CODEC = StringRepresentable.fromEnum(TropicalFish.Pattern::values);
      private static final IntFunction<TropicalFish.Pattern> BY_ID = ByIdMap.sparse(TropicalFish.Pattern::getPackedId, values(), KOB);
      public static final StreamCodec<ByteBuf, TropicalFish.Pattern> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, TropicalFish.Pattern::getPackedId);
      private final String name;
      private final Component displayName;
      private final TropicalFish.Base base;
      private final int packedId;

      Pattern(final String p_454122_, final TropicalFish.Base p_457367_, final int p_454659_) {
         this.name = p_454122_;
         this.base = p_457367_;
         this.packedId = p_457367_.id | p_454659_ << 8;
         this.displayName = Component.translatable("entity.minecraft.tropical_fish.type." + this.name);
      }

      public static TropicalFish.Pattern byId(int p_457964_) {
         return BY_ID.apply(p_457964_);
      }

      public TropicalFish.Base base() {
         return this.base;
      }

      public int getPackedId() {
         return this.packedId;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public Component displayName() {
         return this.displayName;
      }

      @Override
      public void addToTooltip(Item.TooltipContext p_453168_, Consumer<Component> p_451414_, TooltipFlag p_455191_, DataComponentGetter p_451999_) {
         DyeColor dyecolor = p_451999_.getOrDefault(DataComponents.TROPICAL_FISH_BASE_COLOR, TropicalFish.DEFAULT_VARIANT.baseColor());
         DyeColor dyecolor1 = p_451999_.getOrDefault(DataComponents.TROPICAL_FISH_PATTERN_COLOR, TropicalFish.DEFAULT_VARIANT.patternColor());
         ChatFormatting[] achatformatting = new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY};
         int i = TropicalFish.COMMON_VARIANTS.indexOf(new TropicalFish.Variant(this, dyecolor, dyecolor1));
         if (i != -1) {
            p_451414_.accept(Component.translatable(TropicalFish.getPredefinedName(i)).withStyle(achatformatting));
         } else {
            p_451414_.accept(this.displayName.plainCopy().withStyle(achatformatting));
            MutableComponent mutablecomponent = Component.translatable("color.minecraft." + dyecolor.getName());
            if (dyecolor != dyecolor1) {
               mutablecomponent.append(", ").append(Component.translatable("color.minecraft." + dyecolor1.getName()));
            }

            mutablecomponent.withStyle(achatformatting);
            p_451414_.accept(mutablecomponent);
         }
      }
   }

   static class TropicalFishGroupData extends AbstractSchoolingFish.SchoolSpawnGroupData {
      final TropicalFish.Variant variant;

      TropicalFishGroupData(TropicalFish p_454387_, TropicalFish.Variant p_452358_) {
         super(p_454387_);
         this.variant = p_452358_;
      }
   }

   public record Variant(TropicalFish.Pattern pattern, DyeColor baseColor, DyeColor patternColor) {
      public static final Codec<TropicalFish.Variant> CODEC = Codec.INT.xmap(TropicalFish.Variant::new, TropicalFish.Variant::getPackedId);

      public Variant(int p_452699_) {
         this(TropicalFish.getPattern(p_452699_), TropicalFish.getBaseColor(p_452699_), TropicalFish.getPatternColor(p_452699_));
      }

      public int getPackedId() {
         return TropicalFish.packVariant(this.pattern, this.baseColor, this.patternColor);
      }
   }
}
