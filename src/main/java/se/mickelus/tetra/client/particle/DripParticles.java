package se.mickelus.tetra.client.particle;

import net.minecraft.util.RandomSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class DripParticles {
    public static Supplier<SimpleParticleType> fallingBlood;
    public static Supplier<SimpleParticleType> landingBlood;
    public static Supplier<SimpleParticleType> fallingSlime;
    public static Supplier<SimpleParticleType> landingSlime;

    public static class FallingBloodProvider implements ParticleProvider<SimpleParticleType> {
        SpriteSet sprites;

        public FallingBloodProvider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(SimpleParticleType option, ClientLevel level, double x, double y, double z, double dx, double dy,
                double dz, RandomSource random) {
            FallAndLandParticle particle = new FallAndLandParticle(level, x, y, z, Fluids.EMPTY, landingBlood.get(), this.sprites.get(random));
            particle.setParticleSpeed(dx, dy, dz);
            particle.setColor(0.72f, 0.14f, 0.14f);
            return particle;
        }
    }

    public static class LandingBloodProvider implements ParticleProvider<SimpleParticleType> {
        SpriteSet sprites;

        public LandingBloodProvider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(SimpleParticleType option, ClientLevel level, double x, double y, double z, double dx, double dy,
                double dz, RandomSource random) {
            DripLandParticle particle = new DripLandParticle(level, x, y, z, Fluids.EMPTY, this.sprites.get(random));
            particle.setColor(0.72f, 0.14f, 0.14f);
            return particle;
        }
    }

    public static class FallingSlimeProvider implements ParticleProvider<SimpleParticleType> {
        SpriteSet sprites;

        public FallingSlimeProvider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(SimpleParticleType option, ClientLevel level, double x, double y, double z, double dx, double dy,
                double dz, RandomSource random) {
            FallAndLandParticle particle = new FallAndLandParticle(level, x, y, z, Fluids.EMPTY, landingSlime.get(), this.sprites.get(random));
            particle.setParticleSpeed(dx, dy, dz);
            particle.setColor(0.42f, 0.65f, 0.31f);
            return particle;
        }
    }

    public static class LandingSlimeProvider implements ParticleProvider<SimpleParticleType> {
        SpriteSet sprites;

        public LandingSlimeProvider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(SimpleParticleType option, ClientLevel level, double x, double y, double z, double dx, double dy,
                double dz, RandomSource random) {
            DripLandParticle particle = new DripLandParticle(level, x, y, z, Fluids.EMPTY, this.sprites.get(random));
            particle.setColor(0.42f, 0.65f, 0.31f);
            return particle;
        }
    }

    // The drip particles take a sprite chosen up front rather than animating one from the age, which
    // is how vanilla builds its own now, so these subclasses only widen visibility.
    static class FallAndLandParticle extends DripParticle.FallAndLandParticle {
        public FallAndLandParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType, ParticleOptions pLandParticle,
                TextureAtlasSprite sprite) {
            super(pLevel, pX, pY, pZ, pType, pLandParticle, sprite);
        }
    }

    static class DripLandParticle extends DripParticle.DripLandParticle {
        public DripLandParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType, TextureAtlasSprite sprite) {
            super(pLevel, pX, pY, pZ, pType, sprite);
        }
    }
}
