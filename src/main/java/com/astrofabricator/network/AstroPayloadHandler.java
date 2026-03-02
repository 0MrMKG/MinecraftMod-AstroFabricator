package com.astrofabricator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator; // 必须导入

public class AstroPayloadHandler {

    public static void handleData(final AstroPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            MinecraftServer server = player.getServer();
            if (server == null) return;

            switch (payload.action()) {
                case "teleport" -> {
                    ResourceLocation loc = ResourceLocation.parse(payload.data1());
                    ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, loc);
                    ServerLevel level = server.getLevel(levelKey);

                    if (level != null) {
                        double offsetX = 0.5;
                        double offsetZ = 0.5;
                        int targetY;

                        if (level.dimensionType().hasCeiling()) {
                            int minY = level.getMinBuildHeight();
                            int startY = level.getLogicalHeight() - 1;
                            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(offsetX, startY, offsetZ);

                            while (mutablePos.getY() > minY && !level.getBlockState(mutablePos).isAir()) {
                                mutablePos.move(0, -1, 0);
                            }
                            while (mutablePos.getY() > minY && level.getBlockState(mutablePos).isAir()) {
                                mutablePos.move(0, -1, 0);
                            }
                            targetY = mutablePos.getY() + 1;
                        } else {
                            targetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) offsetX, (int) offsetZ);
                            if (targetY <= level.getMinBuildHeight()) {
                                targetY = 64;
                            }
                        }

                        player.teleportTo(level, offsetX, (double) targetY + 0.1, offsetZ, player.getYRot(), player.getXRot());

                        player.sendSystemMessage(Component.literal("§e[导航]§f 已成功跳跃至中心坐标: §b" + (int)offsetX + ", " + targetY + ", " + (int)offsetZ));
                    } else {
                        player.sendSystemMessage(Component.literal("§c[错误]§f 目标维度未加载或不存在"));
                    }
                }

                case "refresh_list" -> {
                    MutableComponent message = Component.literal("\n§e==== [Astro 维度导航]  ====\n");
                    Path worldSaveRoot = server.getWorldPath(LevelResource.ROOT);
                    var registry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);

                    // --- 核心排序逻辑开始 ---
                    registry.registryKeySet().stream()
                            .sorted(Comparator.comparing((ResourceKey<?> k) ->
                                            !k.location().getNamespace().equals("minecraft")) // 1. 原版优先 (false < true)
                                    .thenComparing(k -> k.location().getNamespace())  // 2. 模组 ID 字母序
                                    .thenComparing(k -> k.location().getPath()))     // 3. 维度名称字母序
                            .forEach(stemKey -> {
                                ResourceLocation loc = stemKey.location();
                                ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, loc);

                                boolean isOverworld = loc.equals(Level.OVERWORLD.location());
                                Path regionPath = worldSaveRoot.resolve("dimensions")
                                        .resolve(loc.getNamespace())
                                        .resolve(loc.getPath())
                                        .resolve("region");

                                boolean isLoadedInMemory = server.levelKeys().contains(levelKey);
                                boolean hasPlayers = false;
                                if (isLoadedInMemory) {
                                    ServerLevel lv = server.getLevel(levelKey);
                                    if (lv != null && !lv.players().isEmpty()) {
                                        hasPlayers = true;
                                    }
                                }

                                final boolean isVisited = isOverworld || Files.exists(regionPath);
                                final boolean isActive = hasPlayers;

                                int color;
                                String statusDesc;
                                String icon;

                                if (isActive) {
                                    icon = "§a● ";
                                    color = 0x55FF55;
                                    statusDesc = "§a[信号活跃]";
                                } else if (isVisited) {
                                    icon = "§f○ ";
                                    color = 0xFFFFFF;
                                    statusDesc = "§f[已存坐标]";
                                } else {
                                    icon = "§8○ ";
                                    color = 0x555555;
                                    statusDesc = "§8[未知维度]";
                                }

                                MutableComponent line = Component.literal(icon)
                                        .append(Component.literal(loc.toString()).withStyle(style -> style
                                                .withColor(color)
                                                .withUnderlined(isVisited)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/af tpd " + loc))
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                        Component.literal("§f维度: §b" + loc + "\n" +
                                                                "§7状态: " + statusDesc + "\n\n" +
                                                                (isVisited ? "§e▶ 点击发起时空跳跃" : "§c⚠ 坐标未解密"))))
                                        ))
                                        .append(" " + statusDesc + "\n");

                                message.append(line);
                            });
                    // --- 核心排序逻辑结束 ---

                    message.append(Component.literal("§e================================\n"));
                    player.sendSystemMessage(message);
                }
            }
        });
    }
}