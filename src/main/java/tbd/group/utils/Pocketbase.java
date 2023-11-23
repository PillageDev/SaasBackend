package tbd.group.utils;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.resource.cci.Record;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import tbd.group.utils.exceptions.CollectionNotFound;
import tbd.group.utils.exceptions.InvalidFilter;
import tbd.group.utils.exceptions.TokenAuthFail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to interact with the Pocketbase API.
 * @see <a href="https://docs.pocketbase.io/">Pocketbase API Docs</a>
 * @version 1.0
 */
@UtilityClass
@SuppressWarnings("deprecation")
public class Pocketbase {
    /**
     * The base URL of the Pocketbase API.
     */
    private static final String POCKETBASE_URL = "127.0.0.1"; //TODO: update

    /**
     * Gets a list of users in a collection.
     *
     * @return A list of users in the collection.
     */
    public static UserRecordList getUserRecords() {
        String endpoint = POCKETBASE_URL + "/api/collections/users/records";
        return getJson(endpoint, UserRecordList.class);
    }

    /**
     * Gets a list of users in a collection.
     *
     * @param query The query to use.
     * @return A list of users in the collection.
     */
    public static UserRecordList getUserRecords(@NonNull RecordQuery query) {
        return getJson(POCKETBASE_URL + "/api/collections/users/records?" + query.getQuery(), UserRecordList.class);
    }

    /**
     * Gets a record based on the record id.
     *
     * @param recordID The ID of the record to get.
     * @return The record.
     */
    public static UserRecordList.Item getRecord(String recordID) {
        return getJson(POCKETBASE_URL + "/api/collections/users/records/" + recordID, UserRecordList.Item.class);
    }

