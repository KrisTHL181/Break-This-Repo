package net.minecraft.nbt.visitors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public class CollectFields extends CollectToTag {
   private int fieldsToGetCount;
   private final Set<TagType<?>> wantedTypes;
   private final Deque<FieldTree> stack = new ArrayDeque<>();

   public CollectFields(final FieldSelector... wantedFields) {
      this.fieldsToGetCount = wantedFields.length;
      Builder<TagType<?>> wantedTypes = ImmutableSet.builder();
      FieldTree rootFrame = FieldTree.createRoot();

      for (FieldSelector wantedField : wantedFields) {
         rootFrame.addEntry(wantedField);
         wantedTypes.add(wantedField.type());
      }

      this.stack.push(rootFrame);
      wantedTypes.add(CompoundTag.TYPE);
      this.wantedTypes = wantedTypes.build();
   }

   @Override
   public StreamTagVisitor.ValueResult visitRootEntry(final TagType<?> type) {
      return type != CompoundTag.TYPE ? StreamTagVisitor.ValueResult.HALT : super.visitRootEntry(type);
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(final TagType<?> type) {
      FieldTree currentFrame = this.stack.element();
      if (this.depth() > currentFrame.depth()) {
         return super.visitEntry(type);
      } else if (this.fieldsToGetCount <= 0) {
         return StreamTagVisitor.EntryResult.BREAK;
      } else {
         return !this.wantedTypes.contains(type) ? StreamTagVisitor.EntryResult.SKIP : super.visitEntry(type);
      }
   }

   @Override
   public StreamTagVisitor.EntryResult visitEntry(final TagType<?> type, final String id) {
      FieldTree currentFrame = this.stack.element();
      if (this.depth() > currentFrame.depth()) {
         return super.visitEntry(type, id);
      }

      if (currentFrame.selectedFields().remove(id, type)) {
         this.fieldsToGetCount--;
         return super.visitEntry(type, id);
      }

      if (type == CompoundTag.TYPE) {
         FieldTree newFrame = currentFrame.fieldsToRecurse().get(id);
         if (newFrame != null) {
            this.stack.push(newFrame);
            return super.visitEntry(type, id);
         }
      }

      return StreamTagVisitor.EntryResult.SKIP;
   }

   @Override
   public StreamTagVisitor.ValueResult visitContainerEnd() {
      if (this.depth() == this.stack.element().depth()) {
         this.stack.pop();
      }

      return super.visitContainerEnd();
   }

   public int getMissingFieldCount() {
      return this.fieldsToGetCount;
   }
}
