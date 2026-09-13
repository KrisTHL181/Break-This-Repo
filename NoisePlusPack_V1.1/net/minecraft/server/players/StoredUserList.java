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

   public StoredUserList(File p_11380_, NotificationService p_426637_) {
      this.file = p_11380_;
      this.notificationService = p_426637_;
   }

   public File getFile() {
      return this.file;
   }

   public boolean add(V p_11382_) {
      String s = this.getKeyForUser(p_11382_.getUser());
      V v = this.map.get(s);
      if (p_11382_.equals(v)) {
         return false;
      }

      this.map.put(s, p_11382_);

      try {
         this.save();
      } catch (IOException ioexception) {
         LOGGER.warn("Could not save the list after adding a user.", ioexception);
      }

      return true;
   }

   public @Nullable V get(K p_11389_) {
      this.removeExpired();
      return this.map.get(this.getKeyForUser(p_11389_));
   }

   public boolean remove(K p_11394_) {
      V v = this.map.remove(this.getKeyForUser(p_11394_));
      if (v == null) {
         return false;
      }

      try {
         this.save();
      } catch (IOException ioexception) {
         LOGGER.warn("Could not save the list after removing a user.", ioexception);
      }

      return true;
   }

   public boolean remove(StoredUserEntry<K> p_11387_) {
      return this.remove(Objects.requireNonNull(p_11387_.getUser()));
   }

   public void clear() {
      this.map.clear();

      try {
         this.save();
      } catch (IOException ioexception) {
         LOGGER.warn("Could not save the list after removing a user.", ioexception);
      }
   }

   public String[] getUserList() {
      return this.map.keySet().toArray(new String[0]);
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   protected String getKeyForUser(K p_11384_) {
      return p_11384_.toString();
   }

   protected boolean contains(K p_11397_) {
      return this.map.containsKey(this.getKeyForUser(p_11397_));
   }

   private void removeExpired() {
      List<K> list = Lists.newArrayList();

      for (V v : this.map.values()) {
         if (v.hasExpired()) {
            list.add(v.getUser());
         }
      }

      for (K k : list) {
         this.map.remove(this.getKeyForUser(k));
      }
   }

   protected abstract StoredUserEntry<K> createEntry(JsonObject var1);

   public Collection<V> getEntries() {
      return this.map.values();
   }

   public void save() throws IOException {
      JsonArray jsonarray = new JsonArray();
      this.map.values().stream().map(p_449202_ -> Util.make(new JsonObject(), p_449202_::serialize)).forEach(jsonarray::add);

      try (BufferedWriter bufferedwriter = Files.newWriter(this.file, StandardCharsets.UTF_8)) {
         GSON.toJson(jsonarray, GSON.newJsonWriter(bufferedwriter));
      }
   }

   public void load() throws IOException {
      label37:
      if (this.file.exists()) {
         try (BufferedReader bufferedreader = Files.newReader(this.file, StandardCharsets.UTF_8)) {
            this.map.clear();
            JsonArray jsonarray = (JsonArray)GSON.fromJson(bufferedreader, JsonArray.class);
            if (jsonarray != null) {
               for (JsonElement jsonelement : jsonarray) {
                  JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "entry");
                  StoredUserEntry<K> storeduserentry = this.createEntry(jsonobject);
                  if (storeduserentry.getUser() != null) {
                     this.map.put(this.getKeyForUser(storeduserentry.getUser()), (V)storeduserentry);
                  }
               }
               break label37;
            }
         }

         return;
      }
   }
}
