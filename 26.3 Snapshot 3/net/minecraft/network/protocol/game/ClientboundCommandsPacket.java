package net.minecraft.network.protocol.game;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.BiPredicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ClientboundCommandsPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundCommandsPacket> STREAM_CODEC = Packet.codec(
      ClientboundCommandsPacket::write, ClientboundCommandsPacket::new
   );
   private static final byte MASK_TYPE = 3;
   private static final byte FLAG_EXECUTABLE = 4;
   private static final byte FLAG_REDIRECT = 8;
   private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
   private static final byte FLAG_RESTRICTED = 32;
   private static final byte TYPE_ROOT = 0;
   private static final byte TYPE_LITERAL = 1;
   private static final byte TYPE_ARGUMENT = 2;
   private final int rootIndex;
   private final List<ClientboundCommandsPacket.Entry> entries;

   public <S> ClientboundCommandsPacket(final RootCommandNode<S> root, final ClientboundCommandsPacket.NodeInspector<S> inspector) {
      Object2IntMap<CommandNode<S>> nodeToId = enumerateNodes(root);
      this.entries = createEntries(nodeToId, inspector);
      this.rootIndex = nodeToId.getInt(root);
   }

   private ClientboundCommandsPacket(final FriendlyByteBuf input) {
      this.entries = input.readList(ClientboundCommandsPacket::readNode);
      this.rootIndex = input.readVarInt();
      validateEntries(this.entries);
   }

   private void write(final FriendlyByteBuf output) {
      output.writeCollection(this.entries, (buffer, entry) -> entry.write(buffer));
      output.writeVarInt(this.rootIndex);
   }

   private static void validateEntries(
      final List<ClientboundCommandsPacket.Entry> entries, final BiPredicate<ClientboundCommandsPacket.Entry, IntSet> validator
   ) {
      IntSet elementsToCheck = new IntOpenHashSet(IntSets.fromTo(0, entries.size()));

      while (!elementsToCheck.isEmpty()) {
         boolean worked = elementsToCheck.removeIf(index -> validator.test(entries.get(index), elementsToCheck));
         if (!worked) {
            throw new IllegalStateException("Server sent an impossible command tree");
         }
      }
   }

   private static void validateEntries(final List<ClientboundCommandsPacket.Entry> entries) {
      validateEntries(entries, ClientboundCommandsPacket.Entry::canBuild);
      validateEntries(entries, ClientboundCommandsPacket.Entry::canResolve);
   }

   private static <S> Object2IntMap<CommandNode<S>> enumerateNodes(final RootCommandNode<S> root) {
      Object2IntMap<CommandNode<S>> nodeToId = new Object2IntOpenHashMap();
      Queue<CommandNode<S>> queue = new ArrayDeque<>();
      queue.add(root);

      CommandNode<S> node;
      while ((node = queue.poll()) != null) {
         if (!nodeToId.containsKey(node)) {
            int id = nodeToId.size();
            nodeToId.put(node, id);
            queue.addAll(node.getChildren());
            if (node.getRedirect() != null) {
               queue.add(node.getRedirect());
            }
         }
      }

      return nodeToId;
   }

   private static <S> List<ClientboundCommandsPacket.Entry> createEntries(
      final Object2IntMap<CommandNode<S>> nodeToId, final ClientboundCommandsPacket.NodeInspector<S> inspector
   ) {
      ObjectArrayList<ClientboundCommandsPacket.Entry> result = new ObjectArrayList(nodeToId.size());
      result.size(nodeToId.size());
      ObjectIterator var3 = Object2IntMaps.fastIterable(nodeToId).iterator();

      while (var3.hasNext()) {
         it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<S>> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<S>>)var3.next();
         result.set(entry.getIntValue(), createEntry((CommandNode<S>)entry.getKey(), inspector, nodeToId));
      }

      return result;
   }

   private static ClientboundCommandsPacket.Entry readNode(final FriendlyByteBuf input) {
      byte flags = input.readByte();
      int[] children = input.readVarIntArray();
      int redirect = (flags & 8) != 0 ? input.readVarInt() : 0;
      ClientboundCommandsPacket.NodeStub stub = read(input, flags);
      return new ClientboundCommandsPacket.Entry(stub, flags, redirect, children);
   }

   private static ClientboundCommandsPacket.@Nullable NodeStub read(final FriendlyByteBuf input, final byte flags) {
      int type = flags & 3;
      if (type == 2) {
         String name = input.readUtf();
         int id = input.readVarInt();
         ArgumentTypeInfo<?, ?> argumentType = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId(id);
         if (argumentType == null) {
            return null;
         }

         ArgumentTypeInfo.Template<?> argument = argumentType.deserializeFromNetwork(input);
         Identifier suggestionId = (flags & 16) != 0 ? input.readIdentifier() : null;
         return new ClientboundCommandsPacket.ArgumentNodeStub(name, argument, suggestionId);
      } else if (type == 1) {
         String id = input.readUtf();
         return new ClientboundCommandsPacket.LiteralNodeStub(id);
      } else {
         return null;
      }
   }

   private static <S> ClientboundCommandsPacket.Entry createEntry(
      final CommandNode<S> node, final ClientboundCommandsPacket.NodeInspector<S> inspector, final Object2IntMap<CommandNode<S>> ids
   ) {
      int flags = 0;
      int redirect;
      if (node.getRedirect() != null) {
         flags |= 8;
         redirect = ids.getInt(node.getRedirect());
      } else {
         redirect = 0;
      }

      if (inspector.isExecutable(node)) {
         flags |= 4;
      }

      if (inspector.isRestricted(node)) {
         flags |= 32;
      }

      ClientboundCommandsPacket.NodeStub nodeStub;
      switch (node) {
         case RootCommandNode<S> ignored:
            flags |= 0;
            nodeStub = null;
            break;
         case ArgumentCommandNode<S, ?> arg:
            Identifier suggestionId = inspector.suggestionId(arg);
            nodeStub = new ClientboundCommandsPacket.ArgumentNodeStub(arg.getName(), ArgumentTypeInfos.unpack(arg.getType()), suggestionId);
            flags |= 2;
            if (suggestionId != null) {
               flags |= 16;
            }
            break;
         case LiteralCommandNode<S> literal:
            nodeStub = new ClientboundCommandsPacket.LiteralNodeStub(literal.getLiteral());
            flags |= 1;
            break;
         default:
            throw new UnsupportedOperationException("Unknown node type " + node);
      }

      int[] childrenIds = node.getChildren().stream().mapToInt(ids::getInt).toArray();
      return new ClientboundCommandsPacket.Entry(nodeStub, flags, redirect, childrenIds);
   }

   @Override
   public PacketType<ClientboundCommandsPacket> type() {
      return GamePacketTypes.CLIENTBOUND_COMMANDS;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleCommands(this);
   }

   public <S> RootCommandNode<S> getRoot(final CommandBuildContext context, final ClientboundCommandsPacket.NodeBuilder<S> builder) {
      return (RootCommandNode<S>)new ClientboundCommandsPacket.NodeResolver<>(context, builder, this.entries).resolve(this.rootIndex);
   }

   private record ArgumentNodeStub(String id, ArgumentTypeInfo.Template<?> argumentType, @Nullable Identifier suggestionId)
      implements ClientboundCommandsPacket.NodeStub {
      @Override
      public <S> ArgumentBuilder<S, ?> build(final CommandBuildContext context, final ClientboundCommandsPacket.NodeBuilder<S> builder) {
         ArgumentType<?> type = this.argumentType.instantiate(context);
         return builder.createArgument(this.id, type, this.suggestionId);
      }

      @Override
      public void write(final FriendlyByteBuf output) {
         output.writeUtf(this.id);
         serializeCap(output, this.argumentType);
         if (this.suggestionId != null) {
            output.writeIdentifier(this.suggestionId);
         }
      }

      private static <A extends ArgumentType<?>> void serializeCap(final FriendlyByteBuf output, final ArgumentTypeInfo.Template<A> argumentType) {
         serializeCap(output, argumentType.type(), argumentType);
      }

      private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
         final FriendlyByteBuf output, final ArgumentTypeInfo<A, T> info, final ArgumentTypeInfo.Template<A> argumentType
      ) {
         output.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(info));
         info.serializeToNetwork((T)argumentType, output);
      }
   }

   private record Entry(ClientboundCommandsPacket.@Nullable NodeStub stub, int flags, int redirect, int[] children) {
      public void write(final FriendlyByteBuf output) {
         output.writeByte(this.flags);
         output.writeVarIntArray(this.children);
         if ((this.flags & 8) != 0) {
            output.writeVarInt(this.redirect);
         }

         if (this.stub != null) {
            this.stub.write(output);
         }
      }

      public boolean canBuild(final IntSet unbuiltNodes) {
         return (this.flags & 8) != 0 ? !unbuiltNodes.contains(this.redirect) : true;
      }

      public boolean canResolve(final IntSet unresolvedNodes) {
         for (int child : this.children) {
            if (unresolvedNodes.contains(child)) {
               return false;
            }
         }

         return true;
      }
   }

   private record LiteralNodeStub(String id) implements ClientboundCommandsPacket.NodeStub {
      @Override
      public <S> ArgumentBuilder<S, ?> build(final CommandBuildContext context, final ClientboundCommandsPacket.NodeBuilder<S> builder) {
         return builder.createLiteral(this.id);
      }

      @Override
      public void write(final FriendlyByteBuf output) {
         output.writeUtf(this.id);
      }
   }

   public interface NodeBuilder<S> {
      ArgumentBuilder<S, ?> createLiteral(String id);

      ArgumentBuilder<S, ?> createArgument(String id, ArgumentType<?> argumentType, @Nullable Identifier suggestionId);

      ArgumentBuilder<S, ?> configure(ArgumentBuilder<S, ?> input, boolean executable, boolean restricted);
   }

   public interface NodeInspector<S> {
      @Nullable Identifier suggestionId(ArgumentCommandNode<S, ?> node);

      boolean isExecutable(CommandNode<S> node);

      boolean isRestricted(CommandNode<S> node);
   }

   private static class NodeResolver<S> {
      private final CommandBuildContext context;
      private final ClientboundCommandsPacket.NodeBuilder<S> builder;
      private final List<ClientboundCommandsPacket.Entry> entries;
      private final List<CommandNode<S>> nodes;

      private NodeResolver(
         final CommandBuildContext context, final ClientboundCommandsPacket.NodeBuilder<S> builder, final List<ClientboundCommandsPacket.Entry> entries
      ) {
         this.context = context;
         this.builder = builder;
         this.entries = entries;
         ObjectArrayList<CommandNode<S>> nodes = new ObjectArrayList();
         nodes.size(entries.size());
         this.nodes = nodes;
      }

      public CommandNode<S> resolve(final int index) {
         CommandNode<S> currentNode = this.nodes.get(index);
         if (currentNode != null) {
            return currentNode;
         }

         ClientboundCommandsPacket.Entry entry = this.entries.get(index);
         CommandNode<S> result;
         if (entry.stub == null) {
            result = new RootCommandNode();
         } else {
            ArgumentBuilder<S, ?> resultBuilder = entry.stub.build(this.context, this.builder);
            if ((entry.flags & 8) != 0) {
               resultBuilder.redirect(this.resolve(entry.redirect));
            }

            boolean isExecutable = (entry.flags & 4) != 0;
            boolean isRestricted = (entry.flags & 32) != 0;
            result = this.builder.configure(resultBuilder, isExecutable, isRestricted).build();
         }

         this.nodes.set(index, result);

         for (int childId : entry.children) {
            CommandNode<S> child = this.resolve(childId);
            if (!(child instanceof RootCommandNode)) {
               result.addChild(child);
            }
         }

         return result;
      }
   }

   private interface NodeStub {
      <S> ArgumentBuilder<S, ?> build(CommandBuildContext context, ClientboundCommandsPacket.NodeBuilder<S> builder);

      void write(FriendlyByteBuf output);
   }
}
