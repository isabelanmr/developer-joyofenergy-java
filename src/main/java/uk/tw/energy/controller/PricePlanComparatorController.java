package uk.tw.energy.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.PricePlanService;


/**
 * Controlador responsável por comparar e recomendar planos de preços com base no consumo de eletricidade.
 */
@RestController
@RequestMapping("/price-plans")
public class PricePlanComparatorController {

    public static final String PRICE_PLAN_ID_KEY = "pricePlanId";
    public static final String PRICE_PLAN_COMPARISONS_KEY = "pricePlanComparisons";
    private final PricePlanService pricePlanService;
    private final AccountService accountService;


    /**
     * Construtor para inicializar os serviços de planos de preços e contas.
     *
     * @param pricePlanService Serviço de planos de preços.
     * @param accountService Serviço de contas.
     */
    public PricePlanComparatorController(PricePlanService pricePlanService, AccountService accountService) {
        this.pricePlanService = pricePlanService;
        this.accountService = accountService;
    }

    /**
     * Calcula o custo de consumo de eletricidade para cada plano de preços associado a um smart meter ID.
     *
     * @param smartMeterId ID do smart meter.
     * @return Um mapa contendo o ID do plano de preços e a comparação de preços, ou um status 204 se não houver dados de consumo.
     */
    @GetMapping("/compare-all/{smartMeterId}")
    public ResponseEntity<Map<String, Object>> calculatedCostForEachPricePlan(@PathVariable String smartMeterId) {
        String pricePlanId = accountService.getPricePlanIdForSmartMeterId(smartMeterId);
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        if (!consumptionsForPricePlans.isPresent()) {
            return ResponseEntity.noContent()
                    .header("Message", "Nenhum dado de consumo encontrado para o smart meter ID especificado")
                    .build();
        }

        Map<String, Object> pricePlanComparisons = new HashMap<>();
        pricePlanComparisons.put(PRICE_PLAN_ID_KEY, pricePlanId);
        pricePlanComparisons.put(PRICE_PLAN_COMPARISONS_KEY, consumptionsForPricePlans.get());

        return ResponseEntity.ok(pricePlanComparisons);
    }

    /**
     * Recomenda os planos de preços mais baratos com base no consumo de eletricidade para um smart meter ID.
     *
     * @param smartMeterId ID do smart meter.
     * @param limit Limite opcional de quantos planos de preços devem ser retornados.
     * @return Uma lista de entradas de mapa contendo os planos de preços recomendados e seus custos, ou um status 204 se não houver dados de consumo.
     */
    @GetMapping("/recommend/{smartMeterId}")
    public ResponseEntity<List<Map.Entry<String, BigDecimal>>> recommendCheapestPricePlans(
            @PathVariable String smartMeterId, @RequestParam(value = "limit", required = false) Integer limit) {
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        if (!consumptionsForPricePlans.isPresent()) {
            return ResponseEntity.noContent()
                    .header("Message", "Nenhum dado de consumo encontrado para o smart meter ID especificado")
                    .build();
        }

        List<Map.Entry<String, BigDecimal>> recommendations =
                new ArrayList<>(consumptionsForPricePlans.get().entrySet());
        recommendations.sort(Comparator.comparing(Map.Entry::getValue));

        if (limit != null && limit < recommendations.size()) {
            recommendations = recommendations.subList(0, limit);
        }

        return ResponseEntity.ok(recommendations);
    }
}
