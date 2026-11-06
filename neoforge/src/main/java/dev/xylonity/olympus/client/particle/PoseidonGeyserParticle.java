package dev.xylonity.olympus.client.particle;

import dev.xylonity.olympus.registry.OlympusParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.GeyserBaseParticle;
import net.minecraft.client.particle.GeyserPlumeParticle;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.GeyserBaseParticleOptions;
import net.minecraft.core.particles.GeyserParticleOptions;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public final class PoseidonGeyserParticle extends NoRenderParticle {
    private final double xa;
    private final double ya;
    private final double za;
    private final int waterBlocks;

    private PoseidonGeyserParticle(final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final GeyserParticleOptions options) {
        super(level, x, y, z);
        this.xa = xa;
        this.ya = ya;
        this.za = za;
        this.waterBlocks = options.waterBlocks();
        this.lifetime = 20;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age % 2 == 0) {
            for (int i = 0; i < 2; i++) {
                this.level.addParticle(new GeyserBaseParticleOptions(OlympusParticles.POSEIDON_GEYSER_BASE.get(), this.waterBlocks, 1.5F), this.x, this.y, this.z, this.xa, this.ya, this.za);
            }
        }

        final GeyserParticleOptions plume = new GeyserParticleOptions(OlympusParticles.POSEIDON_GEYSER_PLUME.get(), this.waterBlocks);
        for (int i = 0; i < this.waterBlocks + 2; i++) {
            this.level.addParticle(plume, this.x, this.y, this.z, this.xa, this.ya, this.za);
        }

        if (this.age % 10 == 0) {
            final GeyserBaseParticleOptions poof = new GeyserBaseParticleOptions(OlympusParticles.POSEIDON_GEYSER_POOF.get(), this.waterBlocks, 2.0F);
            for (int i = 0; i < 20; i++) {
                this.level.addParticle(poof, this.x, this.y, this.z, this.xa, this.ya, this.za);
            }
        }
    }

    public static final class EmitterProvider implements ParticleProvider<GeyserParticleOptions> {
        @Override
        public @Nullable Particle createParticle(final GeyserParticleOptions options, final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final RandomSource random) {
            return new PoseidonGeyserParticle(level, x, y, z, xa, ya, za, options);
        }
    }

    public static final class PlumeProvider implements ParticleProvider<GeyserParticleOptions> {
        private final SpriteSet sprites;

        public PlumeProvider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(final GeyserParticleOptions options, final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final RandomSource random) {
            final GeyserPlumeParticle particle = new GeyserPlumeParticle(level, x, y, z, xa, ya, za, options, this.sprites);
            particle.setColor(0.24F, 0.66F, 1.0F);
            return particle;
        }
    }

    public static final class BaseProvider implements ParticleProvider<GeyserBaseParticleOptions> {
        private final SpriteSet sprites;

        public BaseProvider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(final GeyserBaseParticleOptions options, final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final RandomSource random) {
            final GeyserBaseParticle particle = new GeyserBaseParticle(level, x, y, z, xa, ya, za, options.waterBlocks(), options.burstImpulseBase(), this.sprites);
            particle.setColor(0.24F, 0.66F, 1.0F);
            return particle;
        }
    }

}
