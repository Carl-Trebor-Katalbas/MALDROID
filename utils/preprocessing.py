import joblib

class Preprocessor:
    def __init__(self, scaler_path, pca_path=None):
        self.scaler = joblib.load(scaler_path)
        self.pca = joblib.load(pca_path) if pca_path else None

    def transform(self, X):
        X_scaled = self.scaler.transform(X)
        if self.pca:
            X_scaled = self.pca.transform(X_scaled)
        return X_scaled
