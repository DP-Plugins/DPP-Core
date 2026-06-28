package com.darksoldier1404.dppc.builder.action;

import com.darksoldier1404.dppc.builder.action.actions.CancelAction;
import com.darksoldier1404.dppc.builder.action.actions.DelayAction;
import com.darksoldier1404.dppc.builder.action.actions.SendMessageAction;
import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionBuilderTest extends PluginTest {

    /** One normalized line per action type — i.e. exactly what {@code serialize()} produces. */
    private static final String[] ALL_ACTION_LINES = {
            "delay 20",
            "send_message hello",
            "send_title T|S|10|70|20",
            "send_actionbar bar",
            "broadcast hi",
            "broadcast_world hi",
            "execute_as_admin say hi",
            "execute_as_player say hi",
            "teleport world 1.00,2.00,3.00",
            "close_inventory",
            "set_gamemode CREATIVE",
            "give_exp 10",
            "take_exp 5",
            "set_health 20.0",
            "set_hunger 20",
            "kick bye",
            "play_sound ENTITY_PLAYER_LEVELUP 1.00 1.00",
            "play_particle FLAME 5 0.50 0.50 0.50",
            "add_potion_effect SPEED 10 1",
            "remove_potion_effect SPEED",
            "clear_effects",
            "give_item STONE 5",
            "take_item STONE 5",
            "set_temp_variable k v",
            "add_temp_variable n 2.0",
            "random_temp_number r 1 6",
            "set_player_variable k v",
            "add_player_variable n 2.0",
            "random_player_number r 1 6",
            "set_global_variable k v",
            "add_global_variable n 2.0",
            "random_global_number r 1 6",
            "if_has_permission perm.node",
            "if_not_permission perm.node",
            "if_temp_variable_equals a b",
            "if_temp_variable_not_equals a b",
            "if_temp_variable_greater a 1.0",
            "if_temp_variable_less a 1.0",
            "if_player_variable_equals a b",
            "if_player_variable_not_equals a b",
            "if_player_variable_greater a 1.0",
            "if_player_variable_less a 1.0",
            "if_global_variable_equals a b",
            "if_global_variable_not_equals a b",
            "if_global_variable_greater a 1.0",
            "if_global_variable_less a 1.0",
            "else",
            "end_if",
            "cancel",
            "call_action other",
    };

    private ActionBuilder builder() {
        return new ActionBuilder(plugin, "test");
    }

    @Test
    void parseScriptParsesEveryActionTypeAndRoundTrips() {
        String script = String.join("\n", ALL_ACTION_LINES);
        ActionBuilder b = builder().parseScript(script);

        assertEquals(ALL_ACTION_LINES.length, b.getActions().size(),
                "every line should parse to exactly one action");
        // Each parsed action must serialize back to its source line.
        List<Action> actions = b.getActions();
        for (int i = 0; i < ALL_ACTION_LINES.length; i++) {
            assertEquals(ALL_ACTION_LINES[i], actions.get(i).serialize());
        }
    }

    @Test
    void parseScriptSkipsBlankAndCommentLines() {
        ActionBuilder b = builder().parseScript("# a comment\n\n   \ndelay 5\n# trailing");
        assertEquals(1, b.getActions().size());
        assertInstanceOf(DelayAction.class, b.getActions().get(0));
    }

    @Test
    void unknownLineIsIgnored() {
        ActionBuilder b = builder().parseScript("this_is_not_an_action 1 2 3");
        assertTrue(b.getActions().isEmpty());
    }

    @Test
    void fluentApiAppendsInOrder() {
        ActionBuilder b = builder().sendMessage("hi").delay(5).cancel();
        assertEquals(3, b.getActions().size());
        assertInstanceOf(SendMessageAction.class, b.getActions().get(0));
        assertInstanceOf(DelayAction.class, b.getActions().get(1));
        assertInstanceOf(CancelAction.class, b.getActions().get(2));
    }

    @Test
    void editingModeReplacesInsteadOfAppending() {
        ActionBuilder b = builder().sendMessage("first").delay(5);
        b.setEditing(true);
        b.setCurrentEditIndex(0);
        b.sendMessage("replaced");
        assertEquals(2, b.getActions().size());
        assertEquals("send_message replaced", b.getActions().get(0).serialize());
    }

    @Test
    void yamlExportImportRoundTrip() {
        ActionBuilder original = builder().parseScript(String.join("\n", ALL_ACTION_LINES));
        YamlConfiguration yaml = original.exportToYaml();
        assertEquals("test", yaml.getString("ACTION_NAME"));

        ActionBuilder imported = new ActionBuilder(plugin, "other").importFromYaml(yaml);
        assertEquals("test", imported.getActionName());
        assertEquals(original.getActions().size(), imported.getActions().size());
        for (int i = 0; i < original.getActions().size(); i++) {
            assertEquals(original.getActions().get(i).serialize(),
                    imported.getActions().get(i).serialize());
        }
    }
}
