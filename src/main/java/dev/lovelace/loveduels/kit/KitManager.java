package dev.lovelace.loveduels.kit;

import java.util.Collection;
import java.util.Optional;

public interface KitManager {

    Optional<Kit> getKit(String id);

    Collection<Kit> getAllKits();

    void registerKit(Kit kit);

    boolean deleteKit(String id);

    void loadKits();

    void saveKits();
}
