package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class StoredUserList<K, V extends StoredUserEntry<K>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final File file;
   private final Map<String, V> map = Maps.newHashMap();
   protected final NotificationService notificationService;

   public StoredUserList(final File file, final NotificationService notificationService) {
      this.file = file;
      this.notificationService = notificationService;
   }

   public File getFile() {
      return this.file;
   }

   public boolean add(final V infos) {
      String keyForUser = this.getKeyForUser(infos.getUser());
      V previous = this.map.get(keyForUser);
      if (infos.equals(previous)) {
         return false;
      }

      this.map.put(keyForUser, infos);

      try {
         this.save();
      } catch (IOException e) {
         LOGGER.warn("Could not save the list after adding a user.", e);
      }

      return true;
   }

   public @Nullable V get(final K user) {
      this.removeExpired();
      return this.map.get(this.getKeyForUser(user));
   }

   public boolean remove(final K user) {
      V removed = this.map.remove(this.getKeyForUser(user));
      if (removed == null) {
         return false;
      }

      try {
         this.save();
      } catch (IOException e) {
         LOGGER.warn("Could not save the list after removing a user.", e);
      }

      return true;
   }

   public boolean remove(final StoredUserEntry<K> infos) {
      return this.remove(Objects.requireNonNull(infos.getUser()));
   }

   public void clear() {
      this.map.clear();

      try {
         this.save();
      } catch (IOException e) {
         LOGGER.warn("Could not save the list after removing a user.", e);
      }
   }

   public String[] getUserList() {
      return this.map.keySet().toArray(new String[0]);
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   protected String getKeyForUser(final K user) {
      return user.toString();
   }

   protected boolean contains(final K user) {
      return this.map.containsKey(this.getKeyForUser(user));
   }

   private void removeExpired() {
      List<K> toRemove = Lists.newArrayList();

      for (V entry : this.map.values()) {
         if (entry.hasExpired()) {
            toRemove.add(entry.getUser());
         }
      }

      for (K user : toRemove) {
         this.map.remove(this.getKeyForUser(user));
      }
   }

   protected abstract StoredUserEntry<K> createEntry(final JsonObject object);

   public Collection<V> getEntries() {
      return this.map.values();
   }

   public void save() throws IOException {
      JsonArray result = new JsonArray();
      this.map.values().stream().map(entry -> Util.make(new JsonObject(), entry::serialize)).forEach(result::add);

      try (BufferedWriter writer = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
         GSON.toJson(result, GSON.newJsonWriter(writer));
      }
   }

   public void load() throws IOException {
      if (this.file.exists()) {
         try (BufferedReader reader = Files.newReader(this.file, StandardCharsets.UTF_8)) {
            this.map.clear();
            JsonArray contents = (JsonArray)GSON.fromJson(reader, JsonArray.class);
            if (contents == null) {
               return;
            }

            for (JsonElement element : contents) {
               JsonObject object = GsonHelper.convertToJsonObject(element, "entry");
               StoredUserEntry<K> entry = this.createEntry(object);
               if (entry.getUser() != null) {
                  this.map.put(this.getKeyForUser(entry.getUser()), (V)entry);
               }
            }
         }
      }
   }
}
