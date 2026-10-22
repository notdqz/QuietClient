package com.quiet.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;

/**
 * Collects quads in WORLD space for one frame. Everything added here is drawn by WorldRender through walls,
 * so boxes and lines are stuck to the thing they mark instead of floating on the screen.
 */
public final class WorldBatch {
	private final BufferBuilder buf;
	private final Matrix4fc pose;
	private final Vec3 cam;
	private int verts;

	WorldBatch(BufferBuilder buf, Matrix4fc pose, Vec3 cam) { this.buf = buf; this.pose = pose; this.cam = cam; }

	int vertexCount() { return verts; }

	private void vtx(double x, double y, double z, float r, float g, float b, float a) {
		buf.addVertex(pose, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).setColor(r, g, b, a);
		verts++;
	}

	private void quad(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int argb) {
		float a = (argb >>> 24 & 0xFF) / 255f, r = (argb >> 16 & 0xFF) / 255f, g = (argb >> 8 & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
		vtx(p0.x, p0.y, p0.z, r, g, b, a);
		vtx(p1.x, p1.y, p1.z, r, g, b, a);
		vtx(p2.x, p2.y, p2.z, r, g, b, a);
		vtx(p3.x, p3.y, p3.z, r, g, b, a);
	}

	private static Vec3 corner(AABB b, int i) {
		return new Vec3((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
	}

	/** Translucent solid box. */
	public void fillBox(AABB bx, int argb) {
		double x0 = bx.minX, y0 = bx.minY, z0 = bx.minZ, x1 = bx.maxX, y1 = bx.maxY, z1 = bx.maxZ;
		quad(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), argb);   // front
		quad(new Vec3(x1, y0, z0), new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), argb);   // back
		quad(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), argb);   // left
		quad(new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), argb);   // right
		quad(new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), new Vec3(x1, y1, z0), new Vec3(x0, y1, z0), argb);   // top
		quad(new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x0, y0, z1), argb);   // bottom
	}

	/** A line drawn as a thin ribbon that always faces the camera; `pixels` is roughly its on-screen thickness. */
	public void line(Vec3 a, Vec3 b, int argb, float pixels) {
		Vec3 pa = a.subtract(cam), pb = b.subtract(cam);
		Vec3 mid = pa.add(pb).scale(0.5);
		Vec3 side = pb.subtract(pa).cross(mid);
		double sl = side.length();
		if (sl < 1.0E-9) return;
		double half = pixels * 0.0008 * Math.max(1.0, mid.length());
		side = side.scale(half / sl);
		Vec3 a1 = a.add(side), a2 = a.subtract(side), b1 = b.add(side), b2 = b.subtract(side);
		quad(a1, a2, b2, b1, argb);
		quad(b1, b2, a2, a1, argb);     // second winding so it shows from both sides
	}

	/** The 12 edges of a box. */
	public void edgeBox(AABB bx, int argb, float pixels) {
		for (int i = 0; i < 8; i++)
			for (int j = i + 1; j < 8; j++)
				if (Integer.bitCount(i ^ j) == 1) line(corner(bx, i), corner(bx, j), argb, pixels);
	}

	/** Filled box plus outline in one call. */
	public void highlight(AABB bx, int edgeArgb, int fillArgb, float pixels) {
		if ((fillArgb >>> 24) != 0) fillBox(bx, fillArgb);
		edgeBox(bx, edgeArgb, pixels);
	}
}
