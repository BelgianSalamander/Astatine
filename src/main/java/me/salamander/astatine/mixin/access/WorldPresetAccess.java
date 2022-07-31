 package me.salamander.astatine.mixin.access;

 import net.minecraft.client.gui.screens.worldselection.WorldPreset;
 import org.spongepowered.asm.mixin.Mixin;
 import org.spongepowered.asm.mixin.gen.Accessor;

 import java.util.List;

 @Mixin(WorldPreset.class)
public interface WorldPresetAccess {
     @Accessor
     static List<WorldPreset> getPRESETS() {
         throw new Error("Mixin did not apply!");
     }
 }