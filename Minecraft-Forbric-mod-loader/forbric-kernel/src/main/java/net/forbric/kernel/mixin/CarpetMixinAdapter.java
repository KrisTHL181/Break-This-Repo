/* Copyright 2026 The Forbric Project. Licensed under the Apache License, Version 2.0. */
package net.forbric.kernel.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import net.forbric.kernel.util.ForbricLog;

/** Restores Carpet's authored update and cancellable player callbacks at their merged-game equivalents. */
public final class CarpetMixinAdapter {
	public static final String PROPERTY = "forbric.carpetMixins";
	static final String PREFIX = "carpet/mixins/";
	static final String LEVEL = "net/minecraft/world/level/Level";
	static final String POS = "Lnet/minecraft/core/BlockPos;";
	static final String STATE = "Lnet/minecraft/world/level/block/state/BlockState;";
	static final String CIR = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";
	static final String GAME_MODE = "net/minecraft/server/level/ServerPlayerGameMode";
	static final String SERVER_LEVEL = "net/minecraft/server/level/ServerLevel";
	static final String BLOCK = "net/minecraft/world/level/block/Block";
	static final String ENTITY = "Lnet/minecraft/world/level/block/entity/BlockEntity;";
	static final String OLD_FILL = "setBlock(" + POS + STATE + "II)Z";
	static final String LIVE_FILL = "markAndNotifyBlock(" + POS + "Lnet/minecraft/world/level/chunk/LevelChunk;" + STATE + STATE + "II)V";
	private CarpetMixinAdapter() { }

	public static boolean enabled() { return !"off".equalsIgnoreCase(System.getProperty(PROPERTY, "on")); }

	public static int adapt(ClassNode mixin, Function<String, ClassNode> targets) {
		if (!enabled() || !mixin.name.startsWith(PREFIX)) return 0;
		int changed = switch (mixin.name.substring(PREFIX.length())) {
			case "Level_fillUpdatesMixin" -> fill(mixin, targets.apply(LEVEL));
			case "ServerGamePacketListenerImpl_scarpetEventsMixin" -> swap(mixin, targets.apply("net/minecraft/server/network/ServerGamePacketListenerImpl"));
			case "ServerPlayerGameMode_scarpetEventsMixin" -> blockBreak(mixin, targets.apply(GAME_MODE));
			default -> 0;
		};
		if (changed > 0) ForbricLog.info("[Forbric/Carpet] restored %d callback(s) in %s", changed, mixin.name);
		return changed;
	}

	private static int fill(ClassNode mixin, ClassNode target) {
		if (target == null) return 0;
		MethodNode old = selector(target, OLD_FILL), live = selector(target, LIVE_FILL);
		MethodNode flag = named(mixin, "addFillUpdatesInt"), notify = named(mixin, "updateNeighborsMaybe");
		String update = "L" + LEVEL + ";updateNeighborsAt(" + POS + "L" + BLOCK + ";)V";
		if (old == null || live == null || flag == null || notify == null || count(old, update) != 0
				|| count(live, update) != 1 || constants(live, 16) != 1 || constants(old, 16) != 0) return 0;
		AnnotationNode a = MixinFit.injectorOf(flag), b = MixinFit.injectorOf(notify);
		if (!"(I)I".equals(flag.desc) || !("(L"+LEVEL+";"+POS+"L"+BLOCK+";)V").equals(notify.desc)
				|| !selects(a, OLD_FILL) || !selects(b, OLD_FILL)
				|| !"Lorg/spongepowered/asm/mixin/injection/ModifyConstant;".equals(a.desc)
				|| !"Lorg/spongepowered/asm/mixin/injection/Redirect;".equals(b.desc)) return 0;
		List<AnnotationNode> points = MixinFit.atNodes(b);
		if (points.size() != 1 || !update.equals(MixinFit.value(points.getFirst(), "target"))) return 0;
		set(a, "method", List.of(LIVE_FILL)); set(b, "method", List.of(LIVE_FILL));
		return 2;
	}

	private static int swap(ClassNode mixin, ClassNode target) {
		MethodNode handler = named(mixin, "onHandSwap");
		MethodNode host = target == null ? null : selector(target, "handlePlayerAction(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;)V");
		if (handler == null || host == null) return 0;
		AnnotationNode inject = MixinFit.injectorOf(handler);
		if (!selects(inject, "handlePlayerAction")) return 0;
		List<AnnotationNode> ats = MixinFit.atNodes(inject);
		String old = "Lnet/minecraft/server/level/ServerPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;";
		String live = "Lnet/neoforged/neoforge/event/entity/living/LivingSwapItemsEvent$Hands;getItemSwappedToOffHand()Lnet/minecraft/world/item/ItemStack;";
		if (ats.size() != 1 || !old.equals(MixinFit.value(ats.getFirst(), "target"))
				|| !Integer.valueOf(1).equals(MixinFit.value(ats.getFirst(), "ordinal"))
				|| count(host, old) != 1 || count(host, live) != 1) return 0;
		// The new anchor is after the native event's cancellation check and before the first hand is written.
		set(ats.getFirst(), "target", live); set(ats.getFirst(), "ordinal", 0);
		return 1;
	}

