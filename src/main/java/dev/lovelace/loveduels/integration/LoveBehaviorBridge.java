package dev.lovelace.loveduels.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reflection and ServicesManager bridge to LoveBehavior.
 * Modifies politeness/reputation points when players flee from combat or abuse duels.
 */
public final class LoveBehaviorBridge {

    private static final String API_CLASS_NAME = "me.lovelace.lovebehavior.api.LoveBehaviorAPI";
    private final Logger logger;

    public LoveBehaviorBridge(Logger logger) {
        this.logger = logger;
    }

    public boolean isAvailable() {
        return findApiInstance() != null;
    }

    private Object findApiInstance() {
        try {
            ServicesManager sm = Bukkit.getServicesManager();
            for (Class<?> svc : sm.getKnownServices()) {
                if (svc.getName().equals(API_CLASS_NAME)) {
                    var reg = sm.getRegistration(svc);
                    return (reg != null) ? reg.getProvider() : null;
                }
            }
        } catch (Throwable t) {
            logger.fine("LoveBehavior service lookup error: " + t.getMessage());
        }
        return null;
    }

    /**
     * Deducts politeness/reputation points for dishonorable conduct (e.g. -150 points).
     */
    public boolean punishFleeing(UUID player, int penaltyPoints) {
        Object api = findApiInstance();
        if (api == null || player == null) return false;

        try {
            Class<?> cls = api.getClass();

            // Try modify/add method with negative delta
            for (Method m : cls.getMethods()) {
                if ((m.getName().equalsIgnoreCase("addPolitenessPoints") ||
                        m.getName().equalsIgnoreCase("modifyPolitenessPoints")) &&
                        m.getParameterCount() == 2 &&
                        m.getParameterTypes()[0].equals(UUID.class)) {
                    m.invoke(api, player, -Math.abs(penaltyPoints));
                    return true;
                }
            }

            // Fallback: get and set
            Method getMethod = null;
            Method setMethod = null;
            for (Method m : cls.getMethods()) {
                if (m.getName().equalsIgnoreCase("getPolitenessPoints") && m.getParameterCount() == 1) {
                    getMethod = m;
                } else if (m.getName().equalsIgnoreCase("setPolitenessPoints") && m.getParameterCount() == 2) {
                    setMethod = m;
                }
            }

            if (getMethod != null && setMethod != null) {
                int current = (int) getMethod.invoke(api, player);
                int updated = Math.max(0, current - Math.abs(penaltyPoints));
                setMethod.invoke(api, player, updated);
                return true;
            }
        } catch (Exception e) {
            logger.warning("Failed to deduct LoveBehavior points for " + player + ": " + e.getMessage());
        }
        return false;
    }
}
