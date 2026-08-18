package se.mickelus.tetra.blocks.salvage;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class InteractiveBlockOverlay {

    private static boolean isDirty = false;
    private final Minecraft mc;
    private final InteractiveBlockOverlayGui gui;
    private BlockPos previousPos;
    private Direction previousFace;
    private BlockState previousState;

    public InteractiveBlockOverlay() {
        gui = new InteractiveBlockOverlayGui();

        mc = Minecraft.getInstance();
    }

    public static void markDirty() {
        isDirty = true;
    }

    /**
     * The overlay's state still tracks what is being looked at, but it does not draw.
     *
     * RenderHighlightEvent.Block became ExtractBlockOutlineRenderStateEvent, which is an extract
     * phase event, and the draw it used to do is gone with it. A GuiGraphicsExtractor is built over
     * a GuiRenderState now, which the gui renderer later draws in screen space, so there is no
     * longer a constructor that takes a world pose stack and buffer source. Drawing mutil's gui
     * element tree onto a block face again means giving those elements a world space draw path,
     * which is a redesign of mutil's gui layer rather than a signature change, so it is left out
     * of the port. The interaction hints on block faces do not render.
     */
    @SubscribeEvent
    public void renderOverlay(ExtractBlockOutlineRenderStateEvent event) {
        BlockHitResult rayTrace = event.getHitResult();
        if (!rayTrace.getType().equals(HitResult.Type.BLOCK)) {
            return;
        }

        Level world = event.getLevel();
        BlockPos blockPos = event.getBlockPos();
        Direction face = rayTrace.getDirection();
        BlockState blockState = event.getBlockState();
        VoxelShape shape = blockState.getShape(world, blockPos, event.getCollisionContext());

        if (!shape.isEmpty()
                && (isDirty || !blockState.equals(previousState) || !blockPos.equals(previousPos) || !face.equals(previousFace))) {
            gui.update(world, blockPos, blockState, face, mc.player,
                    blockPos.equals(previousPos) && face.equals(previousFace));

            previousPos = blockPos;
            previousFace = face;
            previousState = blockState;

            isDirty = false;
        }
    }
}
