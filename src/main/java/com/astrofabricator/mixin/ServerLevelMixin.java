package com.astrofabricator.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerLevel.class, priority = 800)
public abstract class ServerLevelMixin {

    // 删除了繁琐的 astro$iterationCounter 静态变量

    @Unique
    private long astro$dimensionUniqueSeed;

    /**
     * 1. 修改构造函数种子参数 (主要影响群系边缘平滑)
     * 使用 @Local 捕获需要的 ResourceKey，避免签名不匹配导致崩溃
     */
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static long astro$applyRandomSeed(long originalSeed, @Local(argsOnly = true) ResourceKey<Level> key) {
        if (key.location().getNamespace().equals("astrofabricator")) {
            // 【极简修改】与 ChunkMap 保持一致，砍掉乘法，直接加上维度名哈希
            long newSeed = originalSeed + key.location().hashCode();
            System.out.println("§b[Astro Mixin]§f Registering Biome Zoom Seed for " + key.location().getPath() + ": " + newSeed);
            return newSeed;
        }
        return originalSeed;
    }

    /**
     * 2. 在构造函数末尾记录当前维度的种子
     * 同样使用 @Local 直接拿到刚才被 ModifyVariable 修改后的 seed 变量
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void astro$captureSeed(CallbackInfo ci, @Local(argsOnly = true, ordinal = 0) long finalSeed, @Local(argsOnly = true) ResourceKey<Level> key) {
        if (key.location().getNamespace().equals("astrofabricator")) {
            this.astro$dimensionUniqueSeed = finalSeed; // 直接保存已经被修改过的新种子
        }
    }

    /**
     * 3. 重写 getSeed，让 /seed 指令显示该维度真实的随机种子
     */
    @Inject(method = "getSeed", at = @At("HEAD"), cancellable = true)
    private void astro$overrideInGameSeedDisplay(CallbackInfoReturnable<Long> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (level.dimension().location().getNamespace().equals("astrofabricator") && this.astro$dimensionUniqueSeed != 0) {
            cir.setReturnValue(this.astro$dimensionUniqueSeed);
        }
    }
}