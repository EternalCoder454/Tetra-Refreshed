package se.mickelus.tetra.blocks.holo;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import se.mickelus.mutil.util.RotationHelper;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.gui.GuiColors;
import se.mickelus.tetra.util.Lherper;

public class HolosphereEntityRenderer implements BlockEntityRenderer<HolosphereBlockEntity, HolosphereEntityRenderer.State> {
    public static final SpriteId material = new SpriteId(TextureAtlas.LOCATION_BLOCKS,
            Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "block/holosphere_hud"));
    private final SpriteGetter sprites;

    public HolosphereEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HolosphereBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);

        state.scans.clear();
        state.visible = false;

        long timestamp = entity.getScanModeTimestamp();
        Level level = entity.getLevel();
        if (level == null || timestamp == 0 || timestamp < 0 && level.getGameTime() + timestamp > 20) {
            return;
        }

        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return;
        }

        state.visible = true;
        state.timestamp = timestamp;
        state.time = level.getGameTime() + partialTicks;
        state.light = getLightColor(level, entity.getBlockPos());

        double angle = RotationHelper.getHorizontalAngle(camera.getEyePosition(partialTicks), Vec3.atCenterOf(entity.getBlockPos()));
        state.rotation = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
        state.rotation.mul(Axis.YP.rotationDegrees((float) (angle / Math.PI * 180)));

        ChunkPos chunkPos = ChunkPos.containing(entity.getBlockPos());
        entity.getScanResults().stream()
                .filter(scan -> scan.timestamp() <= level.getGameTime())
                .forEach(scan -> state.scans.add(new Marker(scan, scan.chunkX() - chunkPos.x(), scan.chunkZ() - chunkPos.z())));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) {
            return;
        }

        // the quad building below is unchanged, it just runs inside a recorded draw now and takes
        // its transform from the pose the collector hands back rather than from a live stack
        collector.submitCustomGeometry(poseStack, material.renderType(RenderTypes::entityTranslucent), (pose, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(pose);

            renderBackdrop(consumer, local, state.rotation, state.light, state.time, state.timestamp);

            for (Marker marker : state.scans) {
                long openTimestamp = state.timestamp + (Math.abs(marker.x()) + Math.abs(marker.z()));
                renderMarker(consumer, local, sprites.get(material), state.rotation, state.time, openTimestamp,
                        state.light, 0.5f + marker.x() * 1 / 16f, 0, 0.5f + marker.z() * 1 / 16f, marker.scan());
            }
        });
    }

    private record Marker(HolosphereBlockEntity.ScanResult scan, int x, int z) {}

    public static class State extends BlockEntityRenderState {
        public final List<Marker> scans = new ArrayList<>();
        public boolean visible;
        public long timestamp;
        public float time;
        public int light;
        public Quaternionf rotation = new Quaternionf();
    }

    public void renderBackdrop(VertexConsumer consumer, PoseStack poseStack, Quaternionf rotation, int light, float time, long openTimestamp) {
        float animFast = openTimestamp > 0
                ? Lherper.easeOut(Mth.clampedMap(time - openTimestamp, 0, 5, 0, 1))
                : Lherper.easeOut(Lherper.easeOut(Mth.clampedMap(time + openTimestamp - 2, 0, 5, 1, 0)));
        float animSlow = openTimestamp > 0
                ? Lherper.easeOut(Lherper.easeOut(Mth.clampedMap(time - openTimestamp - 5, 0, 5, 0, 1)))
                : Lherper.easeOut(Mth.clampedMap(time + openTimestamp, 0, 5, 1, 0));
        float animSlow2 = openTimestamp > 0
                ? Lherper.easeOut(Lherper.easeOut(Mth.clampedMap(time - openTimestamp - 10, 0, 5, 0, 1)))
                : Lherper.easeOut(Mth.clampedMap(time + openTimestamp, 0, 5, 1, 0));

        if (animFast > 0) {
            drawQuad(consumer, poseStack, rotation, sprites.get(material), light, 1, animFast * 16, 6, 0,
                    0.5f, (15.5f) / 16, 0.5f, 0xffffff, animFast, -0.0025f, 1);
        }

        if (animSlow > 0) {
            drawQuad(consumer, poseStack, rotation, sprites.get(material), light, 1, 1, 5, 0,
                    0.5f, (6.5f - animSlow * 0.5f) / 16, 0.5f, 0xffffff, animSlow, -0.0025f, 1);
            drawQuad(consumer, poseStack, rotation, sprites.get(material), light, 1, 1, 5, 0,
                    0.5f, (24.5f + animSlow * 0.5f) / 16, 0.5f, 0xffffff, animSlow, -0.0025f, 1);
        }

        if (animSlow2 > 0) {
            float offset = (20f + animSlow2 * 1) / 16;
            Quaternionf up = Axis.YN.rotationDegrees(45);
            up.mul(Axis.XN.rotationDegrees(90));

            drawQuad(consumer, poseStack, up, sprites.get(material), light, 3, 3, 2, 3,
                    offset + 0.5f, (4.5f + animSlow2 * 1.5f) / 16, 0.5f, 0xffffff, animSlow2, 0, 1);
            drawQuad(consumer, poseStack, up, sprites.get(material), light, 3, 3, 2, 9,
                    0.5f - offset, (4.5f + animSlow2 * 1.5f) / 16, 0.5f, 0xffffff, animSlow2, 0, 1);
            drawQuad(consumer, poseStack, up, sprites.get(material), light, 3, 3, 2, 6,
                    0.5f, (4.5f + animSlow2 * 1.5f) / 16, offset + 0.5f, 0xffffff, animSlow2, 0, 1);
            drawQuad(consumer, poseStack, up, sprites.get(material), light, 3, 3, 2, 0,
                    0.5f, (4.5f + animSlow2 * 1.5f) / 16, 0.5f - offset, 0xffffff, animSlow2, 0, 1);
        }
    }

    public void renderMarker(VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite sprite, Quaternionf rotation, float time, long openTimestamp, int light, float x, float y, float z,
            HolosphereBlockEntity.ScanResult scan) {
        float anim = calculateMarkerAnimation(time, openTimestamp, scan.timestamp());

        if (openTimestamp == 0 || anim == 0) {
            return;
        }

        double height = scan.height();
        float ry = -0.1f + (anim * (0.1f + (float) (0.005f * height)));
        int color = scan.structures().isEmpty()
                ? Lherper.lerpColors(Mth.clampedMap(scan.temperature() / 3f, 0, 1, 0, 1f), 0xc5c4ff, 0xc2ffd4, 0xfbffc9, 0xffcbbd)
                : GuiColors.scanner;
        color = Lherper.lerpColors((float) Mth.clampedMap(height, 40, 140, 0.5f, 1f), 0, color);
        float opacity = 0.9f * anim;
//        drawQuad(consumer, poseStack, rotation, sprite, light, 2, 2, 0, 0, x, ry + 2.5f / 16, z, 0, opacity * 0.3f, -0.001f, 1.125f);
        drawQuad(consumer, poseStack, rotation, sprite, light, 1, 4, 7, 0, x, ry - 0.01f, z, 0, opacity * 0.3f, -0.001f, 1.125f);
        drawQuad(consumer, poseStack, rotation, sprite, light, 1, 4, 7, 0, x, ry, z, color, opacity);
//        for (int k = 0; k < 3; k++) {
//            drawQuad(consumer, poseStack, rotation, sprite, light, 1, 1, 0, 0, x, ry - k * 0.0625f, z, color, opacity - k * 0.1f);
//        }

        if (!scan.structures().isEmpty()) {
            float cut = Mth.clamp(ry - 4.5f / 16f, 0, 2) * 16;
            float sh = 16 - cut;
            if (sh > 0) {
                drawQuad(consumer, poseStack, rotation, sprites.get(material), light, 1, sh, 6, 0, x, 1 + ((16f - sh) / 2 - 0.5f) / 16, z, GuiColors.scanner, anim, -0.002f, 1);
            }

//            drawQuad(consumer, poseStack, rotation, sprites.get(material), light, 1, 1, 5, 0, x, 6f / 16, z, GuiColors.scanner, anim, -0.0025f, 1);
            if (cut < 18) {
                drawQuad(consumer, poseStack, rotation, sprites.get(material), light, 1, 1, 5, 0, x, 25f / 16, z, 0, anim, -0.003f, 1.125f);
                drawQuad(consumer, poseStack, rotation, sprites.get(material), light, 1, 1, 5, 0, x, 25f / 16, z, GuiColors.scanner, anim, -0.002f, 1);
            }
        }
    }

    private float calculateMarkerAnimation(float time, long openTimestamp, long scanTimestamp) {
        if (time - scanTimestamp < 5) {
            return Lherper.easeOut(Mth.clampedMap(time - scanTimestamp, 0, 5, 0, 1));

        }

        return openTimestamp > 0
                ? Lherper.easeOut(Mth.clampedMap((time - openTimestamp), 0, 5, 0, 1))
                : Lherper.easeOut(Lherper.easeOut(Mth.clampedMap((time + openTimestamp - 20), 0, 5, 1, 0)));
    }

    private void drawQuad(VertexConsumer consumer, PoseStack poseStack, Quaternionf rotation, TextureAtlasSprite sprite, int light, float width, float height,
            int u, int v, float x, float y, float z, int color, float a) {
        drawQuad(consumer, poseStack, rotation, sprite, light, width, height, u, v, x, y, z, color, a, 0, 1);
    }

    private void drawQuad(VertexConsumer consumer, PoseStack poseStack, Quaternionf rotation, TextureAtlasSprite sprite, int light, float width, float height,
            int u, int v, float x, float y, float z, int color, float a, float zIndex, float scale) {

        float spriteWidth = sprite.contents().width();
        float spriteHeight = sprite.contents().height();
        float voxelSize = 1 / Math.max(spriteWidth, spriteHeight) * scale;

        float r = ARGB.red(color) / 255f;
        float g = ARGB.green(color) / 255f;
        float b = ARGB.blue(color) / 255f;

//        Quaternion quaternion = camera.rotation();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();

        Vector3f[] matrix = new Vector3f[] {
                new Vector3f(-0.5F * width, -0.5F * height, zIndex),
                new Vector3f(-0.5F * width, 0.5F * height, zIndex),
                new Vector3f(0.5F * width, 0.5F * height, zIndex),
                new Vector3f(0.5F * width, -0.5F * height, zIndex)
        };

        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = matrix[i];
            rotation.transform(vector3f);
            vector3f.mul(voxelSize);
//            vector3f.add(x - (float) cameraPos.x(), y - (float) cameraPos.y(), z - (float) cameraPos.z());
            vector3f.add(x, y, z);
        }

        consumer.addVertex(matrix4f, matrix[0].x(), matrix[0].y(), matrix[0].z()).setColor(r, g, b, a).setUv(u / spriteWidth, (v + height) / spriteHeight)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix4f, matrix[1].x(), matrix[1].y(), matrix[1].z()).setColor(r, g, b, a).setUv(u / spriteWidth, v / spriteHeight)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix4f, matrix[2].x(), matrix[2].y(), matrix[2].z()).setColor(r, g, b, a).setUv((u + width) / spriteWidth, v / spriteHeight)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix4f, matrix[3].x(), matrix[3].y(), matrix[3].z()).setColor(r, g, b, a).setUv((u + width) / spriteWidth, (v + height) / spriteHeight)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
    }

    private int getLightColor(Level level, BlockPos pos) {
        return 15728880;//level.hasChunkAt(pos) ? LevelRenderer.getLightColor(level, pos) : 0;
    }

