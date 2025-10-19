package DeepQuard.DeepGuardBackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
public class FileValidationUtil {

    @Value("${app.max-file-size-mb:100}")
    private int maxFileSizeMb;

    @Value("${app.supported-formats:jpg,jpeg,png,pdf,webp,bmp}")
    private String supportedFormats;

    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        long maxSizeBytes = maxFileSizeMb * 1024L * 1024L;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed size of %dMB", maxFileSizeMb));
        }

        // Check file format
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Filename is required");
        }

        String extension = getFileExtension(filename).toLowerCase();
        List<String> allowedFormats = Arrays.asList(supportedFormats.split(","));

        if (!allowedFormats.contains(extension)) {
            throw new IllegalArgumentException(
                    String.format("Unsupported file format: %s. Supported formats: %s",
                            extension, supportedFormats));
        }

        // Basic MIME type validation
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Content type is required");
        }

        if (!isValidContentType(contentType, extension)) {
            throw new IllegalArgumentException(
                    String.format("Content type %s does not match file extension %s",
                            contentType, extension));
        }

        // Basic magic bytes check for security
        validateMagicBytes(file, extension);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new IllegalArgumentException("File must have an extension");
        }
        return filename.substring(lastDotIndex + 1);
    }

    private boolean isValidContentType(String contentType, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> contentType.equals("image/jpeg");
            case "png" -> contentType.equals("image/png");
            case "pdf" -> contentType.equals("application/pdf");
            case "webp" -> contentType.equals("image/webp");
            case "bmp" -> contentType.equals("image/bmp");
            default -> false;
        };
    }

    private void validateMagicBytes(MultipartFile file, String extension) {
        try {
            byte[] bytes = new byte[8];
            int bytesRead = file.getInputStream().read(bytes);

            if (bytesRead < 4) {
                throw new IllegalArgumentException("File is too small to validate");
            }

            // Check magic bytes for common formats
            switch (extension) {
                case "jpg", "jpeg":
                    if (bytes[0] != (byte) 0xFF || bytes[1] != (byte) 0xD8) {
                        throw new IllegalArgumentException("Invalid JPEG file format");
                    }
                    break;
                case "png":
                    if (bytes[0] != (byte) 0x89 || bytes[1] != 0x50 ||
                            bytes[2] != 0x4E || bytes[3] != 0x47) {
                        throw new IllegalArgumentException("Invalid PNG file format");
                    }
                    break;
                case "pdf":
                    if (bytes[0] != 0x25 || bytes[1] != 0x50 ||
                            bytes[2] != 0x44 || bytes[3] != 0x46) {
                        throw new IllegalArgumentException("Invalid PDF file format");
                    }
                    break;
                // Add more format validations as needed
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to validate file format: " + e.getMessage());
        }
    }
}