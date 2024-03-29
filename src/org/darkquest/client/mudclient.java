package org.darkquest.client;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.darkquest.client.entityhandling.EntityHandler;
import org.darkquest.client.gui.QuestionMenu;
import org.darkquest.client.gui.SmithingScreen;
import org.darkquest.client.gui.StatMenu;
import org.darkquest.client.interfaces.InterfaceGUI;
import org.darkquest.client.interfaces.InterfaceHandler;
import org.darkquest.client.model.Display;
import org.darkquest.client.model.Resolutions;
import org.darkquest.client.model.Sprite;
import org.darkquest.client.recorder.Recorder;
import org.darkquest.client.util.Config;
import org.darkquest.client.util.DataConversions;

import defs.ItemDef;
import defs.NPCDef;

public final class mudclient extends GameWindowMiddleMan {

  public static mudclient getMc() {
    return mc;
  }

  private static InterfaceHandler interfacehandler = null;

  public static InterfaceHandler getInterfaceHandler() {
    if(interfacehandler == null) {
      interfacehandler = new InterfaceHandler();
    }
    return interfacehandler;
  }


  static boolean notifyUserOfCrash = false;
  public static void redirectSystemStreams() {
    File f = new File(new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data" + File.separator + "error.log");
    if(f.exists()) {
      f.delete();
    }
    try {
      f.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }


    OutputStream out = new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        if(!notifyUserOfCrash) {
          notifyUserOfCrash = true;
          JOptionPane.showMessageDialog(null, "The client has thrown errors! Please restart the client (the error log will be automatically sent)!");
        }

        String s = String.valueOf((char) b);
        BufferedWriter bw = null;

        try {
          bw = new BufferedWriter(new FileWriter(new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data" + File.separator + "error.log", true));
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
        bw.write(s);
        bw.flush();
        try {
          bw.close();
        } catch (IOException ioe2) {
        }

        bw.write(s);
        //bw.newLine();
        bw.flush();
        System.out.println(s);

      }
      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        String s = new String(b, off, len);
        if(!notifyUserOfCrash) {
          notifyUserOfCrash = true;
          JOptionPane.showMessageDialog(null, "The client has thrown errors! Please restart the client (the error log will be automatically sent)!");
        }
        BufferedWriter bw = null;

        try {
          bw = new BufferedWriter(new FileWriter(new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data" + File.separator + "error.log", true));
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
        bw.write(s);
        bw.newLine();
        bw.flush();
        try {
          bw.close();
        } catch (IOException ioe2) {
        }

      }

      @Override
      public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
      }
    };
    System.setErr(new PrintStream(out, true));
  }

  public static Properties config = Config.loadConfig();

  InterfaceGUI smithingscreen = null;

  QuestionMenu questionMenu = null;

  StatMenu statMenu = null;

  boolean needsClear = false;
  private static final long serialVersionUID = -1311866859199894174L;
  public static final int SPRITE_MEDIA_START = 2000;
  public static final int SPRITE_UTIL_START = 2100;
  public static final int SPRITE_ITEM_START = 2150;
  public static final int SPRITE_LOGO_START = 3150;
  public static int SPRITE_PROJECTILE_START = 3160;
  public static final int SPRITE_TEXTURE_START = 3220;
  public static ArrayList<String> messages = new ArrayList<String>();
  public static int currentChat = 0;
  public static HashMap<String, File> dataFiles = new HashMap<String, File>();
  boolean lastUp = false;
  private boolean menusLoaded = false;
  private int cameraVertical = 912;
  private boolean fog = false;
  private long startTime = 0;
  private long serverStartTime = 0;
  private int fatigue;
  private String serverLocation = "";
  private String localhost;
  private int prayerMenuIndex = 0;
  private int magicMenuIndex = 0;
  private boolean showRoof = true;
  private boolean autoScreenshot = true;
  private long expGained = 0;
  private boolean hasWorldInfo = false;
  private boolean recording = false;
  private LinkedList<BufferedImage> frames = new LinkedList<BufferedImage>();
  private boolean ignoreNext = false;
  private int fogVar = 0;
  private int fps = 50;
  private int cur_fps;
  private long lastFps = 0L;

  private Map<Integer, String> newQuestNames = new HashMap<Integer, String>();
  private Map<Integer, Integer> questStage = new HashMap<Integer, Integer>();

  private Menu questMenu;
  public File dataFile = null;
  private int questMenuHandle;
  private int questPoints = 0;//camera error
  public static boolean showPackets = false;
  private static mudclient mc;



  public static final void main(String[] args) throws Exception {// Duel with
    mc = new mudclient();

    //redirectSystemStreams();

    loadCachedFile("Loading.rscd");
    mc.setLogo(Toolkit.getDefaultToolkit().getImage(
                 new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile()
                 + File.separator + "Loading.rscd"));

    mc.createWindow(mc.windowWidth, mc.windowHeight + 15 - 8,
                    "DarkQuest", false);
    mc.fog = false;
    /**
     * Edit this part to have more messages
     */
    mc.messagesArray = new String[5];
    mc.messagesTimeout = new int[mc.messagesArray.length];

  }
  public static Resolutions reso = new Resolutions();
  public static boolean LOOT_ENABLED = true;

  private boolean handleCommand(String s) {
    try {
      int firstSpace = s.indexOf(" ");
      String cmd = s;
      String[] args = new String[0];
      if (firstSpace != -1) {
        cmd = s.substring(0, firstSpace).trim();
        args = s.substring(firstSpace + 1).trim().split(" ");
      }
      if (cmd.equals("noloot")) {
        if (!LOOT_ENABLED) {
          displayMessage("Ground items have been enabled.", 3, 0);
          LOOT_ENABLED = true;
        } else {
          displayMessage("Ground items have been visually disabled, to enable repeat this command.", 3, 0);
          LOOT_ENABLED = false;
        }
      }
      if (cmd.equals("offer")) {
        int id, amount;
        try {
          id = Integer.parseInt(args[0]);
          amount = Integer.parseInt(args[1]);
          boolean done = false;
          if (showTradeWindow) {
            if (tradeMyItemCount >= 12) {
              displayMessage(
                "@cya@Your trade offer is currently full", 3, 0);
              return true;
            }
            if (inventoryCount(id) < amount) {
              displayMessage("@cya@You do not have that many "
                             + EntityHandler.getItemDef(id).getName()
                             + " to offer", 3, 0);
              return true;
            }
            if (!EntityHandler.getItemDef(id).isStackable()
                && amount > 1) {
              displayMessage(
                "@cya@You can only offer 1 non stackable at a time",
                3, 0);
              return true;
            }
            for (int c = 0; c < tradeMyItemCount; c++) {
              if (tradeMyItems[c] == id) {
                if (EntityHandler.getItemDef(id).isStackable()) {
                  if (inventoryCount(id) < (tradeMyItemsCount[c] + amount)) {
                    displayMessage(
                      "@cya@You do not have that many"
                      + EntityHandler.getItemDef(
                        id).getName()
                      + " to offer", 3, 0);
                    return true;
                  }
                  tradeMyItemsCount[c] += amount;
                  done = true;
                }
                break;
              }
            }

            if (!done) {
              tradeMyItems[tradeMyItemCount] = id;
              tradeMyItemsCount[tradeMyItemCount] = amount;
              tradeMyItemCount++;
            }

            super.streamClass.createPacket(70);
            super.streamClass.addByte(tradeMyItemCount);
            for (int c = 0; c < tradeMyItemCount; c++) {
              super.streamClass.add2ByteInt(tradeMyItems[c]);
              super.streamClass.add4ByteInt(tradeMyItemsCount[c]);
            }
            super.streamClass.formatPacket();

            tradeOtherAccepted = false;
            tradeWeAccepted = false;
          } else if (showDuelWindow) {
            if (duelMyItemCount >= 12) {
              displayMessage(
                "@cya@Your duel offer is currently full", 3, 0);
              return true;
            }
            if (inventoryCount(id) < amount) {
              displayMessage("@cya@You do not have that many"
                             + EntityHandler.getItemDef(id).getName()
                             + " to offer", 3, 0);
              return true;
            }
            if (!EntityHandler.getItemDef(id).isStackable()
                && amount > 1) {
              displayMessage(
                "@cya@You can only offer 1 non stackable at a time",
                3, 0);
              return true;
            }

            for (int c = 0; c < duelMyItemCount; c++) {
              if (duelMyItems[c] == id) {
                if (EntityHandler.getItemDef(id).isStackable()) {
                  if (inventoryCount(id) < (duelMyItemsCount[c] + amount)) {
                    displayMessage(
                      "@cya@You do not have that many"
                      + EntityHandler.getItemDef(
                        id).getName()
                      + " to offer", 3, 0);
                    return true;
                  }
                  duelMyItemsCount[c] += amount;
                  done = true;
                }
                break;
              }
            }

            if (!done) {
              duelMyItems[duelMyItemCount] = id;
              duelMyItemsCount[duelMyItemCount] = amount;
              duelMyItemCount++;
            }

            super.streamClass.createPacket(123);
            super.streamClass.addByte(duelMyItemCount);
            for (int c = 0; c < duelMyItemCount; c++) {
              super.streamClass.add2ByteInt(duelMyItems[c]);
              super.streamClass.add4ByteInt(duelMyItemsCount[c]);
            }
            super.streamClass.formatPacket();

            duelOpponentAccepted = false;
            duelMyAccepted = false;
          } else {
            displayMessage(
              "@cya@You aren't in a trade/stake, there is nothing to offer to.",
              3, 0);
          }
        } catch (Exception e) {
          e.printStackTrace();
          displayMessage("@cya@Invalid args!", 3, 0);
        }
        return true;
      }
      return false;
    } catch(Exception e) {
      System.err.println("CAUGHT HERE");
      e.printStackTrace();
      return false;
    }
  }

  private static String timeSince(long time) {
    try {
      int seconds = (int) ((System.currentTimeMillis() - time) / 1000);
      int minutes = (int) (seconds / 60);
      int hours = (int) (minutes / 60);
      int days = (int) (hours / 24);
      return days + " days " + (hours % 24) + " hours " + (minutes % 60)
             + " mins";
    } catch(Exception e) {
      return "";
    }

  }

  private BufferedImage getImage() throws IOException {
    BufferedImage bufferedImage = new BufferedImage(windowWidth,
        windowHeight + 11, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = bufferedImage.createGraphics();
    g2d.drawImage(gameGraphics.image, 0, 0, null);
    g2d.dispose();
    return bufferedImage;
  }

  private File getEmptyFile(boolean movie) throws IOException {
    String charName = DataOperations.longToString(DataOperations
                      .stringLength12ToLong(currentUser));
    File file = new File(Config.MEDIA_DIR + File.separator + charName);
    if (!file.exists() || !file.isDirectory()) {
      file.mkdir();
    }
    String folder = file.getPath() + File.separator;
    file = null;
    for (int suffix = 0; file == null || file.exists(); suffix++) {
      file = movie ? new File(folder + "movie" + suffix + ".mov")
             : new File(folder + "screenshot" + suffix + ".png");
    }
    return file;
  }

  private boolean takeScreenshot(boolean verbose) {
    try {
      File file = getEmptyFile(false);
      ImageIO.write(getImage(), "png", file);
      if (verbose) {
        handleServerMessage("Screenshot saved as " + file.getName()
                            + ".");
      }
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      if (verbose) {
        handleServerMessage("Error saving screenshot.");
      }
      return false;
    }
  }

  final void method45(int i, int j, int k, int l, int i1, int j1, int k1) {
    if(i1 > npcArray.length)
      return;

    Mob mob = npcArray[i1];
    int l1 = mob.currentSprite + (cameraRotation + 16) / 32 & 7;
    boolean flag = false;
    int i2 = l1;
    if (i2 == 5) {
      i2 = 3;
      flag = true;
    } else if (i2 == 6) {
      i2 = 2;
      flag = true;
    } else if (i2 == 7) {
      i2 = 1;
      flag = true;
    }
    int j2 = i2
             * 3
             + walkModel[(mob.stepCount / EntityHandler.getNpcDef(mob.type)
                          .getWalkModel()) % 4];
    if (mob.currentSprite == 8) {
      i2 = 5;
      l1 = 2;
      flag = false;
      i -= (EntityHandler.getNpcDef(mob.type).getCombatSprite() * k1) / 100;
      j2 = i2
           * 3
           + npcCombatModelArray1[(loginTimer / (EntityHandler
                                   .getNpcDef(mob.type).getCombatModel() - 1)) % 8];
    } else if (mob.currentSprite == 9) {
      i2 = 5;
      l1 = 2;
      flag = true;
      i += (EntityHandler.getNpcDef(mob.type).getCombatSprite() * k1) / 100;
      j2 = i2
           * 3
           + npcCombatModelArray2[(loginTimer / EntityHandler
                                   .getNpcDef(mob.type).getCombatModel()) % 8];
    }
    for (int k2 = 0; k2 < 12; k2++) {
      int l2 = npcAnimationArray[l1][k2];
      int k3 = EntityHandler.getNpcDef(mob.type).getSprite(l2);
      if (k3 >= 0) {
        int i4 = 0;
        int j4 = 0;
        int k4 = j2;
        if (flag && i2 >= 1 && i2 <= 3
            && EntityHandler.getAnimationDef(k3).hasF())
          k4 += 15;
        if (i2 != 5 || EntityHandler.getAnimationDef(k3).hasA()) {
          int l4 = k4 + EntityHandler.getAnimationDef(k3).getNumber();
          i4 = (i4 * k)
               / ((GameImage) (gameGraphics)).sprites[l4]
               .getSomething1();
          j4 = (j4 * l)
               / ((GameImage) (gameGraphics)).sprites[l4]
               .getSomething2();
          int i5 = (k * ((GameImage) (gameGraphics)).sprites[l4]
                    .getSomething1())
                   / ((GameImage) (gameGraphics)).sprites[EntityHandler
                       .getAnimationDef(k3).getNumber()]
                   .getSomething1();
          i4 -= (i5 - k) / 2;
          int colour = EntityHandler.getAnimationDef(k3)
                       .getCharColour();
          int skinColour = 0;
          if (colour == 1) {
            colour = EntityHandler.getNpcDef(mob.type)
                     .getHairColour();
            skinColour = EntityHandler.getNpcDef(mob.type)
                         .getSkinColour();
          } else if (colour == 2) {
            colour = EntityHandler.getNpcDef(mob.type)
                     .getTopColour();
            skinColour = EntityHandler.getNpcDef(mob.type)
                         .getSkinColour();
          } else if (colour == 3) {
            colour = EntityHandler.getNpcDef(mob.type)
                     .getBottomColour();
            skinColour = EntityHandler.getNpcDef(mob.type)
                         .getSkinColour();
          }
          gameGraphics.spriteClip4(i + i4, j + j4, i5, l, l4, colour,
                                   skinColour, j1, flag);
        }
      }
    }

    if (mob.lastMessageTimeout > 0) {
      mobMessagesWidth[mobMessageCount] = gameGraphics.textWidth(
                                            mob.lastMessage, 1) / 2;
      if (mobMessagesWidth[mobMessageCount] > 150)
        mobMessagesWidth[mobMessageCount] = 150;
      mobMessagesHeight[mobMessageCount] = (gameGraphics.textWidth(
                                              mob.lastMessage, 1) / 300)
                                           * gameGraphics.messageFontHeight(1);
      mobMessagesX[mobMessageCount] = i + k / 2;
      mobMessagesY[mobMessageCount] = j;
      mobMessages[mobMessageCount++] = mob.lastMessage;
    }
    if (mob.currentSprite == 8 || mob.currentSprite == 9
        || mob.combatTimer != 0) {
      if (mob.combatTimer > 0) {
        int i3 = i;
        if (mob.currentSprite == 8)
          i3 -= (20 * k1) / 100;
        else if (mob.currentSprite == 9)
          i3 += (20 * k1) / 100;
        int l3 = (mob.hitPointsCurrent * 30) / mob.hitPointsBase;
        anIntArray786[anInt718] = i3 + k / 2;
        anIntArray787[anInt718] = j;
        anIntArray788[anInt718++] = l3;
      }
      if (mob.combatTimer > 150) {
        int j3 = i;
        if (mob.currentSprite == 8)
          j3 -= (10 * k1) / 100;
        else if (mob.currentSprite == 9)
          j3 += (10 * k1) / 100;
        gameGraphics.drawPicture((j3 + k / 2) - 12, (j + l / 2) - 12,
                                 SPRITE_MEDIA_START + 12);
        gameGraphics.drawText(String.valueOf(mob.anInt164),
                              (j3 + k / 2) - 1, j + l / 2 + 5, 3, 0xffffff);
      }
    }
  }

  private final void drawCharacterLookScreen() {
    characterDesignMenu.updateActions(super.mouseX, super.mouseY,
                                      super.lastMouseDownButton, super.mouseDownButton);
    if (characterDesignMenu.hasActivated(characterDesignHeadButton1))
      do
        characterHeadType = ((characterHeadType - 1) + EntityHandler
                             .animationCount())
                            % EntityHandler.animationCount();
      while ((EntityHandler.getAnimationDef(characterHeadType)
              .getGenderModel() & 3) != 1
             || (EntityHandler.getAnimationDef(characterHeadType)
                 .getGenderModel() & 4 * characterHeadGender) == 0);
    if (characterDesignMenu.hasActivated(characterDesignHeadButton2))
      do
        characterHeadType = (characterHeadType + 1)
                            % EntityHandler.animationCount();
      while ((EntityHandler.getAnimationDef(characterHeadType)
              .getGenderModel() & 3) != 1
             || (EntityHandler.getAnimationDef(characterHeadType)
                 .getGenderModel() & 4 * characterHeadGender) == 0);
    if (characterDesignMenu.hasActivated(characterDesignHairColourButton1))
      characterHairColour = ((characterHairColour - 1) + characterHairColours.length)
                            % characterHairColours.length;
    if (characterDesignMenu.hasActivated(characterDesignHairColourButton2))
      characterHairColour = (characterHairColour + 1)
                            % characterHairColours.length;
    if (characterDesignMenu.hasActivated(characterDesignGenderButton1)
        || characterDesignMenu
        .hasActivated(characterDesignGenderButton2)) {
      for (characterHeadGender = 3 - characterHeadGender; (EntityHandler
           .getAnimationDef(characterHeadType).getGenderModel() & 3) != 1
           || (EntityHandler.getAnimationDef(characterHeadType)
               .getGenderModel() & 4 * characterHeadGender) == 0; characterHeadType = (characterHeadType + 1)
                   % EntityHandler.animationCount())
        ;
      for (; (EntityHandler.getAnimationDef(characterBodyGender)
              .getGenderModel() & 3) != 2
           || (EntityHandler.getAnimationDef(characterBodyGender)
               .getGenderModel() & 4 * characterHeadGender) == 0; characterBodyGender = (characterBodyGender + 1)
                   % EntityHandler.animationCount())
        ;
    }
    if (characterDesignMenu.hasActivated(characterDesignTopColourButton1))
      characterTopColour = ((characterTopColour - 1) + characterTopBottomColours.length)
                           % characterTopBottomColours.length;
    if (characterDesignMenu.hasActivated(characterDesignTopColourButton2))
      characterTopColour = (characterTopColour + 1)
                           % characterTopBottomColours.length;
    if (characterDesignMenu.hasActivated(characterDesignSkinColourButton1))
      characterSkinColour = ((characterSkinColour - 1) + characterSkinColours.length)
                            % characterSkinColours.length;
    if (characterDesignMenu.hasActivated(characterDesignSkinColourButton2))
      characterSkinColour = (characterSkinColour + 1)
                            % characterSkinColours.length;
    if (characterDesignMenu
        .hasActivated(characterDesignBottomColourButton1))
      characterBottomColour = ((characterBottomColour - 1) + characterTopBottomColours.length)
                              % characterTopBottomColours.length;
    if (characterDesignMenu
        .hasActivated(characterDesignBottomColourButton2))
      characterBottomColour = (characterBottomColour + 1)
                              % characterTopBottomColours.length;
    if (characterDesignMenu.hasActivated(characterDesignAcceptButton)) {
      super.streamClass.createPacket(218);
      super.streamClass.addByte(characterHeadGender);
      super.streamClass.addByte(characterHeadType);
      super.streamClass.addByte(characterBodyGender);
      super.streamClass.addByte(character2Colour);
      super.streamClass.addByte(characterHairColour);
      super.streamClass.addByte(characterTopColour);
      super.streamClass.addByte(characterBottomColour);
      super.streamClass.addByte(characterSkinColour);
      super.streamClass.formatPacket();
      gameGraphics.method211();
      showCharacterLookScreen = false;
    }
  }

  private final int inventoryCount(int reqID) {
    int amount = 0;
    for (int index = 0; index < inventoryCount; index++) {
      if (getInventoryItems()[index] == reqID) {
        if (!EntityHandler.getItemDef(reqID).isStackable()) {
          amount++;
        } else {
          amount += inventoryItemsCount[index];
        }
      }
    }
    return amount;
  }

  private final void updateLoginScreen() {
    if (super.socketTimeout > 0)
      super.socketTimeout--;
    if (loginScreenNumber == 0) {
      menuWelcome.updateActions(super.mouseX, super.mouseY, super.lastMouseDownButton, super.mouseDownButton);
      if (menuWelcome.hasActivated(loginButtonNewUser))
        loginScreenNumber = 1;
      if (menuWelcome.hasActivated(loginButtonExistingUser)) {
        loginScreenNumber = 2;
        menuLogin.updateText(loginStatusText,
                             "Please enter your username and password");
        menuLogin.updateText(loginUsernameTextBox, currentUser);
        menuLogin.updateText(loginPasswordTextBox, currentPass);
        menuLogin.setFocus(loginUsernameTextBox);
        return;
      }
    } else if (loginScreenNumber == 1) {
      menuNewUser.updateActions(super.mouseX, super.mouseY,
                                super.lastMouseDownButton, super.mouseDownButton);
      if (menuNewUser.hasActivated(newUserOkButton)) {
        loginScreenNumber = 0;
        return;
      }
    } else if (loginScreenNumber == 2) {
      menuLogin.updateActions(super.mouseX, super.mouseY,
                              super.lastMouseDownButton, super.mouseDownButton);
      if (menuLogin.hasActivated(loginCancelButton))
        loginScreenNumber = 0;
      if (menuLogin.hasActivated(loginUsernameTextBox))
        menuLogin.setFocus(loginPasswordTextBox);
      if (menuLogin.hasActivated(loginPasswordTextBox)
          || menuLogin.hasActivated(loginOkButton)) {
        currentUser = menuLogin.getText(loginUsernameTextBox);
        currentPass = menuLogin.getText(loginPasswordTextBox);
        login(currentUser, currentPass, false);

      }
    }
  }

  private final void drawLoginScreen() {
    hasReceivedWelcomeBoxDetails = false;
    gameGraphics.f1Toggle = false;
    gameGraphics.method211();
    if (loginScreenNumber == 0 || loginScreenNumber == 1
        || loginScreenNumber == 2 || loginScreenNumber == 3) {
      gameGraphics.drawPicture(3 + xAddition, 0 + yAddition, SPRITE_LOGO_START);
    }
    //then register on forum
    if (loginScreenNumber == 0)
      menuWelcome.drawMenu();
    if (loginScreenNumber == 1)
      menuNewUser.drawMenu();
    if (loginScreenNumber == 2)
      menuLogin.drawMenu();

    gameGraphics.spriteClip4(0, windowHeight, windowWidth, 10, SPRITE_MEDIA_START + 22, 0, 0, 0, false);

    gameGraphics.drawImage(aGraphics936, 0, 0);
  }
  //abuse
  private final void drawAbuseWindow1() {
    abuseSelectedType = 0;
    int i = 135;
    for (int j = 0; j < 8; j++) {
      if (super.mouseX > 66 + xAddition && super.mouseX < 446 + xAddition
          && super.mouseY >= i - 12 + yAddition && super.mouseY < i + 3 + yAddition)
        abuseSelectedType = j + 1;
      i += 14;
    }

    if (mouseButtonClick != 0 && abuseSelectedType != 0) {
      mouseButtonClick = 0;
      showAbuseWindow = 2;
      super.inputText = "";
      super.enteredText = "";
      return;
    }
    i += 15;
    if (mouseButtonClick != 0) {
      mouseButtonClick = 0;
      if (super.mouseX < 56 + xAddition || super.mouseY < 35 + yAddition || super.mouseX > 456 + xAddition
          || super.mouseY > 325 - 50 + yAddition) {
        showAbuseWindow = 0;
        return;
      }
      if (super.mouseX > 66 + xAddition && super.mouseX < 446 + xAddition
          && super.mouseY >= i - 15 + yAddition && super.mouseY < i + 5 + yAddition) {
        showAbuseWindow = 0;
        return;
      }

    }
    gameGraphics.drawBox(56 + xAddition, 35 + yAddition, 400, 240, 0);
    gameGraphics.drawBoxEdge(56 + xAddition, 35 + yAddition, 400, 240, 0xffffff);
    i = 50 + yAddition;
    gameGraphics
    .drawText(
      "This form is for reporting players who are breaking our rules",
      256 + xAddition, i, 1, 0xffffff);
    i += 15;
    gameGraphics
    .drawText(
      "Using it sends a snapshot of the last 60 secs of activity to us",
      256 + xAddition, i, 1, 0xffffff);
    i += 15;
    gameGraphics.drawText("If you misuse this form, you will be banned.",
                          256 + xAddition, i, 1, 0xff8000);
    i += 15;
    i += 10;
    gameGraphics
    .drawText(
      "First indicate which of our 7 rules is being broken. For a detailed",
      256 + xAddition, i, 1, 0xffff00);
    i += 15;
    gameGraphics
    .drawText(
      "explanation of each rule please read the manual on our website.",
      256 + xAddition, i, 1, 0xffff00);
    i += 15;
    int k;
    if (abuseSelectedType == 1) {
      gameGraphics.drawBoxEdge(66 + xAddition, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    gameGraphics.drawText("1: Extremly abusive behaviour", 256 + xAddition, i, 1, k);
    i += 14;
    if (abuseSelectedType == 2) {
      gameGraphics.drawBoxEdge(66 + xAddition, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    //gameGraphics.drawText("2: Item scamming", 256, i, 1, k);
    //i += 14;
    /*if (abuseSelectedType == 3) {
      gameGraphics.drawBoxEdge(66, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    gameGraphics.drawText("3: Password scamming", 256, i, 1, k);
    */
    //i += 14;

    /*
     if (abuseSelectedType == 4) {
      gameGraphics.drawBoxEdge(66, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    */
    gameGraphics.drawText("2: Bug abuse", 256 + xAddition, i, 1, k);
    i += 14;
    if (abuseSelectedType == 3) {
      gameGraphics.drawBoxEdge(66 + xAddition, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    gameGraphics.drawText("3: DarkQuest Staff impersonation", 256 + xAddition, i, 1, k);
    i += 14;
    if (abuseSelectedType == 4) {
      gameGraphics.drawBoxEdge(66 + xAddition, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }

    gameGraphics.drawText("4: Account trading/selling", 256 + xAddition, i, 1, k);
    i += 14;
    if (abuseSelectedType == 5) {
      gameGraphics.drawBoxEdge(66 + xAddition, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    gameGraphics.drawText("5: Macroing", 256 + xAddition, i, 1, k);
    i += 14;
    if (abuseSelectedType == 6) {
      gameGraphics.drawBoxEdge(66 + xAddition, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    //gameGraphics.drawText("8: Mutiple logging in", 256, i, 1, k);
    //i += 14;
    /*
    if (abuseSelectedType == 9) {
      gameGraphics.drawBoxEdge(66, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }*/
    gameGraphics.drawText("6: Encouraging others to break rules", 256 + xAddition, i,
                          1, k);
    i += 14;
    if (abuseSelectedType == 7) {
      gameGraphics.drawBoxEdge(66 + xAddition, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }

    //gameGraphics.drawText("10: Misuse of customer support", 256+xAddition, i, 1, k);
    //i += 14;
    /*
    if (abuseSelectedType == 11) {
      gameGraphics.drawBoxEdge(66, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    */
    gameGraphics.drawText("7: Advertising / website", 256 + xAddition, i, 1, k);
    i += 14;
    if (abuseSelectedType == 8) {
      gameGraphics.drawBoxEdge(66 + xAddition, i - 12, 380, 15, 0xffffff);
      k = 0xff8000;
    } else {
      k = 0xffffff;
    }
    gameGraphics.drawText("8: Real world item trading", 256 + xAddition, i, 1, k);
    i += 14;
    i += 15;
    k = 0xffffff;
    if (super.mouseX > 196 + xAddition && super.mouseX < 316 + xAddition && super.mouseY > i - 15
        && super.mouseY < i + 5)
      k = 0xffff00;

    gameGraphics.drawText("Click here to cancel", 256 + xAddition, i, 1, k);
  }

  private final void autoRotateCamera() {
    if ((cameraAutoAngle & 1) == 1 && enginePlayerVisible(cameraAutoAngle))
      return;
    if ((cameraAutoAngle & 1) == 0 && enginePlayerVisible(cameraAutoAngle)) {
      if (enginePlayerVisible(cameraAutoAngle + 1 & 7)) {
        cameraAutoAngle = cameraAutoAngle + 1 & 7;
        return;
      }
      if (enginePlayerVisible(cameraAutoAngle + 7 & 7))
        cameraAutoAngle = cameraAutoAngle + 7 & 7;
      return;
    }
    int ai[] = { 1, -1, 2, -2, 3, -3, 4 };
    for (int i = 0; i < 7; i++) {
      if (!enginePlayerVisible(cameraAutoAngle + ai[i] + 8 & 7))
        continue;
      cameraAutoAngle = cameraAutoAngle + ai[i] + 8 & 7;
      break;
    }

    if ((cameraAutoAngle & 1) == 0 && enginePlayerVisible(cameraAutoAngle)) {
      if (enginePlayerVisible(cameraAutoAngle + 1 & 7)) {
        cameraAutoAngle = cameraAutoAngle + 1 & 7;
        return;
      }
      if (enginePlayerVisible(cameraAutoAngle + 7 & 7))
        cameraAutoAngle = cameraAutoAngle + 7 & 7;
    }
  }

  public final Graphics getGraphics() {
    if (GameWindow.gameFrame != null) {
      return GameWindow.gameFrame.getGraphics();
    }
    return super.getGraphics();
  }

  final void method52(int i, int j, int k, int l, int i1, int j1, int k1) {
    Mob mob = playerArray[i1];
    if (mob.colourBottomType == 255)
      return;
    int l1 = mob.currentSprite + (cameraRotation + 16) / 32 & 7;
    boolean flag = false;
    int i2 = l1;
    if (i2 == 5) {
      i2 = 3;
      flag = true;
    } else if (i2 == 6) {
      i2 = 2;
      flag = true;
    } else if (i2 == 7) {
      i2 = 1;
      flag = true;
    }
    int j2 = i2 * 3 + walkModel[(mob.stepCount / 6) % 4];
    if (mob.currentSprite == 8) {
      i2 = 5;
      l1 = 2;
      flag = false;
      i -= (5 * k1) / 100;
      j2 = i2 * 3 + npcCombatModelArray1[(loginTimer / 5) % 8];
    } else if (mob.currentSprite == 9) {
      i2 = 5;
      l1 = 2;
      flag = true;
      i += (5 * k1) / 100;
      j2 = i2 * 3 + npcCombatModelArray2[(loginTimer / 6) % 8];
    }
    for (int k2 = 0; k2 < 12; k2++) {
      int l2 = npcAnimationArray[l1][k2];
      int l3 = mob.animationCount[l2] - 1;
      if (l3 >= 0) {
        int k4 = 0;
        int i5 = 0;
        int j5 = j2;
        if (flag && i2 >= 1 && i2 <= 3)
          if (EntityHandler.getAnimationDef(l3).hasF())
            j5 += 15;
          else if (l2 == 4 && i2 == 1) {
            k4 = -22;
            i5 = -3;
            j5 = i2 * 3 + walkModel[(2 + mob.stepCount / 6) % 4];
          } else if (l2 == 4 && i2 == 2) {
            k4 = 0;
            i5 = -8;
            j5 = i2 * 3 + walkModel[(2 + mob.stepCount / 6) % 4];
          } else if (l2 == 4 && i2 == 3) {
            k4 = 26;
            i5 = -5;
            j5 = i2 * 3 + walkModel[(2 + mob.stepCount / 6) % 4];
          } else if (l2 == 3 && i2 == 1) {
            k4 = 22;
            i5 = 3;
            j5 = i2 * 3 + walkModel[(2 + mob.stepCount / 6) % 4];
          } else if (l2 == 3 && i2 == 2) {
            k4 = 0;
            i5 = 8;
            j5 = i2 * 3 + walkModel[(2 + mob.stepCount / 6) % 4];
          } else if (l2 == 3 && i2 == 3) {
            k4 = -26;
            i5 = 5;
            j5 = i2 * 3 + walkModel[(2 + mob.stepCount / 6) % 4];
          }
        if (i2 != 5 || EntityHandler.getAnimationDef(l3).hasA()) {
          int k5 = j5 + EntityHandler.getAnimationDef(l3).getNumber();
          k4 = (k4 * k)
               / ((GameImage) (gameGraphics)).sprites[k5]
               .getSomething1();
          i5 = (i5 * l)
               / ((GameImage) (gameGraphics)).sprites[k5]
               .getSomething2();
          int l5 = (k * ((GameImage) (gameGraphics)).sprites[k5]
                    .getSomething1())
                   / ((GameImage) (gameGraphics)).sprites[EntityHandler
                       .getAnimationDef(l3).getNumber()]
                   .getSomething1();
          k4 -= (l5 - k) / 2;
          int colour = EntityHandler.getAnimationDef(l3)
                       .getCharColour();
          int skinColour = characterSkinColours[mob.colourSkinType];
          if (colour == 1)
            colour = characterHairColours[mob.colourHairType];
          else if (colour == 2)
            colour = characterTopBottomColours[mob.colourTopType];
          else if (colour == 3)
            colour = characterTopBottomColours[mob.colourBottomType];
          gameGraphics.spriteClip4(i + k4, j + i5, l5, l, k5, colour,
                                   skinColour, j1, flag);
        }
      }
    }

    if (mob.lastMessageTimeout > 0) {
      mobMessagesWidth[mobMessageCount] = gameGraphics.textWidth(
                                            mob.lastMessage, 1) / 2;
      if (mobMessagesWidth[mobMessageCount] > 150)
        mobMessagesWidth[mobMessageCount] = 150;
      mobMessagesHeight[mobMessageCount] = (gameGraphics.textWidth(
                                              mob.lastMessage, 1) / 300)
                                           * gameGraphics.messageFontHeight(1);
      mobMessagesX[mobMessageCount] = i + k / 2;
      mobMessagesY[mobMessageCount] = j;
      mobMessages[mobMessageCount++] = mob.lastMessage;
    }
    if (mob.anInt163 > 0) {
      anIntArray858[anInt699] = i + k / 2;
      anIntArray859[anInt699] = j;
      anIntArray705[anInt699] = k1;
      anIntArray706[anInt699++] = mob.anInt162;
    }
    if (mob.currentSprite == 8 || mob.currentSprite == 9
        || mob.combatTimer != 0) {
      if (mob.combatTimer > 0) {
        int i3 = i;
        if (mob.currentSprite == 8)
          i3 -= (20 * k1) / 100;
        else if (mob.currentSprite == 9)
          i3 += (20 * k1) / 100;
        int i4 = (mob.hitPointsCurrent * 30) / mob.hitPointsBase;
        anIntArray786[anInt718] = i3 + k / 2;
        anIntArray787[anInt718] = j;
        anIntArray788[anInt718++] = i4;
      }
      if (mob.combatTimer > 150) {
        int j3 = i;
        if (mob.currentSprite == 8)
          j3 -= (10 * k1) / 100;
        else if (mob.currentSprite == 9)
          j3 += (10 * k1) / 100;
        gameGraphics.drawPicture((j3 + k / 2) - 12, (j + l / 2) - 12,
                                 SPRITE_MEDIA_START + 11);
        gameGraphics.drawText(String.valueOf(mob.anInt164),
                              (j3 + k / 2) - 1, j + l / 2 + 5, 3, 0xffffff);
      }
    }
    if (mob.anInt179 == 1 && mob.anInt163 == 0) {
      int k3 = j1 + i + k / 2;
      if (mob.currentSprite == 8)
        k3 -= (20 * k1) / 100;
      else if (mob.currentSprite == 9)
        k3 += (20 * k1) / 100;
      int j4 = (16 * k1) / 100;
      int l4 = (16 * k1) / 100;
      gameGraphics.spriteClip1(k3 - j4 / 2, j - l4 / 2 - (10 * k1) / 100,
                               j4, l4, SPRITE_MEDIA_START + 13);
    }
  }

  private final void loadConfigFilter() {
    drawDownloadProgress("Checking local data files", 1);
    EntityHandler.load(true);
  }

  private final void loadModels() {
    drawDownloadProgress("Loading 3d models", 75);

    String[] modelNames = { "torcha2", "torcha3", "torcha4",
                            "skulltorcha2", "skulltorcha3", "skulltorcha4", "firea2",
                            "firea3", "fireplacea2", "fireplacea3", "firespell2",
                            "firespell3", "lightning2", "lightning3", "clawspell2",
                            "clawspell3", "clawspell4", "clawspell5", "spellcharge2",
                            "spellcharge3"
                          };
    for (String name : modelNames) {
      EntityHandler.storeModel(name);
    }

    byte[] models = load("models36.jag");
    if (models == null) {
      lastLoadedNull = true;
      return;
    }
    for (int j = 0; j < EntityHandler.getModelCount(); j++) {
      int k = DataOperations.method358(EntityHandler.getModelName(j)
                                       + ".ob3", models);
      if (k == 0) {
        gameDataModels[j] = new Model(1, 1);
      } else {
        gameDataModels[j] = new Model(models, k, true);
      }
      gameDataModels[j].isGiantCrystal = EntityHandler.getModelName(j)
                                         .equals("giantcrystal");
    }
  }

  protected final void handleMouseDown(int button, int x, int y) {
    mouseClickXArray[mouseClickArrayOffset] = x;
    mouseClickYArray[mouseClickArrayOffset] = y;
    mouseClickArrayOffset = mouseClickArrayOffset + 1 & 0x1fff;
    for (int l = 10; l < 4000; l++) {
      int i1 = mouseClickArrayOffset - l & 0x1fff;
      if (mouseClickXArray[i1] == x && mouseClickYArray[i1] == y) {
        boolean flag = false;
        for (int j1 = 1; j1 < l; j1++) {
          int k1 = mouseClickArrayOffset - j1 & 0x1fff;
          int l1 = i1 - j1 & 0x1fff;
          if (mouseClickXArray[l1] != x || mouseClickYArray[l1] != y)
            flag = true;
          if (mouseClickXArray[k1] != mouseClickXArray[l1]
              || mouseClickYArray[k1] != mouseClickYArray[l1])
            break;
          if (j1 == l - 1 && flag && lastWalkTimeout == 0
              && logoutTimeout == 0) {
            logout();
            return;
          }
        }

      }
    }

  }

  protected final void method4() {
    if(smithingscreen == null) {
      smithingscreen = SmithingScreen.createScreen(mc);
      questionMenu = new QuestionMenu(mc);
      statMenu = new StatMenu(mc);
    }
    if (lastLoadedNull) {
      Graphics g = getGraphics();
      g.setColor(Color.black);
      g.fillRect(0, 0, gameWidth, gameHeight);
      g.setFont(new Font("Helvetica", 1, 16));
      g.setColor(Color.yellow);
      int i = 35;
      g.drawString("Sorry, an error has occured whilst loading RSCA", 30,
                   i);
      i += 50;
      g.setColor(Color.white);
      g.drawString("To fix this try the following (in order):", 30, i);
      i += 50;
      g.setColor(Color.white);
      g.setFont(new Font("Helvetica", 1, 12));
      g
      .drawString(
        "1: Try closing ALL open web-browser windows, and reloading",
        30, i);
      i += 30;
      g
      .drawString(
        "2: Try clearing your web-browsers cache from tools->internet options",
        30, i);
      i += 30;
      g.drawString("3: Try using a different game-world", 30, i);
      i += 30;
      g.drawString("4: Try rebooting your computer", 30, i);
      i += 30;
      g.drawString("5: Post on the DarkQuest forums under support", 30, i);
      changeThreadSleepModifier(1);
      return;
    }
    if (memoryError) {
      Graphics g2 = getGraphics();
      g2.setColor(Color.black);
      g2.fillRect(0, 0, gameWidth, gameHeight);
      g2.setFont(new Font("Helvetica", 1, 20));
      g2.setColor(Color.white);
      g2.drawString("Error - out of memory!", 50, 50);
      g2.drawString("Close ALL unnecessary programs", 50, 100);
      g2.drawString("and windows before loading the game", 50, 150);
      g2.drawString("RSCA needs about 100mb of spare RAM, 300+mb to Record video", 50, 200);
      changeThreadSleepModifier(1);
      return;
    }

    try {
      if (loggedIn == 1) {
        gameGraphics.drawStringShadows = true;
        try {
          drawGame();
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }
      } else {
        gameGraphics.drawStringShadows = false;
        drawLoginScreen();

      }
    } catch (OutOfMemoryError e) {
      e.printStackTrace();
      garbageCollect();
      memoryError = true;
    }
  }
  //drawGame
  private final void walkToObject(int x, int y, int id, int type) {
    int i1;
    int j1;
    if (id == 0 || id == 4) {
      i1 = EntityHandler.getObjectDef(type).getWidth();
      j1 = EntityHandler.getObjectDef(type).getHeight();
    } else {
      j1 = EntityHandler.getObjectDef(type).getWidth();
      i1 = EntityHandler.getObjectDef(type).getHeight();
    }
    if (EntityHandler.getObjectDef(type).getType() == 2
        || EntityHandler.getObjectDef(type).getType() == 3) {
      if (id == 0) {
        x--;
        i1++;
      }
      if (id == 2)
        j1++;
      if (id == 4)
        i1++;
      if (id == 6) {
        y--;
        j1++;
      }
      sendWalkCommand(getSectionX(), getSectionY(), x, y, (x + i1) - 1,
                      (y + j1) - 1, false, true);
      return;
    } else {
      sendWalkCommand(getSectionX(), getSectionY(), x, y, (x + i1) - 1,
                      (y + j1) - 1, true, true);
      return;
    }
  }

  private final void drawBankBox() {
    int c = 408;
    int c1 = 334;
    if (mouseOverBankPageText > 0 && bankItemCount <= 48)
      mouseOverBankPageText = 0;
    if (mouseOverBankPageText > 1 && bankItemCount <= 96)
      mouseOverBankPageText = 1;
    if (mouseOverBankPageText > 2 && bankItemCount <= 144)
      mouseOverBankPageText = 2;
    if (selectedBankItem >= bankItemCount || selectedBankItem < 0)
      selectedBankItem = -1;
    if (selectedBankItem != -1
        && bankItems[selectedBankItem] != selectedBankItemType) {
      selectedBankItem = -1;
      selectedBankItemType = -2;
    }
    if (mouseButtonClick != 0) {
      mouseButtonClick = 0;
      int i = super.mouseX - (256 - c / 2) - xAddition;
      int k = super.mouseY - (170 - c1 / 2) - yAddition;
      if (i >= 0 && k >= 12 && i < 408 && k < 280) {
        int i1 = mouseOverBankPageText * 48;
        for (int l1 = 0; l1 < 6; l1++) {
          for (int j2 = 0; j2 < 8; j2++) {
            int l6 = 7 + j2 * 49;
            int j7 = 28 + l1 * 34;
            if (i > l6 && i < l6 + 49 && k > j7 && k < j7 + 34
                && i1 < bankItemCount && bankItems[i1] != -1) {
              selectedBankItemType = bankItems[i1];
              selectedBankItem = i1;
            }
            i1++;
          }

        }

        i = xAddition + 256 - c / 2;
        k = yAddition + 170 - c1 / 2;
        int k2;
        if (selectedBankItem < 0)
          k2 = -1;
        else
          k2 = bankItems[selectedBankItem];
        if (k2 != -1) {
          int j1 = bankItemsCount[selectedBankItem];
          // if (!EntityHandler.getItemDef(k2).isStackable() && j1 >
          // 1)
          // j1 = 1;
          if (j1 >= 1 && super.mouseX >= i + 220
              && super.mouseY >= k + 238
              && super.mouseX < i + 250
              && super.mouseY <= k + 249) {
            super.streamClass.createPacket(183);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(1);
            super.streamClass.formatPacket();
          }
          if (j1 >= 10 && super.mouseX >= i + 250
              && super.mouseY >= k + 238
              && super.mouseX < i + 280
              && super.mouseY <= k + 249) {
            super.streamClass.createPacket(183);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(10);
            super.streamClass.formatPacket();
          }
          if (j1 >= 100 && super.mouseX >= i + 280
              && super.mouseY >= k + 238
              && super.mouseX < i + 305
              && super.mouseY <= k + 249) {
            super.streamClass.createPacket(183);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(100);
            super.streamClass.formatPacket();
          }
          if (j1 >= 1000 && super.mouseX >= i + 305
              && super.mouseY >= k + 238
              && super.mouseX < i + 335
              && super.mouseY <= k + 249) {
            super.streamClass.createPacket(183);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(1000);
            super.streamClass.formatPacket();
          }
          if (j1 >= 10000 && super.mouseX >= i + 335
              && super.mouseY >= k + 238
              && super.mouseX < i + 368
              && super.mouseY <= k + 249) {
            super.streamClass.createPacket(183);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(10000);
            super.streamClass.formatPacket();
          }
          if (super.mouseX >= i + 370 && super.mouseY >= k + 238
              && super.mouseX < i + 400
              && super.mouseY <= k + 249) {
            super.streamClass.createPacket(183);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(j1);
            super.streamClass.formatPacket();
          }

          if (inventoryCount(k2) >= 1 && super.mouseX >= i + 220
              && super.mouseY >= k + 263
              && super.mouseX < i + 250
              && super.mouseY <= k + 274) {
            super.streamClass.createPacket(198);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(1);
            super.streamClass.formatPacket();
          }
          if (inventoryCount(k2) >= 10 && super.mouseX >= i + 250
              && super.mouseY >= k + 263
              && super.mouseX < i + 280
              && super.mouseY <= k + 274) {
            super.streamClass.createPacket(198);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(10);
            super.streamClass.formatPacket();
          }
          if (inventoryCount(k2) >= 100 && super.mouseX >= i + 280
              && super.mouseY >= k + 263
              && super.mouseX < i + 305
              && super.mouseY <= k + 274) {
            super.streamClass.createPacket(198);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(100);
            super.streamClass.formatPacket();
          }
          if (inventoryCount(k2) >= 1000 && super.mouseX >= i + 305
              && super.mouseY >= k + 263
              && super.mouseX < i + 335
              && super.mouseY <= k + 274) {
            super.streamClass.createPacket(198);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(1000);
            super.streamClass.formatPacket();
          }
          if (inventoryCount(k2) >= 10000 && super.mouseX >= i + 335
              && super.mouseY >= k + 263
              && super.mouseX < i + 368
              && super.mouseY <= k + 274) {
            super.streamClass.createPacket(198);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(10000);
            super.streamClass.formatPacket();
          }
          if (super.mouseX >= i + 370 && super.mouseY >= k + 263
              && super.mouseX < i + 400
              && super.mouseY <= k + 274) {
            super.streamClass.createPacket(198);
            super.streamClass.add2ByteInt(k2);
            super.streamClass.add4ByteInt(inventoryCount(k2));
            super.streamClass.formatPacket();
          }
        }
      } else if (bankItemCount > 48 && i >= 50 && i <= 115 && k <= 12)
        mouseOverBankPageText = 0;
      else if (bankItemCount > 48 && i >= 115 && i <= 180 && k <= 12)
        mouseOverBankPageText = 1;
      else if (bankItemCount > 96 && i >= 180 && i <= 245 && k <= 12)
        mouseOverBankPageText = 2;
      else if (bankItemCount > 144 && i >= 245 && i <= 310 && k <= 12) {
        mouseOverBankPageText = 3;
      } else {
        super.streamClass.createPacket(48);
        super.streamClass.formatPacket();
        showBank = false;
        return;
      }
    }
    int j = xAddition + 256 - c / 2;
    int l = yAddition + 170 - c1 / 2;
    gameGraphics.drawBox(j, l, 408, 12, 192);
    int k1 = 0x989898;
    gameGraphics.drawBoxAlpha(j, l + 12, 408, 17, k1, 160);
    gameGraphics.drawBoxAlpha(j, l + 29, 8, 204, k1, 160);
    gameGraphics.drawBoxAlpha(j + 399, l + 29, 9, 204, k1, 160);
    gameGraphics.drawBoxAlpha(j, l + 233, 408, 47, k1, 160);
    gameGraphics.drawString("Bank", j + 1, l + 10, 1, 0xffffff);
    int i2 = 50;
    if (bankItemCount > 48) {
      int l2 = 0xffffff;
      if (mouseOverBankPageText == 0)
        l2 = 0xff0000;
      else if (super.mouseX > j + i2 && super.mouseY >= l
               && super.mouseX < j + i2 + 65 && super.mouseY < l + 12)
        l2 = 0xffff00;
      gameGraphics.drawString("<page 1>", j + i2, l + 10, 1, l2);
      i2 += 65;
      l2 = 0xffffff;
      if (mouseOverBankPageText == 1)
        l2 = 0xff0000;
      else if (super.mouseX > j + i2 && super.mouseY >= l
               && super.mouseX < j + i2 + 65 && super.mouseY < l + 12)
        l2 = 0xffff00;
      gameGraphics.drawString("<page 2>", j + i2, l + 10, 1, l2);
      i2 += 65;
    }
    if (bankItemCount > 96) {
      int i3 = 0xffffff;
      if (mouseOverBankPageText == 2)
        i3 = 0xff0000;
      else if (super.mouseX > j + i2 && super.mouseY >= l
               && super.mouseX < j + i2 + 65 && super.mouseY < l + 12)
        i3 = 0xffff00;
      gameGraphics.drawString("<page 3>", j + i2, l + 10, 1, i3);
      i2 += 65;
    }
    if (bankItemCount > 144) {
      int j3 = 0xffffff;
      if (mouseOverBankPageText == 3)
        j3 = 0xff0000;
      else if (super.mouseX > j + i2 && super.mouseY >= l
               && super.mouseX < j + i2 + 65 && super.mouseY < l + 12)
        j3 = 0xffff00;
      gameGraphics.drawString("<page 4>", j + i2, l + 10, 1, j3);
      i2 += 65;
    }
    int k3 = 0xffffff;
    if (super.mouseX > j + 320 && super.mouseY >= l
        && super.mouseX < j + 408 && super.mouseY < l + 12)
      k3 = 0xff0000;
    gameGraphics.drawBoxTextRight("Close window", j + 406, l + 10, 1, k3);
    gameGraphics.drawString("Number in bank in green", j + 7, l + 24, 1,
                            65280);
    gameGraphics.drawString("Number held in blue", j + 289, l + 24, 1,
                            65535);
    int i7 = 0xd0d0d0;
    int k7 = mouseOverBankPageText * 48;
    for (int i8 = 0; i8 < 6; i8++) {
      for (int j8 = 0; j8 < 8; j8++) {
        int l8 = j + 7 + j8 * 49;
        int i9 = l + 28 + i8 * 34;
        if (selectedBankItem == k7)
          gameGraphics.drawBoxAlpha(l8, i9, 49, 34, 0xff0000, 160);
        else
          gameGraphics.drawBoxAlpha(l8, i9, 49, 34, i7, 160);
        gameGraphics.drawBoxEdge(l8, i9, 50, 35, 0);
        if (k7 < bankItemCount && bankItems[k7] != -1) {
          gameGraphics.spriteClip4(l8, i9, 48, 32, SPRITE_ITEM_START
                                   + EntityHandler.getItemDef(bankItems[k7])
                                   .getSprite(), EntityHandler.getItemDef(
                                     bankItems[k7]).getPictureMask(), 0, 0, false);
          gameGraphics.drawString(String.valueOf(bankItemsCount[k7]),
                                  l8 + 1, i9 + 10, 1, 65280);
          gameGraphics.drawBoxTextRight(String
                                        .valueOf(inventoryCount(bankItems[k7])), l8 + 47,
                                        i9 + 29, 1, 65535);
        }
        k7++;
      }

    }

    gameGraphics.drawLineX(j + 5, l + 256, 398, 0);
    if (selectedBankItem == -1) {
      gameGraphics.drawText("Select an object to withdraw or deposit",
                            j + 204, l + 248, 3, 0xffff00);
      return;
    }
    int k8;
    if (selectedBankItem < 0)
      k8 = -1;
    else
      k8 = bankItems[selectedBankItem];
    if (k8 != -1) {
      int l7 = bankItemsCount[selectedBankItem];
      // if (!EntityHandler.getItemDef(k8).isStackable() && l7 > 1)
      // l7 = 1;
      if (l7 > 0) {
        gameGraphics.drawString("Withdraw "
                                + EntityHandler.getItemDef(k8).getName(), j + 2,
                                l + 248, 1, 0xffffff);
        int l3 = 0xffffff;
        if (super.mouseX >= j + 220 && super.mouseY >= l + 238
            && super.mouseX < j + 250 && super.mouseY <= l + 249)
          l3 = 0xff0000;
        gameGraphics.drawString("One", j + 222, l + 248, 1, l3);
        if (l7 >= 10) {
          int i4 = 0xffffff;
          if (super.mouseX >= j + 250 && super.mouseY >= l + 238
              && super.mouseX < j + 280
              && super.mouseY <= l + 249)
            i4 = 0xff0000;
          gameGraphics.drawString("10", j + 252, l + 248, 1, i4);
        }
        if (l7 >= 100) {
          int j4 = 0xffffff;
          if (super.mouseX >= j + 280 && super.mouseY >= l + 238
              && super.mouseX < j + 305
              && super.mouseY <= l + 249)
            j4 = 0xff0000;
          gameGraphics.drawString("100", j + 282, l + 248, 1, j4);
        }
        if (l7 >= 1000) {
          int k4 = 0xffffff;
          if (super.mouseX >= j + 305 && super.mouseY >= l + 238
              && super.mouseX < j + 335
              && super.mouseY <= l + 249)
            k4 = 0xff0000;
          gameGraphics.drawString("1k", j + 307, l + 248, 1, k4);
        }
        if (l7 >= 10000) {
          int l4 = 0xffffff;
          if (super.mouseX >= j + 335 && super.mouseY >= l + 238
              && super.mouseX < j + 368
              && super.mouseY <= l + 249)
            l4 = 0xff0000;
          gameGraphics.drawString("10k", j + 337, l + 248, 1, l4);
        }
        int i5 = 0xffffff;
        if (super.mouseX >= j + 370 && super.mouseY >= l + 238
            && super.mouseX < j + 400 && super.mouseY <= l + 249)
          i5 = 0xff0000;
        gameGraphics.drawString("All", j + 370, l + 248, 1, i5);
      }
      if (inventoryCount(k8) > 0) {
        gameGraphics.drawString("Deposit "
                                + EntityHandler.getItemDef(k8).getName(), j + 2,
                                l + 273, 1, 0xffffff);
        int j5 = 0xffffff;
        if (super.mouseX >= j + 220 && super.mouseY >= l + 263
            && super.mouseX < j + 250 && super.mouseY <= l + 274)
          j5 = 0xff0000;
        gameGraphics.drawString("One", j + 222, l + 273, 1, j5);
        if (inventoryCount(k8) >= 10) {
          int k5 = 0xffffff;
          if (super.mouseX >= j + 250 && super.mouseY >= l + 263
              && super.mouseX < j + 280
              && super.mouseY <= l + 274)
            k5 = 0xff0000;
          gameGraphics.drawString("10", j + 252, l + 273, 1, k5);
        }
        if (inventoryCount(k8) >= 100) {
          int l5 = 0xffffff;
          if (super.mouseX >= j + 280 && super.mouseY >= l + 263
              && super.mouseX < j + 305
              && super.mouseY <= l + 274)
            l5 = 0xff0000;
          gameGraphics.drawString("100", j + 282, l + 273, 1, l5);
        }
        if (inventoryCount(k8) >= 1000) {
          int i6 = 0xffffff;
          if (super.mouseX >= j + 305 && super.mouseY >= l + 263
              && super.mouseX < j + 335
              && super.mouseY <= l + 274)
            i6 = 0xff0000;
          gameGraphics.drawString("1k", j + 307, l + 273, 1, i6);
        }
        if (inventoryCount(k8) >= 10000) {
          int j6 = 0xffffff;
          if (super.mouseX >= j + 335 && super.mouseY >= l + 263
              && super.mouseX < j + 368
              && super.mouseY <= l + 274)
            j6 = 0xff0000;
          gameGraphics.drawString("10k", j + 337, l + 273, 1, j6);
        }
        int k6 = 0xffffff;
        if (super.mouseX >= j + 370 && super.mouseY >= l + 263
            && super.mouseX < j + 400 && super.mouseY <= l + 274)
          k6 = 0xff0000;
        gameGraphics.drawString("All", j + 370, l + 273, 1, k6);
      }
    }
  }

  private final void drawLoggingOutBox() {
    gameGraphics.drawBox(126 + xAddition, 137 + yAddition, 260, 60, 0);
    gameGraphics.drawBoxEdge(126 + xAddition, 137 + yAddition, 260, 60, 0xffffff);
    gameGraphics.drawText("Logging out...", 256 + xAddition, 173 + yAddition, 5, 0xffffff);
  }

  private final void drawInventoryMenu(boolean flag) {
    int i = ((GameImage) (gameGraphics)).menuDefaultWidth - 248;
    gameGraphics.drawPicture(i, 3, SPRITE_MEDIA_START + 1);
    for (int j = 0; j < anInt882; j++) {
      int k = i + (j % 5) * 49;
      int i1 = 36 + (j / 5) * 34;
      if (j < inventoryCount && wearing[j] == 1)
        gameGraphics.drawBoxAlpha(k, i1, 49, 34, 0xff0000, 128);
      else
        gameGraphics.drawBoxAlpha(k, i1, 49, 34, GameImage
                                  .convertRGBToLong(181, 181, 181), 128);
      if (j < inventoryCount) {
        gameGraphics.spriteClip4(k, i1, 48, 32, SPRITE_ITEM_START
                                 + EntityHandler.getItemDef(getInventoryItems()[j])
                                 .getSprite(), EntityHandler.getItemDef(
                                   getInventoryItems()[j]).getPictureMask(), 0, 0, false);
        if (EntityHandler.getItemDef(getInventoryItems()[j]).isStackable())
          gameGraphics.drawString(String
                                  .valueOf(inventoryItemsCount[j]), k + 1, i1 + 10,
                                  1, 0xffff00);
      }
    }

    for (int l = 1; l <= 4; l++)
      gameGraphics.drawLineY(i + l * 49, 36, (anInt882 / 5) * 34, 0);

    for (int j1 = 1; j1 <= anInt882 / 5 - 1; j1++)
      gameGraphics.drawLineX(i, 36 + j1 * 34, 245, 0);

    if (!flag)
      return;
    i = super.mouseX
        - (((GameImage) (gameGraphics)).menuDefaultWidth - 248);
    int k1 = super.mouseY - 36;
    if (i >= 0 && k1 >= 0 && i < 248 && k1 < (anInt882 / 5) * 34) {
      int currentInventorySlot = i / 49 + (k1 / 34) * 5;
      if (currentInventorySlot < inventoryCount) {
        int i2 = getInventoryItems()[currentInventorySlot];
        ItemDef itemDef = EntityHandler.getItemDef(i2);
        if (selectedSpell >= 0) {
          if (EntityHandler.getSpellDef(selectedSpell).getSpellType() == 3) {
            menuText1[menuLength] = "Cast "
                                    + EntityHandler.getSpellDef(selectedSpell)
                                    .getName() + " on";
            menuText2[menuLength] = "@lre@" + itemDef.getName();
            menuID[menuLength] = 600;
            menuActionType[menuLength] = currentInventorySlot;
            menuActionVariable[menuLength] = selectedSpell;
            menuLength++;
            return;
          }
        } else {//::
          if (selectedItem >= 0) {
            menuText1[menuLength] = "Use " + selectedItemName
                                    + " with";
            menuText2[menuLength] = "@lre@" + itemDef.getName();
            menuID[menuLength] = 610;
            menuActionType[menuLength] = currentInventorySlot;
            menuActionVariable[menuLength] = selectedItem;
            menuLength++;
            return;
          }
          if (wearing[currentInventorySlot] == 1) {
            menuText1[menuLength] = "Remove";
            menuText2[menuLength] = "@lre@" + itemDef.getName();
            menuID[menuLength] = 620;
            menuActionType[menuLength] = currentInventorySlot;
            menuLength++;
          } else if (EntityHandler.getItemDef(i2).isWieldable()) {
            menuText1[menuLength] = "Wear";
            menuText2[menuLength] = "@lre@" + itemDef.getName();
            menuID[menuLength] = 630;
            menuActionType[menuLength] = currentInventorySlot;
            menuLength++;
          }
          if (!itemDef.getCommand().equals("")) {
            menuText1[menuLength] = itemDef.getCommand();
            menuText2[menuLength] = "@lre@" + itemDef.getName();
            menuID[menuLength] = 640;
            menuActionType[menuLength] = currentInventorySlot;
            menuLength++;
          }
          menuText1[menuLength] = "Use";
          menuText2[menuLength] = "@lre@" + itemDef.getName();
          menuID[menuLength] = 650;
          menuActionType[menuLength] = currentInventorySlot;
          menuLength++;
          menuText1[menuLength] = "Drop";
          menuText2[menuLength] = "@lre@" + itemDef.getName();
          menuID[menuLength] = 660;
          menuActionType[menuLength] = currentInventorySlot;
          menuLength++;
          menuText1[menuLength] = "Examine";
          menuText2[menuLength] = "@lre@"
                                  + itemDef.getName()
                                  + (ourPlayer.admin >= 1 ? " @or1@(" + i2 + ")" : "");
          menuID[menuLength] = 3600;
          menuActionType[menuLength] = i2;
          menuLength++;
        }
      }
    }
  }

  private final void drawChatMessageTabs() {
    int x = xAddition;

    if(windowWidth > 512) {
      gameGraphics.spriteClip4(512, windowHeight, windowWidth - 512, 10, SPRITE_MEDIA_START + 22, 0, 0, 0, false);
      gameGraphics.spriteClip4(0, windowHeight, windowWidth - 512, 10, SPRITE_MEDIA_START + 22, 0, 0, 0, false);
    }


    gameGraphics.drawPicture(0 + x, windowHeight - 4, SPRITE_MEDIA_START + 23);


    int i = GameImage.convertRGBToLong(200, 200, 255);
    if (messagesTab == 0)
      i = GameImage.convertRGBToLong(255, 200, 50);
    if (anInt952 % 30 > 15)
      i = GameImage.convertRGBToLong(255, 50, 50);
    gameGraphics.drawText("All messages", 54 + x, windowHeight + 6, 0, i);
    i = GameImage.convertRGBToLong(200, 200, 255);
    if (messagesTab == 1)
      i = GameImage.convertRGBToLong(255, 200, 50);
    if (anInt953 % 30 > 15)
      i = GameImage.convertRGBToLong(255, 50, 50);
    gameGraphics.drawText("Chat history", 155 + x, windowHeight + 6, 0, i);
    i = GameImage.convertRGBToLong(200, 200, 255);
    if (messagesTab == 2)
      i = GameImage.convertRGBToLong(255, 200, 50);
    if (anInt954 % 30 > 15)
      i = GameImage.convertRGBToLong(255, 50, 50);
    gameGraphics.drawText("Quest history", 255 + x, windowHeight + 6, 0, i);
    i = GameImage.convertRGBToLong(200, 200, 255);
    if (messagesTab == 3)
      i = GameImage.convertRGBToLong(255, 200, 50);
    if (anInt955 % 30 > 15)
      i = GameImage.convertRGBToLong(255, 50, 50);
    gameGraphics.drawText("Private history", 355 + x, windowHeight + 6, 0, i);
    gameGraphics.drawText("Report abuse", 457 + x, windowHeight + 6, 0,
                          0xffffff);
  }

  private final void method62() {
    gameGraphics.f1Toggle = false;
    gameGraphics.method211();
    characterDesignMenu.drawMenu();
    int i = 140;
    int j = 50;
    i += 116;
    j -= 25;
    gameGraphics.spriteClip3(i - 32 - 55, j, 64, 102, EntityHandler
                             .getAnimationDef(character2Colour).getNumber(),
                             characterTopBottomColours[characterBottomColour]);
    gameGraphics.spriteClip4(i - 32 - 55, j, 64, 102, EntityHandler
                             .getAnimationDef(characterBodyGender).getNumber(),
                             characterTopBottomColours[characterTopColour],
                             characterSkinColours[characterSkinColour], 0, false);
    gameGraphics.spriteClip4(i - 32 - 55, j, 64, 102, EntityHandler
                             .getAnimationDef(characterHeadType).getNumber(),
                             characterHairColours[characterHairColour],
                             characterSkinColours[characterSkinColour], 0, false);
    gameGraphics.spriteClip3(i - 32, j, 64, 102, EntityHandler
                             .getAnimationDef(character2Colour).getNumber() + 6,
                             characterTopBottomColours[characterBottomColour]);
    gameGraphics.spriteClip4(i - 32, j, 64, 102, EntityHandler
                             .getAnimationDef(characterBodyGender).getNumber() + 6,
                             characterTopBottomColours[characterTopColour],
                             characterSkinColours[characterSkinColour], 0, false);
    gameGraphics.spriteClip4(i - 32, j, 64, 102, EntityHandler
                             .getAnimationDef(characterHeadType).getNumber() + 6,
                             characterHairColours[characterHairColour],
                             characterSkinColours[characterSkinColour], 0, false);
    gameGraphics.spriteClip3((i - 32) + 55, j, 64, 102, EntityHandler
                             .getAnimationDef(character2Colour).getNumber() + 12,
                             characterTopBottomColours[characterBottomColour]);
    gameGraphics.spriteClip4((i - 32) + 55, j, 64, 102, EntityHandler
                             .getAnimationDef(characterBodyGender).getNumber() + 12,
                             characterTopBottomColours[characterTopColour],
                             characterSkinColours[characterSkinColour], 0, false);
    gameGraphics.spriteClip4((i - 32) + 55, j, 64, 102, EntityHandler
                             .getAnimationDef(characterHeadType).getNumber() + 12,
                             characterHairColours[characterHairColour],
                             characterSkinColours[characterSkinColour], 0, false);
    gameGraphics.spriteClip4(0, windowHeight, windowWidth, 10, SPRITE_MEDIA_START + 22, 0, 0, 0, false);
    gameGraphics.drawImage(aGraphics936, 0, 0);
  }

  private final Mob makePlayer(int mobArrayIndex, int x, int y, int sprite) {
    if (mobArray[mobArrayIndex] == null) {
      mobArray[mobArrayIndex] = new Mob();
      mobArray[mobArrayIndex].serverIndex = mobArrayIndex;
      mobArray[mobArrayIndex].mobIntUnknown = 0;
    }
    Mob mob = mobArray[mobArrayIndex];
    boolean flag = false;
    for (int i1 = 0; i1 < lastPlayerCount; i1++) {
      if (lastPlayerArray[i1].serverIndex != mobArrayIndex)
        continue;
      flag = true;
      break;
    }

    if (flag) {
      mob.nextSprite = sprite;
      int j1 = mob.waypointCurrent;
      if (x != mob.waypointsX[j1] || y != mob.waypointsY[j1]) {
        mob.waypointCurrent = j1 = (j1 + 1) % 10;
        mob.waypointsX[j1] = x;
        mob.waypointsY[j1] = y;
      }
    } else {
      mob.serverIndex = mobArrayIndex;
      mob.waypointEndSprite = 0;
      mob.waypointCurrent = 0;
      mob.waypointsX[0] = mob.currentX = x;
      mob.waypointsY[0] = mob.currentY = y;
      mob.nextSprite = mob.currentSprite = sprite;
      mob.stepCount = 0;
    }
    playerArray[playerCount++] = mob;
    return mob;
  }

  private final void drawWelcomeBox() {
    int i = 65;
    if (!lastLoggedInAddress.equals("0.0.0.0"))
      i += 30;
    if (subscriptionLeftDays > 0)
      i += 15;
    int j = 167 - i / 2 + yAddition;
    gameGraphics.drawBox(56 + xAddition, 167 - i / 2 + yAddition, 400, i, 0);
    gameGraphics.drawBoxEdge(56 + xAddition, 167 - i / 2 + yAddition, 400, i, 0xffffff);
    j += 20;
    gameGraphics.drawText("Welcome to DQ " + currentUser, 256 + xAddition, j, 4,
                          0xffff00);
    j += 30;
    String s;
    if (lastLoggedInDays == 0)
      s = "earlier today";
    else if (lastLoggedInDays == 1)
      s = "yesterday";
    else
      s = lastLoggedInDays + " days ago";
    if (!lastLoggedInAddress.equals("0.0.0.0")) {
      gameGraphics.drawText("You last logged in " + s, 256 + xAddition, j, 1,
                            0xffffff);
      j += 15;
      gameGraphics.drawText("from: " + lastLoggedInAddress, 256 + xAddition, j, 1,
                            0xffffff);
      j += 15;
    }
    if (subscriptionLeftDays > 0) {
      gameGraphics.drawText("Subscription Left: " + subscriptionLeftDays
                            + " days", 256 + xAddition, j, 1, 0xffffff);
      j += 15;
    }
    int l = 0xffffff;
    if (super.mouseY > j - 12 && super.mouseY <= j && super.mouseX > 106 + xAddition
        && super.mouseX < 406 + xAddition)
      l = 0xff0000;
    gameGraphics.drawText("Click here to close window", 256 + xAddition, j, 1, l);
    if (mouseButtonClick == 1) {
      if (l == 0xff0000)
        showWelcomeBox = false;
      if ((super.mouseX < 86 + xAddition || super.mouseX > 426 + xAddition)
          && (super.mouseY < 167 - i / 2 || super.mouseY > 167 + i / 2))
        showWelcomeBox = false;
    }
    mouseButtonClick = 0;
  }

  private final void logout() {
    if (loggedIn == 0) {
      return;
    }
    if (lastWalkTimeout > 450) {
      displayMessage("@cya@You can't logout during combat!", 3, 0);
      return;
    }
    if (lastWalkTimeout > 0) {
      displayMessage("@cya@You can't logout for 10 seconds after combat",
                     3, 0);
      return;
    }
    super.streamClass.createPacket(129);
    super.streamClass.formatPacket();
    logoutTimeout = 1000;
  }

  private final void drawPlayerInfoMenu(boolean flag) {
    int i = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
    int j = 36;
    gameGraphics.drawPicture(i - 49, 3, SPRITE_MEDIA_START + 3);
    char c = '\304';
    char c1 = '\u0113';
    int l;
    int s;
    int k = s = l = GameImage.convertRGBToLong(160, 160, 160);
    if (anInt826 == 0)
      k = GameImage.convertRGBToLong(220, 220, 220);
    else if (anInt826 == 1)
      l = GameImage.convertRGBToLong(220, 220, 220);
    else if (anInt826 == 2)
      s = GameImage.convertRGBToLong(220, 220, 220);

    gameGraphics.drawBoxAlpha(i, j, c / 3, 24, k, 128);
    gameGraphics.drawBoxAlpha(i + c / 3, j, c / 3, 24, l, 128);
    gameGraphics.drawBoxAlpha(i + c / 3 + c / 3, j, c / 3, 24, s, 128);
    gameGraphics.drawBoxAlpha(i, j + 24, c, c1 - 12 + 13, GameImage
                              .convertRGBToLong(220, 220, 220), 128);
    gameGraphics.drawLineX(i, j + 24, c, 0);
    gameGraphics.drawLineY(i + c / 3, j, 24, 0);
    gameGraphics.drawLineY(i + c / 3 + c / 3, j, 24, 0);

    gameGraphics.drawText(" Stats", i + c / 8 + 2, j + 16, 4, 0);
    gameGraphics.drawText(" Quests", i + c / 8 + c / 3 + 4, j + 16, 4, 0);
    gameGraphics.drawText(" Info", i + c / 8 + c / 3 + c / 3 + 2, j + 16, 4,
                          0);
    if (anInt826 == 0) {
      int i1 = 72;
      int k1 = -1;
      gameGraphics.drawString("Skills", i + 5, i1, 3, 0xffff00);
      i1 += 13;
      for (int l1 = 0; l1 < 9; l1++) {
        int i2 = 0xffffff;
        if (super.mouseX > i + 3 && super.mouseY >= i1 - 11
            && super.mouseY < i1 + 2 && super.mouseX < i + 90) {
          i2 = 0xff0000;
          k1 = l1;
        }
        gameGraphics.drawString(skillArray[l1] + ":@yel@"
                                + playerStatCurrent[l1] + "/" + playerStatBase[l1],
                                i + 5, i1, 1, i2);
        if(l1 == 8) {
          gameGraphics.drawString("Fatigue: @yel@" + fatigue + "%",
                                  i + 5, i1 + 13, 1, 0xffffff);

          gameGraphics.drawString("@whi@[@red@ Open stat panel @whi@]", i + 91 - 50, i1 + 13 + 14, 1, 0xffffff);
          if (mouseButtonClick == 1) {
            if(super.mouseX > i + 91 - 50 && super.mouseX < i + 91 + 100 - 50 && super.mouseY > i1 + 13 && super.mouseY < i1 + 13 + 14) {
              mouseOverMenu = 0;
              statMenu.open();

            }
          }
        }
        i2 = 0xffffff;
        if (super.mouseX >= i + 90 && super.mouseY >= i1 - 11 - 13
            && super.mouseY < i1 + 2 - 13 && super.mouseX < i + 196) {
          i2 = 0xff0000;
          k1 = l1 + 9;
        }
        gameGraphics.drawString(skillArray[l1 + 9] + ":@yel@"
                                + playerStatCurrent[l1 + 9] + "/"
                                + playerStatBase[l1 + 9], (i + c / 2) - 5, i1 - 13, 1, i2);
        i1 += 13;
        //Total xp:
        if(l1 == 8) {
          gameGraphics.drawString("Quest Points:@yel@" + questPoints, (i + c / 2) - 5, i1 - 13, 1, 0xffffff);
          i1 += 13;
        }
      }
      i1 += 8;
      i1 += 8;
      gameGraphics.drawString("Equipment Status", i + 5, i1, 3, 0xffff00);
      i1 += 12;
      for (int j2 = 0; j2 < 3; j2++) {
        gameGraphics.drawString(equipmentStatusName[j2] + ":@yel@"
                                + equipmentStatus[j2], i + 5, i1, 1, 0xffffff);
        if(j2 < 2) {
          gameGraphics.drawString(equipmentStatusName[j2 + 3] + ":@yel@"
                                  + equipmentStatus[j2 + 3], i + c / 2 + 25, i1, 1,
                                  0xffffff);

        }
        i1 += 13;
      }

      i1 += 6;
      gameGraphics.drawLineX(i, i1 - 15, c, 0);
      if (k1 != -1) {
        gameGraphics.drawString(skillArrayLong[k1] + " skill", i + 5,
                                i1, 1, 0xffff00);
        i1 += 12;
        int k2 = experienceArray[0];
        for (int i3 = 0; i3 < 98; i3++)
          if (playerStatExperience[k1] >= experienceArray[i3])
            k2 = experienceArray[i3 + 1];

        gameGraphics.drawString(
          "Total xp: " + playerStatExperience[k1], i + 5, i1, 1,
          0xffffff);
        i1 += 12;
        gameGraphics.drawString("Next level at: " + k2, i + 5, i1, 1,
                                0xffffff);
        i1 += 12;
        gameGraphics.drawString("Required xp: "
                                + (k2 - playerStatExperience[k1]), i + 5, i1, 1,
                                0xffffff);
        i1 += 12;
      } else {
        gameGraphics.drawString("Overall levels", i + 5, i1, 1,
                                0xffff00);
        i1 += 12;
        int skillTotal = 0;
        long expTotal = 0;
        for (int j3 = 0; j3 < 18; j3++) {
          skillTotal += playerStatBase[j3];
          expTotal += playerStatExperience[j3];
        }
        gameGraphics.drawString("Skill total: " + skillTotal, i + 5, i1, 1, 0xffffff);

        i1 += 12;
        gameGraphics.drawString("Combat XP: @gre@" + cache.get("xppoints"), i + 5, i1, 1, 0xffffff);


        //Max hit
        i1 += 12;
        float at = playerStatBase[0];
        float de = playerStatBase[1];
        float st = playerStatBase[2];
        float hp = playerStatBase[3];
        float ra = playerStatBase[4];
        float pr = playerStatBase[5];
        float ma = playerStatBase[6];
        if (ra * 1.5 > st + at) {
          double cmb = (de / 4) + (hp / 4) + (pr / 8) + (ma / 8)
                       + (ra / 2.66); // forumla for a ranged based
          // character
          gameGraphics.drawString("Combat level: "
                                  + (Math.round(cmb * 100.0) / 100.0)
                                  , i + 5, i1, 1, 0xffffff);
        } else {
          float cmb = (st / 4) + (de / 4) + (hp / 4) + (at / 4)
                      + (pr / 8) + (ma / 8); // formula for a combat based
          // character
          gameGraphics.drawString("Combat level: " + cmb, i + 5, i1, 1, 0xffffff);
        }


        i1 += 13;
      }
    }
    if (anInt826 == 1) {

      int i1 = 72;
      gameGraphics.drawString("Quest-list (green=completed)", i + 5, i1,
                              1, 0xffffff);
      questMenu.resetListTextCount(questMenuHandle);

      int index = 0;
      for(int questIdx :  newQuestNames.keySet())
        questMenu.drawMenuListText(questMenuHandle, index++, (questStage.get(questIdx) == 0 ? "@red@" : questStage.get(questIdx) > 0 ? "@yel@" : "@gre@") + newQuestNames.get(questIdx));
      try {
        questMenu.drawMenu();
      } catch(Exception e) {
        e.printStackTrace();
      }
    } else if (anInt826 == 2) {
      int i1 = 72; // Player Info
      gameGraphics.drawString("Player Info", i + 5, i1, 3, 0xffff00);
      i1 += 13;
      gameGraphics.drawString("Username:@yel@ " + ourPlayer.name, i + 5,
                              i1, 1, 0xffffff);
      i1 += 13;
      gameGraphics.drawString("Coords:@yel@ (" + (getSectionX() + getAreaX())
                              + ", " + (getSectionY() + getAreaY()) + ")", i + 5, i1, 1, 0xffffff);
      i1 += 13;
      gameGraphics.drawString("Server Index:@yel@ "
                              + ourPlayer.serverIndex, i + 5, i1, 1, 0xffffff);
      i1 += 13;
      gameGraphics.drawString(
        "Exp Gained:@yel@ "
        + (expGained > 1000 ? (expGained / 1000) + "k"
           : expGained), i + 5, i1, 1, 0xffffff);
      if (!lastLoggedInAddress.equals("0.0.0.0")) {
        i1 += 13;
        gameGraphics.drawString("Last IP:@yel@ " + lastLoggedInAddress,
                                i + 5, i1, 1, 0xffffff);
      }
      i1 += 13; // Client Info
      double prayerBonus = 1.0;

      if (prayerOn[1])
        prayerBonus = 1.05;
      else if (prayerOn[4])
        prayerBonus = 1.1;
      else if (prayerOn[10])
        prayerBonus = 1.15;

      int modeBonus = 0;

      if (combatStyle == 0)
        modeBonus = 1;
      else if (combatStyle == 1)
        modeBonus = 3;

      double newStrength = (double)((playerStatCurrent[2] * prayerBonus) + modeBonus);
      int maxHit = (int) ((newStrength
                           * ((((double) equipmentStatus[2] * 0.00175D) + 0.1D)) + 1.05D) * 0.95D);


      gameGraphics.drawString("Max hit:@yel@ " + maxHit, i + 5, i1, 1,
                              0xffffff);
      i1 += 13;
      long expTotal = 0;
      for (int j3 = 0; j3 < 18; j3++) {
        expTotal += playerStatExperience[j3];
      }
      gameGraphics.drawString("Total XP:@yel@ " + expTotal, i + 5, i1, 1,
                              0xffffff);
      i1 += 21;
      gameGraphics.drawString("Client Info", i + 5, i1, 3, 0xffff00);
      i1 += 13;
      gameGraphics.drawString("Hostname:@yel@ " + localhost, i + 5, i1,
                              1, 0xffffff);
      i1 += 13;
      gameGraphics.drawString("Uptime:@yel@ " + timeSince(startTime),
                              i + 5, i1, 1, 0xffffff);
      i1 += 13;
      gameGraphics.drawString("FPS:@yel@ " + fps, i + 5, i1, 1, 0xffffff);
      i1 += 21; // Server Info
      gameGraphics.drawString("Server Info", i + 5, i1, 3, 0xffff00);
      i1 += 13;
      gameGraphics.drawString("Hostname:@yel@ " + Config.SERVER_IP,
                              i + 5, i1, 1, 0xffffff);
      i1 += 13;
      gameGraphics.drawString("Uptime:@yel@ "
                              + timeSince(serverStartTime), i + 5, i1, 1, 0xffffff);
      i1 += 13;
      gameGraphics.drawString("Location:@yel@ " + serverLocation, i + 5,
                              i1, 1, 0xffffff);
      i1 += 13;
    }
    if (!flag) {
      return;
    }

    i = super.mouseX
        - (((GameImage) (gameGraphics)).menuDefaultWidth - 199);
    j = super.mouseY - 36;
    if (i >= 0 && j >= 0 && i < c && j < c1) {
      if (anInt826 == 1)
        questMenu
        .updateActions(
          i
          + (((GameImage) (gameGraphics)).menuDefaultWidth - 199),
          j + 36, super.lastMouseDownButton,
          super.mouseDownButton);

      if (j <= 24 && mouseButtonClick == 1) {
        if (i < 65) {
          anInt826 = 0;
        } else if (i >= 65 && i < 130) {
          anInt826 = 1;
        } else if (i >= 130) {
          anInt826 = 2;
        }
      }
    }
  }

  private final void drawWildernessWarningBox() {
    int i = 97 + yAddition;
    gameGraphics.drawBox(86 + xAddition, 77 + yAddition, 340, 180, 0);
    gameGraphics.drawBoxEdge(86 + xAddition, 77 + yAddition, 340, 180, 0xffffff);
    gameGraphics.drawText("Warning! Proceed with caution", 256 + xAddition, i, 4,
                          0xff0000);
    i += 26;
    gameGraphics.drawText(
      "If you go much further north you will enter the", 256 + xAddition, i, 1,
      0xffffff);
    i += 13;
    gameGraphics.drawText("wilderness. This a very dangerous area where",
                          256 + xAddition, i, 1, 0xffffff);
    i += 13;
    gameGraphics.drawText("other players can attack you!", 256 + xAddition, i, 1,
                          0xffffff);
    i += 22;
    gameGraphics.drawText("The further north you go the more dangerous it",
                          256 + xAddition, i, 1, 0xffffff);
    i += 13;
    gameGraphics.drawText("becomes, but the more treasure you will find.",
                          256 + xAddition, i, 1, 0xffffff);
    i += 22;
    gameGraphics.drawText(
      "In the wilderness an indicator at the bottom-right", 256 + xAddition, i,
      1, 0xffffff);
    i += 13;
    gameGraphics.drawText(
      "of the screen will show the current level of danger", 256 + xAddition, i,
      1, 0xffffff);
    i += 22;
    int j = 0xffffff;
    if (super.mouseY > i - 12 && super.mouseY <= i && super.mouseX > 181 + xAddition
        && super.mouseX < 331 + xAddition)
      j = 0xff0000;
    gameGraphics.drawText("Click here to close window", 256 + xAddition, i, 1, j);
    if (mouseButtonClick != 0) {
      if (super.mouseY > i - 12 && super.mouseY <= i
          && super.mouseX > 181 + xAddition && super.mouseX < 331 + xAddition)
        wildernessType = 2;
      if (super.mouseX < 86 + xAddition || super.mouseX > 426 + xAddition || super.mouseY < 77 + yAddition
          || super.mouseY > 257 + yAddition)
        wildernessType = 2;
      mouseButtonClick = 0;
    }
  }

  final void method68(int i, int j, int k, int l, int i1, int j1, int k1) {
    int l1 = EntityHandler.getItemDef(i1).getSprite() + SPRITE_ITEM_START;
    int i2 = EntityHandler.getItemDef(i1).getPictureMask();
    gameGraphics.spriteClip4(i, j, k, l, l1, i2, 0, 0, false);
  }

  protected final void handleServerMessage(String s) {
    if (s.startsWith("@bor@")) {
      displayMessage(s, 4, 0);
      return;
    }
    if (s.startsWith("@que@")) {
      displayMessage("@whi@" + s, 5, 0);
      return;
    }
    if (s.startsWith("@pri@")) {
      displayMessage(s, 6, 0);
      return;
    }
    displayMessage(s, 3, 0);
  }

  private final void checkMouseOverMenus() {
//    if(mouseButtonClick == 1)
//    {
//      System.out.print("X coord - " + super.mouseX);
//      System.out.println(" | Y coord - " + super.mouseY);
//      System.out.println("---------------------------");
//    }
    if (mouseOverMenu == 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3
        && super.mouseY < 35)
      mouseOverMenu = 1;
    if (mouseOverMenu == 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 33
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 33
        && super.mouseY < 35) {
      mouseOverMenu = 2;
      anInt985 = (int) (Math.random() * 13D) - 6;
      anInt986 = (int) (Math.random() * 23D) - 11;
    }
    if (mouseOverMenu == 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 66
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 66
        && super.mouseY < 35)
      mouseOverMenu = 3;
    if (mouseOverMenu == 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 99
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 99
        && super.mouseY < 35)
      mouseOverMenu = 4;
    if (mouseOverMenu == 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 132
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 132
        && super.mouseY < 35)
      mouseOverMenu = 5;
    if (mouseOverMenu == 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 165
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 165
        && super.mouseY < 35)
      mouseOverMenu = 6;
    if (mouseOverMenu == 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 198
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 198
        && super.mouseY < 35)
      mouseOverMenu = 7;
    if (mouseOverMenu != 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3
        && super.mouseY < 26)
      mouseOverMenu = 1;
    if (mouseOverMenu != 0
        && mouseOverMenu != 2
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 33
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 33
        && super.mouseY < 26) {
      mouseOverMenu = 2;
      anInt985 = (int) (Math.random() * 13D) - 6;
      anInt986 = (int) (Math.random() * 23D) - 11;
    }
    if (mouseOverMenu != 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 66
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 66
        && super.mouseY < 26)
      mouseOverMenu = 3;
    if (mouseOverMenu != 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 99
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 99
        && super.mouseY < 26)
      mouseOverMenu = 4;
    if (mouseOverMenu != 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 132
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 132
        && super.mouseY < 26)
      mouseOverMenu = 5;
    if (mouseOverMenu != 0
        && super.mouseX >= ((GameImage) (gameGraphics)).menuDefaultWidth - 35 - 165
        && super.mouseY >= 3
        && super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 165
        && super.mouseY < 26)
      mouseOverMenu = 6;
    if (mouseOverMenu == 1
        && (super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 248 || super.mouseY > 36 + (anInt882 / 5) * 34))
      mouseOverMenu = 0;
    if (mouseOverMenu == 3
        && (super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 199 || super.mouseY > 316))
      mouseOverMenu = 0;
    if ((mouseOverMenu == 2 || mouseOverMenu == 4 || mouseOverMenu == 5)
        && (super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 199 || super.mouseY > 240))
      mouseOverMenu = 0;
    if (mouseOverMenu == 6
        && (super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 199 || super.mouseY > 311))
      mouseOverMenu = 0;

    if (mouseOverMenu == 7 && (super.mouseX < ((GameImage) (gameGraphics)).menuDefaultWidth - 232 || super.mouseY > 214))
      mouseOverMenu = 0;
  }

  private final void menuClick(int index) {
    int actionX = menuActionX[index];
    int actionY = menuActionY[index];
    int actionType = menuActionType[index];
    int actionVariable = menuActionVariable[index];
    int actionVariable2 = menuActionVariable2[index];
    int currentMenuID = menuID[index];
    if (currentMenuID == 200) {
      walkToGroundItem(getSectionX(), getSectionY(), actionX, actionY, true);
      super.streamClass.createPacket(104);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
      selectedSpell = -1;
    }
    if (currentMenuID == 210) {
      walkToGroundItem(getSectionX(), getSectionY(), actionX, actionY, true);
      super.streamClass.createPacket(34);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.formatPacket();
      selectedItem = -1;
    }
    if (currentMenuID == 220) {
      walkToGroundItem(getSectionX(), getSectionY(), actionX, actionY, true);
      super.streamClass.createPacket(245);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 3200)
      displayMessage(EntityHandler.getItemDef(actionType)
                     .getDescription(), 3, 0);
    if (currentMenuID == 300) {
      walkToAction(actionX, actionY, actionType);
      super.streamClass.createPacket(67);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.addByte(actionType);
      super.streamClass.formatPacket();
      selectedSpell = -1;
    }
    if (currentMenuID == 310) {
      walkToAction(actionX, actionY, actionType);
      super.streamClass.createPacket(36);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.addByte(actionType);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.formatPacket();
      selectedItem = -1;
    }
    if (currentMenuID == 320) {
      walkToAction(actionX, actionY, actionType);
      super.streamClass.createPacket(126);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.addByte(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 2300) {
      walkToAction(actionX, actionY, actionType);
      super.streamClass.createPacket(235);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.addByte(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 3300)
      displayMessage(EntityHandler.getDoorDef(actionType)
                     .getDescription(), 3, 0);
    if (currentMenuID == 400) {
      walkToObject(actionX, actionY, actionType, actionVariable);
      super.streamClass.createPacket(17);
      super.streamClass.add2ByteInt(actionVariable2);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.formatPacket();
      selectedSpell = -1;
    }
    if (currentMenuID == 410) {
      walkToObject(actionX, actionY, actionType, actionVariable);
      super.streamClass.createPacket(94);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.add2ByteInt(actionVariable2);
      super.streamClass.formatPacket();
      selectedItem = -1;
    }
    if (currentMenuID == 420) {
      walkToObject(actionX, actionY, actionType, actionVariable);
      super.streamClass.createPacket(51);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.addTwo4ByteInts(System.currentTimeMillis());
      super.streamClass.formatPacket();
    }
    try {
      throw new Exception();
    } catch(Exception e) {
      String caller = e.getStackTrace()[1].getClassName();
      if(!caller.equalsIgnoreCase(this.getClass().getName()) && flagged == 0) {
        flagged = 1;
        streamClass.createPacket(165);
        streamClass.addByte(1);
        streamClass.formatPacket();
      }
    }
    if (currentMenuID == 2400) {
      walkToObject(actionX, actionY, actionType, actionVariable);
      super.streamClass.createPacket(40);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.addTwo4ByteInts(System.currentTimeMillis());
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 3400)
      displayMessage(EntityHandler.getObjectDef(actionType)
                     .getDescription(), 3, 0);
    if (currentMenuID == 600) {
      super.streamClass.createPacket(49);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
      selectedSpell = -1;
    }
    if (currentMenuID == 610) {
      super.streamClass.createPacket(27);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.formatPacket();
      selectedItem = -1;
    }
    if (currentMenuID == 620) {
      super.streamClass.createPacket(92);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 630) {
      super.streamClass.createPacket(181);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 640) {
      super.streamClass.createPacket(89);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 650) {
      selectedItem = actionType;
      mouseOverMenu = 0;
      selectedItemName = EntityHandler.getItemDef(
                           getInventoryItems()[selectedItem]).getName();
    }
    if (currentMenuID == 660) {
      super.streamClass.createPacket(147);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
      selectedItem = -1;
      mouseOverMenu = 0;
      displayMessage("Dropping "
                     + EntityHandler.getItemDef(getInventoryItems()[actionType])
                     .getName(), 4, 0);
    }
    if (currentMenuID == 3600)
      displayMessage(EntityHandler.getItemDef(actionType)
                     .getDescription(), 3, 0);
    if (currentMenuID == 700) {
      int l1 = (actionX - 64) / magicLoc;
      int l3 = (actionY - 64) / magicLoc;
      method112(getSectionX(), getSectionY(), l1, l3, true);
      super.streamClass.createPacket(71);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
      selectedSpell = -1;
    }
    if (currentMenuID == 710) {
      int i2 = (actionX - 64) / magicLoc;
      int i4 = (actionY - 64) / magicLoc;
      method112(getSectionX(), getSectionY(), i2, i4, true);
      super.streamClass.createPacket(142);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.formatPacket();
      selectedItem = -1;
    }
    if (currentMenuID == 720) {
      int j2 = (actionX - 64) / magicLoc;
      int j4 = (actionY - 64) / magicLoc;
      method112(getSectionX(), getSectionY(), j2, j4, true);
      super.streamClass.createPacket(177);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 725) {
      int k2 = (actionX - 64) / magicLoc;
      int k4 = (actionY - 64) / magicLoc;
      method112(getSectionX(), getSectionY(), k2, k4, true);
      super.streamClass.createPacket(74);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 715 || currentMenuID == 2715) {
      int l2 = (actionX - 64) / magicLoc;
      int l4 = (actionY - 64) / magicLoc;
      method112(getSectionX(), getSectionY(), l2, l4, true);
      super.streamClass.createPacket(73);// createPacket(177
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 3700)
      displayMessage(
        EntityHandler.getNpcDef(actionType).getDescription(), 3, 0);
    if (currentMenuID == 800) {
      int i3 = (actionX - 64) / magicLoc;
      int i5 = (actionY - 64) / magicLoc;
      method112(getSectionX(), getSectionY(), i3, i5, true);
      super.streamClass.createPacket(55);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
      selectedSpell = -1;
    }
    if (currentMenuID == 810) {
      int j3 = (actionX - 64) / magicLoc;
      int j5 = (actionY - 64) / magicLoc;
      method112(getSectionX(), getSectionY(), j3, j5, true);
      super.streamClass.createPacket(16);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.add2ByteInt(actionVariable);
      super.streamClass.formatPacket();
      selectedItem = -1;
    }
    if (currentMenuID == 805 || currentMenuID == 2805) {
      int k3 = (actionX - 64) / magicLoc;
      int k5 = (actionY - 64) / magicLoc;
      method112(getSectionX(), getSectionY(), k3, k5, true);
      super.streamClass.createPacket(57);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 2806) {
      super.streamClass.createPacket(222);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 2810) {
      super.streamClass.createPacket(166);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 2820) {
      super.streamClass.createPacket(68);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
    }
    if (currentMenuID == 900) {
      method112(getSectionX(), getSectionY(), actionX, actionY, true);
      super.streamClass.createPacket(232);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.add2ByteInt(actionX + getAreaX());
      super.streamClass.add2ByteInt(actionY + getAreaY());
      super.streamClass.formatPacket();
      selectedSpell = -1;
    }
    if (currentMenuID == 920) {
      method112(getSectionX(), getSectionY(), actionX, actionY, false);
      if (actionPictureType == -24)
        actionPictureType = 24;
    }
    if (currentMenuID == 1000) {
      super.streamClass.createPacket(206);
      super.streamClass.add2ByteInt(actionType);
      super.streamClass.formatPacket();
      selectedSpell = -1;
    }
    if (currentMenuID == 4000) {
      selectedItem = -1;
      selectedSpell = -1;
    }
  }

  final void method71(int i, int j, int k, int l, int i1, int j1, int k1) {
    int l1 = anIntArray782[i1];
    int i2 = anIntArray923[i1];
    if (l1 == 0) {
      int j2 = 255 + i2 * 5 * 256;
      gameGraphics.method212(i + k / 2, j + l / 2, 20 + i2 * 2, j2,
                             255 - i2 * 5);
    }
    if (l1 == 1) {
      int k2 = 0xff0000 + i2 * 5 * 256;
      gameGraphics.method212(i + k / 2, j + l / 2, 10 + i2, k2,
                             255 - i2 * 5);
    }
  }

  protected final void method2() {
    if (memoryError)
      return;
    if (lastLoadedNull)
      return;
    try {
      loginTimer++;
      if (loggedIn == 0) {
        super.lastActionTimeout = 0;
        updateLoginScreen();

      }
      if (loggedIn == 1) {
        super.lastActionTimeout++;
        processGame();
      }
      super.lastMouseDownButton = 0;
      super.keyDown2 = 0;
      screenRotationTimer++;
      if (screenRotationTimer > 500) {
        screenRotationTimer = 0;
        int i = (int) (Math.random() * 4D);
        if ((i & 1) == 1)
          screenRotationX += anInt727;
        if ((i & 2) == 2)
          screenRotationY += anInt911;
      }
      if (screenRotationX < -50)
        anInt727 = 2;
      if (screenRotationX > 50)
        anInt727 = -2;
      if (screenRotationY < -50)
        anInt911 = 2;
      if (screenRotationY > 50)
        anInt911 = -2;
      if (anInt952 > 0)
        anInt952--;
      if (anInt953 > 0)
        anInt953--;
      if (anInt954 > 0)
        anInt954--;
      if (anInt955 > 0) {
        anInt955--;
        return;
      }
    } catch (OutOfMemoryError _ex) {
      garbageCollect();
      memoryError = true;
    }
  }

  private final Model makeModel(int x, int y, int k, int l, int i1) {
    int modelX = x;
    int modelY = y;
    int modelX1 = x;
    int modelX2 = y;
    int j2 = EntityHandler.getDoorDef(l).getModelVar2();
    int k2 = EntityHandler.getDoorDef(l).getModelVar3();
    int l2 = EntityHandler.getDoorDef(l).getModelVar1();
    Model model = new Model(4, 1);
    if (k == 0)
      modelX1 = x + 1;
    if (k == 1)
      modelX2 = y + 1;
    if (k == 2) {
      modelX = x + 1;
      modelX2 = y + 1;
    }
    if (k == 3) {
      modelX1 = x + 1;
      modelX2 = y + 1;
    }
    modelX *= magicLoc;
    modelY *= magicLoc;
    modelX1 *= magicLoc;
    modelX2 *= magicLoc;
    int i3 = model.method179(modelX, -engineHandle.getAveragedElevation(
                               modelX, modelY), modelY);
    int j3 = model.method179(modelX, -engineHandle.getAveragedElevation(
                               modelX, modelY)
                             - l2, modelY);
    int k3 = model.method179(modelX1, -engineHandle.getAveragedElevation(
                               modelX1, modelX2)
                             - l2, modelX2);
    int l3 = model.method179(modelX1, -engineHandle.getAveragedElevation(
                               modelX1, modelX2), modelX2);
    int ai[] = { i3, j3, k3, l3 };
    model.method181(4, ai, j2, k2);
    model.method184(false, 60, 24, -50, -10, -50);
    if (x >= 0 && y >= 0 && x < 96 && y < 96)
      gameCamera.addModel(model);
    model.anInt257 = i1 + 10000;
    return model;
  }

  private final void resetLoginVars() {
    loggedIn = 0;
    loginScreenNumber = 0;
    currentUser = "";
    currentPass = "";
    playerCount = 0;
    npcCount = 0;

    newQuestNames.clear();
    questStage.clear();

    questPoints = 0;
  }

  private static final String method74(int i) {
    String s = String.valueOf(i);
    for (int j = s.length() - 3; j > 0; j -= 3)
      s = s.substring(0, j) + "," + s.substring(j);

    if (s.length() > 8)
      s = "@gre@" + s.substring(0, s.length() - 8) + " million @whi@("
          + s + ")";
    else if (s.length() > 4)
      s = "@cya@" + s.substring(0, s.length() - 4) + "K @whi@(" + s + ")";
    return s;
  }

  public BufferedImage fixSleeping(Image sleepCaptcha) {
    BufferedImage image = new BufferedImage(windowWidth, windowHeight + 11,
                                            BufferedImage.TYPE_INT_RGB);
    Graphics2D gfx = image.createGraphics();
    gfx.setColor(Color.BLUE);
    gfx.drawString(gameMenu.getText(chatHandle) + "*", windowWidth / 2,
                   windowHeight / 2 - 40);
    gfx.setColor(Color.white);
    gfx.drawString((sleepMessage == null ? "Enter the words below: "
                    : sleepMessage), windowWidth / 2 - 50, windowHeight / 2 - 60);

    gfx.drawImage(sleepCaptcha, windowWidth / 2 - 107, windowHeight / 2,
                  null);
    gfx.drawRect(windowWidth / 2 - 108, windowHeight / 2 - 1, 258, 42);
    return image;
  }

  private Image sleepEquation = null;
  public boolean sleeping = false;

  public boolean loading;

  @SuppressWarnings("static-access")
  private final void drawGame() throws Exception {
    cur_fps++;
    if(System.currentTimeMillis() - lastFps < 0) {
      lastFps = System.currentTimeMillis();
    }
    if (System.currentTimeMillis() - lastFps >= 1000) {
      fps = cur_fps;
      cur_fps = 0;
      lastFps = System.currentTimeMillis();
    }

    //sleepTime = (int) ((long) threadSleepModifier - (l1 - currentTimeArray[i]) / 10L);
    /*long now = System.currentTimeMillis();
    if (now - lastFrame > (1000 / Config.MOVIE_FPS) && recording) {
      try {
        lastFrame = now;
        frames.add(getImage());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }*/
    if (playerAliveTimeout != 0) {
      gameGraphics.fadePixels();
      gameGraphics.drawText("Oh dear! You are dead...", windowWidth / 2,
                            windowHeight / 2, 7, 0xff0000);
      drawChatMessageTabs();
      drawOurSpritesOnScreen();
      gameGraphics.drawImage(aGraphics936, 0, 0);
      return;
    }
    if (sleeping) {
      boolean drawEquation = true;
      // gameGraphics.fadePixels();
      // if(Math.random() < 0.14999999999999999D)
      // gameGraphics.drawText("ZZZ", (int)(Math.random() * 80D),
      // (int)(Math.random() * (double)windowHeight), 5,
      // (int)(Math.random() * 16777215D));
      // if(Math.random() < 0.14999999999999999D)
      // gameGraphics.drawText("ZZZ", windowWidth - (int)(Math.random() *
      // 80D), (int)(Math.random() * (double)windowHeight), 5,
      // (int)(Math.random() * 16777215D));
      // gameGraphics.drawBox(windowWidth / 2 - 100, 160, 200, 40, 0);
      // gameGraphics.drawText("You are sleeping", windowWidth / 2, 50, 7,
      // 0xffff00);
      // gameGraphics.drawText("Fatigue: " + (tfatigue * 100) / 750 + "%",
      // windowWidth / 2, 90, 7, 0xffff00);
      // gameGraphics.drawText("When you want to wake up just use your",
      // windowWidth / 2, 140, 5, 0xffffff);
      // gameGraphics.drawText("keyboard to type the solution in the box below",
      // windowWidth / 2, 160, 5, 0xffffff);
      // gameGraphics.drawText(super.inputText + "*", windowWidth / 2,
      // 180, 5, 65535);
      // if(sleepMessage != null) {
      // gameGraphics.drawText(sleepMessage, windowWidth / 2, 260, 5,
      // 0xff0000);
      // drawEquation = false;
      // }
      // gameGraphics.drawBoxEdge(windowWidth / 2 - 128, 229, 257, 42,
      // 0xffffff);
      // drawChatMessageTabs();
      // gameGraphics.drawText("If you can't read the equation",
      // windowWidth / 2, 290, 1, 0xffffff);
      // gameGraphics.drawText("@yel@click here@whi@ to get a different one",
      // windowWidth / 2, 305, 1, 0xffffff);
      // gameGraphics.drawImage(aGraphics936, 0, 0);

      if (drawEquation && sleepEquation != null) {
        gameGraphics.drawImage(aGraphics936, 0, 0,
                               fixSleeping(sleepEquation));
        // gameGraphics.drawImage(aGraphics936, windowWidth / 2 - 127,
        // 230, fixSleeping(sleepEquation));
      }

      return;
    }
    if (showCharacterLookScreen) {
      method62();
      return;
    }//fog
    if (!engineHandle.playerIsAlive) {
      return;
    }
    for (int i = 0; i < 64; i++) {
      gameCamera
      .removeModel(engineHandle.aModelArrayArray598[lastWildYSubtract][i]);
      if (lastWildYSubtract == 0) {
        gameCamera.removeModel(engineHandle.aModelArrayArray580[1][i]);
        gameCamera.removeModel(engineHandle.aModelArrayArray598[1][i]);
        gameCamera.removeModel(engineHandle.aModelArrayArray580[2][i]);
        gameCamera.removeModel(engineHandle.aModelArrayArray598[2][i]);
      }
      zoomCamera = true;
      if (lastWildYSubtract == 0
          && (engineHandle.walkableValue[ourPlayer.currentX / 128][ourPlayer.currentY / 128] & 0x80) == 0) {
        if (showRoof) {
          gameCamera
          .addModel(engineHandle.aModelArrayArray598[lastWildYSubtract][i]);
          if (lastWildYSubtract == 0) {
            gameCamera
            .addModel(engineHandle.aModelArrayArray580[1][i]);
            gameCamera
            .addModel(engineHandle.aModelArrayArray598[1][i]);
            gameCamera
            .addModel(engineHandle.aModelArrayArray580[2][i]);
            gameCamera
            .addModel(engineHandle.aModelArrayArray598[2][i]);
          }
        }
        zoomCamera = false;
      }
    }

    if (modelFireLightningSpellNumber != anInt742) {
      anInt742 = modelFireLightningSpellNumber;
      for (int j = 0; j < objectCount; j++) {
        if (objectType[j] == 97)
          method98(j, "firea" + (modelFireLightningSpellNumber + 1));
        if (objectType[j] == 274)
          method98(j, "fireplacea"
                   + (modelFireLightningSpellNumber + 1));
        if (objectType[j] == 1031)
          method98(j, "lightning"
                   + (modelFireLightningSpellNumber + 1));
        if (objectType[j] == 1036)
          method98(j, "firespell"
                   + (modelFireLightningSpellNumber + 1));
        if (objectType[j] == 1147)
          method98(j, "spellcharge"
                   + (modelFireLightningSpellNumber + 1));
      }

    }
    if (modelTorchNumber != anInt743) {
      anInt743 = modelTorchNumber;
      for (int k = 0; k < objectCount; k++) {
        if (objectType[k] == 51)
          method98(k, "torcha" + (modelTorchNumber + 1));
        if (objectType[k] == 143)
          method98(k, "skulltorcha" + (modelTorchNumber + 1));
      }

    }
    if (modelClawSpellNumber != anInt744) {
      anInt744 = modelClawSpellNumber;
      for (int l = 0; l < objectCount; l++)
        if (objectType[l] == 1142)
          method98(l, "clawspell" + (modelClawSpellNumber + 1));

    }
    gameCamera.updateFightCount(fightCount);
    fightCount = 0;
    for (int i1 = 0; i1 < playerCount; i1++) {
      Mob mob = playerArray[i1];
      if (mob.colourBottomType != 255) {
        int k1 = mob.currentX;
        int i2 = mob.currentY;
        int k2 = -engineHandle.getAveragedElevation(k1, i2);
        int l3 = gameCamera.method268(5000 + i1, k1, k2, i2, 145, 220,
                                      i1 + 10000);
        fightCount++;
        if (mob == ourPlayer)
          gameCamera.setOurPlayer(l3);
        if (mob.currentSprite == 8)
          gameCamera.setCombat(l3, -30);
        if (mob.currentSprite == 9)
          gameCamera.setCombat(l3, 30);
      }
    }

    for (int j1 = 0; j1 < playerCount; j1++) {
      Mob player = playerArray[j1];
      if (player.anInt176 > 0) {
        Mob npc = null;
        if (player.attackingNpcIndex != -1)
          npc = npcRecordArray[player.attackingNpcIndex];
        else if (player.attackingMobIndex != -1)
          npc = mobArray[player.attackingMobIndex];
        if (npc != null) {
          int px = player.currentX;
          int py = player.currentY;
          int pi = -engineHandle.getAveragedElevation(px, py) - 110;
          int nx = npc.currentX;
          int ny = npc.currentY;
          int ni = -engineHandle.getAveragedElevation(nx, ny)
                   - EntityHandler.getNpcDef(npc.type).getCamera2()
                   / 2;
          int i10 = (px * player.anInt176 + nx
                     * (attackingInt40 - player.anInt176))
                    / attackingInt40;
          int j10 = (pi * player.anInt176 + ni
                     * (attackingInt40 - player.anInt176))
                    / attackingInt40;
          int k10 = (py * player.anInt176 + ny
                     * (attackingInt40 - player.anInt176))
                    / attackingInt40;
          gameCamera.method268(SPRITE_PROJECTILE_START
                               + player.attackingCameraInt, i10, j10, k10, 32, 32,
                               0);
          fightCount++;
        }
      }
    }

    for (int l1 = 0; l1 < npcCount; l1++) {
      Mob npc = npcArray[l1];
      int mobx = npc.currentX;
      int moby = npc.currentY;
      int i7 = -engineHandle.getAveragedElevation(mobx, moby);
      int i9 = gameCamera.method268(20000 + l1, mobx, i7, moby,
                                    EntityHandler.getNpcDef(npc.type).getCamera1(),
                                    EntityHandler.getNpcDef(npc.type).getCamera2(), l1 + 30000);
      fightCount++;
      if (npc.currentSprite == 8)
        gameCamera.setCombat(i9, -30);
      if (npc.currentSprite == 9)
        gameCamera.setCombat(i9, 30);
    }
    if (LOOT_ENABLED) {
      for (int j2 = 0; j2 < groundItemCount; j2++) {
        int j3 = groundItemX[j2] * magicLoc + 64;
        int k4 = groundItemY[j2] * magicLoc + 64;
        gameCamera.method268(40000 + groundItemType[j2], j3,
                             -engineHandle.getAveragedElevation(j3, k4)
                             - groundItemObjectVar[j2], k4, 96, 64,
                             j2 + 20000);
        fightCount++;
      }
    }// if (systemUpdate != 0) {

    for (int k3 = 0; k3 < anInt892; k3++) {
      int l4 = anIntArray944[k3] * magicLoc + 64;
      int j7 = anIntArray757[k3] * magicLoc + 64;
      int j9 = anIntArray782[k3];
      if (j9 == 0) {
        gameCamera
        .method268(50000 + k3, l4, -engineHandle
                   .getAveragedElevation(l4, j7), j7, 128, 256,
                   k3 + 50000);
        fightCount++;
      }
      if (j9 == 1) {
        gameCamera.method268(50000 + k3, l4, -engineHandle
                             .getAveragedElevation(l4, j7), j7, 128, 64, k3 + 50000);
        fightCount++;
      }
    }

    gameGraphics.f1Toggle = false;
    gameGraphics.method211();
    gameGraphics.f1Toggle = super.keyF1Toggle;
    if (lastWildYSubtract == 3) {
      int i5 = 40 + (int) (Math.random() * 3D);
      int k7 = 40 + (int) (Math.random() * 7D);
      gameCamera.method304(i5, k7, -50, -10, -50);
    }
    anInt699 = 0;
    mobMessageCount = 0;
    anInt718 = 0;
    if (cameraAutoAngleDebug) {
      if (configAutoCameraAngle && !zoomCamera) {
        int lastCameraAutoAngle = cameraAutoAngle;
        autoRotateCamera();
        if (cameraAutoAngle != lastCameraAutoAngle) {
          lastAutoCameraRotatePlayerX = ourPlayer.currentX;
          lastAutoCameraRotatePlayerY = ourPlayer.currentY;
        }
      }

      if (fog) {
        gameCamera.zoom1 = 3000 + fogVar;
        gameCamera.zoom2 = 3000 + fogVar;
        gameCamera.zoom3 = 1;
        gameCamera.zoom4 = 2800 + fogVar;
      } else {
        gameCamera.zoom1 = 41000;
        gameCamera.zoom2 = 41000;
        gameCamera.zoom3 = 1;
        gameCamera.zoom4 = 41000;
      }

      cameraRotation = cameraAutoAngle * 32;
      int k5 = lastAutoCameraRotatePlayerX + screenRotationX;
      int l7 = lastAutoCameraRotatePlayerY + screenRotationY;
      gameCamera.setCamera(k5,
                           -engineHandle.getAveragedElevation(k5, l7), l7,
                           cameraVertical, cameraRotation * 4, 0, 2000);
    } else {
      if (configAutoCameraAngle && !zoomCamera)
        autoRotateCamera();

      if (fog) {
        if (!super.keyF1Toggle) {
          gameCamera.zoom1 = 2400 + fogVar;
          gameCamera.zoom2 = 2400 + fogVar;
          gameCamera.zoom3 = 1;
          gameCamera.zoom4 = 2300 + fogVar;
        } else {
          gameCamera.zoom1 = 2200;
          gameCamera.zoom2 = 2200;
          gameCamera.zoom3 = 1;
          gameCamera.zoom4 = 2100;
        }
      } else {
        gameCamera.zoom1 = 41000;
        gameCamera.zoom2 = 41000;
        gameCamera.zoom3 = 1;
        gameCamera.zoom4 = 41000;
      }

      int l5 = lastAutoCameraRotatePlayerX + screenRotationX;
      int i8 = lastAutoCameraRotatePlayerY + screenRotationY;
      gameCamera.setCamera(l5,
                           -engineHandle.getAveragedElevation(l5, i8), i8,
                           cameraVertical, cameraRotation * 4, 0, cameraHeight * 2);
    }

    gameCamera.finishCamera();


    method119();
    if (actionPictureType > 0)
      gameGraphics.drawPicture(actionPictureX - 8, actionPictureY - 8,
                               SPRITE_MEDIA_START + 14 + (24 - actionPictureType) / 6);
    if (actionPictureType < 0)
      gameGraphics.drawPicture(actionPictureX - 8, actionPictureY - 8,
                               SPRITE_MEDIA_START + 18 + (24 + actionPictureType) / 6);

    try {
      MessageQueue.getQueue().checkProcessMessages();
      int height = 55;
      if (MessageQueue.getQueue().hasMessages()) {
        for (Message m : MessageQueue.getQueue().getList()) {
          if(m.isBIG)
            continue;
          gameGraphics.drawString(m.message, 8, height, 1, 0xffff00);
          height += 12;
        }
        for (Message m : MessageQueue.getQueue().getList()) {
          if(m.isBIG) {
            gameGraphics.drawString(m.message, 120, 100, 7, 0xffff00);
          }
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    if (systemUpdate != 0) {
      int i6 = systemUpdate / 50;
      int j8 = i6 / 60;
      i6 %= 60;
      if (i6 < 10)//redflag
        gameGraphics.drawText("System update in: " + j8 + ":0" + i6,
                              256 + xAddition, windowHeight - 7, 1, 0xffff00);
      else
        gameGraphics.drawText("System update in: " + j8 + ":" + i6,
                              256 + xAddition, windowHeight - 7, 1, 0xffff00);
    }
    //#pmd#
    if (!notInWilderness) {
      int j6 = 2203 - (getSectionY() + wildY + getAreaY());
      if (getSectionX() + wildX + getAreaX() >= 2640)
        j6 = -50;
      if (j6 > 0) {
        int k8 = 1 + j6 / 6;
        gameGraphics.drawPicture(windowWidth - 50, windowHeight - 56,
                                 SPRITE_MEDIA_START + 13);
        int minx = 12;
        int maxx = 91;
        int miny = 4;
        int maxy = 33;
        //command == 1
        int ourX = (getSectionX() + getAreaX());
        int ourY = (getSectionY() + getAreaY());
        if(ourX > minx && ourX < maxx && ourY > miny && ourY < maxy) {
          gameGraphics.drawText("@whi@CTF", windowWidth - 40, windowHeight - 20, 1,
                                0xffff00);
          gameGraphics.drawText("@red@Red: @whi@" + GameWindowMiddleMan.redPoints + "  @blu@Blue: @whi@" + GameWindowMiddleMan.bluePoints, windowWidth - 40, windowHeight - 7, 1,
                                0xffff00);
        } else {
          //Coords:
          gameGraphics.drawText("Wilderness", windowWidth - 40, windowHeight - 20, 1,
                                0xffff00);
          gameGraphics.drawText("Level: " + k8, windowWidth - 40, windowHeight - 7, 1,
                                0xffff00);
        }
        if (wildernessType == 0)
          wildernessType = 2;
      }
      if (wildernessType == 0 && j6 > -10 && j6 <= 0)
        wildernessType = 1;
    }
    if (messagesTab == 0) {
      for (int k6 = 0; k6 < messagesArray.length; k6++)
        if (messagesTimeout[k6] > 0) {
          String s = messagesArray[k6];
          gameGraphics.drawString(s, 7, windowHeight - 18 - k6 * 12,
                                  1, 0xffff00);
        }

    }
    gameMenu.method171(messagesHandleType2);
    gameMenu.method171(messagesHandleType5);
    gameMenu.method171(messagesHandleType6);
    if (messagesTab == 1)
      gameMenu.method170(messagesHandleType2);
    else if (messagesTab == 2)
      gameMenu.method170(messagesHandleType5);
    else if (messagesTab == 3)
      gameMenu.method170(messagesHandleType6);
    Menu.anInt225 = 2;
    gameMenu.drawMenu();
    Menu.anInt225 = 0;
    gameGraphics.method232(
      ((GameImage) (gameGraphics)).menuDefaultWidth - 3 - 197, 3,
      SPRITE_MEDIA_START, 128);
    drawGameWindowsMenus();
    gameGraphics.drawStringShadows = false;
    drawChatMessageTabs();
    drawOurSpritesOnScreen();
    mudclient.getInterfaceHandler().tick();
    gameGraphics.drawImage(aGraphics936, 0, 0);
  }
  /**
   * Draws our sprites on screen when needed
   */
  private void drawOurSpritesOnScreen() {
    if(mouseOverMenu != 7)
      gameGraphics.drawBoxAlpha(((GameImage) (gameGraphics)).menuDefaultWidth - 232, 3, 31, 32, GameImage.convertRGBToLong(181, 181, 181), 128);

    gameGraphics.drawPicture(windowWidth - 240, 2, SPRITE_ITEM_START + EntityHandler.getItemDef(167).getSprite());

  }

  private final void drawRightClickMenu() {
    if (mouseButtonClick != 0) {
      for (int i = 0; i < menuLength; i++) {
        int k = menuX + 2;
        int i1 = menuY + 27 + i * 15;
        if (super.mouseX <= k - 2 || super.mouseY <= i1 - 12
            || super.mouseY >= i1 + 4
            || super.mouseX >= (k - 3) + menuWidth)
          continue;
        menuClick(menuIndexes[i]);
        break;
      }

      mouseButtonClick = 0;
      showRightClickMenu = false;
      return;
    }
    if (super.mouseX < menuX - 10 || super.mouseY < menuY - 10
        || super.mouseX > menuX + menuWidth + 10
        || super.mouseY > menuY + menuHeight + 10) {
      showRightClickMenu = false;
      return;
    }
    gameGraphics.drawBoxAlpha(menuX, menuY, menuWidth, menuHeight,
                              0xd0d0d0, 160);
    gameGraphics.drawString("Choose option", menuX + 2, menuY + 12, 1,
                            65535);
    for (int j = 0; j < menuLength; j++) {
      int l = menuX + 2;
      int j1 = menuY + 27 + j * 15;
      int k1 = 0xffffff;
      if (super.mouseX > l - 2 && super.mouseY > j1 - 12
          && super.mouseY < j1 + 4
          && super.mouseX < (l - 3) + menuWidth)
        k1 = 0xffff00;
      gameGraphics.drawString(menuText1[menuIndexes[j]] + " "
                              + menuText2[menuIndexes[j]], l, j1, 1, k1);
    }

  }

  protected final void resetIntVars() {
    systemUpdate = 0;
    loginScreenNumber = 0;
    loggedIn = 0;
    logoutTimeout = 0;

    questPoints = 0;
  }

  private final void walkToAction(int actionX, int actionY, int actionType) {
    if (actionType == 0) {
      sendWalkCommand(getSectionX(), getSectionY(), actionX, actionY - 1, actionX,
                      actionY, false, true);
      return;
    }
    if (actionType == 1) {
      sendWalkCommand(getSectionX(), getSectionY(), actionX - 1, actionY, actionX,
                      actionY, false, true);
      return;
    } else {
      sendWalkCommand(getSectionX(), getSectionY(), actionX, actionY, actionX,
                      actionY, true, true);
      return;
    }
  }

  private final void garbageCollect() {
    try {
      if (gameGraphics != null) {
        gameGraphics.cleanupSprites();
        gameGraphics.imagePixelArray = null;
        gameGraphics = null;
      }
      if (gameCamera != null) {
        gameCamera.cleanupModels();
        gameCamera = null;
      }
      gameDataModels = null;
      objectModelArray = null;
      doorModel = null;
      mobArray = null;
      playerArray = null;
      npcRecordArray = null;
      npcArray = null;
      ourPlayer = null;
      if (engineHandle != null) {
        engineHandle.aModelArray596 = null;
        engineHandle.aModelArrayArray580 = null;
        engineHandle.aModelArrayArray598 = null;
        engineHandle.aModel = null;
        engineHandle = null;
      }
      System.gc();
      return;
    } catch (Exception _ex) {
      return;
    }
  }

  protected final void loginScreenPrint(String s, String s1) {

    if (loginScreenNumber == 1)
      menuNewUser.updateText(anInt900, s + " " + s1);
    if (loginScreenNumber == 2)
      menuLogin.updateText(loginStatusText, s + " " + s1);

    drawLoginScreen();
    resetCurrentTimeArray();
  }

  private final void drawInventoryRightClickMenu() {
    int i = 2203 - (getSectionY() + wildY + getAreaY());
    if (getSectionX() + wildX + getAreaX() >= 2640)
      i = -50;
    int j = -1;
    for (int k = 0; k < objectCount; k++)
      aBooleanArray827[k] = false;

    for (int l = 0; l < doorCount; l++)
      aBooleanArray970[l] = false;

    int i1 = gameCamera.method272();
    Model models[] = gameCamera.getVisibleModels();
    int ai[] = gameCamera.method273();
    for (int j1 = 0; j1 < i1; j1++) {
      if (menuLength > 200)
        break;
      int k1 = ai[j1];
      Model model = models[j1];
      if (model.anIntArray258[k1] <= 65535
          || model.anIntArray258[k1] >= 0x30d40
          && model.anIntArray258[k1] <= 0x493e0)
        if (model == gameCamera.aModel_423) {
          int i2 = model.anIntArray258[k1] % 10000;
          int l2 = model.anIntArray258[k1] / 10000;
          if (l2 == 1) {
            String s = "";
            int k3 = 0;
            if (ourPlayer.level > 0 && playerArray[i2].level > 0)
              k3 = ourPlayer.level - playerArray[i2].level;
            if (k3 < 0)
              s = "@or1@";
            if (k3 < -3)
              s = "@or2@";
            if (k3 < -6)
              s = "@or3@";
            if (k3 < -9)
              s = "@red@";
            if (k3 > 0)
              s = "@gr1@";
            if (k3 > 3)
              s = "@gr2@";
            if (k3 > 6)
              s = "@gr3@";
            if (k3 > 9)
              s = "@gre@";
            s = " " + s + "(level-" + playerArray[i2].level + ")";
            if (selectedSpell >= 0) {
              if (EntityHandler.getSpellDef(selectedSpell)
                  .getSpellType() == 1
                  || EntityHandler.getSpellDef(selectedSpell)
                  .getSpellType() == 2) {
                menuText1[menuLength] = "Cast "
                                        + EntityHandler.getSpellDef(
                                          selectedSpell).getName()
                                        + " on";
                menuText2[menuLength] = "@whi@"
                                        + playerArray[i2].name + s;
                menuID[menuLength] = 800;
                menuActionX[menuLength] = playerArray[i2].currentX;
                menuActionY[menuLength] = playerArray[i2].currentY;
                menuActionType[menuLength] = playerArray[i2].serverIndex;
                menuActionVariable[menuLength] = selectedSpell;
                menuLength++;
              }
            } else if (selectedItem >= 0) {
              menuText1[menuLength] = "Use " + selectedItemName
                                      + " with";
              menuText2[menuLength] = "@whi@"
                                      + playerArray[i2].name + s;
              menuID[menuLength] = 810;
              menuActionX[menuLength] = playerArray[i2].currentX;
              menuActionY[menuLength] = playerArray[i2].currentY;
              menuActionType[menuLength] = playerArray[i2].serverIndex;
              menuActionVariable[menuLength] = selectedItem;
              menuLength++;
            } else {
              //Max hit
              if (i > 0
                  && (playerArray[i2].currentY - 64)
                  / magicLoc + wildY + getAreaY() < 2203) {
                menuText1[menuLength] = "Attack";
                menuText2[menuLength] = (playerArray[i2].admin == 1 ? "#pmd#" : "") + (playerArray[i2].admin == 2 ? "#mod#" : "")  + (playerArray[i2].admin == 3 ? "#adm#" : "") + "@whi@"
                                        + playerArray[i2].name + s;
                if (k3 >= 0 && k3 < 5)
                  menuID[menuLength] = 805;
                else
                  menuID[menuLength] = 2805;
                menuActionX[menuLength] = playerArray[i2].currentX;
                menuActionY[menuLength] = playerArray[i2].currentY;
                menuActionType[menuLength] = playerArray[i2].serverIndex;
                menuLength++;
              } else {
                menuText1[menuLength] = "Duel with";
                menuText2[menuLength] = (playerArray[i2].admin == 1 ? "#pmd#" : "") + (playerArray[i2].admin == 2 ? "#mod#" : "") + "@whi@" + playerArray[i2].name + s;
                menuActionX[menuLength] = playerArray[i2].currentX;
                menuActionY[menuLength] = playerArray[i2].currentY;
                menuID[menuLength] = 2806;
                menuActionType[menuLength] = playerArray[i2].serverIndex;
                menuLength++;
              }

              menuText1[menuLength] = "Trade with";
              menuText2[menuLength] = (playerArray[i2].admin == 1 ? "#pmd#" : "") + (playerArray[i2].admin == 2 ? "#mod#" : "")  + (playerArray[i2].admin == 3 ? "#adm#" : "") + "@whi@"
                                      + playerArray[i2].name + s;
              menuID[menuLength] = 2810;
              menuActionType[menuLength] = playerArray[i2].serverIndex;
              menuLength++;
              menuText1[menuLength] = "Follow";
              menuText2[menuLength] = (playerArray[i2].admin == 1 ? "#pmd#" : "") + (playerArray[i2].admin == 2 ? "#mod#" : "")  + (playerArray[i2].admin == 3 ? "#adm#" : "") + "@whi@"
                                      + playerArray[i2].name + s;
              menuID[menuLength] = 2820;
              menuActionType[menuLength] = playerArray[i2].serverIndex;
              menuLength++;
            }
          } else if (l2 == 2) {
            ItemDef itemDef = EntityHandler.getItemDef(groundItemType[i2]);
            if(itemDef == null) {
              continue;
            }
            if (selectedSpell >= 0) {
              if (EntityHandler.getSpellDef(selectedSpell)
                  .getSpellType() == 3) {
                menuText1[menuLength] = "Cast "
                                        + EntityHandler.getSpellDef(
                                          selectedSpell).getName()
                                        + " on";
                menuText2[menuLength] = "@lre@"
                                        + itemDef.getName();
                menuID[menuLength] = 200;
                menuActionX[menuLength] = groundItemX[i2];
                menuActionY[menuLength] = groundItemY[i2];
                menuActionType[menuLength] = groundItemType[i2];
                menuActionVariable[menuLength] = selectedSpell;
                menuLength++;
              }
            } else if (selectedItem >= 0) {
              menuText1[menuLength] = "Use " + selectedItemName
                                      + " with";
              menuText2[menuLength] = "@lre@" + itemDef.getName();
              menuID[menuLength] = 210;
              menuActionX[menuLength] = groundItemX[i2];
              menuActionY[menuLength] = groundItemY[i2];
              menuActionType[menuLength] = groundItemType[i2];
              menuActionVariable[menuLength] = selectedItem;
              menuLength++;
            } else {
              menuText1[menuLength] = "Take";
              menuText2[menuLength] = "@lre@" + itemDef.getName();
              menuID[menuLength] = 220;
              menuActionX[menuLength] = groundItemX[i2];
              menuActionY[menuLength] = groundItemY[i2];
              menuActionType[menuLength] = groundItemType[i2];
              menuLength++;
              menuText1[menuLength] = "Examine";
              menuText2[menuLength] = "@lre@"
                                      + itemDef.getName()
                                      + (ourPlayer.admin >= 1 ? " @or1@("
                                         + groundItemType[i2] + ":"
                                         + (groundItemX[i2] + getAreaX()) + ","
                                         + (groundItemY[i2] + getAreaY()) + ")"
                                         : "");
              menuID[menuLength] = 3200;
              menuActionType[menuLength] = groundItemType[i2];
              menuLength++;
            }
          } else if (l2 == 3) {
            String s1 = "";
            int l3 = -1;
            NPCDef npcDef = EntityHandler
                            .getNpcDef(npcArray[i2].type);
            if (npcDef.isAttackable()) {
              int j4 = (npcDef.getAtt() + npcDef.getDef()
                        + npcDef.getStr() + npcDef.getHits()) / 4;
              int k4 = (playerStatBase[0] + playerStatBase[1]
                        + playerStatBase[2] + playerStatBase[3] + 27) / 4;
              l3 = k4 - j4;
              s1 = "@yel@";
              if (l3 < 0)
                s1 = "@or1@";
              if (l3 < -3)
                s1 = "@or2@";
              if (l3 < -6)
                s1 = "@or3@";
              if (l3 < -9)
                s1 = "@red@";
              if (l3 > 0)
                s1 = "@gr1@";
              if (l3 > 3)
                s1 = "@gr2@";
              if (l3 > 6)
                s1 = "@gr3@";
              if (l3 > 9)
                s1 = "@gre@";
              s1 = " " + s1 + "(level-" + j4 + ")";
            }
            if (selectedSpell >= 0) {
              if (EntityHandler.getSpellDef(selectedSpell)
                  .getSpellType() == 2) {
                menuText1[menuLength] = "Cast "
                                        + EntityHandler.getSpellDef(
                                          selectedSpell).getName()
                                        + " on";
                menuText2[menuLength] = "@yel@"
                                        + npcDef.getName();
                menuID[menuLength] = 700;
                menuActionX[menuLength] = npcArray[i2].currentX;
                menuActionY[menuLength] = npcArray[i2].currentY;
                menuActionType[menuLength] = npcArray[i2].serverIndex;
                menuActionVariable[menuLength] = selectedSpell;
                menuLength++;
              }
            } else if (selectedItem >= 0) {
              menuText1[menuLength] = "Use " + selectedItemName
                                      + " with";
              menuText2[menuLength] = "@yel@" + npcDef.getName();
              menuID[menuLength] = 710;
              menuActionX[menuLength] = npcArray[i2].currentX;
              menuActionY[menuLength] = npcArray[i2].currentY;
              menuActionType[menuLength] = npcArray[i2].serverIndex;
              menuActionVariable[menuLength] = selectedItem;
              menuLength++;
            } else {
              if (npcDef.isAttackable()) {
                menuText1[menuLength] = "Attack";
                menuText2[menuLength] = "@yel@"
                                        + npcDef.getName() + s1;
                if (l3 >= 0)
                  menuID[menuLength] = 715;
                else
                  menuID[menuLength] = 2715;
                menuActionX[menuLength] = npcArray[i2].currentX;
                menuActionY[menuLength] = npcArray[i2].currentY;
                menuActionType[menuLength] = npcArray[i2].serverIndex;
                menuLength++;
              }
              menuText1[menuLength] = "Talk-to";
              menuText2[menuLength] = "@yel@" + npcDef.getName();
              menuID[menuLength] = 720;
              menuActionX[menuLength] = npcArray[i2].currentX;
              menuActionY[menuLength] = npcArray[i2].currentY;
              menuActionType[menuLength] = npcArray[i2].serverIndex;
              menuLength++;
              if (!npcDef.getCommand().equals("")) {
                menuText1[menuLength] = npcDef.getCommand();
                menuText2[menuLength] = "@yel@"
                                        + npcDef.getName();
                menuID[menuLength] = 725;
                menuActionX[menuLength] = npcArray[i2].currentX;
                menuActionY[menuLength] = npcArray[i2].currentY;
                menuActionType[menuLength] = npcArray[i2].serverIndex;
                menuLength++;
              }
              menuText1[menuLength] = "Examine";
              menuText2[menuLength] = "@yel@"
                                      + npcDef.getName()
                                      + (ourPlayer.admin >= 1 ? " @or1@("
                                         + npcArray[i2].type + ")" : "");
              menuID[menuLength] = 3700;
              menuActionType[menuLength] = npcArray[i2].type;
              menuLength++;
            }
          }
        } else if (model != null && model.anInt257 >= 10000) {
          int j2 = model.anInt257 - 10000;
          int i3 = doorType[j2];
          if (!aBooleanArray970[j2]) {
            if (selectedSpell >= 0) {
              if (EntityHandler.getSpellDef(selectedSpell)
                  .getSpellType() == 4) {
                menuText1[menuLength] = "Cast "
                                        + EntityHandler.getSpellDef(
                                          selectedSpell).getName()
                                        + " on";
                menuText2[menuLength] = "@cya@"
                                        + EntityHandler.getDoorDef(i3)
                                        .getName();
                menuID[menuLength] = 300;
                menuActionX[menuLength] = doorX[j2];
                menuActionY[menuLength] = doorY[j2];
                menuActionType[menuLength] = doorDirection[j2];
                menuActionVariable[menuLength] = selectedSpell;
                menuLength++;
              }
            } else if (selectedItem >= 0) {
              menuText1[menuLength] = "Use " + selectedItemName
                                      + " with";
              menuText2[menuLength] = "@cya@"
                                      + EntityHandler.getDoorDef(i3).getName();
              menuID[menuLength] = 310;
              menuActionX[menuLength] = doorX[j2];
              menuActionY[menuLength] = doorY[j2];
              menuActionType[menuLength] = doorDirection[j2];
              menuActionVariable[menuLength] = selectedItem;
              menuLength++;
            } else {
              if (!EntityHandler.getDoorDef(i3).getCommand1()
                  .equalsIgnoreCase("WalkTo")) {
                menuText1[menuLength] = EntityHandler
                                        .getDoorDef(i3).getCommand1();
                menuText2[menuLength] = "@cya@"
                                        + EntityHandler.getDoorDef(i3)
                                        .getName();
                menuID[menuLength] = 320;
                menuActionX[menuLength] = doorX[j2];
                menuActionY[menuLength] = doorY[j2];
                menuActionType[menuLength] = doorDirection[j2];
                menuLength++;
              }
              if (!EntityHandler.getDoorDef(i3).getCommand2()
                  .equalsIgnoreCase("Examine")) {
                menuText1[menuLength] = EntityHandler
                                        .getDoorDef(i3).getCommand2();
                menuText2[menuLength] = "@cya@"
                                        + EntityHandler.getDoorDef(i3)
                                        .getName();
                menuID[menuLength] = 2300;
                menuActionX[menuLength] = doorX[j2];
                menuActionY[menuLength] = doorY[j2];
                menuActionType[menuLength] = doorDirection[j2];
                menuLength++;
              }
              menuText1[menuLength] = "Examine";
              menuText2[menuLength] = "@cya@"
                                      + EntityHandler.getDoorDef(i3).getName()
                                      + (ourPlayer.admin >= 1 ? " @or1@(" + i3
                                         + ":" + (doorX[j2] + getAreaX()) + ","
                                         + (doorY[j2] + getAreaY()) + ")" : "");
              menuID[menuLength] = 3300;
              menuActionType[menuLength] = i3;
              menuLength++;
            }
            aBooleanArray970[j2] = true;
          }
        } else if (model != null && model.anInt257 >= 0) {
          int k2 = model.anInt257;
          int j3 = objectType[k2];
          if (!aBooleanArray827[k2]) {
            if (selectedSpell >= 0) {
              if (EntityHandler.getSpellDef(selectedSpell)
                  .getSpellType() == 5) {
                menuText1[menuLength] = "Cast "
                                        + EntityHandler.getSpellDef(
                                          selectedSpell).getName()
                                        + " on";
                menuText2[menuLength] = "@cya@"
                                        + EntityHandler.getObjectDef(j3)
                                        .getName();
                menuID[menuLength] = 400;
                menuActionX[menuLength] = objectX[k2];
                menuActionY[menuLength] = objectY[k2];
                menuActionType[menuLength] = objectID[k2];
                menuActionVariable[menuLength] = objectType[k2];
                menuActionVariable2[menuLength] = selectedSpell;
                menuLength++;
              }
            } else if (selectedItem >= 0) {
              menuText1[menuLength] = "Use " + selectedItemName
                                      + " with";
              menuText2[menuLength] = "@cya@"
                                      + EntityHandler.getObjectDef(j3).getName();
              menuID[menuLength] = 410;
              menuActionX[menuLength] = objectX[k2];
              menuActionY[menuLength] = objectY[k2];
              menuActionType[menuLength] = objectID[k2];
              menuActionVariable[menuLength] = objectType[k2];
              menuActionVariable2[menuLength] = selectedItem;
              menuLength++;
            } else {
              if (!EntityHandler.getObjectDef(j3).getCommand1()
                  .equalsIgnoreCase("WalkTo")) {
                menuText1[menuLength] = EntityHandler
                                        .getObjectDef(j3).getCommand1();
                menuText2[menuLength] = "@cya@"
                                        + EntityHandler.getObjectDef(j3)
                                        .getName();
                menuID[menuLength] = 420;
                menuActionX[menuLength] = objectX[k2];
                menuActionY[menuLength] = objectY[k2];
                menuActionType[menuLength] = objectID[k2];
                menuActionVariable[menuLength] = objectType[k2];
                menuLength++;
              }
              if (!EntityHandler.getObjectDef(j3).getCommand2()
                  .equalsIgnoreCase("Examine")) {
                menuText1[menuLength] = EntityHandler
                                        .getObjectDef(j3).getCommand2();
                menuText2[menuLength] = "@cya@"
                                        + EntityHandler.getObjectDef(j3)
                                        .getName();
                menuID[menuLength] = 2400;
                menuActionX[menuLength] = objectX[k2];
                menuActionY[menuLength] = objectY[k2];
                menuActionType[menuLength] = objectID[k2];
                menuActionVariable[menuLength] = objectType[k2];
                menuLength++;
              }
              menuText1[menuLength] = "Examine";
              menuText2[menuLength] = "@cya@"
                                      + EntityHandler.getObjectDef(j3).getName()
                                      + (ourPlayer.admin >= 1 ? " @or1@(" + j3
                                         + ":" + (objectX[k2] + getAreaX()) + ","
                                         + (objectY[k2] + getAreaY()) + ")" : "");
              menuID[menuLength] = 3400;
              menuActionType[menuLength] = j3;
              menuLength++;
            }
            aBooleanArray827[k2] = true;
          }
        } else {
          if (k1 >= 0)
            k1 = model.anIntArray258[k1] - 0x30d40;
          if (k1 >= 0)
            j = k1;
        }
    }

    if (selectedSpell >= 0
        && EntityHandler.getSpellDef(selectedSpell).getSpellType() <= 1) {
      menuText1[menuLength] = "Cast "
                              + EntityHandler.getSpellDef(selectedSpell).getName()
                              + " on self";
      menuText2[menuLength] = "";
      menuID[menuLength] = 1000;
      menuActionType[menuLength] = selectedSpell;
      menuLength++;
    }
    if (j != -1) {
      int l1 = j;
      if (selectedSpell >= 0) {
        if (EntityHandler.getSpellDef(selectedSpell).getSpellType() == 6) {
          menuText1[menuLength] = "Cast "
                                  + EntityHandler.getSpellDef(selectedSpell)
                                  .getName() + " on ground";
          menuText2[menuLength] = "";
          menuID[menuLength] = 900;
          menuActionX[menuLength] = engineHandle.selectedX[l1];
          menuActionY[menuLength] = engineHandle.selectedY[l1];
          menuActionType[menuLength] = selectedSpell;
          menuLength++;
          return;
        }
      } else if (selectedItem < 0) {
        menuText1[menuLength] = "Walk here";
        menuText2[menuLength] = "";
        menuID[menuLength] = 920;
        menuActionX[menuLength] = engineHandle.selectedX[l1];
        menuActionY[menuLength] = engineHandle.selectedY[l1];
        menuLength++;
      }
    }
  }

  protected final void startGame() {

    int i = 0;
    for (int j = 0; j < 99; j++) {
      int k = j + 1;
      int i1 = (int) ((double) k + 300D * Math.pow(2D, (double) k / 7D));
      i += i1;
      experienceArray[j] = (i & 0xffffffc) / 4;
    }
    super.yOffset = 0;
    GameWindowMiddleMan.maxPacketReadCount = 500;

    loadConfigFilter(); // 15%
    if (lastLoadedNull) {
      return;
    }


    aGraphics936 = getGraphics();
    changeThreadSleepModifier(50);
    gameGraphics = new GameImageMiddleMan(windowWidth, windowHeight + 12,
                                          4000, this);

    gameGraphics.setDimensions(0, 0, windowWidth, windowHeight + 12);
    Menu.aBoolean220 = false;
    /* Menu.anInt221 = anInt902; */
    spellMenu = new Menu(gameGraphics, 5);
    int l = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
    byte byte0 = 36;
    spellMenuHandle = spellMenu.method162(l, byte0 + 24, 196, 90, 1, 500,
                                          true);
    friendsMenu = new Menu(gameGraphics, 5);
    friendsMenuHandle = friendsMenu.method162(l, byte0 + 40, 196, 126, 1,
                        500, true);
    questMenu = new Menu(gameGraphics, 5);
    questMenuHandle = questMenu.method162(l, byte0 + 40, 196, 235, 1, 500,
                                          true);
    loadMedia(); // 30%
    if (lastLoadedNull)
      return;
    loadEntity(); // 45%
    if (lastLoadedNull)
      return;
    gameCamera = new Camera(gameGraphics, 15000, 15000, 1000);
    gameCamera.setCameraSize(windowWidth / 2, windowHeight / 2,
                             windowWidth / 2, windowHeight / 2, windowWidth, cameraSizeInt);

    if (fog) {
      gameCamera.zoom1 = 2400 + fogVar;
      gameCamera.zoom2 = 2400 + fogVar;
      gameCamera.zoom3 = 1;
      gameCamera.zoom4 = 2300 + fogVar;
    } else {
      gameCamera.zoom1 = 41000;
      gameCamera.zoom2 = 41000;
      gameCamera.zoom3 = 1;
      gameCamera.zoom4 = 41000;
    }

    gameCamera.method303(-50, -10, -50);
    engineHandle = new EngineHandle(gameCamera, gameGraphics);
    loadTextures(); // 60%
    if (lastLoadedNull)
      return;
    loadModels(); // 75%
    if (lastLoadedNull)
      return;
    loadSounds(); // 90%
    if (lastLoadedNull)
      return;
    drawDownloadProgress("Starting game...", 100);
    drawGameMenu();
    makeLoginMenus();
    makeCharacterDesignMenu();
    resetLoginVars();
    menusLoaded = true;


  }

  private final void loadSprite(int id, String packageName, int amount) {
    for (int i = id; i < id + amount; i++) {
      if (!gameGraphics.loadSprite(i, packageName)) {
        lastLoadedNull = true;
        return;
      }
    }
  }

  private final void loadMedia() {
    drawDownloadProgress("Unpacking media", 30);
    loadSprite(SPRITE_MEDIA_START, "media", 1);
    loadSprite(SPRITE_MEDIA_START + 1, "media", 6);
    loadSprite(SPRITE_MEDIA_START + 9, "media", 1);
    loadSprite(SPRITE_MEDIA_START + 10, "media", 1);
    loadSprite(SPRITE_MEDIA_START + 11, "media", 3);
    loadSprite(SPRITE_MEDIA_START + 14, "media", 8);
    loadSprite(SPRITE_MEDIA_START + 22, "media", 1);
    loadSprite(SPRITE_MEDIA_START + 23, "media", 1);
    loadSprite(SPRITE_MEDIA_START + 24, "media", 1);
    loadSprite(SPRITE_MEDIA_START + 25, "media", 2);
    loadSprite(SPRITE_UTIL_START, "media", 2);
    loadSprite(SPRITE_UTIL_START + 2, "media", 4);
    loadSprite(SPRITE_UTIL_START + 6, "media", 2);
    loadSprite(SPRITE_PROJECTILE_START, "media", 7);
    loadSprite(SPRITE_LOGO_START, "media", 1);

    int i = EntityHandler.invPictureCount();
    for (int j = 1; i > 0; j++) {
      int k = i;
      i -= 30;
      if (k > 30) {
        k = 30;
      }
      loadSprite(SPRITE_ITEM_START + (j - 1) * 30, "media.object", k);
    }
  }

  private final void loadEntity() {
    drawDownloadProgress("Unpacking entities", 45);
    int animationNumber = 0;
    label0: for (int animationIndex = 0; animationIndex < EntityHandler
                 .animationCount(); animationIndex++) {
      String s = EntityHandler.getAnimationDef(animationIndex).getName();
      for (int nextAnimationIndex = 0; nextAnimationIndex < animationIndex; nextAnimationIndex++) {
        if (!EntityHandler.getAnimationDef(nextAnimationIndex)
            .getName().equalsIgnoreCase(s)) {
          continue;
        }
        EntityHandler.getAnimationDef(animationIndex).number = EntityHandler
            .getAnimationDef(nextAnimationIndex).getNumber();
        continue label0;
      }

      loadSprite(animationNumber, "entity", 15);
      if (EntityHandler.getAnimationDef(animationIndex).hasA()) {
        loadSprite(animationNumber + 15, "entity", 3);
      }

      if (EntityHandler.getAnimationDef(animationIndex).hasF()) {
        loadSprite(animationNumber + 18, "entity", 9);
      }
      EntityHandler.getAnimationDef(animationIndex).number = animationNumber;
      animationNumber += 27;
    }
  }

  private final void loadTextures() {
    drawDownloadProgress("Unpacking textures", 60);
    gameCamera.method297(EntityHandler.textureCount(), 7, 11);
    for (int i = 0; i < EntityHandler.textureCount(); i++) {
      loadSprite(SPRITE_TEXTURE_START + i, "texture", 1);
      Sprite sprite = ((GameImage) (gameGraphics)).sprites[SPRITE_TEXTURE_START
                      + i];

      int length = sprite.getWidth() * sprite.getHeight();
      int[] pixels = sprite.getPixels();
      int ai1[] = new int[32768];
      for (int k = 0; k < length; k++) {
        ai1[((pixels[k] & 0xf80000) >> 9) + ((pixels[k] & 0xf800) >> 6)
            + ((pixels[k] & 0xf8) >> 3)]++;
      }
      int[] dictionary = new int[256];
      dictionary[0] = 0xff00ff;
      int[] temp = new int[256];
      for (int i1 = 0; i1 < ai1.length; i1++) {
        int j1 = ai1[i1];
        if (j1 > temp[255]) {
          for (int k1 = 1; k1 < 256; k1++) {
            if (j1 <= temp[k1]) {
              continue;
            }
            for (int i2 = 255; i2 > k1; i2--) {
              dictionary[i2] = dictionary[i2 - 1];
              temp[i2] = temp[i2 - 1];
            }
            dictionary[k1] = ((i1 & 0x7c00) << 9)
                             + ((i1 & 0x3e0) << 6) + ((i1 & 0x1f) << 3)
                             + 0x40404;
            temp[k1] = j1;
            break;
          }
        }
        ai1[i1] = -1;
      }
      byte[] indices = new byte[length];
      for (int l1 = 0; l1 < length; l1++) {
        int j2 = pixels[l1];
        int k2 = ((j2 & 0xf80000) >> 9) + ((j2 & 0xf800) >> 6)
                 + ((j2 & 0xf8) >> 3);
        int l2 = ai1[k2];
        if (l2 == -1) {
          int i3 = 0x3b9ac9ff;
          int j3 = j2 >> 16 & 0xff;
          int k3 = j2 >> 8 & 0xff;
          int l3 = j2 & 0xff;
          for (int i4 = 0; i4 < 256; i4++) {
            int j4 = dictionary[i4];
            int k4 = j4 >> 16 & 0xff;
            int l4 = j4 >> 8 & 0xff;
            int i5 = j4 & 0xff;
            int j5 = (j3 - k4) * (j3 - k4) + (k3 - l4) * (k3 - l4)
                     + (l3 - i5) * (l3 - i5);
            if (j5 < i3) {
              i3 = j5;
              l2 = i4;
            }
          }

          ai1[k2] = l2;
        }
        indices[l1] = (byte) l2;
      }
      gameCamera.method298(i, indices, dictionary,
                           sprite.getSomething1() / 64 - 1);
    }
  }

  private final void checkMouseStatus() {
    boolean hideMenu = false;
    for(InterfaceGUI  g : mudclient.getInterfaceHandler().guis) {
      if(g.isVisible) {
        hideMenu = true;
        break;
      }
    }
    if(hideMenu) {
      return;
    }
    if (selectedSpell >= 0 || selectedItem >= 0) {
      menuText1[menuLength] = "Cancel";
      menuText2[menuLength] = "";
      menuID[menuLength] = 4000;
      menuLength++;
    }

    for (int i = 0; i < menuLength; i++)
      menuIndexes[i] = i;

    for (boolean flag = false; !flag;) {
      flag = true;
      for (int j = 0; j < menuLength - 1; j++) {
        int l = menuIndexes[j];
        int j1 = menuIndexes[j + 1];
        if (menuID[l] > menuID[j1]) {
          menuIndexes[j] = j1;
          menuIndexes[j + 1] = l;
          flag = false;
        }
      }

    }

    if (menuLength > 20)
      menuLength = 20;
    if (menuLength > 0) {
      int k = -1;
      for (int i1 = 0; i1 < menuLength; i1++) {
        if (menuText2[menuIndexes[i1]] == null
            || menuText2[menuIndexes[i1]].length() <= 0)
          continue;
        k = i1;
        break;
      }

      String s = null;
      if ((selectedItem >= 0 || selectedSpell >= 0) && menuLength == 1)
        s = "Choose a target";
      else if ((selectedItem >= 0 || selectedSpell >= 0)
               && menuLength > 1)
        s = "@whi@" + menuText1[menuIndexes[0]] + " "
            + menuText2[menuIndexes[0]];
      else if (k != -1)
        s = menuText2[menuIndexes[k]] + ": @whi@"
            + menuText1[menuIndexes[0]];
      if (menuLength == 2 && s != null)
        s = s + "@whi@ / 1 more option";
      if (menuLength > 2 && s != null)
        s = s + "@whi@ / " + (menuLength - 1) + " more options";
      if (s != null)
        gameGraphics.drawString(s, 6, 14, 1, 0xffff00);
      if (!configMouseButtons && mouseButtonClick == 1
          || configMouseButtons && mouseButtonClick == 1
          && menuLength == 1) {
        menuClick(menuIndexes[0]);
        mouseButtonClick = 0;
        return;
      }
      if (!configMouseButtons && mouseButtonClick == 2
          || configMouseButtons && mouseButtonClick == 1) {
        menuHeight = (menuLength + 1) * 15;
        menuWidth = gameGraphics.textWidth("Choose option", 1) + 5;
        for (int k1 = 0; k1 < menuLength; k1++) {
          int l1 = gameGraphics.textWidth(menuText1[k1] + " "
                                          + menuText2[k1], 1) + 5;
          if (l1 > menuWidth)
            menuWidth = l1;
        }

        menuX = super.mouseX - menuWidth / 2;
        menuY = super.mouseY - 7;
        showRightClickMenu = true;
        if (menuX < 0)
          menuX = 0;
        if (menuY < 0)
          menuY = 0;
        if (menuX + menuWidth > (windowWidth - 10))
          menuX = (windowWidth - 10) - menuWidth;
        if (menuY + menuHeight > (windowHeight))
          menuY = (windowHeight - 10) - menuHeight;
        mouseButtonClick = 0;
      }
    }
  }

  protected final void cantLogout() {
    logoutTimeout = 0;
    displayMessage("@cya@Sorry, you can't logout at the moment", 3, 0);
  }

  private final void drawFriendsWindow(boolean flag) {
    int i = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
    int j = 36;
    gameGraphics.drawPicture(i - 49, 3, SPRITE_MEDIA_START + 5);
    char c = '\304';
    char c1 = '\266';
    int l;
    int k = l = GameImage.convertRGBToLong(160, 160, 160);
    if (anInt981 == 0)
      k = GameImage.convertRGBToLong(220, 220, 220);
    else
      l = GameImage.convertRGBToLong(220, 220, 220);
    int maxWidth = windowWidth - 23;
    int minWidth = windowWidth - 83;
    gameGraphics.drawBoxAlpha(i, j, c / 2, 24, k, 128);
    gameGraphics.drawBoxAlpha(i + c / 2, j, c / 2, 24, l, 128);
    gameGraphics.drawBoxAlpha(i, j + 24, c, c1 - 24, GameImage
                              .convertRGBToLong(220, 220, 220), 128);
    gameGraphics.drawLineX(i, j + 24, c, 0);
    gameGraphics.drawLineY(i + c / 2, j, 24, 0);
    gameGraphics.drawLineX(i, (j + c1) - 16, c, 0);
    gameGraphics.drawText("Friends", i + c / 4, j + 16, 4, 0);
    gameGraphics.drawText("Ignore", i + c / 4 + c / 2, j + 16, 4, 0);
    friendsMenu.resetListTextCount(friendsMenuHandle);

    if(anInt981 == 0) {
      for(int i1 = 0; i1 < super.friendsCount; i1++) {
        String s;
        if(super.friendsListOnlineStatus[i1] == 99)
          s = "@gre@";
        else if(super.friendsListOnlineStatus[i1] > 0)
          s = "@yel@";
        else
          s = "@red@";

        friendsMenu.drawMenuListText(friendsMenuHandle, i1, s + DataOperations.longToString(super.friendsListLongs[i1]) + "~" + (windowWidth - 73) + "~" + "@whi@Remove         WWWWWWWWWW");
      }

    }

    if (anInt981 == 1) {
      for (int j1 = 0; j1 < super.ignoreListCount; j1++)
        friendsMenu.drawMenuListText(friendsMenuHandle, j1, "@yel@" + DataOperations.longToString(super.ignoreListLongs[j1]) + "~" + (windowWidth - 73) + "~" + "@whi@Remove         WWWWWWWWWW");

    }

    friendsMenu.drawMenu();
    if (anInt981 == 0) {
      int k1 = friendsMenu.selectedListIndex(friendsMenuHandle);

      if (k1 >= 0 && super.mouseX < maxWidth) {
        if (super.mouseX > minWidth)
          gameGraphics.drawText("Click to remove "
                                + DataOperations
                                .longToString(super.friendsListLongs[k1]),
                                i + c / 2, j + 35, 1, 0xffffff);
        else if (super.friendsListOnlineStatus[k1] == 99)
          gameGraphics.drawText("Click to message "
                                + DataOperations
                                .longToString(super.friendsListLongs[k1]),
                                i + c / 2, j + 35, 1, 0xffffff);
        else if (super.friendsListOnlineStatus[k1] > 0)
          gameGraphics.drawText(DataOperations
                                .longToString(super.friendsListLongs[k1])
                                + " is on world "
                                + super.friendsListOnlineStatus[k1], i + c / 2,
                                j + 35, 1, 0xffffff);
        else
          gameGraphics.drawText(DataOperations
                                .longToString(super.friendsListLongs[k1])
                                + " is offline", i + c / 2, j + 35, 1, 0xffffff);
      } else
        gameGraphics.drawText("Click a name to send a message", i + c
                              / 2, j + 35, 1, 0xffffff);

      int k2;
      if (super.mouseX > i && super.mouseX < i + c
          && super.mouseY > (j + c1) - 16 && super.mouseY < j + c1)
        k2 = 0xffff00;
      else
        k2 = 0xffffff;

      gameGraphics.drawText("Click here to add a friend", i + c / 2,
                            (j + c1) - 3, 1, k2);
    }
    if (anInt981 == 1) {
      int l1 = friendsMenu.selectedListIndex(friendsMenuHandle);
      if (l1 >= 0 && super.mouseX < maxWidth && super.mouseX > minWidth) {
        if (super.mouseX > minWidth)
          gameGraphics.drawText("Click to remove "
                                + DataOperations
                                .longToString(super.ignoreListLongs[l1]), i
                                + c / 2, j + 35, 1, 0xffffff);
      } else {
        gameGraphics.drawText("Blocking messages from:", i + c / 2,
                              j + 35, 1, 0xffffff);
      }
      int l2;
      if (super.mouseX > i && super.mouseX < i + c
          && super.mouseY > (j + c1) - 16 && super.mouseY < j + c1)
        l2 = 0xffff00;
      else
        l2 = 0xffffff;
      gameGraphics.drawText("Click here to add a name", i + c / 2,
                            (j + c1) - 3, 1, l2);
    }
    if (!flag)
      return;
    i = super.mouseX
        - (((GameImage) (gameGraphics)).menuDefaultWidth - 199);
    j = super.mouseY - 36;
    if (i >= 0 && j >= 0 && i < 196 && j < 182) {
      friendsMenu.updateActions(i
                                + (((GameImage) (gameGraphics)).menuDefaultWidth - 199),
                                j + 36, super.lastMouseDownButton, super.mouseDownButton);
      if (j <= 24 && mouseButtonClick == 1)
        if (i < 98 && anInt981 == 1) {
          anInt981 = 0;
          friendsMenu.method165(friendsMenuHandle, 0);
        } else if (i > 98 && anInt981 == 0) {
          anInt981 = 1;
          friendsMenu.method165(friendsMenuHandle, 0);
        }
      if (mouseButtonClick == 1 && anInt981 == 0) {
        int i2 = friendsMenu.selectedListIndex(friendsMenuHandle);
        if (i2 >= 0 && super.mouseX < maxWidth)
          if (super.mouseX > minWidth)
            removeFromFriends(super.friendsListLongs[i2]);
          else if (super.friendsListOnlineStatus[i2] != 0) {
            inputBoxType = 2;
            privateMessageTarget = super.friendsListLongs[i2];
            super.inputMessage = "";
            super.enteredMessage = "";
          }
      }
      if (mouseButtonClick == 1 && anInt981 == 1) {
        int j2 = friendsMenu.selectedListIndex(friendsMenuHandle);
        if (j2 >= 0 && super.mouseX < maxWidth
            && super.mouseX > minWidth)
          removeFromIgnoreList(super.ignoreListLongs[j2]);
      }
      if (j > 166 && mouseButtonClick == 1 && anInt981 == 0) {
        inputBoxType = 1;
        super.inputText = "";
        super.enteredText = "";
      }
      if (j > 166 && mouseButtonClick == 1 && anInt981 == 1) {
        inputBoxType = 3;
        super.inputText = "";
        super.enteredText = "";
      }
      mouseButtonClick = 0;
    }
  }

  private final boolean loadSection(int i, int j) {
    if (playerAliveTimeout != 0) {
      engineHandle.playerIsAlive = false;
      return false;
    }
    notInWilderness = false;
    i += wildX;
    j += wildY;
    if (lastWildYSubtract == wildYSubtract && i > anInt789 && i < anInt791
        && j > anInt790 && j < anInt792) {
      engineHandle.playerIsAlive = true;
      return false;
    }
    gameGraphics.drawText("Loading... Please wait", 256 + xAddition, 192 + yAddition, 1, 0xffffff);
    drawChatMessageTabs();
    drawOurSpritesOnScreen();
    gameGraphics.drawImage(aGraphics936, 0, 0);
    int k = getAreaX();
    int l = getAreaY();
    int i1 = (i + 24) / 48;
    int j1 = (j + 24) / 48;
    lastWildYSubtract = wildYSubtract;
    setAreaX(i1 * 48 - 48);
    setAreaY(j1 * 48 - 48);
    anInt789 = i1 * 48 - 32;
    anInt790 = j1 * 48 - 32;
    anInt791 = i1 * 48 + 32;
    anInt792 = j1 * 48 + 32;
    engineHandle.method401(i, j, lastWildYSubtract);
    setAreaX(getAreaX() - wildX);
    setAreaY(getAreaY() - wildY);
    int k1 = getAreaX() - k;
    int l1 = getAreaY() - l;
    for (int i2 = 0; i2 < objectCount; i2++) {
      objectX[i2] -= k1;
      objectY[i2] -= l1;
      int j2 = objectX[i2];
      int l2 = objectY[i2];
      int k3 = objectType[i2];
      int m4 = objectID[i2];
      Model model = objectModelArray[i2];
      try {
        int l4 = objectID[i2];
        int k5;
        int i6;
        if (l4 == 0 || l4 == 4) {
          k5 = EntityHandler.getObjectDef(k3).getWidth();
          i6 = EntityHandler.getObjectDef(k3).getHeight();
        } else {
          i6 = EntityHandler.getObjectDef(k3).getWidth();
          k5 = EntityHandler.getObjectDef(k3).getHeight();
        }
        int j6 = ((j2 + j2 + k5) * magicLoc) / 2;
        int k6 = ((l2 + l2 + i6) * magicLoc) / 2;
        if (j2 >= 0 && l2 >= 0 && j2 < 96 && l2 < 96) {
          gameCamera.addModel(model);
          model.method191(j6, -engineHandle.getAveragedElevation(j6,
                          k6), k6);
          engineHandle.method412(j2, l2, k3, m4);
          if (k3 == 74)
            model.method190(0, -480, 0);
        }
      } catch (RuntimeException runtimeexception) {
        System.out.println("Loc Error: "
                           + runtimeexception.getMessage());
        System.out.println("i:" + i2 + " obj:" + model);
        runtimeexception.printStackTrace();
      }
    }

    for (int k2 = 0; k2 < doorCount; k2++) {
      doorX[k2] -= k1;
      doorY[k2] -= l1;
      int i3 = doorX[k2];
      int l3 = doorY[k2];
      int j4 = doorType[k2];
      int i5 = doorDirection[k2];
      try {
        engineHandle.method408(i3, l3, i5, j4);
        Model model_1 = makeModel(i3, l3, i5, j4, k2);
        doorModel[k2] = model_1;
      } catch (RuntimeException runtimeexception1) {
        System.out.println("Bound Error: "
                           + runtimeexception1.getMessage());
        runtimeexception1.printStackTrace();
      }
    }

    for (int j3 = 0; j3 < groundItemCount; j3++) {
      groundItemX[j3] -= k1;
      groundItemY[j3] -= l1;
    }

    for (int i4 = 0; i4 < playerCount; i4++) {
      Mob mob = playerArray[i4];
      mob.currentX -= k1 * magicLoc;
      mob.currentY -= l1 * magicLoc;
      for (int j5 = 0; j5 <= mob.waypointCurrent; j5++) {
        mob.waypointsX[j5] -= k1 * magicLoc;
        mob.waypointsY[j5] -= l1 * magicLoc;
      }

    }

    for (int k4 = 0; k4 < npcCount; k4++) {
      Mob mob_1 = npcArray[k4];
      mob_1.currentX -= k1 * magicLoc;
      mob_1.currentY -= l1 * magicLoc;
      for (int l5 = 0; l5 <= mob_1.waypointCurrent; l5++) {
        mob_1.waypointsX[l5] -= k1 * magicLoc;
        mob_1.waypointsY[l5] -= l1 * magicLoc;
      }

    }

    engineHandle.playerIsAlive = true;
    return true;
  }

  private final void drawMagicWindow(boolean flag) {
    int i = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
    int j = 36;
    gameGraphics.drawPicture(i - 49, 3, SPRITE_MEDIA_START + 4);
    char c = '\304';
    char c1 = '\266';
    int l;
    int k = l = GameImage.convertRGBToLong(160, 160, 160);
    if (menuMagicPrayersSelected == 0)
      k = GameImage.convertRGBToLong(220, 220, 220);
    else
      l = GameImage.convertRGBToLong(220, 220, 220);
    gameGraphics.drawBoxAlpha(i, j, c / 2, 24, k, 128);
    gameGraphics.drawBoxAlpha(i + c / 2, j, c / 2, 24, l, 128);
    gameGraphics.drawBoxAlpha(i, j + 24, c, 90, GameImage.convertRGBToLong(
                                220, 220, 220), 128);
    gameGraphics.drawBoxAlpha(i, j + 24 + 90, c, c1 - 90 - 24, GameImage
                              .convertRGBToLong(160, 160, 160), 128);
    gameGraphics.drawLineX(i, j + 24, c, 0);
    gameGraphics.drawLineY(i + c / 2, j, 24, 0);
    gameGraphics.drawLineX(i, j + 113, c, 0);
    gameGraphics.drawText("Magic", i + c / 4, j + 16, 4, 0);
    gameGraphics.drawText("Prayers", i + c / 4 + c / 2, j + 16, 4, 0);
    if (menuMagicPrayersSelected == 0) {
      spellMenu.resetListTextCount(spellMenuHandle);
      int i1 = 0;
      for (int spellIndex = 0; spellIndex < EntityHandler.spellCount(); spellIndex++) {
        String s = "@yel@";
        for (Entry e : EntityHandler.getSpellDef(spellIndex)
             .getRunesRequired()) {
          if (hasRequiredRunes((Integer) e.getKey(), (Integer) e
                               .getValue())) {
            continue;
          }
          s = "@whi@";
          break;
        }
        int spellLevel = playerStatCurrent[6];
        if (EntityHandler.getSpellDef(spellIndex).getReqLevel() > spellLevel) {
          s = "@bla@";
        }
        spellMenu.drawMenuListText(spellMenuHandle, i1++, s + "Level "
                                   + EntityHandler.getSpellDef(spellIndex).getReqLevel()
                                   + ": "
                                   + EntityHandler.getSpellDef(spellIndex).getName());

      }

      spellMenu.drawMenu();
      int selectedSpellIndex = spellMenu
                               .selectedListIndex(spellMenuHandle);
      if (selectedSpellIndex != -1) {
        gameGraphics.drawString("Level "
                                + EntityHandler.getSpellDef(selectedSpellIndex)
                                .getReqLevel()
                                + ": "
                                + EntityHandler.getSpellDef(selectedSpellIndex)
                                .getName(), i + 2, j + 124, 1, 0xffff00);
        gameGraphics.drawString(EntityHandler.getSpellDef(
                                  selectedSpellIndex).getDescription(), i + 2, j + 136,
                                0, 0xffffff);
        int i4 = 0;
        for (Entry<Integer, Integer> e : EntityHandler.getSpellDef(
               selectedSpellIndex).getRunesRequired()) {
          int runeID = e.getKey();
          gameGraphics.drawPicture(i + 2 + i4 * 44, j + 150,
                                   SPRITE_ITEM_START
                                   + EntityHandler.getItemDef(runeID)
                                   .getSprite());
          int runeInvCount = inventoryCount(runeID);
          int runeCount = e.getValue();
          String s2 = "@red@";
          if (hasRequiredRunes(runeID, runeCount)) {
            s2 = "@gre@";
          }
          gameGraphics.drawString(
            s2 + runeInvCount + "/" + runeCount, i + 2 + i4
            * 44, j + 150, 1, 0xffffff);
          i4++;
        }
      } else {
        gameGraphics.drawString("Point at a spell for a description",
                                i + 2, j + 124, 1, 0);
      }
    }
    if (menuMagicPrayersSelected == 1) {
      spellMenu.resetListTextCount(spellMenuHandle);
      int j1 = 0;
      for (int j2 = 0; j2 < EntityHandler.prayerCount(); j2++) {
        String s1 = "@whi@";
        if (EntityHandler.getPrayerDef(j2).getReqLevel() > playerStatBase[5])
          s1 = "@bla@";
        if (prayerOn[j2])
          s1 = "@gre@";
        spellMenu.drawMenuListText(spellMenuHandle, j1++, s1 + "Level "
                                   + EntityHandler.getPrayerDef(j2).getReqLevel() + ": "
                                   + EntityHandler.getPrayerDef(j2).getName());
      }
      spellMenu.drawMenu();
      int j3 = spellMenu.selectedListIndex(spellMenuHandle);
      if (j3 != -1) {
        gameGraphics.drawText("Level "
                              + EntityHandler.getPrayerDef(j3).getReqLevel() + ": "
                              + EntityHandler.getPrayerDef(j3).getName(), i + c / 2,
                              j + 130, 1, 0xffff00);
        if(j3 == 13) {
          if(playerStatBase[5] > 39) {
            int percent = (int)((playerStatBase[5] - 40) * 0.6);
            percent += 60;
            if(percent > 100) percent = 100;
            gameGraphics.drawText(percent + "% protection from ranged attack", i + c / 2, j + 145, 0, 0xffffff);
          } else
            gameGraphics.drawText("60% protection from ranged attack", i + c / 2, j + 145, 0, 0xffffff);
        } else
          gameGraphics.drawText(EntityHandler.getPrayerDef(j3)
                                .getDescription(), i + c / 2, j + 145, 0, 0xffffff);

        gameGraphics.drawText("Drain rate: "
                              + EntityHandler.getPrayerDef(j3).getDrainRate(), i + c
                              / 2, j + 160, 1, 0);
      } else {
        gameGraphics.drawString("Point at a prayer for a description",
                                i + 2, j + 124, 1, 0);
      }
    }
    if (!flag)
      return;
    i = super.mouseX
        - (((GameImage) (gameGraphics)).menuDefaultWidth - 199);
    j = super.mouseY - 36;
    if (i >= 0 && j >= 0 && i < 196 && j < 182) {
      spellMenu.updateActions(i
                              + (((GameImage) (gameGraphics)).menuDefaultWidth - 199),
                              j + 36, super.lastMouseDownButton, super.mouseDownButton);
      if (j <= 24 && mouseButtonClick == 1)
        if (i < 98 && menuMagicPrayersSelected == 1) {
          menuMagicPrayersSelected = 0;
          prayerMenuIndex = spellMenu.getMenuIndex(spellMenuHandle);
          spellMenu.method165(spellMenuHandle, magicMenuIndex);
        } else if (i > 98 && menuMagicPrayersSelected == 0) {
          menuMagicPrayersSelected = 1;
          magicMenuIndex = spellMenu.getMenuIndex(spellMenuHandle);
          spellMenu.method165(spellMenuHandle, prayerMenuIndex);
        }
      if (mouseButtonClick == 1 && menuMagicPrayersSelected == 0) {
        int k1 = spellMenu.selectedListIndex(spellMenuHandle);
        if (k1 != -1) {
          int k2 = playerStatCurrent[6];
          if (EntityHandler.getSpellDef(k1).getReqLevel() > k2) {
            displayMessage(
              "Your magic ability is not high enough for this spell",
              3, 0);
          } else {
            int k3 = 0;
            for (Entry<Integer, Integer> e : EntityHandler
                 .getSpellDef(k1).getRunesRequired()) {
              if (!hasRequiredRunes(e.getKey(), e.getValue())) {
                displayMessage(
                  "You don't have all the reagents you need for this spell",
                  3, 0);
                k3 = -1;
                break;
              }
              k3++;
            }
            if (k3 == EntityHandler.getSpellDef(k1).getRuneCount()) {
              selectedSpell = k1;
              selectedItem = -1;
            }
          }
        }
      }
      if (mouseButtonClick == 1 && menuMagicPrayersSelected == 1) {
        int l1 = spellMenu.selectedListIndex(spellMenuHandle);
        if (l1 != -1) {
          int l2 = playerStatBase[5];
          if (EntityHandler.getPrayerDef(l1).getReqLevel() > l2)
            displayMessage(
              "Your prayer ability is not high enough for this prayer",
              3, 0);
          else if (playerStatCurrent[5] == 0)
            displayMessage(
              "You have run out of prayer points. Return to a church to recharge",
              3, 0);
          else if (prayerOn[l1]) {
            super.streamClass.createPacket(248);
            super.streamClass.addByte(l1);
            super.streamClass.formatPacket();
            prayerOn[l1] = false;
            playSound("prayeroff");
          } else {
            super.streamClass.createPacket(56);
            super.streamClass.addByte(l1);
            super.streamClass.formatPacket();
            prayerOn[l1] = true;
            playSound("prayeron");
          }
        }
      }
      mouseButtonClick = 0;
    }
  }
  protected final void handleMenuKeyDown(int key) {
    if (!menusLoaded)
      return;
    switch (key) {
    case 1009:
      Config.storeConfig("width", "" + Display.displaymodes.get(0).getWidth());
      Config.storeConfig("height", "" + Display.displaymodes.get(0).getHeight());
      Config.storeConfig("refreshRate", "" + Display.displaymodes.get(0).getRefreshRate());
      Config.storeConfig("bitDepth", "" + Display.displaymodes.get(0).getBitDepth());
      displayMessage("@gre@Client size reset to default, please restart!", 3, 0);
      break;
    case 1003:  // page down
      if (cameraHeight < 3000)
        cameraHeight += 16;
      break;
    case 1002: //page up
      if (cameraHeight > 400)
        cameraHeight -= 16;
      break;
    case 1000: // home
      cameraHeight = 750;
      cameraVertical = 920;
      fogVar = 0;
      break;
    case 127: // delete
      gameMenu.updateText(chatHandle, "");
      break;

    case 1004: // arrow up
      currentChat--;
      if(currentChat < 0) {
        currentChat = 0;
        return;
      }
      gameMenu.updateText(chatHandle, messages.get(currentChat));
      break;
    case 1005: // arrow down
      currentChat++;
      if(currentChat >= messages.size()) {
        currentChat = messages.size() - 1;
        return;
      }
      gameMenu.updateText(chatHandle, messages.get(currentChat));
      break;
    case 1018: // F11
      recording = !recording;
      if (recording) {
        try {
          frames.clear();
          File file = getEmptyFile(true);
          Recorder recorder = new Recorder(windowWidth,
                                           windowHeight + 11, Config.MOVIE_FPS, frames, file
                                           .getAbsolutePath(), "video.quicktime");
          displayMessage("Recording movie to " + file.getName(), 3, 0);
          new Thread(recorder).start();
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        frames.add(null);
        displayMessage("Movie saved.", 3, 0);
      }
      break;
    case 1019: // F12
      takeScreenshot(true);
      break;


    }

    if (loggedIn == 0) {
      if (loginScreenNumber == 0)
        menuWelcome.keyDown(key);
      if (loginScreenNumber == 1)
        menuNewUser.keyDown(key);
      if (loginScreenNumber == 2)
        menuLogin.keyDown(key);
    }
    if (loggedIn == 1) {
      if (showCharacterLookScreen) {
        characterDesignMenu.keyDown(key);
        return;
      }
      if (inputBoxType == 0 && showAbuseWindow == 0)
        gameMenu.keyDown(key);
    }
  }

  private final void drawShopBox() {
    if (mouseButtonClick != 0) {
      mouseButtonClick = 0;
      int i = super.mouseX - 52 - xAddition;
      int j = super.mouseY - 44 - yAddition;
      if (i >= 0 && j >= 12 && i < 408 && j < 246) {
        int k = 0;
        for (int i1 = 0; i1 < 5; i1++) {
          for (int i2 = 0; i2 < 8; i2++) {
            int l2 = 7 + i2 * 49;
            int l3 = 28 + i1 * 34;
            if (i > l2 && i < l2 + 49 && j > l3 && j < l3 + 34
                && shopItems[k] != -1) {
              selectedShopItemIndex = k;
              selectedShopItemType = shopItems[k];
            }
            k++;
          }

        }

        if (selectedShopItemIndex >= 0) {
          int j2 = shopItems[selectedShopItemIndex];
          if (j2 != -1) {
            if (shopItemCount[selectedShopItemIndex] > 0 && i > 298
                && j >= 204 && i < 408 && j <= 215) {
              int i4 = shopItemsBuyPrice[selectedShopItemIndex];// (shopItemBuyPriceModifier
              // *
              // EntityHandler.getItemDef(j2).getBasePrice())
              // /
              // 100;
              super.streamClass.createPacket(128);
              super.streamClass
              .add2ByteInt(shopItems[selectedShopItemIndex]);
              super.streamClass.add4ByteInt(i4);
              super.streamClass.formatPacket();
            }
            if (inventoryCount(j2) > 0 && i > 2 && j >= 229
                && i < 112 && j <= 240) {
              int j4 = shopItemsSellPrice[selectedShopItemIndex];// (shopItemSellPriceModifier
              // *
              // EntityHandler.getItemDef(j2).getBasePrice())
              // /
              // 100;
              super.streamClass.createPacket(255);
              super.streamClass
              .add2ByteInt(shopItems[selectedShopItemIndex]);
              super.streamClass.add4ByteInt(j4);
              super.streamClass.formatPacket();
            }
          }
        }
      } else {
        super.streamClass.createPacket(253);
        super.streamClass.formatPacket();
        showShop = false;
        return;
      }
    }
    int byte0 = 52 + xAddition;
    int byte1 = 44 + yAddition;
    gameGraphics.drawBox(byte0, byte1, 408, 12, 192);
    int l = 0x989898;
    gameGraphics.drawBoxAlpha(byte0, byte1 + 12, 408, 17, l, 160);
    gameGraphics.drawBoxAlpha(byte0, byte1 + 29, 8, 170, l, 160);
    gameGraphics.drawBoxAlpha(byte0 + 399, byte1 + 29, 9, 170, l, 160);
    gameGraphics.drawBoxAlpha(byte0, byte1 + 199, 408, 47, l, 160);
    gameGraphics.drawString("Buying and selling items", byte0 + 1,
                            byte1 + 10, 1, 0xffffff);
    int j1 = 0xffffff;
    if (super.mouseX > byte0 + 320 && super.mouseY >= byte1
        && super.mouseX < byte0 + 408 && super.mouseY < byte1 + 12)
      j1 = 0xff0000;
    gameGraphics.drawBoxTextRight("Close window", byte0 + 406, byte1 + 10,
                                  1, j1);
    gameGraphics.drawString("Shops stock in green", byte0 + 2, byte1 + 24,
                            1, 65280);
    gameGraphics.drawString("Number you own in blue", byte0 + 135,
                            byte1 + 24, 1, 65535);
    gameGraphics.drawString("Your money: " + inventoryCount(10) + "gp",
                            byte0 + 280, byte1 + 24, 1, 0xffff00);
    int k2 = 0xd0d0d0;
    int k3 = 0;
    for (int k4 = 0; k4 < 5; k4++) {
      for (int l4 = 0; l4 < 8; l4++) {
        int j5 = byte0 + 7 + l4 * 49;
        int i6 = byte1 + 28 + k4 * 34;
        if (selectedShopItemIndex == k3)
          gameGraphics.drawBoxAlpha(j5, i6, 49, 34, 0xff0000, 160);
        else
          gameGraphics.drawBoxAlpha(j5, i6, 49, 34, k2, 160);
        gameGraphics.drawBoxEdge(j5, i6, 50, 35, 0);
        if (shopItems[k3] != -1) {
          gameGraphics.spriteClip4(j5, i6, 48, 32, SPRITE_ITEM_START + EntityHandler.getItemDef(shopItems[k3]).getSprite(), EntityHandler.getItemDef(shopItems[k3]).getPictureMask(), 0, 0, false);
          gameGraphics.drawString(String.valueOf(shopItemCount[k3]),
                                  j5 + 1, i6 + 10, 1, 65280);
          gameGraphics.drawBoxTextRight(String
                                        .valueOf(inventoryCount(shopItems[k3])), j5 + 47,
                                        i6 + 10, 1, 65535);
        }
        k3++;
      }

    }

    gameGraphics.drawLineX(byte0 + 5, byte1 + 222, 398, 0);
    if (selectedShopItemIndex == -1) {
      gameGraphics.drawText("Select an object to buy or sell",
                            byte0 + 204, byte1 + 214, 3, 0xffff00);
      return;
    }
    int i5 = shopItems[selectedShopItemIndex];
    if (i5 != -1) {
      if (shopItemCount[selectedShopItemIndex] > 0) {
        int j6 = shopItemsBuyPrice[selectedShopItemIndex];//(shopItemBuyPriceModifier * EntityHandler.getItemDef(
        //  i5).getBasePrice()) / 100;
        gameGraphics.drawString("Buy a new "
                                + EntityHandler.getItemDef(i5).getName() + " for " + j6
                                + "gp", byte0 + 2, byte1 + 214, 1, 0xffff00);
        int k1 = 0xffffff;
        if (super.mouseX > byte0 + 298 && super.mouseY >= byte1 + 204
            && super.mouseX < byte0 + 408
            && super.mouseY <= byte1 + 215)
          k1 = 0xff0000;
        gameGraphics.drawBoxTextRight("Click here to buy", byte0 + 405,
                                      byte1 + 214, 3, k1);
      } else {
        gameGraphics.drawText(
          "This item is not currently available to buy",
          byte0 + 204, byte1 + 214, 3, 0xffff00);
      }
      if (inventoryCount(i5) > 0) {
        int k6 = shopItemsSellPrice[selectedShopItemIndex];//(shopItemSellPriceModifier * EntityHandler.getItemDef(
        //i5).getBasePrice()) / 100;
        gameGraphics.drawBoxTextRight("Sell your "
                                      + EntityHandler.getItemDef(i5).getName() + " for " + k6
                                      + "gp", byte0 + 405, byte1 + 239, 1, 0xffff00);
        int l1 = 0xffffff;
        if (super.mouseX > byte0 + 2 && super.mouseY >= byte1 + 229
            && super.mouseX < byte0 + 112
            && super.mouseY <= byte1 + 240)
          l1 = 0xff0000;
        gameGraphics.drawString("Click here to sell", byte0 + 2,
                                byte1 + 239, 3, l1);
        return;
      }
      gameGraphics.drawText("You do not have any of this item to sell",
                            byte0 + 204, byte1 + 239, 3, 0xffff00);
    }
  }

  private final void drawGameMenu() {
    gameMenu = new Menu(gameGraphics, 10);
    messagesHandleType2 = gameMenu.method159(5, windowHeight - 85,
                          windowWidth - 10, 56, 1, 20, true);
    chatHandle = gameMenu.method160(7, windowHeight - 10, windowWidth - 14,
                                    14, 1, 80, false, true);
    messagesHandleType5 = gameMenu.method159(5, windowHeight - 65,
                          windowWidth - 10, 56, 1, 20, true);
    messagesHandleType6 = gameMenu.method159(5, windowHeight - 65,
                          windowWidth - 10, 56, 1, 20, true);
    gameMenu.setFocus(chatHandle);
  }

  protected final byte[] load(String filename) {
    mudclient.loadCachedFile(filename);
    return super.load(new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data" + File.separator + filename);
  }
  /**
   * Draws our options menu (client size etc)
   * @param flag
   */
  private final void drawOurOptionsMenu(boolean flag) {
    int i = ((GameImage) (gameGraphics)).menuDefaultWidth - 232;

    int j = 36;

    int c = 360;
    gameGraphics.drawBoxAlpha(i, 36, c, 176, GameImage.convertRGBToLong(181, 181, 181), 160);

    /**
     * Draws the gray box behind the icon
     */
    gameGraphics.drawBox(i, 3, 31, 32, GameImage.convertRGBToLong(0, 0, 131));


    int temp = 10;
    gameGraphics.drawBox(i, 26, 274, temp, GameImage.convertRGBToLong(0, 0, 131));
    gameGraphics.drawBox(i, 26 + temp, 274, 1, GameImage.convertRGBToLong(0, 0, 0));

    gameGraphics.drawString("screen size", i + 147, 26 + temp, 4, 0xffffff);
    int k = i + 3;
    int i1 = j + 15;

    i1 += 15;
    gameGraphics.drawString("          Screen size               @gre@" + reso.getResolution(), k, i1, 1,  0xffffff);
    i1 += 15;
    gameGraphics.drawString("          Refresh rate              @gre@" + reso.getRefreshRate(), k, i1, 1, 0xffffff);
    i1 += 30;
    gameGraphics.drawString("      Window size will change after you", k, i1, 1,  0xffffff);
    i1 += 15;
    gameGraphics.drawString("      restart the client.", k, i1, 1,  0xffffff);
    i1 += 30;
    gameGraphics.drawString("      Fullsreen functionality removed.", k, i1, 1, 0xffffff);

    if (!flag)
      return;
    i = super.mouseX - (((GameImage) (gameGraphics)).menuDefaultWidth - 199);
    j = super.mouseY - 36;
    if (i >= 0 && j >= 0 && i < 196 && j < 265) {
      int l1 = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
      byte byte0 = 36;
      char c1 = '\304';
      int l = l1 + 3;
      int j1 = byte0 + 30;
      //amera error
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        reso.findNextResolution();
        Config.storeConfig("width", "" + Display.displaymodes.get(Resolutions.resolutionSetting).getWidth());
        Config.storeConfig("height", "" + Display.displaymodes.get(Resolutions.resolutionSetting).getHeight());
        Config.storeConfig("refreshRate", "" + Display.displaymodes.get(Resolutions.resolutionSetting).getRefreshRate());
        Config.storeConfig("bitDepth", "" + Display.displaymodes.get(Resolutions.resolutionSetting).getBitDepth());
      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;

      boolean flag1 = false;
      j1 += 35;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {

      }
      j1 += 15;

      j1 += 20;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1)

        mouseButtonClick = 0;
    }
  }
  private final void drawOptionsMenu(boolean flag) {
    int i = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
    int j = 36;
    gameGraphics.drawPicture(i - 49, 3, SPRITE_MEDIA_START + 6);
    char c = '\304';
    gameGraphics.drawBoxAlpha(i, 36, c, 65, GameImage.convertRGBToLong(181,
                              181, 181), 160);
    gameGraphics.drawBoxAlpha(i, 101, c, 65, GameImage.convertRGBToLong(
                                181, 181, 181), 160);
    gameGraphics.drawBoxAlpha(i, 166, c, 95, GameImage.convertRGBToLong(
                                181, 181, 181), 160);
    gameGraphics.drawBoxAlpha(i, 261, c, 52, GameImage.convertRGBToLong(
                                181, 181, 181), 160);
    int k = i + 3;
    int i1 = j + 15;
    gameGraphics.drawString("Game options - click to toggle", k, i1, 1, 0);
    i1 += 15;
    if (configAutoCameraAngle)
      gameGraphics.drawString("Camera angle mode - @gre@Auto", k, i1, 1,
                              0xffffff);
    else
      gameGraphics.drawString("Camera angle mode - @red@Manual", k, i1,
                              1, 0xffffff);
    i1 += 15;
    if (configMouseButtons)
      gameGraphics.drawString("Mouse buttons - @red@One", k, i1, 1,
                              0xffffff);
    else
      gameGraphics.drawString("Mouse buttons - @gre@Two", k, i1, 1,
                              0xffffff);
    i1 += 15;
    if (configSoundEffects)
      gameGraphics.drawString("Sound effects - @red@off", k, i1, 1,
                              0xffffff);
    else
      gameGraphics.drawString("Sound effects - @gre@on", k, i1, 1,
                              0xffffff);
    i1 += 15;
    gameGraphics
    .drawString("Client assists - click to toggle", k, i1, 1, 0);
    i1 += 15;
    if (showRoof)
      gameGraphics
      .drawString("Hide Roofs - @red@off", k, i1, 1, 0xffffff);
    else
      gameGraphics.drawString("Hide Roofs - @gre@on", k, i1, 1, 0xffffff);
    i1 += 15;
    if (autoScreenshot)
      gameGraphics.drawString("Auto Screenshots - @gre@on", k, i1, 1,
                              0xffffff);
    else
      gameGraphics.drawString("Auto Screenshots - @red@off", k, i1, 1,
                              0xffffff);
    i1 += 15;
    if (combatWindow)
      gameGraphics.drawString("Fightmode Selector - @gre@on", k, i1, 1,
                              0xffffff);
    else
      gameGraphics.drawString("Fightmode Selector - @red@off", k, i1, 1,
                              0xffffff);
    i1 += 15;
    if (fog)
      gameGraphics.drawString("Fog of War - @gre@on", k, i1, 1, 0xffffff);
    else
      gameGraphics
      .drawString("Fog of War - @red@off", k, i1, 1, 0xffffff);
    i1 += 15;
    i1 += 5;
    gameGraphics.drawString("Privacy settings. Will be applied to", i + 3,
                            i1, 1, 0);
    i1 += 15;
    gameGraphics.drawString("all people not on your friends list", i + 3,
                            i1, 1, 0);
    i1 += 15;
    if (super.blockChatMessages == 0)
      gameGraphics.drawString("Block chat messages: @red@<off>", i + 3,
                              i1, 1, 0xffffff);
    else
      gameGraphics.drawString("Block chat messages: @gre@<on>", i + 3,
                              i1, 1, 0xffffff);
    i1 += 15;
    if (super.blockPrivateMessages == 0)
      gameGraphics.drawString("Block private messages: @red@<off>",
                              i + 3, i1, 1, 0xffffff);
    else
      gameGraphics.drawString("Block private messages: @gre@<on>", i + 3,
                              i1, 1, 0xffffff);
    i1 += 15;
    if (super.blockTradeRequests == 0)
      gameGraphics.drawString("Block trade requests: @red@<off>", i + 3,
                              i1, 1, 0xffffff);
    else
      gameGraphics.drawString("Block trade requests: @gre@<on>", i + 3,
                              i1, 1, 0xffffff);
    i1 += 15;
    if (super.blockDuelRequests == 0)
      gameGraphics.drawString("Block duel requests: @red@<off>", i + 3,
                              i1, 1, 0xffffff);
    else
      gameGraphics.drawString("Block duel requests: @gre@<on>", i + 3,
                              i1, 1, 0xffffff);
    i1 += 15;
    i1 += 5;
    gameGraphics.drawString("Always logout when you finish", k, i1, 1, 0);
    i1 += 15;
    int k1 = 0xffffff;
    if (super.mouseX > k && super.mouseX < k + c && super.mouseY > i1 - 12
        && super.mouseY < i1 + 4)
      k1 = 0xffff00;
    gameGraphics.drawString("Click here to logout", i + 3, i1, 1, k1);
    if (!flag)
      return;
    i = super.mouseX
        - (((GameImage) (gameGraphics)).menuDefaultWidth - 199);
    j = super.mouseY - 36;
    if (i >= 0 && j >= 0 && i < 196 && j < 265) {
      int l1 = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
      byte byte0 = 36;
      char c1 = '\304';
      int l = l1 + 3;
      int j1 = byte0 + 30;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        configAutoCameraAngle = !configAutoCameraAngle;
        super.streamClass.createPacket(157);
        super.streamClass.addByte(0);
        super.streamClass.addByte(configAutoCameraAngle ? 1 : 0);
        super.streamClass.formatPacket();
      }
      j1 += 15;
      //amera error
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        configMouseButtons = !configMouseButtons;
        super.streamClass.createPacket(157);
        super.streamClass.addByte(2);
        super.streamClass.addByte(configMouseButtons ? 1 : 0);
        super.streamClass.formatPacket();
      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        configSoundEffects = !configSoundEffects;
        super.streamClass.createPacket(157);
        super.streamClass.addByte(3);
        super.streamClass.addByte(configSoundEffects ? 1 : 0);
        super.streamClass.formatPacket();
      }
      j1 += 15;
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        showRoof = !showRoof;
        super.streamClass.createPacket(157);
        super.streamClass.addByte(4);
        super.streamClass.addByte(showRoof ? 1 : 0);
        super.streamClass.formatPacket();
      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        autoScreenshot = !autoScreenshot;
        super.streamClass.createPacket(157);
        super.streamClass.addByte(5);
        super.streamClass.addByte(autoScreenshot ? 1 : 0);
        super.streamClass.formatPacket();
      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        combatWindow = !combatWindow;
        super.streamClass.createPacket(157);
        super.streamClass.addByte(6);
        super.streamClass.addByte(combatWindow ? 1 : 0);
        super.streamClass.formatPacket();
      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        fog = !fog;
      }
      j1 += 15;

      boolean flag1 = false;
      j1 += 35;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        super.blockChatMessages = 1 - super.blockChatMessages;
        flag1 = true;
      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        super.blockPrivateMessages = 1 - super.blockPrivateMessages;
        flag1 = true;
      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        super.blockTradeRequests = 1 - super.blockTradeRequests;
        flag1 = true;
      }
      j1 += 15;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1) {
        super.blockDuelRequests = 1 - super.blockDuelRequests;
        flag1 = true;
      }
      j1 += 15;
      if (flag1)
        sendUpdatedPrivacyInfo(super.blockChatMessages,
                               super.blockPrivateMessages, super.blockTradeRequests,
                               super.blockDuelRequests);
      j1 += 20;
      if (super.mouseX > l && super.mouseX < l + c1
          && super.mouseY > j1 - 12 && super.mouseY < j1 + 4
          && mouseButtonClick == 1)
        logout();
      mouseButtonClick = 0;
    }
  }

  private final void processGame() {
    if (systemUpdate > 1) {
      systemUpdate--;
    }
    sendPingPacketReadPacketData();
    if (logoutTimeout > 0) {
      logoutTimeout--;
    }
    if (ourPlayer.currentSprite == 8 || ourPlayer.currentSprite == 9) {
      lastWalkTimeout = 500;
    }
    if (lastWalkTimeout > 0) {
      lastWalkTimeout--;
    }
    if (showCharacterLookScreen) {
      drawCharacterLookScreen();
      return;
    }
    for (int i = 0; i < playerCount; i++) {
      Mob mob = playerArray[i];
      int k = (mob.waypointCurrent + 1) % 10;
      if (mob.waypointEndSprite != k) {
        int i1 = -1;
        int l2 = mob.waypointEndSprite;
        int j4;
        if (l2 < k)
          j4 = k - l2;
        else
          j4 = (10 + k) - l2;
        int j5 = 4;
        if (j4 > 2)
          j5 = (j4 - 1) * 4;
        if (mob.waypointsX[l2] - mob.currentX > magicLoc * 3
            || mob.waypointsY[l2] - mob.currentY > magicLoc * 3
            || mob.waypointsX[l2] - mob.currentX < -magicLoc * 3
            || mob.waypointsY[l2] - mob.currentY < -magicLoc * 3
            || j4 > 8) {
          mob.currentX = mob.waypointsX[l2];
          mob.currentY = mob.waypointsY[l2];
        } else {
          if (mob.currentX < mob.waypointsX[l2]) {
            mob.currentX += j5;
            mob.stepCount++;
            i1 = 2;
          } else if (mob.currentX > mob.waypointsX[l2]) {
            mob.currentX -= j5;
            mob.stepCount++;
            i1 = 6;
          }
          if (mob.currentX - mob.waypointsX[l2] < j5
              && mob.currentX - mob.waypointsX[l2] > -j5)
            mob.currentX = mob.waypointsX[l2];
          if (mob.currentY < mob.waypointsY[l2]) {
            mob.currentY += j5;
            mob.stepCount++;
            if (i1 == -1)
              i1 = 4;
            else if (i1 == 2)
              i1 = 3;
            else
              i1 = 5;
          } else if (mob.currentY > mob.waypointsY[l2]) {
            mob.currentY -= j5;
            mob.stepCount++;
            if (i1 == -1)
              i1 = 0;
            else if (i1 == 2)
              i1 = 1;
            else
              i1 = 7;
          }
          if (mob.currentY - mob.waypointsY[l2] < j5
              && mob.currentY - mob.waypointsY[l2] > -j5)
            mob.currentY = mob.waypointsY[l2];
        }
        if (i1 != -1)
          mob.currentSprite = i1;
        if (mob.currentX == mob.waypointsX[l2]
            && mob.currentY == mob.waypointsY[l2])
          mob.waypointEndSprite = (l2 + 1) % 10;
      } else {
        mob.currentSprite = mob.nextSprite;
      }
      if (mob.lastMessageTimeout > 0)
        mob.lastMessageTimeout--;
      if (mob.anInt163 > 0)
        mob.anInt163--;
      if (mob.combatTimer > 0)
        mob.combatTimer--;
      if (playerAliveTimeout > 0) {
        playerAliveTimeout--;
        if (playerAliveTimeout == 0)
          displayMessage(
            "You have been granted another life. Be more careful this time!",
            3, 0);
        if (playerAliveTimeout == 0)
          displayMessage(
            "You retain your skills. Your objects land where you died",
            3, 0);
      }
    }

    for (int j = 0; j < npcCount; j++) {

      Mob mob_1 = npcArray[j];
      if(mob_1 == null) {
        System.out.println("MOB == NULL, npcCount: " + npcCount + ", j: " + j);
        System.exit(1);
      }
      int j1 = (mob_1.waypointCurrent + 1) % 10;
      if (mob_1.waypointEndSprite != j1) {
        int i3 = -1;
        int k4 = mob_1.waypointEndSprite;
        int k5;
        if (k4 < j1)
          k5 = j1 - k4;
        else
          k5 = (10 + j1) - k4;
        int l5 = 4;
        if (k5 > 2)
          l5 = (k5 - 1) * 4;
        if (mob_1.waypointsX[k4] - mob_1.currentX > magicLoc * 3
            || mob_1.waypointsY[k4] - mob_1.currentY > magicLoc * 3
            || mob_1.waypointsX[k4] - mob_1.currentX < -magicLoc * 3
            || mob_1.waypointsY[k4] - mob_1.currentY < -magicLoc * 3
            || k5 > 8) {
          mob_1.currentX = mob_1.waypointsX[k4];
          mob_1.currentY = mob_1.waypointsY[k4];
        } else {
          if (mob_1.currentX < mob_1.waypointsX[k4]) {
            mob_1.currentX += l5;
            mob_1.stepCount++;
            i3 = 2;
          } else if (mob_1.currentX > mob_1.waypointsX[k4]) {
            mob_1.currentX -= l5;
            mob_1.stepCount++;
            i3 = 6;
          }
          if (mob_1.currentX - mob_1.waypointsX[k4] < l5
              && mob_1.currentX - mob_1.waypointsX[k4] > -l5)
            mob_1.currentX = mob_1.waypointsX[k4];
          if (mob_1.currentY < mob_1.waypointsY[k4]) {
            mob_1.currentY += l5;
            mob_1.stepCount++;
            if (i3 == -1)
              i3 = 4;
            else if (i3 == 2)
              i3 = 3;
            else
              i3 = 5;
          } else if (mob_1.currentY > mob_1.waypointsY[k4]) {
            mob_1.currentY -= l5;
            mob_1.stepCount++;
            if (i3 == -1)
              i3 = 0;
            else if (i3 == 2)
              i3 = 1;
            else
              i3 = 7;
          }
          if (mob_1.currentY - mob_1.waypointsY[k4] < l5
              && mob_1.currentY - mob_1.waypointsY[k4] > -l5)
            mob_1.currentY = mob_1.waypointsY[k4];
        }
        if (i3 != -1)
          mob_1.currentSprite = i3;
        if (mob_1.currentX == mob_1.waypointsX[k4]
            && mob_1.currentY == mob_1.waypointsY[k4])
          mob_1.waypointEndSprite = (k4 + 1) % 10;
      } else {
        mob_1.currentSprite = mob_1.nextSprite;
        if (mob_1.type == 43)
          mob_1.stepCount++;
      }
      if (mob_1.lastMessageTimeout > 0)
        mob_1.lastMessageTimeout--;
      if (mob_1.anInt163 > 0)
        mob_1.anInt163--;
      if (mob_1.combatTimer > 0)
        mob_1.combatTimer--;
    }

    if (mouseOverMenu != 2) {
      if (GameImage.anInt346 > 0)
        anInt658++;
      if (GameImage.anInt347 > 0)
        anInt658 = 0;
      GameImage.anInt346 = 0;
      GameImage.anInt347 = 0;
    }
    for (int l = 0; l < playerCount; l++) {
      Mob mob_2 = playerArray[l];
      if (mob_2.anInt176 > 0)
        mob_2.anInt176--;
    }

    if (cameraAutoAngleDebug) {
      if (lastAutoCameraRotatePlayerX - ourPlayer.currentX < -500
          || lastAutoCameraRotatePlayerX - ourPlayer.currentX > 500
          || lastAutoCameraRotatePlayerY - ourPlayer.currentY < -500
          || lastAutoCameraRotatePlayerY - ourPlayer.currentY > 500) {
        lastAutoCameraRotatePlayerX = ourPlayer.currentX;
        lastAutoCameraRotatePlayerY = ourPlayer.currentY;
      }
    } else {
      if (lastAutoCameraRotatePlayerX - ourPlayer.currentX < -500
          || lastAutoCameraRotatePlayerX - ourPlayer.currentX > 500
          || lastAutoCameraRotatePlayerY - ourPlayer.currentY < -500
          || lastAutoCameraRotatePlayerY - ourPlayer.currentY > 500) {
        lastAutoCameraRotatePlayerX = ourPlayer.currentX;
        lastAutoCameraRotatePlayerY = ourPlayer.currentY;
      }
      if (lastAutoCameraRotatePlayerX != ourPlayer.currentX)
        lastAutoCameraRotatePlayerX += (ourPlayer.currentX - lastAutoCameraRotatePlayerX)
                                       / (16 + (cameraHeight - 500) / 15);
      if (lastAutoCameraRotatePlayerY != ourPlayer.currentY)
        lastAutoCameraRotatePlayerY += (ourPlayer.currentY - lastAutoCameraRotatePlayerY)
                                       / (16 + (cameraHeight - 500) / 15);
      if (configAutoCameraAngle) {
        int k1 = cameraAutoAngle * 32;
        int j3 = k1 - cameraRotation;
        byte byte0 = 1;
        if (j3 != 0) {
          cameraRotationBaseAddition++;
          if (j3 > 128) {
            byte0 = -1;
            j3 = 256 - j3;
          } else if (j3 > 0)
            byte0 = 1;
          else if (j3 < -128) {
            byte0 = 1;
            j3 = 256 + j3;
          } else if (j3 < 0) {
            byte0 = -1;
            j3 = -j3;
          }
          cameraRotation += ((cameraRotationBaseAddition * j3 + 255) / 256)
                            * byte0;
          cameraRotation &= 0xff;
        } else {
          cameraRotationBaseAddition = 0;
        }
      }
    }
    if (anInt658 > 20) {
      aBoolean767 = false;
      anInt658 = 0;
    }
    if (sleeping) {
      ignoreNext = true;
      if (super.enteredText.length() > 0) {
        super.streamClass.createPacket(200);
        super.streamClass.addString(super.enteredText);
        if (!aBoolean767) {
          super.streamClass.addByte(0);
          aBoolean767 = true;
        }
        super.streamClass.formatPacket();
        super.inputText = "";
        super.enteredText = "";
        gameMenu.updateText(chatHandle, "");
        sleepMessage = "Please wait...";
      }
      if (super.lastMouseDownButton != 0
          && super.mouseX >= ((windowWidth / 2) - 100)
          && super.mouseX < ((windowWidth / 2) + 100)
          && super.mouseY > 280 && super.mouseY < 310) {
        super.streamClass.createPacket(200);
        super.streamClass.addString("-null-");
        if (!aBoolean767) {
          super.streamClass.addByte(0);
          aBoolean767 = true;
        }
        super.streamClass.formatPacket();
        super.inputText = "";
        super.enteredText = "";
        sleepMessage = "        Please wait...";
      }
      super.lastMouseDownButton = 0;
      return;
    }
    if (super.mouseY > windowHeight - 4) {
      if (super.mouseX > 15 + xAddition && super.mouseX < 96 + xAddition
          && super.lastMouseDownButton == 1)
        messagesTab = 0;
      if (super.mouseX > 110 + xAddition && super.mouseX < 194 + xAddition
          && super.lastMouseDownButton == 1) {
        messagesTab = 1;
        gameMenu.anIntArray187[messagesHandleType2] = 0xf423f;
      }
      if (super.mouseX > 215 + xAddition && super.mouseX < 295 + xAddition
          && super.lastMouseDownButton == 1) {
        messagesTab = 2;
        gameMenu.anIntArray187[messagesHandleType5] = 0xf423f;
      }
      if (super.mouseX > 315 + xAddition && super.mouseX < 395 + xAddition
          && super.lastMouseDownButton == 1) {
        messagesTab = 3;
        gameMenu.anIntArray187[messagesHandleType6] = 0xf423f;
      }
      if (super.mouseX > 417 + xAddition && super.mouseX < 497 + xAddition
          && super.lastMouseDownButton == 1) {
        showAbuseWindow = 1;
        abuseSelectedType = 0;
        super.inputText = "";
        super.enteredText = "";
      }
      super.lastMouseDownButton = 0;
      super.mouseDownButton = 0;
    }
    gameMenu.updateActions(super.mouseX, super.mouseY,
                           super.lastMouseDownButton, super.mouseDownButton);
    if (messagesTab > 0 && super.mouseX >= 494
        && super.mouseY >= windowHeight - 66)
      super.lastMouseDownButton = 0;
    if (gameMenu.hasActivated(chatHandle)) {
      String s = gameMenu.getText(chatHandle);
      gameMenu.updateText(chatHandle, "");
      if (ignoreNext) {
        ignoreNext = false;
        return;
      }
      if (s.startsWith("::")) {
        s = s.substring(2);
        if (!handleCommand(s) && !sleeping && !ignoreNext) {
          sendChatString(s);
          if(messages.size() == 0 || !messages.get(messages.size() - 1).equalsIgnoreCase("::" + s)) {
            messages.add("::" + s);
            currentChat = messages.size();
          } else if(messages.get(messages.size() - 1).equalsIgnoreCase("::" + s)) {
            currentChat = messages.size();
          }
        }
      } else if (!sleeping && !ignoreNext) {
        byte[] chatMessage = DataConversions.stringToByteArray(s);
        sendChatMessage(chatMessage, chatMessage.length);
        s = DataConversions.byteToString(chatMessage, 0,
                                         chatMessage.length).trim();
        if (s.toLowerCase().trim().startsWith(";;"))
          return;
        if(messages.size() == 0 || !messages.get(messages.size() - 1).equalsIgnoreCase(s)) {
          messages.add(s);
          currentChat = messages.size();
        } else if(messages.get(messages.size() - 1).equalsIgnoreCase(s)) {
          currentChat = messages.size();
        }

        ourPlayer.lastMessageTimeout = 150;
        ourPlayer.lastMessage = s;
        displayMessage(ourPlayer.name + ": " + s, 2, ourPlayer.admin);
      }
    }
    if (messagesTab == 0) {
      for (int l1 = 0; l1 < messagesTimeout.length; l1++)
        if (messagesTimeout[l1] > 0)
          messagesTimeout[l1]--;

    }
    if (playerAliveTimeout != 0)
      super.lastMouseDownButton = 0;
    if (showTradeWindow || showDuelWindow) {
      if (super.mouseDownButton != 0)
        mouseDownTime++;
      else
        mouseDownTime = 0;
      if (mouseDownTime > 500)
        itemIncrement += 100000;
      else if (mouseDownTime > 350)
        itemIncrement += 10000;
      else if (mouseDownTime > 250)
        itemIncrement += 1000;
      else if (mouseDownTime > 150)
        itemIncrement += 100;
      else if (mouseDownTime > 100)
        itemIncrement += 10;
      else if (mouseDownTime > 50)
        itemIncrement++;
      else if (mouseDownTime > 20 && (mouseDownTime & 5) == 0)
        itemIncrement++;
    } else {
      mouseDownTime = 0;
      itemIncrement = 0;
    }
    if (super.lastMouseDownButton == 1)
      mouseButtonClick = 1;
    else if (super.lastMouseDownButton == 2)
      mouseButtonClick = 2;
    gameCamera.updateMouseCoords(super.mouseX, super.mouseY);
    super.lastMouseDownButton = 0;
    if (configAutoCameraAngle) {
      if (cameraRotationBaseAddition == 0 || cameraAutoAngleDebug) {
        if (super.keyLeftDown) {
          cameraAutoAngle = cameraAutoAngle + 1 & 7;
          super.keyLeftDown = false;
          if (!zoomCamera) {
            if ((cameraAutoAngle & 1) == 0)
              cameraAutoAngle = cameraAutoAngle + 1 & 7;
            for (int i2 = 0; i2 < 8; i2++) {
              if (enginePlayerVisible(cameraAutoAngle))
                break;
              cameraAutoAngle = cameraAutoAngle + 1 & 7;
            }

          }
        }
        if (super.keyRightDown) {
          cameraAutoAngle = cameraAutoAngle + 7 & 7;
          super.keyRightDown = false;
          if (!zoomCamera) {
            if ((cameraAutoAngle & 1) == 0)
              cameraAutoAngle = cameraAutoAngle + 7 & 7;
            for (int j2 = 0; j2 < 8; j2++) {
              if (enginePlayerVisible(cameraAutoAngle))
                break;
              cameraAutoAngle = cameraAutoAngle + 7 & 7;
            }

          }
        }
      }
    } else if (super.keyLeftDown)
      cameraRotation = cameraRotation + 2 & 0xff;
    else if (super.keyRightDown)
      cameraRotation = cameraRotation - 2 & 0xff;


    if (actionPictureType > 0)
      actionPictureType--;
    else if (actionPictureType < 0)
      actionPictureType++;
    gameCamera.method301(17);
    modelUpdatingTimer++;
    if (modelUpdatingTimer > 5) {
      modelUpdatingTimer = 0;
      modelFireLightningSpellNumber = (modelFireLightningSpellNumber + 1) % 3;
      modelTorchNumber = (modelTorchNumber + 1) % 4;
      modelClawSpellNumber = (modelClawSpellNumber + 1) % 5;
    }
    for (int k2 = 0; k2 < objectCount; k2++) {
      int l3 = objectX[k2];
      int l4 = objectY[k2];
      if (l3 >= 0 && l4 >= 0 && l3 < 96 && l4 < 96
          && objectType[k2] == 74)
        objectModelArray[k2].method188(1, 0, 0);
    }

    for (int i4 = 0; i4 < anInt892; i4++) {
      anIntArray923[i4]++;
      if (anIntArray923[i4] > 50) {
        anInt892--;
        for (int i5 = i4; i5 < anInt892; i5++) {
          anIntArray944[i5] = anIntArray944[i5 + 1];
          anIntArray757[i5] = anIntArray757[i5 + 1];
          anIntArray923[i5] = anIntArray923[i5 + 1];
          anIntArray782[i5] = anIntArray782[i5 + 1];
        }

      }
    }

  }//command == 11
  public static HashMap<String, File> ctfsounds = new HashMap<String, File>();
  private final void loadSounds() {
    try {
      drawDownloadProgress("Unpacking Sound effects", 90);
      sounds = load("sounds1.mem");
      audioReader = new AudioReader();
      // CTF sounds
      String[] names = {"blueflagreturn", "bluescores", "enemyhasyourflag", "countdown",
                        "firstblood", "humuliation", "preparetofight", "rampage", "redflagreturn",
                        "redscores", "unstoppable", "youhavetheflag", "yourteamhasflag", "youwin"
                       };

      for(final String s : names) {
        new Thread(new Runnable() {
          public void run() {
            ctfsounds.put(s, mudclient.loadCachedFile(s + ".mp3"));
          }
        }).start();
      }
      return;
    } catch (Throwable throwable) {
      System.out.println("Unable to init sounds:" + throwable);
    }
  }

  private final void drawCombatStyleWindow() {
    byte byte0 = 7;
    byte byte1 = 15;
    char c = '\257';
    if (mouseButtonClick != 0) {
      for (int i = 0; i < 5; i++) {
        if (i <= 0 || super.mouseX <= byte0
            || super.mouseX >= byte0 + c
            || super.mouseY <= byte1 + i * 20
            || super.mouseY >= byte1 + i * 20 + 20)
          continue;
        combatStyle = i - 1;
        mouseButtonClick = 0;
        super.streamClass.createPacket(42);
        super.streamClass.addByte(combatStyle);
        super.streamClass.formatPacket();
        break;
      }

    }
    for (int j = 0; j < 5; j++) {
      if (j == combatStyle + 1)
        gameGraphics.drawBoxAlpha(byte0, byte1 + j * 20, c, 20,
                                  GameImage.convertRGBToLong(255, 0, 0), 128);
      else
        gameGraphics.drawBoxAlpha(byte0, byte1 + j * 20, c, 20,
                                  GameImage.convertRGBToLong(190, 190, 190), 128);
      gameGraphics.drawLineX(byte0, byte1 + j * 20, c, 0);
      gameGraphics.drawLineX(byte0, byte1 + j * 20 + 20, c, 0);
    }

    gameGraphics.drawText("Select combat style", byte0 + c / 2, byte1 + 16,
                          3, 0xffffff);
    gameGraphics.drawText("Controlled (+1 of each)", byte0 + c / 2,
                          byte1 + 36, 3, 0);
    gameGraphics.drawText("Aggressive (+3 strength)", byte0 + c / 2,
                          byte1 + 56, 3, 0);
    gameGraphics.drawText("Accurate   (+3 attack)", byte0 + c / 2,
                          byte1 + 76, 3, 0);
    gameGraphics.drawText("Defensive  (+3 defense)", byte0 + c / 2,
                          byte1 + 96, 3, 0);
  }

  private final void drawDuelConfirmWindow() {
    int byte0 = 22 + xAddition;
    int byte1 = 36 + yAddition;
    gameGraphics.drawBox(byte0, byte1, 468, 16, 192);
    int i = 0x989898;
    gameGraphics.drawBoxAlpha(byte0, byte1 + 16, 468, 246, i, 160);
    gameGraphics.drawText("Please confirm your duel with @yel@"
                          + DataOperations.longToString(duelOpponentNameLong),
                          byte0 + 234, byte1 + 12, 1, 0xffffff);
    gameGraphics.drawText("Your stake:", byte0 + 117, byte1 + 30, 1,
                          0xffff00);
    for (int j = 0; j < duelConfirmMyItemCount; j++) {
      String s = EntityHandler.getItemDef(duelConfirmMyItems[j])
                 .getName();
      if (EntityHandler.getItemDef(duelConfirmMyItems[j]).isStackable())
        s = s + " x " + method74(duelConfirmMyItemsCount[j]);
      gameGraphics.drawText(s, byte0 + 117, byte1 + 42 + j * 12, 1,
                            0xffffff);
    }

    if (duelConfirmMyItemCount == 0)
      gameGraphics.drawText("Nothing!", byte0 + 117, byte1 + 42, 1,
                            0xffffff);
    gameGraphics.drawText("Your opponent's stake:", byte0 + 351,
                          byte1 + 30, 1, 0xffff00);
    for (int k = 0; k < duelConfirmOpponentItemCount; k++) {
      String s1 = EntityHandler.getItemDef(duelConfirmOpponentItems[k])
                  .getName();
      if (EntityHandler.getItemDef(duelConfirmOpponentItems[k])
          .isStackable())
        s1 = s1 + " x " + method74(duelConfirmOpponentItemsCount[k]);
      gameGraphics.drawText(s1, byte0 + 351, byte1 + 42 + k * 12, 1,
                            0xffffff);
    }

    if (duelConfirmOpponentItemCount == 0)
      gameGraphics.drawText("Nothing!", byte0 + 351, byte1 + 42, 1,
                            0xffffff);
    if (duelCantRetreat == 0)
      gameGraphics.drawText("You can retreat from this duel",
                            byte0 + 234, byte1 + 180, 1, 65280);
    else
      gameGraphics.drawText("No retreat is possible!", byte0 + 234,
                            byte1 + 180, 1, 0xff0000);
    if (duelUseMagic == 0)
      gameGraphics.drawText("Magic may be used", byte0 + 234,
                            byte1 + 192, 1, 65280);
    else
      gameGraphics.drawText("Magic cannot be used", byte0 + 234,
                            byte1 + 192, 1, 0xff0000);
    if (duelUsePrayer == 0)
      gameGraphics.drawText("Prayer may be used", byte0 + 234,
                            byte1 + 204, 1, 65280);
    else
      gameGraphics.drawText("Prayer cannot be used", byte0 + 234,
                            byte1 + 204, 1, 0xff0000);
    if (duelUseWeapons == 0)
      gameGraphics.drawText("Weapons may be used", byte0 + 234,
                            byte1 + 216, 1, 65280);
    else
      gameGraphics.drawText("Weapons cannot be used", byte0 + 234,
                            byte1 + 216, 1, 0xff0000);
    gameGraphics.drawText(
      "If you are sure click 'Accept' to begin the duel",
      byte0 + 234, byte1 + 230, 1, 0xffffff);
    if (!duelWeAccept) {
      gameGraphics.drawPicture((byte0 + 118) - 35, byte1 + 238,
                               SPRITE_MEDIA_START + 25);
      gameGraphics.drawPicture((byte0 + 352) - 35, byte1 + 238,
                               SPRITE_MEDIA_START + 26);
    } else {
      gameGraphics.drawText("Waiting for other player...", byte0 + 234,
                            byte1 + 250, 1, 0xffff00);
    }
    if (mouseButtonClick == 1) {
      if (super.mouseX < byte0 || super.mouseY < byte1
          || super.mouseX > byte0 + 468 || super.mouseY > byte1 + 262) {
        showDuelConfirmWindow = false;
        super.streamClass.createPacket(35);
        super.streamClass.formatPacket();
      }
      if (super.mouseX >= (byte0 + 118) - 35
          && super.mouseX <= byte0 + 118 + 70
          && super.mouseY >= byte1 + 238
          && super.mouseY <= byte1 + 238 + 21) {
        duelWeAccept = true;
        super.streamClass.createPacket(87);
        super.streamClass.formatPacket();
      }
      if (super.mouseX >= (byte0 + 352) - 35
          && super.mouseX <= byte0 + 353 + 70
          && super.mouseY >= byte1 + 238
          && super.mouseY <= byte1 + 238 + 21) {
        showDuelConfirmWindow = false;
        super.streamClass.createPacket(35);
        super.streamClass.formatPacket();
      }
      mouseButtonClick = 0;
    }
  }

  private final void updateBankItems() {
    bankItemCount = newBankItemCount;
    for (int i = 0; i < newBankItemCount; i++) {
      bankItems[i] = newBankItems[i];
      bankItemsCount[i] = newBankItemsCount[i];
    }

    for (int j = 0; j < inventoryCount; j++) {
      if (bankItemCount >= bankItemsMax)
        break;
      int k = getInventoryItems()[j];
      boolean flag = false;
      for (int l = 0; l < bankItemCount; l++) {
        if (bankItems[l] != k)
          continue;
        flag = true;
        break;
      }

      if (!flag) {
        bankItems[bankItemCount] = k;
        bankItemsCount[bankItemCount] = 0;
        bankItemCount++;
      }
    }

  }

  private final void makeCharacterDesignMenu() {
    characterDesignMenu = new Menu(gameGraphics, 100);
    characterDesignMenu.drawText(256, 10, "Please design Your Character",
                                 4, true);
    int i = 140;
    int j = 34;
    i += 116;
    j -= 10;
    characterDesignMenu.drawText(i - 55, j + 110, "Front", 3, true);
    characterDesignMenu.drawText(i, j + 110, "Side", 3, true);
    characterDesignMenu.drawText(i + 55, j + 110, "Back", 3, true);
    byte byte0 = 54;
    j += 145;
    characterDesignMenu.method157(i - byte0, j, 53, 41);
    characterDesignMenu.drawText(i - byte0, j - 8, "Head", 1, true);
    characterDesignMenu.drawText(i - byte0, j + 8, "Type", 1, true);
    characterDesignMenu.method158(i - byte0 - 40, j, SPRITE_UTIL_START + 7);
    characterDesignHeadButton1 = characterDesignMenu.makeButton(i - byte0
                                 - 40, j, 20, 20);
    characterDesignMenu.method158((i - byte0) + 40, j,
                                  SPRITE_UTIL_START + 6);
    characterDesignHeadButton2 = characterDesignMenu.makeButton(
                                   (i - byte0) + 40, j, 20, 20);
    characterDesignMenu.method157(i + byte0, j, 53, 41);
    characterDesignMenu.drawText(i + byte0, j - 8, "Hair", 1, true);
    characterDesignMenu.drawText(i + byte0, j + 8, "Colour", 1, true);
    characterDesignMenu.method158((i + byte0) - 40, j,
                                  SPRITE_UTIL_START + 7);
    characterDesignHairColourButton1 = characterDesignMenu.makeButton(
                                         (i + byte0) - 40, j, 20, 20);
    characterDesignMenu.method158(i + byte0 + 40, j, SPRITE_UTIL_START + 6);
    characterDesignHairColourButton2 = characterDesignMenu.makeButton(i
                                       + byte0 + 40, j, 20, 20);
    j += 50;
    characterDesignMenu.method157(i - byte0, j, 53, 41);
    characterDesignMenu.drawText(i - byte0, j, "Gender", 1, true);
    characterDesignMenu.method158(i - byte0 - 40, j, SPRITE_UTIL_START + 7);
    characterDesignGenderButton1 = characterDesignMenu.makeButton(i - byte0
                                   - 40, j, 20, 20);
    characterDesignMenu.method158((i - byte0) + 40, j,
                                  SPRITE_UTIL_START + 6);
    characterDesignGenderButton2 = characterDesignMenu.makeButton(
                                     (i - byte0) + 40, j, 20, 20);
    characterDesignMenu.method157(i + byte0, j, 53, 41);
    characterDesignMenu.drawText(i + byte0, j - 8, "Top", 1, true);
    characterDesignMenu.drawText(i + byte0, j + 8, "Colour", 1, true);
    characterDesignMenu.method158((i + byte0) - 40, j,
                                  SPRITE_UTIL_START + 7);
    characterDesignTopColourButton1 = characterDesignMenu.makeButton(
                                        (i + byte0) - 40, j, 20, 20);
    characterDesignMenu.method158(i + byte0 + 40, j, SPRITE_UTIL_START + 6);
    characterDesignTopColourButton2 = characterDesignMenu.makeButton(i
                                      + byte0 + 40, j, 20, 20);
    j += 50;
    characterDesignMenu.method157(i - byte0, j, 53, 41);
    characterDesignMenu.drawText(i - byte0, j - 8, "Skin", 1, true);
    characterDesignMenu.drawText(i - byte0, j + 8, "Colour", 1, true);
    characterDesignMenu.method158(i - byte0 - 40, j, SPRITE_UTIL_START + 7);
    characterDesignSkinColourButton1 = characterDesignMenu.makeButton(i
                                       - byte0 - 40, j, 20, 20);
    characterDesignMenu.method158((i - byte0) + 40, j,
                                  SPRITE_UTIL_START + 6);
    characterDesignSkinColourButton2 = characterDesignMenu.makeButton(
                                         (i - byte0) + 40, j, 20, 20);
    characterDesignMenu.method157(i + byte0, j, 53, 41);
    characterDesignMenu.drawText(i + byte0, j - 8, "Bottom", 1, true);
    characterDesignMenu.drawText(i + byte0, j + 8, "Colour", 1, true);
    characterDesignMenu.method158((i + byte0) - 40, j,
                                  SPRITE_UTIL_START + 7);
    characterDesignBottomColourButton1 = characterDesignMenu.makeButton(
                                           (i + byte0) - 40, j, 20, 20);
    characterDesignMenu.method158(i + byte0 + 40, j, SPRITE_UTIL_START + 6);
    characterDesignBottomColourButton2 = characterDesignMenu.makeButton(i
                                         + byte0 + 40, j, 20, 20);
    j += 82;
    j -= 35;
    characterDesignMenu.drawBox(i, j, 200, 30);
    characterDesignMenu.drawText(i, j, "Accept", 4, false);
    characterDesignAcceptButton = characterDesignMenu.makeButton(i, j, 200,
                                  30);
  }

  private final void drawAbuseWindow2() {
    if (super.enteredText.length() > 0) {
      String s = super.enteredText.trim();
      super.inputText = "";
      super.enteredText = "";
      if (s.length() > 0) {
        long l = DataOperations.stringLength12ToLong(s);
        super.streamClass.createPacket(7);
        super.streamClass.addTwo4ByteInts(l);
        super.streamClass.addByte(abuseSelectedType);
        super.streamClass.formatPacket();
      }
      showAbuseWindow = 0;
      return;
    }
    gameGraphics.drawBox(56 + xAddition, 130 + yAddition, 400, 100, 0);
    gameGraphics.drawBoxEdge(56 + xAddition, 130 + yAddition, 400, 100, 0xffffff);
    int i = 160 + yAddition;
    gameGraphics.drawText(
      "Now type the name of the offending player, and press enter",
      256 + xAddition, i, 1, 0xffff00);
    i += 18;
    gameGraphics.drawText("Name: " + super.inputText + "*", 256 + xAddition, i, 4,
                          0xffffff);
    i = 222 + yAddition;
    int j = 0xffffff;
    if (super.mouseX > 196 + xAddition && super.mouseX < 316 + xAddition && super.mouseY > i - 13
        && super.mouseY < i + 2) {
      j = 0xffff00;
      if (mouseButtonClick == 1) {
        mouseButtonClick = 0;
        showAbuseWindow = 0;
      }
    }
    gameGraphics.drawText("Click here to cancel", 256 + xAddition, i, 1, j);
    if (mouseButtonClick == 1
        && (super.mouseX < 56 + xAddition || super.mouseX > 456 + xAddition
            || super.mouseY < 130 + yAddition || super.mouseY > 230 + yAddition)) {
      mouseButtonClick = 0;
      showAbuseWindow = 0;
    }
  }

  public final void displayMessage(String message, int type, int status) {
    if (type == 2 || type == 4 || type == 6) {
      for (; message.length() > 5 && message.charAt(0) == '@'
           && message.charAt(4) == '@'; message = message.substring(5))
        ;
    }
    if (message.startsWith("%", 0)) {
      message = message.substring(1);
      status = 4;
    }
    if (message.startsWith("&", 0)) {
      message = message.substring(1);
      status = 33;
    }

    // if (command == 131) {
    message = message.replaceAll("\\#pmd\\#", "");
    message = message.replaceAll("\\#mod\\#", "");
    message = message.replaceAll("\\#adm\\#", "");
    if (type == 2)
      message = "@yel@" + message;
    if (type == 3 || type == 4)
      message = "@whi@" + message;
    if (type == 6)
      message = "@cya@" + message;
    if (status == 1)
      message = "#pmd#" + message;

    if (status == 2)
      message = "#mod#" + message;

    if (status == 3)
      message = "#adm#" + message;

    if (status == 4) {
      MessageQueue.getQueue();
      MessageQueue.addMessage(new Message(message));
      gameMenu.addString(messagesHandleType6, message, false);
      return;
    }
    //MessageQueue
    if (status == 33) {
      MessageQueue.getQueue();
      MessageQueue.addMessage(new Message(message, true));
      gameMenu.addString(messagesHandleType6, message, false);
      return;
    }

    if (messagesTab != 0) {
      if (type == 4 || type == 3)
        anInt952 = 200;
      if (type == 2 && messagesTab != 1)
        anInt953 = 200;
      if (type == 5 && messagesTab != 2)
        anInt954 = 200;
      if (type == 6 && messagesTab != 3)
        anInt955 = 200;
      if (type == 3 && messagesTab != 0)
        messagesTab = 0;
      if (type == 6 && messagesTab != 3 && messagesTab != 0)
        messagesTab = 0;
    }

    for (int k = messagesArray.length - 1; k > 0; k--) {
      messagesArray[k] = messagesArray[k - 1];
      messagesTimeout[k] = messagesTimeout[k - 1];
    }

    messagesArray[0] = message;
    /**
     * Change this to add longer chat msg time
     */
    messagesTimeout[0] = 300;
    if (type == 2)
      if (gameMenu.anIntArray187[messagesHandleType2] == gameMenu.menuListTextCount[messagesHandleType2] - 4)
        gameMenu.addString(messagesHandleType2, message, true);
      else
        gameMenu.addString(messagesHandleType2, message, false);
    if (type == 5)
      if (gameMenu.anIntArray187[messagesHandleType5] == gameMenu.menuListTextCount[messagesHandleType5] - 4)
        gameMenu.addString(messagesHandleType5, message, true);
      else
        gameMenu.addString(messagesHandleType5, message, false);
    if (type == 6) {
      if (gameMenu.anIntArray187[messagesHandleType6] == gameMenu.menuListTextCount[messagesHandleType6] - 4) {
        gameMenu.addString(messagesHandleType6, message, true);
        return;
      }
      gameMenu.addString(messagesHandleType6, message, false);
    }
  }

  protected final void logoutAndStop() {
    sendLogoutPacket();
    garbageCollect();
    if (audioReader != null) {
      audioReader.stopAudio();
    }
  }

  private final void method98(int i, String s) {
    int j = objectX[i];
    int k = objectY[i];
    int l = j - ourPlayer.currentX / 128;
    int i1 = k - ourPlayer.currentY / 128;
    byte byte0 = 7;
    if (j >= 0 && k >= 0 && j < 96 && k < 96 && l > -byte0 && l < byte0
        && i1 > -byte0 && i1 < byte0) {
      try {
        gameCamera.removeModel(objectModelArray[i]);
        int j1 = EntityHandler.storeModel(s);
        Model model = gameDataModels[j1].method203();
        gameCamera.addModel(model);
        model.method184(true, 48, 48, -50, -10, -50);
        model.method205(objectModelArray[i]);
        model.anInt257 = i;
        objectModelArray[i] = model;
      } catch (Exception e) {
        //e.printStackTrace();
      }
    }
  }
  /**
   * This function is called on player login.
   */
  protected final void resetVars() {
    systemUpdate = 0;
    combatStyle = 0;
    logoutTimeout = 0;
    loginScreenNumber = 0;
    loggedIn = 1;
    flagged = 0;
    resetPrivateMessageStrings();
    gameGraphics.method211();
    gameGraphics.drawImage(aGraphics936, 0, 0);
    for (int i = 0; i < objectCount; i++) {
      gameCamera.removeModel(objectModelArray[i]);
      engineHandle.updateObject(objectX[i], objectY[i], objectType[i],
                                objectID[i]);
    }

    for (int j = 0; j < doorCount; j++) {
      gameCamera.removeModel(doorModel[j]);
      engineHandle.updateDoor(doorX[j], doorY[j], doorDirection[j],
                              doorType[j]);
    }

    objectCount = 0;
    doorCount = 0;
    groundItemCount = 0;
    playerCount = 0;
    for (int k = 0; k < mobArray.length; k++)
      mobArray[k] = null;

    for (int l = 0; l < playerArray.length; l++)
      playerArray[l] = null;

    npcCount = 0;
    for (int i1 = 0; i1 < npcRecordArray.length; i1++)
      npcRecordArray[i1] = null;

    for (int j1 = 0; j1 < npcArray.length; j1++)
      npcArray[j1] = null;

    for (int k1 = 0; k1 < prayerOn.length; k1++)
      prayerOn[k1] = false;

    mouseButtonClick = 0;
    super.lastMouseDownButton = 0;
    super.mouseDownButton = 0;
    showShop = false;
    showBank = false;
    super.friendsCount = 0;

    questionMenu.close();
    loadConfigFilter();
    loadMedia();
    loadEntity();
    loadTextures();
    loadModels();
    loadSounds();
  }

  private final void drawTradeWindow() {
    if (mouseButtonClick != 0 && itemIncrement == 0)
      itemIncrement = 1;
    if (itemIncrement > 0) {
      int i = super.mouseX - 22 - xAddition;
      int j = super.mouseY - 36 - yAddition;
      if (i >= 0 && j >= 0 && i < 468 && j < 262) {
        if (i > 216 && j > 30 && i < 462 && j < 235) {
          int k = (i - 217) / 49 + ((j - 31) / 34) * 5;
          if (k >= 0 && k < inventoryCount) {
            boolean flag = false;
            int l1 = 0;
            int k2 = getInventoryItems()[k];
            for (int k3 = 0; k3 < tradeMyItemCount; k3++)
              if (tradeMyItems[k3] == k2)
                if (EntityHandler.getItemDef(k2).isStackable()) {
                  for (int i4 = 0; i4 < itemIncrement; i4++) {
                    if (tradeMyItemsCount[k3] < inventoryItemsCount[k])
                      tradeMyItemsCount[k3]++;
                    flag = true;
                  }

                } else {
                  l1++;
                }

            if (inventoryCount(k2) <= l1)
              flag = true;
            if (!flag && tradeMyItemCount < 12) {
              tradeMyItems[tradeMyItemCount] = k2;
              tradeMyItemsCount[tradeMyItemCount] = 1;
              tradeMyItemCount++;
              flag = true;
            }
            if (flag) {
              super.streamClass.createPacket(70);
              super.streamClass.addByte(tradeMyItemCount);
              for (int j4 = 0; j4 < tradeMyItemCount; j4++) {
                super.streamClass.add2ByteInt(tradeMyItems[j4]);
                super.streamClass
                .add4ByteInt(tradeMyItemsCount[j4]);
              }
              super.streamClass.formatPacket();
              tradeOtherAccepted = false;
              tradeWeAccepted = false;
            }
          }
        }
        if (i > 8 && j > 30 && i < 205 && j < 133) {
          int l = (i - 9) / 49 + ((j - 31) / 34) * 4;
          if (l >= 0 && l < tradeMyItemCount) {
            int j1 = tradeMyItems[l];
            for (int i2 = 0; i2 < itemIncrement; i2++) {
              if (EntityHandler.getItemDef(j1).isStackable()
                  && tradeMyItemsCount[l] > 1) {
                tradeMyItemsCount[l]--;
                continue;
              }// shopItem
              tradeMyItemCount--;
              mouseDownTime = 0;
              for (int l2 = l; l2 < tradeMyItemCount; l2++) {
                tradeMyItems[l2] = tradeMyItems[l2 + 1];
                tradeMyItemsCount[l2] = tradeMyItemsCount[l2 + 1];
              }

              break;
            }

            super.streamClass.createPacket(70);
            super.streamClass.addByte(tradeMyItemCount);
            for (int i3 = 0; i3 < tradeMyItemCount; i3++) {
              super.streamClass.add2ByteInt(tradeMyItems[i3]);
              super.streamClass
              .add4ByteInt(tradeMyItemsCount[i3]);
            }

            super.streamClass.formatPacket();
            tradeOtherAccepted = false;
            tradeWeAccepted = false;
          }
        }
        if (i >= 217 && j >= 238 && i <= 286 && j <= 259) {
          tradeWeAccepted = true;
          super.streamClass.createPacket(211);
          super.streamClass.formatPacket();
        }
        if (i >= 394 && j >= 238 && i < 463 && j < 259) {
          showTradeWindow = false;
          super.streamClass.createPacket(216);
          super.streamClass.formatPacket();
        }
      } else if (mouseButtonClick != 0) {
        showTradeWindow = false;
        super.streamClass.createPacket(216);
        super.streamClass.formatPacket();
      }
      mouseButtonClick = 0;
      itemIncrement = 0;
    }
    if (!showTradeWindow)
      return;
    int byte0 = 22 + xAddition;
    int byte1 = 36 + yAddition;
    gameGraphics.drawBox(byte0, byte1, 468, 12, 192);
    int i1 = 0x989898;
    gameGraphics.drawBoxAlpha(byte0, byte1 + 12, 468, 18, i1, 160);
    gameGraphics.drawBoxAlpha(byte0, byte1 + 30, 8, 248, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 205, byte1 + 30, 11, 248, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 462, byte1 + 30, 6, 248, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 133, 197, 22, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 258, 197, 20, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 216, byte1 + 235, 246, 43, i1, 160);
    int k1 = 0xd0d0d0;
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 30, 197, 103, k1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 155, 197, 103, k1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 216, byte1 + 30, 246, 205, k1, 160);
    for (int j2 = 0; j2 < 4; j2++)
      gameGraphics.drawLineX(byte0 + 8, byte1 + 30 + j2 * 34, 197, 0);

    for (int j3 = 0; j3 < 4; j3++)
      gameGraphics.drawLineX(byte0 + 8, byte1 + 155 + j3 * 34, 197, 0);

    for (int l3 = 0; l3 < 7; l3++)
      gameGraphics.drawLineX(byte0 + 216, byte1 + 30 + l3 * 34, 246, 0);

    for (int k4 = 0; k4 < 6; k4++) {
      if (k4 < 5)
        gameGraphics.drawLineY(byte0 + 8 + k4 * 49, byte1 + 30, 103, 0);
      if (k4 < 5)
        gameGraphics
        .drawLineY(byte0 + 8 + k4 * 49, byte1 + 155, 103, 0);
      gameGraphics.drawLineY(byte0 + 216 + k4 * 49, byte1 + 30, 205, 0);
    }

    gameGraphics.drawString("Trading with: " + tradeOtherPlayerName,
                            byte0 + 1, byte1 + 10, 1, 0xffffff);
    gameGraphics.drawString("Your Offer", byte0 + 9, byte1 + 27, 4,
                            0xffffff);
    gameGraphics.drawString("Opponent's Offer", byte0 + 9, byte1 + 152, 4,
                            0xffffff);
    gameGraphics.drawString("Your Inventory", byte0 + 216, byte1 + 27, 4,
                            0xffffff);
    if (!tradeWeAccepted)
      gameGraphics.drawPicture(byte0 + 217, byte1 + 238,
                               SPRITE_MEDIA_START + 25);
    gameGraphics.drawPicture(byte0 + 394, byte1 + 238,
                             SPRITE_MEDIA_START + 26);
    if (tradeOtherAccepted) {
      gameGraphics.drawText("Other player", byte0 + 341, byte1 + 246, 1,
                            0xffffff);
      gameGraphics.drawText("has accepted", byte0 + 341, byte1 + 256, 1,
                            0xffffff);
    }
    if (tradeWeAccepted) {
      gameGraphics.drawText("Waiting for", byte0 + 217 + 35, byte1 + 246,
                            1, 0xffffff);
      gameGraphics.drawText("other player", byte0 + 217 + 35,
                            byte1 + 256, 1, 0xffffff);
    }
    for (int l4 = 0; l4 < inventoryCount; l4++) {
      int i5 = 217 + byte0 + (l4 % 5) * 49;
      int k5 = 31 + byte1 + (l4 / 5) * 34;
      gameGraphics.spriteClip4(i5, k5, 48, 32, SPRITE_ITEM_START
                               + EntityHandler.getItemDef(getInventoryItems()[l4]).getSprite(),
                               EntityHandler.getItemDef(getInventoryItems()[l4])
                               .getPictureMask(), 0, 0, false);
      if (EntityHandler.getItemDef(getInventoryItems()[l4]).isStackable())
        gameGraphics.drawString(
          String.valueOf(inventoryItemsCount[l4]), i5 + 1,
          k5 + 10, 1, 0xffff00);
    }

    for (int j5 = 0; j5 < tradeMyItemCount; j5++) {
      int l5 = 9 + byte0 + (j5 % 4) * 49;
      int j6 = 31 + byte1 + (j5 / 4) * 34;
      gameGraphics
      .spriteClip4(l5, j6, 48, 32, SPRITE_ITEM_START
                   + EntityHandler.getItemDef(tradeMyItems[j5])
                   .getSprite(), EntityHandler.getItemDef(
                     tradeMyItems[j5]).getPictureMask(), 0, 0, false);
      if (EntityHandler.getItemDef(tradeMyItems[j5]).isStackable())
        gameGraphics.drawString(String.valueOf(tradeMyItemsCount[j5]),
                                l5 + 1, j6 + 10, 1, 0xffff00);
      if (super.mouseX > l5 && super.mouseX < l5 + 48
          && super.mouseY > j6 && super.mouseY < j6 + 32)
        gameGraphics.drawString(EntityHandler.getItemDef(
                                  tradeMyItems[j5]).getName()
                                + ": @whi@"
                                + EntityHandler.getItemDef(tradeMyItems[j5])
                                .getDescription(), byte0 + 8, byte1 + 273, 1,
                                0xffff00);
    }

    for (int i6 = 0; i6 < tradeOtherItemCount; i6++) {
      int k6 = 9 + byte0 + (i6 % 4) * 49;
      int l6 = 156 + byte1 + (i6 / 4) * 34;
      gameGraphics.spriteClip4(k6, l6, 48, 32,
                               SPRITE_ITEM_START
                               + EntityHandler.getItemDef(tradeOtherItems[i6])
                               .getSprite(), EntityHandler.getItemDef(
                                 tradeOtherItems[i6]).getPictureMask(), 0, 0, false);
      if (EntityHandler.getItemDef(tradeOtherItems[i6]).isStackable())
        gameGraphics.drawString(String
                                .valueOf(tradeOtherItemsCount[i6]), k6 + 1, l6 + 10, 1,
                                0xffff00);
      if (super.mouseX > k6 && super.mouseX < k6 + 48
          && super.mouseY > l6 && super.mouseY < l6 + 32)
        gameGraphics.drawString(EntityHandler.getItemDef(
                                  tradeOtherItems[i6]).getName()
                                + ": @whi@"
                                + EntityHandler.getItemDef(tradeOtherItems[i6])
                                .getDescription(), byte0 + 8, byte1 + 273, 1,
                                0xffff00);
    }

  }

  private final boolean enginePlayerVisible(int i) {
    int j = ourPlayer.currentX / 128;
    int k = ourPlayer.currentY / 128;
    for (int l = 2; l >= 1; l--) {
      if (i == 1
          && ((engineHandle.walkableValue[j][k - l] & 0x80) == 128
              || (engineHandle.walkableValue[j - l][k] & 0x80) == 128 || (engineHandle.walkableValue[j
                  - l][k - l] & 0x80) == 128))
        return false;
      if (i == 3
          && ((engineHandle.walkableValue[j][k + l] & 0x80) == 128
              || (engineHandle.walkableValue[j - l][k] & 0x80) == 128 || (engineHandle.walkableValue[j
                  - l][k + l] & 0x80) == 128))
        return false;
      if (i == 5
          && ((engineHandle.walkableValue[j][k + l] & 0x80) == 128
              || (engineHandle.walkableValue[j + l][k] & 0x80) == 128 || (engineHandle.walkableValue[j
                  + l][k + l] & 0x80) == 128))
        return false;
      if (i == 7
          && ((engineHandle.walkableValue[j][k - l] & 0x80) == 128
              || (engineHandle.walkableValue[j + l][k] & 0x80) == 128 || (engineHandle.walkableValue[j
                  + l][k - l] & 0x80) == 128))
        return false;
      if (i == 0 && (engineHandle.walkableValue[j][k - l] & 0x80) == 128)
        return false;
      if (i == 2 && (engineHandle.walkableValue[j - l][k] & 0x80) == 128)
        return false;
      if (i == 4 && (engineHandle.walkableValue[j][k + l] & 0x80) == 128)
        return false;
      if (i == 6 && (engineHandle.walkableValue[j + l][k] & 0x80) == 128)
        return false;
    }

    return true;
  }

  private Mob getLastPlayer(int serverIndex) {
    for (int i1 = 0; i1 < lastPlayerCount; i1++) {
      if (lastPlayerArray[i1].serverIndex == serverIndex) {
        return lastPlayerArray[i1];
      }
    }
    return null;
  }

  private Mob getLastNpc(int serverIndex) {
    for (int i1 = 0; i1 < lastNpcCount; i1++) {
      if (lastNpcArray[i1].serverIndex == serverIndex) {
        return lastNpcArray[i1];
      }
    }
    return null;
  }

  /**
   * Cache of key - value pairs sent from the server.
   */
  public Map<String, Object> cache = new HashMap<String, Object>();

  protected final void handleIncomingPacket(int command, int length,
      byte data[]) {
    try {
      if(command == 132) {
        byte keyLenght = data[1];
        String key = new String(data, 2, keyLenght);
        byte valLength = data[2 + keyLenght];
        String value = new String(data, 3 + keyLenght, valLength);
        cache.put(key, value);
      }
      if(command == 133) {
        byte keyLenght = data[1];
        String key = new String(data, 2, keyLenght);
        Long value = DataOperations.getUnsigned8Bytes(data, 2 + keyLenght);
        cache.put(key, value);
      }
      if(command == 134) {
        byte keyLenght = data[1];
        String key = new String(data, 2, keyLenght);
        Integer value = DataOperations.getUnsignedByte(data[2 + keyLenght]);
        System.out.println("key: " + key);
        System.out.println("value: " + value);
        cache.put(key, value);
      }
      if(command == 135) {
        byte keyLenght = data[1];
        String key = new String(data, 2, keyLenght);
        byte val = data[2 + keyLenght];
        cache.put(key, val == 1);
      }
      if(command == 254) {
        int bar = DataOperations.getUnsigned4Bytes(data, 1);
        if(bar == -1) {
          smithingscreen.isVisible = false;
        } else {
          SmithingScreen.changeItems(smithingscreen, bar);
          smithingscreen.isVisible = true;
        }
      }
      if (command == 110) {
        int i = 1;
        serverStartTime = DataOperations.getUnsigned8Bytes(data, i);
        i += 8;
        serverLocation = new String(data, i, length - i);
        return;
      }
      if (command == 145) {
        if (!hasWorldInfo) {
          return;
        }
        loading = true;
        lastPlayerCount = playerCount;
        for (int k = 0; k < lastPlayerCount; k++)
          lastPlayerArray[k] = playerArray[k];

        int currentOffset = 8;
        setSectionX(DataOperations.getIntFromByteArray(data,
                    currentOffset, 11));
        currentOffset += 11;
        setSectionY(DataOperations.getIntFromByteArray(data,
                    currentOffset, 13));
        currentOffset += 13;
        int mobSprite = DataOperations.getIntFromByteArray(data,
                        currentOffset, 4);
        currentOffset += 4;
        boolean sectionLoaded = loadSection(getSectionX(), getSectionY());
        setSectionX(getSectionX() - getAreaX());
        setSectionY(getSectionY() - getAreaY());
        int mapEnterX = getSectionX() * magicLoc + 64;
        int mapEnterY = getSectionY() * magicLoc + 64;
        if (sectionLoaded) {
          ourPlayer.waypointCurrent = 0;
          ourPlayer.waypointEndSprite = 0;
          ourPlayer.currentX = ourPlayer.waypointsX[0] = mapEnterX;
          ourPlayer.currentY = ourPlayer.waypointsY[0] = mapEnterY;
        }
        playerCount = 0;
        ourPlayer = makePlayer(serverIndex, mapEnterX, mapEnterY,
                               mobSprite);
        int newPlayerCount = DataOperations.getIntFromByteArray(data,
                             currentOffset, 8);
        currentOffset += 8;
        for (int currentNewPlayer = 0; currentNewPlayer < newPlayerCount; currentNewPlayer++) {
          Mob lastMob = getLastPlayer(DataOperations
                                      .getIntFromByteArray(data, currentOffset, 16));
          currentOffset += 16;
          int nextPlayer = DataOperations.getIntFromByteArray(data,
                           currentOffset, 1); // 1
          currentOffset++;
          if (nextPlayer != 0) {
            int waypointsLeft = DataOperations.getIntFromByteArray(
                                  data, currentOffset, 1); // 2
            currentOffset++;
            if (waypointsLeft == 0) {
              int currentNextSprite = DataOperations
                                      .getIntFromByteArray(data, currentOffset, 3); // 3
              currentOffset += 3;
              int currentWaypoint = lastMob.waypointCurrent;
              int newWaypointX = lastMob.waypointsX[currentWaypoint];
              int newWaypointY = lastMob.waypointsY[currentWaypoint];
              if (currentNextSprite == 2
                  || currentNextSprite == 1
                  || currentNextSprite == 3)
                newWaypointX += magicLoc;
              if (currentNextSprite == 6
                  || currentNextSprite == 5
                  || currentNextSprite == 7)
                newWaypointX -= magicLoc;
              if (currentNextSprite == 4
                  || currentNextSprite == 3
                  || currentNextSprite == 5)
                newWaypointY += magicLoc;
              if (currentNextSprite == 0
                  || currentNextSprite == 1
                  || currentNextSprite == 7)
                newWaypointY -= magicLoc;
              lastMob.nextSprite = currentNextSprite;
              lastMob.waypointCurrent = currentWaypoint = (currentWaypoint + 1) % 10;
              lastMob.waypointsX[currentWaypoint] = newWaypointX;
              lastMob.waypointsY[currentWaypoint] = newWaypointY;
            } else {
              int needsNextSprite = DataOperations
                                    .getIntFromByteArray(data, currentOffset, 4);
              currentOffset += 4;
              if ((needsNextSprite & 0xc) == 12) {
                continue;
              }
              lastMob.nextSprite = needsNextSprite;
            }
          }
          playerArray[playerCount++] = lastMob;
        }

        int mobCount = 0;
        while (currentOffset + 24 < length * 8) {
          int mobIndex = DataOperations.getIntFromByteArray(data,
                         currentOffset, 16);
          currentOffset += 16;
          int areaMobX = DataOperations.getIntFromByteArray(data,
                         currentOffset, 5);
          currentOffset += 5;
          if (areaMobX > 15)
            areaMobX -= 32;
          int areaMobY = DataOperations.getIntFromByteArray(data,
                         currentOffset, 5);
          currentOffset += 5;
          if (areaMobY > 15)
            areaMobY -= 32;
          int mobArrayMobID = DataOperations.getIntFromByteArray(
                                data, currentOffset, 4);
          currentOffset += 4;
          int addIndex = DataOperations.getIntFromByteArray(data,
                         currentOffset, 1);
          currentOffset++;
          int mobX = (getSectionX() + areaMobX) * magicLoc + 64;
          int mobY = (getSectionY() + areaMobY) * magicLoc + 64;
          makePlayer(mobIndex, mobX, mobY, mobArrayMobID);
          if (addIndex == 0)
            mobArrayIndexes[mobCount++] = mobIndex;
        }
        if (mobCount > 0) {
          super.streamClass.createPacket(83);
          super.streamClass.add2ByteInt(mobCount);
          for (int currentMob = 0; currentMob < mobCount; currentMob++) {
            Mob dummyMob = mobArray[mobArrayIndexes[currentMob]];
            super.streamClass.add2ByteInt(dummyMob.serverIndex);
            super.streamClass.add2ByteInt(dummyMob.mobIntUnknown);
          }

          super.streamClass.formatPacket();
          mobCount = 0;
        }
        loading = false;
        return;
      }
      if (command == 109) {
        if (needsClear) {
          for (int i = 0; i < groundItemType.length; i++) {
            groundItemType[i] = -1;
            groundItemX[i] = -1;
            groundItemY[i] = -1;
          }
          needsClear = false;
        }
        for (int l = 1; l < length;)
          if (DataOperations.getUnsignedByte(data[l]) == 255) { // ???
            int newCount = 0;
            int newSectionX = getSectionX() + data[l + 1] >> 3;
            int newSectionY = getSectionY() + data[l + 2] >> 3;
            l += 3;
            for (int groundItem = 0; groundItem < groundItemCount; groundItem++) {
              int newX = (groundItemX[groundItem] >> 3)
                         - newSectionX;
              int newY = (groundItemY[groundItem] >> 3)
                         - newSectionY;
              if (newX != 0 || newY != 0) {
                if (groundItem != newCount) {
                  groundItemX[newCount] = groundItemX[groundItem];
                  groundItemY[newCount] = groundItemY[groundItem];
                  groundItemType[newCount] = groundItemType[groundItem];
                  groundItemObjectVar[newCount] = groundItemObjectVar[groundItem];
                }
                newCount++;
              }
            }

            groundItemCount = newCount;
          } else {
            int i8 = DataOperations.getUnsigned2Bytes(data, l);
            l += 2;
            int k14 = getSectionX() + data[l++];
            int j19 = getSectionY() + data[l++];
            if ((i8 & 0x8000) == 0) { // New Item
              groundItemX[groundItemCount] = k14;
              groundItemY[groundItemCount] = j19;
              groundItemType[groundItemCount] = i8;
              groundItemObjectVar[groundItemCount] = 0;
              for (int k23 = 0; k23 < objectCount; k23++) {
                if (objectX[k23] != k14 || objectY[k23] != j19)
                  continue;
                groundItemObjectVar[groundItemCount] = EntityHandler
                                                       .getObjectDef(objectType[k23])
                                                       .getGroundItemVar();
                break;
              }

              groundItemCount++;
            } else { // Known Item
              i8 &= 0x7fff;
              int l23 = 0;
              for (int k26 = 0; k26 < groundItemCount; k26++) {
                if (groundItemX[k26] != k14
                    || groundItemY[k26] != j19
                    || groundItemType[k26] != i8) { // Keep
                  // how
                  // it is
                  if (k26 != l23) {
                    groundItemX[l23] = groundItemX[k26];
                    groundItemY[l23] = groundItemY[k26];
                    groundItemType[l23] = groundItemType[k26];
                    groundItemObjectVar[l23] = groundItemObjectVar[k26];
                  }
                  l23++;
                } else { // Remove
                  i8 = -123;
                }
              }

              groundItemCount = l23;
            }
          }

        return;
      }
      if (command == 27) {
        for (int i1 = 1; i1 < length;)
          if (DataOperations.getUnsignedByte(data[i1]) == 255) {
            int j8 = 0;
            int l14 = getSectionX() + data[i1 + 1] >> 3;
            int k19 = getSectionY() + data[i1 + 2] >> 3;
            i1 += 3;
            for (int i24 = 0; i24 < objectCount; i24++) {
              int l26 = (objectX[i24] >> 3) - l14;
              int k29 = (objectY[i24] >> 3) - k19;
              if (l26 != 0 || k29 != 0) {
                if (i24 != j8) {
                  objectModelArray[j8] = objectModelArray[i24];
                  objectModelArray[j8].anInt257 = j8;
                  objectX[j8] = objectX[i24];
                  objectY[j8] = objectY[i24];
                  objectType[j8] = objectType[i24];
                  objectID[j8] = objectID[i24];
                }
                j8++;
              } else {
                gameCamera.removeModel(objectModelArray[i24]);
                engineHandle.updateObject(objectX[i24],
                                          objectY[i24], objectType[i24],
                                          objectID[i24]);
              }
            }

            objectCount = j8;
          } else {
            int k8 = DataOperations.getUnsigned2Bytes(data, i1);
            i1 += 2;
            int i15 = getSectionX() + data[i1++];
            int l19 = getSectionY() + data[i1++];
            int l29 = data[i1++];
            int j24 = 0;
            for (int i27 = 0; i27 < objectCount; i27++)
              if (objectX[i27] != i15 || objectY[i27] != l19
                  || objectID[i27] != l29) {
                if (i27 != j24) {
                  objectModelArray[j24] = objectModelArray[i27];
                  objectModelArray[j24].anInt257 = j24;
                  objectX[j24] = objectX[i27];
                  objectY[j24] = objectY[i27];
                  objectType[j24] = objectType[i27];
                  objectID[j24] = objectID[i27];
                }
                j24++;
              } else {
                gameCamera.removeModel(objectModelArray[i27]);
                engineHandle.updateObject(objectX[i27],
                                          objectY[i27], objectType[i27],
                                          objectID[i27]);
              }

            objectCount = j24;
            if (k8 != 60000) {
              engineHandle.registerObjectDir(i15, l19, l29);
              int i34;
              int j37;
              if (l29 == 0 || l29 == 4) {
                i34 = EntityHandler.getObjectDef(k8).getWidth();
                j37 = EntityHandler.getObjectDef(k8)
                      .getHeight();
              } else {
                j37 = EntityHandler.getObjectDef(k8).getWidth();
                i34 = EntityHandler.getObjectDef(k8)
                      .getHeight();
              }
              int j40 = ((i15 + i15 + i34) * magicLoc) / 2;
              int i42 = ((l19 + l19 + j37) * magicLoc) / 2;
              int k43 = EntityHandler.getObjectDef(k8).modelID;
              Model model_1 = gameDataModels[k43].method203();
              gameCamera.addModel(model_1);
              model_1.anInt257 = objectCount;
              model_1.method188(0, l29 * 32, 0);
              model_1.method190(j40, -engineHandle
                                .getAveragedElevation(j40, i42), i42);
              model_1.method184(true, 48, 48, -50, -10, -50);
              engineHandle.method412(i15, l19, k8, l29);
              if (k8 == 74)
                model_1.method190(0, -480, 0);
              objectX[objectCount] = i15;
              objectY[objectCount] = l19;
              objectType[objectCount] = k8;
              objectID[objectCount] = l29;
              objectModelArray[objectCount++] = model_1;
            }
          }

        return;
      }//command == 48
      if (command == 114) {
        int invOffset = 1;
        inventoryCount = data[invOffset++] & 0xff;
        for (int invItem = 0; invItem < inventoryCount; invItem++) {
          int j15 = DataOperations.getUnsigned2Bytes(data, invOffset);
          invOffset += 2;
          getInventoryItems()[invItem] = (j15 & 0x7fff);
          wearing[invItem] = j15 / 32768;
          if (EntityHandler.getItemDef(j15 & 0x7fff).isStackable()) {
            inventoryItemsCount[invItem] = DataOperations.readInt(
                                             data, invOffset);
            invOffset += 4;
          } else {
            inventoryItemsCount[invItem] = 1;
          }
        }

        return;
      }
      if (command == 53) {
        int mobCount = DataOperations.getUnsigned2Bytes(data, 1);
        int mobUpdateOffset = 3;
        for (int currentMob = 0; currentMob < mobCount; currentMob++) {
          int mobArrayIndex = DataOperations.getUnsigned2Bytes(data,
                              mobUpdateOffset);
          mobUpdateOffset += 2;
          if (mobArrayIndex < 0 || mobArrayIndex > mobArray.length) {
            return;
          }
          Mob mob = mobArray[mobArrayIndex];
          if (mob == null) {
            return;
          }
          byte mobUpdateType = data[mobUpdateOffset++];
          if (mobUpdateType == 0) {
            int i30 = DataOperations.getUnsigned2Bytes(data,
                      mobUpdateOffset);
            mobUpdateOffset += 2;
            if (mob != null) {
              mob.anInt163 = 150;
              mob.anInt162 = i30;
            }
          } else if (mobUpdateType == 1) { // Player talking
            byte byte7 = data[mobUpdateOffset++];
            if (mob != null) {
              String s2 = DataConversions.byteToString(data,
                          mobUpdateOffset, byte7);
              mob.lastMessageTimeout = 150;
              mob.lastMessage = s2;
              displayMessage(mob.name + ": " + mob.lastMessage,
                             2, mob.admin);
            }
            mobUpdateOffset += byte7;
          } else if (mobUpdateType == 2) { // Someone getting hit.
            int j30 = DataOperations
                      .getUnsignedByte(data[mobUpdateOffset++]);
            int hits = DataOperations
                       .getUnsignedByte(data[mobUpdateOffset++]);
            int hitsBase = DataOperations
                           .getUnsignedByte(data[mobUpdateOffset++]);
            if (mob != null) {
              mob.anInt164 = j30;
              mob.hitPointsCurrent = hits;
              mob.hitPointsBase = hitsBase;
              mob.combatTimer = 200;
              if (mob == ourPlayer) {
                playerStatCurrent[3] = hits;
                playerStatBase[3] = hitsBase;
                showWelcomeBox = false;
                // showServerMessageBox = false;
              }
            }
          } else if (mobUpdateType == 3) { // Projectile an npc..
            int k30 = DataOperations.getUnsigned2Bytes(data,
                      mobUpdateOffset);
            mobUpdateOffset += 2;
            int k34 = DataOperations.getUnsigned2Bytes(data,
                      mobUpdateOffset);
            mobUpdateOffset += 2;
            if (mob != null) {
              mob.attackingCameraInt = k30;
              mob.attackingNpcIndex = k34;
              mob.attackingMobIndex = -1;
              mob.anInt176 = attackingInt40;
            }
          } else if (mobUpdateType == 4) { // Projectile another
            // player.
            int l30 = DataOperations.getUnsigned2Bytes(data,
                      mobUpdateOffset);
            mobUpdateOffset += 2;
            int l34 = DataOperations.getUnsigned2Bytes(data,
                      mobUpdateOffset);
            mobUpdateOffset += 2;
            if (mob != null) {
              mob.attackingCameraInt = l30;
              mob.attackingMobIndex = l34;
              mob.attackingNpcIndex = -1;
              mob.anInt176 = attackingInt40;
            }
          } else if (mobUpdateType == 5) { // Apperance update
            if (mob != null) {
              mob.mobIntUnknown = DataOperations
                                  .getUnsigned2Bytes(data, mobUpdateOffset);
              mobUpdateOffset += 2;
              mob.nameLong = DataOperations.getUnsigned8Bytes(
                               data, mobUpdateOffset);
              mobUpdateOffset += 8;
              mob.name = DataOperations
                         .longToString(mob.nameLong);
              int i31 = DataOperations
                        .getUnsignedByte(data[mobUpdateOffset]);
              mobUpdateOffset++;
              for (int i35 = 0; i35 < i31; i35++) {
                mob.animationCount[i35] = DataOperations
                                          .getUnsignedByte(data[mobUpdateOffset]);
                mobUpdateOffset++;
              }

              for (int l37 = i31; l37 < 12; l37++)
                mob.animationCount[l37] = 0;

              mob.colourHairType = data[mobUpdateOffset++] & 0xff;
              mob.colourTopType = data[mobUpdateOffset++] & 0xff;
              mob.colourBottomType = data[mobUpdateOffset++] & 0xff;
              mob.colourSkinType = data[mobUpdateOffset++] & 0xff;
              mob.level = data[mobUpdateOffset++] & 0xff;
              mob.anInt179 = data[mobUpdateOffset++] & 0xff;
              mob.admin = data[mobUpdateOffset++] & 0xff;
            } else {
              mobUpdateOffset += 14;
              int j31 = DataOperations
                        .getUnsignedByte(data[mobUpdateOffset]);
              mobUpdateOffset += j31 + 1;
            }
          } else if (mobUpdateType == 6) { // private player talking
            byte byte8 = data[mobUpdateOffset];
            mobUpdateOffset++;
            if (mob != null) {
              String s3 = DataConversions.byteToString(data,
                          mobUpdateOffset, byte8);
              mob.lastMessageTimeout = 150;
              mob.lastMessage = s3;
              if (mob == ourPlayer)
                displayMessage(mob.name + ": "
                               + mob.lastMessage, 5, mob.admin);
            }
            mobUpdateOffset += byte8;
          }
        }

        return;
      }
      if (command == 129) {
        combatStyle = DataOperations.getUnsignedByte(data[1]);
        return;
      }
      if (command == 95) {
        for (int l1 = 1; l1 < length;)
          if (DataOperations.getUnsignedByte(data[l1]) == 255) {
            int j9 = 0;
            int l15 = getSectionX() + data[l1 + 1] >> 3;
            int j20 = getSectionY() + data[l1 + 2] >> 3;
            l1 += 3;
            for (int currentDoor = 0; currentDoor < doorCount; currentDoor++) {
              int j27 = (doorX[currentDoor] >> 3) - l15;
              int k31 = (doorY[currentDoor] >> 3) - j20;
              if (j27 != 0 || k31 != 0) {
                if (currentDoor != j9) {
                  doorModel[j9] = doorModel[currentDoor];
                  doorModel[j9].anInt257 = j9 + 10000;
                  doorX[j9] = doorX[currentDoor];
                  doorY[j9] = doorY[currentDoor];
                  doorDirection[j9] = doorDirection[currentDoor];
                  doorType[j9] = doorType[currentDoor];
                }
                j9++;
              } else {
                gameCamera.removeModel(doorModel[currentDoor]);
                engineHandle.updateDoor(doorX[currentDoor],
                                        doorY[currentDoor],
                                        doorDirection[currentDoor],
                                        doorType[currentDoor]);
              }
            }

            doorCount = j9;
          } else {
            int k9 = DataOperations.getUnsigned2Bytes(data, l1);
            l1 += 2;
            int i16 = getSectionX() + data[l1++];
            int k20 = getSectionY() + data[l1++];
            byte byte5 = data[l1++];
            int k27 = 0;
            for (int l31 = 0; l31 < doorCount; l31++)
              if (doorX[l31] != i16 || doorY[l31] != k20
                  || doorDirection[l31] != byte5) {
                if (l31 != k27) {
                  doorModel[k27] = doorModel[l31];
                  doorModel[k27].anInt257 = k27 + 10000;
                  doorX[k27] = doorX[l31];
                  doorY[k27] = doorY[l31];
                  doorDirection[k27] = doorDirection[l31];
                  doorType[k27] = doorType[l31];
                }
                k27++;
              } else {
                gameCamera.removeModel(doorModel[l31]);
                engineHandle.updateDoor(doorX[l31], doorY[l31],
                                        doorDirection[l31], doorType[l31]);
              }

            doorCount = k27;
            if (k9 != 60000) { // 65535) {
              engineHandle.method408(i16, k20, byte5, k9);
              Model model = makeModel(i16, k20, byte5, k9,
                                      doorCount);
              doorModel[doorCount] = model;
              doorX[doorCount] = i16;
              doorY[doorCount] = k20;
              doorType[doorCount] = k9;
              doorDirection[doorCount++] = byte5;
            }
          }

        return;
      }
      if (command == 77) {
        lastNpcCount = npcCount;
        npcCount = 0;
        for (int lastNpcIndex = 0; lastNpcIndex < lastNpcCount; lastNpcIndex++)
          lastNpcArray[lastNpcIndex] = npcArray[lastNpcIndex];

        int newNpcOffset = 8;
        int newNpcCount = DataOperations.getIntFromByteArray(data,
                          newNpcOffset, 8);

        newNpcOffset += 8;
        for (int newNpcIndex = 0; newNpcIndex < newNpcCount; newNpcIndex++) {
          Mob newNPC = getLastNpc(DataOperations.getIntFromByteArray(
                                    data, newNpcOffset, 16));
          newNpcOffset += 16;
          int npcNeedsUpdate = DataOperations.getIntFromByteArray(
                                 data, newNpcOffset, 1);
          newNpcOffset++;
          if (npcNeedsUpdate != 0) {
            int i32 = DataOperations.getIntFromByteArray(data,
                      newNpcOffset, 1);
            newNpcOffset++;
            if (i32 == 0) {
              int nextSprite = DataOperations
                               .getIntFromByteArray(data, newNpcOffset, 3);
              newNpcOffset += 3;
              int waypointCurrent = newNPC.waypointCurrent;
              int waypointX = newNPC.waypointsX[waypointCurrent];
              int waypointY = newNPC.waypointsY[waypointCurrent];
              if (nextSprite == 2 || nextSprite == 1
                  || nextSprite == 3)
                waypointX += magicLoc;
              if (nextSprite == 6 || nextSprite == 5
                  || nextSprite == 7)
                waypointX -= magicLoc;
              if (nextSprite == 4 || nextSprite == 3
                  || nextSprite == 5)
                waypointY += magicLoc;
              if (nextSprite == 0 || nextSprite == 1
                  || nextSprite == 7)
                waypointY -= magicLoc;
              newNPC.nextSprite = nextSprite;
              newNPC.waypointCurrent = waypointCurrent = (waypointCurrent + 1) % 10;
              newNPC.waypointsX[waypointCurrent] = waypointX;
              newNPC.waypointsY[waypointCurrent] = waypointY;
            } else {
              int nextSpriteOffset = DataOperations
                                     .getIntFromByteArray(data, newNpcOffset, 4);
              newNpcOffset += 4;
              if ((nextSpriteOffset & 0xc) == 12) {
                continue;
              }
              newNPC.nextSprite = nextSpriteOffset;

            }
          }
          npcArray[npcCount++] = newNPC;
        }

        while (newNpcOffset + 34 < length * 8) {
          int serverIndex = DataOperations.getIntFromByteArray(data,
                            newNpcOffset, 16);
          newNpcOffset += 16;
          int i28 = DataOperations.getIntFromByteArray(data,
                    newNpcOffset, 5);
          newNpcOffset += 5;
          if (i28 > 15)
            i28 -= 32;
          int j32 = DataOperations.getIntFromByteArray(data,
                    newNpcOffset, 5);
          newNpcOffset += 5;
          if (j32 > 15)
            j32 -= 32;
          int nextSprite = DataOperations.getIntFromByteArray(data,
                           newNpcOffset, 4);
          newNpcOffset += 4;
          int x = (getSectionX() + i28) * magicLoc + 64;
          int y = (getSectionY() + j32) * magicLoc + 64;
          int type = DataOperations.getIntFromByteArray(data,
                     newNpcOffset, 10);
          newNpcOffset += 10;
          if (type >= EntityHandler.npcCount())
            type = 24;
          addNPC(serverIndex, x, y, nextSprite, type);
        }
        return;
      }
      if (command == 190) {
        int j2 = DataOperations.getUnsigned2Bytes(data, 1);
        int i10 = 3;
        for (int k16 = 0; k16 < j2; k16++) {
          int i21 = DataOperations.getUnsigned2Bytes(data, i10);
          i10 += 2;
          Mob mob_2 = npcRecordArray[i21];
          int j28 = DataOperations.getUnsignedByte(data[i10]);
          i10++;
          if (j28 == 1) {
            int k32 = DataOperations.getUnsigned2Bytes(data, i10);
            i10 += 2;
            byte byte9 = data[i10];
            i10++;
            if (mob_2 != null) {
              String s4 = DataConversions.byteToString(data, i10,
                          byte9);
              mob_2.lastMessageTimeout = 150;
              mob_2.lastMessage = s4;
              if (k32 == ourPlayer.serverIndex)
                displayMessage("@yel@"
                               + EntityHandler.getNpcDef(mob_2.type)
                               .getName() + ": "
                               + mob_2.lastMessage, 5, 0);
            }
            i10 += byte9;
          } else if (j28 == 2) {
            int l32 = DataOperations.getUnsignedByte(data[i10]);
            i10++;
            int i36 = DataOperations.getUnsignedByte(data[i10]);
            i10++;
            int k38 = DataOperations.getUnsignedByte(data[i10]);
            i10++;
            if (mob_2 != null) {
              mob_2.anInt164 = l32;
              mob_2.hitPointsCurrent = i36;
              mob_2.hitPointsBase = k38;
              mob_2.combatTimer = 200;
            }
          }
        }

        return;
      }
      if (command == 223) {

        int newQuestionMenuCount = DataOperations
                                   .getUnsignedByte(data[1]);
        int newQuestionMenuOffset = 2;
        for (int l16 = 0; l16 < newQuestionMenuCount; l16++) {
          int newQuestionMenuQuestionLength = DataOperations
                                              .getUnsignedByte(data[newQuestionMenuOffset]);
          newQuestionMenuOffset++;
          questionMenuAnswer[l16] = new String(data,
                                               newQuestionMenuOffset,
                                               newQuestionMenuQuestionLength);
          newQuestionMenuOffset += newQuestionMenuQuestionLength;
        }

        questionMenu.parseQuestions(questionMenuAnswer, newQuestionMenuCount);


        return;
      }
      if (command == 233) {
        int questCount = DataOperations .getUnsignedByte(data[1]);

        int questOffset = 2;
        for (int i = 0; i < questCount; i++) {
          int questId =  DataOperations.readInt(data, questOffset);
          questOffset += 4;
          int questStagee =  DataOperations.readInt(data, questOffset);
          questOffset += 4;
          int questNameLength = DataOperations.getUnsignedByte(data[questOffset++]);
          String questName = new String(data, questOffset, questNameLength);
          newQuestNames.put(questId, questName);
          questStage.put(questId, questStagee);
          questOffset += questNameLength;
        }
        return;
      }
      if (command == 234) {
        int questID =  DataOperations.readInt(data, 1);
        int stage = DataOperations.readInt(data, 5);
        System.out.println("QuestID: " + questID + " stage: " + stage);
        questStage.put(questID, stage);
        return;
      }
      if (command == 127) {
        questionMenu.close();
        return;
      }
      if (command == 131) {
        needsClear = true;
        notInWilderness = true;
        hasWorldInfo = true;
        serverIndex = DataOperations.getUnsigned2Bytes(data, 1);
        wildX = DataOperations.getUnsigned2Bytes(data, 3);
        wildY = DataOperations.getUnsigned2Bytes(data, 5);
        wildYSubtract = DataOperations.getUnsigned2Bytes(data, 7);
        wildYMultiplier = DataOperations.getUnsigned2Bytes(data, 9);
        wildY -= wildYSubtract * wildYMultiplier;
        return;
      }
      if (command == 180) {
        int l2 = 1;
        for (int k10 = 0; k10 < 18; k10++) {
          playerStatCurrent[k10] = DataOperations
                                   .getUnsignedByte(data[l2++]);
        }
        for (int i17 = 0; i17 < 18; i17++) {
          playerStatBase[i17] = DataOperations
                                .getUnsignedByte(data[l2++]);
        }
        for (int k21 = 0; k21 < 18; k21++) {
          playerStatExperience[k21] = DataOperations
                                      .readInt(data, l2);
          l2 += 4;
        }
        expGained = 0;
        return;
      }
      if (command == 177) {
        int i3 = 1;
        for (int x = 0; x < 6; x++) {
          equipmentStatus[x] = DataOperations.getSigned2Bytes(data,
                               i3);
          i3 += 2;
        }
        return;
      }
      if (command == 165) {
        playerAliveTimeout = 250;
        return;
      }
      if (command == 115) {
        int thingLength = (length - 1) / 4;
        for (int currentThing = 0; currentThing < thingLength; currentThing++) {
          int currentItemSectionX = getSectionX()
                                    + DataOperations.getSigned2Bytes(data,
                                        1 + currentThing * 4) >> 3;
          int currentItemSectionY = getSectionY()
                                    + DataOperations.getSigned2Bytes(data,
                                        3 + currentThing * 4) >> 3;
          int currentCount = 0;
          for (int currentItem = 0; currentItem < groundItemCount; currentItem++) {
            int currentItemOffsetX = (groundItemX[currentItem] >> 3)
                                     - currentItemSectionX;
            int currentItemOffsetY = (groundItemY[currentItem] >> 3)
                                     - currentItemSectionY;
            if (currentItemOffsetX != 0 || currentItemOffsetY != 0) {
              if (currentItem != currentCount) {
                groundItemX[currentCount] = groundItemX[currentItem];
                groundItemY[currentCount] = groundItemY[currentItem];
                groundItemType[currentCount] = groundItemType[currentItem];
                groundItemObjectVar[currentCount] = groundItemObjectVar[currentItem];
              }
              currentCount++;
            }
          }

          groundItemCount = currentCount;
          currentCount = 0;
          for (int j33 = 0; j33 < objectCount; j33++) {
            int k36 = (objectX[j33] >> 3) - currentItemSectionX;
            int l38 = (objectY[j33] >> 3) - currentItemSectionY;
            if (k36 != 0 || l38 != 0) {
              if (j33 != currentCount) {
                objectModelArray[currentCount] = objectModelArray[j33];
                objectModelArray[currentCount].anInt257 = currentCount;
                objectX[currentCount] = objectX[j33];
                objectY[currentCount] = objectY[j33];
                objectType[currentCount] = objectType[j33];
                objectID[currentCount] = objectID[j33];
              }
              currentCount++;
            } else {
              gameCamera.removeModel(objectModelArray[j33]);
              engineHandle.updateObject(objectX[j33],
                                        objectY[j33], objectType[j33],
                                        objectID[j33]);
            }
          }

          objectCount = currentCount;
          currentCount = 0;
          for (int l36 = 0; l36 < doorCount; l36++) {
            int i39 = (doorX[l36] >> 3) - currentItemSectionX;
            int j41 = (doorY[l36] >> 3) - currentItemSectionY;
            if (i39 != 0 || j41 != 0) {
              if (l36 != currentCount) {
                doorModel[currentCount] = doorModel[l36];
                doorModel[currentCount].anInt257 = currentCount + 10000;
                doorX[currentCount] = doorX[l36];
                doorY[currentCount] = doorY[l36];
                doorDirection[currentCount] = doorDirection[l36];
                doorType[currentCount] = doorType[l36];
              }
              currentCount++;
            } else {
              gameCamera.removeModel(doorModel[l36]);
              engineHandle.updateDoor(doorX[l36], doorY[l36],
                                      doorDirection[l36], doorType[l36]);
            }
          }

          doorCount = currentCount;
        }

        return;
      }
      if (command == 207) {
        showCharacterLookScreen = true;
        return;
      }
      if (command == 4) {
        int currentMob = DataOperations.getUnsigned2Bytes(data, 1);
        if (mobArray[currentMob] != null) // todo: check what that
          // mobArray is
          tradeOtherPlayerName = mobArray[currentMob].name;
        showTradeWindow = true;
        tradeOtherAccepted = false;
        tradeWeAccepted = false;
        tradeMyItemCount = 0;
        tradeOtherItemCount = 0;
        return;
      }
      if (command == 187) {
        showTradeWindow = false;
        showTradeConfirmWindow = false;
        return;
      }
      if (command == 250) {
        tradeOtherItemCount = data[1] & 0xff;
        int l3 = 2;
        for (int i11 = 0; i11 < tradeOtherItemCount; i11++) {
          tradeOtherItems[i11] = DataOperations.getUnsigned2Bytes(
                                   data, l3);
          l3 += 2;
          tradeOtherItemsCount[i11] = DataOperations
                                      .readInt(data, l3);
          l3 += 4;
        }

        tradeOtherAccepted = false;
        tradeWeAccepted = false;
        return;
      }
      if (command == 92) {
        tradeOtherAccepted = data[1] == 1;
      }
      if (command == 253) {
        showShop = true;
        int i4 = 1;
        int j11 = data[i4++] & 0xff;
        byte byte4 = data[i4++];
        int temp = data[i4++] & 0xff;
        temp = data[i4++] & 0xff;
        for (int i22 = 0; i22 < 40; i22++)
          shopItems[i22] = -1;

        for (int j25 = 0; j25 < j11; j25++) {
          shopItems[j25] = DataOperations.getUnsigned2Bytes(data, i4);
          i4 += 2;
          shopItemCount[j25] = DataOperations.getUnsigned2Bytes(data,
                               i4);
          i4 += 2;
          shopItemsBuyPrice[j25] = DataOperations.getUnsigned4Bytes(
                                     data, i4);
          i4 += 4;
          shopItemsSellPrice[j25] = DataOperations.getUnsigned4Bytes(
                                      data, i4);
          i4 += 4;
        }

        if (byte4 == 1) {
          int l28 = 39;
          for (int k33 = 0; k33 < inventoryCount; k33++) {
            if (l28 < j11)
              break;
            boolean flag2 = false;
            for (int j39 = 0; j39 < 40; j39++) {
              if (shopItems[j39] != getInventoryItems()[k33])
                continue;
              flag2 = true;
              break;
            }

            if (getInventoryItems()[k33] == 10)
              flag2 = true;
            if (!flag2) {
              // our own item that's not in stock.

              shopItems[l28] = getInventoryItems()[k33] & 0x7fff;
              shopItemsSellPrice[l28] = EntityHandler
                                        .getItemDef(shopItems[l28]).basePrice
                                        - (int) (EntityHandler
                                                 .getItemDef(shopItems[l28]).basePrice / 2.5);
              shopItemsSellPrice[l28] = shopItemsSellPrice[l28]
                                        - (int) (shopItemsSellPrice[l28] * 0.10);
              shopItemCount[l28] = 0;
              l28--;
            }
          }

        }
        if (selectedShopItemIndex >= 0
            && selectedShopItemIndex < 40
            && shopItems[selectedShopItemIndex] != selectedShopItemType) {
          selectedShopItemIndex = -1;
          selectedShopItemType = -2;
        }
        return;
      }
      if (command == 220) {
        showShop = false;
        return;
      }
      if (command == 18) {
        tradeWeAccepted = data[1] == 1;
      }
      if (command == 152) {
        configAutoCameraAngle = DataOperations.getUnsignedByte(data[1]) == 1;
        configMouseButtons = DataOperations.getUnsignedByte(data[2]) == 1;
        configSoundEffects = DataOperations.getUnsignedByte(data[3]) == 1;
        showRoof = DataOperations.getUnsignedByte(data[4]) == 1;
        autoScreenshot = DataOperations.getUnsignedByte(data[5]) == 1;
        combatWindow = DataOperations.getUnsignedByte(data[6]) == 1;
        return;
      }
      if (command == 209) {
        for (int currentPrayer = 0; currentPrayer < length - 1; currentPrayer++) {
          boolean prayerOff = data[currentPrayer + 1] == 1;
          if (!prayerOn[currentPrayer] && prayerOff)
            playSound("prayeron");
          if (prayerOn[currentPrayer] && !prayerOff)
            playSound("prayeroff");
          prayerOn[currentPrayer] = prayerOff;
        }

        return;
      }
      if (command == 93) {
        showBank = true;
        int l4 = 1;
        newBankItemCount = data[l4++] & 0xff;
        bankItemsMax = data[l4++] & 0xff;
        for (int k11 = 0; k11 < newBankItemCount; k11++) {
          newBankItems[k11] = DataOperations.getUnsigned2Bytes(data,
                              l4);
          l4 += 2;
          newBankItemsCount[k11] = DataOperations.getUnsigned4Bytes(
                                     data, l4);
          l4 += 4;
        }

        updateBankItems();
        return;
      }
      if (command == 171) {
        showBank = false;
        return;
      }
      if (command == 211) {
        int idx = data[1] & 0xFF;
        int oldExp = playerStatExperience[idx];
        playerStatExperience[idx] = DataOperations.readInt(data, 2);
        if (playerStatExperience[idx] > oldExp) {
          expGained += (playerStatExperience[idx] - oldExp);
        }
        return;
      }
      if (command == 229) {
        int j5 = DataOperations.getUnsigned2Bytes(data, 1);
        if (mobArray[j5] != null) {
          duelOpponentName = mobArray[j5].name;
        }
        showDuelWindow = true;
        duelMyItemCount = 0;
        duelOpponentItemCount = 0;
        duelOpponentAccepted = false;
        duelMyAccepted = false;
        duelNoRetreating = false;
        duelNoMagic = false;
        duelNoPrayer = false;
        duelNoWeapons = false;
        return;
      }
      if (command == 160) {
        showDuelWindow = false;
        showDuelConfirmWindow = false;
        return;
      }
      if (command == 251) {
        showTradeConfirmWindow = true;
        tradeConfirmAccepted = false;
        showTradeWindow = false;
        int k5 = 1;
        tradeConfirmOtherNameLong = DataOperations.getUnsigned8Bytes(
                                      data, k5);
        k5 += 8;
        tradeConfirmOtherItemCount = data[k5++] & 0xff;
        for (int l11 = 0; l11 < tradeConfirmOtherItemCount; l11++) {
          tradeConfirmOtherItems[l11] = DataOperations
                                        .getUnsigned2Bytes(data, k5);
          k5 += 2;
          tradeConfirmOtherItemsCount[l11] = DataOperations.readInt(
                                               data, k5);
          k5 += 4;
        }

        tradeConfirmItemCount = data[k5++] & 0xff;
        for (int k17 = 0; k17 < tradeConfirmItemCount; k17++) {
          tradeConfirmItems[k17] = DataOperations.getUnsigned2Bytes(
                                     data, k5);
          k5 += 2;
          tradeConfirmItemsCount[k17] = DataOperations.readInt(data,
                                        k5);
          k5 += 4;
        }

        return;
      }
      if (command == 63) {
        duelOpponentItemCount = data[1] & 0xff;
        int l5 = 2;
        for (int i12 = 0; i12 < duelOpponentItemCount; i12++) {
          duelOpponentItems[i12] = DataOperations.getUnsigned2Bytes(
                                     data, l5);
          l5 += 2;
          duelOpponentItemsCount[i12] = DataOperations.readInt(data,
                                        l5);
          l5 += 4;
        }

        duelOpponentAccepted = false;
        duelMyAccepted = false;
        return;
      }
      if (command == 198) {
        duelNoRetreating = data[1] == 1;
        duelNoMagic = data[2] == 1;
        duelNoPrayer = data[3] == 1;
        duelNoWeapons = data[4] == 1;
        duelOpponentAccepted = false;
        duelMyAccepted = false;
        return;
      }
      if (command == 139) {
        int bankDataOffset = 1;
        int bankSlot = data[bankDataOffset++] & 0xff;
        int bankItemId = DataOperations.getUnsigned2Bytes(data,
                         bankDataOffset);
        bankDataOffset += 2;
        int bankItemCount = DataOperations.getUnsigned4Bytes(data,
                            bankDataOffset);
        bankDataOffset += 4;
        if (bankItemCount == 0) {
          newBankItemCount--;
          for (int currentBankSlot = bankSlot; currentBankSlot < newBankItemCount; currentBankSlot++) {
            newBankItems[currentBankSlot] = newBankItems[currentBankSlot + 1];
            newBankItemsCount[currentBankSlot] = newBankItemsCount[currentBankSlot + 1];
          }

        } else {
          newBankItems[bankSlot] = bankItemId;
          newBankItemsCount[bankSlot] = bankItemCount;
          if (bankSlot >= newBankItemCount)
            newBankItemCount = bankSlot + 1;
        }
        updateBankItems();
        return;
      }
      if (command == 228) {
        int j6 = 1;
        int k12 = 1;
        int i18 = data[j6++] & 0xff;
        int k22 = DataOperations.getUnsigned2Bytes(data, j6);
        j6 += 2;
        if (EntityHandler.getItemDef(k22 & 0x7fff).isStackable()) {
          k12 = DataOperations.readInt(data, j6);
          j6 += 4;
        }
        getInventoryItems()[i18] = k22 & 0x7fff;
        wearing[i18] = k22 / 32768;
        inventoryItemsCount[i18] = k12;
        if (i18 >= inventoryCount)
          inventoryCount = i18 + 1;
        return;
      }
      if (command == 191) {
        int k6 = data[1] & 0xff;
        inventoryCount--;
        for (int l12 = k6; l12 < inventoryCount; l12++) {
          getInventoryItems()[l12] = getInventoryItems()[l12 + 1];
          inventoryItemsCount[l12] = inventoryItemsCount[l12 + 1];
          wearing[l12] = wearing[l12 + 1];
        }
        return;
      }
      if (command == 208) {
        int pointer = 1;
        int idx = data[pointer++] & 0xff;
        int oldExp = playerStatExperience[idx];
        playerStatCurrent[idx] = DataOperations
                                 .getUnsignedByte(data[pointer++]);
        playerStatBase[idx] = DataOperations
                              .getUnsignedByte(data[pointer++]);
        playerStatExperience[idx] = DataOperations.readInt(data,
                                    pointer);
        pointer += 4;

        if (playerStatExperience[idx] > oldExp) {
          expGained += (playerStatExperience[idx] - oldExp);
        }

        statMenu.updateCurrentStats();

        return;
      }
      if (command == 65) {
        duelOpponentAccepted = data[1] == 1;
      }
      if (command == 197) {
        duelMyAccepted = data[1] == 1;
      }
      if (command == 147) {
        showDuelConfirmWindow = true;
        duelWeAccept = false;
        showDuelWindow = false;
        int i7 = 1;
        duelOpponentNameLong = DataOperations.getUnsigned8Bytes(data,
                               i7);
        i7 += 8;
        duelConfirmOpponentItemCount = data[i7++] & 0xff;
        for (int j13 = 0; j13 < duelConfirmOpponentItemCount; j13++) {
          duelConfirmOpponentItems[j13] = DataOperations
                                          .getUnsigned2Bytes(data, i7);
          i7 += 2;
          duelConfirmOpponentItemsCount[j13] = DataOperations
                                               .readInt(data, i7);
          i7 += 4;
        }

        duelConfirmMyItemCount = data[i7++] & 0xff;
        for (int j18 = 0; j18 < duelConfirmMyItemCount; j18++) {
          duelConfirmMyItems[j18] = DataOperations.getUnsigned2Bytes(
                                      data, i7);
          i7 += 2;
          duelConfirmMyItemsCount[j18] = DataOperations.readInt(data,
                                         i7);
          i7 += 4;
        }

        duelCantRetreat = data[i7++] & 0xff;
        duelUseMagic = data[i7++] & 0xff;
        duelUsePrayer = data[i7++] & 0xff;
        duelUseWeapons = data[i7++] & 0xff;
        return;
      }

      if(command == 22) {
        String s = new String(data, 1, length - 1);

        return;
      }
      if (command == 11) {
        String s = new String(data, 1, length - 1);
        playSound(s);
        return;
      }
      if (command == 23) {
        if (anInt892 < 50) {
          int j7 = data[1] & 0xff;
          int k13 = data[2] + getSectionX();
          int k18 = data[3] + getSectionY();
          anIntArray782[anInt892] = j7;
          anIntArray923[anInt892] = 0;
          anIntArray944[anInt892] = k13;
          anIntArray757[anInt892] = k18;
          anInt892++;
        }
        return;
      }
      if (command == 248) {
        if (!hasReceivedWelcomeBoxDetails) {
          lastLoggedInDays = DataOperations
                             .getUnsigned2Bytes(data, 1);
          subscriptionLeftDays = DataOperations.getUnsigned2Bytes(
                                   data, 3);
          lastLoggedInAddress = new String(data, 5, length - 5);
          showWelcomeBox = true;
          hasReceivedWelcomeBoxDetails = true;
        }
        return;
      }
      if (command == 148) {
        serverMessage = new String(data, 1, length - 1);
        showServerMessageBox = true;
        serverMessageBoxTop = false;
        return;
      }
      if (command == 64) {
        serverMessage = new String(data, 1, length - 1);
        showServerMessageBox = true;
        serverMessageBoxTop = true;
        return;
      }
      if (command == 126) {
        fatigue = DataOperations.getUnsigned2Bytes(data, 1);
        return;
      }
      if (command == 206) {
        if (!sleeping) {
        }
        sleeping = true;
        gameMenu.updateText(chatHandle, "");
        super.inputText = "";
        super.enteredText = "";
        sleepEquation = DataOperations.getImage(data, 1, length);
        // sleepMessage = null;
        return;
      }
      if (command == 224) {
        sleeping = false;
        sleepMessage = null;
        return;
      }
      if (command == 225) {
        sleepMessage = "Incorrect - please try again...";
        return;
      }
      if (command == 174) {
        DataOperations.getUnsigned2Bytes(data, 1);
        return;
      }
      if (command == 181) {
        if (autoScreenshot) {
          takeScreenshot(false);
        }
        return;
      }
      if (command == 172) {
        systemUpdate = DataOperations.getUnsigned2Bytes(data, 1) * 32;
        return;
      }

      //System.out.println("Bad ID: " + command + " Length: " + length);
    } catch (Exception e) {
      //e.printStackTrace();
      /*
       * if (handlePacketErrorCount < 3) {
       * super.streamClass.createPacket(156);
       * super.streamClass.addString(runtimeexception.toString());
       * super.streamClass.formatPacket(); handlePacketErrorCount++; }
       */
    }
  }

  private String sleepMessage = "";
  protected final void lostConnection() {
    systemUpdate = 0;
    if (logoutTimeout != 0) {
      resetIntVars();
      return;
    }
    super.lostConnection();
  }

  private final void playSound(String s) {
    if (audioReader == null) {
      return;
    }
    if (configSoundEffects) {
      return;
    }
    audioReader.loadData(sounds, DataOperations.method358(s + ".pcm",
                         sounds), DataOperations.method359(s + ".pcm", sounds));
  }

  private final boolean sendWalkCommand(int walkSectionX, int walkSectionY,
                                        int x1, int y1, int x2, int y2, boolean stepBoolean,
                                        boolean coordsEqual) {
    // todo: needs checking
    int stepCount = engineHandle.getStepCount(walkSectionX, walkSectionY,
                    x1, y1, x2, y2, sectionXArray, sectionYArray, stepBoolean);
    if (stepCount == -1)
      if (coordsEqual) {
        stepCount = 1;
        sectionXArray[0] = x1;
        sectionYArray[0] = y1;
      } else {
        return false;
      }
    stepCount--;
    walkSectionX = sectionXArray[stepCount];
    walkSectionY = sectionYArray[stepCount];
    stepCount--;
    if (coordsEqual)
      super.streamClass.createPacket(246);
    else
      super.streamClass.createPacket(132);
    super.streamClass.add2ByteInt(walkSectionX + getAreaX());
    super.streamClass.add2ByteInt(walkSectionY + getAreaY());
    if (coordsEqual && stepCount == -1 && (walkSectionX + getAreaX()) % 5 == 0)
      stepCount = 0;
    for (int currentStep = stepCount; currentStep >= 0
         && currentStep > stepCount - 25; currentStep--) {
      super.streamClass
      .addByte(sectionXArray[currentStep] - walkSectionX);
      super.streamClass
      .addByte(sectionYArray[currentStep] - walkSectionY);
    }

    super.streamClass.formatPacket();
    actionPictureType = -24;
    actionPictureX = super.mouseX; // guessing the little red/yellow x that
    // appears when you click
    actionPictureY = super.mouseY;
    return true;
  }

  private final boolean sendWalkCommandIgnoreCoordsEqual(int walkSectionX,
      int walkSectionY, int x1, int y1, int x2, int y2,
      boolean stepBoolean, boolean coordsEqual) {
    int stepCount = engineHandle.getStepCount(walkSectionX, walkSectionY,
                    x1, y1, x2, y2, sectionXArray, sectionYArray, stepBoolean);
    if (stepCount == -1)
      return false;
    stepCount--;
    walkSectionX = sectionXArray[stepCount];
    walkSectionY = sectionYArray[stepCount];
    stepCount--;
    if (coordsEqual)
      super.streamClass.createPacket(246);
    else
      super.streamClass.createPacket(132);
    super.streamClass.add2ByteInt(walkSectionX + getAreaX());
    super.streamClass.add2ByteInt(walkSectionY + getAreaY());
    if (coordsEqual && stepCount == -1 && (walkSectionX + getAreaX()) % 5 == 0)
      stepCount = 0;
    for (int currentStep = stepCount; currentStep >= 0
         && currentStep > stepCount - 25; currentStep--) {
      super.streamClass
      .addByte(sectionXArray[currentStep] - walkSectionX);
      super.streamClass
      .addByte(sectionYArray[currentStep] - walkSectionY);
    }

    super.streamClass.formatPacket();
    actionPictureType = -24;
    actionPictureX = super.mouseX;
    actionPictureY = super.mouseY;
    return true;
  }

  public final Image createImage(int i, int j) {
    if (GameWindow.gameFrame != null) {
      return GameWindow.gameFrame.createImage(i, j);
    }
    return super.createImage(i, j);
  }

  private final void drawTradeConfirmWindow() {
    int byte0 = 22 + xAddition;
    int byte1 = 36 + yAddition;
    gameGraphics.drawBox(byte0, byte1, 468, 16, 192);
    int i = 0x989898;
    gameGraphics.drawBoxAlpha(byte0, byte1 + 16, 468, 246, i, 160);
    gameGraphics.drawText("Please confirm your trade with @yel@"
                          + DataOperations.longToString(tradeConfirmOtherNameLong),
                          byte0 + 234, byte1 + 12, 1, 0xffffff);
    gameGraphics.drawText("You are about to give:", byte0 + 117,
                          byte1 + 30, 1, 0xffff00);
    for (int j = 0; j < tradeConfirmItemCount; j++) {
      String s = EntityHandler.getItemDef(tradeConfirmItems[j]).getName();
      if (EntityHandler.getItemDef(tradeConfirmItems[j]).isStackable())
        s = s + " x " + method74(tradeConfirmItemsCount[j]);
      gameGraphics.drawText(s, byte0 + 117, byte1 + 42 + j * 12, 1,
                            0xffffff);
    }

    if (tradeConfirmItemCount == 0)
      gameGraphics.drawText("Nothing!", byte0 + 117, byte1 + 42, 1,
                            0xffffff);
    gameGraphics.drawText("In return you will receive:", byte0 + 351,
                          byte1 + 30, 1, 0xffff00);
    for (int k = 0; k < tradeConfirmOtherItemCount; k++) {
      String s1 = EntityHandler.getItemDef(tradeConfirmOtherItems[k])
                  .getName();
      if (EntityHandler.getItemDef(tradeConfirmOtherItems[k])
          .isStackable())
        s1 = s1 + " x " + method74(tradeConfirmOtherItemsCount[k]);
      gameGraphics.drawText(s1, byte0 + 351, byte1 + 42 + k * 12, 1,
                            0xffffff);
    }

    if (tradeConfirmOtherItemCount == 0)
      gameGraphics.drawText("Nothing!", byte0 + 351, byte1 + 42, 1,
                            0xffffff);
    gameGraphics.drawText("Are you sure you want to do this?", byte0 + 234,
                          byte1 + 200, 4, 65535);
    gameGraphics.drawText(
      "There is NO WAY to reverse a trade if you change your mind.",
      byte0 + 234, byte1 + 215, 1, 0xffffff);
    gameGraphics.drawText("Remember that not all players are trustworthy",
                          byte0 + 234, byte1 + 230, 1, 0xffffff);
    if (!tradeConfirmAccepted) {
      gameGraphics.drawPicture((byte0 + 118) - 35, byte1 + 238,
                               SPRITE_MEDIA_START + 25);
      gameGraphics.drawPicture((byte0 + 352) - 35, byte1 + 238,
                               SPRITE_MEDIA_START + 26);
    } else {
      gameGraphics.drawText("Waiting for other player...", byte0 + 234,
                            byte1 + 250, 1, 0xffff00);
    }
    if (mouseButtonClick == 1) {
      if (super.mouseX < byte0 || super.mouseY < byte1
          || super.mouseX > byte0 + 468 || super.mouseY > byte1 + 262) {
        showTradeConfirmWindow = false;
        super.streamClass.createPacket(216);
        super.streamClass.formatPacket();
      }
      if (super.mouseX >= (byte0 + 118) - 35
          && super.mouseX <= byte0 + 118 + 70
          && super.mouseY >= byte1 + 238
          && super.mouseY <= byte1 + 238 + 21) {
        tradeConfirmAccepted = true;
        super.streamClass.createPacket(53);
        // createPacket(73
        super.streamClass.formatPacket();
      }
      if (super.mouseX >= (byte0 + 352) - 35
          && super.mouseX <= byte0 + 353 + 70
          && super.mouseY >= byte1 + 238
          && super.mouseY <= byte1 + 238 + 21) {
        showTradeConfirmWindow = false;
        super.streamClass.createPacket(216);
        super.streamClass.formatPacket();
      }
      mouseButtonClick = 0;
    }
  }

  private final void walkToGroundItem(int walkSectionX, int walkSectionY,
                                      int x, int y, boolean coordsEqual) {
    if (sendWalkCommandIgnoreCoordsEqual(walkSectionX, walkSectionY, x, y,
                                         x, y, false, coordsEqual)) {
      return;
    } else {
      sendWalkCommand(walkSectionX, walkSectionY, x, y, x, y, true,
                      coordsEqual);
      return;
    }
  }

  private final Mob addNPC(int serverIndex, int x, int y, int nextSprite,
                           int type) {
    if (npcRecordArray[serverIndex] == null) {
      npcRecordArray[serverIndex] = new Mob();
      npcRecordArray[serverIndex].serverIndex = serverIndex;
    }
    Mob mob = npcRecordArray[serverIndex];
    boolean npcAlreadyExists = false;
    for (int lastNpcIndex = 0; lastNpcIndex < lastNpcCount; lastNpcIndex++) {
      if (lastNpcArray[lastNpcIndex].serverIndex != serverIndex)
        continue;
      npcAlreadyExists = true;
      break;
    }

    if (npcAlreadyExists) {
      mob.type = type;
      mob.nextSprite = nextSprite;
      int waypointCurrent = mob.waypointCurrent;
      if (x != mob.waypointsX[waypointCurrent]
          || y != mob.waypointsY[waypointCurrent]) {
        mob.waypointCurrent = waypointCurrent = (waypointCurrent + 1) % 10;
        mob.waypointsX[waypointCurrent] = x;
        mob.waypointsY[waypointCurrent] = y;
      }
    } else {
      mob.serverIndex = serverIndex;
      mob.waypointEndSprite = 0;
      mob.waypointCurrent = 0;
      mob.waypointsX[0] = mob.currentX = x;
      mob.waypointsY[0] = mob.currentY = y;
      mob.type = type;
      mob.nextSprite = mob.currentSprite = nextSprite;
      mob.stepCount = 0;
    }
    npcArray[npcCount++] = mob;
    return mob;
  }

  private final void drawDuelWindow() {
    if (mouseButtonClick != 0 && itemIncrement == 0)
      itemIncrement = 1;
    if (itemIncrement > 0) {
      int i = super.mouseX - 22 - xAddition;
      int j = super.mouseY - 36 - yAddition;
      if (i >= 0 && j >= 0 && i < 468 && j < 262) {
        if (i > 216 && j > 30 && i < 462 && j < 235) {
          int k = (i - 217) / 49 + ((j - 31) / 34) * 5;
          if (k >= 0 && k < inventoryCount) {
            boolean flag1 = false;
            int l1 = 0;
            int k2 = getInventoryItems()[k];
            for (int k3 = 0; k3 < duelMyItemCount; k3++)
              if (duelMyItems[k3] == k2)
                if (EntityHandler.getItemDef(k2).isStackable()) {
                  for (int i4 = 0; i4 < itemIncrement; i4++) {
                    if (duelMyItemsCount[k3] < inventoryItemsCount[k])
                      duelMyItemsCount[k3]++;
                    flag1 = true;
                  }

                } else {
                  l1++;
                }

            if (inventoryCount(k2) <= l1)
              flag1 = true;
            if (!flag1 && duelMyItemCount < 8) {
              duelMyItems[duelMyItemCount] = k2;
              duelMyItemsCount[duelMyItemCount] = 1;
              duelMyItemCount++;
              flag1 = true;
            }
            if (flag1) {
              super.streamClass.createPacket(123);
              super.streamClass.addByte(duelMyItemCount);
              for (int duelItem = 0; duelItem < duelMyItemCount; duelItem++) {
                super.streamClass
                .add2ByteInt(duelMyItems[duelItem]);
                super.streamClass
                .add4ByteInt(duelMyItemsCount[duelItem]);
              }

              super.streamClass.formatPacket();
              duelOpponentAccepted = false;
              duelMyAccepted = false;
            }
          }
        }
        if (i > 8 && j > 30 && i < 205 && j < 129) {
          int l = (i - 9) / 49 + ((j - 31) / 34) * 4;
          if (l >= 0 && l < duelMyItemCount) {
            int j1 = duelMyItems[l];
            for (int i2 = 0; i2 < itemIncrement; i2++) {
              if (EntityHandler.getItemDef(j1).isStackable()
                  && duelMyItemsCount[l] > 1) {
                duelMyItemsCount[l]--;
                continue;
              }
              duelMyItemCount--;
              mouseDownTime = 0;
              for (int l2 = l; l2 < duelMyItemCount; l2++) {
                duelMyItems[l2] = duelMyItems[l2 + 1];
                duelMyItemsCount[l2] = duelMyItemsCount[l2 + 1];
              }

              break;
            }

            super.streamClass.createPacket(123);
            super.streamClass.addByte(duelMyItemCount);
            for (int i3 = 0; i3 < duelMyItemCount; i3++) {
              super.streamClass.add2ByteInt(duelMyItems[i3]);
              super.streamClass.add4ByteInt(duelMyItemsCount[i3]);
            }

            super.streamClass.formatPacket();
            duelOpponentAccepted = false;
            duelMyAccepted = false;
          }
        }
        boolean flag = false;
        if (i >= 93 && j >= 221 && i <= 104 && j <= 232) {
          duelNoRetreating = !duelNoRetreating;
          flag = true;
        }
        if (i >= 93 && j >= 240 && i <= 104 && j <= 251) {
          duelNoMagic = !duelNoMagic;
          flag = true;
        }
        if (i >= 191 && j >= 221 && i <= 202 && j <= 232) {
          duelNoPrayer = !duelNoPrayer;
          flag = true;
        }
        if (i >= 191 && j >= 240 && i <= 202 && j <= 251) {
          duelNoWeapons = !duelNoWeapons;
          flag = true;
        }
        if (flag) {
          super.streamClass.createPacket(225);
          super.streamClass.addByte(duelNoRetreating ? 1 : 0);
          super.streamClass.addByte(duelNoMagic ? 1 : 0);
          super.streamClass.addByte(duelNoPrayer ? 1 : 0);
          super.streamClass.addByte(duelNoWeapons ? 1 : 0);
          super.streamClass.formatPacket();
          duelOpponentAccepted = false;
          duelMyAccepted = false;
        }
        if (i >= 217 && j >= 238 && i <= 286 && j <= 259) {
          duelMyAccepted = true;
          super.streamClass.createPacket(252);
          super.streamClass.formatPacket();
        }
        if (i >= 394 && j >= 238 && i < 463 && j < 259) {
          showDuelWindow = false;
          super.streamClass.createPacket(35);
          super.streamClass.formatPacket();
        }
      } else if (mouseButtonClick != 0) {
        showDuelWindow = false;
        super.streamClass.createPacket(35);
        super.streamClass.formatPacket();
      }
      mouseButtonClick = 0;
      itemIncrement = 0;
    }
    if (!showDuelWindow)
      return;
    int byte0 = 22 + xAddition;
    int byte1 = 36 + yAddition;
    gameGraphics.drawBox(byte0, byte1, 468, 12, 0xc90b1d);
    int i1 = 0x989898;
    gameGraphics.drawBoxAlpha(byte0, byte1 + 12, 468, 18, i1, 160);
    gameGraphics.drawBoxAlpha(byte0, byte1 + 30, 8, 248, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 205, byte1 + 30, 11, 248, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 462, byte1 + 30, 6, 248, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 99, 197, 24, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 192, 197, 23, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 258, 197, 20, i1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 216, byte1 + 235, 246, 43, i1, 160);
    int k1 = 0xd0d0d0;
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 30, 197, 69, k1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 123, 197, 69, k1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 8, byte1 + 215, 197, 43, k1, 160);
    gameGraphics.drawBoxAlpha(byte0 + 216, byte1 + 30, 246, 205, k1, 160);
    for (int j2 = 0; j2 < 3; j2++)
      gameGraphics.drawLineX(byte0 + 8, byte1 + 30 + j2 * 34, 197, 0);

    for (int j3 = 0; j3 < 3; j3++)
      gameGraphics.drawLineX(byte0 + 8, byte1 + 123 + j3 * 34, 197, 0);

    for (int l3 = 0; l3 < 7; l3++)
      gameGraphics.drawLineX(byte0 + 216, byte1 + 30 + l3 * 34, 246, 0);

    for (int k4 = 0; k4 < 6; k4++) {
      if (k4 < 5)
        gameGraphics.drawLineY(byte0 + 8 + k4 * 49, byte1 + 30, 69, 0);
      if (k4 < 5)
        gameGraphics.drawLineY(byte0 + 8 + k4 * 49, byte1 + 123, 69, 0);
      gameGraphics.drawLineY(byte0 + 216 + k4 * 49, byte1 + 30, 205, 0);
    }

    gameGraphics.drawLineX(byte0 + 8, byte1 + 215, 197, 0);
    gameGraphics.drawLineX(byte0 + 8, byte1 + 257, 197, 0);
    gameGraphics.drawLineY(byte0 + 8, byte1 + 215, 43, 0);
    gameGraphics.drawLineY(byte0 + 204, byte1 + 215, 43, 0);
    gameGraphics.drawString("Preparing to duel with: " + duelOpponentName,
                            byte0 + 1, byte1 + 10, 1, 0xffffff);
    gameGraphics.drawString("Your Stake", byte0 + 9, byte1 + 27, 4,
                            0xffffff);
    gameGraphics.drawString("Opponent's Stake", byte0 + 9, byte1 + 120, 4,
                            0xffffff);
    gameGraphics.drawString("Duel Options", byte0 + 9, byte1 + 212, 4,
                            0xffffff);
    gameGraphics.drawString("Your Inventory", byte0 + 216, byte1 + 27, 4,
                            0xffffff);
    gameGraphics.drawString("No retreating", byte0 + 8 + 1,
                            byte1 + 215 + 16, 3, 0xffff00);
    gameGraphics.drawString("No magic", byte0 + 8 + 1, byte1 + 215 + 35, 3,
                            0xffff00);
    gameGraphics.drawString("No prayer", byte0 + 8 + 102, byte1 + 215 + 16,
                            3, 0xffff00);
    gameGraphics.drawString("No weapons", byte0 + 8 + 102,
                            byte1 + 215 + 35, 3, 0xffff00);
    gameGraphics.drawBoxEdge(byte0 + 93, byte1 + 215 + 6, 11, 11, 0xffff00);
    if (duelNoRetreating)
      gameGraphics.drawBox(byte0 + 95, byte1 + 215 + 8, 7, 7, 0xffff00);
    gameGraphics
    .drawBoxEdge(byte0 + 93, byte1 + 215 + 25, 11, 11, 0xffff00);
    if (duelNoMagic)
      gameGraphics.drawBox(byte0 + 95, byte1 + 215 + 27, 7, 7, 0xffff00);
    gameGraphics
    .drawBoxEdge(byte0 + 191, byte1 + 215 + 6, 11, 11, 0xffff00);
    if (duelNoPrayer)
      gameGraphics.drawBox(byte0 + 193, byte1 + 215 + 8, 7, 7, 0xffff00);
    gameGraphics.drawBoxEdge(byte0 + 191, byte1 + 215 + 25, 11, 11,
                             0xffff00);
    if (duelNoWeapons)
      gameGraphics.drawBox(byte0 + 193, byte1 + 215 + 27, 7, 7, 0xffff00);
    if (!duelMyAccepted)
      gameGraphics.drawPicture(byte0 + 217, byte1 + 238,
                               SPRITE_MEDIA_START + 25);
    gameGraphics.drawPicture(byte0 + 394, byte1 + 238,
                             SPRITE_MEDIA_START + 26);
    if (duelOpponentAccepted) {
      gameGraphics.drawText("Other player", byte0 + 341, byte1 + 246, 1,
                            0xffffff);
      gameGraphics.drawText("has accepted", byte0 + 341, byte1 + 256, 1,
                            0xffffff);
    }
    if (duelMyAccepted) {
      gameGraphics.drawText("Waiting for", byte0 + 217 + 35, byte1 + 246,
                            1, 0xffffff);
      gameGraphics.drawText("other player", byte0 + 217 + 35,
                            byte1 + 256, 1, 0xffffff);
    }
    for (int l4 = 0; l4 < inventoryCount; l4++) {
      int i5 = 217 + byte0 + (l4 % 5) * 49;
      int k5 = 31 + byte1 + (l4 / 5) * 34;
      gameGraphics.spriteClip4(i5, k5, 48, 32, SPRITE_ITEM_START
                               + EntityHandler.getItemDef(getInventoryItems()[l4]).getSprite(),
                               EntityHandler.getItemDef(getInventoryItems()[l4])
                               .getPictureMask(), 0, 0, false);
      if (EntityHandler.getItemDef(getInventoryItems()[l4]).isStackable())
        gameGraphics.drawString(
          String.valueOf(inventoryItemsCount[l4]), i5 + 1,
          k5 + 10, 1, 0xffff00);
    }

    for (int j5 = 0; j5 < duelMyItemCount; j5++) {
      int l5 = 9 + byte0 + (j5 % 4) * 49;
      int j6 = 31 + byte1 + (j5 / 4) * 34;
      gameGraphics.spriteClip4(l5, j6, 48, 32, SPRITE_ITEM_START
                               + EntityHandler.getItemDef(duelMyItems[j5]).getSprite(),
                               EntityHandler.getItemDef(duelMyItems[j5]).getPictureMask(),
                               0, 0, false);
      if (EntityHandler.getItemDef(duelMyItems[j5]).isStackable())
        gameGraphics.drawString(String.valueOf(duelMyItemsCount[j5]),
                                l5 + 1, j6 + 10, 1, 0xffff00);
      if (super.mouseX > l5 && super.mouseX < l5 + 48
          && super.mouseY > j6 && super.mouseY < j6 + 32)
        gameGraphics.drawString(EntityHandler.getItemDef(
                                  duelMyItems[j5]).getName()
                                + ": @whi@"
                                + EntityHandler.getItemDef(duelMyItems[j5])
                                .getDescription(), byte0 + 8, byte1 + 273, 1,
                                0xffff00);
    }

    for (int i6 = 0; i6 < duelOpponentItemCount; i6++) {
      int k6 = 9 + byte0 + (i6 % 4) * 49;
      int l6 = 124 + byte1 + (i6 / 4) * 34;
      gameGraphics.spriteClip4(k6, l6, 48, 32, SPRITE_ITEM_START
                               + EntityHandler.getItemDef(duelOpponentItems[i6])
                               .getSprite(), EntityHandler.getItemDef(
                                 duelOpponentItems[i6]).getPictureMask(), 0, 0, false);
      if (EntityHandler.getItemDef(duelOpponentItems[i6]).isStackable())
        gameGraphics.drawString(String
                                .valueOf(duelOpponentItemsCount[i6]), k6 + 1, l6 + 10,
                                1, 0xffff00);
      if (super.mouseX > k6 && super.mouseX < k6 + 48
          && super.mouseY > l6 && super.mouseY < l6 + 32)
        gameGraphics.drawString(EntityHandler.getItemDef(
                                  duelOpponentItems[i6]).getName()
                                + ": @whi@"
                                + EntityHandler.getItemDef(duelOpponentItems[i6])
                                .getDescription(), byte0 + 8, byte1 + 273, 1,
                                0xffff00);
    }

  }

  private final void drawServerMessageBox() {
    int c = '\u0190';
    int c1 = 'd';
    if (serverMessageBoxTop) {
      c1 = '\u01C2';
      c1 = '\u012C';
    }
    gameGraphics.drawBox(256 - c / 2 + xAddition, 167 - c1 / 2 + yAddition, c, c1, 0);
    gameGraphics.drawBoxEdge(256 - c / 2 + xAddition, 167 - c1 / 2 + yAddition, c, c1, 0xffffff);
    gameGraphics.drawBoxTextColour(serverMessage, 256 + xAddition, (167 - c1 / 2) + 20 + yAddition,
                                   1, 0xffffff, c - 40);
    int i = 157 + c1 / 2 + yAddition;
    int j = 0xffffff;
    if (super.mouseY > i - 12 && super.mouseY <= i && super.mouseX > 106 + xAddition
        && super.mouseX < 406 + xAddition)
      j = 0xff0000;
    gameGraphics.drawText("Click here to close window", 256 + xAddition, i, 1, j);
    if (mouseButtonClick == 1) {
      if (j == 0xff0000)
        showServerMessageBox = false;
      if ((super.mouseX < 256 - c / 2 + xAddition || super.mouseX > 256 + c / 2 + xAddition)
          && (super.mouseY < 167 - c1 / 2 + yAddition || super.mouseY > 167 + c1 / 2 + yAddition))
        showServerMessageBox = false;
    }
    mouseButtonClick = 0;
  }

  private final void makeLoginMenus() {
    menuWelcome = new Menu(gameGraphics, 50);
    int i = 40 + yAddition;

    menuWelcome.drawText(256 + xAddition, 200 + i, "Click on an option", 5, true);
    menuWelcome.drawBox(156 + xAddition, 240 + i, 120, 35);
    menuWelcome.drawBox(356 + xAddition, 240 + i, 120, 35);
    menuWelcome.drawText(156 + xAddition, 240 + i, "New User", 5, false);
    menuWelcome.drawText(356 + xAddition, 240 + i, "Existing User", 5, false);
    loginButtonNewUser = menuWelcome.makeButton(156 + xAddition, 240 + i, 120, 35);
    loginButtonExistingUser = menuWelcome.makeButton(356 + xAddition, 240 + i, 120, 35);

    menuNewUser = new Menu(gameGraphics, 50);
    i = 230 + yAddition;
    if (referId == 0) {
      menuNewUser.drawText(256 + xAddition, i + 8,
                           "To create an account please go back to the", 4, true);
      i += 20;
      menuNewUser.drawText(256 + xAddition, i + 8,
                           "www.darkquest.org", 4, true);
    } else if (referId == 1) {
      menuNewUser.drawText(256 + xAddition, i + 8,
                           "then click account", 4,
                           true);
      i += 20;
      menuNewUser.drawText(256 + xAddition, i + 8, "", 4, true);
    } else {
      menuNewUser.drawText(256 + xAddition, i + 8, "", 4, true);
      i += 20;
      menuNewUser.drawText(256 + xAddition, i + 8, "", 4, true);
    }
    i += 30;
    menuNewUser.drawBox(256 + xAddition, i + 17, 150, 34);
    menuNewUser.drawText(256 + xAddition, i + 17, "Ok", 5, false);
    newUserOkButton = menuNewUser.makeButton(256 + xAddition, i + 17, 150, 34);
    menuLogin = new Menu(gameGraphics, 50);
    i = 230 + yAddition;
    loginStatusText = menuLogin.drawText(256 + xAddition, i - 10,
                                         "Please enter your username and password", 4, true);
    i += 28;
    menuLogin.drawBox(140 + xAddition, i, 200, 40);
    menuLogin.drawText(140 + xAddition, i - 10, "Username:", 4, false);
    loginUsernameTextBox = menuLogin.makeTextBox(140 + xAddition, i + 10, 200, 40, 4,
                           12, false, false);
    i += 47;
    menuLogin.drawBox(190 + xAddition, i, 200, 40);
    menuLogin.drawText(190 + xAddition, i - 10, "Password:", 4, false);
    loginPasswordTextBox = menuLogin.makeTextBox(190 + xAddition, i + 10, 200, 40, 4,
                           20, true, false);
    i -= 55;
    menuLogin.drawBox(410 + xAddition, i, 120, 25);
    menuLogin.drawText(410 + xAddition, i, "Play Now", 4, false);
    loginOkButton = menuLogin.makeButton(410 + xAddition, i, 120, 25);
    i += 30;
    menuLogin.drawBox(410 + xAddition, i, 120, 25);
    menuLogin.drawText(410 + xAddition, i, "Cancel", 4, false);
    loginCancelButton = menuLogin.makeButton(410 + xAddition, i, 120, 25);
    i += 30;
    menuLogin.setFocus(loginUsernameTextBox);
  }

  private final void drawGameWindowsMenus() {
    if (logoutTimeout != 0)
      drawLoggingOutBox();
    else if (showWelcomeBox)
      drawWelcomeBox();
    else if (showServerMessageBox)
      drawServerMessageBox();
    else if (wildernessType == 1) // 0 = not wild, 1 = close to wild, 2 =
      // wild
      drawWildernessWarningBox();
    else if (showBank && lastWalkTimeout == 0)
      drawBankBox();
    else if (showShop && lastWalkTimeout == 0)
      drawShopBox();
    else if (showTradeConfirmWindow)
      drawTradeConfirmWindow();
    else if (showTradeWindow)
      drawTradeWindow();
    else if (showDuelConfirmWindow)
      drawDuelConfirmWindow();
    else if (showDuelWindow)
      drawDuelWindow();
    else if (showAbuseWindow == 1)
      drawAbuseWindow1();
    else if (showAbuseWindow == 2)
      drawAbuseWindow2();
    else if (inputBoxType != 0) {
      drawInputBox();
    } else {

      if ((ourPlayer.currentSprite == 8 || ourPlayer.currentSprite == 9)
          || combatWindow)
        drawCombatStyleWindow();
      checkMouseOverMenus();
      boolean noMenusShown = !showRightClickMenu;
      if (noMenusShown)
        menuLength = 0;
      if (mouseOverMenu == 0 && noMenusShown)
        drawInventoryRightClickMenu();
      if (mouseOverMenu == 1)
        drawInventoryMenu(noMenusShown);
      if (mouseOverMenu == 2)
        drawMapMenu(noMenusShown);
      if (mouseOverMenu == 3)
        drawPlayerInfoMenu(noMenusShown);
      if (mouseOverMenu == 4)
        drawMagicWindow(noMenusShown);
      if (mouseOverMenu == 5)
        drawFriendsWindow(noMenusShown);
      if (mouseOverMenu == 6)
        drawOptionsMenu(noMenusShown);
      if (mouseOverMenu == 7)
        drawOurOptionsMenu(noMenusShown);
      if (!showRightClickMenu)
        checkMouseStatus();
      if (showRightClickMenu)
        drawRightClickMenu();
    }
    mouseButtonClick = 0;
  }

  public final void method112(int i, int j, int k, int l, boolean flag) {
    sendWalkCommand(i, j, k, l, k, l, false, flag);
  }

  private final void drawInputBox() {
    if (mouseButtonClick != 0) {
      mouseButtonClick = 0;
      if (inputBoxType == 1
          && (super.mouseX < 106 + xAddition || super.mouseY < 145 + yAddition
              || super.mouseX > 406 + xAddition || super.mouseY > 215 + yAddition)) {
        inputBoxType = 0;
        return;
      }
      if (inputBoxType == 2
          && (super.mouseX < 6 + xAddition || super.mouseY < 145 + yAddition
              || super.mouseX > 506 + xAddition || super.mouseY > 215 + yAddition)) {
        inputBoxType = 0;
        return;
      }
      if (inputBoxType == 3
          && (super.mouseX < 106 + xAddition || super.mouseY < 145 + yAddition
              || super.mouseX > 406 + xAddition || super.mouseY > 215 + yAddition)) {
        inputBoxType = 0;
        return;
      }
      if (super.mouseX > 236 + xAddition && super.mouseX < 276 + xAddition && super.mouseY > 193 + yAddition
          && super.mouseY < 213 + yAddition) {
        inputBoxType = 0;
        return;
      }
    }
    int i = 145 + yAddition;
    if (inputBoxType == 1) {
      gameGraphics.drawBox(106 + xAddition, i, 300, 70, 0);
      gameGraphics.drawBoxEdge(106 + xAddition, i, 300, 70, 0xffffff);
      i += 20;
      gameGraphics.drawText("Enter name to add to friends list", 256 + xAddition, i,
                            4, 0xffffff);
      i += 20;
      gameGraphics.drawText(super.inputText + "*", 256 + xAddition, i, 4, 0xffffff);
      if (super.enteredText.length() > 0) {
        String s = super.enteredText.trim();
        super.inputText = "";
        super.enteredText = "";
        inputBoxType = 0;
        if (s.length() > 0
            && DataOperations.stringLength12ToLong(s) != ourPlayer.nameLong)
          addToFriendsList(s);
      }
    }
    if (inputBoxType == 2) {
      gameGraphics.drawBox(6 + xAddition, i, 500, 70, 0);
      gameGraphics.drawBoxEdge(6 + xAddition, i, 500, 70, 0xffffff);
      i += 20;
      gameGraphics.drawText("Enter message to send to "
                            + DataOperations.longToString(privateMessageTarget), 256 + xAddition,
                            i, 4, 0xffffff);
      i += 20;
      gameGraphics.drawText(super.inputMessage + "*", 256 + xAddition, i, 4, 0xffffff);
      if (super.enteredMessage.length() > 0) {
        String s1 = super.enteredMessage;
        super.inputMessage = "";
        super.enteredMessage = "";
        inputBoxType = 0;
        byte[] message = DataConversions.stringToByteArray(s1);
        sendPrivateMessage(privateMessageTarget, message,
                           message.length);
        s1 = DataConversions.byteToString(message, 0, message.length);
        handleServerMessage("@pri@You tell "
                            + DataOperations.longToString(privateMessageTarget)
                            + ": " + s1);
      }
    }
    if (inputBoxType == 3) {
      gameGraphics.drawBox(106 + xAddition, i, 300, 70, 0);
      gameGraphics.drawBoxEdge(106 + xAddition, i, 300, 70, 0xffffff);
      i += 20;
      gameGraphics.drawText("Enter name to add to ignore list", 256 + xAddition, i,
                            4, 0xffffff);
      i += 20;
      gameGraphics.drawText(super.inputText + "*", 256 + xAddition, i, 4, 0xffffff);
      if (super.enteredText.length() > 0) {
        String s2 = super.enteredText.trim();
        super.inputText = "";
        super.enteredText = "";
        inputBoxType = 0;
        if (s2.length() > 0
            && DataOperations.stringLength12ToLong(s2) != ourPlayer.nameLong)
          addToIgnoreList(s2);
      }
    }
    int j = 0xffffff;
    if (super.mouseX > 236 + xAddition && super.mouseX < 276 + xAddition && super.mouseY > 193 + yAddition
        && super.mouseY < 213 + yAddition)
      j = 0xffff00;
    gameGraphics.drawText("Cancel", 256 + xAddition, 208 + yAddition, 1, j);
  }

  private final boolean hasRequiredRunes(int i, int j) {
    if (i == 31 && (method117(197) || method117(615) || method117(682))) {
      return true;
    }
    if (i == 32 && (method117(102) || method117(616) || method117(683))) {
      return true;
    }
    if (i == 33 && (method117(101) || method117(617) || method117(684))) {
      return true;
    }
    if (i == 34 && (method117(103) || method117(618) || method117(685))) {
      return true;
    }
    return inventoryCount(i) >= j;
  }

  private final void resetPrivateMessageStrings() {
    super.inputMessage = "";
    super.enteredMessage = "";
  }

  private final boolean method117(int i) {
    for (int j = 0; j < inventoryCount; j++)
      if (getInventoryItems()[j] == i && wearing[j] == 1)
        return true;

    return false;
  }

  private final void setPixelsAndAroundColour(int x, int y, int colour) {
    gameGraphics.setPixelColour(x, y, colour);
    gameGraphics.setPixelColour(x - 1, y, colour);
    gameGraphics.setPixelColour(x + 1, y, colour);
    gameGraphics.setPixelColour(x, y - 1, colour);
    gameGraphics.setPixelColour(x, y + 1, colour);
  }

  private final void method119() {
    for (int i = 0; i < mobMessageCount; i++) {
      int j = gameGraphics.messageFontHeight(1);
      int l = mobMessagesX[i];
      int k1 = mobMessagesY[i];
      int j2 = mobMessagesWidth[i];
      int i3 = mobMessagesHeight[i];
      boolean flag = true;
      while (flag) {
        flag = false;
        for (int i4 = 0; i4 < i; i4++)
          if (k1 + i3 > mobMessagesY[i4] - j
              && k1 - j < mobMessagesY[i4]
              + mobMessagesHeight[i4]
              && l - j2 < mobMessagesX[i4] + mobMessagesWidth[i4]
              && l + j2 > mobMessagesX[i4] - mobMessagesWidth[i4]
              && mobMessagesY[i4] - j - i3 < k1) {
            k1 = mobMessagesY[i4] - j - i3;
            flag = true;
          }

      }
      mobMessagesY[i] = k1;
      gameGraphics.drawBoxTextColour(mobMessages[i], l, k1, 1, 0xffff00,
                                     300);
    }

    for (int k = 0; k < anInt699; k++) {
      int i1 = anIntArray858[k];
      int l1 = anIntArray859[k];
      int k2 = anIntArray705[k];
      int j3 = anIntArray706[k];
      int l3 = (39 * k2) / 100;
      int j4 = (27 * k2) / 100;
      int k4 = l1 - j4;
      gameGraphics.spriteClip2(i1 - l3 / 2, k4, l3, j4,
                               SPRITE_MEDIA_START + 9, 85);
      int l4 = (36 * k2) / 100;
      int i5 = (24 * k2) / 100;
      gameGraphics.spriteClip4(i1 - l4 / 2, (k4 + j4 / 2) - i5 / 2, l4,
                               i5, EntityHandler.getItemDef(j3).getSprite()
                               + SPRITE_ITEM_START, EntityHandler.getItemDef(j3)
                               .getPictureMask(), 0, 0, false);
    }

    for (int j1 = 0; j1 < anInt718; j1++) {
      int i2 = anIntArray786[j1];
      int l2 = anIntArray787[j1];
      int k3 = anIntArray788[j1];
      gameGraphics.drawBoxAlpha(i2 - 15, l2 - 3, k3, 5, 65280, 192);
      gameGraphics.drawBoxAlpha((i2 - 15) + k3, l2 - 3, 30 - k3, 5,
                                0xff0000, 192);
    }

  }

  private final void drawMapMenu(boolean flag) {
    int i = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
    char c = '\234';
    char c2 = '\230';
    gameGraphics.drawPicture(i - 49, 3, SPRITE_MEDIA_START + 2);
    i += 40;
    gameGraphics.drawBox(i, 36, c, c2, 0);
    gameGraphics.setDimensions(i, 36, i + c, 36 + c2);
    int k = 192 + anInt986;
    int i1 = cameraRotation + anInt985 & 0xff;
    int k1 = ((ourPlayer.currentX - 6040) * 3 * k) / 2048;
    int i3 = ((ourPlayer.currentY - 6040) * 3 * k) / 2048;
    int k4 = Camera.anIntArray384[1024 - i1 * 4 & 0x3ff];
    int i5 = Camera.anIntArray384[(1024 - i1 * 4 & 0x3ff) + 1024];
    int k5 = i3 * k4 + k1 * i5 >> 18;
    i3 = i3 * i5 - k1 * k4 >> 18;
    k1 = k5;
    gameGraphics.method242((i + c / 2) - k1, 36 + c2 / 2 + i3,
                           SPRITE_MEDIA_START - 1, i1 + 64 & 0xff, k);
    for (int i7 = 0; i7 < objectCount; i7++) {
      int l1 = (((objectX[i7] * magicLoc + 64) - ourPlayer.currentX) * 3 * k) / 2048;
      int j3 = (((objectY[i7] * magicLoc + 64) - ourPlayer.currentY) * 3 * k) / 2048;
      int l5 = j3 * k4 + l1 * i5 >> 18;
      j3 = j3 * i5 - l1 * k4 >> 18;
      l1 = l5;
      setPixelsAndAroundColour(i + c / 2 + l1, (36 + c2 / 2) - j3, 65535);
    }

    for (int j7 = 0; j7 < groundItemCount; j7++) {
      int i2 = (((groundItemX[j7] * magicLoc + 64) - ourPlayer.currentX) * 3 * k) / 2048;
      int k3 = (((groundItemY[j7] * magicLoc + 64) - ourPlayer.currentY) * 3 * k) / 2048;
      int i6 = k3 * k4 + i2 * i5 >> 18;
      k3 = k3 * i5 - i2 * k4 >> 18;
      i2 = i6;
      setPixelsAndAroundColour(i + c / 2 + i2, (36 + c2 / 2) - k3,
                               0xff0000);
    }

    for (int k7 = 0; k7 < npcCount; k7++) {
      Mob mob = npcArray[k7];
      int j2 = ((mob.currentX - ourPlayer.currentX) * 3 * k) / 2048;
      int l3 = ((mob.currentY - ourPlayer.currentY) * 3 * k) / 2048;
      int j6 = l3 * k4 + j2 * i5 >> 18;
      l3 = l3 * i5 - j2 * k4 >> 18;
      j2 = j6;
      setPixelsAndAroundColour(i + c / 2 + j2, (36 + c2 / 2) - l3,
                               0xffff00);
    }

    for (int l7 = 0; l7 < playerCount; l7++) {
      Mob mob_1 = playerArray[l7];
      int k2 = ((mob_1.currentX - ourPlayer.currentX) * 3 * k) / 2048;
      int i4 = ((mob_1.currentY - ourPlayer.currentY) * 3 * k) / 2048;
      int k6 = i4 * k4 + k2 * i5 >> 18;
      i4 = i4 * i5 - k2 * k4 >> 18;
      k2 = k6;
      int j8 = 0xffffff;
      for (int k8 = 0; k8 < super.friendsCount; k8++) {
        if (mob_1.nameLong != super.friendsListLongs[k8]
            || super.friendsListOnlineStatus[k8] != 99)
          continue;
        j8 = 65280;
        break;
      }

      setPixelsAndAroundColour(i + c / 2 + k2, (36 + c2 / 2) - i4, j8);
    }

    gameGraphics.method212(i + c / 2, 36 + c2 / 2, 2, 0xffffff, 255);
    gameGraphics.method242(i + 19, 55, SPRITE_MEDIA_START + 24,
                           cameraRotation + 128 & 0xff, 128);
    gameGraphics.setDimensions(0, 0, windowWidth, windowHeight + 12);
    if (!flag)
      return;
    i = super.mouseX
        - (((GameImage) (gameGraphics)).menuDefaultWidth - 199);
    int i8 = super.mouseY - 36;
    if (i >= 40 && i8 >= 0 && i < 196 && i8 < 152) {
      char c1 = '\234';
      char c3 = '\230';
      int l = 192 + anInt986;
      int j1 = cameraRotation + anInt985 & 0xff;
      int j = ((GameImage) (gameGraphics)).menuDefaultWidth - 199;
      j += 40;
      int l2 = ((super.mouseX - (j + c1 / 2)) * 16384) / (3 * l);
      int j4 = ((super.mouseY - (36 + c3 / 2)) * 16384) / (3 * l);
      int l4 = Camera.anIntArray384[1024 - j1 * 4 & 0x3ff];
      int j5 = Camera.anIntArray384[(1024 - j1 * 4 & 0x3ff) + 1024];
      int l6 = j4 * l4 + l2 * j5 >> 15;
      j4 = j4 * j5 - l2 * l4 >> 15;
      l2 = l6;
      l2 += ourPlayer.currentX;
      j4 = ourPlayer.currentY - j4;
      if (mouseButtonClick == 1)
        method112(getSectionX(), getSectionY(), l2 / 128, j4 / 128, false);
      mouseButtonClick = 0;
    }
  }

  public mudclient() {
    combatWindow = false;
    threadSleepTime = 10;
    try {
      localhost = InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      e.printStackTrace();
      localhost = "unknown";
    }

    if(config.getProperty("width") == null) {
      Config.storeConfig("width", "502");
    }
    if(config.getProperty("height") == null) {
      Config.storeConfig("height", "382");
    }

    if(config.getProperty("refreshRate") == null) {
      Config.storeConfig("refreshRate", "" + Resolutions.oldDisplayMode.getRefreshRate());
    }
    if(config.getProperty("bitDepth") == null) {
      Config.storeConfig("bitDepth", "" + Resolutions.oldDisplayMode.getBitDepth());
    }
    windowWidth = Integer.valueOf(config.getProperty("width"));
    windowHeight = Integer.valueOf(config.getProperty("height")) - 40;
    screenDepth = Integer.valueOf(config.getProperty("bitDepth"));
    screenRefreshRate = Integer.valueOf(config.getProperty("refreshRate"));
    if(windowWidth < defaultWidth || windowHeight < defaultHeight) {
      windowWidth = defaultWidth;
      Config.storeConfig("width", "502");
      windowHeight = defaultHeight;
      Config.storeConfig("height", "382");
    }
    if(windowWidth > 532) {
      xAddition = (windowWidth - defaultWidth) / 2;
      yAddition = (windowHeight - defaultHeight) / 2;
    } else {
      xAddition = 0;
      yAddition = 0;
    }
    startTime = System.currentTimeMillis();
    duelMyItems = new int[8];
    duelMyItemsCount = new int[8];
    configAutoCameraAngle = true;
    questionMenuAnswer = new String[10];
    lastNpcArray = new Mob[500];
    currentUser = "";
    currentPass = "";
    menuText1 = new String[250];
    duelOpponentAccepted = false;
    duelMyAccepted = false;
    tradeConfirmItems = new int[14];
    tradeConfirmItemsCount = new int[14];
    tradeConfirmOtherItems = new int[14];
    tradeConfirmOtherItemsCount = new int[14];
    serverMessage = "";
    duelOpponentName = "";
    setInventoryItems(new int[35]);
    inventoryItemsCount = new int[35];
    wearing = new int[35];
    mobMessages = new String[50];
    showBank = false;
    doorModel = new Model[500];
    mobMessagesX = new int[50];
    mobMessagesY = new int[50];
    mobMessagesWidth = new int[50];
    mobMessagesHeight = new int[50];
    npcArray = new Mob[500];
    equipmentStatus = new int[6];
    prayerOn = new boolean[50];
    tradeOtherAccepted = false;
    tradeWeAccepted = false;
    mobArray = new Mob[8000];
    anIntArray705 = new int[50];
    anIntArray706 = new int[50];
    lastWildYSubtract = -1;
    memoryError = false;
    bankItemsMax = 48;
    magicLoc = 128;
    cameraAutoAngle = 1;
    anInt727 = 2;
    showServerMessageBox = false;
    hasReceivedWelcomeBoxDetails = false;
    playerStatCurrent = new int[18];
    wildYSubtract = -1;
    anInt742 = -1;
    anInt743 = -1;
    anInt744 = -1;
    sectionXArray = new int[8000];
    sectionYArray = new int[8000];
    selectedItem = -1;
    selectedItemName = "";
    duelOpponentItems = new int[8];
    duelOpponentItemsCount = new int[8];
    anIntArray757 = new int[50];
    menuID = new int[250];
    showCharacterLookScreen = false;
    lastPlayerArray = new Mob[500];
    gameDataModels = new Model[1000];
    configMouseButtons = false;
    duelNoRetreating = false;
    duelNoMagic = false;
    duelNoPrayer = false;
    duelNoWeapons = false;
    anIntArray782 = new int[50];
    duelConfirmOpponentItems = new int[8];
    duelConfirmOpponentItemsCount = new int[8];
    anIntArray786 = new int[50];
    anIntArray787 = new int[50];
    anIntArray788 = new int[50];
    objectModelArray = new Model[1500];
    cameraRotation = 128;
    showWelcomeBox = false;
    characterBodyGender = 1;
    character2Colour = 2;
    characterHairColour = 2;
    characterTopColour = 8;
    characterBottomColour = 14;
    characterHeadGender = 1;
    selectedBankItem = -1;
    selectedBankItemType = -2;
    menuText2 = new String[250];
    aBooleanArray827 = new boolean[1500];
    playerStatBase = new int[18];
    menuActionType = new int[250];
    menuActionVariable = new int[250];
    menuActionVariable2 = new int[250];
    shopItems = new int[256];
    shopItemCount = new int[256];
    shopItemsSellPrice = new int[256];
    shopItemsBuyPrice = new int[256];
    anIntArray858 = new int[50];
    anIntArray859 = new int[50];
    newBankItems = new int[256];
    newBankItemsCount = new int[256];
    duelConfirmMyItems = new int[8];
    duelConfirmMyItemsCount = new int[8];
    mobArrayIndexes = new int[500];
    messagesTimeout = new int[5];
    objectX = new int[1500];
    objectY = new int[1500];
    objectType = new int[1500];
    objectID = new int[1500];
    menuActionX = new int[250];
    menuActionY = new int[250];
    ourPlayer = new Mob();
    serverIndex = -1;
    anInt882 = 30;
    showTradeConfirmWindow = false;
    tradeConfirmAccepted = false;
    playerArray = new Mob[500];
    serverMessageBoxTop = false;
    cameraHeight = 750;
    bankItems = new int[256];
    bankItemsCount = new int[256];
    notInWilderness = false;
    selectedSpell = -1;
    anInt911 = 2;
    tradeOtherItems = new int[14];
    tradeOtherItemsCount = new int[14];
    menuIndexes = new int[250];
    zoomCamera = false;
    playerStatExperience = new int[18];
    cameraAutoAngleDebug = false;
    npcRecordArray = new Mob[8000];
    showDuelWindow = false;
    anIntArray923 = new int[50];
    lastLoadedNull = false;
    experienceArray = new int[99];
    showShop = false;
    mouseClickXArray = new int[8192];
    mouseClickYArray = new int[8192];
    showDuelConfirmWindow = false;
    duelWeAccept = false;
    doorX = new int[500];
    doorY = new int[500];
    configSoundEffects = false;
    showRightClickMenu = false;
    attackingInt40 = 40;
    anIntArray944 = new int[50];
    doorDirection = new int[500];
    doorType = new int[500];
    groundItemX = new int[8000];
    groundItemY = new int[8000];
    groundItemType = new int[8000];
    groundItemObjectVar = new int[8000];
    selectedShopItemIndex = -1;
    selectedShopItemType = -2;
    messagesArray = new String[5];
    showTradeWindow = false;
    aBooleanArray970 = new boolean[500];
    tradeMyItems = new int[14];
    tradeMyItemsCount = new int[14];
    cameraSizeInt = 9;
    tradeOtherPlayerName = "";
    mc = this;

  }

  private boolean combatWindow;
  private int lastLoggedInDays;
  private int subscriptionLeftDays;
  private int duelMyItemCount;
  private int duelMyItems[];
  private int duelMyItemsCount[];
  private boolean configAutoCameraAngle;
  private String questionMenuAnswer[];
  private int anInt658;
  private Mob lastNpcArray[];
  private int loginButtonNewUser;
  private int loginButtonExistingUser;
  private String currentUser;
  private String currentPass;
  private int lastWalkTimeout;
  private String menuText1[];
  private boolean duelOpponentAccepted;
  private boolean duelMyAccepted;
  private int tradeConfirmItemCount;
  private int tradeConfirmItems[];
  private int tradeConfirmItemsCount[];
  private int tradeConfirmOtherItemCount;
  private int tradeConfirmOtherItems[];
  private int tradeConfirmOtherItemsCount[];
  static String serverMessage;
  private String duelOpponentName;
  private int mouseOverBankPageText;
  public int playerCount;
  private int lastPlayerCount;
  private int fightCount;
  private int inventoryCount;
  private int inventoryItems[];
  private int inventoryItemsCount[];
  private int wearing[];
  private int mobMessageCount;
  String mobMessages[];
  private boolean showBank;
  private Model doorModel[];
  private int mobMessagesX[];
  private int mobMessagesY[];
  private int mobMessagesWidth[];
  private int mobMessagesHeight[];
  public Mob npcArray[];
  private int equipmentStatus[];
  private final int characterTopBottomColours[] = { 0xff0000, 0xff8000,
                                                    0xffe000, 0xa0e000, 57344, 32768, 41088, 45311, 33023, 12528,
                                                    0xe000e0, 0x303030, 0x604000, 0x805000, 0xffffff
                                                  };
  private int loginScreenNumber;
  private int anInt699;
  private boolean prayerOn[];
  private boolean tradeOtherAccepted;
  private boolean tradeWeAccepted;
  private Mob mobArray[];
  private int npcCombatModelArray1[] = { 0, 1, 2, 1, 0, 0, 0, 0 };
  private int anIntArray705[];
  private int anIntArray706[];
  public int npcCount;
  private int lastNpcCount;
  private int wildX;
  private int wildY;
  private int wildYMultiplier;
  private int lastWildYSubtract;
  private boolean memoryError;
  private int bankItemsMax;
  private int mouseOverMenu;
  private int walkModel[] = { 0, 1, 2, 1 };
  private int anInt718;
  public int magicLoc;
  public int loggedIn;
  private int cameraAutoAngle;
  private int cameraRotationBaseAddition;
  private Menu spellMenu;
  int spellMenuHandle;
  int menuMagicPrayersSelected;
  private int screenRotationX;
  private int anInt727;
  private int showAbuseWindow;
  private int duelCantRetreat;
  private int duelUseMagic;
  private int duelUsePrayer;
  private int duelUseWeapons;
  static boolean showServerMessageBox;
  private boolean hasReceivedWelcomeBoxDetails;
  private String lastLoggedInAddress;
  private int loginTimer;
  private int playerStatCurrent[];
  private int areaX;
  private int areaY;
  private int wildYSubtract;
  private int anInt742;
  private int anInt743;
  private int anInt744;
  public int sectionXArray[];
  public int sectionYArray[];
  private int selectedItem;
  String selectedItemName;
  private int menuX;
  private int menuY;
  private int menuWidth;
  private int menuHeight;
  private int menuLength;
  private int duelOpponentItemCount;
  private int duelOpponentItems[];
  private int duelOpponentItemsCount[];
  private int anIntArray757[];
  private int menuID[];
  private boolean showCharacterLookScreen;
  private int newBankItemCount;
  private int npcCombatModelArray2[] = { 0, 0, 0, 0, 0, 1, 2, 1 };
  private Mob lastPlayerArray[];
  int inputBoxType;
  private int combatStyle;
  private Model gameDataModels[];
  private boolean configMouseButtons;
  private boolean duelNoRetreating;
  private boolean duelNoMagic;
  private boolean duelNoPrayer;
  private boolean duelNoWeapons;
  private int anIntArray782[];
  private int duelConfirmOpponentItemCount;
  private int duelConfirmOpponentItems[];
  private int duelConfirmOpponentItemsCount[];
  private int anIntArray786[];
  private int anIntArray787[];
  private int anIntArray788[];
  private int anInt789;
  private int anInt790;
  private int anInt791;
  private int anInt792;
  private Menu menuLogin;
  private final int characterHairColours[] = { 0xffc030, 0xffa040, 0x805030,
                                               0x604020, 0x303030, 0xff6020, 0xff4000, 0xffffff, 65280, 65535
                                             };
  private Model objectModelArray[];
  private Menu menuWelcome;
  private int systemUpdate;
  private int cameraRotation;
  private int logoutTimeout;
  private Menu gameMenu;
  //Wilderness
  int messagesHandleType2;
  int chatHandle;
  int messagesHandleType5;
  int messagesHandleType6;
  int messagesTab;
  private boolean showWelcomeBox;
  private int characterHeadType;
  private int characterBodyGender;
  private int character2Colour;
  private int characterHairColour;
  private int characterTopColour;
  private int characterBottomColour;
  private int characterSkinColour;
  private int characterHeadGender;
  // shopItems
  private int loginStatusText;
  private int loginUsernameTextBox;
  private int loginPasswordTextBox;
  private int loginOkButton;
  private int loginCancelButton;
  private int selectedBankItem;
  private int selectedBankItemType;
  private String menuText2[];
  int anInt826;
  private boolean aBooleanArray827[];
  public int playerStatBase[];
  private int abuseSelectedType;
  public int actionPictureType;
  int actionPictureX;
  int actionPictureY;
  private int menuActionType[];
  private int menuActionVariable[];
  private int menuActionVariable2[];
  private int shopItems[];
  private int shopItemCount[];
  private int shopItemsBuyPrice[];
  private int shopItemsSellPrice[];
  private int npcAnimationArray[][] = {
    { 11, 2, 9, 7, 1, 6, 10, 0, 5, 8, 3, 4 },
    { 11, 2, 9, 7, 1, 6, 10, 0, 5, 8, 3, 4 },
    { 11, 3, 2, 9, 7, 1, 6, 10, 0, 5, 8, 4 },
    { 3, 4, 2, 9, 7, 1, 6, 10, 8, 11, 0, 5 },
    { 3, 4, 2, 9, 7, 1, 6, 10, 8, 11, 0, 5 },
    { 4, 3, 2, 9, 7, 1, 6, 10, 8, 11, 0, 5 },
    { 11, 4, 2, 9, 7, 1, 6, 10, 0, 5, 8, 3 },
    { 11, 2, 9, 7, 1, 6, 10, 0, 5, 8, 4, 3 }
  };
  private int bankItemCount;
  private int characterDesignHeadButton1;
  private int characterDesignHeadButton2;
  private int characterDesignHairColourButton1;
  private int characterDesignHairColourButton2;
  private int characterDesignGenderButton1;
  private int characterDesignGenderButton2;
  private int characterDesignTopColourButton1;
  private int characterDesignTopColourButton2;
  private int characterDesignSkinColourButton1;
  private int characterDesignSkinColourButton2;
  private int characterDesignBottomColourButton1;
  private int characterDesignBottomColourButton2;
  private int characterDesignAcceptButton;
  private int anIntArray858[];
  private int anIntArray859[];
  private int newBankItems[];
  private int newBankItemsCount[];
  private int duelConfirmMyItemCount;
  private int duelConfirmMyItems[];
  private int duelConfirmMyItemsCount[];
  private int mobArrayIndexes[];
  private Menu menuNewUser;
  private int messagesTimeout[];
  private int lastAutoCameraRotatePlayerX;
  private int lastAutoCameraRotatePlayerY;
  public int objectX[];
  public int objectY[];
  public int objectType[];
  private int objectID[];
  private int menuActionX[];
  private int menuActionY[];
  public Mob ourPlayer;
  private int sectionX;
  private int sectionY;
  int serverIndex;
  private int anInt882;
  private int mouseDownTime;
  private int itemIncrement;
  public int groundItemCount;
  private int modelFireLightningSpellNumber;
  private int modelTorchNumber;
  private int modelClawSpellNumber;
  private boolean showTradeConfirmWindow;
  private boolean tradeConfirmAccepted;
  private int anInt892;
  public EngineHandle engineHandle;
  public Mob playerArray[];
  private boolean serverMessageBoxTop;
  private final String equipmentStatusName[] = { "Armour", "WeaponAim",
                                                 "WeaponPower", "Magic", "Prayer", "Range"
                                               };
  public int flagged = 0;
  private int referId;
  private int anInt900;
  private int newUserOkButton;
  private int mouseButtonClick;
  private int cameraHeight;
  private int bankItems[];
  private int bankItemsCount[];
  private boolean notInWilderness;
  private int selectedSpell;
  private int screenRotationY;
  private int anInt911;
  private int tradeOtherItemCount;
  private int tradeOtherItems[];
  private int tradeOtherItemsCount[];
  private int menuIndexes[];
  private boolean zoomCamera;
  private AudioReader audioReader;
  public int playerStatExperience[];
  private boolean cameraAutoAngleDebug;
  private Mob npcRecordArray[];
  private final String skillArray[] = { "Attack", "Defense", "Strength",
                                        "Hits", "Ranged", "Prayer", "Magic", "Cooking", "Woodcut",
                                        "Fletching", "Fishing", "Firemaking", "Crafting", "Smithing",
                                        "Mining", "Herblaw", "Agility", "Thieving"
                                      };
  private boolean showDuelWindow;
  private int anIntArray923[];
  public GameImageMiddleMan gameGraphics;
  private final String skillArrayLong[] = { "Attack", "Defense", "Strength",
                                            "Hits", "Ranged", "Prayer", "Magic", "Cooking", "Woodcutting",
                                            "Fletching", "Fishing", "Firemaking", "Crafting", "Smithing",
                                            "Mining", "Herblaw", "Agility", "Thieving"
                                          };
  private boolean lastLoadedNull;
  private int experienceArray[];
  private Camera gameCamera;
  private boolean showShop;
  private int mouseClickArrayOffset;
  int mouseClickXArray[];
  int mouseClickYArray[];
  private boolean showDuelConfirmWindow;
  private boolean duelWeAccept;
  private Graphics aGraphics936;
  private int doorX[];
  private int doorY[];
  private int wildernessType;
  private boolean configSoundEffects;
  private boolean showRightClickMenu;
  private int screenRotationTimer;
  private int attackingInt40;
  private int anIntArray944[];
  private Menu characterDesignMenu;
  private int modelUpdatingTimer;
  private int doorCount;
  private int doorDirection[];
  private int doorType[];
  private int anInt952;
  private int anInt953;
  private int anInt954;
  private int anInt955;
  public int groundItemX[];
  public int groundItemY[];
  public int groundItemType[];
  private int groundItemObjectVar[];
  private int selectedShopItemIndex;
  private int selectedShopItemType;
  private String messagesArray[];
  private long tradeConfirmOtherNameLong;
  private boolean showTradeWindow;
  private int playerAliveTimeout;
  private final int characterSkinColours[] = { 0xecded0, 0xccb366, 0xb38c40,
                                               0x997326, 0x906020
                                             };
  private byte sounds[];
  //3150
  private boolean aBooleanArray970[];
  public int objectCount;
  private int tradeMyItemCount;
  private int tradeMyItems[];
  private int tradeMyItemsCount[];
  public int windowWidth = 512;
  public int windowHeight = 334;
  public int screenDepth;
  public int screenRefreshRate;
  int defaultWidth = 512;
  int defaultHeight = 334;
  public int xAddition;
  public int yAddition;
  private int cameraSizeInt;
  private Menu friendsMenu;
  int friendsMenuHandle;
  int anInt981;
  long privateMessageTarget;
  private long duelOpponentNameLong;
  private String tradeOtherPlayerName;
  private int anInt985;
  private int anInt986;
  private boolean aBoolean767;
  public Image img = Toolkit.getDefaultToolkit().getImage(mudclient.class.getResource("dq.png"));

  public final void drawDownloadProgress(String s, int progress) {
    try {

      getLoadingGraphics().setColor(Color.black);
      getLoadingGraphics().fillRect(0, 0, appletWidth, appletHeight);

      int j = (appletWidth - 281) / 2;
      int k = (appletHeight - 148) / 2 + 50;

      j += 2 + 4;
      k += 122;
      //BAR_COLOUR
      int l = (281 * progress) / 100;

      getLoadingGraphics().setColor(new Color(255, 208, 66));
      getLoadingGraphics().fillRect(j, k, l, 1);
      getLoadingGraphics().setColor(new Color(187, 6, 6));
      getLoadingGraphics().drawRect(j - 1, k - 1, 281 + 1, 2);


      getLoadingGraphics().setColor(Color.white);
      getLoadingGraphics().drawString(""  + s + " " + (progress > 0 ? progress + "%" : ""), j + 64, k - 19);

    } catch (Exception _ex) {
      _ex.printStackTrace();
    }
  }




  public static File getFile(String filename) {
    File file = new File(new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data" + File.separator + filename);
    if (file.isFile() && file.exists()) {
      return file;
    } else return null;
  }
  public static File loadCachedFile(String filename) {
    return new File(new File(mudclient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile() + File.separator + "data" + File.separator + filename);
  }

  public void setSectionX(int sectionX) {
    this.sectionX = sectionX;
  }

  public int getSectionX() {
    return sectionX;
  }

  public void setSectionY(int sectionY) {
    this.sectionY = sectionY;
  }

  public int getSectionY() {
    return sectionY;
  }

  public void setAreaY(int areaY) {
    this.areaY = areaY;
  }

  public int getAreaY() {
    return areaY;
  }

  public void setAreaX(int areaX) {
    this.areaX = areaX;
  }

  public int getAreaX() {
    return areaX;
  }
  public static void drawPopup(String message) {
    serverMessage = message;
    showServerMessageBox = true;
  }

  public void sendSmithingItem(int spriteID) {
    streamClass.createPacket(201);
    streamClass.add4ByteInt(spriteID);
    streamClass.formatPacket();
  }
  public void sendSmithingClose() {
    streamClass.createPacket(202);
    streamClass.formatPacket();

  }

  public void setInventoryItems(int inventoryItems[]) {
    this.inventoryItems = inventoryItems;
  }

  public int[] getInventoryItems() {
    return inventoryItems;
  }

  public void handleQuestionMenuAnswer(int clickId) {
    super.streamClass.createPacket(154);
    super.streamClass.addByte(clickId);
    super.streamClass.formatPacket();
  }
}

