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

import javax.swing.*;
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
    static String PUBLIC_SHEET = "1HRoTkeSpNUK4u2ft08EimauMcTNlndrIYl-gLZUy-8E";
    static Sheets service;
    static List<TeamData> teamsInfo;
    public static boolean ROUND_IN_PROGRESS = false;
    static List<MatchUp> matches;

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

    public static void createNewSheet() throws IOException {
        SheetProperties sheetProperties = new SheetProperties();
        int num = getSheetNumber() + 1;
        setSheetNumber(num);
        sheetProperties.setTitle("Match_"+num);

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

    public static void deleteSheet(String sheetName) throws IOException
    {
        var spreadsheet = service.spreadsheets().get(ADMIN_SHEET).execute();
        Integer sheetId = null;
        for (Sheet sheet : spreadsheet.getSheets())
        {
            if (sheet.getProperties().getTitle().equals(sheetName)) {
                sheetId = sheet.getProperties().getSheetId();
                break;
            }
        }
        if (sheetId == null) {
            System.out.println("Sheet not found: " + sheetName);
            return;
        }

        // Create DeleteSheetRequest
        DeleteSheetRequest deleteSheetRequest = new DeleteSheetRequest().setSheetId(sheetId);
        Request request = new Request().setDeleteSheet(deleteSheetRequest);
        List<Request> requests = new ArrayList<>();
        requests.add(request);

        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);

        service.spreadsheets().batchUpdate(ADMIN_SHEET, body).execute();
    }

    public static int getSheetNumber() throws IOException
    {
        String range = "Z1";
        ValueRange response = service.spreadsheets().values()
                .get(ADMIN_SHEET, range)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        }
        else
        {
            return Integer.parseInt(values.get(0).get(0).toString());
        }
        return 0;
    }

    public static void setSheetNumber(int val) throws IOException {
        String range = "Z1";
        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList(val));
        ValueRange body = new ValueRange().setValues(values);
        service.spreadsheets().values().update(ADMIN_SHEET, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

    }

    public static void writeMatchupSheet(List<MatchUp> matches) throws IOException {
        int num = getSheetNumber();
        String range = "Match_"+num+"!A1";
        List<List<Object>> values = new ArrayList<>();

        // Headers for sheet (Readability)
        values.add(Arrays.asList("Team A", "Team B", "Team A Score", "Team B Score"));

        // Data
        for(MatchUp match : matches)
        {
            // Auto filling in bye scores
            int scoreA = 0;
            int scoreB = 0;
            if (match.team1.equals("BYE")) scoreB = 2;
            if (match.team2.equals("BYE")) scoreA = 2;

            values.add(Arrays.asList(match.team1, match.team2, scoreA, scoreB));
        }

        ValueRange body = new ValueRange().setValues(values);
        service.spreadsheets().values().update(ADMIN_SHEET, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

    }

    /*
        Reads data from match sheet and updates win/losses accordingly
     */
    public static void updateRecords() throws IOException
    {
        int num = getSheetNumber();
        String range = "Match_"+num+"!A2:D";

        ValueRange response = service.spreadsheets().values()
                .get(ADMIN_SHEET, range)
                .execute();

        List<List<Object>> data = response.getValues();
        if (data == null || data.isEmpty())
        {
            System.out.println("No match data found.");
            return;
        }

        Map<String, TeamData> teamMap = new HashMap<>();
        for (TeamData t : teamsInfo)
        {
            teamMap.put(t.teamName, t);
        }

        for (List<Object> row : data) {
            if (row.size() < 4) continue; // skip incomplete rows

            String teamA = row.get(0).toString();
            String teamB = row.get(1).toString();

            int scoreA = Integer.parseInt(row.get(2).toString());
            int scoreB = Integer.parseInt(row.get(3).toString());

            if (Objects.equals(teamA, "BYE"))
            {
                teamMap.get(teamB).wins++;
                teamMap.get(teamB).map_wins += 2;
                continue;
            }
            else if (Objects.equals(teamB, "BYE"))
            {
                teamMap.get(teamA).wins++;
                teamMap.get(teamA).map_wins += 2;
                continue;
            }
            else if (scoreA > scoreB) {
                teamMap.get(teamA).wins++;
                teamMap.get(teamB).losses++;
            } else if (scoreB > scoreA) {
                teamMap.get(teamB).wins++;
                teamMap.get(teamA).losses++;
            }
            teamMap.get(teamA).map_wins += scoreA;
            teamMap.get(teamB).map_wins += scoreB;
            teamMap.get(teamA).map_losses += scoreB;
            teamMap.get(teamB).map_losses += scoreA;
        }

    }

    public static void genericSetup() throws IOException {
        sortTeams(true);
        rewriteData();
    }


    // Do this when matches are needed to be generated
    public static void generateRound() throws IOException {
        matches = Matchmaker.createSwissMatchups(teamsInfo);
        createNewSheet();
        writeMatchupSheet(matches);
    }

    public static void cancelRound() throws IOException
    {
        matches.clear();
        int num = getSheetNumber();
        deleteSheet("Match_"+num);
        setSheetNumber(num-1);
    }

    // Do this when all data is filled and all matches are done
    public static void endRound() throws IOException {
        updateRecords();
        updateHistory(matches);
        updateOMWP();
        sortTeams(false);
        rewriteData();
    }

    /*
        Writes necessary information to the public view sheet, ran at the end of each week
     */

    public static void updatePublicStandings() throws IOException {
        sortTeams(false);
        List<List<Object>> sheetData = new ArrayList<>();
        sheetData.add(Arrays.asList("Ranking", "Team", "Score", "Wins", "Losses", "OMWP"));
        int i = 1;
        for (TeamData t : teamsInfo)
        {
            sheetData.add(Arrays.asList(i, t.teamName, t.score, t.wins, t.losses, t.omwp));
            i++;
        }

        ValueRange body = new ValueRange().setValues(sheetData);
        String range = "A1";
        service.spreadsheets().values().update(PUBLIC_SHEET, range, body)
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

        getFullData(); // Initialises data into code

        SwingUtilities.invokeLater(() -> {
            GUIView view  = new GUIView();
            view.show();
        });

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

    public static void listTeams()
    {
        for (TeamData team : teamsInfo)
        {
            System.out.println(team.teamName);
        }
    }

    public static void addTeam(String name, int seeding)
    {
        TeamData data = new TeamData(name, seeding);
        teamsInfo.add(data);
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
        /*
            Placing order Wins -> OMWP -> Map Wins -> Map Losses (Inv) -> H2H
         */
        if (!seeding)
        {
            teamsInfo.sort(Comparator.comparingInt((TeamData t) -> t.wins)
                    .thenComparingDouble((TeamData t) -> t.omwp)
                    .thenComparingInt((TeamData t) -> t.map_wins).reversed()
                    .thenComparingInt((TeamData t) -> t.map_losses).reversed()
                    .thenComparingInt((TeamData t) -> t.seeding).reversed());
        }
        else {
            teamsInfo.sort(Comparator.comparingInt(o -> o.seeding));
            teamsInfo = teamsInfo.reversed();
        }

    }
}