    /**
     * Gets a list of users in a collection.
     *
     * @param endpoint The endpoint to use.
     * @param clazz    The class to serialize the JSON to.
     */
    private static <T> T getJson(String endpoint, Class<T> clazz) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();
            if (responseCode == 400) {
                throw new InvalidFilter();
            } else if (responseCode == 404) {
                throw new CollectionNotFound();
            } else if (responseCode == 500) {
                throw new InternalServerErrorException();
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                @Cleanup Jsonb jsonb = JsonbBuilder.create();
                JsonObject jsonObject = jsonb.fromJson(in, JsonObject.class);
                return jsonb.fromJson(jsonObject.toString(), clazz);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method that posts a json to a Pocketbase endpoint
     *
     * @param user The user object to serialize
     * @return if the transaction was successful
     */
    private static boolean postJson(@NonNull PreparedUser user) {
        try {
            String endpoint = "/api/collections/users/records";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            JsonObject object = Json.createObjectBuilder()
                    .add("username", user.getUsername())
                    .add("password", user.getPassword())
                    .add("passwordConfirm", user.getPasswordConfirm())
                    .add("email", user.getEmail())
                    .add("emailVisibility", user.isEmailVisibility())
                    .add("verified", user.isVerified())
                    .add("name", user.getName())
                    .build();
            sendData(connection, object);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates a user based on the id and parameters
     * @param id The id of the user to update
     * @param update The update object
     */
    public static void updateUser(String id, @NonNull PreparedUserUpdate update) {
        try {
            String endpoint = "/api/collections/users/records/" + id;
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PATCH");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            JsonObject object = Json.createObjectBuilder()
                    .add("username", update.getUsername() == null ? "" : update.getUsername())
                    .add("password", update.getPassword() == null ? "" : update.getPassword())
                    .add("passwordConfirm", update.getPasswordConfirm() == null ? "" : update.getPasswordConfirm())
                    .add("email", update.getEmail() == null ? "" : update.getEmail())
                    .add("emailVisibility", update.isEmailVisibility())
                    .add("verified", update.isVerified())
                    .add("name", update.getName() == null ? "" : update.getName())
                    .build();
            sendData(connection, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a user from the database
     * @param id The id of the user to delete
     * @return If the transaction was successful
     */
    public static boolean deleteUser(String id) {
        try {
            String endpoint = "/api/collections/users/records/" + id;
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Listens for server side events and returns them
     * @return The server side event data.
     */
    public static SSEData listen() {
        try {
            String endpoint = "/api/realtime";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "text/event-stream");
            int responseCode = connection.getResponseCode();
            if (responseCode == 400) {
                throw new InvalidFilter();
            } else if (responseCode == 404) {
                throw new CollectionNotFound();
            } else if (responseCode == 500) {
                throw new InternalServerErrorException();
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                @Cleanup Jsonb jsonb = JsonbBuilder.create();
                JsonObject jsonObject = jsonb.fromJson(in, JsonObject.class);
                return jsonb.fromJson(jsonObject.toString(), SSEData.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendData(@NonNull HttpURLConnection connection, @NonNull JsonObject object) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = object.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        int responseCode = connection.getResponseCode();
        if (responseCode == 400) {
            throw new InvalidFilter();
        } else if (responseCode == 404) {
            throw new CollectionNotFound();
        } else if (responseCode == 500) {
            throw new InternalServerErrorException();
        }
    }

    /**
     * Authenticates a user with a username and password.
     *
     * @param identity The username or email of the user.
     * @param password The password of the user.
     * @return The session token.
     */
    public static long authenticate(String identity, String password) {
        try {
            String endpoint = "/api/collections/users/auth-with-password";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            JsonObject object = Json.createObjectBuilder()
                    .add("identity", identity)
                    .add("password", password)
                    .build();
            sendData(connection, object);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                @Cleanup Jsonb jsonb = JsonbBuilder.create();
                JsonObject jsonObject = jsonb.fromJson(in, JsonObject.class);
                return jsonObject.getJsonNumber("token").longValue();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Authenticates a user with 3rd party oath provider.
     * TODO: Register api/oauth2-redirect as a redirect url
     *
     * @param provider    The provider to use. (google, github, etc.)
     * @param authCode    The auth code from the provider.
     * @param verifier    The verifier from the provider.
     * @param redirectUrl The redirect url to use.
     * @return The session token.
     */
    public static long authenticate(String provider, String authCode, String verifier, String redirectUrl) {
        try {
            String endpoint = "/api/collections/users/auth-with-oauth2";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; utf-8");
            connection.setDoOutput(true);
            JsonObject object = Json.createObjectBuilder()
                    .add("provider", provider)
                    .add("authCode", authCode)
                    .add("verifier", verifier)
                    .add("redirectUrl", redirectUrl)
                    .build();
            sendData(connection, object);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                @Cleanup Jsonb jsonb = JsonbBuilder.create();
                JsonObject jsonObject = jsonb.fromJson(in, JsonObject.class);
                if (jsonObject.containsKey("token")) {
                    return jsonObject.getJsonNumber("token").longValue();
                } else {
                    throw new RuntimeException("Error authenticating with 3rd party provider");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns new auth token for an already authenticated user.
     *
     * @param token The token to use.
     * @return The session token.
     */
    public static long authRefresh(String token) {
        try {
            String endpoint = "/api/collections/users/auth-refresh";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setDoOutput(true);
            int responseCode = connection.getResponseCode();
            if (responseCode == 401) {
                throw new TokenAuthFail();
            } else if (responseCode == 404) {
                throw new CollectionNotFound("Missing auth record context.");
            } else if (responseCode == 500) {
                throw new InternalServerErrorException();
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                @Cleanup Jsonb jsonb = JsonbBuilder.create();
                JsonObject jsonObject = jsonb.fromJson(in, JsonObject.class);
                return jsonObject.getJsonNumber("token").longValue();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a verification email to the user containing an auth token
     *
     * @param email The email of the user to send the verification email to.
     * @return If the transaction was successful.
     * @see #confirmVerificationEmail(String)
     */
    public static boolean sendVerificationEmail(String email) {
        try {
            String endpoint = "/api/collections/users/request-verification";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            JsonObject object = Json.createObjectBuilder()
                    .add("email", email)
                    .build();
            sendData(connection, object);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Confirms a verification email based on the token received in the email.
     *
     * @param token The token to use.
     * @return If the transaction was successful.
     * @see #sendVerificationEmail(String)
     */
    public static boolean confirmVerificationEmail(String token) {
        try {
            String endpoint = "/api/collections/users/confirm-verification";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            JsonObject object = Json.createObjectBuilder()
                    .add("token", token)
                    .build();
            sendData(connection, object);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a password reset email to the user containing an auth token
     *
     * @param email The email of the user to send the password reset email to.
     * @return If the transaction was successful.
     */
    public static boolean resetPassword(String email) {
        try {
            String endpoint = "/api/collections/users/request-password-reset";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            JsonObject object = Json.createObjectBuilder()
                    .add("email", email)
                    .build();
            sendData(connection, object);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Confirms a password reset based on the token received in the email.
     * WARNING: This will invalidate all current auth tokens.
     *
     * @param token           The token to use.
     * @param password        The new password.
     * @param passwordConfirm The new password confirmation.
     * @return If the transaction was successful.
     */
    public static boolean confirmReset(String token, String password, String passwordConfirm) {
        try {
            String endpoint = "/api/collections/users/confirm-password-reset";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            JsonObject object = Json.createObjectBuilder()
                    .add("token", token)
                    .add("password", password)
                    .add("passwordConfirm", passwordConfirm)
                    .build();
            sendData(connection, object);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Changes the email of the user.
     *
     * @param newEmail The new email.
     * @param token    The token to use.
     * @return If the transaction was successful.
     */
    public static boolean requestEmailChange(String newEmail, String token) {
        try {
            String endpoint = "/api/collections/users/request-email-change";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            JsonObject object = Json.createObjectBuilder()
                    .add("newEmail", newEmail)
                    .add("token", token)
                    .build();
            connection.setRequestProperty("Authorization", "Bearer " + token);
            sendData(connection, object);
            if (connection.getResponseCode() == 401) {
                throw new TokenAuthFail();
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Confirms an email change request.
     *
     * @param token    The token to use.
     * @param password The password of the user.
     */
    public static void confirmEmailChange(String token, String password) {
        try {
            String endpoint = "/api/collections/users/confirm-email-change";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            JsonObject object = Json.createObjectBuilder()
                    .add("token", token)
                    .add("password", password)
                    .build();
            sendData(connection, object);
            if (connection.getResponseCode() == 401) {
                throw new TokenAuthFail();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a list of auth providers.
     *
     * @return A list of auth providers.
     */
    public static @NonNull List<AuthProvider> getAuthMethods() {
        try {
            String endpoint = "/api/collections/users/auth-providers";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                @Cleanup Jsonb jsonb = JsonbBuilder.create();
                JsonObject jsonObject = jsonb.fromJson(in, JsonObject.class);
                List<AuthProvider> providers = new ArrayList<>();
                for (String key : jsonObject.keySet()) {
                    JsonObject provider = jsonObject.getJsonObject(key);
                    providers.add(AuthProvider.builder()
                            .name(key)
                            .state(provider.getString("state"))
                            .codeVerifier(provider.getString("codeVerifier"))
                            .codeChallenge(provider.getString("codeChallenge"))
                            .codeChallengeMethod(provider.getString("codeChallengeMethod"))
                            .authUrl(provider.getString("authUrl"))
                            .build());
                }
                return providers;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of all Oath2 accounts for a user.
     * Only admins and account owners can access this endpoint.
     *
     * @param id    The id of the user.
     * @param token The token to use.
     * @return A list of Oath2 accounts.
     */
    public static @NonNull List<Oath2Account> oath2Accounts(String id, String token) {
        try {
            String endpoint = "/api/collections/users/records/" + id + "/external-auths";
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                @Cleanup Jsonb jsonb = JsonbBuilder.create();
                JsonObject jsonObject = jsonb.fromJson(in, JsonObject.class);
                List<Oath2Account> accounts = new ArrayList<>();
                for (String key : jsonObject.keySet()) {
                    JsonObject account = jsonObject.getJsonObject(key);
                    accounts.add(Oath2Account.builder()
                            .id(key)
                            .created(account.getString("created"))
                            .updated(account.getString("updated"))
                            .recordId(account.getString("recordId"))
                            .collectionId(account.getString("collectionId"))
                            .provider(account.getString("provider"))
                            .providerId(account.getString("providerId"))
                            .build());
                }
                return accounts;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unlinks an oath2 account from a user.
     * Only admins and account owners can access this endpoint.
     * @param id The id of the user.
     * @param provider The provider to unlink.
     * @param token The token to use.
     * @return If the transaction was successful.
     */
    public static boolean unlinkOathAccount(String id, String provider, String token) {
        try {
            String endpoint = "/api/collections/users/records/" + id + "/external-auths/" + provider;
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            if (connection.getResponseCode() == 401) {
                throw new TokenAuthFail();
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A query to use when getting records.
     */
    @Getter
    @Setter
    public static class RecordQuery {
        /**
         * The page (offset) of the paginated list. (Default is 1)
         */
        private int page;
        /**
         * The max returned items per page. (Default is 30)
         */
        private int perPage;
        /**
         * Specify the ORDER BY fields
         * Add a minus (-) before the field name to sort DESC
         * <p>Example: ?sort=-created,id</p>
         * DESC by created, ASC by id
         * Supported record sort fields:
         * <ul>
         * <li>@random</li>
         * <li>id</li>
         * <li>created</li>
         * <li>updated</li>
         * </ul>
         */
        private String sort;
        /**
         * Filter expression to filter/search the returned record list (in addition to the collection's listRule)
         * <p>Example: ?filter=(title~'abc' && created>'2023-01-01')</p>
         * Supported record filter fields:
         * <ul>
         * <li>id</li>
         * <li>created</li>
         * <li>updated</li>
         * </ul>
         * Syntax guide:
         * <p style="color: green">OPERAND</p> <p style="color: red">OPERATOR</p> <p style="color: green">OPERAND</p> where:
         * <p style="color: green">OPERAND</p> - could be any above field literal, string (single or double), number, null, boolean
         * <p style="color: red">OPERATOR</p> - could be any in the operators enum
         * @see FilterBuilder.Operators
         * @see FilterBuilder
         */
        private String filter;
        /**
         * If this is set to true, the totalItems and totalPages fields will be set to -1.
         */
        private boolean skipTotal;

        public RecordQuery(int page, int perPage, String sort, String filter, boolean skipTotal) {
            this.page = page;
            this.perPage = perPage;
            this.sort = sort;
            this.filter = filter;
            this.skipTotal = skipTotal;
        }

        public String getQuery() {
            StringBuilder query = new StringBuilder();
            String nextChar = "?";
            if (page != 0) {
                query.append("page=").append(page).append(nextChar);
                nextChar = "&";
            }
            if (perPage != 0) {
                query.append("perPage=").append(perPage).append(nextChar);
                nextChar = "&";
            }
            if (sort != null) {
                query.append("sort=").append(sort).append(nextChar);
                nextChar = "&";
            }
            if (filter != null) {
                query.append("filter=").append(filter).append(nextChar);
                nextChar = "&";
            }
            if (skipTotal) {
                query.append("skipTotal=").append(skipTotal).append(nextChar);
            }
            return query.toString();
        }

        public static class FilterBuilder {
            private final List<String> filter;

            public FilterBuilder(List<String> filter) {
                this.filter = filter;
            }

            public FilterBuilder add(String field, @NonNull Operators operator, String value) {
                filter.add(field + operator.getOperator() + value);
                return this;
            }

            public String build() {
                StringBuilder filter = new StringBuilder();
                filter.append("(");
                for (String filterItem : this.filter) {
                    filter.append(" && ");
                    filter.append(filterItem);
                }
                filter.append(")");
                return filter.toString();
            }

            @Getter
            public enum Operators {
                EQUAL("="),
                NOT_EQUAL("!="),
                GREATER_THAN(">"),
                LESS_THAN("<"),
                GREATER_THAN_OR_EQUAL(">="),
                LESS_THAN_OR_EQUAL("<="),
                LIKE("~"),
                NOT_LIKE("!~"),
                AND("&&"),
                OR("||"),
                ANY_EQUAL("?="),
                ANY_NOT_EQUAL("?!="),
                ANY_LIKE("?~"),
                ANY_NOT_LIKE("?!~"),
                ANY_GREATER_THAN("?>"),
                ANY_LESS_THAN("?<"),
                ANY_GREATER_THAN_OR_EQUAL("?>="),
                ANY_LESS_THAN_OR_EQUAL("?<=");

                private final String operator;

                Operators(String operator) {
                    this.operator = operator;
                }

            }
        }
    }

    @Getter
    @FieldDefaults(makeFinal = true)
    public static class UserRecordList {
        private int page;
        private int perPage;
        private int totalPages;
        private int totalItems;
        private List<Item> items;

        public UserRecordList(int page, int perPage, int totalPages, int totalItems, List<Item> items) {
            this.page = page;
            this.perPage = perPage;
            this.totalPages = totalPages;
            this.totalItems = totalItems;
            this.items = items;
        }

        @Getter
        @FieldDefaults(makeFinal = true)
        @AllArgsConstructor
        public static class Item {
            private String id;
            private String collectionId;
            private String collectionName;
            private String username;
            private boolean verified;
            private boolean emailVisibility;
            private String email;
            private String created;
            private String updated;
            private String name;
            private String avatar;
            private String role;
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PreparedUser {
        private String username;
        private String password;
        private String passwordConfirm;
        private String email;
        private boolean emailVisibility;
        private boolean verified;
        private String name;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PreparedUserUpdate {
        private String username;
        private String password;
        private String passwordConfirm;
        private String email;
        private boolean emailVisibility;
        private boolean verified;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class SSEData {
        private String action;
        private UserRecordList.Item record;
    }

    @Getter
    @AllArgsConstructor
    @FieldDefaults(makeFinal = true)
    @Builder
    public static class AuthProvider {
        private String name;
        private String state;
        private String codeVerifier;
        private String codeChallenge;
        private String codeChallengeMethod;
        private String authUrl;
    }

    @Getter
    @AllArgsConstructor
    @FieldDefaults(makeFinal = true)
    @Builder
    public static class Oath2Account {
        private String id;
        private String created;
        private String updated;
        private String recordId;
        private String collectionId;
        private String provider;
        private String providerId;
    }
}