package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public abstract class BanListEntry<T> extends StoredUserEntry<T> {
   public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
   public static final String EXPIRES_NEVER = "forever";
   protected final Date created;
   protected final String source;
   protected final @Nullable Date expires;
   protected final @Nullable String reason;

   public BanListEntry(
      final @Nullable T user, final @Nullable Date created, final @Nullable String source, final @Nullable Date expires, final @Nullable String reason
   ) {
      super(user);
      this.created = created == null ? new Date() : created;
      this.source = source == null ? "(Unknown)" : source;
      this.expires = expires;
      this.reason = reason;
   }

   protected BanListEntry(final @Nullable T user, final JsonObject object) {
      super(user);

      Date created;
      try {
         created = object.has("created") ? DATE_FORMAT.parse(object.get("created").getAsString()) : new Date();
      } catch (ParseException ignored) {
         created = new Date();
      }

      this.created = created;
      this.source = object.has("source") ? object.get("source").getAsString() : "(Unknown)";

      Date expires;
      try {
         expires = object.has("expires") ? DATE_FORMAT.parse(object.get("expires").getAsString()) : null;
      } catch (ParseException ignored) {
         expires = null;
      }

      this.expires = expires;
      this.reason = object.has("reason") ? object.get("reason").getAsString() : null;
   }

   public Date getCreated() {
      return this.created;
   }

   public String getSource() {
      return this.source;
   }

   public @Nullable Date getExpires() {
      return this.expires;
   }

   public @Nullable String getReason() {
      return this.reason;
   }

   public Component getReasonMessage() {
      String reason = this.getReason();
      return reason == null ? Component.translatable("multiplayer.disconnect.banned.reason.default") : Component.literal(reason);
   }

   public abstract Component getDisplayName();

   @Override
   public boolean hasExpired() {
      return this.expires == null ? false : this.expires.before(new Date());
   }

   @Override
   protected void serialize(final JsonObject object) {
      object.addProperty("created", DATE_FORMAT.format(this.created));
      object.addProperty("source", this.source);
      object.addProperty("expires", this.expires == null ? "forever" : DATE_FORMAT.format(this.expires));
      object.addProperty("reason", this.reason);
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         BanListEntry<?> that = (BanListEntry<?>)o;
         return Objects.equals(this.source, that.source)
            && Objects.equals(this.expires, that.expires)
            && Objects.equals(this.reason, that.reason)
            && Objects.equals(this.getUser(), that.getUser());
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.source, this.expires, this.reason, this.getUser());
   }
}