	private static int blockBreak(ClassNode mixin, ClassNode target) {
		MethodNode handler = named(mixin, "onBlockBroken");
		MethodNode host = target == null ? null : selector(target, "destroyBlock(" + POS + ")Z");
		if (handler == null || host == null || mixin.methods.stream().anyMatch(m -> m.name.equals("onBlockBroken$forbricOriginal"))) return 0;
		String originalDesc = "(" + POS + CIR + ENTITY + "L" + BLOCK + ";" + STATE + ")V";
		AnnotationNode inject = MixinFit.injectorOf(handler);
		String old = "L" + SERVER_LEVEL + ";removeBlock(" + POS + "Z)Z";
		String live = "L" + BLOCK + ";playerWillDestroy(L" + LEVEL + ";" + POS + STATE + "Lnet/minecraft/world/entity/player/Player;)" + STATE;
		if (!originalDesc.equals(handler.desc) || !selects(inject, "destroyBlock") || count(host, old) != 0 || count(host, live) != 1) return 0;
		List<AnnotationNode> ats = MixinFit.atNodes(inject);
		if (ats.size() != 1 || !old.equals(MixinFit.value(ats.getFirst(), "target")) || !Boolean.TRUE.equals(MixinFit.value(inject, "cancellable"))) return 0;
		// The captured state is the original getBlockState(pos), before playerWillDestroy can replace it.
		int reads = 0;
		for (var i : host.instructions) if (i instanceof MethodInsnNode c && c.owner.equals(SERVER_LEVEL) && c.name.equals("getBlockState")
				&& c.desc.equals("(" + POS + ")" + STATE)) {
			if (!(next(c) instanceof VarInsnNode store) || store.getOpcode() != Opcodes.ASTORE || store.var != 2) return 0;
			reads++;
		}
		if (reads != 1 || java.util.stream.StreamSupport.stream(host.instructions.spliterator(),false)
				.filter(i -> i instanceof VarInsnNode v && v.getOpcode()==Opcodes.ASTORE && v.var==2).count()!=1) return 0;
		handler.name += "$forbricOriginal"; handler.visibleAnnotations.remove(inject);
		remove(inject, "locals"); set(ats.getFirst(), "target", live);
		MethodNode outer = new MethodNode(Opcodes.ACC_PRIVATE, "onBlockBroken", "(" + POS + CIR + STATE + ")V", null, null);
		outer.visibleAnnotations = new ArrayList<>(List.of(inject));
		outer.invisibleParameterAnnotations = local(3, 2, 2);
		outer.invisibleAnnotableParameterCount = 3;
		InsnList code = outer.instructions;
		for (int slot : new int[] {0, 1, 2}) code.add(new VarInsnNode(Opcodes.ALOAD, slot));
		code.add(new VarInsnNode(Opcodes.ALOAD, 0));
		code.add(new FieldInsnNode(Opcodes.GETFIELD, mixin.name, "level", "L" + SERVER_LEVEL + ";"));
		code.add(new VarInsnNode(Opcodes.ALOAD, 1));
		code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, SERVER_LEVEL, "getBlockEntity", "(" + POS + ")" + ENTITY, false));
		code.add(new VarInsnNode(Opcodes.ALOAD, 3));
		code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, STATE.substring(1, STATE.length()-1), "getBlock", "()L" + BLOCK + ";", false));
		code.add(new VarInsnNode(Opcodes.ALOAD, 3));
		code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, mixin.name, handler.name, handler.desc, false));
		code.add(new InsnNode(Opcodes.RETURN)); outer.maxLocals = 4; outer.maxStack = 7;
		mixin.methods.add(outer); return 1;
	}

	@SuppressWarnings("unchecked")
	static List<AnnotationNode>[] local(int params, int param, int index) {
		List<AnnotationNode>[] result = new List[params];
		AnnotationNode local = new AnnotationNode("Lcom/llamalad7/mixinextras/sugar/Local;");
		local.values = new ArrayList<>(List.of("index", index)); result[param] = new ArrayList<>(List.of(local)); return result;
	}
	static boolean selects(AnnotationNode a, String selector) { return a != null && MixinFit.stringList(MixinFit.value(a,"method")).equals(List.of(selector)); }
	static MethodNode named(ClassNode c, String name) { return c.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElse(null); }
	static MethodNode selector(ClassNode c, String s) { return c.methods.stream().filter(m -> (m.name+m.desc).equals(s)).findFirst().orElse(null); }
	static int count(MethodNode m, String member) { int n=0;for(var i:m.instructions)if(i instanceof MethodInsnNode c && member.equals("L"+c.owner+";"+c.name+c.desc))n++;return n; }
	static int constants(MethodNode m,int value) { int n=0;for(var i:m.instructions)if(i instanceof IntInsnNode c && c.operand==value)n++;return n; }
	static AbstractInsnNode next(AbstractInsnNode n) { do {n=n.getNext();}while(n!=null&&n.getOpcode()<0);return n; }
	static AbstractInsnNode previous(AbstractInsnNode n) { do {n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n; }
	static void set(AnnotationNode a,String key,Object value) { if(a.values==null)a.values=new ArrayList<>();for(int i=0;i<a.values.size();i+=2)if(key.equals(a.values.get(i))){a.values.set(i+1,value);return;}a.values.add(key);a.values.add(value); }
	static void remove(AnnotationNode a,String key) { if(a.values==null)return;for(int i=0;i<a.values.size();i+=2)if(key.equals(a.values.get(i))){a.values.remove(i+1);a.values.remove(i);return;} }
}
