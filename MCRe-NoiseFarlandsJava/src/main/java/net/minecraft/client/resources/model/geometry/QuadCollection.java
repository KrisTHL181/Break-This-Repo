package net.minecraft.client.resources.model.geometry;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class QuadCollection {
    public static final QuadCollection EMPTY = new QuadCollection(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    private static final int FLAGS_NOT_COMPUTED = -1;
    private final List<BakedQuad> all;
    private final List<BakedQuad> unculled;
    private final List<BakedQuad> north;
    private final List<BakedQuad> south;
    private final List<BakedQuad> east;
    private final List<BakedQuad> west;
    private final List<BakedQuad> up;
    private final List<BakedQuad> down;
    private int materialFlags = -1;

    private QuadCollection(
        final List<BakedQuad> all,
        final List<BakedQuad> unculled,
        final List<BakedQuad> north,
        final List<BakedQuad> south,
        final List<BakedQuad> east,
        final List<BakedQuad> west,
        final List<BakedQuad> up,
        final List<BakedQuad> down
    ) {
        this.all = all;
        this.unculled = unculled;
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
        this.up = up;
        this.down = down;
    }

    private static @BakedQuad.MaterialFlags int computeMaterialFlags(final List<BakedQuad> quads) {
        int flags = 0;

        for (BakedQuad quad : quads) {
            flags |= quad.materialInfo().flags();
        }

        return flags;
    }

    public List<BakedQuad> getQuads(final @Nullable Direction direction) {
        return switch (direction) {
            case null -> this.unculled;
            case NORTH -> this.north;
            case SOUTH -> this.south;
            case EAST -> this.east;
            case WEST -> this.west;
            case UP -> this.up;
            case DOWN -> this.down;
        };
    }

    public List<BakedQuad> getAll() {
        return this.all;
    }

    public @BakedQuad.MaterialFlags int materialFlags() {
        if (this.materialFlags == -1) {
            this.materialFlags = computeMaterialFlags(this.all);
        }

        return this.materialFlags;
    }

    public boolean hasMaterialFlag(final @BakedQuad.MaterialFlags int flag) {
        return (this.materialFlags() & flag) != 0;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
        private final Multimap<Direction, BakedQuad> culledFaces = ArrayListMultimap.create();

        public QuadCollection.Builder addCulledFace(final Direction direction, final BakedQuad quad) {
            this.culledFaces.put(direction, quad);
            return this;
        }

        public QuadCollection.Builder addUnculledFace(final BakedQuad quad) {
            this.unculledFaces.add(quad);
            return this;
        }

        public QuadCollection.Builder addAll(final QuadCollection quadCollection) {
            this.culledFaces.putAll(Direction.UP, quadCollection.up);
            this.culledFaces.putAll(Direction.DOWN, quadCollection.down);
            this.culledFaces.putAll(Direction.NORTH, quadCollection.north);
            this.culledFaces.putAll(Direction.SOUTH, quadCollection.south);
            this.culledFaces.putAll(Direction.EAST, quadCollection.east);
            this.culledFaces.putAll(Direction.WEST, quadCollection.west);
            this.unculledFaces.addAll(quadCollection.unculled);
            return this;
        }

        private static QuadCollection createFromSublists(
            final List<BakedQuad> all,
            final int unculledCount,
            final int northCount,
            final int southCount,
            final int eastCount,
            final int westCount,
            final int upCount,
            final int downCount
        ) {
            int index = 0;
            int var16;
            List<BakedQuad> unculled = all.subList(index, var16 = index + unculledCount);
            List<BakedQuad> north = all.subList(var16, index = var16 + northCount);
            int var18;
            List<BakedQuad> south = all.subList(index, var18 = index + southCount);
            List<BakedQuad> east = all.subList(var18, index = var18 + eastCount);
            int var20;
            List<BakedQuad> west = all.subList(index, var20 = index + westCount);
            List<BakedQuad> up = all.subList(var20, index = var20 + upCount);
            List<BakedQuad> down = all.subList(index, index + downCount);
            return new QuadCollection(all, unculled, north, south, east, west, up, down);
        }

        public QuadCollection build() {
            ImmutableList<BakedQuad> unculledFaces = this.unculledFaces.build();
            if (this.culledFaces.isEmpty()) {
                return unculledFaces.isEmpty()
                    ? QuadCollection.EMPTY
                    : new QuadCollection(unculledFaces, unculledFaces, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
            }

            ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
            quads.addAll(unculledFaces);
            Collection<BakedQuad> north = this.culledFaces.get(Direction.NORTH);
            quads.addAll(north);
            Collection<BakedQuad> south = this.culledFaces.get(Direction.SOUTH);
            quads.addAll(south);
            Collection<BakedQuad> east = this.culledFaces.get(Direction.EAST);
            quads.addAll(east);
            Collection<BakedQuad> west = this.culledFaces.get(Direction.WEST);
            quads.addAll(west);
            Collection<BakedQuad> up = this.culledFaces.get(Direction.UP);
            quads.addAll(up);
            Collection<BakedQuad> down = this.culledFaces.get(Direction.DOWN);
            quads.addAll(down);
            return createFromSublists(quads.build(), unculledFaces.size(), north.size(), south.size(), east.size(), west.size(), up.size(), down.size());
        }
    }
}