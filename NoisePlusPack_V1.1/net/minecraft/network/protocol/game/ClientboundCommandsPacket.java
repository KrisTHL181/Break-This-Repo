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

   public <S> ClientboundCommandsPacket(RootCommandNode<S> p_131861_, ClientboundCommandsPacket.NodeInspector<S> p_408963_) {
      Object2IntMap<CommandNode<S>> object2intmap = enumerateNodes(p_131861_);
      this.entries = createEntries(object2intmap, p_408963_);
      this.rootIndex = object2intmap.getInt(p_131861_);
   }

   private ClientboundCommandsPacket(FriendlyByteBuf p_178805_) {
      this.entries = p_178805_.readList(ClientboundCommandsPacket::readNode);
      this.rootIndex = p_178805_.readVarInt();
      validateEntries(this.entries);
   }

   private void write(FriendlyByteBuf p_131886_) {
      p_131886_.writeCollection(this.entries, (p_237642_, p_237643_) -> p_237643_.write(p_237642_));
      p_131886_.writeVarInt(this.rootIndex);
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> p_237631_, BiPredicate<ClientboundCommandsPacket.Entry, IntSet> p_237632_) {
      IntSet intset = new IntOpenHashSet(IntSets.fromTo(0, p_237631_.size()));

      while (!intset.isEmpty()) {
         boolean flag = intset.removeIf(p_237637_ -> p_237632_.test(p_237631_.get(p_237637_), intset));
         if (!flag) {
            throw new IllegalStateException("Server sent an impossible command tree");
         }
      }
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> p_237629_) {
      validateEntries(p_237629_, ClientboundCommandsPacket.Entry::canBuild);
      validateEntries(p_237629_, ClientboundCommandsPacket.Entry::canResolve);
   }

   private static <S> Object2IntMap<CommandNode<S>> enumerateNodes(RootCommandNode<S> p_131863_) {
      Object2IntMap<CommandNode<S>> object2intmap = new Object2IntOpenHashMap();
      Queue<CommandNode<S>> queue = new ArrayDeque<>();
      queue.add(p_131863_);

      CommandNode<S> commandnode;
      while ((commandnode = queue.poll()) != null) {
         if (!object2intmap.containsKey(commandnode)) {
            int i = object2intmap.size();
            object2intmap.put(commandnode, i);
            queue.addAll(commandnode.getChildren());
            if (commandnode.getRedirect() != null) {
               queue.add(commandnode.getRedirect());
            }
         }
      }

      return object2intmap;
   }

   private static <S> List<ClientboundCommandsPacket.Entry> createEntries(
      Object2IntMap<CommandNode<S>> p_237627_, ClientboundCommandsPacket.NodeInspector<S> p_408878_
   ) {
      ObjectArrayList<ClientboundCommandsPacket.Entry> objectarraylist = new ObjectArrayList(p_237627_.size());
      objectarraylist.size(p_237627_.size());
      ObjectIterator var3 = Object2IntMaps.fastIterable(p_237627_).iterator();

      while (var3.hasNext()) {
         it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<S>> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<S>>)var3.next();
         objectarraylist.set(entry.getIntValue(), createEntry((CommandNode<S>)entry.getKey(), p_408878_, p_237627_));
      }

      return objectarraylist;
   }

   private static ClientboundCommandsPacket.Entry readNode(FriendlyByteBuf p_131888_) {
      byte b0 = p_131888_.readByte();
      int[] aint = p_131888_.readVarIntArray();
      int i = (b0 & 8) != 0 ? p_131888_.readVarInt() : 0;
      ClientboundCommandsPacket.NodeStub clientboundcommandspacket$nodestub = read(p_131888_, b0);
      return new ClientboundCommandsPacket.Entry(clientboundcommandspacket$nodestub, b0, i, aint);
   }

   private static ClientboundCommandsPacket.@Nullable NodeStub read(FriendlyByteBuf p_237639_, byte p_237640_) {
      int i = p_237640_ & 3;
      if (i == 2) {
         String s1 = p_237639_.readUtf();
         int j = p_237639_.readVarInt();
         ArgumentTypeInfo<?, ?> argumenttypeinfo = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId(j);
         if (argumenttypeinfo == null) {
            return null;
         }

         ArgumentTypeInfo.Template<?> template = argumenttypeinfo.deserializeFromNetwork(p_237639_);
         Identifier identifier = (p_237640_ & 16) != 0 ? p_237639_.readIdentifier() : null;
         return new ClientboundCommandsPacket.ArgumentNodeStub(s1, template, identifier);
      } else if (i == 1) {
         String s = p_237639_.readUtf();
         return new ClientboundCommandsPacket.LiteralNodeStub(s);
      } else {
         return null;
      }
   }

   private static <S> ClientboundCommandsPacket.Entry createEntry(
      CommandNode<S> p_237622_, ClientboundCommandsPacket.NodeInspector<S> p_409182_, Object2IntMap<CommandNode<S>> p_237623_
   ) {
      int i = 0;
      int j;
      if (p_237622_.getRedirect() != null) {
         i |= 8;
         j = p_237623_.getInt(p_237622_.getRedirect());
      } else {
         j = 0;
      }

      if (p_409182_.isExecutable(p_237622_)) {
         i |= 4;
      }

      if (p_409182_.isRestricted(p_237622_)) {
         i |= 32;
      }

      ClientboundCommandsPacket.NodeStub clientboundcommandspacket$nodestub;
      switch (p_237622_) {
         case RootCommandNode<S> rootcommandnode:
            i |= 0;
            clientboundcommandspacket$nodestub = null;
            break;
         case ArgumentCommandNode<S, ?> argumentcommandnode:
            Identifier identifier = p_409182_.suggestionId(argumentcommandnode);
            clientboundcommandspacket$nodestub = new ClientboundCommandsPacket.ArgumentNodeStub(
               argumentcommandnode.getName(), ArgumentTypeInfos.unpack(argumentcommandnode.getType()), identifier
            );
            i |= 2;
            if (identifier != null) {
               i |= 16;
            }
            break;
         case LiteralCommandNode<S> literalcommandnode:
            clientboundcommandspacket$nodestub = new ClientboundCommandsPacket.LiteralNodeStub(literalcommandnode.getLiteral());
            i |= 1;
            break;
         default:
            throw new UnsupportedOperationException("Unknown node type " + p_237622_);
      }

      int[] aint = p_237622_.getChildren().stream().mapToInt(p_237623_::getInt).toArray();
      return new ClientboundCommandsPacket.Entry(clientboundcommandspacket$nodestub, i, j, aint);
   }

   @Override
   public PacketType<ClientboundCommandsPacket> type() {
      return GamePacketTypes.CLIENTBOUND_COMMANDS;
   }

   public void handle(ClientGamePacketListener p_131878_) {
      p_131878_.handleCommands(this);
   }

   public <S> RootCommandNode<S> getRoot(CommandBuildContext p_237625_, ClientboundCommandsPacket.NodeBuilder<S> p_409424_) {
      return (RootCommandNode<S>)new ClientboundCommandsPacket.NodeResolver<>(p_237625_, p_409424_, this.entries).resolve(this.rootIndex);
   }

   record ArgumentNodeStub(String id, ArgumentTypeInfo.Template<?> argumentType, @Nullable Identifier suggestionId)
      implements ClientboundCommandsPacket.NodeStub {
      @Override
      public <S> ArgumentBuilder<S, ?> build(CommandBuildContext p_237656_, ClientboundCommandsPacket.NodeBuilder<S> p_407604_) {
         ArgumentType<?> argumenttype = this.argumentType.instantiate(p_237656_);
         return p_407604_.createArgument(this.id, argumenttype, this.suggestionId);
      }

      @Override
      public void write(FriendlyByteBuf p_237658_) {
         p_237658_.writeUtf(this.id);
         serializeCap(p_237658_, this.argumentType);
         if (this.suggestionId != null) {
            p_237658_.writeIdentifier(this.suggestionId);
         }
      }

      private static <A extends ArgumentType<?>> void serializeCap(FriendlyByteBuf p_237660_, ArgumentTypeInfo.Template<A> p_237661_) {
         serializeCap(p_237660_, p_237661_.type(), p_237661_);
      }

      private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
         FriendlyByteBuf p_237663_, ArgumentTypeInfo<A, T> p_237664_, ArgumentTypeInfo.Template<A> p_237665_
      ) {
         p_237663_.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(p_237664_));
         p_237664_.serializeToNetwork((T)p_237665_, p_237663_);
      }
   }

   record Entry(ClientboundCommandsPacket.@Nullable NodeStub stub, int flags, int redirect, int[] children) {
      public void write(FriendlyByteBuf p_237675_) {
         p_237675_.writeByte(this.flags);
         p_237675_.writeVarIntArray(this.children);
         if ((this.flags & 8) != 0) {
            p_237675_.writeVarInt(this.redirect);
         }

         if (this.stub != null) {
            this.stub.write(p_237675_);
         }
      }

      public boolean canBuild(IntSet p_237673_) {
         return (this.flags & 8) != 0 ? !p_237673_.contains(this.redirect) : true;
      }

      public boolean canResolve(IntSet p_237677_) {
         for (int i : this.children) {
            if (p_237677_.contains(i)) {
               return false;
            }
         }

         return true;
      }
   }

   record LiteralNodeStub(String id) implements ClientboundCommandsPacket.NodeStub {
      @Override
      public <S> ArgumentBuilder<S, ?> build(CommandBuildContext p_237682_, ClientboundCommandsPacket.NodeBuilder<S> p_407985_) {
         return p_407985_.createLiteral(this.id);
      }

      @Override
      public void write(FriendlyByteBuf p_237684_) {
         p_237684_.writeUtf(this.id);
      }
   }

   public interface NodeBuilder<S> {
      ArgumentBuilder<S, ?> createLiteral(String var1);

      ArgumentBuilder<S, ?> createArgument(String var1, ArgumentType<?> var2, @Nullable Identifier var3);

      ArgumentBuilder<S, ?> configure(ArgumentBuilder<S, ?> var1, boolean var2, boolean var3);
   }

   public interface NodeInspector<S> {
      @Nullable Identifier suggestionId(ArgumentCommandNode<S, ?> var1);

      boolean isExecutable(CommandNode<S> var1);

      boolean isRestricted(CommandNode<S> var1);
   }

   static class NodeResolver<S> {
      private final CommandBuildContext context;
      private final ClientboundCommandsPacket.NodeBuilder<S> builder;
      private final List<ClientboundCommandsPacket.Entry> entries;
      private final List<CommandNode<S>> nodes;

      NodeResolver(CommandBuildContext p_237689_, ClientboundCommandsPacket.NodeBuilder<S> p_406424_, List<ClientboundCommandsPacket.Entry> p_237690_) {
         this.context = p_237689_;
         this.builder = p_406424_;
         this.entries = p_237690_;
         ObjectArrayList<CommandNode<S>> objectarraylist = new ObjectArrayList();
         objectarraylist.size(p_237690_.size());
         this.nodes = objectarraylist;
      }

      public CommandNode<S> resolve(int p_237692_) {
         CommandNode<S> commandnode = this.nodes.get(p_237692_);
         if (commandnode != null) {
            return commandnode;
         }

         ClientboundCommandsPacket.Entry clientboundcommandspacket$entry = this.entries.get(p_237692_);
         CommandNode<S> commandnode1;
         if (clientboundcommandspacket$entry.stub == null) {
            commandnode1 = new RootCommandNode();
         } else {
            ArgumentBuilder<S, ?> argumentbuilder = clientboundcommandspacket$entry.stub.build(this.context, this.builder);
            if ((clientboundcommandspacket$entry.flags & 8) != 0) {
               argumentbuilder.redirect(this.resolve(clientboundcommandspacket$entry.redirect));
            }

            boolean flag = (clientboundcommandspacket$entry.flags & 4) != 0;
            boolean flag1 = (clientboundcommandspacket$entry.flags & 32) != 0;
            commandnode1 = this.builder.configure(argumentbuilder, flag, flag1).build();
         }

         this.nodes.set(p_237692_, commandnode1);

         for (int i : clientboundcommandspacket$entry.children) {
            CommandNode<S> commandnode2 = this.resolve(i);
            if (!(commandnode2 instanceof RootCommandNode)) {
               commandnode1.addChild(commandnode2);
            }
         }

         return commandnode1;
      }
   }

   interface NodeStub {
      <S> ArgumentBuilder<S, ?> build(CommandBuildContext var1, ClientboundCommandsPacket.NodeBuilder<S> var2);

      void write(FriendlyByteBuf var1);
   }
}
