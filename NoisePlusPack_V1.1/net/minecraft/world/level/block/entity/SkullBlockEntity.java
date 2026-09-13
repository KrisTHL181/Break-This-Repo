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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SkullBlockEntity extends BlockEntity {
   private static final String TAG_PROFILE = "profile";
   private static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
   private static final String TAG_CUSTOM_NAME = "custom_name";
   private @Nullable ResolvableProfile owner;
   private @Nullable Identifier noteBlockSound;
   private int animationTickCount;
   private boolean isAnimating;
   private @Nullable Component customName;

   public SkullBlockEntity(BlockPos p_155731_, BlockState p_155732_) {
      super(BlockEntityType.SKULL, p_155731_, p_155732_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_409149_) {
      super.saveAdditional(p_409149_);
      p_409149_.storeNullable("profile", ResolvableProfile.CODEC, this.owner);
      p_409149_.storeNullable("note_block_sound", Identifier.CODEC, this.noteBlockSound);
      p_409149_.storeNullable("custom_name", ComponentSerialization.CODEC, this.customName);
   }

   @Override
   protected void loadAdditional(ValueInput p_409251_) {
      super.loadAdditional(p_409251_);
      this.owner = p_409251_.<ResolvableProfile>read("profile", ResolvableProfile.CODEC).orElse(null);
      this.noteBlockSound = p_409251_.<Identifier>read("note_block_sound", Identifier.CODEC).orElse(null);
      this.customName = parseCustomNameSafe(p_409251_, "custom_name");
   }

   public static void animation(Level p_261710_, BlockPos p_262153_, BlockState p_262021_, SkullBlockEntity p_261594_) {
      if (p_262021_.hasProperty(SkullBlock.POWERED) && p_262021_.getValue(SkullBlock.POWERED)) {
         p_261594_.isAnimating = true;
         p_261594_.animationTickCount++;
      } else {
         p_261594_.isAnimating = false;
      }
   }

   public float getAnimation(float p_262053_) {
      return this.isAnimating ? this.animationTickCount + p_262053_ : this.animationTickCount;
   }

   public @Nullable ResolvableProfile getOwnerProfile() {
      return this.owner;
   }

   public @Nullable Identifier getNoteBlockSound() {
      return this.noteBlockSound;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_335540_) {
      return this.saveCustomOnly(p_335540_);
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter p_393162_) {
      super.applyImplicitComponents(p_393162_);
      this.owner = p_393162_.get(DataComponents.PROFILE);
      this.noteBlockSound = p_393162_.get(DataComponents.NOTE_BLOCK_SOUND);
      this.customName = p_393162_.get(DataComponents.CUSTOM_NAME);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder p_335245_) {
      super.collectImplicitComponents(p_335245_);
      p_335245_.set(DataComponents.PROFILE, this.owner);
      p_335245_.set(DataComponents.NOTE_BLOCK_SOUND, this.noteBlockSound);
      p_335245_.set(DataComponents.CUSTOM_NAME, this.customName);
   }

   @Override
   public void removeComponentsFromTag(ValueOutput p_410487_) {
      super.removeComponentsFromTag(p_410487_);
      p_410487_.discard("profile");
      p_410487_.discard("note_block_sound");
      p_410487_.discard("custom_name");
   }
}
