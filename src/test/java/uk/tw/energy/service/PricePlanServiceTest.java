package uk.tw.energy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PricePlanServiceTest {

    private PricePlanService pricePlanService;
    private MeterReadingService meterReadingService;
    private List<PricePlan> pricePlans;

    @BeforeEach
    void setUp() {
        meterReadingService = mock(MeterReadingService.class);
        pricePlans = Arrays.asList(
                new PricePlan("plan1", "supplier1", BigDecimal.valueOf(0.20), Collections.emptyList()),
                new PricePlan("plan2", "supplier2", BigDecimal.valueOf(0.25), Collections.emptyList())
        );
        pricePlanService = new PricePlanService(pricePlans, meterReadingService);
    }

    @Test
    void testGetConsumptionCostOfElectricityReadingsForEachPricePlan_NoReadings() {
        String smartMeterId = "smartMeter1";
        when(meterReadingService.getReadings(smartMeterId)).thenReturn(Optional.empty());

        Optional<Map<String, BigDecimal>> result = pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetConsumptionCostOfElectricityReadingsForEachPricePlan_WithReadings() {
        String smartMeterId = "smartMeter1";
        List<ElectricityReading> readings = Arrays.asList(
                new ElectricityReading(Instant.parse("2023-07-01T10:00:00Z"), BigDecimal.valueOf(10)),
                new ElectricityReading(Instant.parse("2023-07-01T11:00:00Z"), BigDecimal.valueOf(20))
        );
        when(meterReadingService.getReadings(smartMeterId)).thenReturn(Optional.of(readings));

        Optional<Map<String, BigDecimal>> result = pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        assertTrue(result.isPresent());
        Map<String, BigDecimal> costs = result.get();
        assertEquals(2, costs.size());
        assertEquals(BigDecimal.valueOf(3.0000).setScale(4), costs.get("plan1"));
        assertEquals(BigDecimal.valueOf(3.7500).setScale(4), costs.get("plan2"));
    }
}
