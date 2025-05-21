import java.util.Scanner;

import java.util.Map;
import java.util.HashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) {
    CensusChecker checker = new CensusChecker();
    Scanner scanner = new Scanner(System.in);
    System.out.println("Enter the state name (e.g., North Carolina): ");
    String stateName = scanner.nextLine();
    String stateFIPS = checker.getStateFipsFromName(stateName);
    if (stateFIPS == null) {
        System.out.println("Invalid state name. Please try again.");
        return;
    } else {
        //System.out.println("------------------"); //THIS LINE IS FOR DEBUGGING
        int total = checker.CensusCheckerAge(15, 65, stateFIPS); // use stateFIPS, exact case
        System.out.println("------------------");
        System.out.println("TOTAL STATE POPULATION BETWEEN 15 AND 65: " + total);
        System.out.println("------------------");
    }
}

}

class CensusChecker {
    private static final Logger logger = Logger.getLogger(CensusChecker.class.getName());

    // Replace with your actual API key here:
    private static final String API_KEY = "77502d8fcdde51ae873d05b0e50a2b9fc180b41b"; // your actual key


    private final ObjectMapper mapper;

    public CensusChecker() {
        if (API_KEY == null || API_KEY.isEmpty()) {
            throw new IllegalStateException("API key is not set. Replace YOUR_CENSUS_API_KEY with your actual key.");
        }
        this.mapper = new ObjectMapper();
    }

    // Public method: accepts an age range and state, prints town data, returns total population
    public int CensusCheckerAge(int minAge, int maxAge, String stateFIPS) {
        //logger.info("Querying Census data for ages " + minAge + " to " + maxAge + " in state " + stateFIPS); //THIS LINE IS FOR DEBUGGING

        String url = buildAgeBasedUrl(minAge, maxAge, stateFIPS);

        try {
            String jsonData = fetch(url);
            return parseAndDisplayPopulationRange(jsonData);
        } catch (IOException e) {
            logger.severe("Failed to fetch or parse data: " + e.getMessage());
            return -1;
        }
    }

    // Build the Census API URL based on age range (currently hardcoded for age 15–65)
    private String buildAgeBasedUrl(int minAge, int maxAge, String stateFIPS) {
        // Variable codes from B01001 table for ages 15–65, male and female
        String[] variables = {
            "B01001_007E", "B01001_008E", "B01001_009E", "B01001_010E", "B01001_011E", "B01001_012E", "B01001_013E",
            "B01001_014E", "B01001_015E", "B01001_016E", "B01001_017E", // Male
            "B01001_031E", "B01001_032E", "B01001_033E", "B01001_034E", "B01001_035E", "B01001_036E", "B01001_037E",
            "B01001_038E", "B01001_039E", "B01001_040E", "B01001_041E"  // Female
        };

        String varList = String.join(",", variables);
        return String.format("https://api.census.gov/data/2021/acs/acs5?get=NAME,%s&for=place:*&in=state:%s&key=%s", varList, stateFIPS, API_KEY);
    }

    // Fetch JSON data from Census API
    private String fetch(String fullUrl) throws IOException {
        //logger.info("Requesting URL: " + fullUrl); //THIS LINE IS FOR DEBUGGING
        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int status = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            status == HttpURLConnection.HTTP_OK ? conn.getInputStream() : conn.getErrorStream()
        ));

        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        reader.close();
        conn.disconnect();

        if (status != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP Error " + status + ": " + content);
        }

        return content.toString();
    }

    // Parse and display per-town populations, return total population across all towns
    private int parseAndDisplayPopulationRange(String jsonData) throws IOException {
        JsonNode root = mapper.readTree(jsonData);
        if (!root.isArray() || root.size() < 2) {
            logger.warning("No results returned.");
            return 0;
        }

        JsonNode headers = root.get(0);
        int nameIndex = findIndex(headers, "NAME");

        List<Integer> popIndexes = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).asText();
            if (header.startsWith("B01001_") && header.endsWith("E")) {
                popIndexes.add(i);
            }
        }

        int totalPopulation = 0;

        for (int i = 1; i < root.size(); i++) {
            JsonNode row = root.get(i);
            String placeName = row.get(nameIndex).asText();
            int sum = 0;
            for (int index : popIndexes) {
                sum += Integer.parseInt(row.get(index).asText());
            }
            //System.out.println(placeName + " — Population: " + sum); //THIS LINE ADDS INDIVIDUAL TOWNS
            totalPopulation += sum;
        }

        return totalPopulation;
    }

    // Find the index of a column header
    private int findIndex(JsonNode headers, String columnName) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).asText().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private static final Map<String, String> STATE_NAME_TO_FIPS = createStateFipsMap();

    private static Map<String, String> createStateFipsMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Alabama", "01");
        map.put("Alaska", "02");
        map.put("Arizona", "04");
        map.put("Arkansas", "05");
        map.put("California", "06");
        map.put("Colorado", "08");
        map.put("Connecticut", "09");
        map.put("Delaware", "10");
        map.put("District of Columbia", "11");
        map.put("Florida", "12");
        map.put("Georgia", "13");
        map.put("Hawaii", "15");
        map.put("Idaho", "16");
        map.put("Illinois", "17");
        map.put("Indiana", "18");
        map.put("Iowa", "19");
        map.put("Kansas", "20");
        map.put("Kentucky", "21");
        map.put("Louisiana", "22");
        map.put("Maine", "23");
        map.put("Maryland", "24");
        map.put("Massachusetts", "25");
        map.put("Michigan", "26");
        map.put("Minnesota", "27");
        map.put("Mississippi", "28");
        map.put("Missouri", "29");
        map.put("Montana", "30");
        map.put("Nebraska", "31");
        map.put("Nevada", "32");
        map.put("New Hampshire", "33");
        map.put("New Jersey", "34");
        map.put("New Mexico", "35");
        map.put("New York", "36");
        map.put("North Carolina", "37");
        map.put("North Dakota", "38");
        map.put("Ohio", "39");
        map.put("Oklahoma", "40");
        map.put("Oregon", "41");
        map.put("Pennsylvania", "42");
        map.put("Rhode Island", "44");
        map.put("South Carolina", "45");
        map.put("South Dakota", "46");
        map.put("Tennessee", "47");
        map.put("Texas", "48");
        map.put("Utah", "49");
        map.put("Vermont", "50");
        map.put("Virginia", "51");
        map.put("Washington", "53");
        map.put("West Virginia", "54");
        map.put("Wisconsin", "55");
        map.put("Wyoming", "56");
        return map;
    }

    // New method to get FIPS from state name
    public String getStateFipsFromName(String stateName) {
        return STATE_NAME_TO_FIPS.getOrDefault(stateName, null);
    }
}
