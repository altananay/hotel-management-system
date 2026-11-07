package com.hotel.media.internal.application;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaRules;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class JavaMediaImageProcessor implements MediaImageProcessor {

    private static final int[][] TARGETS = {
        {480, 320}, {1200, 800}, {1920, 1280}
    };
    private static final MediaVariantType[] VARIANTS = {
        MediaVariantType.THUMB, MediaVariantType.CARD, MediaVariantType.COVER
    };

    static {
        ImageIO.setUseCache(false);
    }

    @Override
    public ImageInfo inspect(byte[] bytes) {
        String mimeType = detectMimeType(bytes);
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                MediaRules.validateImageDimensions(width, height);
                return new ImageInfo(mimeType, width, height);
            } finally {
                reader.dispose();
            }
        } catch (DomainError ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalidImage();
        }
    }

    @Override
    public ProcessedImage process(byte[] bytes) {
        ImageInfo info = inspect(bytes);
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                throw invalidImage();
            }
            BufferedImage oriented = applyOrientation(source, readOrientation(bytes));
            BufferedImage rgb = toRgb(oriented);
            List<GeneratedVariant> variants = new ArrayList<>();
            for (int index = 0; index < VARIANTS.length; index++) {
                BufferedImage variantImage =
                        resizeCenterCrop(rgb, TARGETS[index][0], TARGETS[index][1]);
                variants.add(
                        new GeneratedVariant(
                                VARIANTS[index],
                                MediaVariantFormat.WEBP,
                                variantImage.getWidth(),
                                variantImage.getHeight(),
                                encode(variantImage, "webp")));
                variants.add(
                        new GeneratedVariant(
                                VARIANTS[index],
                                MediaVariantFormat.JPEG,
                                variantImage.getWidth(),
                                variantImage.getHeight(),
                                encode(variantImage, "jpeg")));
            }
            return new ProcessedImage(info.width(), info.height(), variants);
        } catch (DomainError ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalidImage();
        }
    }

    private String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw invalidImage();
        }
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return "image/webp";
        }
        throw new DomainError(
                MediaErrorCodes.INVALID_CONTENT_TYPE,
                "Only JPEG, PNG and WebP images are accepted.",
                DomainErrorCategory.VALIDATION);
    }

    private int readOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignored) {
            // Missing/broken EXIF is treated as normal orientation; metadata is stripped anyway.
        }
        return 1;
    }

    private BufferedImage applyOrientation(BufferedImage source, int orientation) {
        AffineTransform transform = new AffineTransform();
        int width = source.getWidth();
        int height = source.getHeight();
        switch (orientation) {
            case 2 -> {
                transform.scale(-1, 1);
                transform.translate(-width, 0);
            }
            case 3 -> {
                transform.translate(width, height);
                transform.rotate(Math.PI);
            }
            case 4 -> {
                transform.scale(1, -1);
                transform.translate(0, -height);
            }
            case 5 -> {
                transform.rotate(Math.PI / 2);
                transform.scale(1, -1);
            }
            case 6 -> {
                transform.translate(height, 0);
                transform.rotate(Math.PI / 2);
            }
            case 7 -> {
                transform.scale(-1, 1);
                transform.translate(-height, 0);
                transform.translate(0, width);
                transform.rotate(3 * Math.PI / 2);
            }
            case 8 -> {
                transform.translate(0, width);
                transform.rotate(3 * Math.PI / 2);
            }
            default -> {
                return source;
            }
        }
        int targetWidth = orientation >= 5 ? height : width;
        int targetHeight = orientation >= 5 ? width : height;
        BufferedImage result =
                new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(source, transform, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private BufferedImage toRgb(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    private BufferedImage resizeCenterCrop(BufferedImage source, int targetWidth, int targetHeight) {
        double targetRatio = (double) targetWidth / targetHeight;
        double sourceRatio = (double) source.getWidth() / source.getHeight();
        int cropWidth = source.getWidth();
        int cropHeight = source.getHeight();
        if (sourceRatio > targetRatio) {
            cropWidth = (int) Math.round(source.getHeight() * targetRatio);
        } else {
            cropHeight = (int) Math.round(source.getWidth() / targetRatio);
        }
        int cropX = (source.getWidth() - cropWidth) / 2;
        int cropY = (source.getHeight() - cropHeight) / 2;
        // Never upscale: small-but-valid images keep their native crop dimensions.
        int outputWidth = Math.min(targetWidth, cropWidth);
        int outputHeight = Math.min(targetHeight, cropHeight);
        BufferedImage target =
                new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(
                    source,
                    0,
                    0,
                    outputWidth,
                    outputHeight,
                    cropX,
                    cropY,
                    cropX + cropWidth,
                    cropY + cropHeight,
                    null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] encode(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, output)) {
            throw new IllegalStateException("No ImageIO writer for " + format);
        }
        return output.toByteArray();
    }

    private DomainError invalidImage() {
        return new DomainError(
                MediaErrorCodes.INVALID_IMAGE,
                "The uploaded image is invalid.",
                DomainErrorCategory.VALIDATION);
    }
}
