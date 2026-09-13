package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.dto.RealmsServer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;

public class RealmsServerList implements Iterable<RealmsServer> {
   private final Minecraft minecraft;
   private final Set<RealmsServer> removedServers = new HashSet<>();
   private List<RealmsServer> servers = List.of();

   public RealmsServerList(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void updateServersList(final List<RealmsServer> fetchedServers) {
      List<RealmsServer> sortedServers = new ArrayList<>(fetchedServers);
      sortedServers.sort(new RealmsServer.McoServerComparator(this.minecraft.getUser().getName()));
      boolean removedAnyServers = sortedServers.removeAll(this.removedServers);
      if (!removedAnyServers) {
         this.removedServers.clear();
      }

      this.servers = sortedServers;
   }

   public void removeItem(final RealmsServer server) {
      this.servers.remove(server);
      this.removedServers.add(server);
   }

   @Override
   public Iterator<RealmsServer> iterator() {
      return this.servers.iterator();
   }

   public boolean isEmpty() {
      return this.servers.isEmpty();
   }
}
