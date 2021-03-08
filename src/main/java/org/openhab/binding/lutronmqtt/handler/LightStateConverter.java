package org.openhab.binding.lutronmqtt.handler;

import static org.openhab.binding.lutronmqtt.LutronMQTTBindingConstants.LUTRON_PROPERTY_LEVEL;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.openhab.binding.lutronmqtt.model.LutronDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LightStateConverter} is responsible for mapping Eclipse SmartHome
 * types to jue types and vice versa.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Oliver Libutzki - Adjustments
 * @author Kai Kreuzer - made code static
 * @author Andre Fuechsel - added method for brightness
 * @author Yordan Zhelev - added method for alert
 * @author Denis Dudnik - switched to internally integrated source of Jue library, minor code cleanup
 *
 */
public class LightStateConverter {

    private static final double BRIGHTNESS_FACTOR = 1.0;

    private static final int DIM_STEPSIZE = 65535 / 25; // 4 %

    protected static Logger log = LoggerFactory.getLogger(LightStateConverter.class);

    /**
     * Transforms the given {@link OnOffType} into a light state containing the
     * 'on' value.
     *
     * @param onOffType
     *            on or off state
     * @return light state containing the 'on' value
     */
    public static int toOnOffLightState(OnOffType onOffType) {
        int f = 0;
        if (onOffType == OnOffType.ON)
            f = 65535;
        return f;
    }

    public static int toPercentLightState(PercentType percentType) {
        int f = 0;
        if (PercentType.ZERO.equals(percentType))
            f = 0;
        else if (PercentType.HUNDRED.equals(percentType))
            f = 65535;
        else {
            f = (int) Math.round(percentType.floatValue() * 65535 / 100);
        }
        return f;
    }

    public static Map<String, Object> toLightState(OnOffType onOffType, LutronDevice device) {
        int level = toOnOffLightState(onOffType);
        Map<String, Object> m = makeGoToLevelCommand(level, device);
        return m;
    }

    public static Map<String, Object> makeGoToLevelCommand(int level, LutronDevice device) {
        Map<String, Object> a = new HashMap<>();
        log.warn("device: " + device);
        a.put("ObjectId", device.getObjectId());
        a.put("ObjectType", 15);
        a.put("Fade", 0);
        a.put("Delay", 0);
        a.put("Level", level);
        Map<String, Object> m = new HashMap<>();
        m.put("cmd", "GoToLevel");
        m.put("args", a);

        return m;
    }

    /**
     * Transforms the given {@link PercentType} into a light state containing
     * the brightness and the 'on' value represented by {@link PercentType}.
     *
     * @param percentType
     *            brightness represented as {@link PercentType}
     * @return light state containing the brightness and the 'on' value
     */
    public static Map<String, Object> toLightState(PercentType percentType, LutronDevice device) {
        int level = toPercentLightState(percentType);

        Map<String, Object> m = makeGoToLevelCommand(level, device);

        return m;
    }

    public static Map<String, Object> toLightState(IncreaseDecreaseType increaseDecreaseType, LutronDevice device) {
        int level = toAdjustedBrightness(increaseDecreaseType, device.getProperty(LUTRON_PROPERTY_LEVEL));

        Map<String, Object> m = makeGoToLevelCommand(level, device);

        return m;
    }

    public static Map<String, Object> toLightState(UpDownType upDownType, LutronDevice device) {
        int level = toAdjustedBrightness(upDownType, device.getProperty(LUTRON_PROPERTY_LEVEL));

        Map<String, Object> m = makeGoToLevelCommand(level, device);

        return m;
    }

    /**
     * Adjusts the given brightness using the {@link IncreaseDecreaseType} and
     * returns the updated value.
     *
     * @param command
     *            The {@link IncreaseDecreaseType} to be used
     * @param currentBrightness
     *            The current brightness
     * @return The adjusted brightness value
     */
    public static int toAdjustedBrightness(IncreaseDecreaseType command, int currentBrightness) {
        int newBrightness;
        if (command == IncreaseDecreaseType.DECREASE) {
            newBrightness = Math.max(currentBrightness - DIM_STEPSIZE, 0);
        } else {
            newBrightness = Math.min(currentBrightness + DIM_STEPSIZE, 65535);
        }
        return newBrightness;
    }

    /**
     * Adjusts the given brightness using the {@link IncreaseDecreaseType} and
     * returns the updated value.
     *
     * @param command
     *            The {@link UpDownType} to be used
     * @param currentBrightness
     *            The current brightness
     * @return The adjusted brightness value
     */
    public static int toAdjustedBrightness(UpDownType command, int currentBrightness) {
        int newBrightness;
        if (command == UpDownType.DOWN) {
            newBrightness = Math.max(currentBrightness - DIM_STEPSIZE, 0);
        } else {
            newBrightness = Math.min(currentBrightness + DIM_STEPSIZE, 65535);
        }
        return newBrightness;
    }

    /**
     * Transforms Luton device state into {@link PercentType} representing
     * the brightness.
     *
     * @param device
     *            lutron device
     * @return percent type representing the brightness
     */
    public static PercentType toBrightnessPercentType(LutronDevice device) {
        int percent = (int) Math.round(device.getProperty(LUTRON_PROPERTY_LEVEL) / (65535 / 100));
        if (log.isTraceEnabled()) {
            log.trace("Converting " + device.getProperty(LUTRON_PROPERTY_LEVEL) + " -> " + percent + " -> "
                    + new PercentType(restrictToBounds(percent)));
        }
        return new PercentType(restrictToBounds(percent));
    }

    private static int restrictToBounds(int percentValue) {
        if (percentValue < 0) {
            return 0;
        } else if (percentValue > 100) {
            return 100;
        }
        return percentValue;
    }
}
