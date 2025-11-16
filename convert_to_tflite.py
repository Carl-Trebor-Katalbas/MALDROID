import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import LinearSVC
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split, RandomizedSearchCV
from imblearn.over_sampling import RandomOverSampler
from mrmr import mrmr_classif
import tensorflow as tf
import joblib
import os

def create_simple_neural_network(X_train, y_train, feature_names, output_path):
    """
    Create and train a simple neural network that can be converted to TFLite
    """
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(64, activation='relu', input_shape=(len(feature_names),)),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(2, activation='softmax')  
    ])
    
    model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    history = model.fit(
        X_train, y_train,
        epochs=50,
        batch_size=32,
        validation_split=0.2,
        verbose=1
    )
    
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    tflite_model = converter.convert()
    
    # Save the model
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"Neural Network TFLite model saved to: {output_path}")
    return model, tflite_model

def create_mobile_optimized_model(X_train, y_train, feature_names, output_path):
    """Create a smaller, mobile-optimized model"""
    print(f"Creating mobile-optimized model with {len(feature_names)} features...")
    
    mobile_model = tf.keras.Sequential([
        tf.keras.layers.Dense(32, activation='relu', input_shape=(len(feature_names),)),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(2, activation='softmax')
    ])
    
    mobile_model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    
    # Train quickly
    mobile_model.fit(
        X_train, y_train,
        epochs=30,
        batch_size=32,
        validation_split=0.2,
        verbose=1
    )
    
    # Convert with size optimization
    converter = tf.lite.TFLiteConverter.from_keras_model(mobile_model)
    converter.optimizations = [tf.lite.Optimize.OPTIMIZE_FOR_SIZE]
    
    mobile_tflite = converter.convert()
    
    with open(output_path, 'wb') as f:
        f.write(mobile_tflite)
    
    print(f"Mobile-optimized TFLite model saved to: {output_path}")
    return mobile_model

def inspect_dataset(df, dataset_name):
    """Inspect the dataset to understand its structure"""
    print(f"\n=== {dataset_name} Dataset Inspection ===")
    print(f"Shape: {df.shape}")
    print(f"Columns: {list(df.columns)}")
    print(f"First few rows:")
    print(df.head(2))
    print(f"Column types:\n{df.dtypes}")

