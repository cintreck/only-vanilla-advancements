# Only Vanilla Advancements (Fabric)

Keeps the advancements screen strictly vanilla by removing every non‑Minecraft advancement (mods and datapacks) unless you explicitly keep them. Preserves the vanilla tab layout and avoids orphaned/stacked entries.

### Without the mod
![Without Only Vanilla Advancements](https://cdn.modrinth.com/data/cached_images/6d42aa1212d0f3a8819ba78870a043ce0cb070de.png)

### With the mod
![With Only Vanilla Advancements](https://cdn.modrinth.com/data/cached_images/b950039e6220302c08de1b2bdf23de36e2d4e475.png)

### With the mod + 'biomesoplenty' kept using the config
![With 'biomesoplenty' Kept](https://cdn.modrinth.com/data/cached_images/c7ecd782e4a30f2b2b9a14a858ff6ac23d2accbd.png)

What it does
- Filters out all non‑`minecraft:*` advancements on server start and after successful data pack reload.
- Optionally keeps specific mods or specific advancement IDs via a simple TOML config.
- Optionally keeps parent advancements for any kept entries so the UI remains intact.
- Zero tick cost; runs only at load/reload.

Install
- Requires Fabric Loader and Fabric API.
- Drop the mod jar into `mods/` and launch.

Configure
- File: `config/only_vanilla_advancements.toml` (created on first run).
- Keys: `kept_mods`, `kept_advancements`, `removed_mods`, `removed_advancements`, `keep_parent_advancements`.
- Optional UI: If you also install Mod Menu and Cloth Config, you get an in‑game config screen.

Compatibility
- Server‑side logic; works in singleplayer (integrated server) and dedicated servers.
- Compatible with Mod Menu and Cloth Config if present; not required.

Build
- Java 21, Gradle + Fabric Loom.
- See `gradle.properties` for versions. Run `./gradlew build`.

License
- CC0‑1.0. Use it freely.
