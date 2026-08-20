package se.mickelus.tetra.effect.data.outcome;

import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import se.mickelus.tetra.effect.data.ItemEffectContext;
import se.mickelus.tetra.effect.data.provider.entity.EntityProvider;
import se.mickelus.tetra.effect.data.provider.vector.VectorProvider;

public class RunCommandItemEffectOutcome extends ItemEffectOutcome {
    String command;
    EntityProvider entity;
    VectorProvider position;

    @Override
    public boolean perform(ItemEffectContext context) {
        if (context.getLevel() instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();

            CommandSourceStack commandSourceStack = server.createCommandSourceStack()
                    .withLevel(serverLevel)
                    .withPermission(LevelBasedPermissionSet.GAMEMASTER)
                    .withSuppressedOutput();

            if (position != null) {
                commandSourceStack = commandSourceStack.withPosition(position.getVector(context));
            }

            if (entity != null && entity.getEntity(context) != null) {
                commandSourceStack = commandSourceStack.withEntity(entity.getEntity(context));
            }

            // Report whether the command actually did anything, rather than that it was run.
            //
            // Upstream returns `result > 0` from performPrefixedCommand. That method returns void
            // in 26.1.2, so the result arrives through a callback on the source stack instead. It
            // matters because this outcome nests: loop, multiple, find_blocks, find_entities and
            // conditioned all branch on what perform returns, and a command that failed reporting
            // success makes a loop keep going and a conditional take the wrong arm.
            boolean[] succeeded = { false };
            commandSourceStack = commandSourceStack.withCallback(
                    (success, result) -> succeeded[0] = success && result > 0);

            server.getCommands().performPrefixedCommand(commandSourceStack, this.command);
            return succeeded[0];
        }
        return false;
    }
}
