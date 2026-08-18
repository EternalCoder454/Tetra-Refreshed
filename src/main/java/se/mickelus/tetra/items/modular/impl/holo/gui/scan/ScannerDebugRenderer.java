package se.mickelus.tetra.items.modular.impl.holo.gui.scan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.shapes.Shapes;

@ParametersAreNonnullByDefault
public class ScannerDebugRenderer {
    private final ScannerOverlayGui overlayGui;

    public ScannerDebugRenderer(ScannerOverlayGui overlayGui) {
        this.overlayGui = overlayGui;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderWorld(RenderLevelStageEvent event) {
        Player player = Minecraft.getInstance().player;

        if (player != null && player.isCreative()) {
            PoseStack matrixStack = event.getPoseStack();
            VertexConsumer vertexBuilder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderTypes.lines());
            // The camera position lives on the level render state now rather than being derived
            // from a partial tick at draw time.
            Vec3 eyePos = event.getLevelRenderState().cameraRenderState.pos;

            if (overlayGui.upHighlight != null) drawDebugBox(overlayGui.upHighlight, eyePos, matrixStack, vertexBuilder, 1, 0, 0, 0.5f);
            if (overlayGui.midHighlight != null) drawDebugBox(overlayGui.midHighlight, eyePos, matrixStack, vertexBuilder, 0, 1, 0, 0.5f);
            if (overlayGui.downHighlight != null) drawDebugBox(overlayGui.downHighlight, eyePos, matrixStack, vertexBuilder, 0, 0, 1, 0.5f);
        }
    }

    private void drawDebugBox(BlockPos blockPos, Vec3 eyePos, PoseStack matrixStack, VertexConsumer vertexBuilder, float red, float green, float blue, float alpha) {
        Vec3 pos = /*new Vec3(1023, 7, 1159).subtract(eyePos)*/Vec3.atLowerCornerOf(blockPos).subtract(eyePos);
        AABB aabb = new AABB(pos, pos.add(1, 1, 1));

        // LevelRenderer#renderLineBox is gone; ShapeRenderer draws the edges of a shape, and the
        // colour is packed rather than passed as four channels.
        ShapeRenderer.renderShape(matrixStack, vertexBuilder, Shapes.create(aabb.inflate(0.0030000000949949026D)),
                0, 0, 0, ARGB.colorFromFloat(alpha, red, green, blue), 1.0f);
    }
}
