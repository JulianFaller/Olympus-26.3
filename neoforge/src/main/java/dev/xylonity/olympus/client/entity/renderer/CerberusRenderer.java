package dev.xylonity.olympus.client.entity.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import dev.xylonity.olympus.registry.OlympusEntities;
import dev.xylonity.olympus.common.entity.CerberusEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class CerberusRenderer extends GeoEntityRenderer<CerberusEntity, EntityRenderState> {
    public CerberusRenderer(EntityRendererProvider.Context context) {
        super(context, OlympusEntities.CERBERUS.get());
    }
}
