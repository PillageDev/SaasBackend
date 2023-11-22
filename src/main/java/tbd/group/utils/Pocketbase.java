package tbd.group.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Cleanup;
import tbd.group.utils.exceptions.CollectionNotFound;
import tbd.group.utils.exceptions.InvalidFilter;

/**
 * This class is used to interact with the Pocketbase API.
 */
public class Pocketbase {
    /**
     * The base URL of the Pocketbase API.
     */
    private static final String POCKETBASE_URL = "127.0.0.1"; //TODO: update

    /**
     * Gets a list of users in a collection.
     * @return A list of users in the collection.
     */
    public static UserRecordList getUserRecords() {
        String endpoint = POCKETBASE_URL + "/api/collections/users/records";
        return getJson(endpoint, UserRecordList.class);
    }

    /**
     * Gets a list of users in a collection.
     * @param query The query to use.
     * @return A list of users in the collection.
     */
    public static UserRecordList getUserRecords(RecordQuery query) {
        return getJson(POCKETBASE_URL + "/api/collections/users/records?" + query.getQuery(), UserRecordList.class);
    }

    /**
     * Gets a record based on the record id.
     * @param recordID The ID of the record to get.
     * @return The record.
     */
    public static UserRecordList.Item getRecord(String recordID) {
        return getJson(POCKETBASE_URL + "/api/collections/usrs/records/" + recordID, UserRecordList.Item.class);
    }

    /**
     * Gets a list of users in a collection.
     * @param endpoint The endpoint to use.
     * @param clazz The class to serialize the JSON to.
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
                Jsonb jsonb = JsonbBuilder.create();
                JsonObject jsonObject = jsonb.fromJson(in, JsonObject.class);
                return jsonb.fromJson(jsonObject.toString(), clazz);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean postJson(String endpoint, JsonObject json) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.toString().getBytes("utf-8");
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
    static class RecordQuery implements QueryType {
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
         * <p style="color: red">OPERATOR</p> - could be any of the following:
         * <ul>
         * <li>= - Equal</li>
         * <li>!= - Not equal</li>
         * <li>> - Greater than</li>
         * <li>< - Less than</li>
         * <li>>= - Greater than or equal</li>
         * <li><= - Less than or equal</li>
         * <li>~ - Like / contains</li>
         * <li>!~ - Not like / does not contain</li>`
         * <li>&& - AND</li>
         * <li>|| - OR</li>
         * <li>?= - Any / at least one equal</li>
         * <li>?!= - Any / at least one not equal</li>
         * <li>?~ - Any / at least one like / contains</li>
         * <li>?!~ - Any / at least one not like / does not contain</li>
         * <li>?> - Any / at least one greater than</li>
         * <li>?< - Any / at least one less than</li>
         * <li>?>= - Any / at least one greater than or equal</li>
         * <li>?<= - Any / at least one less than or equal</li>
         * </ul>
         * 
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

        @Override
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
                nextChar = "&";
            }
            return query.toString();
        }

        static class FilterBuilder {
            private List<String> filter;

            public FilterBuilder(List<String> filter) {
                this.filter = filter;
            }

            public FilterBuilder add(String field, Operators operator, String value) {
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

            enum Operators {
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

                private String operator;

                Operators(String operator) {
                    this.operator = operator;
                }

                public String getOperator() {
                    return operator;
                }
            }
        }
    }

    @Getter
    static class UserRecordList {
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
        /**
         * A user in a collection.
         */
        @Getter
        static class Item {
            /**
             * The ID of the user
             */
            private String id;
            /**
             * The ID of the collection
             */
            private String collectionId;
            /**
             * The name of the collection
             */
            private String collectionName;
            /**
             * The username of the user
             */
            private String username;
            /**
             * Whether the user is verified
             */
            private boolean verified;
            /**
             * Whether the user's email is visible
             */
            private boolean emailVisibility;
            /**
             * The email of the user
             */
            private String email;
            /**
             * The date the user was created
             */
            private String created;
            /**
             * The date the user was updated
             */
            private String updated;
            /**
             * The name of the user
             */
            private String name;
            /**
             * The avatar of the user
             */
            private String avatar;
            /**
             * The role of the user
             */
            private String role;

            public Item(String id, String collectionId, String collectionName, String username, boolean verified,
                    boolean emailVisibility, String email, String created, String updated, String name, String avatar,
                    String role) {
                this.id = id;
                this.collectionId = collectionId;
                this.collectionName = collectionName;
                this.username = username;
                this.verified = verified;
                this.emailVisibility = emailVisibility;
                this.email = email;
                this.created = created;
                this.updated = updated;
                this.name = name;
                this.avatar = avatar;
                this.role = role;
            }
        }
    }

    @Getter
    @Builder(builderMethodName = "build")
    @AllArgsConstructor
    static class PreparedUser {
        private String username;
        private String password;
        private String passwordConfirm; 
        private String email;
        private boolean emailVisibility;
        private boolean verified;
        private String name;    }

    interface QueryType {
        public String getQuery();
    }
}
