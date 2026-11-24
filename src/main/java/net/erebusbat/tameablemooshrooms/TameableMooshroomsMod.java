package net.erebusbat.tameablemooshrooms;

import net.erebusbat.tameablemooshrooms.behavior.MooshroomDamageGuard;
import net.erebusbat.tameablemooshrooms.behavior.MooshroomInteractions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TameableMooshroomsMod implements ModInitializer {
	public static final String MOD_ID = "tameable_mooshrooms";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		UseEntityCallback.EVENT.register(MooshroomInteractions::interact);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(MooshroomDamageGuard::allowDamage);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
