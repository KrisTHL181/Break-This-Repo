package net.minecraft.server.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;

public class Ticket {
   public static final MapCodec<Ticket> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BuiltInRegistries.TICKET_TYPE.byNameCodec().fieldOf("type").forGetter(Ticket::getType),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("level").forGetter(Ticket::getTicketLevel),
            Codec.LONG.optionalFieldOf("ticks_left", 0L).forGetter(t -> t.ticksLeft)
         )
         .apply(i, Ticket::new)
   );
   private final TicketType type;
   private final int ticketLevel;
   private long ticksLeft;

   public Ticket(final TicketType type, final int ticketLevel) {
      this(type, ticketLevel, type.timeout());
   }

   private Ticket(final TicketType type, final int ticketLevel, final long ticksLeft) {
      this.type = type;
      this.ticketLevel = ticketLevel;
      this.ticksLeft = ticksLeft;
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
