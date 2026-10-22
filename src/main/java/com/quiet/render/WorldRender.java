package com.quiet.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.quiet.Quiet;
import com.quiet.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Real in-world drawing (Fabric's custom render pipeline recipe for 1.21.11): one pipeline with the depth test
 * turned off, so boxes and lines show through walls but stay fixed to the world.
 */
public final class WorldRender {
	public interface Layer { void extract(WorldBatch b, Minecraft mc, float pt); }

	private record Entry(Module m, Layer l) {}

	private static final List<Entry> LAYERS = new ArrayList<>();
	private static boolean hooked;

	private static final RenderPipeline PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Quiet.id("pipeline/through_walls"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.build());

	private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
	private static MappableRingBuffer vertexBuffer;

	public static void add(Module m, Layer l) {
		LAYERS.add(new Entry(m, l));
		if (!hooked) {
			hooked = true;
			WorldRenderEvents.BEFORE_TRANSLUCENT.register(WorldRender::onRender);
		}
	}

	private static void onRender(WorldRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		boolean any = false;
		for (Entry e : LAYERS) if (e.m().isEnabled()) { any = true; break; }
		if (!any) return;

		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 cam = ctx.worldState().cameraRenderState.pos;
		PoseStack matrices = ctx.matrices();

		BufferBuilder buffer = new BufferBuilder(ALLOCATOR, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
		WorldBatch batch = new WorldBatch(buffer, matrices.last().pose(), cam);
		for (Entry e : LAYERS) {
			if (!e.m().isEnabled()) continue;
			try {
				e.l().extract(batch, mc, pt);
			} catch (Throwable t) {
				Quiet.LOGGER.error("World layer for {} crashed and was disabled", e.m().name(), t);
				e.m().setEnabled(false);
			}
		}

		MeshData mesh = buffer.build();
		if (mesh == null) return;       // nothing was added this frame
		draw(mc, mesh);
	}

	private static void draw(Minecraft client, MeshData builtBuffer) {
		MeshData.DrawState drawParameters = builtBuffer.drawState();
		VertexFormat format = drawParameters.format();

		// ---- upload
		int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();
		if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
			if (vertexBuffer != null) vertexBuffer.close();
			vertexBuffer = new MappableRingBuffer(() -> Quiet.MOD_ID + " through-walls buffer",
					GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
		}
		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
				vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
			MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
		}
		GpuBuffer vertices = vertexBuffer.currentBuffer();

		// ---- draw
		GpuBuffer indices;
		VertexFormat.IndexType indexType;
		if (PIPELINE.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
			builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
			indices = PIPELINE.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
			indexType = builtBuffer.drawState().indexType();
		} else {
			RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(PIPELINE.getVertexFormatMode());
			indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
			indexType = shapeIndexBuffer.type();
		}

		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
		try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> Quiet.MOD_ID + " through-walls pass",
				client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
				client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
			renderPass.setPipeline(PIPELINE);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", dynamicTransforms);
			renderPass.setVertexBuffer(0, vertices);
			renderPass.setIndexBuffer(indices, indexType);
			renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
		}

		vertexBuffer.rotate();
		builtBuffer.close();
	}
}