//    for (int i = -5; i <= 5; i++) {
//            for (int j = -5; j <= 5; j++) {
//                int x = i + j;
//                int z = i - j;
//                int height = entity.getLevel().getChunk(x, z).getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
//                renderMarker(vertexBuilder, matrixStack, entity.getLevel(), dispatcher.camera, sprites.get(material), 0, light, 0.5f + x * 0.075f, 0.01f * height, 0.5f + z * 0.075f, 1, 1, 1, 0.9f);
//                for (int k = height / 10; k > 0; k--) {
//                    renderMarker(vertexBuilder, matrixStack, entity.getLevel(), dispatcher.camera, sprites.get(material), 0, light, 0.5f + x * 0.075f, 0.1f * k, 0.5f + z * 0.075f, 1, 1, 1, k * 0.05f);
//                }
//                renderMarker(vertexBuilder, matrixStack, entity.getLevel(), dispatcher.camera, sprites.get(material), 0, light, 0.5f + x * 0.075f, 0.5f, 0.5f + z * 0.075f, 1, 1, 1, 0.2f);
//            }
//        }

//        GlStateManager.lineWidth(3);

//        AABB aabb = new AABB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
//                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
//        AABB aabb = new AABB(0.5, 0.5, 0.5,
//                0.5, 0.5, 0.5);

    // draw center box
//        VertexConsumer vertexBuilder2 = buffer.getBuffer(RenderType.lines());
//        LevelRenderer.renderLineBox(matrixStack, vertexBuilder2, aabb.inflate(0.5030000000949949026D), 1, 0, 1, 1);
//        LevelRenderer.renderLineBox(p_112585_, vertexconsumer, d4, d5, d6, d7, d8, d9, 0.9F, 0.9F, 0.9F, 1.0F, 0.5F, 0.5F, 0.5F);
//        DebugRenderer.renderLineBox(aabb.grow(0.1), 1, 1, 1, 0.6f);
//
//        // draw outline
//        WorldRenderer.drawBoundingBox(matrixStack, vertexBuilder, aabb.grow(0.5030000000949949026D), 1, 0, 1, 1);
//
//        Arrays.stream(feature.children).forEach(featureChild -> renderChild(featureChild, matrixStack, buffer, (float) x, (float) y, (float) z));
//
//        Arrays.stream(feature.loot).forEach(featureLoot -> renderLoot(featureLoot, matrixStack, vertexBuilder, x, y, z));
//
//        GlStateManager.lineWidth(1.0F);
}
