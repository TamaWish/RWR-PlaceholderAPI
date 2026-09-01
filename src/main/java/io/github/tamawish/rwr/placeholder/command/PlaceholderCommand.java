package io.github.tamawish.rwr.placeholder.command;

import io.github.tamawish.rwr.placeholder.RwrPlaceholderPlugin;
import io.github.tamawish.rwr.placeholder.locale.LocaleService;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/** Handles the {@code /rwr placeholder} administration namespace. */
public final class PlaceholderCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "rwrplaceholder.admin";

    private final RwrPlaceholderPlugin plugin;

    public PlaceholderCommand(RwrPlaceholderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LocaleService locale = plugin.locale();
        if (locale == null) {
            sender.sendMessage("RWR-PlaceholderAPI is not ready.");
            return true;
        }
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(locale.format("command.no-permission", "permission", PERMISSION));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(locale.raw("command.unknown"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> handleReload(sender, locale);
            case "status" -> handleStatus(sender, locale);
            default -> sender.sendMessage(locale.raw("command.unknown"));
        }
        return true;
    }

    private void handleReload(CommandSender sender, LocaleService locale) {
        Optional<String> error = plugin.reloadServices();
        if (error.isPresent()) {
            sender.sendMessage(locale.format("command.reload-failed", "reason", error.get()));
        } else {
            sender.sendMessage(locale.raw("command.reload-success"));
        }
    }

    private void handleStatus(CommandSender sender, LocaleService locale) {
        plugin.refreshApiAvailability();
        plugin.refreshPlaceholderApi();
        sender.sendMessage(locale.raw("command.status-header"));
        sender.sendMessage(locale.format(
                "command.status-service", "state", plugin.isApiAvailable() ? "available" : "unavailable"));
        sender.sendMessage(locale.format(
                "command.status-papi", "state", plugin.isPlaceholderApiAvailable() ? "available" : "unavailable"));
        sender.sendMessage(locale.format(
                "command.status-expansion", "state", plugin.isExpansionRegistered() ? "registered" : "unregistered"));
        int worlds = plugin.cache() == null ? 0 : plugin.cache().knownWorldIds().size();
        sender.sendMessage(locale.format("command.status-worlds", "count", String.valueOf(worlds)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("reload", "status").stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
