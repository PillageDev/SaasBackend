package tbd.group.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.InternalServerErrorException;
import tbd.group.utils.exceptions.CollectionNotFound;
import tbd.group.utils.exceptions.InvalidFilter;

public class Pocketbase {
    private static final String POCKETBASE_URL = "127.0.0.1"; //TODO: update

    public static UserRecordList getRecords(String collectionIdentifier) {
        String endpoint = POCKETBASE_URL + "/api/collections/users/records";
        return getJson(endpoint, UserRecordList.class);
    }

    public static UserRecordList getRecords(String collectionIdentifier, RecordQuery query) {
        return getJson(POCKETBASE_URL + "/api/collections/users/records?" + query.getQuery(), UserRecordList.class);
    }

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

    static class RecordQuery implements QueryType {
        private int page;
        private int perPage;
        private String sort;
        private String filter;
        private String expand;
        private String fields;
        private boolean skipTotal;

        public RecordQuery(int page, int perPage, String sort, String filter, String expand, String fields,
                boolean skipTotal) {
            this.page = page;
            this.perPage = perPage;
            this.sort = sort;
            this.filter = filter;
            this.expand = expand;
            this.fields = fields;
            this.skipTotal = skipTotal;
        }

        public int getPage() {
            return page;
        }

        public int getPerPage() {
            return perPage;
        }

        public String getSort() {
            return sort;
        }

        public String getFilter() {
            return filter;
        }

        public String getExpand() {
            return expand;
        }

        public String getFields() {
            return fields;
        }

        public boolean isSkipTotal() {
            return skipTotal;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public void setPerPage(int perPage) {
            this.perPage = perPage;
        }

        public void setSort(String sort) {
            this.sort = sort;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        public void setExpand(String expand) {
            this.expand = expand;
        }

        public void setFields(String fields) {
            this.fields = fields;
        }

        public void setSkipTotal(boolean skipTotal) {
            this.skipTotal = skipTotal;
        }

        @Override
        public String getQuery() {
            StringBuilder query = new StringBuilder();
            if (page != 0) {
                query.append("page=").append(page).append("&");
            }
            if (perPage != 0) {
                query.append("perPage=").append(perPage).append("&");
            }
            if (sort != null) {
                query.append("sort=").append(sort).append("&");
            }
            if (filter != null) {
                query.append("filter=").append(filter).append("&");
            }
            if (expand != null) {
                query.append("expand=").append(expand).append("&");
            }
            if (fields != null) {
                query.append("fields=").append(fields).append("&");
            }
            if (skipTotal) {
                query.append("skipTotal=").append(skipTotal).append("&");
            }
            return query.toString();
        }
    }

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

        public int getPage() {
            return page;
        }

        public int getPerPage() {
            return perPage;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public List<Item> getItems() {
            return items;
        }

        static class Item {
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

            public String getId() {
                return id;
            }

            public String getCollectionId() {
                return collectionId;
            }

            public String getCollectionName() {
                return collectionName;
            }

            public String getUsername() {
                return username;
            }

            public boolean isVerified() {
                return verified;
            }

            public boolean isEmailVisibility() {
                return emailVisibility;
            }

            public String getEmail() {
                return email;
            }

            public String getCreated() {
                return created;
            }

            public String getUpdated() {
                return updated;
            }

            public String getName() {
                return name;
            }

            public String getAvatar() {
                return avatar;
            }

            public String getRole() {
                return role;
            }
        }
    }

    interface QueryType {
        public String getQuery();
    }
}
