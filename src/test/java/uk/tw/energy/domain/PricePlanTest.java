package uk.tw.energy.domain;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.tw.energy.service.MeterReadingService;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class PricePlanTest {

    private PricePlanService pricePlanService;
    private Map<String, List<ElectricityReading>> meterAssociatedReadings;
    private final String ENERGY_SUPPLIER_NAME = "Energy Supplier Name";

//    @BeforeEach
//    public void setUp() {
//        meterAssociatedReadings = new HashMap<>();
//        MeterReadingService meterReadingService = new MeterReadingService(meterAssociatedReadings);
//
//        List<PricePlan> pricePlans = List.of(
//                new PricePlan("price-plan-0", "Supplier 0", BigDecimal.TEN, List.of(
//                        new PricePlan.PeakTimeMultiplier(DayOfWeek.THURSDAY, BigDecimal.valueOf(2))
//                )),
//                new PricePlan("price-plan-1", "Supplier 1", BigDecimal.ONE, List.of(
//                        new PricePlan.PeakTimeMultiplier(DayOfWeek.THURSDAY, BigDecimal.valueOf(2))
//                )),
//                new PricePlan("price-plan-2", "Supplier 2", BigDecimal.valueOf(2), List.of(
//                        new PricePlan.PeakTimeMultiplier(DayOfWeek.THURSDAY, BigDecimal.valueOf(2))
//                ))
//        );
//        pricePlanService = new PricePlanService(pricePlans, meterReadingService);
//    }

    @Test
    public void shouldReturnTheEnergySupplierGivenInTheConstructor() {
        PricePlan pricePlan = new PricePlan(null, ENERGY_SUPPLIER_NAME, null, null);

        assertThat(pricePlan.getEnergySupplier()).isEqualTo(ENERGY_SUPPLIER_NAME);
    }

    @Test
    public void shouldReturnTheBasePriceGivenAnOrdinaryDateTime() throws Exception {
        LocalDateTime normalDateTime = LocalDateTime.of(2017, Month.AUGUST, 31, 12, 0, 0);
        PricePlan.PeakTimeMultiplier peakTimeMultiplier =
                new PricePlan.PeakTimeMultiplier(DayOfWeek.WEDNESDAY, BigDecimal.TEN);
        PricePlan pricePlan = new PricePlan(null, null, BigDecimal.ONE, singletonList(peakTimeMultiplier));

        BigDecimal price = pricePlan.getPrice(normalDateTime.toInstant(ZoneOffset.UTC));

        assertThat(price).isCloseTo(BigDecimal.ONE, Percentage.withPercentage(1));
    }

    @Test
    public void shouldReturnAnExceptionPriceGivenExceptionalDateTime() throws Exception {
        LocalDateTime exceptionalDateTime = LocalDateTime.of(2017, Month.AUGUST, 30, 23, 0, 0);
        PricePlan.PeakTimeMultiplier peakTimeMultiplier =
                new PricePlan.PeakTimeMultiplier(DayOfWeek.WEDNESDAY, BigDecimal.TEN);
        PricePlan pricePlan = new PricePlan(null, null, BigDecimal.ONE, singletonList(peakTimeMultiplier));

        BigDecimal price = pricePlan.getPrice(exceptionalDateTime.toInstant(ZoneOffset.UTC));

        assertThat(price).isCloseTo(BigDecimal.TEN, Percentage.withPercentage(1));
    }

    @Test
    public void shouldReceiveMultipleExceptionalDateTimes() throws Exception {
        LocalDateTime exceptionalDateTime = LocalDateTime.of(2017, Month.AUGUST, 30, 23, 0, 0);
        PricePlan.PeakTimeMultiplier peakTimeMultiplier =
                new PricePlan.PeakTimeMultiplier(DayOfWeek.WEDNESDAY, BigDecimal.TEN);
        PricePlan.PeakTimeMultiplier otherPeakTimeMultiplier =
                new PricePlan.PeakTimeMultiplier(DayOfWeek.TUESDAY, BigDecimal.TEN);
        List<PricePlan.PeakTimeMultiplier> peakTimeMultipliers =
                Arrays.asList(peakTimeMultiplier, otherPeakTimeMultiplier);
        PricePlan pricePlan = new PricePlan(null, null, BigDecimal.ONE, peakTimeMultipliers);

        BigDecimal price = pricePlan.getPrice(exceptionalDateTime.toInstant(ZoneOffset.UTC));

        assertThat(price).isCloseTo(BigDecimal.TEN, Percentage.withPercentage(1));
    }

//    @Test
//    public void shouldApplyPeakTimeMultiplierToAverageReading() throws Exception {
//        // Definindo as variáveis de tempo dentro do método de teste
//        Instant peakTime = LocalDateTime.of(2024, Month.JULY, 18, 18, 30).toInstant(ZoneOffset.UTC); // Horário de pico
//        Instant offPeakTime = LocalDateTime.of(2024, Month.JULY, 18, 10, 0).toInstant(ZoneOffset.UTC); // Horário fora de pico
//
//        List<ElectricityReading> electricityReadings = List.of(
//                new ElectricityReading(peakTime, BigDecimal.valueOf(0.5)),
//                new ElectricityReading(offPeakTime, BigDecimal.valueOf(0.5))
//        );
//
//        meterAssociatedReadings.put("smart-meter-0", electricityReadings);
//
//        Optional<Map<String, BigDecimal>> costMap = pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan("smart-meter-0");
//
//        assertThat(costMap).isPresent();
//        BigDecimal cost = costMap.get().get("price-plan-0");
//
//        // Calculando o custo esperado
//        BigDecimal expectedCost = BigDecimal.valueOf((0.5 * 2 + 0.5) / 2).multiply(BigDecimal.TEN).divide(BigDecimal.valueOf(10.0 / 3600.0), RoundingMode.HALF_UP);
//
//        assertThat(cost).isCloseTo(expectedCost, Percentage.withPercentage(1));
//    }
}
