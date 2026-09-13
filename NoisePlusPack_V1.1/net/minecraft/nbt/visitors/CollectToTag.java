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

   private void appendEntry(Tag p_197683_) {
      this.containerStack.getLast().acceptValue(p_197683_);
   }

   @Override
   public StreamTagVisitor.ValueResult visitEnd() {
      this.appendEntry(EndTag.INSTANCE);
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(String p_197678_) {
      this.appendEntry(StringTag.valueOf(p_197678_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(byte p_197668_) {
      this.appendEntry(ByteTag.valueOf(p_197668_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(short p_197693_) {
      this.appendEntry(ShortTag.valueOf(p_197693_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(int p_197674_) {
      this.appendEntry(IntTag.valueOf(p_197674_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(long p_197676_) {
      this.appendEntry(LongTag.valueOf(p_197676_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(float p_197672_) {
      this.appendEntry(FloatTag.valueOf(p_197672_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(double p_197670_) {
      this.appendEntry(DoubleTag.valueOf(p_197670_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(byte[] p_197695_) {
      this.appendEntry(new ByteArrayTag(p_197695_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(int[] p_197697_) {
      this.appendEntry(new IntArrayTag(p_197697_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visit(long[] p_197699_) {
      this.appendEntry(new LongArrayTag(p_197699_));
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visitList(TagType<?> p_197687_, int p_197688_) {
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.EntryResult visitElement(TagType<?> p_197709_, int p_197710_) {
      this.enterContainerIfNeeded(p_197709_);
      return StreamTagVisitor.EntryResult.ENTER;
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(TagType<?> p_197685_) {
      return StreamTagVisitor.EntryResult.ENTER;
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(TagType<?> p_197690_, String p_197691_) {
      this.containerStack.getLast().acceptKey(p_197691_);
      this.enterContainerIfNeeded(p_197690_);
      return StreamTagVisitor.EntryResult.ENTER;
   }

   private void enterContainerIfNeeded(TagType<?> p_197712_) {
      if (p_197712_ == ListTag.TYPE) {
         this.containerStack.addLast(new CollectToTag.ListBuilder());
      } else if (p_197712_ == CompoundTag.TYPE) {
         this.containerStack.addLast(new CollectToTag.CompoundBuilder());
      }
   }

   @Override
   public StreamTagVisitor.ValueResult visitContainerEnd() {
      CollectToTag.ContainerBuilder collecttotag$containerbuilder = this.containerStack.removeLast();
      Tag tag = collecttotag$containerbuilder.build();
      if (tag != null) {
         this.containerStack.getLast().acceptValue(tag);
      }

      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   @Override
   public StreamTagVisitor.ValueResult visitRootEntry(TagType<?> p_197707_) {
      this.enterContainerIfNeeded(p_197707_);
      return StreamTagVisitor.ValueResult.CONTINUE;
   }

   static class CompoundBuilder implements CollectToTag.ContainerBuilder {
      private final CompoundTag compound = new CompoundTag();
      private String lastId = "";

      @Override
      public void acceptKey(String p_392161_) {
         this.lastId = p_392161_;
      }

      @Override
      public void acceptValue(Tag p_393993_) {
         this.compound.put(this.lastId, p_393993_);
      }

      @Override
      public Tag build() {
         return this.compound;
      }
   }

   interface ContainerBuilder {
      default void acceptKey(String p_396233_) {
      }

      void acceptValue(Tag var1);

      @Nullable Tag build();
   }

   static class ListBuilder implements CollectToTag.ContainerBuilder {
      private final ListTag list = new ListTag();

      @Override
      public void acceptValue(Tag p_397294_) {
         this.list.addAndUnwrap(p_397294_);
      }

      @Override
      public Tag build() {
         return this.list;
      }
   }

   static class RootBuilder implements CollectToTag.ContainerBuilder {
      private @Nullable Tag result;

      @Override
      public void acceptValue(Tag p_392906_) {
         this.result = p_392906_;
      }

      @Override
      public @Nullable Tag build() {
         return this.result;
      }
   }
}
