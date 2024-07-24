package uk.tw.energy.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class PricePlan {

    private final String energySupplier;
    private final String planName;
    private final BigDecimal unitRate; // unit price per kWh
    private final List<PeakTimeMultiplier> peakTimeMultipliers;

    public PricePlan(
            String planName, String energySupplier, BigDecimal unitRate, List<PeakTimeMultiplier> peakTimeMultipliers) {
        this.planName = planName;
        this.energySupplier = energySupplier;
        this.unitRate = unitRate;
        this.peakTimeMultipliers = peakTimeMultipliers;
    }

    public String getEnergySupplier() {
        return energySupplier;
    }

    public String getPlanName() {
        return planName;
    }

    public BigDecimal getUnitRate() {
        return unitRate;
    }

    @Override
    public String toString() {
        return "PricePlan{" + "energySupplier='"
                + energySupplier + '\'' + ", planName='"
                + planName + '\'' + ", unitRate="
                + unitRate + ", peakTimeMultipliers="
                + peakTimeMultipliers + '}';
    }

    public BigDecimal getPrice(Instant timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC);
        BigDecimal price = peakTimeMultipliers.stream()
                .filter(multiplier -> multiplier.dayOfWeek.equals(dateTime.getDayOfWeek()))
                .findFirst()
                .map(multiplier -> unitRate.multiply(multiplier.multiplier))
                .orElse(unitRate);
        System.out.println("DateTime: " + dateTime + ", Price: " + price);
        return price;
    }

    public static class PeakTimeMultiplier {

        DayOfWeek dayOfWeek;
        BigDecimal multiplier;

        public PeakTimeMultiplier(DayOfWeek dayOfWeek, BigDecimal multiplier) {
            this.dayOfWeek = dayOfWeek;
            this.multiplier = multiplier;
        }

        public DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }

        public BigDecimal getMultiplier() {
            return multiplier;
        }
    }
}
