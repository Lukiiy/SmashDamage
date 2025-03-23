package me.lukiiy.smashDamage;

import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;

public class EntitySize {
    public final double width;
    public final double height;

    public EntitySize(Entity entity, double yOffset) {
        double scale = 1;
        if (entity instanceof Attributable attributable) {
            AttributeInstance scaleInst = attributable.getAttribute(Attribute.SCALE);
            if (scaleInst != null) scale = scaleInst.getValue();
        }

        width = entity.getWidth() / 2 * scale;
        height = entity.getHeight() * scale + yOffset;
    }

    public EntitySize(Entity entity) {
        this(entity, 0);
    }
}