def main():
    # Create models directory
    os.makedirs("models", exist_ok=True)
    
    # Load and prepare your data
    print("Loading datasets...")
    
    # Load Drebin dataset
    print("Loading Drebin dataset...")
    drebin = pd.read_csv("./datasets/DREBIN Dataset - APK signatures.csv", low_memory=False)
    drebin['class'] = drebin['class'].replace({'S': 1, 'B': 0}).astype(int)
    drebin.replace('?', np.nan, inplace=True)
    drebin = drebin.dropna()
    
    # Inspect Drebin dataset
    inspect_dataset(drebin, "Drebin")
    
    # Load AM dataset
    print("\nLoading AM dataset...")
    am = pd.read_csv("./datasets/AM.csv", sep=";")
    
    # Inspect AM dataset first to see what columns it has
    inspect_dataset(am, "AM Raw")
    
    # Check what the target column is called in AM dataset
    target_column = None
    possible_target_columns = ['class', 'label', 'malware', 'type', 'Category', 'target']
    
    for col in possible_target_columns:
        if col in am.columns:
            target_column = col
            print(f"Found target column: {target_column}")
            break
    
    if target_column is None:
        print("Warning: No target column found. Using first column as target.")
        target_column = am.columns[0]
        print(f"Using '{target_column}' as target column")
    
    # Map the target values - check what values exist
    print(f"Unique values in {target_column}: {am[target_column].unique()}")
    
    # Map to binary (adjust based on what values you see)
    unique_vals = am[target_column].unique()
    if len(unique_vals) == 2:
        # Try common malware/benign mappings
        if any('mal' in str(val).lower() for val in unique_vals) and any('ben' in str(val).lower() for val in unique_vals):
            for val in unique_vals:
                if 'mal' in str(val).lower():
                    malicious_val = val
                if 'ben' in str(val).lower():
                    benign_val = val
            am['class'] = am[target_column].replace({benign_val: 0, malicious_val: 1})
            print(f"Mapped {benign_val} -> 0 (benign), {malicious_val} -> 1 (malicious)")
        else:
            # Assume first value is benign, second is malicious
            am['class'] = am[target_column].replace({unique_vals[0]: 0, unique_vals[1]: 1})
            print(f"Mapped {unique_vals[0]} -> 0 (benign), {unique_vals[1]} -> 1 (malicious)")
    else:
        # If more than 2 values, use the most common as benign
        print("Warning: More than 2 unique values in target column")
        most_common = am[target_column].mode()[0]
        am['class'] = (am[target_column] != most_common).astype(int)
        print(f"Mapped {most_common} -> 0 (benign), others -> 1 (malicious)")
    
    # Clean AM dataset columns
    am_cols = [
        "name", ".//MD5", "version", ".//Min_SDK", ".//Min_Screen", ".//Min_OpenGL", 
        ".//Supported_CPU", ".//Signature", ".//Developer", ".//Organization", 
        ".//Locality", ".//Country", ".//State", "description", target_column
    ]
    
    # Only drop columns that exist
    cols_to_drop = [c for c in am_cols if c in am.columns]
    am = am.drop(columns=cols_to_drop)
    
    am.columns = am.columns.str.strip().str.replace(' ', '_').str.lower()
    cols = [c for c in am.columns if c not in ['class']]
    am_cleaned = am[cols + ['class']]
    
    # Inspect cleaned AM dataset
    inspect_dataset(am_cleaned, "AM Cleaned")
    
    # Combine datasets
    print("\nCombining datasets...")
    combined = pd.concat([drebin, am_cleaned], ignore_index=True)
    combined = combined.fillna(0)
    
    # Inspect combined dataset
    inspect_dataset(combined, "Combined")
    
    # Check if we have the 'class' column
    if 'class' not in combined.columns:
        print("ERROR: 'class' column not found in combined dataset!")
        print("Available columns:", list(combined.columns))
        return
    
    # Prepare features and target
    X_combined = combined.drop(columns=['class'], errors='ignore')
    y_combined = combined['class']
    
    print(f"\nFinal dataset shape: {X_combined.shape}")
    print(f"Class distribution: {y_combined.value_counts()}")
    
    # Split and select features
    X_train, X_test, y_train, y_test = train_test_split(
        X_combined, y_combined, test_size=0.3, random_state=42, stratify=y_combined, shuffle=True
    )

    print(f"Training set shape: {X_train.shape}")
    print(f"Test set shape: {X_test.shape}")

    # Feature selection
    print("\nPerforming feature selection with mRMR...")
    try:
        selected_features = mrmr_classif(X=X_train, y=y_train, K=70, n_jobs=-1, show_progress=True)
        print(f"Selected {len(selected_features)} features")
        print("First 10 features:", selected_features[:10])
    except Exception as e:
        print(f"mRMR failed: {e}")
        print("Using first 70 features instead...")
        selected_features = X_train.columns.tolist()[:70]
    
    # Balance and scale
    ros = RandomOverSampler(random_state=42)
    X_bal, y_bal = ros.fit_resample(X_train[selected_features], y_train)
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_bal)
    X_test_scaled = scaler.transform(X_test[selected_features])
    
    # Save the scaler and feature names for later use in Android
    joblib.dump(scaler, 'models/scaler.pkl')
    joblib.dump(selected_features, 'models/selected_features.pkl')
    
    print("Training models and creating TFLite files...")
    
    # Option 1: Create Neural Network TFLite model (Recommended)
    print("\n1. Creating Neural Network TFLite model...")
    nn_model, tflite_model = create_simple_neural_network(
        X_train_scaled, y_bal, selected_features, "models/maldroid_model.tflite"
    )
    
    # Evaluate the neural network
    nn_test_loss, nn_test_accuracy = nn_model.evaluate(X_test_scaled, y_test, verbose=0)
    print(f"Neural Network Test Accuracy: {nn_test_accuracy:.4f}")
    
    # Option 2: Create Mobile-optimized model
    print("\n2. Creating Mobile-optimized Neural Network...")
    mobile_model = create_mobile_optimized_model(
        X_train_scaled, y_bal, selected_features, "models/maldroid_mobile_model.tflite"
    )
    
    # Evaluate mobile model
    mobile_test_loss, mobile_test_accuracy = mobile_model.evaluate(X_test_scaled, y_test, verbose=0)
    print(f"Mobile Model Test Accuracy: {mobile_test_accuracy:.4f}")
    
    # Option 3: Train and save scikit-learn models (for reference, not TFLite)
    print("\n3. Training scikit-learn models for reference...")
    
    # Random Forest
    rf_model = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        random_state=42
    )
    rf_model.fit(X_train_scaled, y_bal)
    rf_accuracy = rf_model.score(X_test_scaled, y_test)
    print(f"Random Forest Test Accuracy: {rf_accuracy:.4f}")
    joblib.dump(rf_model, 'models/random_forest_model.pkl')
    
    # Save feature information for Android app
    feature_info = {
        'feature_names': selected_features,
        'input_size': len(selected_features),
        'num_classes': 2,
        'class_names': ['benign', 'malicious']
    }
    joblib.dump(feature_info, 'models/feature_info.pkl')
    
    # Test TFLite model functionality
    print("\n4. Testing TFLite model...")
    test_tflite_model("models/maldroid_model.tflite", X_test_scaled, y_test)
    
    print("\n" + "="*60)
    print("CONVERSION COMPLETED SUCCESSFULLY!")
    print("="*60)
    print("Generated files in 'models/' directory:")
    print("  📱 TFLite Models (for Android):")
    print("     ├── maldroid_model.tflite      (Main model)")
    print("     └── maldroid_mobile_model.tflite (Optimized for mobile)")
    print("  🔧 Support Files:")
    print("     ├── scaler.pkl                 (Feature scaler)")
    print("     ├── selected_features.pkl      (70 selected features)")
    print("     ├── feature_info.pkl           (Model information)")
    print("     └── random_forest_model.pkl    (Reference scikit-learn model)")
    print("\n📱 Android Integration:")
    print("  - Copy *.tflite files to: app/src/main/assets/")
    print("  - The model expects 70 input features (from mRMR selection)")
    print("  - Output: [benign_probability, malicious_probability]")

