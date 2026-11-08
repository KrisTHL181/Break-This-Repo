package forbric.carpetprobe;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import carpet.patches.FakeClientConnection;
import carpet.script.CarpetScriptHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Exercises released Carpet callbacks in a real server; only staged in isolated gate worlds. */
@Mod("forbriccarpetprobe")
public final class CarpetProbe {
	private static MinecraftServer server;
	private static int ticks;
	private static boolean nativeSwapVeto, nativeBreakVeto;
	private static final List<Map<String,Object>> cases=new ArrayList<>();
	private static CarpetScriptHost host;
	private static ServerPlayer player;
	public CarpetProbe(IEventBus bus) {
		NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class,e->{server=e.getServer();server.overworld().setChunkForced(0,0,true);});
		NeoForge.EVENT_BUS.addListener(LivingSwapItemsEvent.Hands.class,e->{if(nativeSwapVeto)e.setCanceled(true);});
		NeoForge.EVENT_BUS.addListener(BreakBlockEvent.class,e->{if(nativeBreakVeto)e.setCanceled(true);});
		NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class,e->{if(e.getServer()!=server||++ticks!=30)return;try{run();}catch(Throwable t){test("infrastructure",()->{throw t;});}finally{finish();}});
	}
	interface Case { void run()throws Throwable; }
	private static void test(String name,Case body){String error="";try{body.run();}catch(Throwable t){error=t.toString();t.printStackTrace();}cases.add(Map.of("name",name,"pass",error.isEmpty(),"detail",error));System.out.println("[CarpetProbe] "+name+" "+(error.isEmpty()?"PASS":error));}
	private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
	private static void command(String command){server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command);}
	private static void resetEvents(boolean cancel){command("script in forbric_carpet_probe run global_swaps = 0; global_breaks = 0; global_cancel = "+cancel);nativeSwapVeto=false;nativeBreakVeto=false;}
	private static int count(String variable){return (int)Double.parseDouble(host.getGlobalVariable(variable).evalValue(null).getString());}
	private static void run()throws Exception {
		ServerLevel world=server.overworld();
		for(boolean updates:List.of(false,true)) {
			test("fill.lamp."+updates,()->{
				BlockPos p=new BlockPos(2,80,2),lamp=p.east();world.setBlock(p,Blocks.AIR.defaultBlockState(),18);world.setBlock(lamp,Blocks.REDSTONE_LAMP.defaultBlockState(),18);
				command("carpet fillUpdates "+updates);command("fill 2 80 2 2 80 2 minecraft:redstone_block");
				require(world.getBlockState(lamp).getValue(RedstoneLampBlock.LIT)==updates,"lamp did not respect fillUpdates="+updates);
			});
			test("fill.shape."+updates,()->{
				BlockPos p=new BlockPos(6,80,2);world.setBlock(p,Blocks.STONE.defaultBlockState(),18);world.setBlock(p.above(),Blocks.TORCH.defaultBlockState(),18);
				command("carpet fillUpdates "+updates);command("fill 6 80 2 6 80 2 minecraft:air");
				require(world.getBlockState(p.above()).isAir()==updates,"torch shape update did not respect fillUpdates="+updates);
			});
		}
		command("carpet fillUpdates true");
		for(boolean enabled:List.of(false,true)) {
			test("fluid.blackstone."+enabled,()->{CarpetSettings.renewableBlackstone=enabled;CarpetSettings.renewableDeepslate=false;BlockPos p=cell(world,80);world.setBlock(p.east(),Blocks.BLUE_ICE.defaultBlockState(),18);world.setBlock(p,Blocks.LAVA.defaultBlockState(),3);require(world.getBlockState(p).is(enabled?Blocks.BLACKSTONE:Blocks.LAVA),"result="+world.getBlockState(p));});
			test("fluid.deepslate."+enabled,()->{CarpetSettings.renewableBlackstone=false;CarpetSettings.renewableDeepslate=enabled;BlockPos p=cell(world,-10);world.setBlock(p.east(),Blocks.WATER.defaultBlockState(),18);world.setBlock(p,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,1),3);require(world.getBlockState(p).is(enabled?Blocks.COBBLED_DEEPSLATE:Blocks.COBBLESTONE),"result="+world.getBlockState(p));});
		}
		test("fluid.sourceStaysObsidian",()->{CarpetSettings.renewableDeepslate=true;BlockPos p=cell(world,-10);world.setBlock(p.east(),Blocks.WATER.defaultBlockState(),18);world.setBlock(p,Blocks.LAVA.defaultBlockState(),3);require(world.getBlockState(p).is(Blocks.OBSIDIAN),"result="+world.getBlockState(p));});
		test("fluid.aboveZeroStaysCobblestone",()->{BlockPos p=cell(world,80);world.setBlock(p.east(),Blocks.WATER.defaultBlockState(),18);world.setBlock(p,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,1),3);require(world.getBlockState(p).is(Blocks.COBBLESTONE),"result="+world.getBlockState(p));});
		test("fluid.basaltPrecedesBlackstone",()->{CarpetSettings.renewableBlackstone=true;BlockPos p=cell(world,80);world.setBlock(p.below(),Blocks.SOUL_SOIL.defaultBlockState(),18);world.setBlock(p.east(),Blocks.BLUE_ICE.defaultBlockState(),18);world.setBlock(p,Blocks.LAVA.defaultBlockState(),3);require(world.getBlockState(p).is(Blocks.BASALT),"result="+world.getBlockState(p));});
		test("fluid.noWaterNoDeepslate",()->{CarpetSettings.renewableBlackstone=false;BlockPos p=cell(world,-10);world.setBlock(p,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,1),3);require(world.getBlockState(p).is(Blocks.LAVA),"result="+world.getBlockState(p));});
		test("fluid.blackstone.neighbor",()->{CarpetSettings.renewableBlackstone=true;CarpetSettings.renewableDeepslate=false;BlockPos p=cell(world,80);world.setBlock(p,Blocks.LAVA.defaultBlockState(),3);world.setBlock(p.east(),Blocks.BLUE_ICE.defaultBlockState(),3);require(world.getBlockState(p).is(Blocks.BLACKSTONE),"result="+world.getBlockState(p));});
		test("fluid.deepslate.neighbor",()->{CarpetSettings.renewableBlackstone=false;CarpetSettings.renewableDeepslate=true;BlockPos p=cell(world,-10);world.setBlock(p,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,1),3);world.setBlock(p.east(),Blocks.WATER.defaultBlockState(),3);require(world.getBlockState(p).is(Blocks.COBBLED_DEEPSLATE),"result="+world.getBlockState(p));});
		CarpetSettings.renewableBlackstone=false;CarpetSettings.renewableDeepslate=false;

		// The genuine Carpet fake player and connection, created without fetching a skin/account over the network.
		var constructor=EntityPlayerMPFake.class.getDeclaredConstructor(MinecraftServer.class,ServerLevel.class,GameProfile.class,ClientInformation.class,boolean.class);constructor.setAccessible(true);
		GameProfile profile=new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000071"),"CarpetProbe");
		ClientInformation info=ClientInformation.createDefault();player=constructor.newInstance(server,world,profile,info,false);
		server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND),player,new CommonListenerCookie(profile,0,info,false));
		player.connection.markClientLoaded();player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
		command("script load forbric_carpet_probe");host=CarpetServer.scriptServer.getAppHostByName("forbric_carpet_probe");require(host!=null,"Scarpet event probe did not load");
		for(boolean cancel:List.of(true,false))test("swap.scarpetCancel."+cancel,()->{
			resetEvents(cancel);hands();swap();require(count("global_swaps")==1,"swap event count="+count("global_swaps"));
			require(player.getMainHandItem().is(cancel?Items.STONE:Items.DIRT)&&player.getOffhandItem().is(cancel?Items.DIRT:Items.STONE),"hands do not respect Scarpet cancellation");
		});
		test("swap.nativeVeto",()->{resetEvents(false);hands();nativeSwapVeto=true;try{swap();require(count("global_swaps")==0&&player.getMainHandItem().is(Items.STONE),"native cancellation was bypassed");}finally{nativeSwapVeto=false;}});
		for(GameType mode:List.of(GameType.CREATIVE,GameType.SURVIVAL))for(boolean cancel:List.of(true,false))test("break."+mode.getName()+".scarpetCancel."+cancel,()->{
			resetEvents(cancel);BlockPos p=new BlockPos(3,81,3);world.setBlock(p,Blocks.STONE.defaultBlockState(),3);player.gameMode.changeGameModeForPlayer(mode);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
			boolean removed=player.gameMode.destroyBlock(p);require(removed!=cancel&&world.getBlockState(p).isAir()!=cancel,"block did not respect Scarpet cancellation");
			require(count("global_breaks")==1,"break event count="+count("global_breaks"));
			require(host.getGlobalVariable("global_previous").evalValue(null).getString().contains("stone"),"event lost the original block state");
			if(cancel)require(player.getMainHandItem().getDamageValue()==0,"canceled break consumed tool durability");
		});
		test("break.nativeVeto",()->{resetEvents(false);BlockPos p=new BlockPos(3,81,3);world.setBlock(p,Blocks.STONE.defaultBlockState(),3);nativeBreakVeto=true;try{require(!player.gameMode.destroyBlock(p)&&world.getBlockState(p).is(Blocks.STONE)&&count("global_breaks")==0,"native break cancellation was bypassed");}finally{nativeBreakVeto=false;}});
	}
	private static BlockPos cell(ServerLevel w,int y){BlockPos p=new BlockPos(10,y,10);for(BlockPos q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1)))w.setBlock(q,Blocks.AIR.defaultBlockState(),18);w.setBlock(p.below(),Blocks.STONE.defaultBlockState(),18);return p;}
	private static void hands(){player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.DIRT));}
	private static void swap(){player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,BlockPos.ZERO,Direction.DOWN));}
	private static void finish(){try{Files.writeString(Path.of("carpet-probe.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("cases",cases)));}catch(Exception e){e.printStackTrace();}server.halt(false);}
}
