package com.harris.cinnato.solace;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.harris.cinnato.outputs.Output;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.NamingException;

public abstract class SolaceConsumer implements MessageListener, ExceptionListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    final Config config;

    private Connection connection;
    private final String connectionName;
    private final Meter messageRate;
    private final Output reporter;

    /**
     * Constructor sets configuration items
     * @param config the consumer config properties
     */
    SolaceConsumer(Config config, MetricRegistry metrics, Output reporter) throws NamingException {
        this.messageRate = metrics.meter("messages");
        this.config = config;
        this.reporter = reporter;
        this.connectionName = "[ " + config.getString("providerUrl") + " -- " + config.getString("queue") + " ]";
    }

    protected abstract Connection getConnection () throws Exception;

    protected abstract Queue getQueue (Session session) throws Exception;

    /**
     * Opens a connection and creates a message consumer for receiving messages
     */
    public void connect() throws Exception {
        try {
            this.connection = this.getConnection();
            this.connection.setExceptionListener(this);
            Session session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue theQueue = this.getQueue(session);
            MessageConsumer consumer = session.createConsumer(theQueue);
            consumer.setMessageListener(this);
            this.start();
        } catch (Exception e) {
            this.logger.error("Failed to create the connection: " + this.connectionName, e);
            throw e;
        }
    }

    /**
     * Start receiving messages
     */
    private synchronized void start() throws Exception {
        try {
            this.connection.start();
        } catch (Exception e) {
            this.logger.error("Failed to start the connection: " + this.connectionName, e);
            throw e;
        }
    }

    /**
     * Sends received message to the broker. Includes original JMS properties in addition to the message body.
     * @param message The incoming message.
     */
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof BytesMessage) {
                BytesMessage byteMessage = (BytesMessage) message;
                byte[] content = new byte[(int) byteMessage.getBodyLength()];
                byteMessage.readBytes(content);
                this.reporter.output(new String(content));
            } else if (message instanceof TextMessage) {
                this.reporter.output(((TextMessage) message).getText());
            } else {
                this.logger.error("Received incorrect message type");
            }

            this.messageRate.mark();

        } catch (Exception e) {
            this.logger.error("Error receiving message", e);
        }
    }

    @Override
    public void onException(JMSException e) {
        this.logger.error("Consumer Exception", e);
        System.exit(-1);
    }
}
