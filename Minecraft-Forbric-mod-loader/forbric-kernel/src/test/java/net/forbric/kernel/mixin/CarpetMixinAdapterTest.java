package net.forbric.kernel.mixin;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import net.forbric.kernel.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Reads the exact released Carpet handlers and real carrier bytecode; none of the injector bodies is a mock. */
@ResourceLock("system-properties")
class CarpetMixinAdapterTest {
	static final List<String> NAMES=List.of("Level_fillUpdatesMixin","ServerGamePacketListenerImpl_scarpetEventsMixin",
			"ServerPlayerGameMode_scarpetEventsMixin","LiquidBlock_renewableBlackstoneMixin","LiquidBlock_renewableDeepslateMixin");
	static ClassNode mixin(String name)throws Exception {
		return from(Path.of("build/compat-inputs/carpet/mods/fabric-carpet-26.2+v260616.jar"),"carpet/mixins/"+name);
	}
	static ClassNode target(String name) {
		Path root=Path.of(System.getProperty("forbric.stagedRoot"));
		try{return from(root.resolve(name.startsWith("net/minecraftforge/")?"forge-runtime/forge-runtime.jar":name.startsWith("net/neoforged/")?"neoforge-runtime/neoforge-runtime.jar":"merged-base/patched-mc-merged-26.2.jar"),name);}
		catch(Exception e){throw new AssertionError(e);}
	}
	static ClassNode from(Path jar,String name)throws Exception {
		assumeTrue(Files.isRegularFile(jar),"real fixture required: "+jar);
		try(ZipFile zip=new ZipFile(jar.toFile())){ClassNode c=new ClassNode();new ClassReader(zip.getInputStream(zip.getEntry(name+".class"))).accept(c,0);return c;}
	}
	static byte[] bytes(ClassNode c){ClassWriter w=new ClassWriter(0);c.accept(w);return w.toByteArray();}
	static int adapt(ClassNode c){return CarpetMixinAdapter.adapt(c,CarpetMixinAdapterTest::target)+CarpetFluidMixinAdapter.adapt(c,CarpetMixinAdapterTest::target);}
	static MethodNode method(ClassNode c,String n){return CarpetMixinAdapter.named(c,n);}
	static void verify(ClassNode c)throws Exception {for(MethodNode m:c.methods)if((m.access&Opcodes.ACC_ABSTRACT)==0)new Analyzer<>(new BasicVerifier()).analyze(c.name,m);}

