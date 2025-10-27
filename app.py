from flask import Flask, request, jsonify
from flask_cors import CORS
import pandas as pd
import joblib

app = Flask(__name__)
CORS(app)

scaler = joblib.load("models/malroid_scaler.pkl")
pca = joblib.load("models/malroid_pca.pkl")
model = joblib.load("models/malroid_rf_model.pkl")

FEATURE_COLUMNS = ['feature1', 'feature2', 'feature3', ...]  

@app.route('/')
def home():
    return jsonify({"status": "MALDROID backend is running"})

@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.json
        df = pd.DataFrame([data], columns=FEATURE_COLUMNS)

        df_scaled = scaler.transform(df)
        df_pca = pca.transform(df_scaled)

        prediction = model.predict(df_pca)[0]
        label = "Malicious" if prediction == 1 else "Benign"

        return jsonify({"prediction": int(prediction), "label": label})

    except Exception as e:
        return jsonify({"error": str(e)}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
