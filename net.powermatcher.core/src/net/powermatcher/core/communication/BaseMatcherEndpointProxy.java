package net.powermatcher.core.communication;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.powermatcher.api.AgentEndpoint;
import net.powermatcher.api.MatcherEndpoint;
import net.powermatcher.api.Session;
import net.powermatcher.api.data.Bid;
import net.powermatcher.api.data.MarketBasis;
import net.powermatcher.api.messages.PriceUpdate;
import net.powermatcher.core.BaseAgent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for remote agents. This is the "sending end" of a remote communication pair.
 *
 * @author FAN
 * @version 2.0
 */
public abstract class BaseMatcherEndpointProxy
    extends BaseAgent
    implements MatcherEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMatcherEndpointProxy.class);

    /**
     * The {@link Session} to the local {@link AgentEndpoint}.
     */
    private Session localSession;

    /**
     * Scheduler that can schedule commands to run after a given delay, or to execute periodically.
     */
    private ScheduledExecutorService scheduler;

    /**
     * A delayed result-bearing action that can be cancelled.
     */
    private ScheduledFuture<?> scheduledFuture;

    /**
     * This method will be called by the annotated Activate() method of the subclasses.
     *
     * @param reconnectTimeout
     *            Time in seconds between connections to the {@link AgentEndpointProxy}.
     */
    protected void baseActivate(int reconnectTimeout) {
        // Start connector thread
        scheduledFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                connectRemote();
            }
        }, 0, reconnectTimeout, TimeUnit.SECONDS);
    }

    /**
     * This method will be called by the annotated Deactivate() method of the subclasses.
     */
    protected void baseDeactivate() {
        // Stop connector thread
        scheduledFuture.cancel(false);

        // Disconnect the agent
        disconnectRemote();
    }

    /**
     * Creates a {@link Session} and a connection to the remote {@link AgentEndpointProxy}.
     *
     * @return <code>true</code> if the connection was created successfully.
     */
    public abstract boolean connectRemote();

    /**
     * @return <code>true</code> if {@link Session} and the connection to the remote {@link AgentEndpointProxy} is
     *         active.
     */
    public abstract boolean isRemoteConnected();

    /**
     * Closes the {@link Session} and the connection to the remote {@link AgentEndpointProxy}.
     *
     * @return <code>true</code> if the disconnecting was successful.
     */
    public abstract boolean disconnectRemote();

    /**
     * @return <code>true</code> isf the local {@link Session} is not <code>null</code>.
     */
    public boolean isLocalConnected() {
        return localSession != null;
    }

    /**
     * This method sets the {@link MarketBasis} of the local {@link Session}. It is called when the
     * {@link AgentEndpointProxy} communicates with this instance.
     *
     * @param marketBasis
     *            the new {@link MarketBasis}
     */
    public void updateRemoteMarketBasis(MarketBasis marketBasis) {
        // Sync marketbasis with local session, for new connections
        if (isLocalConnected() && localSession.getMarketBasis() == null) {
            localSession.setMarketBasis(marketBasis);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean connectToAgent(Session session) {
        localSession = session;
        LOGGER.info("Agent connected with session [{}]", session.getSessionId());

        // Initiate a remote connection
        connectRemote();

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void agentEndpointDisconnected(Session session) {
        // Disconnect local agent
        localSession = null;
        LOGGER.info("Agent disconnected with session [{}]", session.getSessionId());

        // Disconnect remote agent
        disconnectRemote();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleBidUpdate(Session session, Bid newBid) {
        if (localSession != session) {
            LOGGER.warn("Received bid update for unknown session.");
            return;
        }

        if (!isRemoteConnected()) {
            LOGGER.warn("Received bid update, but remote agent is not connected.");
            return;
        }

        if (localSession.getMarketBasis() == null) {
            LOGGER.info("Skip bid update to local agent, no marketbasis available.");
            return;
        }

        // Relay bid to remote agent
        updateBidRemote(newBid);
    }

    /**
     * Sends the {@link Bid} it receives through from the local {@link AgentEndpoint} to the remote
     * {@link AgentEndpoint} through he {@link AgentEndpointProxy}.
     *
     * @param newBid
     *            the new {@link Bid} sent by the {@link AgentEndpointProxy}.
     */
    protected abstract void updateBidRemote(Bid newBid);

    /**
     * Sends the {@link PriceUpdate} it receives from the remote {@link AgentEndpoint} through the {@link AgentEndpoint}
     * to the local {@link AgentEndpoint} through the local {@link Session}.
     *
     * @param priceUpdate
     *            the new {@link PriceUpdate}
     */
    public void updateLocalPrice(PriceUpdate priceUpdate) {
        if (!isLocalConnected()) {
            LOGGER.info("Skip price update to local agent, not connected.");
            return;
        }

        if (localSession.getMarketBasis() == null) {
            LOGGER.info("Skip price update to local agent, no marketbasis available.");
            return;
        }

        localSession.updatePrice(priceUpdate);
    }

}