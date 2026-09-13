package dev.miru.options.base;

import java.util.List;

public class Vec3Options<T> {
   public T x;
   public T y;
   public T z;
   public T defaultX;
   public T defaultY;
   public T defaultZ;
   private String idX = "";
   private String idY = "";
   private String idZ = "";

   public Vec3Options(T x, T y, T z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public Vec3Options<T> setDefaultValue(T x, T y, T z) {
      this.defaultX = x;
      this.defaultY = y;
      this.defaultZ = z;
      return this;
   }

   public Vec3Options<T> setSettingIds(String idX, String idY, String idZ) {
      this.idX = idX;
      this.idY = idY;
      this.idZ = idZ;
      return this;
   }

   public boolean isDefault() {
      return this.x.equals(this.defaultX) && this.y.equals(this.defaultY) && this.z.equals(this.defaultZ);
   }

   public boolean equalsDefault(T x, T y, T z) {
      return x.equals(this.defaultX) && y.equals(this.defaultY) && z.equals(this.defaultZ);
   }

   public List<SettingItem<T>> toItemArray() {
      return List.of(
         new SettingItem<T>(this.x).setDefaultValue(this.defaultX),
         new SettingItem<T>(this.y).setDefaultValue(this.defaultY),
         new SettingItem<T>(this.z).setDefaultValue(this.defaultZ)
      );
   }

   public String xId() {
      return this.idX;
   }

   public String yId() {
      return this.idY;
   }

   public String zId() {
      return this.idZ;
   }
}
