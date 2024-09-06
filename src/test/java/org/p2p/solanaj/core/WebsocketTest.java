package org.p2p.solanaj.core;

import org.junit.Before;
import org.junit.Test;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.ws.SubscriptionWebSocketClient;
import org.p2p.solanaj.ws.listeners.NotificationEventListener;
import java.util.concurrent.CountDownLatch;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class WebsocketTest {

    private SubscriptionWebSocketClient devnetClient;
    private static final Logger LOGGER = Logger.getLogger(WebsocketTest.class.getName());
    private static final String POPULAR_ACCOUNT = "SysvarC1ock11111111111111111111111111111111";

    @Before
    public void setUp() {
        devnetClient = SubscriptionWebSocketClient.getInstance(Cluster.DEVNET.getEndpoint());
    }

    @Test
    public void testAccountSubscribe() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        
        devnetClient.accountSubscribe(POPULAR_ACCOUNT, (NotificationEventListener) data -> {
            LOGGER.info("Received notification: " + data);
            future.complete((Map<String, Object>) data);
            latch.countDown(); // Count down the latch
        });

        // Set a timeout for the test
        if (!latch.await(30, TimeUnit.SECONDS)) {
            fail("Test timed out waiting for notification");
        }

        Map<String, Object> result = future.get(30, TimeUnit.SECONDS);
        assertNotNull("Notification should not be null", result);
        assertTrue("Notification should contain 'value'", result.containsKey("value"));
        Map<String, Object> value = (Map<String, Object>) result.get("value");
        assertTrue("Value should contain 'lamports'", value.containsKey("lamports"));
    }

    @Test
    public void testMultipleSubscriptions() throws Exception {
        CountDownLatch latch = new CountDownLatch(3); // Assuming we expect 3 notifications
        CompletableFuture<Map<String, Object>> future1 = new CompletableFuture<>();
        CompletableFuture<Map<String, Object>> future2 = new CompletableFuture<>();
        
        devnetClient.accountSubscribe(POPULAR_ACCOUNT, (NotificationEventListener) data -> {
            LOGGER.info("Received notification for subscription 1: " + data);
            future1.complete((Map<String, Object>) data);
            latch.countDown(); // Count down the latch
        });

        devnetClient.accountSubscribe(POPULAR_ACCOUNT, (NotificationEventListener) data -> {
            LOGGER.info("Received notification for subscription 2: " + data);
            future2.complete((Map<String, Object>) data);
            latch.countDown(); // Count down the latch
        });

        // Set a timeout for the test
        if (!latch.await(30, TimeUnit.SECONDS)) {
            fail("Test timed out waiting for notifications");
        }

        CompletableFuture.allOf(future1, future2).join(); // Wait for all to complete

        Map<String, Object> result1 = future1.get(30, TimeUnit.SECONDS);
        Map<String, Object> result2 = future2.get(30, TimeUnit.SECONDS);

        assertNotNull("Notification 1 should not be null", result1);
        assertNotNull("Notification 2 should not be null", result2);
        assertTrue("Notification 1 should contain 'value'", result1.containsKey("value"));
        assertTrue("Notification 2 should contain 'value'", result2.containsKey("value"));
        Map<String, Object> value1 = (Map<String, Object>) result1.get("value");
        Map<String, Object> value2 = (Map<String, Object>) result2.get("value");
        assertTrue("Value 1 should contain 'lamports'", value1.containsKey("lamports"));
        assertTrue("Value 2 should contain 'lamports'", value2.containsKey("lamports"));
    }
}
