package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;

public interface HangingSignBlock {
   HangingSignBlock.Attachment attachmentPoint(BlockState state);

   static HangingSignBlock.Attachment getAttachmentPoint(final BlockState blockState) {
      return blockState.getBlock() instanceof HangingSignBlock hangingSignBlock
         ? hangingSignBlock.attachmentPoint(blockState)
         : HangingSignBlock.Attachment.CEILING;
   }

   enum Attachment implements StringRepresentable {
      WALL("wall"),
      CEILING("ceiling"),
      CEILING_MIDDLE("ceiling_middle");

      public static final Codec<HangingSignBlock.Attachment> CODEC = StringRepresentable.fromEnum(HangingSignBlock.Attachment::values);
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
