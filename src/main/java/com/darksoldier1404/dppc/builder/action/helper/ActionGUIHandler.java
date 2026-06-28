package com.darksoldier1404.dppc.builder.action.helper;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.lang.DLang;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dppc.utils.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionGUIHandler implements Listener {
    private final Map<UUID, Tuple<ActionGUI, ActionType>> pendingInput = new HashMap<>();

    private DLang lang() {
        return DPPCore.getInstance().getLang();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DInventory)) return;
        DInventory inv = (DInventory) e.getInventory().getHolder();
        if (!(inv.getObj() instanceof ActionGUI)) return;
        ActionGUI ag = (ActionGUI) inv.getObj();
        if (!inv.isValidHandler(ag.getPlugin())) return;

        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        if (e.getInventory().getType() == InventoryType.PLAYER) return;

        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        // --- Navigation / control buttons ---
        if (NBT.hasTagKey(item, "dppc.action")) {
            String action = NBT.getStringTag(item, "dppc.action");
            switch (action) {
                case "add":
                    ag.openActionSelectGUI(p);
                    return;
                case "save":
                    YamlConfiguration raw = ag.getActionBuilder().exportToYaml();
                    ConfigUtils.saveCustomData(ag.getPlugin(), raw, ag.getActionBuilder().getActionName(), "actions");
                    DPPCore.actions.put(ag.getActionBuilder().getActionName(), ag.getActionBuilder());
                    p.closeInventory();
                    p.sendMessage(lang().getWithArgs("ab.msg.saved", ag.getActionBuilder().getActionName()));
                    return;
                case "prev":
                case "next":
                    int targetPage = NBT.getIntegerTag(item, "dppc.page");
                    ag.openActionBuilderGUI(p, targetPage);
                    return;
            }
        }

        // --- Action list (channel 0) ---
        if (inv.isValidChannel(0) && NBT.hasTagKey(item, "dppc.actionType")) {
            int globalIndex = NBT.getIntegerTag(item, "dppc.actionIndex");

            if (e.getClick() == ClickType.RIGHT) {
                if (globalIndex < ag.getActionBuilder().getActions().size()) {
                    ag.getActionBuilder().getActions().remove(globalIndex);
                }
                ag.openActionBuilderGUI(p, ag.currentPage);
                return;
            }

            if (e.getClick() == ClickType.LEFT) {
                ActionType actionType = ActionType.valueOf(NBT.getStringTag(item, "dppc.actionType"));
                ag.getActionBuilder().setCurrentEditIndex(globalIndex);
                ag.getActionBuilder().setEditing(true);
                pendingInput.put(p.getUniqueId(), Tuple.of(ag, actionType));
                sendInputPrompt(p, actionType);
                p.closeInventory();
            }
            return;
        }

        // --- Action type selector (channel 1) ---
        if (inv.isValidChannel(1) && NBT.hasTagKey(item, "dppc.actionTypeSelect")) {
            ActionType actionType = ActionType.valueOf(NBT.getStringTag(item, "dppc.actionTypeSelect"));

            // No-parameter actions: add immediately
            if (isNoParamAction(actionType)) {
                addNoParamAction(ag, actionType);
                ag.openActionBuilderGUI(p, ag.currentPage);
                return;
            }

            pendingInput.put(p.getUniqueId(), Tuple.of(ag, actionType));
            sendInputPrompt(p, actionType);
            p.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        if (!pendingInput.containsKey(uid)) return;

        e.setCancelled(true);
        ActionGUI ag = pendingInput.get(uid).getA();
        ActionType type = pendingInput.get(uid).getB();
        Player p = e.getPlayer();
        String input = e.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            pendingInput.remove(uid);
            ag.getActionBuilder().setEditing(false);
            Bukkit.getScheduler().runTaskLater(ag.getPlugin(), () -> ag.openActionBuilderGUI(p, ag.currentPage), 1L);
            p.sendMessage(lang().get("ab.msg.cancelled"));
            return;
        }

        boolean success = applyInput(ag, type, input, p);
        if (success) {
            pendingInput.remove(uid);
            Bukkit.getScheduler().runTaskLater(ag.getPlugin(), () -> ag.openActionBuilderGUI(p, ag.currentPage), 1L);
        }
    }

    private boolean applyInput(ActionGUI ag, ActionType type, String input, Player p) {
        try {
            switch (type) {
                case DELAY:
                    ag.getActionBuilder().delay(Long.parseLong(input));
                    break;
                case SEND_MESSAGE:
                    ag.getActionBuilder().sendMessage(input);
                    break;
                case SEND_TITLE:
                    // Format: title|subtitle or title|subtitle|fadeIn|stay|fadeOut
                    String[] titleParts = input.split("\\|", -1);
                    String title = titleParts.length > 0 ? titleParts[0] : "";
                    String subtitle = titleParts.length > 1 ? titleParts[1] : "";
                    int fadeIn = titleParts.length > 2 ? Integer.parseInt(titleParts[2]) : 10;
                    int stay = titleParts.length > 3 ? Integer.parseInt(titleParts[3]) : 70;
                    int fadeOut = titleParts.length > 4 ? Integer.parseInt(titleParts[4]) : 20;
                    ag.getActionBuilder().sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                    break;
                case SEND_ACTIONBAR:
                    ag.getActionBuilder().sendActionBar(input);
                    break;
                case BROADCAST:
                    ag.getActionBuilder().broadcast(input);
                    break;
                case BROADCAST_WORLD:
                    ag.getActionBuilder().broadcastWorld(input);
                    break;
                case EXECUTE_AS_ADMIN:
                    ag.getActionBuilder().executeCommandAsAdmin(input);
                    break;
                case EXECUTE_AS_PLAYER:
                    ag.getActionBuilder().executeCommandAsPlayer(input);
                    break;
                case TELEPORT: {
                    // Format: world x,y,z
                    String[] tpParts = input.split("\\s+");
                    if (tpParts.length != 2) {
                        p.sendMessage(lang().get("ab.format.teleport"));
                        return false;
                    }
                    String[] coords = tpParts[1].split(",");
                    if (coords.length != 3) {
                        p.sendMessage(lang().get("ab.format.coords"));
                        return false;
                    }
                    ag.getActionBuilder().teleport(tpParts[0],
                            Double.parseDouble(coords[0]),
                            Double.parseDouble(coords[1]),
                            Double.parseDouble(coords[2]));
                    break;
                }
                case SET_GAMEMODE:
                    ag.getActionBuilder().setGamemode(input.toUpperCase());
                    break;
                case GIVE_EXP:
                    ag.getActionBuilder().giveExp(Integer.parseInt(input));
                    break;
                case TAKE_EXP:
                    ag.getActionBuilder().takeExp(Integer.parseInt(input));
                    break;
                case SET_HEALTH:
                    ag.getActionBuilder().setHealth(Double.parseDouble(input));
                    break;
                case SET_HUNGER:
                    ag.getActionBuilder().setHunger(Integer.parseInt(input));
                    break;
                case KICK:
                    ag.getActionBuilder().kick(input);
                    break;
                case PLAY_SOUND: {
                    String[] sp = input.split("\\s+");
                    String sound = sp[0];
                    float volume = sp.length > 1 ? Float.parseFloat(sp[1]) : 1.0f;
                    float pitch = sp.length > 2 ? Float.parseFloat(sp[2]) : 1.0f;
                    ag.getActionBuilder().playSound(sound, volume, pitch);
                    break;
                }
                case PLAY_PARTICLE: {
                    String[] pp = input.split("\\s+");
                    if (pp.length < 2) {
                        p.sendMessage(lang().get("ab.format.particle"));
                        return false;
                    }
                    ag.getActionBuilder().playParticle(pp[0], Integer.parseInt(pp[1]));
                    break;
                }
                case ADD_POTION_EFFECT: {
                    String[] ep = input.split("\\s+");
                    if (ep.length < 3) {
                        p.sendMessage(lang().get("ab.format.potion_effect"));
                        return false;
                    }
                    ag.getActionBuilder().addPotionEffect(ep[0], Integer.parseInt(ep[1]), Integer.parseInt(ep[2]));
                    break;
                }
                case REMOVE_POTION_EFFECT:
                    ag.getActionBuilder().removePotionEffect(input);
                    break;
                case GIVE_ITEM: {
                    String[] ip = input.split("\\s+");
                    if (ip.length < 2) {
                        p.sendMessage(lang().get("ab.format.item"));
                        return false;
                    }
                    ag.getActionBuilder().giveItem(ip[0], Integer.parseInt(ip[1]));
                    break;
                }
                case TAKE_ITEM: {
                    String[] ip = input.split("\\s+");
                    if (ip.length < 2) {
                        p.sendMessage(lang().get("ab.format.item"));
                        return false;
                    }
                    ag.getActionBuilder().takeItem(ip[0], Integer.parseInt(ip[1]));
                    break;
                }
                case SET_TEMP_VARIABLE: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().setTempVariable(vp[0], vp[1]);
                    break;
                }
                case SET_PLAYER_VARIABLE: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().setPlayerVariable(vp[0], vp[1]);
                    break;
                }
                case SET_GLOBAL_VARIABLE: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().setGlobalVariable(vp[0], vp[1]);
                    break;
                }
                case ADD_TEMP_VARIABLE: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().addTempVariable(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case ADD_PLAYER_VARIABLE: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().addPlayerVariable(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case ADD_GLOBAL_VARIABLE: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().addGlobalVariable(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case RANDOM_TEMP_NUMBER: {
                    String[] rp = input.split("\\s+");
                    if (rp.length < 3) {
                        p.sendMessage(lang().get("ab.format.random_number"));
                        return false;
                    }
                    ag.getActionBuilder().randomTempNumber(rp[0], Integer.parseInt(rp[1]), Integer.parseInt(rp[2]));
                    break;
                }
                case RANDOM_PLAYER_NUMBER: {
                    String[] rp = input.split("\\s+");
                    if (rp.length < 3) {
                        p.sendMessage(lang().get("ab.format.random_number"));
                        return false;
                    }
                    ag.getActionBuilder().randomPlayerNumber(rp[0], Integer.parseInt(rp[1]), Integer.parseInt(rp[2]));
                    break;
                }
                case RANDOM_GLOBAL_NUMBER: {
                    String[] rp = input.split("\\s+");
                    if (rp.length < 3) {
                        p.sendMessage(lang().get("ab.format.random_number"));
                        return false;
                    }
                    ag.getActionBuilder().randomGlobalNumber(rp[0], Integer.parseInt(rp[1]), Integer.parseInt(rp[2]));
                    break;
                }
                case IF_HAS_PERMISSION:
                    ag.getActionBuilder().ifHasPermission(input);
                    break;
                case IF_NOT_PERMISSION:
                    ag.getActionBuilder().ifNotPermission(input);
                    break;
                case IF_TEMP_VARIABLE_EQUALS: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().ifTempVariableEquals(vp[0], vp[1]);
                    break;
                }
                case IF_TEMP_VARIABLE_NOT_EQUALS: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().ifTempVariableNotEquals(vp[0], vp[1]);
                    break;
                }
                case IF_TEMP_VARIABLE_GREATER: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().ifTempVariableGreater(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case IF_TEMP_VARIABLE_LESS: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().ifTempVariableLess(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case IF_PLAYER_VARIABLE_EQUALS: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().ifPlayerVariableEquals(vp[0], vp[1]);
                    break;
                }
                case IF_PLAYER_VARIABLE_NOT_EQUALS: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().ifPlayerVariableNotEquals(vp[0], vp[1]);
                    break;
                }
                case IF_PLAYER_VARIABLE_GREATER: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().ifPlayerVariableGreater(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case IF_PLAYER_VARIABLE_LESS: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().ifPlayerVariableLess(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case IF_GLOBAL_VARIABLE_EQUALS: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().ifGlobalVariableEquals(vp[0], vp[1]);
                    break;
                }
                case IF_GLOBAL_VARIABLE_NOT_EQUALS: {
                    String[] vp = input.split("\\s+", 2);
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable"));
                        return false;
                    }
                    ag.getActionBuilder().ifGlobalVariableNotEquals(vp[0], vp[1]);
                    break;
                }
                case IF_GLOBAL_VARIABLE_GREATER: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().ifGlobalVariableGreater(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case IF_GLOBAL_VARIABLE_LESS: {
                    String[] vp = input.split("\\s+");
                    if (vp.length < 2) {
                        p.sendMessage(lang().get("ab.format.variable_number"));
                        return false;
                    }
                    ag.getActionBuilder().ifGlobalVariableLess(vp[0], Double.parseDouble(vp[1]));
                    break;
                }
                case CALL_ACTION:
                    ag.getActionBuilder().callAction(input);
                    break;
                default:
                    p.sendMessage(lang().get("ab.msg.unsupported_input"));
                    return false;
            }
            return true;
        } catch (NumberFormatException ex) {
            p.sendMessage(lang().getWithArgs("ab.msg.invalid_number", String.valueOf(ex.getMessage())));
            return false;
        } catch (Exception ex) {
            p.sendMessage(lang().getWithArgs("ab.msg.error", String.valueOf(ex.getMessage())));
            return false;
        }
    }

    private void sendInputPrompt(Player p, ActionType type) {
        p.sendMessage(lang().get("ab.msg.input_mode"));
        String key = "ab.prompt." + type.name().toLowerCase();
        p.sendMessage(lang().has(key) ? lang().get(key) : lang().get("ab.prompt.default"));
    }

    private boolean isNoParamAction(ActionType type) {
        return type == ActionType.CLOSE_INVENTORY
                || type == ActionType.CLEAR_EFFECTS
                || type == ActionType.ELSE
                || type == ActionType.END_IF
                || type == ActionType.CANCEL;
    }

    private void addNoParamAction(ActionGUI ag, ActionType type) {
        switch (type) {
            case CLOSE_INVENTORY: ag.getActionBuilder().closeInventory(); break;
            case CLEAR_EFFECTS: ag.getActionBuilder().clearEffects(); break;
            case ELSE: ag.getActionBuilder().orElse(); break;
            case END_IF: ag.getActionBuilder().endIf(); break;
            case CANCEL: ag.getActionBuilder().cancel(); break;
        }
    }
}
