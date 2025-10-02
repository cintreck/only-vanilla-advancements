package com.codex.onlyvanadv.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads and saves only_vanilla_advancements.toml with helpful comments.
 */
public final class OvaConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("only_vanilla_advancements-config");

    public final Set<String> kept_mods = new HashSet<>();
    public final Set<String> kept_advancements = new HashSet<>();
    public final Set<String> removed_mods = new HashSet<>();
    public final Set<String> removed_advancements = new HashSet<>();
    public boolean keep_parent_advancements = true;

    public static Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
    public static Path tomlPath() { return configDir().resolve("only_vanilla_advancements.toml"); }

    public static OvaConfig loadOrCreate() {
        Path path = tomlPath();
        if (!Files.exists(path)) {
            OvaConfig cfg = new OvaConfig();
            cfg.saveWithComments();
            return cfg;
        }
        try (CommentedFileConfig c = CommentedFileConfig.builder(path)
                .preserveInsertionOrder()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            c.load();
            return fromToml(c);
        } catch (Throwable t) {
            LOGGER.warn("Failed to load {}: {}. Using defaults.", path.getFileName(), t.toString());
            OvaConfig cfg = new OvaConfig();
            cfg.saveWithComments();
            return cfg;
        }
    }

    private static OvaConfig fromToml(CommentedConfig c) {
        OvaConfig cfg = new OvaConfig();
        cfg.kept_mods.addAll(readStrList(c, "kept_mods"));
        cfg.kept_advancements.addAll(readStrList(c, "kept_advancements"));
        cfg.removed_mods.addAll(readStrList(c, "removed_mods"));
        cfg.removed_advancements.addAll(readStrList(c, "removed_advancements"));
        Object b = c.get("keep_parent_advancements");
        cfg.keep_parent_advancements = b instanceof Boolean ? (Boolean) b : true;
        return cfg;
    }

    public void saveWithComments() {
        Path path = tomlPath();
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException ignored) {}

        try (CommentedFileConfig c = CommentedFileConfig.builder(path)
                .preserveInsertionOrder()
                .writingMode(WritingMode.REPLACE)
                .build()) {
            c.load();

            c.set("#", "Only Vanilla Advancements - Config");
            c.set("#1", "Vanilla (minecraft:*) is always kept. This file lets you keep/remove non-vanilla entries.");

            // No strict toggle: logic always preserves true vanilla and explicit keeps, and removes others.

            c.set("kept_mods", new ArrayList<>(kept_mods));
            c.setComment("kept_mods", "Keep ALL advancements from these mods/datapacks (their short id, e.g. myquests)");

            c.set("kept_advancements", new ArrayList<>(kept_advancements));
            c.setComment("kept_advancements", "Keep these specific advancement IDs (e.g. myquests:story/root)");

            c.set("removed_mods", new ArrayList<>(removed_mods));
            c.setComment("removed_mods", "Always remove ALL advancements from these mods/datapacks");

            c.set("removed_advancements", new ArrayList<>(removed_advancements));
            c.setComment("removed_advancements", "Always remove these specific advancement IDs");

            c.set("keep_parent_advancements", keep_parent_advancements);
            c.setComment("keep_parent_advancements", "If true, keeping an advancement also keeps its required parents so the UI looks correct (recommended)");

            c.save();
        }
    }

    private static List<String> readStrList(CommentedConfig c, String key) {
        Object v = c.get(key);
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) if (o != null) out.add(o.toString());
            return out;
        }
        return new ArrayList<>();
    }

    
}
