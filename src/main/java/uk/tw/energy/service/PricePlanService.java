package uk.tw.energy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

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

        return Optional.of(pricePlans.stream()
                .collect(Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(electricityReadings.get(), t))));
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal totalCost = BigDecimal.ZERO;

        electricityReadings.sort(Comparator.comparing(ElectricityReading::time));

        for (int i = 0; i < electricityReadings.size() - 1; i++) {
            ElectricityReading currentReading = electricityReadings.get(i);
            ElectricityReading nextReading = electricityReadings.get(i + 1);

            long secondsElapsed =
                    Duration.between(currentReading.time(), nextReading.time()).getSeconds();

            if (secondsElapsed == 0) {
                continue;
            }

            BigDecimal averageReading = (currentReading.reading().add(nextReading.reading()))
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
