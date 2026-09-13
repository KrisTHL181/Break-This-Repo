package net.minecraft.server.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;

public class Ticket {
   public static final MapCodec<Ticket> CODEC = RecordCodecBuilder.mapCodec(
      p_392519_ -> p_392519_.group(
            BuiltInRegistries.TICKET_TYPE.byNameCodec().fieldOf("type").forGetter(Ticket::getType),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("level").forGetter(Ticket::getTicketLevel),
            Codec.LONG.optionalFieldOf("ticks_left", 0L).forGetter(p_391632_ -> p_391632_.ticksLeft)
         )
         .apply(p_392519_, Ticket::new)
   );
   private final TicketType type;
   private final int ticketLevel;
   private long ticksLeft;

   public Ticket(TicketType p_9425_, int p_9426_) {
      this(p_9425_, p_9426_, p_9425_.timeout());
   }

   private Ticket(TicketType p_392802_, int p_394500_, long p_395875_) {
      this.type = p_392802_;
      this.ticketLevel = p_394500_;
      this.ticksLeft = p_395875_;
   }

   @Override
   public String toString() {
      return this.type.hasTimeout()
         ? "Ticket["
            + Util.getRegisteredName(BuiltInRegistries.TICKET_TYPE, this.type)
            + " "
            + this.ticketLevel
            + "] with "
            + this.ticksLeft
            + " ticks left ( out of"
            + this.type.timeout()
            + ")"
         : "Ticket[" + Util.getRegisteredName(BuiltInRegistries.TICKET_TYPE, this.type) + " " + this.ticketLevel + "] with no timeout";
   }

   public TicketType getType() {
      return this.type;
   }

   public int getTicketLevel() {
      return this.ticketLevel;
   }

   public void resetTicksLeft() {
      this.ticksLeft = this.type.timeout();
   }

   public void decreaseTicksLeft() {
      if (this.type.hasTimeout()) {
         this.ticksLeft--;
      }
   }

   public boolean isTimedOut() {
      return this.type.hasTimeout() && this.ticksLeft < 0L;
   }
}
