package bonn2.movingendcities.inventories;

import bonn2.movingendcities.utils.EndCityManager;
import bonn2.movingendcities.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Objects;

import static org.bukkit.Bukkit.getServer;

public class ConfirmDeleteInventory implements Listener {

    private final Inventory inventory;
    private final NamespacedKey namespace;
    private final Player player;
    private final String cityName;

    public ConfirmDeleteInventory(Player player, ItemStack item) {
        Main plugin = Main.plugin;
        inventory = Bukkit.createInventory(null, 9, ChatColor.RED + "Are you sure?");
        namespace = new NamespacedKey(plugin, "Action");
        this.player = player;
        cityName = ChatColor.stripColor(Objects.requireNonNull(item.getItemMeta()).getDisplayName());

        ItemStack no = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta noMeta = no.getItemMeta();
        assert noMeta != null;
        noMeta.setDisplayName(ChatColor.GREEN + "No");
        noMeta.getPersistentDataContainer().set(namespace, PersistentDataType.STRING, "Cancel");
        no.setItemMeta(noMeta);

        ItemStack yes = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta yesMeta = yes.getItemMeta();
        assert yesMeta != null;
        yesMeta.setDisplayName(ChatColor.RED + "Yes");
        yesMeta.getPersistentDataContainer().set(namespace, PersistentDataType.STRING, "Confirm");
        yes.setItemMeta(yesMeta);

        inventory.setItem(0, no);
        inventory.setItem(1, no);
        inventory.setItem(2, no);
        inventory.setItem(3, no);
        inventory.setItem(4, no);
        inventory.setItem(5, no);
        inventory.setItem(6, no);
        inventory.setItem(7, yes);
        inventory.setItem(8, no);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void close() {
        player.closeInventory();
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(Main.plugin, () -> {
            ManageCitiesInventory manageCitiesInventory = new ManageCitiesInventory(player);
            manageCitiesInventory.open();
        }, 2L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();
        if (!inventory.equals(this.inventory) || clickedItem == null) {
            System.out.println("Failed");
            return;
        }
        PersistentDataContainer data = Objects.requireNonNull(clickedItem.getItemMeta()).getPersistentDataContainer();

        switch (Objects.requireNonNull(data.get(namespace, PersistentDataType.STRING))) {
            case "Confirm": {
                close();
                if (EndCityManager.removeCity(cityName)) {
                    EndCityManager.deleteCity(cityName);
                } else if (Main.citiesYml.getBoolean(cityName + ".Moving")) {
                    EndCityManager.deleteCity(cityName);
                }
                break;
            }
            case "Cancel": {
                close();
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        unregister();
    }
}
