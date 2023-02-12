package com.shinybunny.villagerhelper.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.minecraft.command.argument.RegistryEntryArgumentType.INVALID_TYPE_EXCEPTION;

public class LookingForCommand {
    public static final DynamicCommandExceptionType ENCHANTMENT_NOT_AVAILABLE = new DynamicCommandExceptionType(ench -> Text.translatable(((Enchantment) ench).getTranslationKey()).append(Text.literal(" cannot be found in villager offers!")));
    public static final DynamicCommandExceptionType ENCHANTMENT_ALREADY_TRACKED = new DynamicCommandExceptionType(ench -> Text.translatable(((Enchantment) ench).getTranslationKey()).append(Text.literal(" is already being looked for.")));
    public static final DynamicCommandExceptionType ENCHANTMENT_NOT_TRACKED = new DynamicCommandExceptionType(ench -> Text.translatable(((Enchantment) ench).getTranslationKey()).append(Text.literal(" is not being looked for.")));
    public static final Set<Enchantment> lookingFor = new HashSet<>();
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("lookingfor")
                .then(literal("add")
                        .then(argument("enchantment", RegistryEntryArgumentType.registryEntry(registryAccess, RegistryKeys.ENCHANTMENT))
                                .executes(ctx -> addLookingForEnchantment(ctx.getSource(), getEnchantment(ctx, "enchantment")))
                        )
                )
                .then(literal("remove")
                        .then(argument("enchantment", RegistryEntryArgumentType.registryEntry(registryAccess, RegistryKeys.ENCHANTMENT))
                                .suggests((ctx, builder) -> CommandSource.suggestFromIdentifier(lookingFor, builder, Registries.ENCHANTMENT::getId, ench -> Text.translatable(ench.getTranslationKey())))
                                .executes(ctx -> removeLookingForEnchantment(ctx.getSource(), getEnchantment(ctx, "enchantment")))
                        )
                )
                .then(literal("clear")
                        .executes(ctx -> {
                            lookingFor.clear();
                            ctx.getSource().sendFeedback(Text.literal("Cleared enchantments being looked for"));
                            return 1;
                        })
                )
                .then(literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("Enchantments being looked for:"));
                            for (Enchantment ench : lookingFor) {
                                ctx.getSource().sendFeedback(ench.getName(ench.getMaxLevel()));
                            }
                            return lookingFor.size();
                        })
                )
        );
    }

    private static int removeLookingForEnchantment(FabricClientCommandSource source, Enchantment enchantment) throws CommandSyntaxException {
        if (lookingFor.remove(enchantment)) {
            source.sendFeedback(Text.literal("Stopped looking for ").append(enchantment.getName(enchantment.getMaxLevel())));
            return 1;
        }
        throw ENCHANTMENT_NOT_TRACKED.create(enchantment);
    }

    private static int addLookingForEnchantment(FabricClientCommandSource source, Enchantment enchantment) throws CommandSyntaxException {
        if (!enchantment.isAvailableForEnchantedBookOffer()) throw ENCHANTMENT_NOT_AVAILABLE.create(enchantment);
        if (lookingFor.add(enchantment)) {
            source.sendFeedback(Text.literal("Looking for ").append(enchantment.getName(enchantment.getMaxLevel())));
        } else {
            throw ENCHANTMENT_ALREADY_TRACKED.create(enchantment);
        }
        return 1;
    }

    public static Enchantment getEnchantment(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        RegistryEntry.Reference<?> reference = context.getArgument(name, RegistryEntry.Reference.class);
        RegistryKey<?> registryKey = reference.registryKey();
        if (registryKey.isOf(RegistryKeys.ENCHANTMENT)) {
            return (Enchantment) reference.value();
        }
        throw INVALID_TYPE_EXCEPTION.create(registryKey.getValue(), registryKey.getRegistry(), RegistryKeys.ENCHANTMENT.getValue());
    }

}
