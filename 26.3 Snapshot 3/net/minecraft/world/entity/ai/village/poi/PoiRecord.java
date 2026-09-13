package net.minecraft.world.entity.ai.village.poi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.VisibleForDebug;

public class PoiRecord {
   private final BlockPos pos;
   private final Holder<PoiType> poiType;
   private int freeTickets;
   private final Runnable setDirty;

   private PoiRecord(final BlockPos pos, final Holder<PoiType> poiType, final int freeTickets, final Runnable setDirty) {
      this.pos = pos.immutable();
      this.poiType = poiType;
      this.freeTickets = freeTickets;
      this.setDirty = setDirty;
   }

   public PoiRecord(final BlockPos pos, final Holder<PoiType> poiType, final Runnable setDirty) {
      this(pos, poiType, poiType.value().maxTickets(), setDirty);
   }

   public PoiRecord.Packed pack() {
      return new PoiRecord.Packed(this.pos, this.poiType, this.freeTickets);
   }

   @Deprecated
   @VisibleForDebug
   public int getFreeTickets() {
      return this.freeTickets;
   }

   protected boolean acquireTicket() {
      if (this.freeTickets <= 0) {
         return false;
      }

      this.freeTickets--;
      this.setDirty.run();
      return true;
   }

   protected boolean releaseTicket() {
      if (this.freeTickets >= this.poiType.value().maxTickets()) {
         return false;
      }

      this.freeTickets++;
      this.setDirty.run();
      return true;
   }

   public boolean hasSpace() {
      return this.freeTickets > 0;
   }

   public boolean isOccupied() {
      return this.freeTickets != this.poiType.value().maxTickets();
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public Holder<PoiType> getPoiType() {
      return this.poiType;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else {
         return o != null && this.getClass() == o.getClass() ? Objects.equals(this.pos, ((PoiRecord)o).pos) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.pos.hashCode();
   }

   public record Packed(BlockPos pos, Holder<PoiType> poiType, int freeTickets) {
      public static final Codec<PoiRecord.Packed> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(PoiRecord.Packed::pos),
               RegistryFixedCodec.create(Registries.POINT_OF_INTEREST_TYPE).fieldOf("type").forGetter(PoiRecord.Packed::poiType),
               Codec.INT.optionalFieldOf("free_tickets", 0).forGetter(PoiRecord.Packed::freeTickets)
            )
            .apply(i, PoiRecord.Packed::new)
      );

      public PoiRecord unpack(final Runnable setDirty) {
         return new PoiRecord(this.pos, this.poiType, this.freeTickets, setDirty);
      }
   }
}
