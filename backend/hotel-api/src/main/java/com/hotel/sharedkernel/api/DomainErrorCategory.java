package com.hotel.sharedkernel.api;

/**
 * Domain-nötr hata kategorisi. HTTP statü eşlemesi web katmanında yapılır; bu enum bilinçli olarak
 * Spring web/http tiplerine bağımlı değildir (docs/conventions.md §2.3, §2.1).
 */
public enum DomainErrorCategory {
    VALIDATION,
    NOT_FOUND,
    CONFLICT,
    FORBIDDEN,
    INTERNAL
}
