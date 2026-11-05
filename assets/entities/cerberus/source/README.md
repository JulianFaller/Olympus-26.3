# Cerberus entity assets

Keep Cerberus' exported assets together in this folder:

- `cerberus.geo.json`: GeckoLib geometry and bones.
- `cerberus.animation.json`: GeckoLib animations; current exports define `walk` and `idle`.
- `cerberus.png`: the model texture.
- Optional `.bbmodel` files: editable Blockbench sources, not loaded directly by Minecraft.

Gradle's `stageEntityAssets` task copies the three runtime files from this folder to the required GeckoLib model, animation, and texture resource paths before resource processing and packaging. Do not edit the generated files under `build/` or create duplicate runtime copies under `src/main/resources/`.
