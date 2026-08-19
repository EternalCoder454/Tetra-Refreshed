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
     * Track what is being looked at, then hand NeoForge something that draws it on the face.
     *
     * The draw used to happen here, because RenderHighlightEvent.Block carried the world pose stack
     * and buffer source. Its replacement is an extract phase event and carries neither, which is
     * why this stopped drawing during the port. addCustomRenderer is the way back: NeoForge calls
     * the renderer during the draw phase and passes exactly the two things that went missing.
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

        // A block with no shape has no face to draw on, and asking for its bounds would give the
        // empty box rather than nothing.
        if (shape.isEmpty()) {
            return;
        }

        if (isDirty || !blockState.equals(previousState) || !blockPos.equals(previousPos) || !face.equals(previousFace)) {
            gui.update(world, blockPos, blockState, face, mc.player,
                    blockPos.equals(previousPos) && face.equals(previousFace));

            previousPos = blockPos;
            previousFace = face;
            previousState = blockState;

            isDirty = false;
        }

        // Registered every frame the outline is drawn, which is how the event is meant to be used.
        // The state above is what decides the contents, this only draws whatever that decided.
        event.addCustomRenderer((outlineState, bufferSource, pose, translucent, levelState) -> {
            gui.drawWorld(pose, bufferSource, event.getCamera().position(), rayTrace, shape);
            // false, because the vanilla outline should still draw underneath. Returning true would
            // replace it, and the hints are meant to sit on top of the outline rather than instead
            // of it.
            return false;
        });
    }
}