def test_tflite_model(tflite_path, X_test, y_test):
    """Test the TFLite model to ensure it works correctly"""
    try:
        # Load TFLite model
        interpreter = tf.lite.Interpreter(model_path=tflite_path)
        interpreter.allocate_tensors()
        
        # Get input and output details
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        print(f"TFLite Model Details:")
        print(f"  Input shape: {input_details[0]['shape']}")
        print(f"  Input type: {input_details[0]['dtype']}")
        print(f"  Output shape: {output_details[0]['shape']}")
        print(f"  Output type: {output_details[0]['dtype']}")
        
        # Test with a few samples
        correct_predictions = 0
        total_samples = min(50, len(X_test))
        
        for i in range(total_samples):
            # Prepare input
            input_data = X_test[i:i+1].astype(np.float32)
            interpreter.set_tensor(input_details[0]['index'], input_data)
            
            # Run inference
            interpreter.invoke()
            
            # Get prediction
            output_data = interpreter.get_tensor(output_details[0]['index'])
            predicted_class = np.argmax(output_data[0])
            
            if predicted_class == y_test.iloc[i]:
                correct_predictions += 1
        
        tflite_accuracy = correct_predictions / total_samples
        print(f"  Test Accuracy (on {total_samples} samples): {tflite_accuracy:.4f}")
        
    except Exception as e:
        print(f"  TFLite test failed: {e}")

if __name__ == "__main__":
    main()