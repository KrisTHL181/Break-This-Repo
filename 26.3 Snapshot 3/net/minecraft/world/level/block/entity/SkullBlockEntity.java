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

   public SkullBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      super(BlockEntityTypes.SKULL, worldPosition, blockState);
   }

   @Override
   protected void saveAdditional(final ValueOutput output) {
      super.saveAdditional(output);
      output.storeNullable("profile", ResolvableProfile.CODEC, this.owner);
      output.storeNullable("note_block_sound", Identifier.CODEC, this.noteBlockSound);
      output.storeNullable("custom_name", ComponentSerialization.CODEC, this.customName);
   }

   @Override
   protected void loadAdditional(final ValueInput input) {
      super.loadAdditional(input);
      this.owner = input.<ResolvableProfile>read("profile", ResolvableProfile.CODEC).orElse(null);
      this.noteBlockSound = input.<Identifier>read("note_block_sound", Identifier.CODEC).orElse(null);
      this.customName = parseCustomNameSafe(input, "custom_name");
   }

   public static void animation(final Level level, final BlockPos pos, final BlockState state, final SkullBlockEntity entity) {
      if (state.hasProperty(SkullBlock.POWERED) && state.getValue(SkullBlock.POWERED)) {
         entity.isAnimating = true;
         entity.animationTickCount++;
      } else {
         entity.isAnimating = false;
      }
   }

   public float getAnimation(final float a) {
      return this.isAnimating ? this.animationTickCount + a : this.animationTickCount;
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
   public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      super.applyImplicitComponents(components);
      this.owner = components.get(DataComponents.PROFILE);
      this.noteBlockSound = components.get(DataComponents.NOTE_BLOCK_SOUND);
      this.customName = components.get(DataComponents.CUSTOM_NAME);
   }

   @Override
   protected void collectImplicitComponents(final DataComponentMap.Builder components) {
      super.collectImplicitComponents(components);
      components.set(DataComponents.PROFILE, this.owner);
      components.set(DataComponents.NOTE_BLOCK_SOUND, this.noteBlockSound);
      components.set(DataComponents.CUSTOM_NAME, this.customName);
   }

   @Override
   public void removeComponentsFromTag(final ValueOutput output) {
      super.removeComponentsFromTag(output);
      output.discard("profile");
      output.discard("note_block_sound");
      output.discard("custom_name");
   }
}
