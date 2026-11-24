package com.djawad1.dont.mixin;

import com.djawad1.dont.client.DontClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/** Only changes how a finished world is displayed; the folder and save path are untouched. */
@Mixin(LevelSummary.class)
public abstract class LevelSummaryMixin {
    private static final String[] MSGS = {
            "DONT", "DON'T LOAD ME", "YOU BROUGHT IT BACK", "WHY DID YOU RESTART?", "I CAN SEE YOU",
            "PLEASE DON'T LOAD", "IT'S NOT YOUR WORLD", "YOU ALREADY LEFT",
            "you thought closing the game would help?", "I'M STILL HERE", "DON'T OPEN IT",
            "THANK YOU FOR BRINGING US HOME"
    };
    private static long firstSeen = 0;

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void dont$name(CallbackInfoReturnable<String> cir) {
        if (DontClient.isEnded(((LevelSummary) (Object) this).getName())) cir.setReturnValue("dont");
    }

    @Inject(method = "getDetails", at = @At("HEAD"), cancellable = true)
    private void dont$details(CallbackInfoReturnable<Text> cir) {
        if (!DontClient.isEnded(((LevelSummary) (Object) this).getName())) return;
        long now = System.currentTimeMillis();
        if (firstSeen == 0 || now - firstSeen > 600000) firstSeen = now;
        long t = now - firstSeen;
        if (t < 4000) { cir.setReturnValue(Text.empty()); return; }       // first only the name "dont"
        Random r = new Random(t / 2200);                                   // word changes irregularly
        String msg = MSGS[r.nextInt(MSGS.length)];
        boolean visible = r.nextInt(4) != 0;                               // sometimes nothing at all
        if (!visible) { cir.setReturnValue(Text.empty()); return; }
        Random flick = new Random(now / 110);
        double p = flick.nextDouble();
        if (p < 0.08) { cir.setReturnValue(Text.literal(msg).formatted(Formatting.DARK_RED, Formatting.OBFUSCATED)); return; }
        if (p < 0.14) { cir.setReturnValue(Text.empty()); return; }
        StringBuilder sb = new StringBuilder();
        for (char ch : msg.toCharArray()) {
            sb.append(flick.nextInt(18) == 0 && ch != ' ' ? "#%&?!".charAt(flick.nextInt(5)) : ch);
        }
        cir.setReturnValue(Text.literal(sb.toString()).formatted(flick.nextInt(3) == 0 ? Formatting.WHITE : Formatting.RED));
    }
}
