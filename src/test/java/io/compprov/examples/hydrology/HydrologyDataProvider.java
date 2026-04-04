package io.compprov.examples.hydrology;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.compprov.examples.nav.NetAssetValueCalculator;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads aggregated JSON with simulations and observations from Villamar's publication. Date range: [1991-01-01; 1991-06-30]
 */
public class HydrologyDataProvider {

    private Map<String, List<BigDecimal>> dataMap;

    public HydrologyDataProvider(ObjectMapper mapper) {
        try {
            dataMap = mapper.readValue(
                    NetAssetValueCalculator.class.getResourceAsStream("/inputs/hydrology_aggregation.json").readAllBytes(),
                    new TypeReference<Map<String, List<BigDecimal>>>() {
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<BigDecimal> fetchObservedValues() {
        return dataMap.get("observed");
    }

    public List<BigDecimal> fetchSimulatedValues(String caseId) {
        return Objects.requireNonNull(dataMap.get(caseId));
    }

    public List<String> fetchCaseIds() {
        return Arrays.asList("00", "01", "03", "05", "06", "07", "08", "09", "10", "11", "12");
    }

    public List<String> allIds() {
        return Arrays.asList("observed", "00", "01", "03", "05", "06", "07", "08", "09", "10", "11", "12");
    }
}
