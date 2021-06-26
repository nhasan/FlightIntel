package com.harris.cinnato.solace;

import com.codahale.metrics.MetricRegistry;
import com.harris.cinnato.outputs.Output;
import com.typesafe.config.Config;

import org.apache.qpid.jms.JmsConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.NamingException;

public class AMQPConsumer extends SolaceConsumer {
    public AMQPConsumer (Config config, MetricRegistry metrics, Output reporter) throws NamingException {
        super(config, metrics, reporter);
    }

    @Override
    protected Connection getConnection() throws JMSException {
        String username = this.config.getString("username");
        String password = this.config.getString("password");
        String providerUrl = this.config.getString("providerUrl");
        return new JmsConnectionFactory(username, password, providerUrl).createConnection();
    }

    @Override
    protected Queue getQueue(Session session) throws Exception {
        return session.createQueue(this.config.getString("queue"));
    }
}
