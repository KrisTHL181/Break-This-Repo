package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BannerBlockEntity extends BlockEntity implements Nameable {
   public static final int MAX_PATTERNS = 6;
   private static final String TAG_PATTERNS = "patterns";
   private static final Component DEFAULT_NAME = Component.translatable("block.minecraft.banner");
   private @Nullable Component name;
   private final DyeColor baseColor;
   private BannerPatternLayers patterns = BannerPatternLayers.EMPTY;

   public BannerBlockEntity(BlockPos p_155035_, BlockState p_155036_) {
      this(p_155035_, p_155036_, ((AbstractBannerBlock)p_155036_.getBlock()).getColor());
   }

   public BannerBlockEntity(BlockPos p_155038_, BlockState p_155039_, DyeColor p_155040_) {
      super(BlockEntityType.BANNER, p_155038_, p_155039_);
      this.baseColor = p_155040_;
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : DEFAULT_NAME;
   }

   @Override
   public @Nullable Component getCustomName() {
      return this.name;
   }

   @Override
   protected void saveAdditional(ValueOutput p_410544_) {
      super.saveAdditional(p_410544_);
      if (!this.patterns.equals(BannerPatternLayers.EMPTY)) {
         p_410544_.store("patterns", BannerPatternLayers.CODEC, this.patterns);
      }

      p_410544_.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
   }

   @Override
   protected void loadAdditional(ValueInput p_407786_) {
      super.loadAdditional(p_407786_);
      this.name = parseCustomNameSafe(p_407786_, "CustomName");
      this.patterns = p_407786_.<BannerPatternLayers>read("patterns", BannerPatternLayers.CODEC).orElse(BannerPatternLayers.EMPTY);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_335241_) {
      return this.saveWithoutMetadata(p_335241_);
   }

   public BannerPatternLayers getPatterns() {
      return this.patterns;
   }

   public ItemStack getItem() {
      ItemStack itemstack = new ItemStack(BannerBlock.byColor(this.baseColor));
      itemstack.applyComponents(this.collectComponents());
      return itemstack;
   }

   public DyeColor getBaseColor() {
      return this.baseColor;
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_396293_) {
      super.applyImplicitComponents(p_396293_);
      this.patterns = p_396293_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      this.name = p_396293_.get(DataComponents.CUSTOM_NAME);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_332512_) {
      super.collectImplicitComponents(p_332512_);
      p_332512_.set(DataComponents.BANNER_PATTERNS, this.patterns);
      p_332512_.set(DataComponents.CUSTOM_NAME, this.name);
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_410646_) {
      p_410646_.discard("patterns");
      p_410646_.discard("CustomName");
   }
}
