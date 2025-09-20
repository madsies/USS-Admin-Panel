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
import com.google.api.services.sheets.v4.model.ValueRange;
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

    public static void createNewSheet()
    {

    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Main.getAdminSheet();

        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();

        //addTeam("meow");
        sortTeams(true);
        List<MatchUp> matches = Matchmaker.createSwissMatchups(Objects.requireNonNull(getFullData()));
        updateHistory(matches);
        updateOMWP();
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
        List<List<Object>> sheetData;
        try {
            sheetData = getFullData();
        }
        catch (IOException e) {
            return;
        }

        // For each match, add opponent names to each team
        for (MatchUp m : matches) {
            addOpponent(m.team1, m.team2);
            addOpponent(m.team2, m.team1);
        }
        rewriteData(sheetData);
    }


    private static void addOpponent( String team, String opponent)
    {
        for (TeamData team : teamsInfo) {
            String teamName = team.teamName;
            if (teamName.equals(team)) {
                row.add(opponent);
                return;
            }
        }
    }

    public static void updateOMWP() throws IOException {
        List<List<Object>> sheetData;
        try {
            sheetData = getFullData();
        } catch (IOException e) {
            return;
        }

        Map<String, int[]> teamRecords = new HashMap<>();
        for (List<Object> row : sheetData) {
            String team = row.get(0).toString();
            int wins = Integer.parseInt(row.get(4).toString());
            int losses = Integer.parseInt(row.get(5).toString());
            teamRecords.put(team, new int[]{wins, losses});
        }

        for (List<Object> row : sheetData)
        {
            String team = row.get(0).toString();
            List<Object> opponents = row.subList(9, row.size());

            double sum = 0;
            int count = 0;

            for (Object oppObj : opponents)
            {
                String opp = oppObj.toString();
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

            double omwp = (count == 0) ? 0 : sum / count;
            if (row.size() > 8) {
                row.set(8, omwp);
            } else {
                row.add(omwp);
            }
        }
        rewriteData(sheetData);
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

    public static List<List<Object>> getFullData() throws IOException {
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
            return values;
        }
        return null;
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