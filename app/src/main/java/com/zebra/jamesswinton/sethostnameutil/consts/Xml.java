package com.zebra.jamesswinton.sethostnameutil.consts;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import com.zebra.jamesswinton.sethostnameutil.utils.PackageManagerHelper;
import org.apache.commons.codec.DecoderException;

public class Xml {

  // Profile Names
  public static final String GrantSerialPermissionProfileName = "GrantSerialPermission";
  public static final String SetCustomHostNameProfileName = "SetCustomHostName";

  // Holders
  private Context mContext;
  private String mPackageName;
  private String mPackageSignatureHex;

  public Xml(Context context) {
    this.mContext = context;
  }

  public String getHostNameXml(String hostName) {
    return
        "<wap-provisioningdoc>\n" +
        "  <characteristic type=\"Profile\">\n" +
        "    <parm name=\"ProfileName\" value=" + '"' + SetCustomHostNameProfileName + '"' + " />\n" +
        "    <characteristic type=\"HostsMgr\">\n" +
        "      <parm name=\"HostName\" value=" + '"' + hostName + '"' + " />\n" +
        "    </characteristic>\n" +
        "  </characteristic>\n" +
        "</wap-provisioningdoc>";
  }

  public String getSerialPermissionXml() throws NameNotFoundException, DecoderException {
    mPackageSignatureHex = PackageManagerHelper.getSigningCertBase64(mContext);
    mPackageName = mContext.getPackageName();
    return
        "<wap-provisioningdoc>\n" +
        "  <characteristic type=\"Profile\">\n" +
        "    <parm name=\"ProfileName\" value=" + '"' + GrantSerialPermissionProfileName + '"' + " />\n" +
        "    <characteristic version=\"8.3\" type=\"AccessMgr\">\n" +
        "      <parm name=\"OperationMode\" value=\"1\" />\n" +
        "      <parm name=\"ServiceAccessAction\" value=\"4\" />\n" +
        "      <parm name=\"ServiceIdentifier\" value=\"content://oem_info/oem.zebra.secure/build_serial\" />\n" +
        "      <parm name=\"CallerPackageName\" value=" + '"' + mPackageName + '"' + " />\n" +
        "      <parm name=\"CallerSignature\" value=" + '"' + mPackageSignatureHex + '"' + "  />\n" +
        "    </characteristic>\n" +
        "  </characteristic>\n" +
        "</wap-provisioningdoc>";
  }

}
