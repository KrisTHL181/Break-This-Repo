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

   PoiRecord(BlockPos p_218008_, Holder<PoiType> p_218009_, int p_218010_, Runnable p_218011_) {
      this.pos = p_218008_.immutable();
      this.poiType = p_218009_;
      this.freeTickets = p_218010_;
      this.setDirty = p_218011_;
   }

   public PoiRecord(BlockPos p_218013_, Holder<PoiType> p_218014_, Runnable p_218015_) {
      this(p_218013_, p_218014_, p_218014_.value().maxTickets(), p_218015_);
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
   public boolean equals(Object p_27256_) {
      if (this == p_27256_) {
         return true;
      } else {
         return p_27256_ != null && this.getClass() == p_27256_.getClass() ? Objects.equals(this.pos, ((PoiRecord)p_27256_).pos) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.pos.hashCode();
   }

   public record Packed(BlockPos pos, Holder<PoiType> poiType, int freeTickets) {
      public static final Codec<PoiRecord.Packed> CODEC = RecordCodecBuilder.create(
         p_362341_ -> p_362341_.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(PoiRecord.Packed::pos),
               RegistryFixedCodec.create(Registries.POINT_OF_INTEREST_TYPE).fieldOf("type").forGetter(PoiRecord.Packed::poiType),
               Codec.INT.fieldOf("free_tickets").orElse(0).forGetter(PoiRecord.Packed::freeTickets)
            )
            .apply(p_362341_, PoiRecord.Packed::new)
      );

      public PoiRecord unpack(Runnable p_362041_) {
         return new PoiRecord(this.pos, this.poiType, this.freeTickets, p_362041_);
      }
   }
}
