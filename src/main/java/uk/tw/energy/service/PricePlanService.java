package uk.tw.energy.service;

import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PricePlanService {

    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }

    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(
            String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        if (!electricityReadings.isPresent()) {
            System.out.println("No readings found for smartMeterId: " + smartMeterId);
            return Optional.empty();
        }

        System.out.println("Calculating costs for smartMeterId: " + smartMeterId);

        // Converter a lista de pricePlans para uma lista mutável antes de realizar operações
        List<PricePlan> mutablePricePlans = new ArrayList<>(pricePlans);

        return Optional.of(mutablePricePlans.stream()
                .collect(Collectors.toMap(
                        PricePlan::getPlanName, t -> calculateCost(new ArrayList<>(electricityReadings.get()), t))));
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal totalCost = BigDecimal.ZERO;

        // Converter a lista de electricityReadings para uma lista mutável antes de ordenar
        List<ElectricityReading> mutableElectricityReadings = new ArrayList<>(electricityReadings);

        mutableElectricityReadings.sort(Comparator.comparing(ElectricityReading::time));

        for (int i = 0; i < mutableElectricityReadings.size() - 1; i++) {
            ElectricityReading currentReading = mutableElectricityReadings.get(i);
            ElectricityReading nextReading = mutableElectricityReadings.get(i + 1);

            long secondsElapsed =
                    Duration.between(currentReading.time(), nextReading.time()).getSeconds();

            if (secondsElapsed == 0) {
                continue;
            }

            BigDecimal averageReading = currentReading
                    .reading()
                    .add(nextReading.reading())
                    .divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);

            BigDecimal hoursElapsed = BigDecimal.valueOf(secondsElapsed / 3600.0);
            BigDecimal consumptionInKWh = averageReading.multiply(hoursElapsed);

            BigDecimal price = pricePlan.getPrice(currentReading.time());

            BigDecimal cost = consumptionInKWh.multiply(price).setScale(4, RoundingMode.HALF_UP);

            System.out.println("Reading Time: " + currentReading.time() + ", Hours Elapsed: " + hoursElapsed
                    + ", Average Reading (kWh): " + averageReading + ", Price: " + price + ", Cost: " + cost);
            totalCost = totalCost.add(cost);
        }

        System.out.println("Total Cost: " + totalCost);
        return totalCost;
    }
}
