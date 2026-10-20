package com.hotel.catalog.internal.domain;

import com.hotel.catalog.api.AmenityId;
import com.hotel.sharedkernel.api.DomainError;
import com.hotel.sharedkernel.api.DomainErrorCategory;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Amenity {

    public static final int MAX_NAME_LENGTH = 80;
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,49}$");

    private final AmenityId id;
    private final String code;
    private final String nameTr;
    private final String nameEn;

    public Amenity(AmenityId id, String code, String nameTr, String nameEn) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = validateCode(code);
        this.nameTr =
                TextValidation.validateAndTrim(
                        nameTr,
                        MAX_NAME_LENGTH,
                        CatalogErrorCodes.AMENITY_NAME_REQUIRED,
                        CatalogErrorCodes.AMENITY_NAME_INVALID,
                        "Amenity Turkish name");
        this.nameEn =
                TextValidation.validateAndTrim(
                        nameEn,
                        MAX_NAME_LENGTH,
                        CatalogErrorCodes.AMENITY_NAME_REQUIRED,
                        CatalogErrorCodes.AMENITY_NAME_INVALID,
                        "Amenity English name");
    }

    public static Amenity create(String code, String nameTr, String nameEn) {
        return new Amenity(AmenityId.generate(), code, nameTr, nameEn);
    }

    private static String validateCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new DomainError(
                    CatalogErrorCodes.AMENITY_CODE_INVALID,
                    "Amenity code must match ^[a-z][a-z0-9_]{1,49}$.",
                    DomainErrorCategory.VALIDATION);
        }
        return code;
    }

    public AmenityId id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String nameTr() {
        return nameTr;
    }

    public String nameEn() {
        return nameEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Amenity other)) {
            return false;
        }
        return id.equals(other.id)
                && code.equals(other.code)
                && nameTr.equals(other.nameTr)
                && nameEn.equals(other.nameEn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, nameTr, nameEn);
    }
}
