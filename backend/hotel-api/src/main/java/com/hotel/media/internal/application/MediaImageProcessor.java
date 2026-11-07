package com.hotel.media.internal.application;

import com.hotel.media.internal.domain.MediaVariantFormat;
import com.hotel.media.internal.domain.MediaVariantType;
import java.util.List;

public interface MediaImageProcessor {

    ImageInfo inspect(byte[] bytes);

    ProcessedImage process(byte[] bytes);

    record ImageInfo(String mimeType, int width, int height) {}

    record GeneratedVariant(
            MediaVariantType variant,
            MediaVariantFormat format,
            int width,
            int height,
            byte[] bytes) {}

    record ProcessedImage(int width, int height, List<GeneratedVariant> variants) {}
}
