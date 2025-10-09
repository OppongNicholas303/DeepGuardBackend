package DeepQuard.DeepGuardBackend.service.auth.analyzerService;

import DeepQuard.DeepGuardBackend.model.BreachDetection;
import DeepQuard.DeepGuardBackend.model.DeepfakeAnalysis;
import DeepQuard.DeepGuardBackend.model.MediaFile;
import DeepQuard.DeepGuardBackend.model.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AnalyzerService {
    CompletableFuture<DeepfakeAnalysis> analyzeMedia(MediaFile mediaFile);
    CompletableFuture<BreachDetection> analyzeForBreaches(String content, User user);
    Map<String, Object> getAnalysisProgress(UUID analysisId);
}
