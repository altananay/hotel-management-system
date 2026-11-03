package com.hotel.media.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.hotel.media.internal.domain.MediaErrorCodes;
import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import com.hotel.sharedkernel.api.DomainError;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Unit tests for image inspection, EXIF stripping and variant generation (AC-2, AC-3, Q4). */
class JavaMediaImageProcessorTest {

    private final JavaMediaImageProcessor processor = new JavaMediaImageProcessor();

    @Test
    void shouldInspectJpegDimensionsAndDetectMimeType() throws Exception {
        byte[] jpeg = jpeg(1200, 800);

        MediaImageProcessor.ImageInfo info = processor.inspect(jpeg);

        assertThat(info.mimeType()).isEqualTo("image/jpeg");
        assertThat(info.width()).isEqualTo(1200);
        assertThat(info.height()).isEqualTo(800);
    }

    @Test
    void shouldGenerateWebpAndJpegForEveryTargetVariant() throws Exception {
        byte[] jpeg = jpeg(2400, 1600);

        MediaImageProcessor.ProcessedImage processed = processor.process(jpeg);

        assertThat(processed.width()).isEqualTo(2400);
        assertThat(processed.height()).isEqualTo(1600);
        assertThat(processed.variants()).hasSize(6);
        assertVariant(processed, MediaVariantType.THUMB, 480, 320);
        assertVariant(processed, MediaVariantType.CARD, 1200, 800);
        assertVariant(processed, MediaVariantType.COVER, 1920, 1280);

        MediaImageProcessor.GeneratedVariant webp =
                find(processed, MediaVariantType.CARD, MediaVariantFormat.WEBP);
        assertThat(new String(webp.bytes(), 0, 4)).isEqualTo("RIFF");
        assertThat(new String(webp.bytes(), 8, 4)).isEqualTo("WEBP");
        MediaImageProcessor.GeneratedVariant jpegVariant =
                find(processed, MediaVariantType.CARD, MediaVariantFormat.JPEG);
        assertThat(jpegVariant.bytes()[0] & 0xFF).isEqualTo(0xFF);
        assertThat(jpegVariant.bytes()[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    void shouldAcceptPngInputWithoutUpscalingSmallValidImages() throws Exception {
        byte[] png = png(800, 600);

        MediaImageProcessor.ProcessedImage processed = processor.process(png);

        assertThat(processed.variants()).hasSize(6);
        assertVariant(processed, MediaVariantType.THUMB, 480, 320);
        assertVariant(processed, MediaVariantType.CARD, 800, 533);
        assertVariant(processed, MediaVariantType.COVER, 800, 533);
    }

    @Test
    void shouldAcceptWebpInput_whenReaderAndWriterAreAvailable() throws Exception {
        byte[] webp = find(processor.process(jpeg(1200, 800)), MediaVariantType.CARD, MediaVariantFormat.WEBP).bytes();

        MediaImageProcessor.ImageInfo info = processor.inspect(webp);
        assertThat(info.mimeType()).isEqualTo("image/webp");

        MediaImageProcessor.ProcessedImage processed = processor.process(webp);
        assertThat(processed.variants()).hasSize(6);
    }

    @Test
    void shouldStripExifAndGpsFromGeneratedVariants() throws Exception {
        byte[] withMetadata = withExifGps(jpeg(1200, 800));

        Metadata original = ImageMetadataReader.readMetadata(new ByteArrayInputStream(withMetadata));
        assertThat(original.getDirectoriesOfType(GpsDirectory.class)).isNotEmpty();

        MediaImageProcessor.ProcessedImage processed = processor.process(withMetadata);
        // Orientation 6 rotates 1200x800 to 800x1200; 3:2 crop caps card at 800x533.
        assertVariant(processed, MediaVariantType.CARD, 800, 533);
        for (MediaImageProcessor.GeneratedVariant variant : processed.variants()) {
            if (variant.format() != MediaVariantFormat.JPEG) {
                continue;
            }
            Metadata metadata =
                    ImageMetadataReader.readMetadata(new ByteArrayInputStream(variant.bytes()));
            assertThat(metadata.getDirectoriesOfType(GpsDirectory.class)).isEmpty();
            assertThat(metadata.getDirectories()).allSatisfy(
                    directory -> assertThat(directory.getName()).doesNotContain("Exif"));
        }
    }

    @Test
    void shouldRejectSvgAndGifContent() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".getBytes();
        byte[] gif = new byte[] {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0};

        assertThatThrownBy(() -> processor.inspect(svg))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.INVALID_CONTENT_TYPE));
        assertThatThrownBy(() -> processor.inspect(gif))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.INVALID_CONTENT_TYPE));
    }

    @Test
    void shouldRejectCorruptImageWithAllowedMagicBytes() {
        byte[] corrupt = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        assertThatThrownBy(() -> processor.inspect(corrupt))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.INVALID_IMAGE));
    }

    @Test
    void shouldRejectImagesBelowMinimumDimensions() throws Exception {
        byte[] small = jpeg(640, 480);

        assertThatThrownBy(() -> processor.inspect(small))
                .isInstanceOf(DomainError.class)
                .satisfies(
                        error ->
                                assertThat(((DomainError) error).code())
                                        .isEqualTo(MediaErrorCodes.IMAGE_TOO_LARGE));
    }

    private static void assertVariant(
            MediaImageProcessor.ProcessedImage processed,
            MediaVariantType type,
            int width,
            int height) {
        MediaImageProcessor.GeneratedVariant webp = find(processed, type, MediaVariantFormat.WEBP);
        MediaImageProcessor.GeneratedVariant jpeg = find(processed, type, MediaVariantFormat.JPEG);
        assertThat(webp.width()).isEqualTo(width);
        assertThat(webp.height()).isEqualTo(height);
        assertThat(jpeg.width()).isEqualTo(width);
        assertThat(jpeg.height()).isEqualTo(height);
    }

    private static MediaImageProcessor.GeneratedVariant find(
            MediaImageProcessor.ProcessedImage processed,
            MediaVariantType type,
            MediaVariantFormat format) {
        return processed.variants().stream()
                .filter(variant -> variant.variant() == type && variant.format() == format)
                .findFirst()
                .orElseThrow();
    }

    private static byte[] jpeg(int width, int height) throws Exception {
        ByteArrayOutputStream output = output();
        if (!ImageIO.write(image(width, height), "jpeg", output)) {
            throw new IllegalStateException("JPEG writer is unavailable");
        }
        return output.toByteArray();
    }

    private static byte[] png(int width, int height) throws Exception {
        ByteArrayOutputStream output = output();
        if (!ImageIO.write(image(width, height), "png", output)) {
            throw new IllegalStateException("PNG writer is unavailable");
        }
        return output.toByteArray();
    }

    private static BufferedImage image(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(140, 110, 90));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(new Color(220, 190, 150));
            graphics.fillOval(width / 4, height / 4, width / 2, height / 2);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static ByteArrayOutputStream output() {
        return new ByteArrayOutputStream();
    }

    /** Inserts a minimal EXIF APP1 segment with orientation and one GPS tag into a JPEG. */
    private static byte[] withExifGps(byte[] jpeg) throws Exception {
        ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(tiff);
        data.write(new byte[] {'I', 'I', 0x2A, 0x00});
        data.writeInt(Integer.reverseBytes(8));
        data.writeShort(Short.reverseBytes((short) 2));
        writeEntry(data, 0x0112, 3, 1, 6);
        writeEntry(data, 0x8825, 4, 1, 38);
        data.writeInt(0);
        data.writeShort(Short.reverseBytes((short) 1));
        writeEntry(data, 0x0001, 2, 2, 'N');
        data.writeInt(0);
        data.flush();

        ByteArrayOutputStream app1 = new ByteArrayOutputStream();
        DataOutputStream segment = new DataOutputStream(app1);
        segment.writeByte(0xFF);
        segment.writeByte(0xE1);
        segment.writeShort(2 + 6 + tiff.size());
        segment.write(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        segment.write(tiff.toByteArray());
        segment.flush();

        byte[] result = new byte[jpeg.length + app1.size()];
        result[0] = jpeg[0];
        result[1] = jpeg[1];
        System.arraycopy(app1.toByteArray(), 0, result, 2, app1.size());
        System.arraycopy(jpeg, 2, result, 2 + app1.size(), jpeg.length - 2);
        return result;
    }

    private static void writeEntry(
            DataOutputStream data, int tag, int type, int count, int value) throws Exception {
        data.writeShort(Short.reverseBytes((short) tag));
        data.writeShort(Short.reverseBytes((short) type));
        data.writeInt(Integer.reverseBytes(count));
        if (type == 3) {
            data.writeShort(Short.reverseBytes((short) value));
            data.writeShort(0);
        } else {
            data.writeInt(Integer.reverseBytes(value));
        }
    }
}
