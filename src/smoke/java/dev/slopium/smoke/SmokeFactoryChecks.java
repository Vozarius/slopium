package dev.slopium.smoke;

import com.simibubi.create.content.logistics.factoryBoard.*;
import dev.slopium.client.*;
import dev.slopium.visibility.CreateDistanceControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;

final class SmokeFactoryChecks {
    static void run() throws ReflectiveOperationException {
        var c = SlopiumConfig.get(); var mc = Minecraft.getInstance();
        var previous = mc.level; var screen = mc.screen; var previousPlayer = mc.player;
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "factory_gauge"));
        var be = ((EntityBlock) block).newBlockEntity(new BlockPos(1000, 0, 0), block.defaultBlockState());
        if (!(be instanceof FactoryPanelBlockEntity panel)) throw new AssertionError("Factory fixture missing");
        var server = (Level) SmokeRadicalChecks.allocate(net.minecraft.server.level.ServerLevel.class);
        panel.setLevel(server);
        c.distanceCreateTicks = true;
        if (CreateDistanceControl.skip(panel, c)) throw new AssertionError("Server tick throttled");
        if (Slopium.sableAvailable()) return;
        var level = (ClientLevel) SmokeRadicalChecks.allocate(ClientLevel.class);
        var side = Level.class.getDeclaredField("isClientSide"); side.setAccessible(true); side.setBoolean(level, true);
        var data = (ClientLevel.ClientLevelData) SmokeRadicalChecks.allocate(ClientLevel.ClientLevelData.class);
        var ld = Level.class.getDeclaredField("levelData"); ld.setAccessible(true); ld.set(level, data);
        try {
            mc.level = level; mc.screen = null; panel.setLevel(level);
            var behaviour = panel.panels.values().iterator().next();
            var connection = new FactoryPanelConnection(new FactoryPanelPosition(new BlockPos(1001, 0, 0), behaviour.slot), 1);
            c.factoryConnectionRange = c.factoryBulbRange = true;
            c.factoryConnectionDistance = c.factoryBulbDistance = 16;
            long paths = SlopiumStats.factoryPathsSkipped, bulbs = SlopiumStats.factoryBulbsSkipped;
            FactoryPanelRenderer.renderPath(behaviour, connection, 0, null, null, 0, 0);
            FactoryPanelRenderer.renderBulb(behaviour, 0, null, null, 0, 0);
            if (SlopiumStats.factoryPathsSkipped != paths + 1 || SlopiumStats.factoryBulbsSkipped != bulbs + 1) throw new AssertionError("Factory actual render entry did not skip");
            var player = (net.minecraft.client.player.LocalPlayer) SmokeRadicalChecks.allocate(net.minecraft.client.player.LocalPlayer.class);
            var position = net.minecraft.world.entity.Entity.class.getDeclaredField("position"); position.setAccessible(true); position.set(player, net.minecraft.world.phys.Vec3.ZERO);
            mc.player = player;
            var init = com.simibubi.create.foundation.blockEntity.SmartBlockEntity.class.getDeclaredField("initialized"); init.setAccessible(true); init.setBoolean(panel, true);
            c.createTickDistance = 16; c.createTickInterval = 4; c.hiddenGraceTicks = 10;
            int skipped = 0;
            for (int t = 0; t < 18; t++) {
                data.setGameTime(t);
                boolean skip = CreateDistanceControl.skip(panel, c);
                if (t < 10 && skip) throw new AssertionError("Create grace lost");
                if (t >= 10 && skip) skipped++;
            }
            if (skipped != 6) throw new AssertionError("Create cadence incorrect: " + skipped);
            var tracked = (dev.slopium.visibility.TrackedBlockEntity) panel;
            tracked.slopium$tickGate().wake(18, 10); data.setGameTime(19);
            if (CreateDistanceControl.skip(panel, c)) throw new AssertionError("Packet wake ignored");
            mc.screen = screen;
            if (CreateDistanceControl.skip(panel, c)) throw new AssertionError("Menu guard failed");
            mc.screen = null;
            position.set(player, new net.minecraft.world.phys.Vec3(1000, 0, 0));
            data.setGameTime(40);
            if (CreateDistanceControl.skip(panel, c)) throw new AssertionError("Player interaction radius failed");
        } finally { mc.level = previous; mc.screen = screen; mc.player = previousPlayer; }
    }
}
