package net.minecraft.world.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TicketStorage extends SavedData {
   private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Codec<Pair<ChunkPos, Ticket>> TICKET_ENTRY = Codec.mapPair(ChunkPos.CODEC.fieldOf("chunk_pos"), Ticket.CODEC).codec();
   public static final Codec<TicketStorage> CODEC = RecordCodecBuilder.create(
      i -> i.group(TICKET_ENTRY.listOf().optionalFieldOf("tickets", List.of()).forGetter(TicketStorage::packTickets)).apply(i, TicketStorage::fromPacked)
   );
   public static final SavedDataType<TicketStorage> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace("chunk_tickets"), TicketStorage::new, CODEC, DataFixTypes.SAVED_DATA_FORCED_CHUNKS
   );
   private final Long2ObjectOpenHashMap<List<Ticket>> tickets;
   private final Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets;
   private LongSet chunksWithForcedTickets = new LongOpenHashSet();
   private TicketStorage.@Nullable ChunkUpdated loadingChunkUpdatedListener;
   private TicketStorage.@Nullable ChunkUpdated simulationChunkUpdatedListener;

   private TicketStorage(final Long2ObjectOpenHashMap<List<Ticket>> tickets, final Long2ObjectOpenHashMap<List<Ticket>> deactivatedTickets) {
      this.tickets = tickets;
      this.deactivatedTickets = deactivatedTickets;
      this.updateForcedChunks();
   }

   public TicketStorage() {
      this(new Long2ObjectOpenHashMap(4), new Long2ObjectOpenHashMap());
   }

   private static TicketStorage fromPacked(final List<Pair<ChunkPos, Ticket>> tickets) {
      Long2ObjectOpenHashMap<List<Ticket>> ticketsToLoad = new Long2ObjectOpenHashMap();

      for (Pair<ChunkPos, Ticket> ticket : tickets) {
         ChunkPos pos = (ChunkPos)ticket.getFirst();
         List<Ticket> ticketsInChunk = (List<Ticket>)ticketsToLoad.computeIfAbsent(pos.pack(), k -> new ObjectArrayList(4));
         ticketsInChunk.add((Ticket)ticket.getSecond());
      }

      return new TicketStorage(new Long2ObjectOpenHashMap(4), ticketsToLoad);
   }

   private List<Pair<ChunkPos, Ticket>> packTickets() {
      List<Pair<ChunkPos, Ticket>> tickets = new ArrayList<>();
      this.forEachTicket((pos, ticket) -> {
         if (ticket.getType().persist()) {
            tickets.add(new Pair(pos, ticket));
         }
      });
      return tickets;
   }

   private void forEachTicket(final BiConsumer<ChunkPos, Ticket> output) {
      forEachTicket(output, this.tickets);
      forEachTicket(output, this.deactivatedTickets);
   }

   private static void forEachTicket(final BiConsumer<ChunkPos, Ticket> output, final Long2ObjectOpenHashMap<List<Ticket>> tickets) {
      ObjectIterator var2 = Long2ObjectMaps.fastIterable(tickets).iterator();

      while (var2.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var2.next();
         ChunkPos chunkPos = ChunkPos.unpack(entry.getLongKey());

         for (Ticket ticket : (List)entry.getValue()) {
            output.accept(chunkPos, ticket);
         }
      }
   }

   public void activateAllDeactivatedTickets() {
      ObjectIterator var1 = Long2ObjectMaps.fastIterable(this.deactivatedTickets).iterator();

      while (var1.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var1.next();

         for (Ticket ticket : (List)entry.getValue()) {
            this.addTicket(entry.getLongKey(), ticket);
         }
      }

      this.deactivatedTickets.clear();
   }

   public void setLoadingChunkUpdatedListener(final TicketStorage.@Nullable ChunkUpdated loadingChunkUpdatedListener) {
      this.loadingChunkUpdatedListener = loadingChunkUpdatedListener;
   }

   public void setSimulationChunkUpdatedListener(final TicketStorage.@Nullable ChunkUpdated simulationChunkUpdatedListener) {
      this.simulationChunkUpdatedListener = simulationChunkUpdatedListener;
   }

   public boolean hasTickets() {
      return !this.tickets.isEmpty();
   }

   public boolean shouldKeepDimensionActive() {
      ObjectIterator var1 = this.tickets.values().iterator();

      while (var1.hasNext()) {
         List<Ticket> group = (List<Ticket>)var1.next();

         for (Ticket ticket : group) {
            if (ticket.getType().shouldKeepDimensionActive()) {
               return true;
            }
         }
      }

      return false;
   }

   public List<Ticket> getTickets(final long key) {
      return (List<Ticket>)this.tickets.getOrDefault(key, List.of());
   }

   private List<Ticket> getOrCreateTickets(final long key) {
      return (List<Ticket>)this.tickets.computeIfAbsent(key, k -> new ObjectArrayList(4));
   }

   public void addTicketWithRadius(final TicketType type, final ChunkPos chunkPos, final int radius) {
      Ticket ticket = new Ticket(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
      this.addTicket(chunkPos.pack(), ticket);
   }

   public void addTicket(final Ticket ticket, final ChunkPos chunkPos) {
      this.addTicket(chunkPos.pack(), ticket);
   }

   public boolean addTicket(final long key, final Ticket ticket) {
      List<Ticket> tickets = this.getOrCreateTickets(key);

      for (Ticket t : tickets) {
         if (isTicketSameTypeAndLevel(ticket, t)) {
            t.resetTicksLeft();
            this.setDirty();
            return false;
         }
      }

      int oldSimulationTicketLevel = getTicketLevelAt(tickets, true);
      int oldLoadingTicketLevel = getTicketLevelAt(tickets, false);
      tickets.add(ticket);
      if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
         LOGGER.debug("ATI {} {}", ChunkPos.unpack(key), ticket);
      }

      if (ticket.getType().doesSimulate() && ticket.getTicketLevel() < oldSimulationTicketLevel && this.simulationChunkUpdatedListener != null) {
         this.simulationChunkUpdatedListener.update(key, ticket.getTicketLevel(), true);
      }

      if (ticket.getType().doesLoad() && ticket.getTicketLevel() < oldLoadingTicketLevel && this.loadingChunkUpdatedListener != null) {
         this.loadingChunkUpdatedListener.update(key, ticket.getTicketLevel(), true);
      }

      if (ticket.getType().equals(TicketType.FORCED)) {
         this.chunksWithForcedTickets.add(key);
      }

      this.setDirty();
      return true;
   }

   private static boolean isTicketSameTypeAndLevel(final Ticket ticket, final Ticket t) {
      return t.getType() == ticket.getType() && t.getTicketLevel() == ticket.getTicketLevel();
   }

   public int getTicketLevelAt(final long key, final boolean simulation) {
      return getTicketLevelAt(this.getTickets(key), simulation);
   }

   private static int getTicketLevelAt(final List<Ticket> tickets, final boolean simulation) {
      Ticket lowestTicket = getLowestTicket(tickets, simulation);
      return lowestTicket == null ? ChunkLevel.MAX_LEVEL + 1 : lowestTicket.getTicketLevel();
   }

   private static @Nullable Ticket getLowestTicket(final @Nullable List<Ticket> tickets, final boolean simulation) {
      if (tickets == null) {
         return null;
      }

      Ticket t = null;

      for (Ticket ticket : tickets) {
         if (t == null || ticket.getTicketLevel() < t.getTicketLevel()) {
            if (simulation && ticket.getType().doesSimulate()) {
               t = ticket;
            } else if (!simulation && ticket.getType().doesLoad()) {
               t = ticket;
            }
         }
      }

      return t;
   }

   public void removeTicketWithRadius(final TicketType type, final ChunkPos chunkPos, final int radius) {
      Ticket ticket = new Ticket(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
      this.removeTicket(chunkPos.pack(), ticket);
   }

   public void removeTicket(final Ticket ticket, final ChunkPos chunkPos) {
      this.removeTicket(chunkPos.pack(), ticket);
   }

   public boolean removeTicket(final long key, final Ticket ticket) {
      List<Ticket> tickets = (List<Ticket>)this.tickets.get(key);
      if (tickets == null) {
         return false;
      }

      boolean found = false;
      Iterator<Ticket> iterator = tickets.iterator();

      while (iterator.hasNext()) {
         Ticket t = iterator.next();
         if (isTicketSameTypeAndLevel(ticket, t)) {
            iterator.remove();
            if (SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
               LOGGER.debug("RTI {} {}", ChunkPos.unpack(key), t);
            }

            found = true;
            break;
         }
      }

      if (!found) {
         return false;
      }

      if (tickets.isEmpty()) {
         this.tickets.remove(key);
      }

      if (ticket.getType().doesSimulate() && this.simulationChunkUpdatedListener != null) {
         this.simulationChunkUpdatedListener.update(key, getTicketLevelAt(tickets, true), false);
      }

      if (ticket.getType().doesLoad() && this.loadingChunkUpdatedListener != null) {
         this.loadingChunkUpdatedListener.update(key, getTicketLevelAt(tickets, false), false);
      }

      if (ticket.getType().equals(TicketType.FORCED)) {
         this.updateForcedChunks();
      }

      this.setDirty();
      return true;
   }

   private void updateForcedChunks() {
      this.chunksWithForcedTickets = this.getAllChunksWithTicketThat(t -> t.getType().equals(TicketType.FORCED));
   }

   public String getTicketDebugString(final long key, final boolean simulation) {
      List<Ticket> tickets = this.getTickets(key);
      Ticket lowestTicket = getLowestTicket(tickets, simulation);
      return lowestTicket == null ? "no_ticket" : lowestTicket.toString();
   }

   public void purgeStaleTickets(final ChunkMap chunkMap) {
      this.removeTicketIf((ticket, chunkPos) -> {
         if (this.canTicketExpire(chunkMap, ticket, chunkPos)) {
            ticket.decreaseTicksLeft();
            return ticket.isTimedOut();
         } else {
            return false;
         }
      }, null);
      this.setDirty();
   }

   private boolean canTicketExpire(final ChunkMap chunkMap, final Ticket ticket, final long chunkPos) {
      if (!ticket.getType().hasTimeout()) {
         return false;
      }

      if (ticket.getType().canExpireIfUnloaded()) {
         return true;
      }

      ChunkHolder updatingChunk = chunkMap.getUpdatingChunkIfPresent(chunkPos);
      return updatingChunk == null || updatingChunk.isReadyForSaving();
   }

   public void deactivateTicketsOnClosing() {
      this.removeTicketIf((ticket, chunkPos) -> ticket.getType() != TicketType.UNKNOWN, this.deactivatedTickets);
   }

   public void removeTicketIf(final TicketStorage.TicketPredicate predicate, final @Nullable Long2ObjectOpenHashMap<List<Ticket>> removedTickets) {
      ObjectIterator<Entry<List<Ticket>>> ticketsPerChunkIterator = this.tickets.long2ObjectEntrySet().fastIterator();
      boolean removedForced = false;

      while (ticketsPerChunkIterator.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)ticketsPerChunkIterator.next();
         Iterator<Ticket> chunkTicketsIterator = ((List)entry.getValue()).iterator();
         long chunkPos = entry.getLongKey();
         boolean removedSimulation = false;
         boolean removedLoading = false;

         while (chunkTicketsIterator.hasNext()) {
            Ticket ticket = chunkTicketsIterator.next();
            if (predicate.test(ticket, chunkPos)) {
               if (removedTickets != null) {
                  List<Ticket> tickets = (List<Ticket>)removedTickets.computeIfAbsent(chunkPos, k -> new ObjectArrayList(((List)entry.getValue()).size()));
                  tickets.add(ticket);
               }

               chunkTicketsIterator.remove();
               if (ticket.getType().doesLoad()) {
                  removedLoading = true;
               }

               if (ticket.getType().doesSimulate()) {
                  removedSimulation = true;
               }

               if (ticket.getType().equals(TicketType.FORCED)) {
                  removedForced = true;
               }
            }
         }

         if (removedLoading || removedSimulation) {
            if (removedLoading && this.loadingChunkUpdatedListener != null) {
               this.loadingChunkUpdatedListener.update(chunkPos, getTicketLevelAt((List<Ticket>)entry.getValue(), false), false);
            }

            if (removedSimulation && this.simulationChunkUpdatedListener != null) {
               this.simulationChunkUpdatedListener.update(chunkPos, getTicketLevelAt((List<Ticket>)entry.getValue(), true), false);
            }

            this.setDirty();
            if (((List)entry.getValue()).isEmpty()) {
               ticketsPerChunkIterator.remove();
            }
         }
      }

      if (removedForced) {
         this.updateForcedChunks();
      }
   }

   public void replaceTicketLevelOfType(final int newLevel, final TicketType ticketType) {
      List<Pair<Ticket, Long>> affectedTickets = new ArrayList<>();
      ObjectIterator var4 = this.tickets.long2ObjectEntrySet().iterator();

      while (var4.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var4.next();

         for (Ticket ticket : (List)entry.getValue()) {
            if (ticket.getType() == ticketType) {
               affectedTickets.add(Pair.of(ticket, entry.getLongKey()));
            }
         }
      }

      for (Pair<Ticket, Long> pair : affectedTickets) {
         Long key = (Long)pair.getSecond();
         Ticket ticket = (Ticket)pair.getFirst();
         this.removeTicket(key, ticket);
         TicketType type = ticket.getType();
         this.addTicket(key, new Ticket(type, newLevel));
      }
   }

   public boolean updateChunkForced(final ChunkPos chunkPos, final boolean forced) {
      Ticket ticket = new Ticket(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL);
      return forced ? this.addTicket(chunkPos.pack(), ticket) : this.removeTicket(chunkPos.pack(), ticket);
   }

   public LongSet getForceLoadedChunks() {
      return this.chunksWithForcedTickets;
   }

   private LongSet getAllChunksWithTicketThat(final Predicate<Ticket> ticketCheck) {
      LongOpenHashSet chunks = new LongOpenHashSet();
      ObjectIterator var3 = Long2ObjectMaps.fastIterable(this.tickets).iterator();

      while (var3.hasNext()) {
         Entry<List<Ticket>> entry = (Entry<List<Ticket>>)var3.next();

         for (Ticket ticket : (List)entry.getValue()) {
            if (ticketCheck.test(ticket)) {
               chunks.add(entry.getLongKey());
               break;
            }
         }
      }

      return chunks;
   }

   @FunctionalInterface
   public interface ChunkUpdated {
      void update(final long node, final int newLevelFrom, final boolean onlyDecreased);
   }

   public interface TicketPredicate {
      boolean test(Ticket ticket, long chunkPos);
   }
}
