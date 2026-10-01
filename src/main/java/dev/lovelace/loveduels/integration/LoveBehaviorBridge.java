package dev.lovelace.loveduels.integration;

import dev.lovelace.lovecore.api.LoveCore;
import dev.lovelace.lovecore.api.social.ReputationOracle;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Bridge to LoveBehavior and LoveCore social services.
 * Изгой = ReputationOracle.Tier.OUTCAST or politenessLevel &lt;= 0.
 */
public final class LoveBehaviorBridge {

    private static final String API_CLASS_NAME = "me.lovelace.lovebehavior.api.LoveBehaviorAPI";
    private final Logger logger;

    public LoveBehaviorBridge(Logger logger) {
        this.logger = logger;
    }

    public boolean isAvailable() {
        return LoveCore.service(ReputationOracle.class).isPresent()
                || findApiInstance() != null;
    }

    public boolean isOutcast(UUID playerId) {
        if (playerId == null) return false;

        Optional<ReputationOracle> oracle = LoveCore.service(ReputationOracle.class);
        if (oracle.isPresent()) {
            try {
                return oracle.get().tier(playerId) == ReputationOracle.Tier.OUTCAST;
            } catch (Throwable t) {
                logger.fine("ReputationOracle.tier failed: " + t.getMessage());
            }
        }

        Object api = findApiInstance();
        if (api != null) {
            try {
                for (Method m : api.getClass().getMethods()) {
                    if (m.getName().equalsIgnoreCase("getPolitenessLevel") && m.getParameterCount() == 1) {
                        Object r = m.invoke(api, playerId);
                        if (r instanceof Number n) return n.intValue() <= 0;
                        if (r instanceof String s) {
                            String lower = s.toLowerCase(Locale.ROOT);
                            return lower.contains("изгой") || lower.contains("outcast") || lower.contains("terrible");
                        }
                    }
                    if (m.getName().equalsIgnoreCase("getPolitenessPoints") && m.getParameterCount() == 1) {
                        Object r = m.invoke(api, playerId);
                        if (r instanceof Number n) return n.intValue() <= 0;
                    }
                }
            } catch (Exception e) {
                logger.fine("LoveBehavior outcast check failed: " + e.getMessage());
            }
        }
        return false;
    }

    public int getPolitenessLevel(UUID playerId) {
        if (playerId == null) return 3;
        Object api = findApiInstance();
        if (api != null) {
            try {
                for (Method m : api.getClass().getMethods()) {
                    if (m.getName().equalsIgnoreCase("getPolitenessLevel") && m.getParameterCount() == 1) {
                        Object r = m.invoke(api, playerId);
                        if (r instanceof Number n) return n.intValue();
                    }
                }
            } catch (Throwable t) {
                return 3;
            }
        }
        return 3;
    }

    public boolean punishFleeing(UUID player, int penaltyPoints) {
        if (player == null || penaltyPoints == 0) return false;

        Optional<ReputationOracle> oracle = LoveCore.service(ReputationOracle.class);
        if (oracle.isPresent()) {
            try {
                oracle.get().modify(player, -Math.abs(penaltyPoints));
                return true;
            } catch (Throwable t) {
                logger.warning("ReputationOracle.modify failed for " + player + ": " + t.getMessage());
            }
        }

        Object api = findApiInstance();
        if (api == null) return false;

        try {
            Class<?> cls = api.getClass();
            for (Method m : cls.getMethods()) {
                if ((m.getName().equalsIgnoreCase("addPolitenessPoints")
                        || m.getName().equalsIgnoreCase("modifyPolitenessPoints"))
                        && m.getParameterCount() == 2
                        && m.getParameterTypes()[0].equals(UUID.class)) {
                    m.invoke(api, player, -Math.abs(penaltyPoints));
                    return true;
                }
            }
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
            logger.fine("LoveBehavior service lookup: " + t.getMessage());
        }
        return null;
    }
}