	@Test void fillHooksMoveTogetherToTheActualNotificationBody()throws Exception {
		ClassNode c=mixin(NAMES.get(0));assertEquals(2,adapt(c));
		for(String name:List.of("addFillUpdatesInt","updateNeighborsMaybe"))assertEquals(List.of(CarpetMixinAdapter.LIVE_FILL),MixinFit.value(MixinFit.injectorOf(method(c,name)),"method"));
		verify(c);assertEquals(0,adapt(c));
	}
	@Test void handSwapRunsAtTheFirstNativeHandWriteAfterItsCancellationGate()throws Exception {
		ClassNode c=mixin(NAMES.get(1));assertEquals(1,adapt(c));
		AnnotationNode a=MixinFit.atNodes(MixinFit.injectorOf(method(c,"onHandSwap"))).getFirst();
		assertTrue(String.valueOf(MixinFit.value(a,"target")).contains("getItemSwappedToOffHand"));assertEquals(0,MixinFit.value(a,"ordinal"));
		assertEquals(true,MixinFit.value(MixinFit.injectorOf(method(c,"onHandSwap")),"cancellable"));verify(c);
	}
	@Test void breakKeepsTheOriginalCancellableCallbackAndThePreBreakState()throws Exception {
		ClassNode c=mixin(NAMES.get(2));assertEquals(1,adapt(c));MethodNode outer=method(c,"onBlockBroken");
		assertNull(MixinFit.value(MixinFit.injectorOf(outer),"locals"));
		assertEquals(2,MixinFit.value(outer.invisibleParameterAnnotations[2].getFirst(),"index"));
		assertTrue(String.valueOf(MixinFit.value(MixinFit.atNodes(MixinFit.injectorOf(outer)).getFirst(),"target")).contains("playerWillDestroy"));
		assertNull(MixinFit.injectorOf(method(c,"onBlockBroken$forbricOriginal")));verify(c);assertEquals(0,adapt(c));
	}
	@Test void blackstoneRunsOnlyAfterTheNativeRegistryDeclinesAnInteraction()throws Exception {
		ClassNode c=mixin(NAMES.get(3));assertEquals(2,adapt(c));
		for(String name:List.of("onPlace","neighborChanged")){
			MethodNode m=method(c,"forbric$carpetBlackstone$"+name);int nativeCall=-1,callback=-1,guard=-1;
			for(var i:m.instructions){int index=m.instructions.indexOf(i);if(i instanceof MethodInsnNode call){if(call.name.equals("call"))nativeCall=index;if(call.name.endsWith("$forbricOriginal"))callback=index;}if(i.getOpcode()==Opcodes.IFNE)guard=index;}
			assertTrue(nativeCall<guard&&guard<callback);assertEquals(1,MixinFit.value(MixinFit.injectorOf(m),"require"));
		}
		verify(c);assertEquals(0,adapt(c));
	}
	@Test void deepslateUsesTheSelectedWaterInteractionAndPreservesTheOriginalRuleBody()throws Exception {
		ClassNode c=mixin(NAMES.get(4));assertEquals(1,adapt(c));
		assertEquals(Set.of(CarpetFluidMixinAdapter.REGISTRIES.getFirst()),new HashSet<>(MixinFit.mixinTargets(c)));
		MethodNode original=method(c,"receiveFluidToDeepslate$forbricOriginal"),outer=method(c,"forbric$carpetDeepslate");
		assertTrue((original.access&Opcodes.ACC_STATIC)!=0);assertNull(MixinFit.injectorOf(original));
		assertEquals(5,MixinFit.value(outer.invisibleParameterAnnotations[3].getFirst(),"index"));
		assertEquals(1,MixinFit.atNodes(MixinFit.injectorOf(outer)).size());assertNotNull(method(c,"forbric$carpetFizz").desc);
		verify(c);assertEquals(0,adapt(c));
	}
	@Test void reshapedNativeFluidLocalRefusesTheWholeRetarget()throws Exception {
		ClassNode c=mixin(NAMES.get(4));byte[] before=bytes(c);
		assertEquals(0,CarpetFluidMixinAdapter.adapt(c,name->{ClassNode t=target(name);if(CarpetFluidMixinAdapter.REGISTRIES.contains(name))for(var m:t.methods)for(var i:m.instructions)if(i instanceof VarInsnNode v&&v.getOpcode()==Opcodes.ASTORE&&v.var==5)v.var=9;return t;}));
		assertArrayEquals(before,bytes(c));
	}
	@Test void vanillaAndDisabledRepairLeaveAllReleasedHandlersUntouched()throws Exception {
		Path vanilla=TestFixtures.vanillaJar();
		for(String name:NAMES){ClassNode c=mixin(name);byte[] before=bytes(c);java.util.function.Function<String,ClassNode> resolver=n->{try{return n.startsWith("net/minecraft/")?from(vanilla,n):null;}catch(Exception e){throw new AssertionError(e);}};
			assertEquals(0,CarpetMixinAdapter.adapt(c,resolver)+CarpetFluidMixinAdapter.adapt(c,resolver));assertArrayEquals(before,bytes(c));}
		String old=System.setProperty(CarpetMixinAdapter.PROPERTY,"off");try{for(String name:NAMES)assertEquals(0,adapt(mixin(name)));}finally{if(old==null)System.clearProperty(CarpetMixinAdapter.PROPERTY);else System.setProperty(CarpetMixinAdapter.PROPERTY,old);}
	}
}
