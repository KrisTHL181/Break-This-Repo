package net.minecraft.server.dialog;

import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum DialogAction implements StringRepresentable {
   CLOSE(0, "close"),
   NONE(1, "none"),
   WAIT_FOR_RESPONSE(2, "wait_for_response");

   public static final IntFunction<DialogAction> BY_ID = ByIdMap.continuous(p_407844_ -> p_407844_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StringRepresentable.EnumCodec<DialogAction> CODEC = StringRepresentable.fromEnum(DialogAction::values);
   public static final StreamCodec<ByteBuf, DialogAction> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_410706_ -> p_410706_.id);
   private final int id;
   private final String name;

   DialogAction(final int p_406044_, final String p_408038_) {
      this.id = p_406044_;
      this.name = p_408038_;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public boolean willUnpause() {
      return this == CLOSE || this == WAIT_FOR_RESPONSE;
   }
}
