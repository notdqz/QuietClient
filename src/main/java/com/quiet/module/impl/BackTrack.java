package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.render.Palette;
import com.quiet.render.WorldBatch;
import com.quiet.render.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Holds back the server's movement updates for a nearby player for a moment, so the model YOU see (and aim at) lags
 * behind where the server really has them. A player backing away looks closer than they are, and the server still
 * accepts a hit up to a bit past normal reach, so you can hit what looks barely in range. If they are really closer
 * than they look, the held updates are released at once so you never aim at a stale spot.
 * "Show real position" draws a box where the server actually has them. Hooks: ClientPacketListenerMixin.
 */
public class BackTrack extends Module implements WorldRender.Layer {
	private record Held(ClientboundMoveEntityPacket pkt, long at) {}

	private static final class Track {
		final ArrayDeque<Held> q = new ArrayDeque<>();
		Vec3 server;
	}

	private static BackTrack inst;
	private static boolean releasing;

	private final Setting.Num delay = num("Delay (ms)", 120, 20, 400, 10);
	private final Setting.Num range = num("Range", 6.0, 3.0, 10.0, 0.5);
	private final Setting.Bool showServer = bool("Show real position", true);
	private final Setting.Mode color = mode("Box color", "Red", Palette.NAMES);
	private final Map<Integer, Track> tracks = new HashMap<>();

	public BackTrack() {
		super("BackTrack", Category.COMBAT);
		inst = this;
		WorldRender.add(this, this);
		color.visibleWhen(showServer::get);
	}

	private static boolean live() {
		Minecraft mc = Minecraft.getInstance();
		return inst != null && inst.isEnabled() && !releasing && mc.isSameThread() && mc.level != null && mc.player != null;
	}

	/** HEAD of handleMoveEntity. Returns true to swallow the packet (it is replayed later). */
	public static boolean onMove(ClientboundMoveEntityPacket pkt) {
		if (!live()) return false;
		Minecraft mc = Minecraft.getInstance();
		Entity e = pkt.getEntity(mc.level);
		if (!(e instanceof Player) || e == mc.player) return false;

		Track t = inst.tracks.get(e.getId());
		if (t == null) {
			if (mc.player.distanceTo(e) > inst.range.get()) return false;
			t = new Track();
			t.server = e.position();
			inst.tracks.put(e.getId(), t);
		}
		t.q.add(new Held(pkt, System.currentTimeMillis()));
		if (pkt.hasPosition())
			t.server = t.server.add(pkt.getXa() / 4096.0, pkt.getYa() / 4096.0, pkt.getZa() / 4096.0);
		return true;
	}

	/** A teleport / position sync for an entity: play back everything held for it first so the order stays right. */
	public static void onSync(int entityId) {
		if (!live()) return;
		Track t = inst.tracks.remove(entityId);
		if (t != null) while (!t.q.isEmpty()) apply(t.q.poll().pkt());
	}

	private static void apply(ClientboundMoveEntityPacket pkt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) return;
		releasing = true;
		try { mc.getConnection().handleMoveEntity(pkt); } finally { releasing = false; }
	}

	@Override
	protected void onDisable() {
		for (Track t : tracks.values()) while (!t.q.isEmpty()) apply(t.q.poll().pkt());
		tracks.clear();
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<Integer, Track>> it = tracks.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Track> en = it.next();
			Track t = en.getValue();
			Entity e = mc.level.getEntity(en.getKey());

			boolean flush = e == null;
			if (!flush) {
				double shown = mc.player.position().distanceTo(e.position());
				double real = mc.player.position().distanceTo(t.server);
				if (real < shown - 0.05) flush = true;       // really closer than they look: show the truth
				if (shown > range.get() + 3) flush = true;   // out of range
			}
			while (!t.q.isEmpty() && (flush || now - t.q.peek().at() >= delay.get())) apply(t.q.poll().pkt());
			if (t.q.isEmpty()) it.remove();
		}
	}

	@Override
	public void extract(WorldBatch b, Minecraft mc, float pt) {
		if (!showServer.get()) return;
		int c = Palette.color(color.mode());
		for (Map.Entry<Integer, Track> en : tracks.entrySet()) {
			Entity e = mc.level.getEntity(en.getKey());
			if (e == null || en.getValue().q.isEmpty()) continue;
			Vec3 s = en.getValue().server;
			AABB box = e.getBoundingBox().move(s.x - e.getX(), s.y - e.getY(), s.z - e.getZ());
			b.highlight(box, c, Palette.alpha(c, 0x30), 1.5f);
		}
	}
}
