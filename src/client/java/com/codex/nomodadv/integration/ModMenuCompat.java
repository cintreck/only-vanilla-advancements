package com.codex.onlyvanadv.integration;

import com.codex.onlyvanadv.integration.cloth.ClothConfigCompat;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (Screen parent) -> {
            if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
                return ClothConfigCompat.build(parent);
            }
            return parent; // fallback: no GUI if Cloth missing
        };
    }
}

