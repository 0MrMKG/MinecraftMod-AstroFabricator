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
                        // 目标坐标固定为维度中心 (0, 0)
                        double offsetX = 0.5; // 加上 0.5 居中方块
                        double offsetZ = 0.5;
                        int targetY;

                        // --- 表面寻找逻辑 ---
                        if (level.dimensionType().hasCeiling()) {
                            // 针对有顶棚维度（如下界）的扫描逻辑
                            int minY = level.getMinBuildHeight();
                            // 从逻辑高度（下界通常是 127）开始向下找
                            int startY = level.getLogicalHeight() - 1;

                            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(offsetX, startY, offsetZ);

                            // 1. 跳过顶层的基岩层（寻找空气）
                            while (mutablePos.getY() > minY && !level.getBlockState(mutablePos).isAir()) {
                                mutablePos.move(0, -1, 0);
                            }
                            // 2. 已经在空气中了，寻找下方的第一个实心方块（地板）
                            while (mutablePos.getY() > minY && level.getBlockState(mutablePos).isAir()) {
                                mutablePos.move(0, -1, 0);
                            }
                            targetY = mutablePos.getY() + 1;
                        } else {
                            // 针对普通维度（主世界、末地、插件维度）
                            // 使用 MOTION_BLOCKING_NO_LEAVES 自动寻找最高非叶子方块
                            targetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) offsetX, (int) offsetZ);

                            // 兜底方案：如果是空岛或虚空，设定在高度 64
                            if (targetY <= level.getMinBuildHeight()) {
                                targetY = 64;
                            }
                        }

                        // 传送前视觉反馈：失明与反胃
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));

                        // 执行跨维度传送
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

                    registry.registryKeySet().forEach(stemKey -> {
                        ResourceLocation loc = stemKey.location();
                        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, loc);

                        // 检查维度是否存在物理存档
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

                        // UI 样式逻辑
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

                    message.append(Component.literal("§e================================\n"));
                    player.sendSystemMessage(message);
                }
            }
        });
    }
}