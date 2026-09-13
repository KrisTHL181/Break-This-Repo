package net.minecraft.client;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RotatingSectionStorage<T extends RotatingSectionStorage.Value> implements Iterable<T> {
    private final RotatingSectionStorage.Node<T>[] nodes;
    private final int radius;
    private final int minY;
    private final int maxY;
    private final int sectionGridSizeY;
    private final int sectionGridSizeXZ;
    private SectionPos centerSectionPos = SectionPos.of(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    public RotatingSectionStorage(final int radius, final int minY, final int maxY, final RotatingSectionStorage.ValueCreator<T> valueCreator) {
        this.radius = radius;
        this.minY = minY;
        this.maxY = maxY;
        this.sectionGridSizeY = maxY - minY + 1;
        this.sectionGridSizeXZ = radius * 2 + 1;
        int totalSections = this.sectionGridSizeXZ * this.sectionGridSizeXZ * this.sectionGridSizeY;
        this.nodes = new RotatingSectionStorage.Node[totalSections];

        for (int x = 0; x < this.sectionGridSizeXZ; x++) {
            for (int y = 0; y < this.sectionGridSizeY; y++) {
                for (int z = 0; z < this.sectionGridSizeXZ; z++) {
                    int index = this.getSectionIndex(x, y, z);
                    SectionPos sectionNode = SectionPos.of(x, y + minY, z);
                    this.nodes[index] = new RotatingSectionStorage.Node<>(valueCreator.createValue(index, sectionNode));
                }
            }
        }
    }

    public boolean repositionCenter(final SectionPos newCenterSectionPos) {
        if (newCenterSectionPos.equals(this.centerSectionPos)) {
            return false;
        }

        int lowestX = newCenterSectionPos.x() - this.radius;
        int lowestZ = newCenterSectionPos.z() - this.radius;

        for (int gridX = 0; gridX < this.sectionGridSizeXZ; gridX++) {
            int newSectionX = lowestX + Math.floorMod(gridX - lowestX, this.sectionGridSizeXZ);

            for (int gridZ = 0; gridZ < this.sectionGridSizeXZ; gridZ++) {
                int newSectionZ = lowestZ + Math.floorMod(gridZ - lowestZ, this.sectionGridSizeXZ);

                for (int gridY = 0; gridY < this.sectionGridSizeY; gridY++) {
                    int newSectionY = this.minY + gridY;
                    T value = this.nodes[this.getSectionIndex(gridX, gridY, gridZ)].value;
                    SectionPos sectionNode = value.getSectionNode();
                    if (!sectionNode.equals(SectionPos.of(newSectionX, newSectionY, newSectionZ))) {
                        value.setSectionNode(SectionPos.of(newSectionX, newSectionY, newSectionZ));
                    }
                }
            }
        }

        this.centerSectionPos = newCenterSectionPos;
        return true;
    }

    public int radius() {
        return this.radius;
    }

    public int minY() {
        return this.minY;
    }

    public int maxY() {
        return this.maxY;
    }

    public int height() {
        return this.sectionGridSizeY;
    }

    public SectionPos centerSectionPos() {
        return this.centerSectionPos;
    }

    public @Nullable T getValueAt(final BlockPos pos) {
        return this.getValue(SectionPos.of(pos));
    }

    public @Nullable T getValue(final SectionPos sectionNode) {
        int sectionX = sectionNode.x();
        int sectionY = sectionNode.y();
        int sectionZ = sectionNode.z();
        return this.getValue(sectionX, sectionY, sectionZ);
    }

    public @Nullable T getValue(final int sectionX, final int sectionY, final int sectionZ) {
        if (!this.containsSection(sectionX, sectionY, sectionZ)) {
            return null;
        }

        int y = sectionY - this.minY;
        int x = Math.floorMod(sectionX, this.sectionGridSizeXZ);
        int z = Math.floorMod(sectionZ, this.sectionGridSizeXZ);
        return this.nodes[this.getSectionIndex(x, y, z)].value;
    }

    private boolean containsSection(final int sectionX, final int sectionY, final int sectionZ) {
        if (sectionY >= this.minY && sectionY <= this.maxY) {
            return sectionX < this.centerSectionPos.x() - this.radius || sectionX > this.centerSectionPos.x() + this.radius
                ? false
                : sectionZ >= this.centerSectionPos.z() - this.radius && sectionZ <= this.centerSectionPos.z() + this.radius;
        } else {
            return false;
        }
    }

    private int getSectionIndex(final int x, final int y, final int z) {
        return (z * this.sectionGridSizeY + y) * this.sectionGridSizeXZ + x;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int i;

            @Override
            public boolean hasNext() {
                return this.i < RotatingSectionStorage.this.nodes.length - 1;
            }

            public T next() {
                if (this.i >= RotatingSectionStorage.this.nodes.length) {
                    throw new NoSuchElementException();
                } else {
                    return RotatingSectionStorage.this.nodes[this.i++].value;
                }
            }
        };
    }

    @Override
    public void forEach(final Consumer<? super T> action) {
        for (RotatingSectionStorage.Node<T> node : this.nodes) {
            action.accept(node.value);
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(this.iterator(), this.nodes.length, 0);
    }

    public int size() {
        return this.nodes.length;
    }

    @OnlyIn(Dist.CLIENT)
    private record Node<T>(T value) {
    }

    @OnlyIn(Dist.CLIENT)
    public interface Value {
        void setSectionNode(SectionPos sectionNode);

        SectionPos getSectionNode();
    }

    @OnlyIn(Dist.CLIENT)
    public interface ValueCreator<T extends RotatingSectionStorage.Value> {
        T createValue(int index, SectionPos sectionNode);
    }
}