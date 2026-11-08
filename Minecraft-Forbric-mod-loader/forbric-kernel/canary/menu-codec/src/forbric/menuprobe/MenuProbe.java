package forbric.menuprobe;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.impl.menu.Networking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Opens Farmer's Delight's cooking pot the way a client's right click does; only staged in isolated gate worlds. */
public final class MenuProbe implements ModInitializer {
	private static final Identifier POT = Identifier.fromNamespaceAndPath("farmersdelight", "cooking_pot");
	private static MinecraftServer server;
	private static int ticks;
	private static final List<Map<String,Object>> cases=new ArrayList<>();
	@Override public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(s->{server=s;s.overworld().setChunkForced(0,0,true);});
		ServerTickEvents.END_SERVER_TICK.register(s->{if(s!=server||++ticks!=30)return;try{run();}catch(Throwable t){test("infrastructure",()->{throw t;});}finally{finish();}});
	}
	interface Case { void run()throws Throwable; }
	private static void test(String name,Case body){String error="";try{body.run();}catch(Throwable t){error=t.toString();t.printStackTrace();}cases.add(Map.of("name",name,"pass",error.isEmpty(),"detail",error));System.out.println("[MenuProbe] "+name+" "+(error.isEmpty()?"PASS":error));}
	private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
	private static void run() {
		MenuType<?> type=BuiltInRegistries.MENU.getValue(POT);
		test("menu.extended",()->require(type instanceof ExtendedMenuType<?,?>,"farmersdelight:cooking_pot is "+type));
		test("menu.codec",()->require(Networking.CODEC_BY_ID.containsKey(POT),"fabric-menu-api-v1 recorded no codec for "+POT));
		test("pot.opens",()->{
			ServerLevel world=server.overworld();BlockPos pot=new BlockPos(2,80,2);Block block=BuiltInRegistries.BLOCK.getValue(POT);
			world.setBlock(pot,block.defaultBlockState(),3);require(world.getBlockState(pot).is(block),"no cooking pot at "+pot);
			GameProfile profile=new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000072"),"MenuProbe");
			ServerPlayer player=new ServerPlayer(server,world,profile,ClientInformation.createDefault());
			// A connection whose packets stay in an in-memory channel, where the probe reads what a client would receive.
			EmbeddedChannel channel=new EmbeddedChannel();Connection connection=new Connection(PacketFlow.SERVERBOUND);
			Field field=Connection.class.getDeclaredField("channel");field.setAccessible(true);field.set(connection,channel);
			player.connection=new ServerGamePacketListenerImpl(server,connection,player,CommonListenerCookie.createInitial(profile,false));
			player.connection.markClientLoaded();player.snapTo(pot.getX()+0.5,pot.getY()+1,pot.getZ()+0.5);
			// The packet a client sends on right click, handled on the server thread as the game handles it.
			player.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pot),Direction.UP,pot,false),1));
			require(player.containerMenu!=player.inventoryMenu&&player.containerMenu.getType()==type,"the cooking pot's menu did not open: "+player.containerMenu);
			List<Object> sent=new ArrayList<>();for(Object packet;(packet=channel.readOutbound())!=null;)sent.add(packet instanceof ClientboundCustomPayloadPacket custom?custom.payload():packet);
			require(sent.stream().anyMatch(p->p instanceof Networking.OpenScreenPayload<?> open&&open.identifier().equals(POT)),"no fabric-menu-api open_screen for "+POT+" was sent: "+sent);
		});
	}
	private static void finish(){try{Files.writeString(Path.of("menu-probe.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("cases",cases)));}catch(Exception e){e.printStackTrace();}server.halt(false);}
}
