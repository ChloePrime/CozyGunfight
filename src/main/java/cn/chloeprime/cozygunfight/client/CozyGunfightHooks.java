package cn.chloeprime.cozygunfight.client;

import cn.chloeprime.cozygunfight.mixin.client.MeleeKeyAccessor;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.ShootKey;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.InputExtraCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class CozyGunfightHooks {
    public static boolean isMeleeWeapon(CommonGunIndex index) {
        var range = index.getBulletData().getSpeed() * index.getBulletData().getLifeSecond() * 20F;
        return range <= 4;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.MouseButton.Post event) {
        var gun = Optional.ofNullable(Minecraft.getInstance().player)
                .filter(IGun::mainhandHoldGun)
                .map(LivingEntity::getMainHandItem)
                .orElse(ItemStack.EMPTY);
        if (gun.isEmpty()) {
            return;
        }
        var kun = IGun.getIGunOrNull(gun);
        if (kun == null) {
            return;
        }
        var index = TimelessAPI.getCommonGunIndex(kun.getGunId(gun)).orElse(null);
        if (index == null) {
            return;
        }

        if (InputExtraCheck.isInGame() && event.getAction() == 1 && ShootKey.SHOOT_KEY.matchesMouse(event.getButton())) {
            var totalAmmo = getTotalAmmo(kun, gun, index);
            // 左键近战
            if (totalAmmo == 0 && isMeleeWeapon(index)) {
                MeleeKeyAccessor.invokeDoMeleeLogic();
                return;
            }
            // 左键装弹
            var player = Minecraft.getInstance().player;
            if (totalAmmo == 0 && player != null && !player.isSpectator()) {
                IClientPlayerGunOperator.fromLocalPlayer(player).reload();
            }
        }
    }

    public static int getTotalAmmo(IGun gunItem, ItemStack gunStack, CommonGunIndex index) {
        int mag = gunItem.getCurrentAmmoCount(gunStack);
        int barrel = index.getGunData().getBolt() == Bolt.OPEN_BOLT
                ? 0
                : gunItem.hasBulletInBarrel(gunStack) ? 1 : 0;
        return mag + barrel;
    }
}
