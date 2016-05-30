package net.aimeizi.kafka;

import org.apache.log4j.Logger;

public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException {

//        for (int i = 20; i < 25; i++) {
//            LOGGER.info("This is Message [" + i + "] from log4j producer");
//            Thread.sleep(1000);
//        }

        for (int i = 20; i < 40; i++) {
            LOGGER.info("Info [" + i + "]");
            Thread.sleep(1000);
        }

//        LOGGER.debug("Debug Message.");
//        LOGGER.info("Info Message.");
//        LOGGER.warn("Warn Message.");
//        LOGGER.error("Error Message.");
//        LOGGER.fatal("Fatal Message.");
    }
}
