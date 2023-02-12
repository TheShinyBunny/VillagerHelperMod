package com.shinybunny.villagerhelper.mixin;

import com.shinybunny.villagerhelper.client.LookingForCommand;
import com.shinybunny.villagerhelper.client.VillagerHelperClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getDataTracker()Lnet/minecraft/entity/data/DataTracker;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onTrackedDataUpdate(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci, Entity entity) {
        if (VillagerHelperClient.enabled && entity instanceof VillagerEntity villager) {
            for (DataTracker.SerializedEntry<?> entry : packet.trackedValues()) {
                if (entry.handler() == TrackedDataHandlerRegistry.VILLAGER_DATA) {
                    if (villager.getVillagerData().getProfession() != VillagerProfession.LIBRARIAN && ((VillagerData) entry.value()).getProfession() == VillagerProfession.LIBRARIAN) {
                        VillagerHelperClient.awaitingVillagerScreen = true;
                        client.interactionManager.interactEntity(client.player, villager, Hand.MAIN_HAND);
                    }
                    break;
                }
            }
        }
    }

    @Inject(method = "onSetTradeOffers", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/MerchantScreenHandler;setCanRefreshTrades(Z)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onGetOffers(SetTradeOffersS2CPacket packet, CallbackInfo ci, ScreenHandler screenHandler, MerchantScreenHandler merchantScreenHandler) {
        if (VillagerHelperClient.awaitingVillagerScreen) {
            VillagerHelperClient.awaitingVillagerScreen = false;
            boolean foundBook = false;
            for (TradeOffer offer : merchantScreenHandler.getRecipes()) {
                if (offer.getSellItem().isOf(Items.ENCHANTED_BOOK)) {
                    Map<Enchantment, Integer> enchs = EnchantmentHelper.get(offer.copySellItem());
                    for (Map.Entry<Enchantment, Integer> ench : enchs.entrySet()) {
                        if (LookingForCommand.lookingFor.isEmpty() || LookingForCommand.lookingFor.contains(ench.getKey())) {
                            foundBook = true;
                            int minPrice = 2 + 3 * ench.getValue();
                            int maxPrice = Math.max(6 + ench.getValue() * 13, 64);
                            if (ench.getKey().isTreasure()) {
                                minPrice *= 2;
                                maxPrice *= 2;
                            }
                            int actualPrice = offer.getAdjustedFirstBuyItem().getCount();
                            client.inGameHud.getChatHud().addMessage(Text.literal("").append(ench.getKey().getName(ench.getValue()))
                                    .append(Text.literal(" for " + actualPrice + " emeralds (min: " + minPrice + ", max: " + maxPrice + ")")));
                        }
                    }
                }
            }
            if (!foundBook) {
                client.inGameHud.getChatHud().addMessage(Text.literal("No book offer!"));
            }
            client.player.closeHandledScreen();
        }
    }
    
}
