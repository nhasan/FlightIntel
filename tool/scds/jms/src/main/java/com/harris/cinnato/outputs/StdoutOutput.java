package com.harris.cinnato.outputs;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdoutOutput extends Output {
    private static final Logger logger = LoggerFactory.getLogger("stdout");

    public StdoutOutput(Config config) {
        super(config);
    }

    @Override
    public void output(String message) {
        logger.info(this.convert(message));
    }
}
