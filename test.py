from onnx_tf.backend import prepare
import onnx

# Load the ONNX model
onnx_model = onnx.load("models/malroid_rf_model.onnx")

# Convert ONNX -> TensorFlow
tf_rep = prepare(onnx_model)

# Export as TensorFlow SavedModel
tf_rep.export_graph("models/malroid_tf_model")