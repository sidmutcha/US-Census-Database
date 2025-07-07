import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    public static void main(String[] args) {
        CensusChecker checker = new CensusChecker();
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the state name (e.g., North Carolina): ");
        String stateName = scanner.nextLine().trim();
        String stateFIPS = checker.getStateFipsFromName(stateName);

        if (stateFIPS == null) {
            System.out.println("Invalid state name. Exiting.");
            return;
        }

        System.out.println("Choose data to fetch:");
        System.out.println("[T] Total Population");
        System.out.println("[R] Population by Age Range");
        System.out.println("[O] Population by Occupation");
        String option = scanner.nextLine().trim().toUpperCase();

        switch (option) {
            case "T":
                int total = checker.CensusCheckerAge(0, 100, stateFIPS);
                System.out.println("TOTAL STATE POPULATION: " + total);
                break;

            case "R":
                System.out.print("Enter minimum age: ");
                int minAge = scanner.nextInt();
                System.out.print("Enter maximum age: ");
                int maxAge = scanner.nextInt();
                scanner.nextLine(); // flush
                int ageTotal = checker.CensusCheckerAge(minAge, maxAge, stateFIPS);
                System.out.println("AGE RANGE POPULATION: " + ageTotal);
                break;

            case "O":
                System.out.println("Select occupation type:");
                for (OccupationCode code : OccupationCode.values()) {
                    System.out.println("- " + code.name());
                }
                String occInput = scanner.nextLine().trim().toUpperCase();
                try {
                    OccupationCode occupation = OccupationCode.valueOf(occInput);
                    int occTotal = checker.getOccupationTotal(stateFIPS, occupation);
                    System.out.println("WORKERS in " + occupation.name() + ": " + occTotal);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid occupation type.");
                }
                break;

            default:
                System.out.println("Invalid option. Exiting.");
        }
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

    boolean overlaps(int minAge, int maxAge) {
        return this.max >= minAge && this.min <= maxAge;
    }
}

enum OccupationCode {
    MANAGEMENT("C24010_003E", "C24010_050E"),
    SERVICE("C24010_010E", "C24010_057E"),
    SALES("C24010_016E", "C24010_063E"),
    NATURAL_RESOURCES("C24010_023E", "C24010_070E"),
    PRODUCTION("C24010_029E", "C24010_076E");

    public final String maleCode;
    public final String femaleCode;

    OccupationCode(String maleCode, String femaleCode) {
        this.maleCode = maleCode;
        this.femaleCode = femaleCode;
    }
}

class CensusChecker {
    private static final String API_KEY = "77502d8fcdde51ae873d05b0e50a2b9fc180b41b";
    private static final ObjectMapper mapper = new ObjectMapper();

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

    public int CensusCheckerAge(int minAge, int maxAge, String stateFips) {
        int total = 0;
        for (AgeGroup group : AGE_GROUPS) {
            if (group.overlaps(minAge, maxAge)) {
                total += fetch(group.maleCode, stateFips);
                total += fetch(group.femaleCode, stateFips);
            }
        }
        return total;
    }

    public int getOccupationTotal(String stateFips, OccupationCode occ) {
        return fetch(occ.maleCode, stateFips) + fetch(occ.femaleCode, stateFips);
    }

    public String getStateFipsFromName(String name) {
        return STATE_NAME_TO_FIPS.getOrDefault(name.trim(), null);
    }

    private int fetch(String var, String stateFips) {
        try {
            String url = "https://api.census.gov/data/2021/acs/acs5?get=" + var +
                         "&for=state:" + stateFips + "&key=" + API_KEY;

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            InputStream is = conn.getInputStream();
            JsonNode root = mapper.readTree(is);
            String value = root.get(1).get(0).asText();
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new RuntimeException("HTTP Error: " + e.getMessage(), e);
        }
    }
}
