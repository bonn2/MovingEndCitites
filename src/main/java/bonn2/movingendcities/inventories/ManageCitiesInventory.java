package bonn2.movingendcities.inventories;

import bonn2.movingendcities.EndCityManager;
import bonn2.movingendcities.Main;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.bukkit.Bukkit.getServer;

public class ManageCitiesInventory implements Listener {

    private final List<Inventory> inventories;
    private final Player player;
    private final NamespacedKey namespace;
    private int currentPage;
    private boolean protect;
    private boolean awaitingText;

    public ManageCitiesInventory(Player player) {
        Main plugin = Main.plugin;
        this.player = player;
        namespace = new NamespacedKey(plugin, "Action");
        protect = false;
        awaitingText = false;
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        inventories = new ArrayList<>();

        int inventorySize = 54;
        int neededInventories = ((int) Math.ceil(((double) yml.getKeys(false).size()) / inventorySize));
        if (neededInventories == 0) neededInventories++;
        // Setup inventory with buttons
        for (int i = 0; i < neededInventories; i++) {
            Inventory inv = Bukkit.createInventory(null, inventorySize, "End Cities");

            ItemStack previous = new ItemStack(Material.ARROW);
            ItemMeta previousMeta = previous.getItemMeta();
            assert previousMeta != null;
            previousMeta.setDisplayName(ChatColor.GREEN + "<< Previous Page");
            previousMeta.getPersistentDataContainer().set(namespace, PersistentDataType.STRING, "Previous");
            previous.setItemMeta(previousMeta);

            ItemStack newCity = new ItemStack(Material.EMERALD);
            ItemMeta newCityMeta = newCity.getItemMeta();
            assert newCityMeta != null;
            newCityMeta.setDisplayName(ChatColor.GREEN + "New City");
            newCityMeta.getPersistentDataContainer().set(namespace, PersistentDataType.STRING, "New");
            newCity.setItemMeta(newCityMeta);

            ItemStack close = new ItemStack(Material.BARRIER);
            ItemMeta closeMeta = close.getItemMeta();
            assert closeMeta != null;
            closeMeta.setDisplayName(ChatColor.RED + "Close Menu");
            closeMeta.getPersistentDataContainer().set(namespace, PersistentDataType.STRING, "Close");
            close.setItemMeta(closeMeta);

            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            assert nextMeta != null;
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page >>");
            nextMeta.getPersistentDataContainer().set(namespace, PersistentDataType.STRING, "Next");
            next.setItemMeta(nextMeta);

            if (i != 0) {
                // Show on all pages but the first
                inv.setItem(45, previous);
            }
            inv.setItem(48, newCity);
            inv.setItem(50, close);
            if (i != neededInventories - 1) {
                // Show on all pages but the last
                inv.setItem(53, next);
            }

            inventories.add(inv);
        }

        int count = 0;
        for (String key : yml.getKeys(false)) {
            ItemStack itemStack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
            ItemMeta itemMeta = itemStack.getItemMeta();
            assert itemMeta != null;
            itemMeta.setDisplayName(ChatColor.LIGHT_PURPLE + key);
            List<String> lore = new ArrayList<>();
            if (yml.getBoolean(key + ".Moving")) {
                itemStack.setType(Material.YELLOW_STAINED_GLASS_PANE);
                lore.add(ChatColor.GOLD + "Moving...");
            } else {
                Location maxLocation = yml.getLocation(key + ".MaxLocation");
                assert maxLocation != null;
                lore.add(ChatColor.GOLD + "" + maxLocation.getX() + " " + maxLocation.getY() + " " + maxLocation.getZ());
                lore.add(ChatColor.GOLD + Objects.requireNonNull(maxLocation.getWorld()).getName());
                lore.add(ChatColor.GOLD + yml.getString(key + ".Schematic"));
                lore.add(ChatColor.BLUE + "Left click to teleport");
            }
            lore.add(ChatColor.RED + "Right click to delete");
            itemMeta.setLore(lore);
            itemMeta.getPersistentDataContainer().set(namespace, PersistentDataType.STRING, "City");
            itemStack.setItemMeta(itemMeta);
            int inventoryNumber = ((int) Math.floor((double) count / 45));
            int slotNumber = count - (inventoryNumber * 45);

            inventories.get(inventoryNumber).setItem(slotNumber, itemStack);
            count++;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        player.openInventory(inventories.get(0));
        currentPage = 0;
    }

    public void nextPage() {
        close(true);
        player.openInventory(inventories.get(++currentPage));
    }

    public void previousPage() {
        close(true);
        player.openInventory(inventories.get(--currentPage));
    }

    public void getCityName() {
        awaitingText = true;
        close(true);
        player.sendMessage("Enter the name of the city in chat.");
    }

    public void newCity(String name) {
        player.sendMessage("Attempting to summon end city in world " + player.getWorld().getName() + ".\nFurther info will be printed to console.");
        Main.citiesYml.set(name + ".World", player.getWorld().getName());
        try {
            Main.saveCitiesYml();
        } catch (IOException e) {
            e.printStackTrace();
        }
        EndCityManager.summonEndCity(player.getWorld(), name);
    }

    public void close(boolean protect) {
        this.protect = protect;
        player.closeInventory();
        this.protect = false;
    }

    public void teleport(ItemStack item) {
        Main plugin = Main.plugin;
        String key = ChatColor.stripColor(Objects.requireNonNull(item.getItemMeta()).getDisplayName());
        File endcityYml = new File(plugin.getDataFolder() + File.separator + "cities.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
        if (yml.getBoolean(key + ".Moving")) {
            return;
        }
        close(false);
        Location location = yml.getLocation(key + ".MaxLocation");
        assert location != null;
        player.teleport(location);
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();
        if (!event.getWhoClicked().getUniqueId().equals(player.getUniqueId()) || !inventories.contains(inventory)) {
            return;
        }
        try {
            PersistentDataContainer data = Objects.requireNonNull(clickedItem.getItemMeta()).getPersistentDataContainer();
            switch (Objects.requireNonNull(data.get(namespace, PersistentDataType.STRING))) {
                case "Next": {
                    nextPage();
                    break;
                }
                case "Previous": {
                    previousPage();
                    break;
                }
                case "Close": {
                    close(false);
                    break;
                }
                case "City": {
                    if (event.getClick().equals(ClickType.LEFT)) {
                        teleport(clickedItem);
                    } else if (event.getClick().equals(ClickType.RIGHT)) {
                        close(false);
                        ConfirmDeleteInventory confirmDeleteInventory = new ConfirmDeleteInventory(player, clickedItem);
                        confirmDeleteInventory.open();
                    }
                    event.setCancelled(true);
                    break;
                }
                case "New": {
                    getCityName();
                    break;
                }
                case "Name": {
                    if (event.getSlot() != 2) {
                        break;
                    }
                    String name = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                    newCity(name);
                    break;
                }
                default: {
                    event.setCancelled(true);
                }
            }
        } catch (NullPointerException e) {
            event.setCancelled(true);
        }
    }

    // TODO: Update gui, while it is open

    @EventHandler
    public void chatEvent(AsyncPlayerChatEvent event) {
        if (event.getPlayer().getUniqueId().equals(player.getUniqueId()) && awaitingText) {
            File endcityYml = new File(Main.plugin.getDataFolder() + File.separator + "cities.yml");
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(endcityYml);
            if (yml.getKeys(false).contains(event.getMessage())) {
                player.sendMessage(ChatColor.RED + "That city already exists!");
                event.setCancelled(true);
                return;
            }
            newCity(event.getMessage());
            awaitingText = false;
            event.setCancelled(true);
            unregister();
            BukkitScheduler scheduler = getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(Main.plugin, () -> {
                ManageCitiesInventory manageCitiesInventory = new ManageCitiesInventory(player);
                manageCitiesInventory.open();
            }, 2L);
        }
    }

    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        if (!protect && event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
            unregister();
        }
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }
}
