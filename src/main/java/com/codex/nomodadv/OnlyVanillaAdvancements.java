package com.codex.onlyvanadv;

import com.codex.onlyvanadv.config.OvaConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Conservative filter that preserves vanilla advancements and UI layout.
 * - Never removes vanilla roots/chains: keeps all entries in the "minecraft" namespace
 *   whose parent chain also stays within the kept set.
 * - Removes all non-minecraft advancements (mods/datapacks) and any minecraft entries
 *   that depend on removed parents (prevents UI stacking/orphans).
 * - Runs only at server start and after successful data pack reload. No per-tick logic.
 */
public class OnlyVanillaAdvancements implements ModInitializer {
    public static final String MOD_ID = "only_vanilla_advancements";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::filterAdvancements);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, rm, success) -> {
            if (success) filterAdvancements(server);
        });
    }

    private void filterAdvancements(MinecraftServer server) {
        try {
            ServerAdvancementLoader loader = server.getAdvancementLoader();
            var manager = loader.getManager();
            // Load or create TOML config with comments
            OvaConfig cfg = OvaConfig.loadOrCreate();

            // Snapshot all nodes and ids
            Map<Identifier, PlacedAdvancement> nodes = new HashMap<>();
            Set<Identifier> allIds = new HashSet<>();
            for (PlacedAdvancement pa : manager.getAdvancements()) {
                Identifier id = pa.getAdvancementEntry().id();
                nodes.put(id, pa);
                allIds.add(id);
            }

            // Build keep/remove sets from config
            Set<String> keepMods = new HashSet<>(cfg.kept_mods);
            Set<String> removeMods = new HashSet<>(cfg.removed_mods);
            Set<Identifier> keepIds = parseIdSet(cfg.kept_advancements);
            Set<Identifier> removeIds = parseIdSet(cfg.removed_advancements);

            // Expand kept ids upward to include parents if requested
            if (cfg.keep_parent_advancements) {
                Deque<Identifier> stack = new ArrayDeque<>(keepIds);
                while (!stack.isEmpty()) {
                    Identifier id = stack.pop();
                    PlacedAdvancement pa = nodes.get(id);
                    if (pa == null) continue;
                    PlacedAdvancement parent = pa.getParent();
                    if (parent == null) continue;
                    Identifier pid = parent.getAdvancementEntry().id();
                    if (!keepIds.contains(pid)) { keepIds.add(pid); stack.push(pid); }
                }
            }

            ResourceManager rm = server.getResourceManager();
            // Origin-aware removal set
            Set<Identifier> toRemove = new HashSet<>();
            for (Identifier id : allIds) {
                boolean isKept = keepIds.contains(id) || keepMods.contains(id.getNamespace());
                boolean isMinecraft = "minecraft".equals(id.getNamespace());
                if (isKept) continue; // never remove kept

                if (!isMinecraft) {
                    // Non-vanilla default: remove unless explicitly kept
                    toRemove.add(id);
                    continue;
                }

                // minecraft:* -> check if effective resource comes from built-in "vanilla" pack
                boolean isTrueVanilla = isEffectiveVanilla(rm, id);

                if (!isTrueVanilla) {
                    // This is an override from a datapack/mod; remove unless explicitly kept
                    if (!(keepIds.contains(id) || keepMods.contains("minecraft"))) {
                        toRemove.add(id);
                    }
                }
            }

            // Expand kept ids with their parent chain if configured
            // Prune orphans: remove anything whose parent is not present after rules
            boolean changed;
            do {
                changed = false;
                for (Identifier id : new HashSet<>(allIds)) {
                    if (toRemove.contains(id)) continue;
                    // never prune vanilla or explicitly kept entries, even if orphaned
                    if ("minecraft".equals(id.getNamespace()) || keepMods.contains(id.getNamespace()) || keepIds.contains(id)) {
                        continue;
                    }
                    PlacedAdvancement pa = nodes.get(id);
                    if (pa == null) continue;
                    PlacedAdvancement parent = pa.getParent();
                    if (parent == null) continue; // root ok
                    Identifier pid = parent.getAdvancementEntry().id();
                    if (toRemove.contains(pid)) {
                        toRemove.add(id);
                        changed = true;
                    }
                }
            } while (changed);

            if (!toRemove.isEmpty()) {
                manager.removeAll(toRemove);
                server.getPlayerManager().getPlayerList().forEach(p -> p.getAdvancementTracker().reload(loader));
                LOGGER.info("OnlyVanillaAdvancements: removed {} advancements (after rules).", toRemove.size());
            } else {
                LOGGER.info("OnlyVanillaAdvancements: no removals needed.");
            }
        } catch (Throwable t) {
            LOGGER.error("OnlyVanillaAdvancements: filtering failed", t);
        }
    }

    private String safePackId(Resource res) {
        try { return res.getPackId(); } catch (Throwable t) { return ""; }
    }

    private boolean isEffectiveVanilla(ResourceManager rm, Identifier advId) {
        try {
            String resPath = "advancements/" + advId.getPath() + ".json";
            Identifier resId = Identifier.of("minecraft", resPath);
            java.util.Optional<Resource> opt = rm.getResource(resId);
            if (opt.isEmpty()) {
                // If we can’t resolve, fail-open and keep (avoid nuking vanilla)
                return true;
            }
            String pid = safePackId(opt.get());
            return isBuiltinPack(pid);
        } catch (Throwable t) {
            // Fail-open as vanilla to avoid accidental removal
            return true;
        }
    }

    private boolean isBuiltinPack(String pid) {
        String p = pid == null ? "" : pid.toLowerCase();
        // Accept common built-in identifiers defensively across launcher variants
        return p.equals("vanilla") || p.equals("minecraft") || p.contains("builtin") || p.contains("default");
    }

    private Set<Identifier> parseIdSet(Collection<String> raw) {
        Set<Identifier> out = new HashSet<>();
        if (raw == null) return out;
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            Identifier id = Identifier.tryParse(s.trim());
            if (id != null) out.add(id);
        }
        return out;
    }
}
