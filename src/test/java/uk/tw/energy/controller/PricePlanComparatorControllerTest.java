package uk.tw.energy.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.MeterReadingService;
import uk.tw.energy.service.PricePlanService;

public class PricePlanComparatorControllerTest {
    private static final String WORST_PLAN_ID = "worst-supplier";
    private static final String BEST_PLAN_ID = "best-supplier";
    private static final String SECOND_BEST_PLAN_ID = "second-best-supplier";
    private static final String SMART_METER_ID = "smart-meter-id";
    private PricePlanComparatorController controller;
    private MeterReadingService meterReadingService;
    private AccountService accountService;

    @BeforeEach
    public void setUp() {
        meterReadingService = new MeterReadingService(new HashMap<>());

        PricePlan pricePlan1 = new PricePlan(WORST_PLAN_ID, null, BigDecimal.TEN, List.of());
        PricePlan pricePlan2 = new PricePlan(BEST_PLAN_ID, null, BigDecimal.ONE, List.of());
        PricePlan pricePlan3 = new PricePlan(SECOND_BEST_PLAN_ID, null, BigDecimal.valueOf(2), List.of());
        List<PricePlan> pricePlans = List.of(pricePlan1, pricePlan2, pricePlan3);
        PricePlanService pricePlanService = new PricePlanService(pricePlans, meterReadingService);

        accountService = new AccountService(Map.of(SMART_METER_ID, WORST_PLAN_ID));

        controller = new PricePlanComparatorController(pricePlanService, accountService);
    }

    /**
     * Testa o método {@link PricePlanComparatorController#calculatedCostForEachPricePlan(String)}
     * para o caso em que há leituras de consumo de energia e o cálculo deve ser realizado com sucesso.
     * Verifica se os custos calculados para cada plano são arredondados e retornados corretamente.
     */
    @Test
    public void calculatedCostForEachPricePlan_happyPath() {
        var electricityReading = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(15.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(5.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        ResponseEntity<Map<String, Object>> response = controller.calculatedCostForEachPricePlan(SMART_METER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Para evitar avisos de cast não verificado. Isso é necessário porque o tipo genérico do mapa não pode ser
        // verificado em tempo de execução.
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> originalComparisons =
                (Map<String, BigDecimal>) response.getBody().get("pricePlanComparisons");
        Map<String, BigDecimal> roundedComparisons = getRoundedComparisons(originalComparisons);

        response.getBody().put("pricePlanComparisons", roundedComparisons);

        Map<String, Object> expected = Map.of(
                PricePlanComparatorController.PRICE_PLAN_ID_KEY,
                WORST_PLAN_ID,
                PricePlanComparatorController.PRICE_PLAN_COMPARISONS_KEY,
                Map.of(
                        WORST_PLAN_ID, BigDecimal.valueOf(100.0),
                        BEST_PLAN_ID, BigDecimal.valueOf(10.0),
                        SECOND_BEST_PLAN_ID, BigDecimal.valueOf(20.0)));
        assertThat(response.getBody()).isEqualTo(expected);
    }

    /**
     * Testa o método {@link PricePlanComparatorController#calculatedCostForEachPricePlan(String)}
     * para o caso em que não há leituras de consumo de energia.
     * Verifica se o status da resposta é NOT_FOUND (404) quando o identificador do medidor não é encontrado.
     */
    @Test
    public void calculatedCostForEachPricePlan_noReadings() {
        ResponseEntity<Map<String, Object>> response = controller.calculatedCostForEachPricePlan("not-found");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    /**
     * Testa o método {@link PricePlanComparatorController#recommendCheapestPricePlans(String, Integer)}
     * sem aplicar limite ao número de planos recomendados.
     * Verifica se a lista de planos recomendados é calculada corretamente com base nas leituras de consumo.
     */
    @Test
    public void recommendCheapestPricePlans_noLimit() {
        var electricityReading = new ElectricityReading(Instant.now().minusSeconds(1800), BigDecimal.valueOf(35.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response =
                controller.recommendCheapestPricePlans(SMART_METER_ID, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> map = response.getBody().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().setScale(1, RoundingMode.HALF_UP)));

        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, BigDecimal.valueOf(9.5)),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, BigDecimal.valueOf(19.0)),
                new AbstractMap.SimpleEntry<>(WORST_PLAN_ID, BigDecimal.valueOf(95.0)));

        Map<String, Object> map2 = new HashMap<>();
        for (AbstractMap.SimpleEntry<String, BigDecimal> entry : expectedPricePlanToCost) {
            map2.put(entry.getKey(), entry.getValue());
        }
        assertThat(map).isEqualTo(map2);
    }

    /**
     * Testa o método {@link PricePlanComparatorController#recommendCheapestPricePlans(String, Integer)}
     * aplicando um limite ao número de planos recomendados.
     * Verifica se apenas o número especificado de planos mais baratos é retornado com base nas leituras de consumo.
     */
    @Test
    public void recommendCheapestPricePlans_withLimit() {
        var electricityReading = new ElectricityReading(Instant.now().minusSeconds(2700), BigDecimal.valueOf(5.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(20.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response =
                controller.recommendCheapestPricePlans(SMART_METER_ID, 2);

        Map<String, Object> map = response.getBody().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().setScale(1, RoundingMode.HALF_UP)));

        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, BigDecimal.valueOf(9.4)),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, BigDecimal.valueOf(18.8)));

        Map<String, Object> map2 = new HashMap<>();
        for (AbstractMap.SimpleEntry<String, BigDecimal> entry : expectedPricePlanToCost) {
            map2.put(entry.getKey(), entry.getValue());
        }
        assertThat(map).isEqualTo(map2);
    }

    /**
     * Testa o método {@link PricePlanComparatorController#recommendCheapestPricePlans(String, Integer)}
     * quando o limite de planos recomendados é maior do que o número total de planos disponíveis.
     * Verifica se todos os planos são retornados corretamente, mesmo quando o limite é maior do que o número de planos.
     */
    @Test
    public void recommendCheapestPricePlans_limitHigherThanNumberOfEntries() {
        var reading0 = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(25.0));
        var reading1 = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(reading0, reading1));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response =
                controller.recommendCheapestPricePlans(SMART_METER_ID, 5);

        Map<String, Object> map = response.getBody().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().setScale(1, RoundingMode.HALF_UP)));

        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, BigDecimal.valueOf(14.0)),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, BigDecimal.valueOf(28.0)),
                new AbstractMap.SimpleEntry<>(WORST_PLAN_ID, BigDecimal.valueOf(140.0)));

        Map<String, Object> map2 = new HashMap<>();
        for (AbstractMap.SimpleEntry<String, BigDecimal> entry : expectedPricePlanToCost) {
            map2.put(entry.getKey(), entry.getValue());
        }
        assertThat(map).isEqualTo(map2);
    }

    /**
     * Lógica de arredondamento dos preços para cada plano obtido no response.
     */
    private static Map<String, BigDecimal> getRoundedComparisons(Map<String, BigDecimal> originalComparisons) {
        Map<String, BigDecimal> roundedComparisons = new HashMap<>();

        // A lógica de arredondamento foi extraída para um método separado para melhorar a clareza e reutilização do
        // código.
        for (Map.Entry<String, BigDecimal> entry : originalComparisons.entrySet()) {
            String key = entry.getKey();
            BigDecimal value = entry.getValue();
            BigDecimal roundedValue = value.setScale(1, RoundingMode.HALF_UP);
            roundedComparisons.put(key, roundedValue);
        }
        return roundedComparisons;
    }
}
