package com.uss.madsies;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

public class Main {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    static String ADMIN_SHEET;
    static Sheets service;
    static List<TeamData> teamsInfo;

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static void getAdminSheet() throws IOException {
        InputStream in = Main.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }

        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            ADMIN_SHEET = json.get("admin_sheet").getAsString();
        }
    }

    public static void createNewSheet(String newSheetName) throws IOException {
        SheetProperties sheetProperties = new SheetProperties();
        sheetProperties.setTitle(newSheetName);

        // Wrap in an AddSheetRequest
        AddSheetRequest addSheetRequest = new AddSheetRequest();
        addSheetRequest.setProperties(sheetProperties);

        // Wrap in a general Request
        Request request = new Request();
        request.setAddSheet(addSheetRequest);

        // Send batchUpdate request
        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest();
        batchUpdateRequest.setRequests(Collections.singletonList(request));

        service.spreadsheets().batchUpdate(ADMIN_SHEET, batchUpdateRequest).execute();
    }

    public static void writeMatchupSheet(String sheetID, List<MatchUp> matches) throws IOException {
        String range = sheetID + "!A1";
        List<List<Object>> values = new ArrayList<>();

        // Headers for sheet (Readability)
        values.add(Arrays.asList("Team A", "Team B", "Team A Score", "Team B Score"));

        // Data
        for(MatchUp match : matches)
        {
            values.add(Arrays.asList(match.team1, match.team2, 0, 0));
        }

        ValueRange body = new ValueRange().setValues(values);
        service.spreadsheets().values().update(ADMIN_SHEET, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Main.getAdminSheet();
        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();

        sortTeams(true);
        getFullData(); // Initialises data into code
        List<MatchUp> matches = Matchmaker.createSwissMatchups(teamsInfo);
        createNewSheet("Match1");
        writeMatchupSheet("Match1", matches);

        updateHistory(matches);
        updateOMWP();
        rewriteData();
    }

    public static void rewriteData() throws IOException {
        List<List<Object>> sheetData = new ArrayList<>();
        for (TeamData teamData : teamsInfo) {
            sheetData.add(teamData.convertToSpreadsheetRow());
        }
        ValueRange body = new ValueRange().setValues(sheetData);
        String range = "A2:ZZ1000";
        service.spreadsheets().values().update(ADMIN_SHEET, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    public static void updateHistory(List<MatchUp> matches) throws IOException
    {
        for (MatchUp m : matches) {
            addOpponent(m.team1, m.team2);
            addOpponent(m.team2, m.team1);
        }
    }

    private static void addOpponent( String team, String opponent)
    {
        for (TeamData t : teamsInfo)
        {
            String teamName = t.teamName;
            if (teamName.equals(team))
            {
                t.history.add(opponent);
                return;
            }
        }
    }

    public static void updateOMWP() {

        Map<String, int[]> teamRecords = new HashMap<>();
        for (TeamData team : teamsInfo)
        {
            teamRecords.put(team.teamName, new int[]{team.wins, team.losses});
        }

        for (TeamData team : teamsInfo)
        {
            double sum = 0;
            int count = 0;

            for (String opp : team.history)
            {
                if (!teamRecords.containsKey(opp)) continue;

                int[] rec = teamRecords.get(opp);
                int oppWins = rec[0];
                int oppLosses = rec[1];

                int totalGames = oppWins + oppLosses;
                if (totalGames == 0) continue;

                double winPct = (double) oppWins / totalGames;
                sum += winPct;
                count++;
            }

            team.omwp = (float) ((count == 0) ? 0 : sum / count);
        }
    }


    public static void listTeams()  throws IOException
    {
        final String range = "A2:A5";
        ValueRange response = service.spreadsheets().values()
                .get(ADMIN_SHEET, range)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        }
        else
        {
            for (List row : values)
            {
                System.out.printf("%s\n", row.get(0));
            }
        }
    }

    public static void addTeam(String name) throws IOException
    {
        List<List<Object>> value = Arrays.asList(Arrays.asList(name, 0, true, 0, 0));
        String range = "A2:A1000";
        ValueRange response = service.spreadsheets().values()
                .get(ADMIN_SHEET, range)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        }
        else
        {
            int placementOffset = values.size()+2; // Title and index offset
            ValueRange body = new ValueRange().setValues(value);
            range = "A"+placementOffset;
            service.spreadsheets().values().update(ADMIN_SHEET, range, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        }
    }

    public static void getFullData() throws IOException {
        String range = "A2:ZZ1000";
        ValueRange response = service.spreadsheets().values()
                .get(ADMIN_SHEET, range)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        }
        else
        {
            List<TeamData> data = new ArrayList<>();
            for (List<Object> row : values)
            {
               data.add(new TeamData(row));
            }
            teamsInfo = data;
        }
    }

    public static void sortTeams(boolean seeding) throws IOException
    {
        String range = "A2:E1000";
        ValueRange response = service.spreadsheets().values()
                .get(ADMIN_SHEET, range)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        }
        else
        {
            // Sort by seeding score
            if (seeding)
            {
                values.sort(Comparator.comparingInt(o -> Integer.parseInt((String) o.get(1))));
            }
            else {

            }
            values = values.reversed();
            ValueRange body = new ValueRange().setValues(values);
            service.spreadsheets().values().update(ADMIN_SHEET, range, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        }
    }
}