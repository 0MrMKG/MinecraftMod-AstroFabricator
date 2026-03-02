package com.astrofabricator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// 我们将目标改为 ChunkMap，因为 RandomState 是在这里诞生的
@Mixin(ChunkMap.class)
public abstract class AstroRandomStateMixin {

    /**
     * 拦截 ChunkMap 构造函数中向 ServerLevel 获取种子的调用。
     * ChunkMap 在初始化时会创建 RandomState（管理所有地形噪声和群系分布的核心引擎），
     * 它会通过 level.getSeed() 拿种子。我们就在这里把它截胡！
     */
    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getSeed()J"
            )
    )
    private long astro$injectTrueTerrainSeed(ServerLevel instance, Operation<Long> original) {
        // 先拿到主世界的原始种子
        long originalSeed = original.call(instance);

        // 如果是我们自己的星域维度
        if (instance.dimension().location().getNamespace().equals("astrofabricator")) {

            // 【极简修改】直接将原种子加上维度名称的哈希值，砍掉所有多余运算
            long newSeed = originalSeed + instance.dimension().location().hashCode();

            System.out.println("§d[Astro Mixin]§f 地形引擎已为 " + instance.dimension().location().getPath() + " 重构极简底层种子: " + newSeed);
            return newSeed;
        }

        // 其他原版维度原样返回
        return originalSeed;
    }
}