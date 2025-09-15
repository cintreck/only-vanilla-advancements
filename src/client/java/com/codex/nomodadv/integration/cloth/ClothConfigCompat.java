package com.codex.onlyvanadv.integration.cloth;

import com.codex.onlyvanadv.config.OvaConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ClothConfigCompat {
    private ClothConfigCompat() {}

    public static Screen build(Screen parent) {
        OvaConfig cfg = OvaConfig.loadOrCreate();

        ConfigBuilder b = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Only Vanilla Advancements"));
        b.setSavingRunnable(cfg::saveWithComments);

        ConfigCategory cat = b.getOrCreateCategory(Text.literal("General"));
        ConfigEntryBuilder eb = b.entryBuilder();


        cat.addEntry(eb.startStrList(Text.literal("Kept Mods"), new ArrayList<>(cfg.kept_mods))
                .setTooltip(Text.literal("Keep ALL advancements from these mods/datapacks (their short id)"))
                .setDefaultValue(List.of())
                .setSaveConsumer(list -> { cfg.kept_mods.clear(); cfg.kept_mods.addAll(list); })
                .build());

        cat.addEntry(eb.startStrList(Text.literal("Kept Advancements"), new ArrayList<>(cfg.kept_advancements))
                .setTooltip(Text.literal("Keep these specific advancement IDs (e.g. mymod:story/root)"))
                .setDefaultValue(List.of())
                .setSaveConsumer(list -> { cfg.kept_advancements.clear(); cfg.kept_advancements.addAll(list); })
                .build());

        cat.addEntry(eb.startStrList(Text.literal("Removed Mods"), new ArrayList<>(cfg.removed_mods))
                .setTooltip(Text.literal("Always remove ALL advancements from these mods/datapacks"))
                .setDefaultValue(List.of())
                .setSaveConsumer(list -> { cfg.removed_mods.clear(); cfg.removed_mods.addAll(list); })
                .build());

        cat.addEntry(eb.startStrList(Text.literal("Removed Advancements"), new ArrayList<>(cfg.removed_advancements))
                .setTooltip(Text.literal("Always remove these advancement IDs (e.g. pack:progress/special)"))
                .setDefaultValue(List.of())
                .setSaveConsumer(list -> { cfg.removed_advancements.clear(); cfg.removed_advancements.addAll(list); })
                .build());

        cat.addEntry(eb.startBooleanToggle(Text.literal("Keep Parent Advancements"), cfg.keep_parent_advancements)
                .setDefaultValue(true)
                .setTooltip(Text.literal("If enabled, also keep the required parent advancements for any kept entry"))
                .setSaveConsumer(v -> cfg.keep_parent_advancements = v)
                .build());

        // Strict behavior is always on internally: true vanilla is preserved, overrides removed unless kept.

        return b.build();
    }
}
