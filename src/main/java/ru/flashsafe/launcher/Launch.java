package ru.flashsafe.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import net.samuelcampos.usbdrivedectector.USBDeviceDetectorManager;
import net.samuelcampos.usbdrivedectector.USBStorageDevice;
import org.pkcs11.jacknji11.CKA;
import org.pkcs11.jacknji11.CKO;
import org.pkcs11.jacknji11.CK_TOKEN_INFO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flashsafe.launcher.util.OSUtils;
import ru.flashsafe.launcher.util.TokenUtil;

/**
 * @author Alexander Krysin
 *
 */
public class Launch {
        private static final Logger LOGGER = LoggerFactory.getLogger(ru.flashsafe.launcher.Launch.class);
	private static final Class[] parameters = new Class[]{URL.class};
	
        private static long session;
        private static long slot;
        
	public static File flash;
        
        public static final String LIB_PATH = "F:/Eclipse/ui-client/starter/";

	/**
	 * @param args
	 */
	public static void main(String[] args)  {
            if(OSUtils.isLinux()) {
                if(is64BitSystem()) {
                    loadLibsForLinux64Bit();
                } else if(is86BitSystem()) {
                    loadLibsForLinux32Bit();
                }
            } else if(OSUtils.isMacOS()) {
                loadLibsForMacOS();
            } else if(OSUtils.isWindows()) {
                if(is64BitSystem()) {
                    loadLibsForWindows64Bit();
                } else if(is86BitSystem()) {
                    loadLibsForWindows32Bit();
                }
            }
            if(initToken()) {
                if(checkInternet()) {
                    if(checkUpdate()) {
                        System.out.println("Update found");
                        if(update()) {
                            System.out.println("Update loaded");
                            updateCurrentVersion();
                            System.out.println("Version updated");
                            //launch();
                            System.out.println("Launched");
                        }
                    } else {
                        //launch();
                        System.out.println("Launched");
                    }
                }
            } else {
                LOGGER.error("Error on initialization of token");
            }
	}
        
        private static void loadLibsForLinux64Bit() {
            try {
                System.load(LIB_PATH + "libs/linux/x64/libcastle.so.1.0.0");
                addFile(LIB_PATH + "libs/linux/x64/qtjambi-4.8.7.jar");
                addFile(LIB_PATH + "libs/linux/x64/qtjambi-native-linux64-gcc-4.8.7.jar");
            } catch(IOException e) {
                
            }
        }
        
        private static void loadLibsForLinux32Bit() {
            try {
                System.load(LIB_PATH + "libs/linux/x86/libcastle.so.1.0.0");
                addFile(LIB_PATH + "libs/linux/x86/qtjambi-4.8.7.jar");
                addFile(LIB_PATH + "libs/linux/x86/qtjambi-native-linux32-gcc-4.8.7.jar");
            } catch(IOException e) {
                
            }
        }
        
        private static void loadLibsForMacOS() {
            try {
                addFile(LIB_PATH + "libs/macos/x64/qtjambi-4.8.6.jar");
                addFile(LIB_PATH + "libs/macos/x64/qtjambi-native-macosx-gcc-4.8.6.jar");
                addFile(LIB_PATH + "libs/macos/x64/qtjambi-util.jar");
                } catch(IOException e) {
                
            }
        }
        
        private static void loadLibsForWindows64Bit() {
            try {
                System.load(LIB_PATH + "libs/windows/x64/eps2003csp11.dll");
                addFile(LIB_PATH + "libs/linux/x64/qtjambi-4.8.7.jar");
                addFile(LIB_PATH + "libs/linux/x64/qtjambi-native-win64-msvc2013x64-4.8.7.jar");
            } catch(IOException e) {
                
            }
        }
        
        private static void loadLibsForWindows32Bit() {
            try {
                System.load(LIB_PATH + "libs/windows/x86/eps2003csp11.dll");
                addFile(LIB_PATH + "libs/windows/x86/qtjambi-4.8.6.jar");
                addFile(LIB_PATH + "libs/windows/x86/qtjambi-native-win32-msvc2012-4.8.6.jar");
            } catch(IOException e) {
                
            }
        }
        
        private static void updateCurrentVersion() {
            try {
                File ver = new File(detectFlashPath() + "version.fs");
                if(ver != null) {
                    ver.delete();
                    ver.createNewFile();
                    FileWriter fwriter = new FileWriter(ver);
                    fwriter.write(getActualVersion());
                    fwriter.close();
                }
            } catch(IOException e) {
                LOGGER.error("Error on update current version code to actual version code", e);
            }
        }
        
        private static void launch() {
            try {
                addFile(new File(detectFlashPath() + "ui-client.jar"));
                Class.forName("ru.flashsafe.client.Main").getMethod("launch", null).invoke(null, null);
            } catch(IOException | NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Error on adding ui library", e);
            }
        }
	
        private static boolean initToken() {
            TokenUtil.PKCS11Initialize();
            long[] slots = TokenUtil.getSlotList();
            if(slots.length > 0) {
                slot = slots[0];
                TokenUtil.closeAllSessions(slot);
                session = TokenUtil.openSession(slot);
                TokenUtil.login(session, "12345678");
                return true;
            }
            return false;
        }
        
