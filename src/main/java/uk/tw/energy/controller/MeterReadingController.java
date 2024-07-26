package uk.tw.energy.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.MeterReadings;
import uk.tw.energy.service.MeterReadingService;

/**
 * Controlador para armazenar e recuperar leituras de eletricidade com base no ID do medidor inteligente.
 */
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
     * ou status 400 (Bad Request) se as leituras forem inválidas.
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

    /**
     * Endpoint para ler as leituras de eletricidade associadas a um smart meter.
     *
     * Este método busca as leituras de eletricidade para um dado ID de medidor inteligente.
     * Se as leituras estiverem presentes e não estiverem vazias, retorna uma resposta com status 200 OK contendo a lista de leituras.
     * Caso contrário, retorna uma resposta com status 204 No Content.
     *
     * @param smartMeterId o ID do medidor inteligente
     * @return ResponseEntity contendo a lista de leituras de eletricidade ou um status 204 No Content se não houver leituras
     */
    @GetMapping("/read/{smartMeterId}")
    public ResponseEntity<List<ElectricityReading>> readReadings(@PathVariable String smartMeterId) {
        Optional<List<ElectricityReading>> readings = meterReadingService.getReadings(smartMeterId);

        if (readings.isPresent() && !readings.get().isEmpty()) {
            return ResponseEntity.ok(readings.get());
        } else {
            return ResponseEntity.noContent().build();
        }
    }
}
