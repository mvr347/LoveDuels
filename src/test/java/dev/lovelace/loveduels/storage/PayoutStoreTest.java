package dev.lovelace.loveduels.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayoutStoreTest {

    @TempDir
    File dir;

    @Test
    void asyncWritesAreOrderedAndDrainedOnSynchronousMode() {
        Logger log = Logger.getAnonymousLogger();
        Database db = new Database(new File(dir, "t.db"), log);
        PayoutStore store = new PayoutStore(db, log);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();

        store.queue(a, 100);
        store.queue(a, 50);
        // hold then release must never be reordered, otherwise an orphan escrow row would refund twice
        store.holdEscrow(a, b, 10);
        store.releaseEscrow(a);
        store.enterSynchronousMode();

        assertEquals(0, store.recoverEscrow());
        assertEquals(150L, store.takePending(a));
        assertEquals(0L, store.takePending(a));

        // after the switch calls run inline and are durable immediately
        store.queue(b, 7);
        assertEquals(7L, store.takePending(b));
        db.close();
    }
}
