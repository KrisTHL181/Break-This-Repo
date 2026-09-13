package dev.miru.helper;

public class FaqItem {
   private String title;
   private String desc;

   public FaqItem(String title, String desc) {
      this.title = title;
      this.desc = desc;
   }

   public String getTitle() {
      return this.title;
   }

   public String getDesc() {
      return this.desc;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public void setDesc(String desc) {
      this.desc = desc;
   }
}
