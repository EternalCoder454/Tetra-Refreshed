package se.mickelus.tetra.blocks.salvage;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import se.mickelus.mutil.gui.GuiAttachment;
import se.mickelus.mutil.gui.GuiElement;
import se.mickelus.mutil.gui.GuiTexture;
import se.mickelus.mutil.gui.animation.Applier;
import se.mickelus.mutil.gui.animation.KeyframeAnimation;
import se.mickelus.tetra.TetraMod;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class InteractiveOutlineGui extends GuiElement {
    private static final Identifier texture = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "textures/gui/block-interaction.png");

    /**
     * How long the hints wait before fading in, staggered so the near corners lead.
     *
     * Upstream waits 500 and 650, which stops the hints flashing while the crosshair sweeps across
     * blocks. That reason is a good one and these keep it, but half a second reads as lag rather
     * than as restraint, so they are shorter. This is a deliberate divergence from upstream Tetra
     * and the only one in this file.
     *
     * InteractiveToolGui reads the second of these, because the tool icon arrives with the far
     * corners rather than on a delay of its own.
     */
    public static final int nearCornerDelay = 150;
    public static final int farCornerDelay = 200;

    private final BlockInteraction blockInteraction;

    private final GuiTexture topLeft;
    private final GuiTexture topRight;
    private final GuiTexture bottomLeft;
    private final GuiTexture bottomRight;

    private InteractiveToolGui tool;

    public InteractiveOutlineGui(BlockInteraction blockInteraction, Player player) {
        super((int) blockInteraction.minX * 4, (int) blockInteraction.minY * 4,
                (int) (blockInteraction.maxX - blockInteraction.minX) * 4,
                (int) (blockInteraction.maxY - blockInteraction.minY) * 4);

        this.blockInteraction = blockInteraction;

        opacity = 0.5f;

        topLeft = new GuiTexture(-2, -2, 4, 4, 0, 0, texture);
        addChild(topLeft);
        new KeyframeAnimation(100, topLeft)
                .applyTo(new Applier.Opacity(0, 1),
                        new Applier.TranslateX(0, -2),
                        new Applier.TranslateY(0, -2))
                .withDelay(nearCornerDelay)
                .start();

        topRight = new GuiTexture(2, -2, 4, 4, 3, 0, texture);
        topRight.setAttachment(GuiAttachment.topRight);
        addChild(topRight);
        new KeyframeAnimation(100, topRight)
                .applyTo(new Applier.Opacity(0, 1),
                        new Applier.TranslateX(0, 2),
                        new Applier.TranslateY(0, -2))
                .withDelay(farCornerDelay)
                .start();

        bottomLeft = new GuiTexture(-2, 2, 4, 4, 3, 0, texture);
        bottomLeft.setAttachment(GuiAttachment.bottomLeft);
        addChild(bottomLeft);
        new KeyframeAnimation(100, bottomLeft)
                .applyTo(new Applier.Opacity(0, 1),
                        new Applier.TranslateX(0, -2),
                        new Applier.TranslateY(0, 2))
                .withDelay(nearCornerDelay)
                .start();

        bottomRight = new GuiTexture(2, 2, 4, 4, 0, 0, texture);
        bottomRight.setAttachment(GuiAttachment.bottomRight);
        addChild(bottomRight);
        new KeyframeAnimation(100, bottomRight)
                .applyTo(new Applier.Opacity(0, 1),
                        new Applier.TranslateX(0, 2),
                        new Applier.TranslateY(0, 2))
                .withDelay(farCornerDelay)
                .onStop(complete -> {
                    if (tool != null) tool.updateFadeTime();
                })
                .start();

        if (blockInteraction.requiredTool != null) {
            tool = new InteractiveToolGui(0, 0, blockInteraction.requiredTool, blockInteraction.requiredLevel, player);
            addChild(tool);

            float centerY = y + height / 2f;
            float centerX = x + width / 2f;

            if (Math.abs(centerX - 16) > Math.abs(centerY - 16)) {
                if (centerX < 16) {
                    tool.setAttachmentPoint(GuiAttachment.middleLeft);
                    tool.setAttachmentAnchor(GuiAttachment.middleRight);
                    tool.setX(-1);
                } else {
                    tool.setAttachmentPoint(GuiAttachment.middleRight);
                    tool.setAttachmentAnchor(GuiAttachment.middleLeft);
                }
            } else {
                if (centerY < 16) {
                    tool.setAttachmentPoint(GuiAttachment.topCenter);
                    tool.setAttachmentAnchor(GuiAttachment.bottomCenter);
                    tool.setY(1);
                } else {
                    tool.setAttachmentPoint(GuiAttachment.bottomCenter);
                    tool.setAttachmentAnchor(GuiAttachment.topCenter);
                    tool.setY(-2);
                }
            }
        }
    }

    @Override
    protected void onFocus() {
        super.onFocus();

        if (tool != null) {
            tool.show();
        }
    }

    @Override
    protected void onBlur() {
        super.onBlur();

        if (tool != null) {
            tool.hide();
        }
    }

    public BlockInteraction getBlockInteraction() {
        return blockInteraction;
    }

    public void transitionOut(Runnable onStop) {
        new KeyframeAnimation(200, topLeft)
                .applyTo(new Applier.Opacity(1, 0),
                        new Applier.TranslateX(-5),
                        new Applier.TranslateY(-5))
                .start();

        new KeyframeAnimation(200, topRight)
                .applyTo(new Applier.Opacity(1, 0),
                        new Applier.TranslateX(5),
                        new Applier.TranslateY(-5))
                .start();

        new KeyframeAnimation(200, bottomLeft)
                .applyTo(new Applier.Opacity(1, 0),
                        new Applier.TranslateX(-5),
                        new Applier.TranslateY(5))
                .start();

        new KeyframeAnimation(200, bottomRight)
                .applyTo(new Applier.Opacity(1, 0),
                        new Applier.TranslateX(5),
                        new Applier.TranslateY(5))
                .onStop(finished -> onStop.run())
                .start();
    }
}
