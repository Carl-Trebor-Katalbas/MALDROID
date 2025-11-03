from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
from utils.feature_mapper import feature_extraction
from utils.preprocessing import Preprocessor
import os
import joblib
import traceback

app = Flask(__name__)
CORS(app)

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

preprocessor = Preprocessor(
    scaler_path="model/malroid_scaler.pkl",
    pca_path="model/malroid_pca.pkl"
)
model = joblib.load("model/malroid_knn_model.pkl")

FEATURE_COLUMNS = [
    "SEND_SMS", "android.content.pm.Signature", "ACCESS_LOCATION_EXTRA_COMMANDS",
    "com.android.launcher.permission.uninstall_shortcut", "android.permission.read_sync_settings",
    "android.telephony.SmsManager", "transact", "android.permission.authenticate_accounts",
    "READ_SMS", "attachInterface", "android.permission.write_sync_settings",
    "WRITE_HISTORY_BOOKMARKS", "RECEIVE_SMS", "android.permission.manage_accounts",
    "onServiceConnected", "com.android.launcher.permission.install_shortcut", "bindService",
    "android.telephony.gsm.SmsManager", "android.permission.read_logs", "ServiceConnection",
    "android.permission.use_credentials", "INSTALL_PACKAGES", "TelephonyManager.getLine1Number",
    "android.os.Binder", "createSubprocess", "Ljava.lang.Class.getMethods",
    "READ_HISTORY_BOOKMARKS", "Ljava.lang.Class.getCanonicalName", "WRITE_SMS",
    "Ljava.lang.Class.cast", "READ_PHONE_STATE", "android.permission.get_accounts",
    "getBinder", "android.intent.action.BOOT_COMPLETED", "Ljava.net.URLDecoder",
    "TelephonyManager.getSubscriberId", "GET_ACCOUNTS", "WRITE_APN_SETTINGS", "getCallingUid",
    "android.permission.bluetooth", "USE_CREDENTIALS", "MANAGE_ACCOUNTS", "chown",
    "Landroid.content.Context.unregisterReceiver", "Ljava.lang.Class.getField",
    "android.permission.read_phone_state", "android.intent.action.SEND", "abortBroadcast",
    "UPDATE_DEVICE_STATS", "android.permission.access_coarse_location",
    "Landroid.content.Context.registerReceiver", "sendDataMessage", "RESTART_PACKAGES",
    "android.permission.vibrate", "READ_SYNC_SETTINGS", "ClassLoader", "DELETE_CACHE_FILES",
    "DexClassLoader", "chmod", "Ljava.lang.Class.getDeclaredField",
    "android.permission.access_fine_location", "android.permission.bluetooth_admin",
    "AUTHENTICATE_ACCOUNTS", "android.intent.action.PACKAGE_REPLACED",
    "Ljavax.crypto.spec.SecretKeySpec", "DELETE_PACKAGES", "BIND_WALLPAPER",
    "URLClassLoader", "remount", "android.intent.action.TIME_SET"
]

# For testing
FEATURE_COLUMNS_30 = [
    "SEND_SMS", "android.content.pm.Signature", "ACCESS_LOCATION_EXTRA_COMMANDS",
    "com.android.launcher.permission.uninstall_shortcut", "android.permission.read_sync_settings",
    "android.telephony.SmsManager", "transact", "android.permission.authenticate_accounts",
    "READ_SMS", "attachInterface", "android.permission.write_sync_settings",
    "WRITE_HISTORY_BOOKMARKS", "RECEIVE_SMS", "android.permission.manage_accounts",
    "onServiceConnected", "com.android.launcher.permission.install_shortcut", "bindService",
    "android.telephony.gsm.SmsManager", "android.permission.read_logs", "ServiceConnection",
    "android.permission.use_credentials", "INSTALL_PACKAGES", "TelephonyManager.getLine1Number",
    "android.os.Binder", "createSubprocess", "Ljava.lang.Class.getMethods",
    "READ_HISTORY_BOOKMARKS", "Ljava.lang.Class.getCanonicalName", "WRITE_SMS",
    "Ljava.lang.Class.cast"
]

@app.route('/')
def home():
    return jsonify({"status": "MALDROID backend is running"})

# Scan App
@app.route('/analyze', methods=['POST'])
def analyze():
    apk_path = None
    try:
        if 'apkFile' not in request.files:
            return jsonify({"error": "No APK file uploaded"}), 400

        apk_file = request.files['apkFile']
        
        if not apk_file.filename.endswith(".apk"):
            return jsonify({"error": "Only APK files are supported"}), 400
        
        apk_path = os.path.join(UPLOAD_DIR, apk_file.filename)
        apk_file.save(apk_path)
        print(f"Received APK: {apk_file.filename}")

        print("Extracting features from APK...")
        feature_vector = feature_extraction(apk_path, FEATURE_COLUMNS) 
        print("Feature extraction complete, shape:", feature_vector.shape)

        df = pd.DataFrame(feature_vector, columns=FEATURE_COLUMNS)
        X_processed = preprocessor.transform(df)

        prediction = model.predict(X_processed)[0]
        confidence = float(max(model.predict_proba(X_processed)[0])) if hasattr(model, "predict_proba") else 0.0
        label = "Malicious" if prediction == 1 else "Benign"

        print(f"Prediction: {label} (Confidence: {confidence:.4f})")

        return jsonify({
            "prediction": int(prediction),
            "label": label,
            "confidence": confidence
        })

    except Exception as e:
        print("Error during analysis:", e)
        traceback.print_exc()
        return jsonify({"error": str(e)}), 400
    
    finally:
        if apk_path and os.path.exists(apk_path):
            os.remove(apk_path)
    
# Scan All
@app.route('/analyze_batch', methods=['POST'])
def analyze_batch():
    results = []
    try:
        apk_files = request.files.getlist('apkFiles')
        if not apk_files:
            return jsonify({"error": "No APK files uploaded"}), 400

        for apk_file in apk_files:
            apk_path = os.path.join(UPLOAD_DIR, apk_file.filename)
            apk_file.save(apk_path)

            try:
                feature_vector = feature_extraction(apk_path, FEATURE_COLUMNS)
                df = pd.DataFrame(feature_vector, columns=FEATURE_COLUMNS)
                X_processed = preprocessor.transform(df)

                prediction = model.predict(X_processed)[0]
                confidence = float(max(model.predict_proba(X_processed)[0]))
                label = "Malicious" if prediction == 1 else "Benign"

                results.append({
                    "file": apk_file.filename,
                    "size_kb": round(os.path.getsize(apk_path) / 1024, 2),
                    "prediction": int(prediction),
                    "label": label,
                    "confidence": confidence
                })

            except Exception as e:
                print(f"Error analyzing {apk_file.filename}: {e}")
                results.append({
                    "file": apk_file.filename,
                    "error": str(e)
                })

            finally:
                if apk_path and os.path.exists(apk_path):
                    os.remove(apk_path)
                    print(f"Deleted temporary file: {apk_path}")

        return jsonify(results)

    except Exception as e:
        print("Batch analysis error:", e)
        traceback.print_exc()
        return jsonify({"error": str(e)}), 400
        

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)