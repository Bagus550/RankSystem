package id.bagus.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class Messages {

    /** File bahasa yang ikut di dalam jar. */
    private static final List<String> BUNDLED = List.of("en", "id");

    private final RankPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private YamlConfiguration active;
    private YamlConfiguration fallback;

    public Messages(RankPlugin plugin) {
        this.plugin = plugin;
    }

    /** Dipanggil saat plugin menyala dan setiap /rank reload. */
    public void load() {
        // Salin file bawaan ke plugins/RankSystem/lang/ kalau belum ada
        for (String code : BUNDLED) {
            File f = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
            if (!f.exists()) plugin.saveResource("lang/" + code + ".yml", false);
        }
        fallback = readBundled("en");

        String code = plugin.getConfig().getString("language", "en").toLowerCase(Locale.ROOT);
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".yml");
        if (file.exists()) {
            active = YamlConfiguration.loadConfiguration(file);
        } else {
            plugin.getLogger().warning("Language file lang/" + code + ".yml not found, using English.");
            active = fallback;
        }
    }

    private YamlConfiguration readBundled(String code) {
        try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
            if (in != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {}
        return new YamlConfiguration();
    }

    /** Teks mentah. Kalau kunci tidak ada di bahasa aktif, pakai bahasa Inggris. */
    public String raw(String key) {
        String value = active.getString(key);
        if (value == null) value = fallback.getString(key);
        return value != null ? value : "<red>[missing message: " + key + "]";
    }

    public Component get(String key, TagResolver... resolvers) {
        return mm.deserialize(raw(key), resolvers);
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(get(key, resolvers));
    }
}