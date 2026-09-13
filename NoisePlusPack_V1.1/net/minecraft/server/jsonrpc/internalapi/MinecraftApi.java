package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.notifications.NotificationManager;

public class MinecraftApi {
   private final NotificationManager notificationManager;
   private final MinecraftAllowListService allowListService;
   private final MinecraftBanListService banListService;
   private final MinecraftPlayerListService minecraftPlayerListService;
   private final MinecraftGameRuleService gameRuleService;
   private final MinecraftOperatorListService minecraftOperatorListService;
   private final MinecraftServerSettingsService minecraftServerSettingsService;
   private final MinecraftServerStateService minecraftServerStateService;
   private final MinecraftExecutorService executorService;

   public MinecraftApi(
      NotificationManager p_431630_,
      MinecraftAllowListService p_425044_,
      MinecraftBanListService p_431162_,
      MinecraftPlayerListService p_427079_,
      MinecraftGameRuleService p_426404_,
      MinecraftOperatorListService p_428894_,
      MinecraftServerSettingsService p_425108_,
      MinecraftServerStateService p_427351_,
      MinecraftExecutorService p_423830_
   ) {
      this.notificationManager = p_431630_;
      this.allowListService = p_425044_;
      this.banListService = p_431162_;
      this.minecraftPlayerListService = p_427079_;
      this.gameRuleService = p_426404_;
      this.minecraftOperatorListService = p_428894_;
      this.minecraftServerSettingsService = p_425108_;
      this.minecraftServerStateService = p_427351_;
      this.executorService = p_423830_;
   }

   public <V> CompletableFuture<V> submit(Supplier<V> p_426955_) {
      return this.executorService.submit(p_426955_);
   }

   public CompletableFuture<Void> submit(Runnable p_422676_) {
      return this.executorService.submit(p_422676_);
   }

   public MinecraftAllowListService allowListService() {
      return this.allowListService;
   }

   public MinecraftBanListService banListService() {
      return this.banListService;
   }

   public MinecraftPlayerListService playerListService() {
      return this.minecraftPlayerListService;
   }

   public MinecraftGameRuleService gameRuleService() {
      return this.gameRuleService;
   }

   public MinecraftOperatorListService operatorListService() {
      return this.minecraftOperatorListService;
   }

   public MinecraftServerSettingsService serverSettingsService() {
      return this.minecraftServerSettingsService;
   }

   public MinecraftServerStateService serverStateService() {
      return this.minecraftServerStateService;
   }

   public NotificationManager notificationManager() {
      return this.notificationManager;
   }

   public static MinecraftApi of(DedicatedServer p_427317_) {
      JsonRpcLogger jsonrpclogger = new JsonRpcLogger();
      MinecraftAllowListServiceImpl minecraftallowlistserviceimpl = new MinecraftAllowListServiceImpl(p_427317_, jsonrpclogger);
      MinecraftBanListServiceImpl minecraftbanlistserviceimpl = new MinecraftBanListServiceImpl(p_427317_, jsonrpclogger);
      MinecraftPlayerListServiceImpl minecraftplayerlistserviceimpl = new MinecraftPlayerListServiceImpl(p_427317_, jsonrpclogger);
      MinecraftGameRuleServiceImpl minecraftgameruleserviceimpl = new MinecraftGameRuleServiceImpl(p_427317_, jsonrpclogger);
      MinecraftOperatorListServiceImpl minecraftoperatorlistserviceimpl = new MinecraftOperatorListServiceImpl(p_427317_, jsonrpclogger);
      MinecraftServerSettingsServiceImpl minecraftserversettingsserviceimpl = new MinecraftServerSettingsServiceImpl(p_427317_, jsonrpclogger);
      MinecraftServerStateServiceImpl minecraftserverstateserviceimpl = new MinecraftServerStateServiceImpl(p_427317_, jsonrpclogger);
      MinecraftExecutorService minecraftexecutorservice = new MinecraftExecutorServiceImpl(p_427317_);
      return new MinecraftApi(
         p_427317_.notificationManager(),
         minecraftallowlistserviceimpl,
         minecraftbanlistserviceimpl,
         minecraftplayerlistserviceimpl,
         minecraftgameruleserviceimpl,
         minecraftoperatorlistserviceimpl,
         minecraftserversettingsserviceimpl,
         minecraftserverstateserviceimpl,
         minecraftexecutorservice
      );
   }
}
