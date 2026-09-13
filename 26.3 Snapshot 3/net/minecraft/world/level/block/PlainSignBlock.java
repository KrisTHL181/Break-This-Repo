package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;

public interface PlainSignBlock {
   PlainSignBlock.Attachment attachmentPoint(BlockState state);

   static PlainSignBlock.Attachment getAttachmentPoint(final BlockState blockState) {
      return blockState.getBlock() instanceof PlainSignBlock plainSignBlock ? plainSignBlock.attachmentPoint(blockState) : PlainSignBlock.Attachment.GROUND;
   }

   enum Attachment implements StringRepresentable {
      WALL("wall"),
      GROUND("ground");

      public static final Codec<PlainSignBlock.Attachment> CODEC = StringRepresentable.fromEnum(PlainSignBlock.Attachment::values);
      private final String name;

      Attachment(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
