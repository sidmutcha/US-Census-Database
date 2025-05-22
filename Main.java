import java.util.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
        }

        System.out.println("Would you like the total population (T) or a specific age range (R)? Enter T or R:");
        String choice = scanner.nextLine().trim().toUpperCase();

        int total;
        if (choice.equals("T")) {
            total = checker.CensusCheckerAge(0, 100, stateFIPS);
        } else if (choice.equals("R")) {
            System.out.println("Enter minimum age: ");
            int minAge = scanner.nextInt();
            System.out.println("Enter maximum age: ");
            int maxAge = scanner.nextInt();
            scanner.nextLine();
            total = checker.CensusCheckerAge(minAge, maxAge, stateFIPS);
        } else {
            System.out.println("Invalid option. Exiting.");
            return;
        }

        System.out.println("------------------");
        System.out.println("TOTAL STATE POPULATION: " + total);
        System.out.println("------------------");
    }
}

class AgeGroup {
    int min, max;
    String maleCode, femaleCode;

    AgeGroup(int min, int max, String maleCode, String femaleCode) {
        this.min = min;
        this.max = max;
        this.maleCode = maleCode;
        this.femaleCode = femaleCode;
    }

    boolean overlaps(int rangeMin, int rangeMax) {
        return this.max >= rangeMin && this.min <= rangeMax;
    }
}

class CensusChecker {
    private static final Logger logger = Logger.getLogger(CensusChecker.class.getName());
    private static final String API_KEY = "77502d8fcdde51ae873d05b0e50a2b9fc180b41b";
    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<AgeGroup> AGE_GROUPS = List.of(
        new AgeGroup(0, 4, "B01001_003E", "B01001_027E"),
        new AgeGroup(5, 9, "B01001_004E", "B01001_028E"),
        new AgeGroup(10, 14, "B01001_005E", "B01001_029E"),
        new AgeGroup(15, 17, "B01001_006E", "B01001_030E"),
        new AgeGroup(18, 19, "B01001_007E", "B01001_031E"),
        new AgeGroup(20, 20, "B01001_008E", "B01001_032E"),
        new AgeGroup(21, 21, "B01001_009E", "B01001_033E"),
        new AgeGroup(22, 24, "B01001_010E", "B01001_034E"),
        new AgeGroup(25, 29, "B01001_011E", "B01001_035E"),
        new AgeGroup(30, 34, "B01001_012E", "B01001_036E"),
        new AgeGroup(35, 39, "B01001_013E", "B01001_037E"),
        new AgeGroup(40, 44, "B01001_014E", "B01001_038E"),
        new AgeGroup(45, 49, "B01001_015E", "B01001_039E"),
        new AgeGroup(50, 54, "B01001_016E", "B01001_040E"),
        new AgeGroup(55, 59, "B01001_017E", "B01001_041E"),
        new AgeGroup(60, 61, "B01001_018E", "B01001_042E"),
        new AgeGroup(62, 64, "B01001_019E", "B01001_043E"),
        new AgeGroup(65, 66, "B01001_020E", "B01001_044E"),
        new AgeGroup(67, 69, "B01001_021E", "B01001_045E"),
        new AgeGroup(70, 74, "B01001_022E", "B01001_046E"),
        new AgeGroup(75, 79, "B01001_023E", "B01001_047E"),
        new AgeGroup(80, 84, "B01001_024E", "B01001_048E"),
        new AgeGroup(85, 100, "B01001_025E", "B01001_049E")
    );

    public int CensusCheckerAge(int minAge, int maxAge, String stateFIPS) {
        List<String> variables = new ArrayList<>();
        for (AgeGroup group : AGE_GROUPS) {
            if (group.overlaps(minAge, maxAge)) {
                variables.add(group.maleCode);
                variables.add(group.femaleCode);
            }
        }

        if (variables.isEmpty()) return 0;
        String varList = String.join(",", variables);
        String url = String.format("https://api.census.gov/data/2021/acs/acs5?get=NAME,%s&for=place:*&in=state:%s&key=%s",
                                    varList, stateFIPS, API_KEY);
        try {
            String jsonData = fetch(url);
            return parseAndSum(jsonData);
        } catch (IOException e) {
            logger.severe("Failed to fetch or parse data: " + e.getMessage());
            return -1;
        }
    }

    private String fetch(String fullUrl) throws IOException {
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

    private int parseAndSum(String jsonData) throws IOException {
        JsonNode root = mapper.readTree(jsonData);
        if (!root.isArray() || root.size() < 2) return 0;

        JsonNode headers = root.get(0);
        List<Integer> popIndexes = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).asText();
            if (h.startsWith("B01001_")) popIndexes.add(i);
        }

        int total = 0;
        for (int i = 1; i < root.size(); i++) {
            JsonNode row = root.get(i);
            for (int idx : popIndexes) {
                total += Integer.parseInt(row.get(idx).asText());
            }
        }
        return total;
    }

    private static final Map<String, String> STATE_NAME_TO_FIPS = Map.ofEntries(
        Map.entry("Alabama", "01"), Map.entry("Alaska", "02"), Map.entry("Arizona", "04"),
        Map.entry("Arkansas", "05"), Map.entry("California", "06"), Map.entry("Colorado", "08"),
        Map.entry("Connecticut", "09"), Map.entry("Delaware", "10"), Map.entry("District of Columbia", "11"),
        Map.entry("Florida", "12"), Map.entry("Georgia", "13"), Map.entry("Hawaii", "15"),
        Map.entry("Idaho", "16"), Map.entry("Illinois", "17"), Map.entry("Indiana", "18"),
        Map.entry("Iowa", "19"), Map.entry("Kansas", "20"), Map.entry("Kentucky", "21"),
        Map.entry("Louisiana", "22"), Map.entry("Maine", "23"), Map.entry("Maryland", "24"),
        Map.entry("Massachusetts", "25"), Map.entry("Michigan", "26"), Map.entry("Minnesota", "27"),
        Map.entry("Mississippi", "28"), Map.entry("Missouri", "29"), Map.entry("Montana", "30"),
        Map.entry("Nebraska", "31"), Map.entry("Nevada", "32"), Map.entry("New Hampshire", "33"),
        Map.entry("New Jersey", "34"), Map.entry("New Mexico", "35"), Map.entry("New York", "36"),
        Map.entry("North Carolina", "37"), Map.entry("North Dakota", "38"), Map.entry("Ohio", "39"),
        Map.entry("Oklahoma", "40"), Map.entry("Oregon", "41"), Map.entry("Pennsylvania", "42"),
        Map.entry("Rhode Island", "44"), Map.entry("South Carolina", "45"), Map.entry("South Dakota", "46"),
        Map.entry("Tennessee", "47"), Map.entry("Texas", "48"), Map.entry("Utah", "49"),
        Map.entry("Vermont", "50"), Map.entry("Virginia", "51"), Map.entry("Washington", "53"),
        Map.entry("West Virginia", "54"), Map.entry("Wisconsin", "55"), Map.entry("Wyoming", "56")
    );

    public String getStateFipsFromName(String stateName) {
        return STATE_NAME_TO_FIPS.getOrDefault(stateName, null);
    }
}
