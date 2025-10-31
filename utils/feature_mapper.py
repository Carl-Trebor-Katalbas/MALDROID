import numpy as np
from androguard.misc import AnalyzeAPK
from fuzzywuzzy import fuzz

def feature_extraction(apk_path, most_relevant_features):
    """
    Extract and map APK features to the model's relevant feature vector.
    Returns a numpy array [1 x len(most_relevant_features)]
    """

    a, d, dx = AnalyzeAPK(apk_path)
    apk_permissions = a.get_permissions()
    apk_activities = a.get_activities()
    apk_services = a.get_services()
    apk_receivers = a.get_receivers()
    apk_providers = a.get_providers()
    apk_features = a.get_features()

    all_app_features = apk_permissions + apk_activities + apk_services + apk_receivers + apk_providers + apk_features

    # Extract intent filters
    intents = []
    for comp_type in ["activity", "service", "receiver", "provider"]:
        for comp in getattr(a, f"get_{comp_type}s")():
            filters = a.get_intent_filters(comp_type, comp)
            for f in filters.values():
                for items in f.values():
                    intents.extend(items)

    all_app_features += intents

    extraction_result = []
    for feature in most_relevant_features:
        match_found = any(
            fuzz.partial_ratio(feature, app_feat) >= 90 or feature in app_feat
            for app_feat in all_app_features
        )
        extraction_result.append(1 if match_found else 0)

    return np.array(extraction_result).reshape(1, -1)
