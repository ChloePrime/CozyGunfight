package cn.chloeprime.cozygunfight.client;

import cn.chloeprime.cozygunfight.mixin.client.MeleeKeyAccessor;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.input.ShootKey;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.InputExtraCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Optional;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class CozyGunfightHooks {
    public static boolean isMeleeWeapon(CommonGunIndex index) {
        var range = index.getBulletData().getSpeed() * index.getBulletData().getLifeSecond() * 20F;
        return range <= 4;
    }

    public static boolean canDoMeleeAttack(IGun gunItem, ItemStack gunStack, CommonGunIndex index) {
        if (index.getGunData().getMeleeData().getDefaultMeleeData() != null) {
            return true;
        }
        return Arrays.stream(MELEE_ABLE_ATTACHMENT_TYPES)
                .map(attachmentType -> gunItem.getAttachmentId(gunStack, attachmentType))
                .filter(attachmentId -> !DefaultAssets.isEmptyAttachmentId(attachmentId))
                .flatMap(attachmentId -> TimelessAPI.getCommonAttachmentIndex(attachmentId).stream())
                .anyMatch(attachment -> attachment.getData().getMeleeData() != null);
    }

    private static final AttachmentType[] MELEE_ABLE_ATTACHMENT_TYPES = {AttachmentType.MUZZLE, AttachmentType.STOCK};

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKeyInput(InputEvent.MouseButton.Pre event) {
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
            var player = Minecraft.getInstance().player;

            // 左键装弹
            if (!isMeleeWeapon(index) && totalAmmo == 0 && player != null && !player.isSpectator()) {
                // 如果可以装弹则装弹
                var canReload = (!IGunOperator.fromLivingEntity(player).needCheckAmmo())
                        || (!(kun instanceof AbstractGunItem agi))
                        || agi.canReload(player, gun);
                if (canReload) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).reload();
                    return;
                }
                // 否则执行近战
            }
            // 左键近战
            if (totalAmmo == 0 && canDoMeleeAttack(kun, gun, index)) {
                MeleeKeyAccessor.invokeDoMeleeLogic();
                event.setCanceled(true);
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
