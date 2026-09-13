package net.minecraft.world.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

/**
 * TicketStorage — 区块票存储（MCRe NoiseFarlands 对象化版）
 * 原版以 long 打包键（ChunkPos.pack），本版直接用 ChunkPos 对象为键。
 */
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
    private final HashMap<ChunkPos, List<Ticket>> tickets;
    private final HashMap<ChunkPos, List<Ticket>> deactivatedTickets;
    private Set<ChunkPos> chunksWithForcedTickets = new HashSet<>();
    private TicketStorage.@Nullable ChunkUpdated loadingChunkUpdatedListener;
    private TicketStorage.@Nullable ChunkUpdated simulationChunkUpdatedListener;

    private TicketStorage(final HashMap<ChunkPos, List<Ticket>> tickets, final HashMap<ChunkPos, List<Ticket>> deactivatedTickets) {
        this.tickets = tickets;
        this.deactivatedTickets = deactivatedTickets;
        this.updateForcedChunks();
    }

    public TicketStorage() {
        this(new HashMap<>(4), new HashMap<>());
    }

    private static TicketStorage fromPacked(final List<Pair<ChunkPos, Ticket>> tickets) {
        HashMap<ChunkPos, List<Ticket>> ticketsToLoad = new HashMap<>();

        for (Pair<ChunkPos, Ticket> ticket : tickets) {
            ChunkPos pos = ticket.getFirst();
            List<Ticket> ticketsInChunk = ticketsToLoad.computeIfAbsent(pos, k -> new ObjectArrayList<>(4));
            ticketsInChunk.add(ticket.getSecond());
        }

        return new TicketStorage(new HashMap<>(4), ticketsToLoad);
    }

    private List<Pair<ChunkPos, Ticket>> packTickets() {
        List<Pair<ChunkPos, Ticket>> tickets = new ArrayList<>();
        this.forEachTicket((pos, ticket) -> {
            if (ticket.getType().persist()) {
                tickets.add(new Pair<>(pos, ticket));
            }
        });
        return tickets;
    }

    private void forEachTicket(final BiConsumer<ChunkPos, Ticket> output) {
        forEachTicket(output, this.tickets);
        forEachTicket(output, this.deactivatedTickets);
    }

    private static void forEachTicket(final BiConsumer<ChunkPos, Ticket> output, final HashMap<ChunkPos, List<Ticket>> tickets) {
        for (Map.Entry<ChunkPos, List<Ticket>> entry : tickets.entrySet()) {
            ChunkPos chunkPos = entry.getKey();

            for (Ticket ticket : entry.getValue()) {
                output.accept(chunkPos, ticket);
            }
        }
    }

    public void activateAllDeactivatedTickets() {
        for (Map.Entry<ChunkPos, List<Ticket>> entry : this.deactivatedTickets.entrySet()) {
            for (Ticket ticket : entry.getValue()) {
                this.addTicket(entry.getKey(), ticket);
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
        for (List<Ticket> group : this.tickets.values()) {
            for (Ticket ticket : group) {
                if (ticket.getType().shouldKeepDimensionActive()) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<Ticket> getTickets(final ChunkPos key) {
        return this.tickets.getOrDefault(key, List.of());
    }

    private List<Ticket> getOrCreateTickets(final ChunkPos key) {
        return this.tickets.computeIfAbsent(key, k -> new ObjectArrayList<>(4));
    }

    public void addTicketWithRadius(final TicketType type, final ChunkPos chunkPos, final int radius) {
        Ticket ticket = new Ticket(type, ChunkLevel.byStatus(FullChunkStatus.FULL) - radius);
        this.addTicket(chunkPos, ticket);
    }

    public void addTicket(final Ticket ticket, final ChunkPos chunkPos) {
        this.addTicket(chunkPos, ticket);
    }

    public boolean addTicket(final ChunkPos key, final Ticket ticket) {
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
            LOGGER.debug("ATI {} {}", key, ticket);
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

    public int getTicketLevelAt(final ChunkPos key, final boolean simulation) {
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
        this.removeTicket(chunkPos, ticket);
    }

    public void removeTicket(final Ticket ticket, final ChunkPos chunkPos) {
        this.removeTicket(chunkPos, ticket);
    }

    public boolean removeTicket(final ChunkPos key, final Ticket ticket) {
        List<Ticket> tickets = this.tickets.get(key);
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
                    LOGGER.debug("RTI {} {}", key, t);
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

    public String getTicketDebugString(final ChunkPos key, final boolean simulation) {
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

    private boolean canTicketExpire(final ChunkMap chunkMap, final Ticket ticket, final ChunkPos chunkPos) {
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

    public void removeTicketIf(final TicketStorage.TicketPredicate predicate, final @Nullable HashMap<ChunkPos, List<Ticket>> removedTickets) {
        Iterator<Map.Entry<ChunkPos, List<Ticket>>> ticketsPerChunkIterator = this.tickets.entrySet().iterator();
        boolean removedForced = false;

        while (ticketsPerChunkIterator.hasNext()) {
            Map.Entry<ChunkPos, List<Ticket>> entry = ticketsPerChunkIterator.next();
            Iterator<Ticket> chunkTicketsIterator = entry.getValue().iterator();
            ChunkPos chunkPos = entry.getKey();
            boolean removedSimulation = false;
            boolean removedLoading = false;

            while (chunkTicketsIterator.hasNext()) {
                Ticket ticket = chunkTicketsIterator.next();
                if (predicate.test(ticket, chunkPos)) {
                    if (removedTickets != null) {
                        List<Ticket> tickets = removedTickets.computeIfAbsent(chunkPos, k -> new ObjectArrayList<>(entry.getValue().size()));
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
                    this.loadingChunkUpdatedListener.update(chunkPos, getTicketLevelAt(entry.getValue(), false), false);
                }

                if (removedSimulation && this.simulationChunkUpdatedListener != null) {
                    this.simulationChunkUpdatedListener.update(chunkPos, getTicketLevelAt(entry.getValue(), true), false);
                }

                this.setDirty();
                if (entry.getValue().isEmpty()) {
                    ticketsPerChunkIterator.remove();
                }
            }
        }

        if (removedForced) {
            this.updateForcedChunks();
        }
    }

    public void replaceTicketLevelOfType(final int newLevel, final TicketType ticketType) {
        List<Pair<Ticket, ChunkPos>> affectedTickets = new ArrayList<>();

        for (Map.Entry<ChunkPos, List<Ticket>> entry : this.tickets.entrySet()) {
            for (Ticket ticket : entry.getValue()) {
                if (ticket.getType() == ticketType) {
                    affectedTickets.add(Pair.of(ticket, entry.getKey()));
                }
            }
        }

        for (Pair<Ticket, ChunkPos> pair : affectedTickets) {
            ChunkPos key = pair.getSecond();
            Ticket ticket = pair.getFirst();
            this.removeTicket(key, ticket);
            TicketType type = ticket.getType();
            this.addTicket(key, new Ticket(type, newLevel));
        }
    }

    public boolean updateChunkForced(final ChunkPos chunkPos, final boolean forced) {
        Ticket ticket = new Ticket(TicketType.FORCED, ChunkMap.FORCED_TICKET_LEVEL);
        return forced ? this.addTicket(chunkPos, ticket) : this.removeTicket(chunkPos, ticket);
    }

    public Set<ChunkPos> getForceLoadedChunks() {
        return this.chunksWithForcedTickets;
    }

    private Set<ChunkPos> getAllChunksWithTicketThat(final Predicate<Ticket> ticketCheck) {
        HashSet<ChunkPos> chunks = new HashSet<>();

        for (Map.Entry<ChunkPos, List<Ticket>> entry : this.tickets.entrySet()) {
            for (Ticket ticket : entry.getValue()) {
                if (ticketCheck.test(ticket)) {
                    chunks.add(entry.getKey());
                    break;
                }
            }
        }

        return chunks;
    }

    @FunctionalInterface
    public interface ChunkUpdated {
        void update(final ChunkPos node, final int newLevelFrom, final boolean onlyDecreased);
    }

    public interface TicketPredicate {
        boolean test(Ticket ticket, ChunkPos chunkPos);
    }
}
