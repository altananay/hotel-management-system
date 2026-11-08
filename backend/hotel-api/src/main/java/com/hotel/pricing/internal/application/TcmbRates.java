package com.hotel.pricing.internal.application;

import com.hotel.pricing.api.SupportedCurrency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record TcmbRates(LocalDate rateDate, Map<SupportedCurrency, BigDecimal> sellingRates) {}
