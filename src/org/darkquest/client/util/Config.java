package org.darkquest.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.darkquest.client.mudclient;

public class Config {
  public static String SERVER_IP = "rsc.beefsec.com";
  public static String BOOBS = new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data";
  public static String CONF_DIR = new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data";
  public static String MEDIA_DIR = new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data";
  public static int SERVER_PORT = 43596, MOVIE_FPS = 5;
  public static long START_TIME = System.currentTimeMillis();


  public static final int CLIENT_VERSION = 53;

  /**
   * Loads the configuration file config.properties
   * @return Properties loaded from the configuration file
   */
  public static Properties loadConfig() {
    System.out.println("FUCK: " + BOOBS);
    File f = new File(CONF_DIR + File.separator + "config.properties");
    if(!f.exists())
      try {
        f.createNewFile();

      } catch (IOException e1) {
        System.out.println("Unable to create configuration file!");
      }

    Properties defaultProps = new Properties();
    try {
      defaultProps.load(new FileInputStream(CONF_DIR + File.separator + "config.properties"));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return defaultProps;
  }
  /**
   * Stores information into the properties + writes it to the configuration file
   * @param key
   * @param value
   */
  public static void storeConfig(String key, String value) {
    Properties config = mudclient.config;
    config.setProperty(key, value);
    try {
      FileOutputStream out = new FileOutputStream(CONF_DIR + File.separator + "config.properties");
      config.store(out, "DarkQuest configuration file");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
