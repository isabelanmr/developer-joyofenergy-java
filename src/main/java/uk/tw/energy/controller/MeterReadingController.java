package uk.tw.energy.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.MeterReadings;
import uk.tw.energy.service.MeterReadingService;

@RestController
@RequestMapping("/readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    public MeterReadingController(MeterReadingService meterReadingService) {
        this.meterReadingService = meterReadingService;
    }

    /**
     * Endpoint para armazenar leituras de eletricidade.
     *
     * @param meterReadings Leituras de eletricidade e ID do medidor.
     * @return ResponseEntity com status 200 (OK) se as leituras forem válidas e armazenadas com sucesso,
     * ou status 400 (Bad Request) se as leituras forem inválidas, com mensagens de erro detalhadas.
     */
    @PostMapping("/store")
    public ResponseEntity<List<String>> storeReadings(@RequestBody MeterReadings meterReadings) {
        List<String> validationErrors = validate(meterReadings);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationErrors);
        }
        meterReadingService.storeReadings(meterReadings.smartMeterId(), meterReadings.electricityReadings());
        return ResponseEntity.ok().build();
    }

    /**
     * Valida as leituras de eletricidade de um medidor.
     *
     * @param meterReadings Leituras de eletricidade e ID do medidor.
     * @return Lista de mensagens de erro de validação, ou uma lista vazia se não houver erros.
     */
    public static List<String> validate(MeterReadings meterReadings) {
        List<String> errors = new ArrayList<>();

        String smartMeterId = meterReadings.smartMeterId();
        if (smartMeterId == null || smartMeterId.isEmpty()) {
            errors.add("Smart meter ID is null or empty");
        }

        List<ElectricityReading> electricityReadings = meterReadings.electricityReadings();
        if (electricityReadings == null || electricityReadings.isEmpty()) {
            errors.add("Electricity readings list is null or empty");
        } else {
            for (int i = 0; i < electricityReadings.size(); i++) {
                ElectricityReading reading = electricityReadings.get(i);
                if (reading.time() == null) {
                    errors.add("Electricity reading at index " + i + " has null time");
                }
                if (reading.reading() == null) {
                    errors.add("Electricity reading at index " + i + " has null reading");
                }
            }
        }

        return errors;
    }

    @GetMapping("/read/{smartMeterId}")
    public ResponseEntity readReadings(@PathVariable String smartMeterId) {
        Optional<List<ElectricityReading>> readings = meterReadingService.getReadings(smartMeterId);
        return readings.isPresent()
                ? ResponseEntity.ok(readings.get())
                : ResponseEntity.notFound().build();
    }
}
