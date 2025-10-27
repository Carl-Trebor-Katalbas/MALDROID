import numpy as np


def print_evaluation_metrics(evaluation_metrics):
    means = get_evaluation_metrics_means(evaluation_metrics)
    print("\n------------------ Model Evaluation Metrics ------------------\n")
    print("Accuracy: ", means[0])
    print("Confusion Matrix: ", "| TN:", means[1][0], "| FP:", means[1][1], "| FN:",
          means[1][2], "| TP:", means[1][3], "|")
    print("Precision: ", means[2])
    print("Recall: ", means[3])
    print("F1-score: ", means[4])
    print("ROC-AUC: ", means[5])
    print("\n-------------------------------------------------------------\n")

    distance_deviations = get_evaluation_metrics_distance_deviations(evaluation_metrics)

    print("\n--------------------- Distance Deviations ---------------------\n")
    print("Accuracy: ", distance_deviations[0] if len(distance_deviations) > 0 else "N/A")
    print("Confusion Matrix: ", "| TN:", distance_deviations[1] if len(distance_deviations) > 1 else "N/A",
          "| FP:", distance_deviations[2] if len(distance_deviations) > 2 else "N/A",
          "| FN:", distance_deviations[3] if len(distance_deviations) > 3 else "N/A",
          "| TP:", distance_deviations[4] if len(distance_deviations) > 4 else "N/A", "|")
    print("Precision: ", distance_deviations[5] if len(distance_deviations) > 5 else "N/A")
    print("Recall: ", distance_deviations[6] if len(distance_deviations) > 6 else "N/A")
    print("F1-score: ", distance_deviations[7] if len(distance_deviations) > 7 else "N/A")
    print("ROC-AUC: ", distance_deviations[8] if len(distance_deviations) > 8 else "N/A")
    print("\n-------------------------------------------------------------\n")



def get_evaluation_metrics_means(evaluation_metrics):
    accuracies = evaluation_metrics[0]
    confusion_matrices = evaluation_metrics[1]
    precisions = evaluation_metrics[2]
    recalls = evaluation_metrics[3]
    f1_scores = evaluation_metrics[4]
    roc_aucs = evaluation_metrics[5]

    means = [
        round(sum(accuracies) / len(accuracies), 4) * 100,
        sum(confusion_matrices) / len(confusion_matrices),
        round(sum(precisions) / len(precisions), 4) * 100,
        round(sum(recalls) / len(recalls), 4) * 100,
        round(sum(f1_scores) / len(f1_scores), 4) * 100,
        round(sum(roc_aucs) / len(roc_aucs), 4) * 100
    ]

    return means


def get_evaluation_metrics_distance_deviations(evaluation_metrics):
    accuracies = evaluation_metrics[0]
    confusion_matrices = evaluation_metrics[1]
    precisions = evaluation_metrics[2]
    recalls = evaluation_metrics[3]
    f1_scores = evaluation_metrics[4]
    roc_aucs = evaluation_metrics[5]

    cm_tn, cm_fp, cm_fn, cm_tp = [], [], [], []
    for cm in confusion_matrices:
        cm_tn.append(cm[0])
        cm_fp.append(cm[1])
        cm_fn.append(cm[2])
        cm_tp.append(cm[3])

    distance_deviations = [
        round(np.sqrt(np.var(accuracies)), 4) * 100 if accuracies else None,
        np.sqrt(np.var(cm_tn)) if cm_tn else None,
        np.sqrt(np.var(cm_fp)) if cm_fp else None,
        np.sqrt(np.var(cm_fn)) if cm_fn else None,
        np.sqrt(np.var(cm_tp)) if cm_tp else None,
        round(np.sqrt(np.var(precisions)), 4) * 100 if precisions else None,
        round(np.sqrt(np.var(recalls)), 4) * 100 if recalls else None,
        round(np.sqrt(np.var(f1_scores)), 4) * 100 if f1_scores else None,
        round(np.sqrt(np.var(roc_aucs)), 4) * 100 if roc_aucs else None
    ]

    while len(distance_deviations) < 9:
        distance_deviations.append(None)

    return distance_deviations