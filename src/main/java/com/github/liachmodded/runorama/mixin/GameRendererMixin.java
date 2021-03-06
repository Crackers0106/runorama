/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.liachmodded.runorama.mixin;

import com.github.liachmodded.runorama.Runorama;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.nio.file.Path;
import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private boolean renderingPanorama;

    @Shadow
    public abstract void renderWorld(float float_1, long long_1, MatrixStack matrixStack_1);

    @Inject(method = "render", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V"))
    public void runorama$renderPanorama(float tickDelta, long startTime, boolean tick, CallbackInfo ci, int i1, int i2, MatrixStack outerMatrixStack) {
        Runorama runorama = Runorama.getInstance();
        if (runorama.needsScreenshot) {
            Runorama.LOGGER.info("Taking screenshot");
            runorama.needsScreenshot = false;

            Path root = runorama.getSettings().getCurrentRunoramaFolder();

            // setup
            boolean oldFov90 = renderingPanorama;
            renderingPanorama = true;
            List<Quaternion> rotations = Runorama.ROTATIONS;
            // take
            for (int i = 0; i < rotations.size(); i++) {
                MatrixStack stack = new MatrixStack(); // Adding a layer in the old one fails empty check
                stack.multiply(rotations.get(i));
                doRender(tickDelta, startTime, stack);
                takeScreenshot(runorama, root, i);
            }
            // restore
            renderingPanorama = oldFov90;

            client.player.addChatMessage(new TranslatableText("runorama.shot", new LiteralText(root.toAbsolutePath().toString()).styled(style -> {
                style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, root.toAbsolutePath().toString()));
            })), false);
            runorama.getSettings().nextScreenshot();
        }
    }

    private void doRender(float tickDelta, long startTime, MatrixStack matrixStack_1) {
        this.renderWorld(tickDelta, Util.getMeasuringTimeNano() + startTime, matrixStack_1);
    }

    private void takeScreenshot(Runorama runorama, Path folder, int id) {
        NativeImage shot = ScreenshotUtils.takeScreenshot(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(),
                client.getFramebuffer());
        runorama.saveScreenshot(shot, folder, id);
    }
}
