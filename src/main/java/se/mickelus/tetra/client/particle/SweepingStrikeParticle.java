package se.mickelus.tetra.client.particle;

import net.minecraft.util.RandomSource;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

public class SweepingStrikeParticle extends SingleQuadParticle {
    private static final Vector3f ROTATION_VECTOR = new Vector3f(0.5F, 0.5F, 0.5F).normalize();
    private final boolean reverse;
    private final SpriteSet sprites;

    private final float pitch;
    private final float yaw;

    protected SweepingStrikeParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet, int lifetime, boolean reverse, float pitch, float yaw) {
        super(level, x, y, z, 0, 0, 0, spriteSet.first());

        float shade = 0.6f + random.nextFloat() * 0.4f;
        this.rCol = shade;
        this.gCol = shade;
        this.bCol = shade;
        this.quadSize = 1.0F;

        this.pitch = pitch / 180F * Mth.PI;
        this.yaw = yaw / 180F * Mth.PI;

        this.sprites = spriteSet;
        this.lifetime = lifetime;
        this.reverse = reverse;


        this.setSpriteFromAge(spriteSet);
    }

    @Override
    protected Layer getLayer() {
        // the old anonymous render type disabled blending and wrote depth against the particle
        // atlas, which is what Layer.OPAQUE is. Culling stayed on, as it did before.
        return Layer.OPAQUE;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTicks) {
        Quaternionf rotation = new Quaternionf().setAngleAxis(0.0F, ROTATION_VECTOR.x(), ROTATION_VECTOR.y(), ROTATION_VECTOR.z());
        rotation.mul(Axis.YP.rotation(-yaw));
        rotation.mul(Axis.XP.rotation(pitch + Mth.PI / 3f));

        extractRotatedQuad(renderState, camera, rotation, partialTicks);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    protected float getU0() {
        return reverse ? super.getU1() : super.getU0();
    }

    @Override
    protected float getU1() {
        return reverse ? super.getU0() : super.getU1();
    }

    @OnlyIn(Dist.CLIENT)
    @ParametersAreNonnullByDefault
    public static class Provider implements ParticleProvider<SweepingStrikeParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(SweepingStrikeParticleOption option, ClientLevel level, double x, double y, double z, double dx, double dy, double dz, RandomSource random) {
            return new SweepingStrikeParticle(level, x, y, z, this.sprites, option.duration(), option.reverse(), option.pitch(), option.yaw());
        }
    }
}
