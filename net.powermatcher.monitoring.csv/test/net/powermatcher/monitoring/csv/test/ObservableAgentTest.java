package net.powermatcher.monitoring.csv.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import net.powermatcher.api.data.MarketBasis;
import net.powermatcher.api.data.Price;
import net.powermatcher.api.messages.PriceUpdate;
import net.powermatcher.api.monitoring.ObservableAgent;
import net.powermatcher.api.monitoring.events.AgentEvent;
import net.powermatcher.api.monitoring.events.IncomingPriceUpdateEvent;
import net.powermatcher.mock.MockDeviceAgent;
import net.powermatcher.mock.MockObserver;

import org.junit.Test;

/**
 * JUnit tests for the {@link ObservableAgent} class.
 *
 * @author FAN
 * @version 2.1
 */
public class ObservableAgentTest {

    @Test
    public void oneObserverTest() {
        MockObserver observer = new MockObserver();

        MockDeviceAgent observable = new MockDeviceAgent("agentId", "matcherId");
        observable.addObserver(observer);
        observable.publishEvent(createDummyEvent());

        assertTrue(observer.hasReceivedEvent());
    }

    @Test
    public void twoObserversTest() {
        MockObserver observer1 = new MockObserver();
        MockObserver observer2 = new MockObserver();

        MockDeviceAgent observable = new MockDeviceAgent("agentId", "matcherId");
        observable.addObserver(observer1);
        observable.addObserver(observer2);
        observable.publishEvent(createDummyEvent());

        assertTrue(observer1.hasReceivedEvent());
        assertTrue(observer2.hasReceivedEvent());
    }

    @Test
    public void removeObserversTest() {
        MockObserver observer1 = new MockObserver();
        MockObserver observer2 = new MockObserver();

        MockDeviceAgent observable = new MockDeviceAgent("agentId", "matcherId");
        observable.addObserver(observer1);
        observable.addObserver(observer2);
        observable.removeObserver(observer2);
        observable.publishEvent(createDummyEvent());

        assertTrue(observer1.hasReceivedEvent());
        assertFalse(observer2.hasReceivedEvent());
    }

    @Test
    public void duplicateRemoveObserversTest() {
        MockObserver observer1 = new MockObserver();

        MockDeviceAgent observable = new MockDeviceAgent("agentId", "matcherId");
        observable.addObserver(observer1);
        observable.removeObserver(observer1);
        observable.removeObserver(observer1);
    }

    @Test
    public void noObserversTest() {
        MockDeviceAgent observable = new MockDeviceAgent("agentId", "matcherId");

        MockObserver observer1 = new MockObserver();
        observable.addObserver(observer1);
        observable.removeObserver(observer1);

        observable.publishEvent(createDummyEvent());
        assertFalse(observer1.hasReceivedEvent());
    }

    private AgentEvent createDummyEvent() {
        MarketBasis marketBasis = new MarketBasis("Electicity", "EUR", 10, 0.0, 100.0);
        PriceUpdate priceUpdate = new PriceUpdate(new Price(marketBasis, 0), 0);
        return new IncomingPriceUpdateEvent("defaultCluster", "agent1", "sessionId", new Date(), priceUpdate);
    }
}
