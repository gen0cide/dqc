package org.darkquest.client.util;

import com.thoughtworks.xstream.XStream;

import java.io.*;
import java.io.File;
import java.nio.channels.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PersistenceManager {
  private static final XStream xstream = new XStream();

  static {
    addAlias("NPCDef", "defs.NPCDef");
    addAlias("ItemDef", "defs.ItemDef");
    addAlias("TextureDef", "defs.extras.TextureDef");
    addAlias("AnimationDef", "defs.extras.AnimationDef");
    addAlias("ItemDropDef", "defs.extras.ItemDropDef");
    addAlias("SpellDef", "defs.SpellDef");
    addAlias("PrayerDef", "defs.PrayerDef");
    addAlias("TileDef", "defs.TileDef");
    addAlias("DoorDef", "defs.DoorDef");
    addAlias("ElevationDef", "defs.ElevationDef");
    addAlias("GameObjectDef", "defs.GameObjectDef");
    addAlias("org.darkquest.spriteeditor.Sprite",
             "org.darkquest.client.model.Sprite");
  }

  private static void addAlias(String name, String className) {
    try {
      xstream.alias(name, Class.forName(className));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static Object loadURL(String url) {
    try {
      URL link = new URL(url);
      //ReadableByteChannel rbc = Channels.newChannel(link.openStream());
      InputStream fis = null;
      File tempFile = File.createTempFile("dqc-",".rscd");
      FileOutputStream fos = new FileOutputStream(tempFile);
      URLConnection urlConn = link.openConnection();
      fis = urlConn.getInputStream();
      byte[] buffer = new byte[4096];
      int len;
      while ((len = fis.read(buffer)) > 0) {
        fos.write(buffer,0,len);
      }

      //fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
      InputStream is = new GZIPInputStream(new FileInputStream(tempFile));
      Object rv = xstream.fromXML(is);
      return rv;
    } catch (IOException ioe) {
      System.err.println(ioe.getMessage());
    }
    return null;
  }

  public static Object load(File file) {
    try {
      InputStream is = new GZIPInputStream(new FileInputStream(file));
      Object rv = xstream.fromXML(is);
      return rv;
    } catch (IOException ioe) {
      System.err.println(ioe.getMessage());
    }
    return null;
  }

  public static void write(File file, Object o) {
    try {
      OutputStream os = new GZIPOutputStream(new FileOutputStream(file));
      xstream.toXML(o, os);
    } catch (IOException ioe) {
      System.err.println(ioe.getMessage());
    }
  }
}
