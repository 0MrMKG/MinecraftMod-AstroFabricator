package com.astrofabricator.network;

import com.astrofabricator.AstroDimensionManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class AstroPayloadHandler {

    public static void handleData(final AstroPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MinecraftServer server = player.getServer();
            if (server == null) return;

            switch (payload.action()) {
                case "create" -> {
                    String template = payload.data1();
                    // 特殊处理：如果是 sakura，映射到我们的 JSON 路径
                    String path = template.equalsIgnoreCase("sakura")
                            ? "astrofabricator:sakura_template" : template;

                    AstroDimensionManager.createDimension(server, path, payload.data2());
                    player.sendSystemMessage(Component.literal("§d[Astro]§f 正在构建星域: " + payload.data2()));
                }
                case "teleport" -> {
                    ResourceLocation loc = ResourceLocation.parse(payload.data1());
                    ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, loc));
                    if (level != null) {
                        player.teleportTo(level, player.getX(), 100, player.getZ(), player.getYRot(), player.getXRot());
                        player.sendSystemMessage(Component.literal("§a[跃迁]§f 已到达目标星域"));
                    } else {
                        player.sendSystemMessage(Component.literal("§c[错误]§f 维度未就绪或不存在"));
                    }
                }
                case "refresh_list" -> {
                    StringBuilder sb = new StringBuilder("§e==== 已加载维度 ====\n");
                    server.levelKeys().forEach(key -> sb.append("§7- §f").append(key.location()).append("\n"));
                    player.sendSystemMessage(Component.literal(sb.toString()));
                }
            }
        });
    }
}