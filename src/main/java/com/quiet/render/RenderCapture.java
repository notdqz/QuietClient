package com.quiet.render;

import com.quiet.Quiet;
import com.quiet.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Grabs the world's view + projection matrices and camera position every frame (LevelRendererMixin), so
 * world positions can be projected onto the 2D HUD. All the ESP-style modules draw through this.
 */
public final class RenderCapture {
	private static final Matrix4f A = new Matrix4f(), B = new Matrix4f(), VIEW = new Matrix4f(), PROJ = new Matrix4f();
	private static final Vector4f V = new Vector4f();
	private static boolean hasA, hasB, hasCam, resolved, valid, warned;
	private static double cx, cy, cz;

	public static void first(Matrix4f m) { A.set(m); hasA = true; resolved = false; }
	public static void second(Matrix4f m) { B.set(m); hasB = true; resolved = false; }

	public static void camera(Camera c) {
		try {
			Vec3 p = ((CameraAccessor) (Object) c).quiet$position();
			cx = p.x; cy = p.y; cz = p.z;
			hasCam = true;
		} catch (Throwable t) {
			hasCam = false;
		}
		resolved = false;
	}

	private static boolean resolve() {
		if (resolved) return valid;
		resolved = true;
		valid = false;
		if (!hasA || !hasB || !hasCam) return false;
		// the projection matrix is the one with the perspective -1 in m23, so the order doesn't matter
		boolean aProj = Math.abs(A.m23() + 1f) < 1e-3f, bProj = Math.abs(B.m23() + 1f) < 1e-3f;
		if (bProj && !aProj) { VIEW.set(A); PROJ.set(B); valid = true; }
		else if (aProj && !bProj) { VIEW.set(B); PROJ.set(A); valid = true; }
		else if (!warned) { warned = true; Quiet.LOGGER.warn("Quiet: couldn't identify the projection matrix; ESP overlays are off"); }
		return valid;
	}

	public static boolean ready() { return resolve(); }

	/** GUI-space {x, y} for a world position, or null if it's behind the camera. */
	public static float[] project(double x, double y, double z, int gw, int gh) {
		if (!resolve()) return null;
		V.set((float) (x - cx), (float) (y - cy), (float) (z - cz), 1f);
		VIEW.transform(V);
		PROJ.transform(V);
		if (V.w <= 0.01f) return null;
		float nx = V.x / V.w, ny = V.y / V.w;
		return new float[]{(nx * 0.5f + 0.5f) * gw, (1f - (ny * 0.5f + 0.5f)) * gh};
	}

	/** Like project(), but a point behind you gets pushed to the screen edge in the right direction (for tracers). */
	public static float[] projectClamped(double x, double y, double z, int gw, int gh) {
		if (!resolve()) return null;
		V.set((float) (x - cx), (float) (y - cy), (float) (z - cz), 1f);
		VIEW.transform(V);
		PROJ.transform(V);
		boolean behind = V.w <= 0.01f;
		float w = behind ? 0.01f : V.w;
		float nx = V.x / w, ny = V.y / w;
		float ox = nx * gw / 2f, oy = -ny * gh / 2f;
		if (behind) { ox = -ox; oy = -oy; }
		float k = Math.min((gw / 2f - 6f) / Math.max(Math.abs(ox), 1e-3f), (gh / 2f - 6f) / Math.max(Math.abs(oy), 1e-3f));
		if (behind || k < 1f) { ox *= k; oy *= k; }
		return new float[]{gw / 2f + ox, gh / 2f + oy};
	}

	public static float[][] corners(AABB b, int gw, int gh) {
		float[][] out = new float[8][];
		for (int i = 0; i < 8; i++)
			out[i] = project((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY,
					(i & 4) == 0 ? b.minZ : b.maxZ, gw, gh);
		return out;
	}

	/** Screen rectangle {x1, y1, x2, y2} around a box, or null if any corner is behind the camera. */
	public static float[] projectBox(AABB b, int gw, int gh) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		for (float[] c : corners(b, gw, gh)) {
			if (c == null) return null;
			minX = Math.min(minX, c[0]); maxX = Math.max(maxX, c[0]);
			minY = Math.min(minY, c[1]); maxY = Math.max(maxY, c[1]);
		}
		return new float[]{minX, minY, maxX, maxY};
	}
}
