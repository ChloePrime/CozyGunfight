package cn.chloeprime.cozygunfight.mixin.client;

import com.tacz.guns.client.input.MeleeKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MeleeKey.class, remap = false)
public interface MeleeKeyAccessor {
    @Invoker static void invokeDoMeleeLogic() {
        throw new AbstractMethodError();
    }
}
