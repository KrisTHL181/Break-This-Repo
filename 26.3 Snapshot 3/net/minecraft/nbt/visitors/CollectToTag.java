package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import org.jspecify.annotations.Nullable;

public class CollectToTag implements StreamTagVisitor {
   private final Deque<CollectToTag.ContainerBuilder> containerStack = new ArrayDeque<>();

   public CollectToTag() {
      this.containerStack.addLast(new CollectToTag.RootBuilder());
   }

   public @Nullable Tag getResult() {
      return this.containerStack.getFirst().build();
   }

   protected int depth() {
      return this.containerStack.size() - 1;
   }

   private void appendEntry(final Tag instance) {
      this.containerStack.getLast().acceptValue(instance);
   }

   @Override
   public StreamTagVisitor.ValueResult visitEnd() {
      this.appendEntry(EndTag.INSTANCE);
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final String value) {
      this.appendEntry(StringTag.valueOf(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final byte value) {
      this.appendEntry(ByteTag.valueOf(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final short value) {
      this.appendEntry(ShortTag.valueOf(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final int value) {
      this.appendEntry(IntTag.valueOf(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final long value) {
      this.appendEntry(LongTag.valueOf(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final float value) {
      this.appendEntry(FloatTag.valueOf(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final double value) {
      this.appendEntry(DoubleTag.valueOf(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final byte[] value) {
      this.appendEntry(new ByteArrayTag(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final int[] value) {
      this.appendEntry(new IntArrayTag(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(final long[] value) {
      this.appendEntry(new LongArrayTag(value));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visitList(final TagType<?> elementType, final int size) {
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.EntryResult visitElement(final TagType<?> type, final int index) {
      this.enterContainerIfNeeded(type);
      return StreamTagVisitor.EntryResult.ENTER;
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(final TagType<?> type) {
      return StreamTagVisitor.EntryResult.ENTER;
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(final TagType<?> type, final String id) {
      this.containerStack.getLast().acceptKey(id);
      this.enterContainerIfNeeded(type);
      return StreamTagVisitor.EntryResult.ENTER;
   }

   private void enterContainerIfNeeded(final TagType<?> type) {
      if (type == ListTag.TYPE) {
         this.containerStack.addLast(new CollectToTag.ListBuilder());
      } else if (type == CompoundTag.TYPE) {
         this.containerStack.addLast(new CollectToTag.CompoundBuilder());
      }
   }

   @Override
   public StreamTagVisitor.ValueResult visitContainerEnd() {
      CollectToTag.ContainerBuilder container = this.containerStack.removeLast();
      Tag tag = container.build();
      if (tag != null) {
         this.containerStack.getLast().acceptValue(tag);
      }

      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visitRootEntry(final TagType<?> type) {
      this.enterContainerIfNeeded(type);
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   private static class CompoundBuilder implements CollectToTag.ContainerBuilder {
      private final CompoundTag compound = new CompoundTag();
      private String lastId = "";

      @Override
      public void acceptKey(final String id) {
         this.lastId = id;
      }

      @Override
      public void acceptValue(final Tag tag) {
         this.compound.put(this.lastId, tag);
      }

      @Override
      public Tag build() {
         return this.compound;
      }
   }

   private interface ContainerBuilder {
      default void acceptKey(final String id) {
      }

      void acceptValue(Tag tag);

      @Nullable Tag build();
   }

   private static class ListBuilder implements CollectToTag.ContainerBuilder {
      private final ListTag list = new ListTag();

      @Override
      public void acceptValue(final Tag tag) {
         this.list.addAndUnwrap(tag);
      }

      @Override
      public Tag build() {
         return this.list;
      }
   }

   private static class RootBuilder implements CollectToTag.ContainerBuilder {
      private @Nullable Tag result;

      @Override
      public void acceptValue(final Tag tag) {
         this.result = tag;
      }

      @Override
      public @Nullable Tag build() {
         return this.result;
      }
   }
}
