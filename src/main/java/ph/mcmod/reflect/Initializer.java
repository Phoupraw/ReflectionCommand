package ph.mcmod.reflect;

import net.fabricmc.api.ModInitializer;

public class Initializer implements ModInitializer {
	@Override
	public void onInitialize() {
		ReflectCommand.loadClass();
	}
}
