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
public class FileHash extends CordovaPlugin
{
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException
    {
        String url = args.getString(0);
        url = URLDecoder.decode(url);
        JSONObject r = new JSONObject();
        String ealgo;
        switch (action)
        {
            case "md2":
                ealgo = "MD2";
                break;
            case "md5":
                ealgo = "MD5";
                break;
            case "sha1":
                ealgo = "SHA-1";
                break;
            case "sha256":
                ealgo = "SHA-256";
                break;
            case "sha384":
                ealgo = "SHA-384";
                break;
            case "sha512":
                ealgo = "SHA-512";
                break;
            default:
                r.put("code", 1);
                r.put("message", "Unknown Algorithm");
                callbackContext.error(r);
                return true;
        }
        if (url.contains("file:///android_asset/"))
        {
            // LordKBX version copied file, we should omit it with error
            r.put("code", 3);
            r.put("message", "File access error");
            callbackContext.error(r);
            return true;
        }
        url = url.replace("file:///", "/").replace("file:", "");

        String finalEalgo = ealgo;
        String finalUrl = url;
        cordova.getThreadPool().execute((Runnable) () ->
        {
            FileInputStream fis;
            JSONObject r1 = new JSONObject();
            try
            {
                fis = new FileInputStream(finalUrl);
                MessageDigest md = MessageDigest.getInstance(finalEalgo);
                byte[] dataBytes = new byte[1024];
                int nread;
                while ((nread = fis.read(dataBytes)) != -1)
                {
                    md.update(dataBytes, 0, nread);
                }

                byte[] mdbytes = md.digest();
                // convert the byte to hex format method 1
                StringBuffer sb = new StringBuffer();
                for (byte mdbyte : mdbytes)
                {
                    sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
                }
                r1.put("file", finalUrl);
                r1.put("algo", finalEalgo);
                r1.put("result", sb.toString());
                callbackContext.success(r1);
            }
            catch (Exception ex)
            {
                try
                {
                    if (ex instanceof FileNotFoundException)
                    {
                        File f = new File(finalUrl);
                        if (f.exists())
                        {
                            r1.put("code", 3);
                            r1.put("message", "File access error");
                        }
                        else
                        {
                            r1.put("code", 2);
                            r1.put("message", "File not found");
                        }
                    }
                    else
                    {
                        r1.put("code", 4);
                        r1.put("message", "Digest error");
                    }
                    callbackContext.error(r1);
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }
}
