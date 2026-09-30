package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IfHasMaterialActionTest extends MockServerTest {

    private ActionContext contextWithPlayer() {
        return new ActionContext(spawnPlayerInWorld("Steve"));
    }

    @Test
    void pushesTrueWhenPlayerHasEnoughMaterial() {
        ActionContext ctx = contextWithPlayer();
        ctx.getPlayer().getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        new IfHasMaterialAction("DIAMOND", 3).execute(ctx);
        assertTrue(ctx.shouldExecute());
    }

    @Test
    void pushesFalseWhenPlayerLacksMaterial() {
        ActionContext ctx = contextWithPlayer();
        ctx.getPlayer().getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
        new IfHasMaterialAction("DIAMOND", 3).execute(ctx);
        assertFalse(ctx.shouldExecute());
    }

    @Test
    void ignoresItemMetaAndMatchesByMaterialOnly() {
        ActionContext ctx = contextWithPlayer();
        ItemStack namedDiamond = new ItemStack(Material.DIAMOND, 3);
        ItemMeta meta = namedDiamond.getItemMeta();
        meta.setDisplayName("Special Diamond");
        namedDiamond.setItemMeta(meta);
        ctx.getPlayer().getInventory().addItem(namedDiamond);
        new IfHasMaterialAction("DIAMOND", 3).execute(ctx);
        assertTrue(ctx.shouldExecute());
    }

    @Test
    void pushesFalseForInvalidMaterial() {
        ActionContext ctx = contextWithPlayer();
        new IfHasMaterialAction("NOT_A_MATERIAL", 1).execute(ctx);
        assertFalse(ctx.shouldExecute());
    }

    @Test
    void isFlowControl() {
        assertTrue(new IfHasMaterialAction("DIAMOND", 1).isFlowControl());
    }

    @Test
    void getActionTypeAndSerialize() {
        IfHasMaterialAction a = new IfHasMaterialAction("diamond", 3);
        assertEquals(ActionType.IF_HAS_MATERIAL, a.getActionType());
        assertEquals("if_has_material DIAMOND 3", a.serialize());
    }

    @Test
    void parseValidLine() {
        IfHasMaterialAction a = IfHasMaterialAction.parse("if_has_material DIAMOND 3");
        assertEquals("if_has_material DIAMOND 3", a.serialize());
    }

    @Test
    void parseRejectsBadInput() {
        assertNull(IfHasMaterialAction.parse("if_has_material DIAMOND notanumber"));
        assertNull(IfHasMaterialAction.parse("wrong DIAMOND 3"));
        assertNull(IfHasMaterialAction.parse("if_has_material DIAMOND"));
    }
}