        private static boolean checkInternet() {
            try {
                InetAddress add = InetAddress.getByName("api.flash.so");
                return add.isReachable(3000);
            } catch(IOException e) {
                LOGGER.error("Error on check Intenet connection", e);
            }
            return false;
        }
        
        private static boolean checkUpdate() {
            String flashPath = detectFlashPath();
            if(flashPath != null) {
                String curVersion = getCurrentVersion(flashPath);
                String actVersion = getActualVersion();
                if(curVersion != null && actVersion != null) {
                    return !curVersion.trim().equalsIgnoreCase(actVersion.trim());
                }
            }
            return false;
        }
        
        private static String getSN() {
            CK_TOKEN_INFO tokenInfo = TokenUtil.getTokenInfo(slot);
            return new String(tokenInfo.serialNumber);
        }
        
        private static String getSecret() {
            TokenUtil.findObjectsInit(session, new CKA(CKA.CLASS, CKO.PUBLIC_KEY));
            long[] objects = TokenUtil.findObjects(session, 1);
            TokenUtil.findObjectsFinal(session);
            CKA publicValue = TokenUtil.getAttributeValue(session, objects[0], CKA.MODULUS);
            return MD5(publicValue.getValue());
        }
        
        private static String MD5(byte[] md5) {
            try {
                 java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                 byte[] array = md.digest(md5);
                 StringBuffer sb = new StringBuffer();
                 for (int i = 0; i < array.length; ++i) {
                   sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
                }
                 return sb.toString();
             } catch (java.security.NoSuchAlgorithmException e) {
                 LOGGER.error("Error on generate MD5", e);
             }
            return null;
        }
        
        private static String getCurrentVersion(String path) {
            try {
                File ver = new File(path + "version.fs");
                FileReader freader = new FileReader(ver);
                StringBuilder sb = new StringBuilder();
                int b;
                while((b = freader.read()) != -1) sb.append((char) b);
                freader.close();
                return sb.toString();
            } catch(IOException e) {
                LOGGER.error("Error on load current version: ", e);
            }
            return null;
        }
        
        private static String detectFlashPath() {
            List<USBStorageDevice> removableDevices = new USBDeviceDetectorManager().getRemovableDevices();
            for(USBStorageDevice device : removableDevices) {
                if(device.getSystemDisplayName().contains("FLASHSAFE")) {
                    return device.getRootDirectory().getAbsolutePath();
                }
            }
            return null;
        }
        
	private static boolean update() {
		try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("http://api.flash.so/update.php?dsn=" + getSN() + "&token=" + getSecret()).openConnection();
                    conn.setRequestMethod("GET");
                    conn.connect();
                    InputStream in = conn.getInputStream();
                    File update = new File(detectFlashPath() + "ui-client-new.jar");
                    update.createNewFile();
                    OutputStream out = new FileOutputStream(update);
                    int b;
                    while((b = in.read()) != -1) out.write((byte) b);
                    in.close();
                    out.close();
                    conn.disconnect();
                    new File(detectFlashPath() + "ui-client.jar").delete();
                    update.renameTo(new File(detectFlashPath() + "ui-client.jar"));
                    return true;
		} catch(IOException e) {
                    LOGGER.error("Error on update", e);
                }
                return false;
	}
	
	private static String getActualVersion() {
		try {
                    HttpURLConnection conn = (HttpURLConnection) new URL("http://api.flash.so/api.php?dsn=" + getSN() + "&token=" + getSecret() + "&method=GetLastVersion").openConnection();
                    conn.setRequestMethod("GET");
                    conn.connect();
                    InputStream in = conn.getInputStream();
                    StringBuilder sb = new StringBuilder();
                    int b;
                    while((b = in.read()) != -1) sb.append((char) b);
                    in.close();
                    conn.disconnect();
                    return sb.toString().replace("{\"status\":\"success\",\"response\":\"", "").replace("\"}", "");
		} catch(IOException e) {
                    LOGGER.error("Error on getting actual version", e);
                    return null;
		}
	}
	
	public static void addFile(String s) throws IOException {
		   File f = new File(s);
		   addFile(f);
	}

	public static void addFile(File f) throws IOException {
	   addURL(f.toURL());
	}

	
	public static void addURL(URL u) throws IOException {
		  URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		  Class sysclass = URLClassLoader.class;
		  try {
		     Method method = sysclass.getDeclaredMethod("addURL", parameters);
		     method.setAccessible(true);
		     method.invoke(sysloader, new Object[]{u});
		  } catch (Throwable t) {
		     t.printStackTrace();
		     throw new IOException("Error, could not add URL to system classloader");
		  }
	}
        
        private static boolean is64BitSystem() {
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            if(arch.endsWith("64") || (wow64Arch != null && wow64Arch.endsWith("64"))) {
                return true;
            }
            return false;
        }
        
        private static boolean is86BitSystem() {
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            if(arch.endsWith("86") || (wow64Arch != null && wow64Arch.endsWith("86"))) {
                return true;
            }
            return false;
        }

}
