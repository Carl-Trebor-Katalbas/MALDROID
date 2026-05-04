# MALDROID

**Malware Detection in Android Applications through APK Analysis using Machine Learning**

---

## 📌 Overview

**MALDROID** is a machine learning-driven system designed to detect malicious Android applications by analyzing APK files. It leverages static feature extraction and intelligent classification techniques to identify potentially harmful apps in real time.

---

## ⚙️ Project Structure & Development Workflow

Development for MALDROID is **modularized across separate branches**, each focusing on a specific component of the system:

* **`maldroid-app` branch**
  Contains the Android application built using **Android Studio (Kotlin)**.

  * User interface for scanning APKs
  * Features: *Scan APK*, *Scan All APK*, *Recent Scans*, and settings
  * Communicates with backend via REST API (Retrofit / OkHttp)

* **`maldroid-model` branch**
  Hosts the **backend API and machine learning model**.

  * Built using **Flask**
  * Handles APK feature processing and prediction
  * Implements trained ML models (primarily Random Forest)

* **`maldroid-data` branch**
  Contains **Jupyter Notebooks** used for:

  * Dataset aggregation and preprocessing
  * Feature extraction using AndroGuard
  * Model training, evaluation, and experimentation

---

## 🧠 Key Features

* 📱 Real-time APK scanning via Android app
* 🤖 Machine learning-based malware detection
* 🔍 Feature extraction from APKs (permissions, API calls, etc.)
* 📊 Model comparison (Random Forest, KNN, SVM)
* 🔗 RESTful API integration between app and backend

---

## 🗂️ Datasets Used

* Drebin
* CICAndMal2017
* AM Dataset
* AMSF Dataset

After evaluation, **Drebin and AM datasets** were selected for aggregation due to compatibility and performance.

---

## 🧪 Methodology

1. **Data Collection & Aggregation**
2. **Feature Extraction** (AndroGuard)
3. **Preprocessing & Feature Selection** (RRFS)
4. **Model Training**

   * Random Forest (Best performing)
   * K-Nearest Neighbors (KNN)
   * Support Vector Machine (SVM)
5. **Evaluation & Deployment**

---

## 📊 Results Summary

* **Random Forest (RF)** achieved the best real-world performance
* High accuracy across multiple datasets (up to ~99%)
* Strong generalization compared to KNN and SVM
* Successfully deployed in the MALDROID mobile application

⚠️ *Limitations:*

* Some false negatives in repackaged or obfuscated malware
* Heavy reliance on static analysis

---

## 📱 Application Features

* Scan individual APK files
* Scan all installed APKs
* View scan history
* Clean and minimalist UI
* Fast API-based detection

---

## 🚧 Limitations

* Limited dynamic (runtime) analysis
* Difficulty detecting highly obfuscated malware
* Limited explainability of model predictions

---

## 🚀 Future Improvements

* Integrate **dynamic/runtime analysis**
* Expand dataset with newer malware samples
* Implement **hybrid or ensemble models**
* Add malware type classification (e.g., ransomware, spyware)
* Improve explainability of predictions
* Optimize performance for low-end devices

---

## 👨‍💻 Researchers

* Carl Trebor Katalbas
* Johnneri Garcia

---

## 📄 License

This project is for academic and research purposes. Licensing details may be added in future versions.

---
