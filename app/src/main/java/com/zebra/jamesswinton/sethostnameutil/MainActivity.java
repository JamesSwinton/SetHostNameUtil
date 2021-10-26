package com.zebra.jamesswinton.sethostnameutil;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.zebra.jamesswinton.sethostnameutil.consts.Xml;
import com.zebra.jamesswinton.sethostnameutil.profilemanager.ProcessProfileAsync;
import com.zebra.jamesswinton.sethostnameutil.profilemanager.ProcessProfileAsync.OnProfileApplied;
import com.zebra.jamesswinton.sethostnameutil.profilemanager.XmlParsingError;
import com.zebra.jamesswinton.sethostnameutil.utils.CustomDialog;
import com.zebra.jamesswinton.sethostnameutil.utils.RetrieveOemInfo;
import com.zebra.jamesswinton.sethostnameutil.utils.RetrieveOemInfo.OnOemInfoRetrievedListener;
import java.util.Map;
import org.apache.commons.codec.DecoderException;

public class MainActivity extends AppCompatActivity implements OnOemInfoRetrievedListener,
    EMDKListener {

  // UI
  private AlertDialog mProgressDialog;

  // Xml
  private Xml mXml;

  // Private Variables
  private EMDKManager mEmdkManager = null;
  private ProfileManager mProfileManager = null;

  // Content URIs
  private static final String SERIAL_URI = "content://oem_info/oem.zebra.secure/build_serial";
  private static final Uri[] CONTENT_PROVIDER_URIS = {
      Uri.parse(SERIAL_URI)
  };

  /**
   * Lifecycle Callbacks
   */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Init XML
    EMDKResults emdkManagerResults = EMDKManager.getEMDKManager(this, this);
    if (emdkManagerResults == null || emdkManagerResults.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
      Toast.makeText(MainActivity.this,"Could not obtain EMDKManager",
          Toast.LENGTH_LONG).show();
      finish();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // Release EMDK Manager Instance
    if (mEmdkManager != null) {
      mEmdkManager.release();
      mEmdkManager = null;
    }

    // Remove Progress
    if (mProgressDialog != null && mProgressDialog.isShowing()) {
      mProgressDialog.dismiss();
    }
  }

  private void showXmlParsingError(XmlParsingError... parsingErrors) {
    StringBuilder errsConcat = new StringBuilder("Error Processing XML!\n\n");
    for (int i = 0; i < parsingErrors.length; i++) {
      XmlParsingError err = parsingErrors[i];
      errsConcat.append(String.format("Error %1$s/%2$s", i + 1, parsingErrors.length));
      errsConcat.append("\n\n");
      errsConcat.append(String.format("Type: %1$s", err.getType()));
      errsConcat.append("\n");
      errsConcat.append(String.format("Desc: %1$s", err.getDescription()));
      errsConcat.append("\n\n");
    } Toast.makeText(this, errsConcat, Toast.LENGTH_LONG).show();
  }

  /**
   * OEM Identifiers Permissions
   */

  private void grantOemIdentifierPermissions() {
    // Init Progress Dialog
    mProgressDialog = CustomDialog.buildLoadingDialog(this,
        "Applying MX XML to Grant Permissions...", false);
    mProgressDialog.show();

    // Get Xml
    String serialPermissionXml;
    try {
      serialPermissionXml = mXml.getSerialPermissionXml();
    } catch (NameNotFoundException | DecoderException e) {
      e.printStackTrace();
      Toast.makeText(this, "Could not grant device serial permission"
          + " - could not read package signature", Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    // Process Xml
    new ProcessProfileAsync(Xml.GrantSerialPermissionProfileName, mProfileManager,
        new OnProfileApplied() {
          @Override
          public void profileApplied(String xml, EMDKResults emdkResults) {
            // Dismiss Dialog
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
              mProgressDialog.dismiss();
            }

            // Attempt to get Serial & IMEI from Content Providers
            new RetrieveOemInfo(MainActivity.this, CONTENT_PROVIDER_URIS,
                MainActivity.this).execute();
          }

          @Override
          public void profileError(XmlParsingError... parsingErrors) {
            // Dismiss Dialog
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
              mProgressDialog.dismiss();
            }

            // Display Error
            showXmlParsingError(parsingErrors);
            finish();
          }
        })
        .execute(serialPermissionXml);
  }

  private void setHostName(String buildSerial) {
    // Init Progress Dialog
    mProgressDialog = CustomDialog.buildLoadingDialog(this,
        "Applying MX XML to set Host Name...", false);
    mProgressDialog.show();

    // Get Xml
    String serialNumber = Build.MODEL + "_" + buildSerial;
    String hostNameXml = mXml.getHostNameXml(serialNumber);

    // Process Xml
    new ProcessProfileAsync(Xml.SetCustomHostNameProfileName, mProfileManager,
        new OnProfileApplied() {
          @Override
          public void profileApplied(String xml, EMDKResults emdkResults) {
            // Dismiss Dialog
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
              mProgressDialog.dismiss();
            }

            Toast.makeText(MainActivity.this, "Host Name Set", Toast.LENGTH_LONG).show();
            finish();
          }

          @Override
          public void profileError(XmlParsingError... parsingErrors) {
            // Dismiss Dialog
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
              mProgressDialog.dismiss();
            }

            showXmlParsingError(parsingErrors);
            finish();
          }
        })
        .execute(hostNameXml);
  }

  /**
   * OEMIdentifier Callbacks
   */

  @Override
  public void onDetailsRetrieved(Map<String, String> oemIdentifiers) {
    setHostName(oemIdentifiers.get("build_serial"));
  }

  @Override
  public void onPermissionError(String e) {
    grantOemIdentifierPermissions();
  }

  @Override
  public void onUnknownError(String e) {
    Log.e(this.getClass().getName(), "Unknown Error: " + e);
    Toast.makeText(this, getString(R.string.unknown_error_dialog_title), Toast.LENGTH_LONG)
        .show();
    finish();
  }

  /**
   * EMDK Callbacks
   */

  @Override
  public void onOpened(EMDKManager emdkManager) {
    mEmdkManager = emdkManager;
    mProfileManager = (ProfileManager) mEmdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);

    // Apply Profile
    if (mProfileManager != null) {
      mXml = new Xml(this);
      new RetrieveOemInfo(this, CONTENT_PROVIDER_URIS, this).execute();
    } else {
      Log.e(this.getClass().getName(), "Error Obtaining ProfileManager!");
      Toast.makeText(this, "Error Obtaining ProfileManager!", Toast.LENGTH_LONG)
          .show();
      finish();
    }
  }

  @Override
  public void onClosed() {
    // Release EMDK Manager Instance
    if (mEmdkManager != null) {
      mEmdkManager.release();
      mEmdkManager = null;
    }
  }
}