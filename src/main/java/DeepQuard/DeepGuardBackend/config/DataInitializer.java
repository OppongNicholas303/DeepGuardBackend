package DeepQuard.DeepGuardBackend.config;

import DeepQuard.DeepGuardBackend.model.AiModel;
import DeepQuard.DeepGuardBackend.repository.AiModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AiModelRepository aiModelRepository;

    @Override
    public void run(String... args) throws Exception {
        if (aiModelRepository.count() == 0) {
            createAiModel("DeepFake Detector", "1.0", AiModel.ModelType.IMAGE, new BigDecimal("0.95"));
            createAiModel("Video Analyzer", "1.0", AiModel.ModelType.VIDEO, new BigDecimal("0.92"));
            createAiModel("Audio Detector", "1.0", AiModel.ModelType.AUDIO, new BigDecimal("0.88"));
        }
    }

    private void createAiModel(String name, String version, AiModel.ModelType type, BigDecimal accuracy) {
        AiModel model = new AiModel();
        model.setModelName(name);
        model.setModelVersion(version);
        model.setModelType(type);
        model.setAccuracyScore(accuracy);
        model.setIsActive(true);
        aiModelRepository.save(model);
    }
}