package net.minecraft.commands.arguments;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class NbtPathArgument implements ArgumentType<NbtPathArgument.NbtPath> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo.bar", "foo[0]", "[0]", "[]", "{foo=bar}");
   public static final SimpleCommandExceptionType ERROR_INVALID_NODE = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.node.invalid"));
   public static final SimpleCommandExceptionType ERROR_DATA_TOO_DEEP = new SimpleCommandExceptionType(Component.translatable("arguments.nbtpath.too_deep"));
   public static final DynamicCommandExceptionType ERROR_NOTHING_FOUND = new DynamicCommandExceptionType(
      path -> Component.translatableEscape("arguments.nbtpath.nothing_found", path)
   );
   private static final DynamicCommandExceptionType ERROR_EXPECTED_LIST = new DynamicCommandExceptionType(
      node -> Component.translatableEscape("commands.data.modify.expected_list", node)
   );
   private static final DynamicCommandExceptionType ERROR_INVALID_INDEX = new DynamicCommandExceptionType(
      node -> Component.translatableEscape("commands.data.modify.invalid_index", node)
   );
   private static final char INDEX_MATCH_START = '[';
   private static final char INDEX_MATCH_END = ']';
   private static final char KEY_MATCH_START = '{';
   private static final char KEY_MATCH_END = '}';
   private static final char QUOTED_KEY_START = '"';
   private static final char SINGLE_QUOTED_KEY_START = '\'';

   public static NbtPathArgument nbtPath() {
      return new NbtPathArgument();
   }

   public static NbtPathArgument.NbtPath getPath(final CommandContext<CommandSourceStack> context, final String name) {
      return (NbtPathArgument.NbtPath)context.getArgument(name, NbtPathArgument.NbtPath.class);
   }

   public NbtPathArgument.NbtPath parse(final StringReader reader) throws CommandSyntaxException {
      List<NbtPathArgument.Node> nodes = Lists.newArrayList();
      int start = reader.getCursor();
      Object2IntMap<NbtPathArgument.Node> nodeToOriginalPosition = new Object2IntOpenHashMap();
      boolean firstNode = true;

      while (reader.canRead() && reader.peek() != ' ') {
         NbtPathArgument.Node node = parseNode(reader, firstNode);
         nodes.add(node);
         nodeToOriginalPosition.put(node, reader.getCursor() - start);
         firstNode = false;
         if (reader.canRead()) {
            char next = reader.peek();
            if (next != ' ' && next != '[' && next != '{') {
               reader.expect('.');
            }
         }
      }

      return new NbtPathArgument.NbtPath(
         reader.getString().substring(start, reader.getCursor()), nodes.toArray(new NbtPathArgument.Node[0]), nodeToOriginalPosition
      );
   }

   private static NbtPathArgument.Node parseNode(final StringReader reader, final boolean firstNode) throws CommandSyntaxException {
      return switch (reader.peek()) {
         case '"', '\'' -> readObjectNode(reader, reader.readString());
         case '[' -> {
            reader.skip();
            int next = reader.peek();
            if (next == 123) {
               CompoundTag pattern = TagParser.parseCompoundAsArgument(reader);
               reader.expect(']');
               yield new NbtPathArgument.MatchElementNode(pattern);
            } else if (next == 93) {
               reader.skip();
               yield NbtPathArgument.AllElementsNode.INSTANCE;
            } else {
               int index = reader.readInt();
               reader.expect(']');
               yield new NbtPathArgument.IndexedElementNode(index);
            }
         }
         case '{' -> {
            if (!firstNode) {
               throw ERROR_INVALID_NODE.createWithContext(reader);
            }

            CompoundTag pattern = TagParser.parseCompoundAsArgument(reader);
            yield new NbtPathArgument.MatchRootObjectNode(pattern);
         }
         default -> readObjectNode(reader, readUnquotedName(reader));
      };
   }

   private static NbtPathArgument.Node readObjectNode(final StringReader reader, final String name) throws CommandSyntaxException {
      if (name.isEmpty()) {
         throw ERROR_INVALID_NODE.createWithContext(reader);
      } else if (reader.canRead() && reader.peek() == '{') {
         CompoundTag pattern = TagParser.parseCompoundAsArgument(reader);
         return new NbtPathArgument.MatchObjectNode(name, pattern);
      } else {
         return new NbtPathArgument.CompoundChildNode(name);
      }
   }

   private static String readUnquotedName(final StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();

      while (reader.canRead() && isAllowedInUnquotedName(reader.peek())) {
         reader.skip();
      }

      if (reader.getCursor() == start) {
         throw ERROR_INVALID_NODE.createWithContext(reader);
      } else {
         return reader.getString().substring(start, reader.getCursor());
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static boolean isAllowedInUnquotedName(final char c) {
      return c != ' ' && c != '"' && c != '\'' && c != '[' && c != ']' && c != '.' && c != '{' && c != '}';
   }

   private static Predicate<Tag> createTagPredicate(final CompoundTag pattern) {
      return tag -> NbtUtils.compareNbt(pattern, tag, true);
   }

   private static class AllElementsNode implements NbtPathArgument.Node {
      public static final NbtPathArgument.AllElementsNode INSTANCE = new NbtPathArgument.AllElementsNode();

      @Override
      public void getTag(final Tag parent, final List<Tag> output) {
         if (parent instanceof CollectionTag collection) {
            Iterables.addAll(output, collection);
         }
      }

      @Override
      public void getOrCreateTag(final Tag parent, final Supplier<Tag> child, final List<Tag> output) {
         if (parent instanceof CollectionTag list) {
            if (list.isEmpty()) {
               Tag result = child.get();
               if (list.addTag(0, result)) {
                  output.add(result);
               }
            } else {
               Iterables.addAll(output, list);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(final Tag parent, final Supplier<Tag> toAdd) {
         if (!(parent instanceof CollectionTag list)) {
            return 0;
         } else {
            int size = list.size();
            if (size == 0) {
               list.addTag(0, toAdd.get());
               return 1;
            }

            Tag newValue = toAdd.get();
            int changedCount = size - (int)list.stream().filter(newValue::equals).count();
            if (changedCount == 0) {
               return 0;
            }

            list.clear();
            if (!list.addTag(0, newValue)) {
               return 0;
            }

            for (int i = 1; i < size; i++) {
               list.addTag(i, toAdd.get());
            }

            return changedCount;
         }
      }

      @Override
      public int removeTag(final Tag parent) {
         if (parent instanceof CollectionTag list) {
            int size = list.size();
            if (size > 0) {
               list.clear();
               return size;
            }
         }

         return 0;
      }
   }

   private static class CompoundChildNode implements NbtPathArgument.Node {
      private final String name;

      public CompoundChildNode(final String name) {
         this.name = name;
      }

      @Override
      public void getTag(final Tag parent, final List<Tag> output) {
         if (parent instanceof CompoundTag compoundTag) {
            Tag result = compoundTag.get(this.name);
            if (result != null) {
               output.add(result);
            }
         }
      }

      @Override
      public void getOrCreateTag(final Tag parent, final Supplier<Tag> child, final List<Tag> output) {
         if (parent instanceof CompoundTag compound) {
            Tag result;
            if (compound.contains(this.name)) {
               result = compound.get(this.name);
            } else {
               result = child.get();
               compound.put(this.name, result);
            }

            output.add(result);
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(final Tag parent, final Supplier<Tag> toAdd) {
         if (parent instanceof CompoundTag compound) {
            Tag newValue = toAdd.get();
            Tag previousValue = compound.put(this.name, newValue);
            if (!newValue.equals(previousValue)) {
               return 1;
            }
         }

         return 0;
      }

      @Override
      public int removeTag(final Tag parent) {
         if (parent instanceof CompoundTag compound && compound.contains(this.name)) {
            compound.remove(this.name);
            return 1;
         } else {
            return 0;
         }
      }
   }

   private static class IndexedElementNode implements NbtPathArgument.Node {
      private final int index;

      public IndexedElementNode(final int index) {
         this.index = index;
      }

      @Override
      public void getTag(final Tag parent, final List<Tag> output) {
         if (parent instanceof CollectionTag list) {
            int size = list.size();
            int actualIndex = this.index < 0 ? size + this.index : this.index;
            if (0 <= actualIndex && actualIndex < size) {
               output.add(list.get(actualIndex));
            }
         }
      }

      @Override
      public void getOrCreateTag(final Tag parent, final Supplier<Tag> child, final List<Tag> output) {
         this.getTag(parent, output);
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(final Tag parent, final Supplier<Tag> toAdd) {
         if (parent instanceof CollectionTag list) {
            int size = list.size();
            int actualIndex = this.index < 0 ? size + this.index : this.index;
            if (0 <= actualIndex && actualIndex < size) {
               Tag previousValue = list.get(actualIndex);
               Tag newValue = toAdd.get();
               if (!newValue.equals(previousValue) && list.setTag(actualIndex, newValue)) {
                  return 1;
               }
            }
         }

         return 0;
      }

      @Override
      public int removeTag(final Tag parent) {
         if (parent instanceof CollectionTag list) {
            int size = list.size();
            int actualIndex = this.index < 0 ? size + this.index : this.index;
            if (0 <= actualIndex && actualIndex < size) {
               list.remove(actualIndex);
               return 1;
            }
         }

         return 0;
      }
   }

   private static class MatchElementNode implements NbtPathArgument.Node {
      private final CompoundTag pattern;
      private final Predicate<Tag> predicate;

      public MatchElementNode(final CompoundTag pattern) {
         this.pattern = pattern;
         this.predicate = NbtPathArgument.createTagPredicate(pattern);
      }

      @Override
      public void getTag(final Tag parent, final List<Tag> output) {
         if (parent instanceof ListTag list) {
            list.stream().filter(this.predicate).forEach(output::add);
         }
      }

      @Override
      public void getOrCreateTag(final Tag parent, final Supplier<Tag> child, final List<Tag> output) {
         MutableBoolean foundAnything = new MutableBoolean();
         if (parent instanceof ListTag list) {
            list.stream().filter(this.predicate).forEach(t -> {
               output.add(t);
               foundAnything.setTrue();
            });
            if (foundAnything.isFalse()) {
               CompoundTag newTag = this.pattern.copy();
               list.add(newTag);
               output.add(newTag);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new ListTag();
      }

      @Override
      public int setTag(final Tag parent, final Supplier<Tag> toAdd) {
         int changedCount = 0;
         if (parent instanceof ListTag list) {
            int size = list.size();
            if (size == 0) {
               list.add(toAdd.get());
               changedCount++;
            } else {
               for (int i = 0; i < size; i++) {
                  Tag currentValue = list.get(i);
                  if (this.predicate.test(currentValue)) {
                     Tag newValue = toAdd.get();
                     if (!newValue.equals(currentValue) && list.setTag(i, newValue)) {
                        changedCount++;
                     }
                  }
               }
            }
         }

         return changedCount;
      }

      @Override
      public int removeTag(final Tag parent) {
         int changedCount = 0;
         if (parent instanceof ListTag list) {
            for (int i = list.size() - 1; i >= 0; i--) {
               if (this.predicate.test(list.get(i))) {
                  list.remove(i);
                  changedCount++;
               }
            }
         }

         return changedCount;
      }
   }

   private static class MatchObjectNode implements NbtPathArgument.Node {
      private final String name;
      private final CompoundTag pattern;
      private final Predicate<Tag> predicate;

      public MatchObjectNode(final String name, final CompoundTag pattern) {
         this.name = name;
         this.pattern = pattern;
         this.predicate = NbtPathArgument.createTagPredicate(pattern);
      }

      @Override
      public void getTag(final Tag parent, final List<Tag> output) {
         if (parent instanceof CompoundTag compoundTag) {
            Tag result = compoundTag.get(this.name);
            if (this.predicate.test(result)) {
               output.add(result);
            }
         }
      }

      @Override
      public void getOrCreateTag(final Tag parent, final Supplier<Tag> child, final List<Tag> output) {
         if (parent instanceof CompoundTag compound) {
            Tag result = compound.get(this.name);
            if (result == null) {
               result = this.pattern.copy();
               compound.put(this.name, result);
               output.add(result);
            } else if (this.predicate.test(result)) {
               output.add(result);
            }
         }
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(final Tag parent, final Supplier<Tag> toAdd) {
         if (parent instanceof CompoundTag compound) {
            Tag currentValue = compound.get(this.name);
            if (this.predicate.test(currentValue)) {
               Tag newValue = toAdd.get();
               if (!newValue.equals(currentValue)) {
                  compound.put(this.name, newValue);
                  return 1;
               }
            }
         }

         return 0;
      }

      @Override
      public int removeTag(final Tag parent) {
         if (parent instanceof CompoundTag compound) {
            Tag current = compound.get(this.name);
            if (this.predicate.test(current)) {
               compound.remove(this.name);
               return 1;
            }
         }

         return 0;
      }
   }

   private static class MatchRootObjectNode implements NbtPathArgument.Node {
      private final Predicate<Tag> predicate;

      public MatchRootObjectNode(final CompoundTag pattern) {
         this.predicate = NbtPathArgument.createTagPredicate(pattern);
      }

      @Override
      public void getTag(final Tag self, final List<Tag> output) {
         if (self instanceof CompoundTag && this.predicate.test(self)) {
            output.add(self);
         }
      }

      @Override
      public void getOrCreateTag(final Tag self, final Supplier<Tag> child, final List<Tag> output) {
         this.getTag(self, output);
      }

      @Override
      public Tag createPreferredParentTag() {
         return new CompoundTag();
      }

      @Override
      public int setTag(final Tag parent, final Supplier<Tag> toAdd) {
         return 0;
      }

      @Override
      public int removeTag(final Tag parent) {
         return 0;
      }
   }

   public static class NbtPath {
      private final String original;
      private final Object2IntMap<NbtPathArgument.Node> nodeToOriginalPosition;
      private final NbtPathArgument.Node[] nodes;
      public static final Codec<NbtPathArgument.NbtPath> CODEC = Codec.STRING.comapFlatMap(string -> {
         try {
            NbtPathArgument.NbtPath parsed = new NbtPathArgument().parse(new StringReader(string));
            return DataResult.success(parsed);
         } catch (CommandSyntaxException e) {
            return DataResult.error(() -> "Failed to parse path " + string + ": " + e.getMessage());
         }
      }, NbtPathArgument.NbtPath::asString);

      public static NbtPathArgument.NbtPath of(final String string) throws CommandSyntaxException {
         return new NbtPathArgument().parse(new StringReader(string));
      }

      public NbtPath(final String original, final NbtPathArgument.Node[] nodes, final Object2IntMap<NbtPathArgument.Node> nodeToOriginalPosition) {
         this.original = original;
         this.nodes = nodes;
         this.nodeToOriginalPosition = nodeToOriginalPosition;
      }

      public List<Tag> get(final Tag tag) throws CommandSyntaxException {
         List<Tag> result = Collections.singletonList(tag);

         for (NbtPathArgument.Node node : this.nodes) {
            result = node.get(result);
            if (result.isEmpty()) {
               throw this.createNotFoundException(node);
            }
         }

         return result;
      }

      public int countMatching(final Tag tag) {
         List<Tag> result = Collections.singletonList(tag);

         for (NbtPathArgument.Node node : this.nodes) {
            result = node.get(result);
            if (result.isEmpty()) {
               return 0;
            }
         }

         return result.size();
      }

      private List<Tag> getOrCreateParents(final Tag tag) throws CommandSyntaxException {
         List<Tag> result = Collections.singletonList(tag);

         for (int i = 0; i < this.nodes.length - 1; i++) {
            NbtPathArgument.Node node = this.nodes[i];
            int next = i + 1;
            result = node.getOrCreate(result, this.nodes[next]::createPreferredParentTag);
            if (result.isEmpty()) {
               throw this.createNotFoundException(node);
            }
         }

         return result;
      }

      public List<Tag> getOrCreate(final Tag tag, final Supplier<Tag> newTagValue) throws CommandSyntaxException {
         List<Tag> result = this.getOrCreateParents(tag);
         NbtPathArgument.Node lastNode = this.nodes[this.nodes.length - 1];
         return lastNode.getOrCreate(result, newTagValue);
      }

      private static int apply(final List<Tag> targets, final Function<Tag, Integer> operation) {
         return targets.stream().map(operation).reduce(0, (a, b) -> a + b);
      }

      public static boolean isTooDeep(final Tag tag, final int depth) {
         if (depth >= 512) {
            return true;
         }

         if (tag instanceof CompoundTag compound) {
            for (Tag child : compound.values()) {
               if (isTooDeep(child, depth + 1)) {
                  return true;
               }
            }
         } else if (tag instanceof ListTag) {
            for (Tag listEntry : (ListTag)tag) {
               if (isTooDeep(listEntry, depth + 1)) {
                  return true;
               }
            }
         }

         return false;
      }

      public int set(final Tag tag, final Tag toAdd) throws CommandSyntaxException {
         if (isTooDeep(toAdd, this.estimatePathDepth())) {
            throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
         }

         Tag firstCopy = toAdd.copy();
         List<Tag> result = this.getOrCreateParents(tag);
         if (result.isEmpty()) {
            return 0;
         }

         NbtPathArgument.Node lastNode = this.nodes[this.nodes.length - 1];
         MutableBoolean usedFirstCopy = new MutableBoolean(false);
         return apply(result, t -> lastNode.setTag(t, () -> {
            if (usedFirstCopy.isFalse()) {
               usedFirstCopy.setTrue();
               return firstCopy;
            } else {
               return firstCopy.copy();
            }
         }));
      }

      private int estimatePathDepth() {
         return this.nodes.length;
      }

      public int insert(final int index, final CompoundTag target, final List<Tag> toInsert) throws CommandSyntaxException {
         List<Tag> toInsertCopy = new ArrayList<>(toInsert.size());

         for (Tag tag : toInsert) {
            Tag copy = tag.copy();
            toInsertCopy.add(copy);
            if (isTooDeep(copy, this.estimatePathDepth())) {
               throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
            }
         }

         Collection<Tag> targets = this.getOrCreate(target, ListTag::new);
         int modifiedCount = 0;
         boolean usedFirst = false;

         for (Tag targetTag : targets) {
            if (!(targetTag instanceof CollectionTag targetList)) {
               throw NbtPathArgument.ERROR_EXPECTED_LIST.create(targetTag);
            }

            boolean modified = false;
            int actualIndex = index < 0 ? targetList.size() + index + 1 : index;

            for (Tag sourceTag : toInsertCopy) {
               try {
                  if (targetList.addTag(actualIndex, usedFirst ? sourceTag.copy() : sourceTag)) {
                     actualIndex++;
                     modified = true;
                  }
               } catch (IndexOutOfBoundsException e) {
                  throw NbtPathArgument.ERROR_INVALID_INDEX.create(actualIndex);
               }
            }

            usedFirst = true;
            modifiedCount += modified ? 1 : 0;
         }

         return modifiedCount;
      }

      public int remove(final Tag tag) {
         List<Tag> result = Collections.singletonList(tag);

         for (int i = 0; i < this.nodes.length - 1; i++) {
            result = this.nodes[i].get(result);
         }

         NbtPathArgument.Node lastNode = this.nodes[this.nodes.length - 1];
         return apply(result, lastNode::removeTag);
      }

      private CommandSyntaxException createNotFoundException(final NbtPathArgument.Node node) {
         int index = this.nodeToOriginalPosition.getInt(node);
         return NbtPathArgument.ERROR_NOTHING_FOUND.create(this.original.substring(0, index));
      }

      @Override
      public String toString() {
         return this.original;
      }

      public String asString() {
         return this.original;
      }
   }

   private interface Node {
      void getTag(Tag parent, final List<Tag> output);

      void getOrCreateTag(Tag parent, Supplier<Tag> child, final List<Tag> output);

      Tag createPreferredParentTag();

      int setTag(Tag parent, Supplier<Tag> toAdd);

      int removeTag(Tag parent);

      default List<Tag> get(final List<Tag> tags) {
         return this.collect(tags, this::getTag);
      }

      default List<Tag> getOrCreate(final List<Tag> tags, final Supplier<Tag> child) {
         return this.collect(tags, (tag, output) -> this.getOrCreateTag(tag, child, output));
      }

      default List<Tag> collect(final List<Tag> tags, final BiConsumer<Tag, List<Tag>> collector) {
         List<Tag> result = Lists.newArrayList();

         for (Tag tag : tags) {
            collector.accept(tag, result);
         }

         return result;
      }
   }
}
