/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lordkbx.FileHash;

import android.app.Activity;
import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.Config;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaHttpAuthHandler;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/**
 *
 * @author LordKBX, vgavro
 */
public class FileHash extends CordovaPlugin {
  public boolean execute(String action, JSONArray args,
                         CallbackContext callbackContext) throws JSONException {
    String url = args.getString(0);
    url = URLDecoder.decode(url);
    JSONObject r = new JSONObject();
    String ealgo = "";
    if (action.equals("md2")) ealgo = "MD2";
    else if (action.equals("md5")) ealgo = "MD5";
    else if (action.equals("sha1")) ealgo = "SHA-1";
    else if (action.equals("sha256")) ealgo = "SHA-256";
    else if (action.equals("sha384")) ealgo = "SHA-384";
    else if (action.equals("sha512")) ealgo = "SHA-512";
    else {
      r.put("code", 1);
      r.put("message", "Unknown Algorithm");
      callbackContext.error(r);
      return true;
    }
    if (url.contains("file:///android_asset/")) {
      // LordKBX version copied file, we should omit it with error
      r.put("code", 3);
      r.put("message", "File access error");
      callbackContext.error(r);
      return true;
    }
    url = url.replace("file:///", "/").replace("file:", "");

    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        FileInputStream fis;
        JSONObject r = new JSONObject();
        try {
          fis = new FileInputStream(url);
          MessageDigest md = MessageDigest.getInstance(algo);
          byte[] dataBytes = new byte[1024];
          int nread = 0;
          while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
          };
          byte[] mdbytes = md.digest();
          // convert the byte to hex format method 1
          StringBuffer sb = new StringBuffer();
          for (int i = 0; i < mdbytes.length; i++) {
            sb.append(
                Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
          }
          r.put("file", url);
          r.put("algo", ealgo);
          r.put("result", sb.toString());
          callbackContext.success(r);
        } catch (Exception ex) {
          if (ex instanceof FileNotFoundException) {
            File f = new File(url);
            if (f.exists()) {
              r.put("code", 3);
              r.put("message", "File access error");
            } else {
              r.put("code", 2);
              r.put("message", "File not found");
            }
          } else {
            r.put("code", 4);
            r.put("message", "Digest error");
          }
          callbackContext.error(r);
        }
      }
    });
    return true;
  }
}
